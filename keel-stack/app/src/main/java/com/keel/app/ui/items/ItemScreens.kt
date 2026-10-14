package com.keel.app.ui.items

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keel.app.AppViewModel
import com.keel.app.data.Item
import com.keel.app.data.remote.ItemBody
import com.keel.app.data.remote.ItemPriority
import com.keel.app.data.remote.ItemStatus
import com.keel.app.ui.components.KeelAlertDialog
import com.keel.app.ui.components.KeelChip
import com.keel.app.ui.components.KeelSwitch
import com.keel.app.ui.components.GlassIconButton
import com.keel.app.ui.components.GlassPanel
import com.keel.app.ui.components.GlassTextField
import com.keel.app.ui.components.LocalPhoto
import com.keel.app.ui.components.PageTopBar
import com.keel.app.ui.components.PrimaryButton
import com.keel.app.ui.components.SecondaryButton
import com.keel.app.ui.components.SectionTitle
import com.keel.app.ui.components.StatusPill
import com.keel.app.ui.components.keelPressable
import com.keel.app.ui.theme.LocalKeelPalette
import com.keel.app.ui.theme.Success

/** 状态 / 优先级取色，固定色值，不随主题变化，保证两端一致 */
private val statusColors = mapOf("TODO" to Color(0xFF8E8E9A), "DOING" to Color(0xFFE6A23C), "DONE" to Color(0xFF27AE85))
private val priorityColors = mapOf("LOW" to Color(0xFF33BFB5), "NORMAL" to Color(0xFFE6A23C), "HIGH" to Color(0xFFE65353))
private fun statusColor(status: String) = statusColors[status] ?: Color(0xFF8E8E9A)
private fun priorityColor(priority: String) = priorityColors[priority] ?: Color(0xFFE6A23C)

private val statusFilter = listOf<Pair<String?, String>>(
    null to "全部状态", "TODO" to "待处理", "DOING" to "进行中", "DONE" to "已完成"
)

/* ============================================================ 列表页（根页） */

@Composable
fun ItemListScreen(vm: AppViewModel, onOpenItem: (Int) -> Unit, onNew: () -> Unit) {
    var keyword by remember { mutableStateOf(vm.filterKeyword) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageTopBar(
            "条目列表",
            onBack = null,
            actions = {
                GlassIconButton(Icons.Rounded.Refresh, "刷新", { vm.refreshItems() })
                GlassIconButton(Icons.Rounded.Add, "新建条目", onNew)
            }
        )

        // 状态概览：服务端按状态计数
        if (vm.itemStatusCounts.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vm.itemStatusCounts.forEach { count ->
                    KeelChip("${count.statusText} ${count.count}", false, onClick = { vm.applyItemFilters(vm.filterCategory, count.status, vm.filterKeyword) })
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // 分类筛选
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeelChip("全部分类", vm.filterCategory == null, { vm.applyItemFilters(null, vm.filterStatus, vm.filterKeyword) })
            vm.itemCategories.forEach { category ->
                KeelChip(category, vm.filterCategory == category, { vm.applyItemFilters(category, vm.filterStatus, vm.filterKeyword) })
            }
        }
        Spacer(Modifier.height(8.dp))

        // 状态筛选
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusFilter.forEach { (value, label) ->
                KeelChip(label, vm.filterStatus == value, { vm.applyItemFilters(vm.filterCategory, value, vm.filterKeyword) })
            }
        }
        Spacer(Modifier.height(12.dp))

        // 关键字搜索
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassTextField(keyword, { keyword = it }, "搜索标题/内容", modifier = Modifier.weight(1f), leadingIcon = Icons.Rounded.Search)
            KeelChip("搜索", false) { vm.applyItemFilters(vm.filterCategory, vm.filterStatus, keyword) }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (vm.items.isEmpty()) {
                item { ItemEmpty("还没有条目", "点右上角 + 新建一个，或用筛选换个条件") }
            } else {
                item { Text("共 ${vm.itemsTotal} 条，当前显示 ${vm.items.size} 条", style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted) }
                items(vm.items, key = { it.id }) { item -> ItemCard(item) { onOpenItem(item.id) } }
                if (vm.itemsHasMore) {
                    item { SecondaryButton("加载更多", { vm.loadMoreItems() }, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(item: Item, onClick: () -> Unit) {
    GlassPanel(onClick = onClick, strong = true, radius = 20, padding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LocalPhoto(item.coverUrl, item.title, Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.size(8.dp))
                    StatusPill(item.statusText, statusColor(item.status))
                }
                Spacer(Modifier.height(3.dp))
                Text(item.category, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
                if (item.content.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(item.content, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("优先级 ${ItemPriority.of(item.priority)}", priorityColor(item.priority))
                    if (!item.published) StatusPill("未发布", Color(0xFF8E8E9A))
                }
            }
        }
    }
}

/* ============================================================ 详情页 */

@Composable
fun ItemDetailScreen(vm: AppViewModel, id: Int, onBack: () -> Unit, onEdit: () -> Unit) {
    var item by remember { mutableStateOf(vm.items.firstOrNull { it.id == id }) }
    var confirmDelete by remember { mutableStateOf(false) }

    // 先用列表里的数据顶上，再拉一次最新；之后跟随列表的实时更新联动
    LaunchedEffect(id) { vm.fetchItem(id) { item = it ?: item } }
    LaunchedEffect(vm.items) { vm.items.firstOrNull { it.id == id }?.let { item = it } }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageTopBar(
            item?.title ?: "条目详情",
            onBack = onBack,
            actions = { if (item != null) GlassIconButton(Icons.Rounded.Edit, "编辑", onEdit) }
        )
        val current = item
        if (current == null) {
            ItemEmpty("正在加载…", "如果一直转圈，检查一下服务地址")
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 30.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { LocalPhoto(current.coverUrl, current.title, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(current.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        StatusPill(current.statusText, statusColor(current.status))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(current.category, MaterialTheme.colorScheme.primary)
                        StatusPill("优先级 ${ItemPriority.of(current.priority)}", priorityColor(current.priority))
                        StatusPill(if (current.published) "已发布" else "未发布", if (current.published) Success else Color(0xFF8E8E9A))
                    }
                }
                item { SectionTitle("内容") }
                item {
                    GlassPanel(strong = true) {
                        Text(current.content.ifBlank { "（没有填写内容）" }, style = MaterialTheme.typography.bodyLarge, color = LocalKeelPalette.current.muted)
                    }
                }
                item { SectionTitle("状态流转", "根据服务端状态机给出的可推进状态") }
                item {
                    if (current.allowNext.isEmpty()) {
                        GlassPanel(strong = true) { Text("当前状态没有可推进的下一步", style = MaterialTheme.typography.bodyMedium, color = LocalKeelPalette.current.muted) }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            current.allowNext.forEach { next ->
                                SecondaryButton("推进到「${ItemStatus.of(next)}」", { vm.updateItemStatus(id, next) }, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                item { SecondaryButton("删除条目", { confirmDelete = true }, Modifier.fillMaxWidth(), danger = true) }
            }
        }
    }

    if (confirmDelete) KeelAlertDialog(
        "删除这个条目？",
        "删除后不可恢复。",
        "删除",
        onConfirm = { confirmDelete = false; vm.deleteItem(id) { if (it) onBack() } },
        onDismiss = { confirmDelete = false },
        danger = true,
        icon = Icons.Rounded.Delete
    )
}

/* ============================================================ 编辑页（新建 / 编辑） */

@Composable
fun ItemEditScreen(vm: AppViewModel, id: Int?, onBack: () -> Unit) {
    val context = LocalContext.current
    val isEdit = id != null

    var loaded by remember { mutableStateOf(!isEdit) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("默认分类") }
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("NORMAL") }
    var published by remember { mutableStateOf(true) }
    var sortOrder by remember { mutableStateOf("0") }
    var coverUrl by remember { mutableStateOf<String?>(null) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null) {
            vm.fetchItem(id) { item ->
                if (item != null) {
                    title = item.title
                    category = item.category
                    content = item.content
                    priority = item.priority
                    published = item.published
                    sortOrder = item.sortOrder.toString()
                    coverUrl = item.coverUrl
                }
                loaded = true
            }
        }
    }

    // 相册选图 -> 读字节 -> 上传。返回的是服务端相对地址，存进 coverUrl
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedUri = uri
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes != null) {
                uploading = true
                vm.uploadImage(bytes, "item_${System.currentTimeMillis()}.jpg") { url ->
                    uploading = false
                    if (url != null) coverUrl = url
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageTopBar(if (isEdit) "编辑条目" else "新建条目", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionTitle("封面", "选图后自动上传")
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.size(120.dp).clip(RoundedCornerShape(20.dp)).keelPressable { picker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    LocalPhoto(pickedUri ?: coverUrl, "封面", Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Image, "选择封面", tint = Color.White)
                    }
                }
                if (uploading) {
                    Spacer(Modifier.height(6.dp))
                    Text("封面上传中…", style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
                }
            }
            item { GlassTextField(title, { title = it }, "标题") }
            item { GlassTextField(category, { category = it }, "分类", leadingIcon = Icons.Rounded.Category) }
            item { GlassTextField(content, { content = it }, "内容", singleLine = false) }
            item {
                Column {
                    Text("优先级", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOW" to "低", "NORMAL" to "普通", "HIGH" to "高").forEach { (value, label) ->
                            KeelChip(label, priority == value, { priority = value })
                        }
                    }
                }
            }
            item {
                GlassPanel(strong = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("立即发布", style = MaterialTheme.typography.titleMedium)
                            Text("关闭则保存为未发布", style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
                        }
                        KeelSwitch(published) { published = it }
                    }
                }
            }
            item { GlassTextField(sortOrder, { sortOrder = it.filter { c -> c.isDigit() } }, "排序（越小越前）", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        }

        GlassPanel(Modifier.navigationBarsPadding().padding(14.dp), strong = true, radius = 30, padding = 8) {
            PrimaryButton(
                if (isEdit) "保存" else "创建",
                {
                    val body = ItemBody(
                        title = title.trim(),
                        category = category.trim().ifBlank { "默认分类" },
                        content = content.trim(),
                        coverUrl = coverUrl,
                        priority = priority,
                        published = published,
                        sortOrder = sortOrder.toIntOrNull() ?: 0
                    )
                    // 注意用 id != null 判断：isEdit 只是个布尔值，编译器不会据此对 id 做 smart cast
                    if (id != null) vm.updateItem(id, body) { if (it) onBack() } else vm.createItem(body) { if (it) onBack() }
                },
                Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && loaded && !uploading
            )
        }
    }
}

@Composable
private fun ItemEmpty(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(88.dp).background(MaterialTheme.colorScheme.primary.copy(.09f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Category, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = LocalKeelPalette.current.muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
