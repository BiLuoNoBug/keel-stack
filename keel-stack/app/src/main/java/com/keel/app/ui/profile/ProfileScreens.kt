package com.keel.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keel.app.AppViewModel
import com.keel.app.data.AppNotice
import com.keel.app.data.Role
import com.keel.app.ui.components.KeelAlertDialog
import com.keel.app.ui.components.GlassPanel
import com.keel.app.ui.components.PageTopBar
import com.keel.app.ui.components.SettingItem
import com.keel.app.ui.components.StatusPill
import com.keel.app.ui.theme.LocalKeelPalette

@Composable
fun ProfileScreen(vm: AppViewModel, onSettings: () -> Unit, onNotices: () -> Unit) {
    var roleConfirm by remember { mutableStateOf(false) }
    val admin = vm.role == Role.ADMIN
    val todoCount = vm.itemStatusCounts.firstOrNull { it.status == "TODO" }?.count ?: 0

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Box(Modifier.statusBarsPadding().fillMaxWidth().height(150.dp).padding(horizontal = 20.dp).clip(RoundedCornerShape(30.dp))) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))))
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(if (admin) "管理端" else "客户端", color = Color.White.copy(.82f), style = MaterialTheme.typography.bodyMedium)
                    Text("我的", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
        item {
            GlassPanel(Modifier.padding(horizontal = 20.dp), strong = true, onClick = { roleConfirm = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(50.dp).background(MaterialTheme.colorScheme.primary.copy(.12f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(if (admin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp)) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("当前模式：${if (admin) "管理端" else "客户端"}", style = MaterialTheme.typography.titleMedium)
                        Text("点一下可以切换角色，配色和导航会跟着变", style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
                    }
                    StatusPill("切换", MaterialTheme.colorScheme.primary)
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileMetric("${vm.itemsTotal}", "条目总数", Modifier.weight(1f))
                ProfileMetric("$todoCount", "待处理", Modifier.weight(1f))
                ProfileMetric("${vm.notices.size}", "消息", Modifier.weight(1f))
            }
        }
        item {
            GlassPanel(Modifier.padding(horizontal = 20.dp), strong = true) {
                SettingItem(Icons.Rounded.NotificationsNone, "消息", if (vm.notices.isEmpty()) "服务端的通知会出现在这里" else "最近 ${vm.notices.size} 条", onNotices)
                Divider()
                SettingItem(Icons.Rounded.Settings, "应用设置", "切换角色、通知、显示与服务地址", onSettings)
            }
        }
        item {
            Text("Keel 三端工程骨架 · App 端", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
        }
    }

    if (roleConfirm) KeelAlertDialog(
        "切换到${if (admin) "客户端" else "管理端"}？",
        "配色、底部导航会随角色变化，数据仍是同一份。",
        "切换",
        onConfirm = { roleConfirm = false; vm.switchRole(); vm.feedback("已切换到${if (admin) "客户端" else "管理端"}") },
        onDismiss = { roleConfirm = false },
        icon = if (admin) Icons.Rounded.Person else Icons.Rounded.AdminPanelSettings
    )
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier) {
    GlassPanel(modifier, strong = true, padding = 14) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun NoticeScreen(vm: AppViewModel, onBack: () -> Unit, onOpenItem: (String) -> Unit) {
    // 打开消息页就算已读，清掉小红点
    LaunchedEffect(Unit) { vm.markNoticesRead() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageTopBar("消息", onBack)
        if (vm.notices.isEmpty()) {
            NoticeEmpty()
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.notices, key = { it.id }) { notice ->
                    NoticeCard(notice) { if (notice.link.isNotBlank()) onOpenItem(notice.link) }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: AppNotice, onClick: () -> Unit) {
    val tone = if (notice.important) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    GlassPanel(onClick = onClick, strong = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(tone.copy(.13f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (notice.important) Icons.Rounded.Campaign else Icons.Rounded.ChatBubbleOutline, null, tint = tone, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notice.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (notice.important) { Spacer(Modifier.size(6.dp)); StatusPill("重要", tone) }
                }
                Text(notice.date, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(notice.content, style = MaterialTheme.typography.bodyMedium, color = LocalKeelPalette.current.muted)
        if (notice.link.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("点一下查看条目 ${notice.link}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NoticeEmpty() {
    Column(Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(100.dp).background(MaterialTheme.colorScheme.primary.copy(.09f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.NotificationsNone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("还没有消息", style = MaterialTheme.typography.headlineMedium)
        Text("服务端发来通知的时候，消息会出现在这里", color = LocalKeelPalette.current.muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
