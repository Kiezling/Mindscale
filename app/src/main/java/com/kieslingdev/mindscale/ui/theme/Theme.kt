package com.kieslingdev.mindscale.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kieslingdev.mindscale.data.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = MindScaleDarkGold,
    onPrimary = Color(0xFF2A1E0A),
    primaryContainer = MindScaleDarkGoldContainer,
    onPrimaryContainer = MindScaleDarkInk,
    secondary = MindScaleDarkGold,
    secondaryContainer = Color(0xFF342B1D),
    background = MindScaleDarkBackground,
    onBackground = MindScaleDarkInk,
    surface = MindScaleDarkSurface,
    onSurface = MindScaleDarkInk,
    surfaceVariant = Color(0xFF26211B),
    onSurfaceVariant = Color(0xFFD5CEC2)
)

private val LightColorScheme = lightColorScheme(
    primary = MindScaleLightGold,
    onPrimary = Color.White,
    primaryContainer = MindScaleLightGoldContainer,
    onPrimaryContainer = MindScaleLightInk,
    secondary = MindScaleLightGold,
    secondaryContainer = Color(0xFFF4EAD7),
    background = MindScaleLightBackground,
    onBackground = MindScaleLightInk,
    surface = MindScaleLightSurface,
    onSurface = MindScaleLightInk,
    surfaceVariant = Color(0xFFF3EFE8),
    onSurfaceVariant = Color(0xFF5D554A)
)

@Composable
fun MindScaleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
