package com.nova.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Brand Colors ──
val Purple500 = Color(0xFF7C3AED)
val Purple400 = Color(0xFFA78BFA)
val Indigo500 = Color(0xFF6366F1)
val Indigo400 = Color(0xFF818CF8)
val Cyan400  = Color(0xFF22D3EE)
val Emerald400 = Color(0xFF34D399)
val Rose400  = Color(0xFFFB7185)
val Amber400 = Color(0xFFFBBF24)

// ── Dark Theme Colors ──
val DarkBg       = Color(0xFF09090B)
val DarkSurface  = Color(0xFF18181B)
val DarkCard     = Color(0xFF27272A)
val DarkBorder   = Color(0xFF3F3F46)
val DarkText     = Color(0xFFFAFAFA)
val DarkSubtext  = Color(0xFFA1A1AA)
val DarkMuted    = Color(0xFF71717A)

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    secondary = Purple400,
    tertiary = Cyan400,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = DarkSubtext,
    outline = DarkBorder,
)

@Composable
fun NovaTheme(
    darkTheme: Boolean = true, // Always dark for Nova
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 0.5.sp),
        ),
        content = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                content()
            }
        }
    )
}
