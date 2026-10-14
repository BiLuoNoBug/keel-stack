package com.keel.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BrandOrange = Color(0xFFFF6B35)
val BrandOrangeDeep = Color(0xFFF4531E)
val BrandMint = Color(0xFF33BFB5)
val Ink = Color(0xFF171725)
val Paper = Color(0xFFF7F8FC)
val Gold = Color(0xFFFFBE3F)
val Success = Color(0xFF27AE85)
val Danger = Color(0xFFE65353)

@Immutable
data class KeelPalette(
    val ambientTop: Color,
    val ambientBottom: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val muted: Color,
    val chip: Color,
    val scrim: Color
)

val LocalKeelPalette = staticCompositionLocalOf {
    KeelPalette(Paper, Color.White, Color.White.copy(.70f), Color.White.copy(.92f), Color.White.copy(.75f), Color(0xFF757582), Color.White.copy(.72f), Color.Black.copy(.42f))
}

private val LightClient = lightColorScheme(
    primary = BrandOrange, onPrimary = Color.White, primaryContainer = Color(0xFFFFE6DC),
    onPrimaryContainer = Color(0xFF7A270A), secondary = BrandMint, background = Paper,
    surface = Color.White, surfaceVariant = Color(0xFFF0F1F6), onBackground = Ink,
    onSurface = Ink, outline = Color(0xFFE2E2E9), error = Danger
)

private val LightAdmin = LightClient.copy(
    primary = BrandMint, primaryContainer = Color(0xFFDDF7F3),
    onPrimaryContainer = Color(0xFF064D48), secondary = BrandOrange
)

private val DarkClient = darkColorScheme(
    primary = Color(0xFFFF8A61), onPrimary = Color.White, primaryContainer = Color(0xFF5B2515),
    secondary = Color(0xFF69D6CC), background = Color(0xFF0D0D15), surface = Color(0xFF181820),
    surfaceVariant = Color(0xFF24242E), onBackground = Color(0xFFF5F4FA),
    onSurface = Color(0xFFF5F4FA), outline = Color(0xFF3A3944), error = Color(0xFFFF7A82)
)

private val DarkAdmin = DarkClient.copy(
    primary = Color(0xFF69D6CC), primaryContainer = Color(0xFF16443F), secondary = Color(0xFFFF8A61)
)

val KeelTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 45.sp, letterSpacing = (-.8).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-.4).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 29.sp, letterSpacing = (-.2).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp)
)

@Composable
fun KeelTheme(darkTheme: Boolean, admin: Boolean, content: @Composable () -> Unit) {
    val scheme = when {
        darkTheme && admin -> DarkAdmin
        darkTheme -> DarkClient
        admin -> LightAdmin
        else -> LightClient
    }
    val palette = if (darkTheme) {
        KeelPalette(
            if (admin) Color(0xFF102422) else Color(0xFF251611), Color(0xFF0D0D15),
            Color(0xFF23232D).copy(.74f), Color(0xFF282833).copy(.94f), Color.White.copy(.11f),
            Color(0xFFA9A7B2), Color.White.copy(.08f), Color.Black.copy(.62f)
        )
    } else {
        KeelPalette(
            if (admin) Color(0xFFE9FFF9) else Color(0xFFFFEFE7),
            if (admin) Color(0xFFF5FAFA) else Color(0xFFF4F3FF),
            Color.White.copy(.67f), Color.White.copy(.91f), Color.White.copy(.88f),
            Color(0xFF73717E), Color.White.copy(.76f), Color(0xFF171725).copy(.45f)
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalKeelPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = KeelTypography, content = content)
    }
}
