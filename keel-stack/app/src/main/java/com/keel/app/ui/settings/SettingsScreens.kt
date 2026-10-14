package com.keel.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keel.app.AppViewModel
import com.keel.app.BuildConfig
import com.keel.app.data.Role
import com.keel.app.ui.components.KeelAlertDialog
import com.keel.app.ui.components.KeelChip
import com.keel.app.ui.components.KeelSwitch
import com.keel.app.ui.components.GlassPanel
import com.keel.app.ui.components.GlassTextField
import com.keel.app.ui.components.PageTopBar
import com.keel.app.ui.components.PrimaryButton
import com.keel.app.ui.components.SectionTitle
import com.keel.app.ui.components.SettingItem
import com.keel.app.ui.components.StatusPill
import com.keel.app.ui.theme.LocalKeelPalette

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onApi: () -> Unit, onAbout: () -> Unit) {
    var roleConfirm by remember { mutableStateOf(false) }
    var clearCache by remember { mutableStateOf(false) }
    val admin = vm.role == Role.ADMIN

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageTopBar("设置", onBack)
        LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                GlassPanel(onClick = { roleConfirm = true }, strong = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(58.dp).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(if (admin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text("当前模式", style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
                            Text(if (admin) "管理端" else "客户端", style = MaterialTheme.typography.headlineSmall)
                            Text("配色与底部导航随角色变化", style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
                        }
                        StatusPill("切换角色", MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { SectionTitle("系统设置") }
            item {
                GlassPanel(strong = true) {
                    SettingItem(Icons.Rounded.Notifications, "消息通知", "收到站内消息时弹顶部横幅", { vm.updateNotificationsEnabled(!vm.notificationsEnabled); vm.feedback(if (vm.notificationsEnabled) "消息通知已开启" else "消息通知已关闭") }) {
                        KeelSwitch(vm.notificationsEnabled) { vm.updateNotificationsEnabled(it); vm.feedback(if (it) "消息通知已开启" else "消息通知已关闭") }
                    }
                    Divider()
                    SettingItem(Icons.Rounded.DarkMode, "深色模式", if (vm.darkMode) "已开启" else "已关闭", { vm.updateDarkMode(!vm.darkMode) }) { KeelSwitch(vm.darkMode, vm::updateDarkMode) }
                    Divider()
                    SettingItem(Icons.Rounded.Api, "服务地址", "指向 keel-stack/server", onApi)
                }
            }
            item { SectionTitle("关于") }
            item {
                GlassPanel(strong = true) {
                    SettingItem(Icons.Rounded.Info, "关于 Keel", "骨架版 ${BuildConfig.VERSION_NAME}", onAbout)
                    Divider()
                    SettingItem(Icons.Rounded.CleaningServices, "清理图片缓存", "不影响服务端数据", { clearCache = true })
                }
            }
        }
    }

    if (roleConfirm) KeelAlertDialog(
        "切换到${if (admin) "客户端" else "管理端"}？",
        "配色、底部导航会随角色变化，数据仍是同一份。",
        "切换",
        onConfirm = { roleConfirm = false; vm.switchRole(); vm.feedback("模式已切换") },
        onDismiss = { roleConfirm = false },
        icon = if (admin) Icons.Rounded.Person else Icons.Rounded.AdminPanelSettings
    )
    if (clearCache) KeelAlertDialog(
        "清理图片缓存？",
        "不会影响服务端数据和其他设置。",
        "清理",
        onConfirm = { clearCache = false; vm.feedback("图片缓存已清理") },
        onDismiss = { clearCache = false },
        icon = Icons.Rounded.CleaningServices
    )
}

@Composable
fun ApiConfigScreen(vm: AppViewModel, onBack: () -> Unit) {
    var baseUrl by remember { mutableStateOf(vm.apiBaseUrl) }
    val valid = baseUrl.startsWith("http://") || baseUrl.startsWith("https://")

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageTopBar("服务地址", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { SectionTitle("服务地址", "只有连接你自己的 server 时才需要修改") }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("本机 8080" to "http://127.0.0.1:8080", "模拟器 10.0.2.2" to "http://10.0.2.2:8080").forEach { (label, url) ->
                        KeelChip(label, baseUrl == url, { baseUrl = url })
                    }
                }
            }
            item {
                GlassPanel(strong = true) {
                    GlassTextField(
                        baseUrl, { baseUrl = it }, "基础 URL",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        error = if (baseUrl.isNotBlank() && !valid) "请输入有效的 http(s) 地址" else null
                    )
                }
            }
            item {
                Text(
                    "默认 ${BuildConfig.DEFAULT_API_HOST}；真机连本机可先执行 adb reverse tcp:8080 tcp:8080。",
                    style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted
                )
            }
        }
        GlassPanel(Modifier.navigationBarsPadding().padding(14.dp), strong = true, radius = 30, padding = 8) {
            PrimaryButton("保存", { vm.updateApiBaseUrl(baseUrl); onBack() }, Modifier.fillMaxWidth(), enabled = valid)
        }
    }
}

@Composable
fun AboutScreen(vm: AppViewModel, onBack: () -> Unit) {
    var showLicense by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageTopBar("关于 Keel", onBack)
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.size(94.dp).background(Brush.linearGradient(listOf(Color(0xFFFF8A58), Color(0xFFFF5E45))), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Apps, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            Spacer(Modifier.height(18.dp))
            Text("Keel", style = MaterialTheme.typography.headlineLarge)
            Text("三端工程骨架 · App 端", color = LocalKeelPalette.current.muted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(30.dp))
            GlassPanel(strong = true) {
                SettingItem(Icons.Rounded.Info, "版本", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", { vm.feedback("Keel 骨架版 ${BuildConfig.VERSION_NAME}") })
                Divider()
                SettingItem(
                    Icons.Rounded.Cached, "检查更新",
                    when {
                        vm.checkingUpdate -> "正在检查…"
                        vm.updateDownloading -> "正在下载 ${(vm.updateProgress * 100).toInt()}%"
                        vm.updateInfo != null -> "发现新版本 ${vm.updateInfo?.versionName} · 点此更新"
                        else -> "已是最新版本"
                    },
                    { if (vm.updateInfo != null) vm.downloadAndInstallUpdate() else vm.checkUpdate() }
                )
                Divider()
                SettingItem(Icons.Rounded.Description, "开源许可", "第三方软件声明", { showLicense = true })
            }
            vm.updateInfo?.let { info ->
                Spacer(Modifier.height(14.dp))
                GlassPanel(strong = true, modifier = Modifier.fillMaxWidth()) {
                    Text("发现新版本 ${info.versionName}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(info.changelog, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
                    Spacer(Modifier.height(12.dp))
                    if (vm.updateDownloading) {
                        LinearProgressIndicator(progress = { vm.updateProgress }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("下载中 ${(vm.updateProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
                    } else {
                        PrimaryButton("立即更新", { vm.downloadAndInstallUpdate() }, Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("© 2026 Keel · App 端骨架", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
        }
    }
    if (showLicense) KeelAlertDialog(
        "开源许可",
        "本应用使用 Jetpack Compose、AndroidX Navigation、Retrofit、OkHttp、Coil、socket.io-client 等开源组件，遵循各自许可协议。",
        "知道了", "关闭",
        { showLicense = false }, { showLicense = false },
        icon = Icons.Rounded.Description
    )
}
