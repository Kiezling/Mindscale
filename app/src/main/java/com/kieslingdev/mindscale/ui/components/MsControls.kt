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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.kieslingdev.mindscale.ui.theme.MindScaleTextStyles
import com.kieslingdev.mindscale.ui.theme.MsShapes
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms

/**
 * The design's outlined pill: 999 dp radius, hairline gold border, uppercase tracked label, no
 * fill until it is selected (`docs/specs/SPEC-visual-foundation.md`, D-12 and D-15).
 *
 * Selection uses the D-9 inverse pair — an ink fill with warm `onInk` text — which is the same
 * treatment in both themes but not the same colours: `onInk` is `#E3CE9F` in light and `#2A2114`
 * in dark. It is never colour alone: the fill itself is the second signal (D-23).
 *
 * The unselected border is **full** gold rather than the design's `rgba(174,140,79,.45)`. An
 * unselected pill has no fill, so its border is the control's only boundary, and 45% gold measures
 * 1.59:1 against `card` where D-23 requires 3:1. Full gold measures 3.15:1 and keeps the hue
 * exactly (`docs/specs/SPEC-track-and-log-visual.md`, D-4 and D-15).
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
                color = if (selected) palette.ink else palette.gold,
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
 *
 * The unselected border is `outline` rather than the design's `rgba(var(--ink-rgb),.14)`. An
 * unselected chip has no fill, so its border is the control's only boundary, and 14% ink measures
 * 1.35:1 against `card` where D-23 requires 3:1 — which is exactly the case D-6 of the foundation
 * carves out when it says an ink border that is the *only* boundary of an interactive control is
 * not decorative (`docs/specs/SPEC-track-and-log-visual.md`, D-4 and D-15).
 *
 * The touch target is a transparent box around the painted pill rather than the pill itself, which
 * is what D-23 actually prescribes — "the painted geometry is the design's; the target is
 * MindScale's". Growing the pill to 48 dp instead made an eleven-chip value row read as a row of
 * tall ovals rather than the design's compact pills, and it still left a chip labelled `0` only
 * 33 dp *wide*, below the same floor it was over-satisfying vertically. Found by installed-app
 * capture in Phase 16, which is the only place it was visible (D-15).
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
            .defaultMinSize(
                minWidth = MsSpacing.minTouchTarget,
                minHeight = MsSpacing.minTouchTarget
            )
            .selectable(selected = selected, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(MsShapes.pill)
                .background(
                    if (selected) palette.ink else androidx.compose.ui.graphics.Color.Transparent
                )
                .border(
                    width = MsSpacing.hairline,
                    color = if (selected) palette.ink else palette.outline,
                    shape = MsShapes.pill
                )
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
 * The design's circular hairline button: a ring around a single glyph, with the touch target
 * raised to 48 dp by transparent padding rather than by growing the ring (D-16, D-23).
 *
 * Two sizes exist in the design and both are this control: the 34 dp header back button at line 85
 * of the design authority and Track's 26 dp help toggle at line 84. [size] and [textStyle] are
 * defaulted to the header's values so the chrome's call site is unchanged
 * (`docs/specs/SPEC-track-and-log-visual.md`, D-15).
 *
 * The ring is `outline` rather than the design's `rgba(var(--ink-rgb),.16)`. The ring is the only
 * boundary either control has, and 16% ink measures 1.41:1 against `card` where D-23 requires
 * 3:1 (D-4).
 */
@Composable
fun MsCircularHeaderButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = MsSpacing.headerButton,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge
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
                .defaultMinSize(minWidth = size, minHeight = size)
                .clip(MsShapes.circle)
                .border(MsSpacing.hairline, palette.outline, MsShapes.circle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = textStyle.copy(fontWeight = FontWeight.Normal),
                color = palette.inkSecondary
            )
        }
    }
}
