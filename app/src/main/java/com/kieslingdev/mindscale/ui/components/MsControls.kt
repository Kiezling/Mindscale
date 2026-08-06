package com.kieslingdev.mindscale.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kieslingdev.mindscale.ui.theme.MindScaleTextStyles
import com.kieslingdev.mindscale.ui.theme.MsShapes
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms

/**
 * The design's outlined pill: 999 dp radius, hairline gold or ink border, uppercase tracked
 * label, no fill until it is selected (`docs/specs/SPEC-visual-foundation.md`, D-12 and D-15).
 *
 * Selection uses the D-9 inverse pair — an ink fill with warm `onInk` text — which is the same
 * treatment in both themes but not the same colours: `onInk` is `#E3CE9F` in light and `#2A2114`
 * in dark. It is never colour alone: the fill itself is the second signal (D-23).
 */
@Composable
fun MsPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    val palette = MaterialTheme.ms
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = MsSpacing.minTouchTarget)
            .clip(MsShapes.pill)
            .background(if (selected) palette.ink else androidx.compose.ui.graphics.Color.Transparent)
            .border(
                width = MsSpacing.hairline,
                color = if (selected) palette.ink else palette.gold.copy(alpha = 0.45f),
                shape = MsShapes.pill
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = MsSpacing.xxl, vertical = MsSpacing.mdPlus),
        contentAlignment = Alignment.Center
    ) {
        MsUppercaseText(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !enabled -> palette.inkQuaternary.copy(alpha = 0.38f)
                selected -> palette.onInk
                else -> palette.goldText
            }
        )
    }
}

/**
 * An onset chip.
 *
 * Unlike almost every other label in the design, a chip is **not** uppercased: `chipBtn` at line
 * 1223 of the design authority sets no `text-transform`, because a chip carries the user's own
 * words and shouting them back is both a change of voice and a TalkBack problem.
 */
@Composable
fun MsChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = MaterialTheme.ms
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = MsSpacing.minTouchTarget)
            .clip(MsShapes.pill)
            .background(if (selected) palette.ink else androidx.compose.ui.graphics.Color.Transparent)
            .border(
                width = MsSpacing.hairline,
                color = if (selected) palette.ink else palette.outlineDecorative,
                shape = MsShapes.pill
            )
            .selectable(selected = selected, enabled = enabled, onClick = onClick)
            .padding(horizontal = MsSpacing.lg, vertical = MsSpacing.smPlus),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MindScaleTextStyles.chip,
            color = if (selected) palette.onInk else palette.inkTertiary
        )
    }
}

/**
 * The design's segmented control: a pill-shaped hairline container holding equal-weight
 * segments, the selected one filled with ink and lettered in `onInk`.
 *
 * **Equal weight is the point.** The prototype's segments size to their content, so
 * `12-hour`/`24-hour` and `8h`/`12h`/`16h`/`24h` render at visibly different widths and the row
 * loses its rhythm. Correcting that is layout flaw L-6 in D-22, and fixing it here in the
 * component means no screen can reintroduce it.
 *
 * This component renders the segments a caller already had. It does not decide what segments
 * exist (D-15).
 */
@Composable
fun MsSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = MaterialTheme.ms
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MsShapes.pill)
            .border(MsSpacing.hairline, palette.hairline, MsShapes.pill)
            .padding(MsSpacing.xxxs + MsSpacing.xxxs + MsSpacing.hairline),
        horizontalArrangement = Arrangement.spacedBy(MsSpacing.xxxs)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    // Equal weight, so every segment is the same width regardless of its text.
                    .weight(1f)
                    .defaultMinSize(minHeight = MsSpacing.minTouchTarget)
                    .clip(MsShapes.pill)
                    .background(
                        if (isSelected) palette.ink
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        onClick = { onSelect(index) }
                    )
                    .padding(horizontal = MsSpacing.xxs, vertical = MsSpacing.smPlus),
                contentAlignment = Alignment.Center
            ) {
                MsUppercaseText(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) palette.onInk else palette.inkQuaternary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The design's toast: an ink pill with warm `onInk` lettering, 999 dp radius, uppercase and
 * tracked (D-20). Measured 11.98:1 in light and 13.93:1 in dark.
 *
 * Presentation only — it owns no timer and dismisses nothing.
 */
@Composable
fun MsToastPill(text: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.ms
    Box(
        modifier = modifier
            .clip(MsShapes.pill)
            .background(palette.ink)
            .padding(horizontal = MsSpacing.xlPlus, vertical = MsSpacing.smPlus),
        contentAlignment = Alignment.Center
    ) {
        MsUppercaseText(
            text = text,
            style = MindScaleTextStyles.toast,
            color = palette.onInk
        )
    }
}

/**
 * The design's 34 dp circular header button: a hairline ring around a single glyph, with the
 * touch target raised to 48 dp by transparent padding rather than by growing the ring (D-16,
 * D-23).
 */
@Composable
fun MsCircularHeaderButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = MaterialTheme.ms
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = MsSpacing.minTouchTarget,
                minHeight = MsSpacing.minTouchTarget
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(
                    minWidth = MsSpacing.headerButton,
                    minHeight = MsSpacing.headerButton
                )
                .clip(MsShapes.circle)
                .border(MsSpacing.hairline, palette.outlineDecorative, MsShapes.circle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                color = palette.inkSecondary
            )
        }
    }
}
