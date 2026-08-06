package com.kieslingdev.mindscale.designgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.ui.components.MsSegmentedControl
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import com.kieslingdev.mindscale.ui.theme.MsSpacing

/**
 * The debug-only design gallery (`docs/specs/SPEC-visual-foundation.md`, D-21).
 *
 * This class exists only in the debug source set. It is not in the release manifest, not in the
 * release APK, and not reachable from `MainActivity` — the visual-only rule forbids adding a
 * navigation destination to the shipping app, so the gallery is a second launcher icon on debug
 * installs instead of a screen inside MindScale.
 *
 * It holds no `ViewModel`, opens no database, and reads nothing the user recorded.
 */
class DesignGalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var mode by remember {
                mutableStateOf(if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT)
            }
            MindScaleTheme(themeMode = mode) {
                Column {
                    MsSegmentedControl(
                        options = listOf("Light", "Dark"),
                        selectedIndex = if (mode == ThemeMode.LIGHT) 0 else 1,
                        onSelect = { mode = if (it == 0) ThemeMode.LIGHT else ThemeMode.DARK },
                        modifier = Modifier.padding(
                            start = MsSpacing.xxl,
                            end = MsSpacing.xxl,
                            top = MsSpacing.huge,
                            bottom = MsSpacing.sm
                        )
                    )
                    DesignGallery(mode)
                }
            }
        }
    }
}
