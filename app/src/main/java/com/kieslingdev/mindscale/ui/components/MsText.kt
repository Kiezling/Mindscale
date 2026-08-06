package com.kieslingdev.mindscale.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kieslingdev.mindscale.ui.theme.MindScaleTextStyles
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms

/**
 * The design's eyebrow label: 9 sp, weight 500, uppercase, tracked, at the faintest emphasis
 * level the accessibility floor allows (`docs/specs/SPEC-visual-foundation.md`, D-6 and D-10).
 *
 * The prototype paints these at `rgba(ink, .38)`, which measures 2.43:1. This paints them at
 * 4.77:1 on the page and 4.58:1 on the dimmest surface.
 */
@Composable
fun MsEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    MsUppercaseText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = if (color == Color.Unspecified) MaterialTheme.ms.inkQuaternary else color
    )
}

/** The three tones a bare text action can take. */
enum class MsActionTone { Gold, Muted, Danger }

/**
 * The design's action: bare tracked uppercase text, no fill and no border.
 *
 * This is the single most characteristic control in the prototype — every Save, Cancel, Start
 * again, Export or delete is `background:none;border:none;padding:0` with a gold or faint-ink
 * label. Filled buttons appear nowhere.
 *
 * The painted text keeps the design's size; the *touch target* is raised to 48 dp with
 * transparent padding, because the prototype's zero-padding action is roughly 12 dp tall and
 * fails the target floor (D-23).
 */
@Composable
fun MsTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: MsActionTone = MsActionTone.Gold,
    enabled: Boolean = true
) {
    val palette = MaterialTheme.ms
    val color = when (tone) {
        MsActionTone.Gold -> palette.goldText
        MsActionTone.Muted -> palette.inkQuaternary
        MsActionTone.Danger -> palette.danger
    }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = MsSpacing.minTouchTarget, minHeight = MsSpacing.minTouchTarget)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = MsSpacing.xxs),
        contentAlignment = Alignment.Center
    ) {
        MsUppercaseText(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) color else palette.inkQuaternary.copy(alpha = 0.38f)
        )
    }
}

/**
 * The header wordmark treatment: uppercase, weight 500, tracked to 0.542 em, with the design's
 * 22 x 1 dp gold rule sitting beneath it (D-16).
 */
@Composable
fun MsWordmark(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    MsUppercaseText(
        text = text,
        modifier = modifier,
        style = MindScaleTextStyles.wordmark,
        color = if (color == Color.Unspecified) MaterialTheme.ms.inkPrimary else color
    )
}

/** The 22 x 1 dp gold rule beneath the header title. Decorative; contributes no semantics. */
@Composable
fun MsHeaderRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(MsSpacing.headerRuleWidth)
            .height(MsSpacing.hairline)
            .background(MaterialTheme.ms.gold.copy(alpha = 0.85f))
    )
}
