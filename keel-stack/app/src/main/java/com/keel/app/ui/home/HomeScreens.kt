package com.keel.app.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keel.app.AppViewModel
import com.keel.app.data.Role
import com.keel.app.data.remote.StatusCountDto
import com.keel.app.ui.components.GlassIconButton
import com.keel.app.ui.components.GlassPanel
import com.keel.app.ui.components.SectionTitle
import com.keel.app.ui.components.SettingItem
import com.keel.app.ui.components.keelPressable
import com.keel.app.ui.theme.LocalKeelPalette

/**
 * 两种角色的首页共用同一个骨架：欢迎语与快捷入口按角色换文案，
 * 数据（状态计数）来自同一份服务端接口。
 */
@Composable
fun HomeScreen(vm: AppViewModel, onOpenList: () -> Unit, onOpenNotices: () -> Unit) {
    val admin = vm.role == Role.ADMIN
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                Modifier.statusBarsPadding().padding(start = 20.dp, end = 14.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary.copy(.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (admin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(if (admin) "管理端" else "客户端", style = MaterialTheme.typography.labelSmall, color = LocalKeelPalette.current.muted)
                    Text("欢迎回来", style = MaterialTheme.typography.titleMedium)
                }
                GlassIconButton(Icons.Rounded.NotificationsNone, "消息", onOpenNotices, badge = vm.hasUnreadNotices)
            }
        }
        item { HomeHero(onOpenList) }
        item { Column(Modifier.padding(horizontal = 20.dp)) { SectionTitle("状态概览", "来自服务端的实时计数") } }
        item { HomeStatusRow(vm) }
        item { Column(Modifier.padding(horizontal = 20.dp)) { SectionTitle("快捷入口") } }
        item {
            GlassPanel(Modifier.padding(horizontal = 20.dp), strong = true) {
                SettingItem(Icons.Rounded.List, "条目列表", "浏览、筛选、编辑全部条目", onOpenList)
                Divider()
                SettingItem(
                    Icons.Rounded.NotificationsNone, "消息",
                    if (vm.notices.isEmpty()) "服务端的通知会出现在这里" else "最近 ${vm.notices.size} 条",
                    onOpenNotices
                )
            }
        }
    }
}

@Composable
private fun HomeHero(onClick: () -> Unit) {
    Box(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(186.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
    ) {
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Keel 工程骨架", color = Color.White.copy(.85f), style = MaterialTheme.typography.labelMedium)
            Column {
                Text("让每一条都清晰", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Text("网络 / 实时 / 图片 / 主题 / 双角色导航，全部就位", color = Color.White.copy(.85f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(CircleShape).background(Color.White.copy(.22f)).keelPressable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("打开条目列表", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeStatusRow(vm: AppViewModel) {
    val counts = vm.itemStatusCounts.ifEmpty {
        listOf(
            StatusCountDto("TODO", "待处理", 0),
            StatusCountDto("DOING", "进行中", 0),
            StatusCountDto("DONE", "已完成", 0)
        )
    }
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        counts.forEach { HomeMetric("${it.count}", it.statusText, Modifier.weight(1f)) }
    }
}

@Composable
private fun HomeMetric(value: String, label: String, modifier: Modifier) {
    GlassPanel(modifier, strong = true, padding = 14) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
