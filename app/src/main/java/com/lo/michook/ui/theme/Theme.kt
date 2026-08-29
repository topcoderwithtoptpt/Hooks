package com.lo.michook.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val CyanPrimary = Color(0xFF00E5FF)
val CyanSecondary = Color(0xFF00B0FF)
val VioletAccent = Color(0xFF7C4DFF)
val EmeraldSuccess = Color(0xFF00E676)
val AmberWarning = Color(0xFFFFAB00)
val RoseError = Color(0xFFFF1744)

val DarkBackground = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF131B2A)
val DarkSurfaceElevated = Color(0xFF1B2538)
val DarkSurfaceCard = Color(0xFF222E46)

val TextPrimary = Color(0xFFF0F4FC)
val TextSecondary = Color(0xFF90A4AE)
val TextMuted = Color(0xFF607D8B)

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = CyanPrimary,
    secondary = VioletAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF381E72),
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = EmeraldSuccess,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = RoseError
)

private val LightColorScheme = darkColorScheme( // Keep high-contrast studio dark theme
    primary = CyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = CyanPrimary,
    secondary = VioletAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF381E72),
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = EmeraldSuccess,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = RoseError
)

@Composable
fun MicHookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
