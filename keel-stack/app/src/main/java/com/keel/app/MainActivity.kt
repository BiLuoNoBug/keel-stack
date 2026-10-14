package com.keel.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keel.app.data.AppNotice
import com.keel.app.data.Role
import com.keel.app.ui.components.AmbientSurface
import com.keel.app.ui.components.keelPressable
import com.keel.app.ui.home.HomeScreen
import com.keel.app.ui.items.ItemDetailScreen
import com.keel.app.ui.items.ItemEditScreen
import com.keel.app.ui.items.ItemListScreen
import com.keel.app.ui.profile.NoticeScreen
import com.keel.app.ui.profile.ProfileScreen
import com.keel.app.ui.settings.AboutScreen
import com.keel.app.ui.settings.ApiConfigScreen
import com.keel.app.ui.settings.SettingsScreen
import com.keel.app.ui.theme.KeelTheme
import com.keel.app.ui.theme.LocalKeelPalette
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        setContent {
            val vm: AppViewModel = viewModel()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !vm.darkMode
                    isAppearanceLightNavigationBars = !vm.darkMode
                }
            }
            KeelTheme(vm.darkMode, vm.role == Role.ADMIN) {
                AnimatedContent(
                    targetState = vm.role,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = .96f)) togetherWith (fadeOut() + scaleOut(targetScale = .96f)) },
                    label = "roleSwitch"
                ) { role -> KeelNavigation(vm, role) }
            }
        }
    }
}

private object Routes {
    const val CLIENT_HOME = "client/home"
    const val CLIENT_LIST = "client/items"
    const val CLIENT_PROFILE = "client/profile"
    const val ADMIN_HOME = "admin/home"
    const val ADMIN_LIST = "admin/items"
    const val ADMIN_PROFILE = "admin/profile"
    const val ITEM_DETAIL = "item/{id}"
    const val ITEM_NEW = "item/new"
    const val ITEM_EDIT = "item/{id}/edit"
    const val NOTICES = "notices"
    const val SETTINGS = "settings/main"
    const val API = "settings/api"
    const val ABOUT = "settings/about"
}

@Composable
private fun KeelNavigation(vm: AppViewModel, role: Role) {
    val nav = rememberNavController()
    val route by nav.currentBackStackEntryAsState()
    val current = route?.destination?.route
    val clientRoots = setOf(Routes.CLIENT_HOME, Routes.CLIENT_LIST, Routes.CLIENT_PROFILE)
    val adminRoots = setOf(Routes.ADMIN_HOME, Routes.ADMIN_LIST, Routes.ADMIN_PROFILE)
    val showNav = current in clientRoots || current in adminRoots

    AmbientSurface(role == Role.ADMIN) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = if (role == Role.CLIENT) Routes.CLIENT_HOME else Routes.ADMIN_HOME
            ) {
                // —— 根页：双角色各 3 个（首页 / 列表 / 我的），共用同一套页面
                composable(Routes.CLIENT_HOME) { HomeScreen(vm, { nav.navigateRoot(Routes.CLIENT_LIST) }, { nav.navigate(Routes.NOTICES) }) }
                composable(Routes.ADMIN_HOME) { HomeScreen(vm, { nav.navigateRoot(Routes.ADMIN_LIST) }, { nav.navigate(Routes.NOTICES) }) }
                composable(Routes.CLIENT_LIST) { ItemListScreen(vm, { nav.navigate("item/$it") }, { nav.navigate(Routes.ITEM_NEW) }) }
                composable(Routes.ADMIN_LIST) { ItemListScreen(vm, { nav.navigate("item/$it") }, { nav.navigate(Routes.ITEM_NEW) }) }
                composable(Routes.CLIENT_PROFILE) { ProfileScreen(vm, { nav.navigate(Routes.SETTINGS) }, { nav.navigate(Routes.NOTICES) }) }
                composable(Routes.ADMIN_PROFILE) { ProfileScreen(vm, { nav.navigate(Routes.SETTINGS) }, { nav.navigate(Routes.NOTICES) }) }

                // —— 二级页：条目详情 / 编辑、设置、服务地址、关于、消息
                composable(Routes.ITEM_DETAIL) { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
                    ItemDetailScreen(vm, id, nav::popBackStack) { nav.navigate("item/$id/edit") }
                }
                composable(Routes.ITEM_NEW) { ItemEditScreen(vm, null, nav::popBackStack) }
                composable(Routes.ITEM_EDIT) { entry ->
                    ItemEditScreen(vm, entry.arguments?.getString("id")?.toIntOrNull(), nav::popBackStack)
                }
                composable(Routes.NOTICES) { NoticeScreen(vm, nav::popBackStack) { link -> nav.navigate("item/$link") } }
                composable(Routes.SETTINGS) { SettingsScreen(vm, nav::popBackStack, { nav.navigate(Routes.API) }, { nav.navigate(Routes.ABOUT) }) }
                composable(Routes.API) { ApiConfigScreen(vm, nav::popBackStack) }
                composable(Routes.ABOUT) { AboutScreen(vm, nav::popBackStack) }
            }

            if (showNav) FloatingBottomNav(role, current) { nav.navigateRoot(it) }

            // 顶部通知横幅：仿 iOS 通知样式，从顶部滑下来，带震动。
            // 消息里的 link 指向条目时，点一下直接进对应条目详情。
            NoticeBanner(
                notice = vm.bannerNotice,
                onDismiss = { vm.dismissBanner() },
                onOpen = { data ->
                    vm.dismissBanner()
                    if (data.link.isBlank()) nav.navigate(Routes.NOTICES) else nav.navigate("item/${data.link}")
                }
            )

            val feedback = vm.feedbackMessage
            LaunchedEffect(feedback) { if (feedback != null) { delay(2200); vm.clearFeedback() } }
            AnimatedVisibility(
                visible = feedback != null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (showNav) 112.dp else 28.dp),
                enter = slideInVertically { it / 2 } + fadeIn(), exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                Surface(shape = CircleShape, color = Color(0xE62A2831), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.22f)), shadowElevation = 16.dp) {
                    Text(feedback.orEmpty(), Modifier.padding(horizontal = 18.dp, vertical = 11.dp), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * 底部导航切根页面。
 *
 * 先 popBackStack 退回目标根页（顺带把压在上面的二级页清掉），
 * 这样「列表 → 首页」这种切换一定生效；目标不在栈里时才新建。
 */
private fun NavHostController.navigateRoot(route: String) {
    if (currentDestination?.route == route) return
    if (!popBackStack(route, inclusive = false)) {
        navigate(route) { launchSingleTop = true }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

/** 收到推送时的震动：重要消息双震，普通消息短震一下 */
private fun vibrateForNotice(context: Context, important: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    val pattern = if (important) longArrayOf(0, 110, 70, 110) else longArrayOf(0, 90)
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}

/**
 * 顶部通知横幅 —— 仿 iOS 通知样式：
 * 毛玻璃圆角卡片从屏幕顶部滑入，停留几秒后自动滑走，也可手动关闭或点开查看。
 */
@Composable
private fun NoticeBanner(notice: AppNotice?, onDismiss: () -> Unit, onOpen: (AppNotice) -> Unit) {
    // 退出动画期间 notice 已经变回 null，先把最后一条内容留住
    var lastNotice by remember { mutableStateOf<AppNotice?>(null) }
    if (notice != null) lastNotice = notice

    val context = LocalContext.current
    LaunchedEffect(notice?.id) {
        if (notice != null) {
            // 收到推送时震一下：重要消息双震，普通消息短震
            vibrateForNotice(context, notice.important)
            delay(6000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = notice != null,
        modifier = Modifier.fillMaxWidth(),
        enter = slideInVertically(spring(dampingRatio = 0.72f, stiffness = 320f)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(260)) { -it } + fadeOut()
    ) {
        val data = lastNotice ?: return@AnimatedVisibility
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val panel = if (dark) Color(0xF21E1E28) else Color(0xF4FFFFFF)
        val ink = if (dark) Color.White else Color(0xFF171725)
        val sub = if (dark) Color.White.copy(.60f) else Color(0xFF171725).copy(.52f)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(20.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black.copy(.18f), spotColor = Color.Black.copy(.38f))
                .clip(RoundedCornerShape(26.dp))
                .keelPressable { onOpen(data) },
            shape = RoundedCornerShape(26.dp),
            color = panel,
            border = androidx.compose.foundation.BorderStroke(0.75.dp, if (dark) Color.White.copy(.14f) else Color.White.copy(.85f))
        ) {
            Column {
                Row(
                    Modifier.padding(start = 13.dp, top = 12.dp, end = 10.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFF8A58), Color(0xFFF4531E)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(21.dp))
                    }

                    Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Keel", style = MaterialTheme.typography.labelSmall, color = sub, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(6.dp))
                            Text(data.date.ifBlank { "刚刚" }, style = MaterialTheme.typography.labelSmall, color = sub)
                            if (data.important) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    Modifier.clip(CircleShape).background(Color(0xFFE65353))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) { Text("重要", color = Color.White, fontSize = 9.sp) }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            data.title, style = MaterialTheme.typography.titleMedium, color = ink,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            data.content, style = MaterialTheme.typography.bodySmall, color = sub,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(ink.copy(.07f)).keelPressable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = sub, modifier = Modifier.size(14.dp))
                    }
                }

                // 底部一条品牌色渐隐线
                Box(
                    Modifier.fillMaxWidth().height(2.dp)
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF6B35), Color(0xFFFF6B35).copy(.18f), Color.Transparent)))
                )
            }
        }
    }
}

@Composable
private fun FloatingBottomNav(role: Role, route: String?, onNavigate: (String) -> Unit) {
    val items = if (role == Role.CLIENT) listOf(
        NavItem(Routes.CLIENT_HOME, "首页", Icons.Rounded.Home),
        NavItem(Routes.CLIENT_LIST, "列表", Icons.Rounded.List),
        NavItem(Routes.CLIENT_PROFILE, "我的", Icons.Rounded.Person)
    ) else listOf(
        NavItem(Routes.ADMIN_HOME, "首页", Icons.Rounded.Home),
        NavItem(Routes.ADMIN_LIST, "列表", Icons.Rounded.List),
        NavItem(Routes.ADMIN_PROFILE, "我的", Icons.Rounded.Person)
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = LocalKeelPalette.current.glassStrong,
            border = androidx.compose.foundation.BorderStroke(1.dp, LocalKeelPalette.current.glassBorder), shadowElevation = 20.dp
        ) {
            Row(Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceAround) {
                items.forEach { item ->
                    val selected = route == item.route
                    val scale by animateFloatAsState(if (selected) 1.08f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "bottomNav")
                    Column(Modifier.weight(1f).keelPressable { onNavigate(item.route) }.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = if (selected) MaterialTheme.colorScheme.primary.copy(.13f) else Color.Transparent, modifier = Modifier.width(48.dp)) { Box(Modifier.padding(vertical = 5.dp), contentAlignment = Alignment.Center) { Icon(item.icon, item.label, tint = if (selected) MaterialTheme.colorScheme.primary else LocalKeelPalette.current.muted.copy(.65f), modifier = Modifier.size(23.dp).scale(scale)) } }
                        Text(item.label, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.primary else LocalKeelPalette.current.muted.copy(.7f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
