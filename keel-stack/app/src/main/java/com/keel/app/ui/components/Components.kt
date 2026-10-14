package com.keel.app.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.keel.app.data.remote.MediaUrl
import com.keel.app.ui.theme.BrandOrangeDeep
import com.keel.app.ui.theme.LocalKeelPalette

@Composable
fun AmbientSurface(admin: Boolean = false, content: @Composable () -> Unit) {
    val p = LocalKeelPalette.current
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(p.ambientTop, p.ambientBottom, MaterialTheme.colorScheme.background))
        )
    ) { content() }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    strong: Boolean = false,
    radius: Int = 24,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        if (pressed) .975f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f), label = "glassPress"
    )
    val p = LocalKeelPalette.current
    val shape = RoundedCornerShape(radius.dp)
    val action = if (onClick == null) Modifier else Modifier.clickable(
        interactionSource = interaction, indication = null, role = Role.Button
    ) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
    Surface(
        modifier = modifier.scale(scale).shadow(if (strong) 18.dp else 8.dp, shape, ambientColor = Color.Black.copy(.10f), spotColor = Color.Black.copy(.14f)).then(action),
        shape = shape,
        color = if (strong) p.glassStrong.copy(alpha = if (strong) .82f else .68f) else p.glass,
        border = BorderStroke(1.dp, p.glassBorder.copy(.9f))
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color.White.copy(.34f), Color.White.copy(.06f), Color.Transparent), start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(650f, 520f)))
                .background(Brush.radialGradient(listOf(Color.White.copy(.28f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(35f, 12f), radius = 420f))
                .border(0.5.dp, Color.White.copy(.32f), shape)
                .padding(padding.dp), content = content
        )
    }
}

@Composable
fun Modifier.keelPressable(
    enabled: Boolean = true,
    haptic: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .955f else 1f, spring(dampingRatio = .62f, stiffness = 650f), label = "keelPress")
    val feedback = LocalHapticFeedback.current
    return this.scale(scale).clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
        feedback.performHapticFeedback(haptic)
        onClick()
    }
}

@Composable
fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit, dark: Boolean = false, badge: Boolean = false) {
    val shape = CircleShape
    Box(
        Modifier.size(46.dp).shadow(7.dp, shape).clip(shape)
            .background(if (dark) Color.Black.copy(.24f) else LocalKeelPalette.current.glassStrong.copy(.82f))
            .background(Brush.linearGradient(listOf(Color.White.copy(if (dark) .10f else .34f), Color.Transparent)))
            .border(1.dp, Color.White.copy(if (dark) .28f else .82f), shape)
            .keelPressable(onClick = onClick).semantics { contentDescription = if (badge) "$description，有未读" else description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (dark) Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp))
        // 有未读消息时右上角点一颗小红点
        if (badge) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(10.dp).clip(CircleShape)
                    .background(Color(0xFFE65353))
                    .border(1.5.dp, if (dark) Color.Black.copy(.35f) else Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f, spring(stiffness = 600f), label = "primaryPress")
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val gradient = if (primary.red > .7f) listOf(Color(0xFFFF8A58), BrandOrangeDeep) else listOf(Color(0xFF62D9CF), Color(0xFF26AAA1))
    Box(
        modifier.scale(scale).height(56.dp).clip(CircleShape)
            .background(if (enabled) Brush.verticalGradient(gradient) else Brush.verticalGradient(listOf(primary.copy(.35f), primary.copy(.35f))))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .padding(horizontal = 26.dp), contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)) }
            Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    // 注意顺序：内边距必须在背景/边框之后，否则文字会紧贴胶囊边缘
    Box(
        modifier.height(56.dp).widthIn(min = 132.dp).clip(CircleShape)
            .background(color.copy(.075f)).border(1.25.dp, color.copy(.42f), CircleShape)
            .keelPressable(onClick = onClick).padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun KeelChip(text: String, selected: Boolean, onClick: () -> Unit, badge: String? = null) {
    val background by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else LocalKeelPalette.current.chip, label = "chipColor")
    val foreground = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(.68f)
    Row(Modifier.sizeIn(minHeight = 42.dp).shadow(if (selected) 5.dp else 0.dp, CircleShape).clip(CircleShape).background(background).then(if (selected) Modifier else Modifier.border(1.dp, LocalKeelPalette.current.glassBorder, CircleShape)).keelPressable(onClick = onClick).padding(horizontal = 15.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text, color = foreground, style = MaterialTheme.typography.labelMedium)
            if (badge != null) Box(Modifier.heightIn(min = 20.dp).widthIn(min = 20.dp).background(foreground.copy(.16f), CircleShape).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) { Text(badge, color = foreground, fontSize = 10.sp, maxLines = 1) }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted, modifier = Modifier.padding(top = 3.dp))
        }
        if (action != null) Row(Modifier.keelPressable { onAction?.invoke() }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PageTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) GlassIconButton(Icons.Rounded.ArrowBackIosNew, "返回", onBack) else Spacer(Modifier.size(46.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun LocalPhoto(model: Any?, description: String?, modifier: Modifier = Modifier, contentScale: ContentScale = Crop) {
    when (model) {
        // 刚在相册里选中的图片（content:// Uri）：选了立刻能看到
        is Uri -> AsyncImage(
            model = model,
            contentDescription = description,
            modifier = modifier,
            contentScale = contentScale
        )
        // 服务端下发的图片（相对路径会自动补成完整地址）
        is String -> AsyncImage(
            model = MediaUrl.of(model),
            contentDescription = description,
            modifier = modifier,
            contentScale = contentScale
        )
        is Int -> if (model > 0) {
            Image(painterResource(model), description, modifier, contentScale = contentScale)
        } else {
            PhotoFallback(modifier)
        }
        else -> PhotoFallback(modifier)
    }
}

/** 图片缺失时的占位，避免 0 资源 id 把页面搞崩 */
@Composable
private fun PhotoFallback(modifier: Modifier) {
    Box(
        modifier.background(LocalKeelPalette.current.chip, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.LocalDining, null, tint = LocalKeelPalette.current.muted, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    error: String? = null
) {
    OutlinedTextField(
        value, onValueChange, modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 4,
        maxLines = if (singleLine) 1 else 7, keyboardOptions = keyboardOptions, shape = RoundedCornerShape(18.dp),
        label = { Text(label) }, leadingIcon = if (leadingIcon == null) null else ({ Icon(leadingIcon, null) }),
        isError = error != null, supportingText = if (error == null) null else ({ Text(error) }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LocalKeelPalette.current.glassStrong,
            unfocusedContainerColor = LocalKeelPalette.current.glass,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = LocalKeelPalette.current.glassBorder,
            disabledIndicatorColor = Color.Transparent,
            errorContainerColor = LocalKeelPalette.current.glass
        )
    )
}

@Composable
fun SettingItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit, end: @Composable RowScope.() -> Unit = { Icon(Icons.Rounded.ChevronRight, null, tint = LocalKeelPalette.current.muted) }) {
    Row(Modifier.fillMaxWidth().keelPressable(onClick = onClick).sizeIn(minHeight = 66.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary.copy(.11f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp)) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalKeelPalette.current.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        end()
    }
}

@Composable
fun KeelSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(checked, onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = Color.White))
}

@Composable
fun StatusPill(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Box(Modifier.heightIn(min = 26.dp).widthIn(min = 44.dp).background(color.copy(.11f), CircleShape).border(1.dp, color.copy(.22f), CircleShape).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
    }
}

@Composable
fun KeelAlertDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    icon: ImageVector? = null
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 24.dp), strong = true, radius = 30, padding = 20) {
            if (icon != null) {
                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(.11f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.height(14.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = LocalKeelPalette.current.muted)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(dismissText, onDismiss, Modifier.weight(1f))
                if (danger) SecondaryButton(confirmText, onConfirm, Modifier.weight(1f), danger = true)
                else PrimaryButton(confirmText, onConfirm, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BottomSheetHandle() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 38.dp, height = 5.dp).background(MaterialTheme.colorScheme.onSurface.copy(.16f), CircleShape))
    }
}
