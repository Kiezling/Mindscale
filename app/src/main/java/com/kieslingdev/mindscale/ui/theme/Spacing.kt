package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale the design actually uses (`docs/specs/SPEC-visual-foundation.md`, D-14).
 *
 * The 246 hardcoded dimension literals in the app are **not** all retired at once. Rewriting the
 * numbers inside screen bodies that a phase is not otherwise touching would be a large, untested
 * diff whose only purpose is tidiness, and every one of those edits is a chance to silently drop
 * one of the 158 testTags the connected suite depends on. Literals are converted screen by
 * screen, in the phase that already restyles that screen. Phase 18's closing audit asserts that
 * nothing outside `ui/theme` is left holding an undocumented literal.
 */
object MsSpacing {
    val xxxs: Dp = 2.dp
    val xxs: Dp = 4.dp
    val xs: Dp = 6.dp
    val sm: Dp = 8.dp
    val smPlus: Dp = 9.dp
    val md: Dp = 10.dp
    val mdPlus: Dp = 12.dp
    val lg: Dp = 14.dp
    val lgPlus: Dp = 16.dp
    val xl: Dp = 18.dp
    val xlPlus: Dp = 20.dp
    val xxl: Dp = 22.dp
    val xxlPlus: Dp = 24.dp
    val xxxl: Dp = 30.dp
    val huge: Dp = 36.dp

    /**
     * The floor for every interactive element regardless of its painted size
     * (`docs/specs/SPEC-visual-foundation.md`, D-23). The design's 26 dp help button, 34 dp
     * header buttons, and 42 dp entry dots keep their painted size and reach this with
     * transparent padding.
     */
    val minTouchTarget: Dp = 48.dp

    /** The hairline that carries separation in place of a shadow (D-13). */
    val hairline: Dp = 1.dp

    /** The 22 x 1 dp gold rule beneath the header title (D-16). */
    val headerRuleWidth: Dp = 22.dp

    /** The design's 34 dp circular header buttons (D-16). */
    val headerButton: Dp = 34.dp
}
