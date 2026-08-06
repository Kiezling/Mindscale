package com.kieslingdev.mindscale.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.kieslingdev.mindscale.data.ThemeMode

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
    CompositionLocalProvider(
        LocalMindScalePalette provides if (darkTheme) DarkPalette else LightPalette
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) MindScaleDarkColorScheme else MindScaleLightColorScheme,
            typography = MindScaleTypography,
            // Passed for the first time in Phase 15. Before this the app rendered every
            // Material surface on Material's default radii, which is why nothing was 14 dp.
            shapes = MindScaleShapes,
            content = content
        )
    }
}
