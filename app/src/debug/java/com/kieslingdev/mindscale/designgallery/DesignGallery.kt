package com.kieslingdev.mindscale.designgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.ui.components.MsActionTone
import com.kieslingdev.mindscale.ui.components.MsCard
import com.kieslingdev.mindscale.ui.components.MsChip
import com.kieslingdev.mindscale.ui.components.MsCircularHeaderButton
import com.kieslingdev.mindscale.ui.components.MsEyebrow
import com.kieslingdev.mindscale.ui.components.MsHairline
import com.kieslingdev.mindscale.ui.components.MsHeaderRule
import com.kieslingdev.mindscale.ui.components.MsPillButton
import com.kieslingdev.mindscale.ui.components.MsSegmentedControl
import com.kieslingdev.mindscale.ui.components.MsTextAction
import com.kieslingdev.mindscale.ui.components.MsToastPill
import com.kieslingdev.mindscale.ui.components.MsWordmark
import com.kieslingdev.mindscale.ui.theme.MindScaleTextStyles
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import com.kieslingdev.mindscale.ui.theme.MsShapes
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms

/**
 * Every token and every component on one screen, in both themes, at any font scale
 * (`docs/specs/SPEC-visual-foundation.md`, D-21).
 *
 * Debug source set only. It touches no `ViewModel`, no DAO, and no database — every value here
 * is a literal — so it cannot read, write, or fabricate user data.
 */
@Composable
fun DesignGallery(themeMode: ThemeMode) {
    MindScaleTheme(themeMode = themeMode) {
        val palette = MaterialTheme.ms
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .verticalScroll(rememberScrollState())
                .padding(MsSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(MsSpacing.xxlPlus)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)
            ) {
                MsWordmark("MindScale")
                MsHeaderRule()
            }

            Section("Surfaces") {
                Swatch("bg", palette.bg)
                Swatch("card", palette.card)
                Swatch("ink", palette.ink)
                Swatch("onInk", palette.onInk)
                Swatch("gold", palette.gold)
                Swatch("goldText", palette.goldText)
                Swatch("danger", palette.danger)
                Swatch("sleepBand", palette.sleepBand)
            }

            Section("Emphasis") {
                EmphasisRow("primary 1.00", palette.inkPrimary)
                EmphasisRow("secondary 0.80", palette.inkSecondary)
                EmphasisRow("tertiary 0.70", palette.inkTertiary)
                EmphasisRow("quaternary 0.60", palette.inkQuaternary)
            }

            Section("Type") {
                TypeRow("displayLarge 26", MaterialTheme.typography.displayLarge, "0123456789")
                TypeRow("displaySmall 21", MaterialTheme.typography.displaySmall, "10")
                TypeRow("titleSmall 13", MaterialTheme.typography.titleSmall, "Settings row")
                TypeRow("bodyLarge 13.5", MaterialTheme.typography.bodyLarge, "Nothing recorded means nothing was happening.")
                TypeRow("bodySmall 12.5", MaterialTheme.typography.bodySmall, "The numbers only have to mean the same thing to you each time.")
                TypeRow("labelLarge 10.5", MaterialTheme.typography.labelLarge, "SEGMENT")
                TypeRow("labelMedium 9.5", MaterialTheme.typography.labelMedium, "SAVE")
                TypeRow("labelSmall 9", MaterialTheme.typography.labelSmall, "EYEBROW")
                TypeRow("wordmark 12", MindScaleTextStyles.wordmark, "MINDSCALE")
                TypeRow("chip 11.5", MindScaleTextStyles.chip, "not shouting")
            }

            Section("Shape") {
                ShapeRow("extraSmall 2", MaterialTheme.shapes.extraSmall)
                ShapeRow("small 9", MaterialTheme.shapes.small)
                ShapeRow("medium 14", MaterialTheme.shapes.medium)
                ShapeRow("large 16", MaterialTheme.shapes.large)
                ShapeRow("extraLarge 24", MaterialTheme.shapes.extraLarge)
                ShapeRow("pill 999", MsShapes.pill)
                ShapeRow("circle", MsShapes.circle)
            }

            Section("Cards") {
                MsCard(modifier = Modifier.fillMaxWidth()) {
                    MsEyebrow("Tracking paused")
                    Text(
                        "Your data is still here and still yours.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.inkSecondary
                    )
                }
                MsCard(modifier = Modifier.fillMaxWidth(), emphasized = true) {
                    MsEyebrow("Something earlier")
                    Text(
                        "Emphasized: 16 dp radius, gold border.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.inkSecondary
                    )
                }
                MsHairline()
                MsHairline(faint = true)
            }

            Section("Actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus)) {
                    MsTextAction("Save", {})
                    MsTextAction("Cancel", {}, tone = MsActionTone.Muted)
                    MsTextAction("Delete", {}, tone = MsActionTone.Danger)
                    MsTextAction("Disabled", {}, enabled = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)) {
                    MsPillButton("Sleep", {})
                    MsPillButton("Wake", {}, selected = true)
                }
                MsCircularHeaderButton("‹", {})
            }

            Section("Selection") {
                var chip by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)) {
                    MsChip("Work", selected = chip, onClick = { chip = !chip })
                    MsChip("Alone", selected = !chip, onClick = { chip = !chip })
                }
                var segment by remember { mutableIntStateOf(0) }
                MsSegmentedControl(
                    options = listOf("Light", "Dark", "System"),
                    selectedIndex = segment,
                    onSelect = { segment = it }
                )
                var hold by remember { mutableIntStateOf(3) }
                MsSegmentedControl(
                    options = listOf("8h", "12h", "16h", "24h"),
                    selectedIndex = hold,
                    onSelect = { hold = it }
                )
            }

            Section("Toast") {
                MsToastPill("Asleep at 23:40")
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)) {
        MsEyebrow(title)
        MsHairline()
        content()
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val palette = MaterialTheme.ms
    Row(
        horizontalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color)
                .border(MsSpacing.hairline, palette.outlineDecorative, MaterialTheme.shapes.small)
        )
        Text(name, style = MaterialTheme.typography.titleSmall, color = palette.inkPrimary)
    }
}

@Composable
private fun EmphasisRow(name: String, color: Color) {
    Text(
        "$name — the quick brown fox",
        style = MaterialTheme.typography.bodyMedium,
        color = color
    )
}

@Composable
private fun TypeRow(
    name: String,
    style: androidx.compose.ui.text.TextStyle,
    sample: String
) {
    val palette = MaterialTheme.ms
    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = palette.inkQuaternary)
        Text(sample, style = style, color = palette.inkPrimary)
    }
}

@Composable
private fun ShapeRow(name: String, shape: androidx.compose.ui.graphics.Shape) {
    val palette = MaterialTheme.ms
    Row(
        horizontalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(56.dp, 40.dp)
                .clip(shape)
                .background(palette.card)
                .border(MsSpacing.hairline, palette.gold, shape)
        )
        Text(name, style = MaterialTheme.typography.titleSmall, color = palette.inkPrimary)
    }
}

// ── previews (D-21, and the "@Preview in light/dark at 100%/200%" criterion) ──

@Preview(name = "Gallery light 100%", showBackground = true, heightDp = 2400)
@Composable
private fun GalleryLightPreview() = DesignGallery(ThemeMode.LIGHT)

@Preview(name = "Gallery dark 100%", showBackground = true, heightDp = 2400)
@Composable
private fun GalleryDarkPreview() = DesignGallery(ThemeMode.DARK)

@Preview(name = "Gallery light 200%", showBackground = true, heightDp = 4200, fontScale = 2.0f)
@Composable
private fun GalleryLightLargeFontPreview() = DesignGallery(ThemeMode.LIGHT)

@Preview(name = "Gallery dark 200%", showBackground = true, heightDp = 4200, fontScale = 2.0f)
@Composable
private fun GalleryDarkLargeFontPreview() = DesignGallery(ThemeMode.DARK)
