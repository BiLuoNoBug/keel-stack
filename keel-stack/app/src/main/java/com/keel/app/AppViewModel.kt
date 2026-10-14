package com.keel.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keel.app.data.AppNotice
import com.keel.app.data.Item
import com.keel.app.data.Role
import com.keel.app.data.UpdateInfo
import com.keel.app.data.remote.ApiClient
import com.keel.app.data.remote.ItemBody
import com.keel.app.data.remote.RealtimeClient
import com.keel.app.data.remote.StatusCountDto
import com.keel.app.data.remote.StatusRequest
import com.keel.app.data.remote.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

/**
 * 状态中心。
 *
 * 数据来源两层：
 *   1. 服务端（items / notifications / releases）—— 唯一事实来源
 *   2. Socket.IO 实时通道 —— 条目变动、状态流转、站内消息、新版本到来时自动刷新
 *
 * 骨架不含离线兜底：网络失败只提示，不塞本地假数据。
 * 写操作采用「后台同步 + 成功后重新拉取」，UI 层不必关心异步细节。
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("keel_app", 0)

    /* ------------------------------------------------------------ 本地偏好 */
    var role by mutableStateOf(runCatching { Role.valueOf(prefs.getString("role", Role.CLIENT.name)!!) }.getOrDefault(Role.CLIENT))
        private set
    var darkMode by mutableStateOf(prefs.getBoolean("dark", false))
        private set
    var apiBaseUrl by mutableStateOf(prefs.getString("api", ApiClient.DEFAULT_HOST) ?: ApiClient.DEFAULT_HOST)
        private set
    var notificationsEnabled by mutableStateOf(prefs.getBoolean("notifications", true))
        private set

    /* ------------------------------------------------------------ 条目列表 */
    var items by mutableStateOf<List<Item>>(emptyList())
        private set
    var itemsTotal by mutableStateOf(0)
        private set
    var itemsHasMore by mutableStateOf(false)
        private set
    var itemsPage by mutableStateOf(1)
        private set
    var itemCategories by mutableStateOf<List<String>>(emptyList())
        private set
    var itemStatusCounts by mutableStateOf<List<StatusCountDto>>(emptyList())
        private set

    /** 列表页当前筛选条件；socket 刷新时沿用同一套条件 */
    var filterCategory by mutableStateOf<String?>(null)
        private set
    var filterStatus by mutableStateOf<String?>(null)
        private set
    var filterKeyword by mutableStateOf("")
        private set

    private val pageSize = 20

    /* ------------------------------------------------------------ 站内消息 */
    var notices by mutableStateOf<List<AppNotice>>(emptyList())
        private set

    /** 已经读到哪条消息，用来算小红点 */
    var lastReadNoticeId by mutableStateOf(prefs.getInt("lastReadNoticeId", 0))
        private set

    /** 最新一条消息比「读到的位置」新，就说明有未读 */
    val hasUnreadNotices: Boolean get() = (notices.maxOfOrNull { it.id } ?: 0) > lastReadNoticeId

    /** 顶部横幅要展示的消息；null 表示没有 */
    var bannerNotice by mutableStateOf<AppNotice?>(null)
        private set

    /* ------------------------------------------------------------ 更新 */
    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set
    var checkingUpdate by mutableStateOf(false)
        private set
    var updateDownloading by mutableStateOf(false)
        private set
    var updateProgress by mutableStateOf(0f)
        private set

    /* ------------------------------------------------------------ 连接状态 */
    var syncing by mutableStateOf(false)
        private set
    var online by mutableStateOf(false)
        private set
    var realtimeConnected by mutableStateOf(false)
        private set
    var feedbackMessage by mutableStateOf<String?>(null)
        private set

    init {
        ApiClient.updateHost(apiBaseUrl)
        RealtimeClient.bind(object : RealtimeClient.Listener {
            override fun onConnected() {
                realtimeConnected = true
                online = true
            }

            override fun onDisconnected() {
                realtimeConnected = false
            }

            override fun onEvent(event: String, payload: JSONObject?) {
                when (event) {
                    "item:changed" -> loadItems(reset = true)
                    "item:status" -> payload?.let(::itemFromJson)?.let { updated ->
                        items = items.map { if (it.id == updated.id) updated else it }
                        loadItemStatusCounts()
                    }
                    "notification:new" -> {
                        loadNotices()
                        val notice = payload?.let {
                            AppNotice(
                                id = it.optInt("id"),
                                title = it.optString("title"),
                                content = it.optString("content"),
                                important = it.optString("level") == "IMPORTANT",
                                date = "刚刚",
                                link = it.optString("link")
                            )
                        }
                        // 推送是广播给所有设备的，但 App 端只在意发给 CLIENT（或全体）的那条
                        val target = payload?.optString("target").orEmpty().ifBlank { "ALL" }
                        if (notice != null && notice.title.isNotBlank() &&
                            (target == "ALL" || target == "CLIENT") && notificationsEnabled
                        ) {
                            bannerNotice = notice
                        }
                    }
                    "release:new" -> checkUpdate()
                }
            }
        })
        loadAll()
        connectRealtime()
        checkUpdate(silent = true)
    }

    /** 服务端 item:status 推的是完整 ItemDto，转成本地模型原地更新列表 */
    private fun itemFromJson(json: JSONObject): Item {
        val allowNext = json.optJSONArray("allowNext")
            ?.let { arr -> (0 until arr.length()).map { arr.optString(it) } }
            ?: emptyList()
        return Item(
            id = json.optInt("id"),
            title = json.optString("title"),
            category = json.optString("category"),
            content = json.optString("content"),
            coverUrl = json.optString("coverUrl").ifBlank { null },
            status = json.optString("status"),
            statusText = json.optString("statusText"),
            allowNext = allowNext,
            priority = json.optString("priority").ifBlank { "NORMAL" },
            published = json.optBoolean("published", true),
            sortOrder = json.optInt("sortOrder"),
            createdAt = json.optString("createdAt"),
            updatedAt = json.optString("updatedAt")
        )
    }

    /* ------------------------------------------------------------ 拉取 */

    private fun connectRealtime() {
        // 角色决定订阅的房间：与 server/socket/index.js 里的 ROOM 一一对应
        RealtimeClient.connect(if (role == Role.ADMIN) "admin" else "client", apiBaseUrl)
    }

    fun loadAll() {
        loadItems(reset = true)
        loadItemCategories()
        loadItemStatusCounts()
        loadNotices()
    }

    /** 列表页改筛选条件后重拉第一页 */
    fun applyItemFilters(category: String?, status: String?, keyword: String) {
        filterCategory = category
        filterStatus = status
        filterKeyword = keyword
        loadItems(reset = true)
    }

    fun loadItems(reset: Boolean = true) {
        val nextPage = if (reset) 1 else itemsPage + 1
        io {
            val resp = ApiClient.api.items(
                category = filterCategory,
                keyword = filterKeyword.ifBlank { null },
                status = filterStatus,
                page = nextPage,
                pageSize = pageSize
            )
            val list = resp.data?.list?.map { it.toModel() } ?: emptyList()
            val total = resp.data?.total ?: 0
            items = if (reset) list else items + list
            itemsPage = nextPage
            itemsTotal = total
            itemsHasMore = items.size < total
        }
    }

    fun loadMoreItems() {
        if (itemsHasMore) loadItems(reset = false)
    }

    /** 下拉刷新：回到第一页，顺带刷新分类与计数 */
    fun refreshItems() {
        loadItems(reset = true)
        loadItemCategories()
        loadItemStatusCounts()
    }

    fun loadItemCategories() = io {
        ApiClient.api.itemCategories().data?.let { itemCategories = it }
    }

    fun loadItemStatusCounts() = io {
        ApiClient.api.itemStatusCount().data?.let { itemStatusCounts = it }
    }

    /** 拉单条（详情 / 编辑用），结果通过回调交给调用方，避免多个页面抢同一份状态 */
    fun fetchItem(id: Int, onLoaded: (Item?) -> Unit) {
        viewModelScope.launch {
            val item = runCatching { ApiClient.api.item(id).data?.toModel() }.getOrNull()
            onLoaded(item)
        }
    }

    /** 站内消息：App 端取发给 CLIENT 的消息 */
    fun loadNotices() = io {
        ApiClient.api.notifications("CLIENT").data?.list?.map { it.toModel() }?.let { notices = it }
    }

    fun dismissBanner() {
        bannerNotice = null
    }

    /** 打开消息列表时调用，清掉小红点 */
    fun markNoticesRead() {
        val latest = notices.maxOfOrNull { it.id } ?: return
        if (latest > lastReadNoticeId) {
            lastReadNoticeId = latest
            prefs.edit().putInt("lastReadNoticeId", latest).apply()
        }
    }

    /* ------------------------------------------------------------ 条目写操作 */

    fun createItem(body: ItemBody, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { ApiClient.api.createItem(body).ok }.getOrDefault(false)
            feedback(if (ok) "已创建" else "创建失败，检查服务地址")
            if (ok) refreshItems()
            onDone(ok)
        }
    }

    fun updateItem(id: Int, body: ItemBody, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { ApiClient.api.updateItem(id, body).ok }.getOrDefault(false)
            feedback(if (ok) "已保存" else "保存失败，检查服务地址")
            if (ok) refreshItems()
            onDone(ok)
        }
    }

    fun updateItemStatus(id: Int, status: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val resp = runCatching { ApiClient.api.updateItemStatus(id, StatusRequest(status)) }.getOrNull()
            val updated = resp?.data?.toModel()
            if (resp?.ok == true && updated != null) {
                // 原地更新，列表和详情都能立刻反映新状态
                items = items.map { if (it.id == updated.id) updated else it }
                loadItemStatusCounts()
                feedback("状态已更新为「${updated.statusText}」")
                onDone(true)
            } else {
                feedback(resp?.message?.ifBlank { "状态更新失败" } ?: "状态更新失败")
                onDone(false)
            }
        }
    }

    fun deleteItem(id: Int, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { ApiClient.api.deleteItem(id).ok }.getOrDefault(false)
            if (ok) {
                items = items.filterNot { it.id == id }
                feedback("已删除")
            } else {
                feedback("删除失败，检查服务地址")
            }
            onDone(ok)
        }
    }

    /** 图片上传：编辑页选图后回传相对 URL */
    fun uploadImage(bytes: ByteArray, filename: String, mime: String = "image/jpeg", onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val url = runCatching { ApiClient.uploadImage(bytes, filename, mime) }.getOrNull()
            if (url == null) feedback("图片没传上去，检查服务地址")
            onDone(url)
        }
    }

    /* ------------------------------------------------------------ 更新 */

    /** 检查更新；silent=true 时不提示「已是最新」 */
    fun checkUpdate(silent: Boolean = false) {
        checkingUpdate = true
        viewModelScope.launch {
            runCatching { ApiClient.api.checkUpdate(BuildConfig.VERSION_CODE) }
                .onSuccess { resp ->
                    val release = resp.data?.release
                    updateInfo = release?.let {
                        UpdateInfo(
                            hasUpdate = true,
                            versionCode = it.versionCode,
                            versionName = it.versionName,
                            changelog = it.changelog,
                            downloadUrl = it.downloadUrl,
                            forceUpdate = it.forceUpdate
                        )
                    }
                    val info = updateInfo
                    if (info != null) feedback("发现新版本 ${info.versionName}")
                    else if (!silent) feedback("已经是最新版本啦")
                }
                .onFailure {
                    if (!silent) feedback("检查更新失败，看看服务地址")
                }
            checkingUpdate = false
        }
    }

    /** 下载更新包，下完直接拉起系统安装器 */
    fun downloadAndInstallUpdate() {
        val info = updateInfo ?: return
        if (updateDownloading) return
        updateDownloading = true
        updateProgress = 0f
        viewModelScope.launch {
            val app = getApplication<Application>()
            runCatching {
                val target = File(File(app.cacheDir, "update").apply { mkdirs() }, "keel-${info.versionName}.apk")
                var lastPercent = -1
                ApiClient.download(info.downloadUrl, target) { read, total ->
                    if (total > 0) {
                        val percent = (read * 100 / total).toInt()
                        if (percent != lastPercent) {
                            lastPercent = percent
                            // 回调在 IO 线程，切回主线程再更新 Compose 状态
                            launch(Dispatchers.Main.immediate) { updateProgress = (percent / 100f).coerceIn(0f, 1f) }
                        }
                    }
                }
                target
            }.onSuccess { file ->
                updateDownloading = false
                updateProgress = 1f
                installApk(file)
            }.onFailure {
                updateDownloading = false
                updateProgress = 0f
                feedback("更新包下载失败：${it.message ?: "请稍后再试"}")
            }
        }
    }

    /** 把 apk 交给系统安装器；Android 8+ 先引导「安装未知应用」权限 */
    private fun installApk(file: File) {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            feedback("请先允许「安装未知应用」，再点一次更新")
            runCatching {
                app.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }
        runCatching {
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            app.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure { feedback("拉起安装失败：${it.message ?: "请手动安装"}") }
    }

    /**
     * 统一的网络调用壳。
     *
     * 注意：Retrofit 的 suspend 函数内部已经是异步的，这里**不能**再包 withContext(IO)，
     * 否则 block 里的 Compose 状态读写会跨线程，直接抛异常被静默吞掉。
     */
    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch {
            syncing = true
            runCatching { block() }
                .onSuccess { online = true }
                .onFailure { err ->
                    online = false
                    if (feedbackMessage == null) feedbackMessage = "没连上服务：${err.message ?: "网络异常"}"
                }
            syncing = false
        }
    }

    fun refresh() = loadAll()

    /* ------------------------------------------------------------ 偏好 */

    fun switchRole() {
        role = if (role == Role.CLIENT) Role.ADMIN else Role.CLIENT
        prefs.edit().putString("role", role.name).apply()
        RealtimeClient.disconnect()
        connectRealtime()
        loadAll()
    }

    fun updateDarkMode(value: Boolean) {
        darkMode = value
        prefs.edit().putBoolean("dark", value).apply()
    }

    /** 消息通知开关：关掉后新消息不再弹顶部横幅（消息列表照常刷新） */
    fun updateNotificationsEnabled(value: Boolean) {
        notificationsEnabled = value
        prefs.edit().putBoolean("notifications", value).apply()
    }

    fun updateApiBaseUrl(value: String) {
        apiBaseUrl = value
        prefs.edit().putString("api", value).apply()
        ApiClient.updateHost(value)
        realtimeConnected = false
        RealtimeClient.disconnect()
        connectRealtime()
        loadAll()
    }

    fun feedback(message: String) {
        feedbackMessage = message
    }

    fun clearFeedback() {
        feedbackMessage = null
    }
}
