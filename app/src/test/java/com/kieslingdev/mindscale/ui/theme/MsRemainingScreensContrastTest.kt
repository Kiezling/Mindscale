package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * `docs/specs/SPEC-remaining-screens-visual.md` D-7 and Invariant 5.
 *
 * Phases 16 and 17 already measured the design's `rgba(ink,.07–.16)` and `rgba(gold,.4–.95)`
 * border idioms and found every one of them below `SPEC-visual-foundation.md` D-23's 3:1 non-text
 * floor. Those idioms recur across Settings, Profile, Report, Safety and Breathing, and this phase
 * **reuses** those figures rather than re-deriving them. What this test adds is the four values the
 * earlier phases did not have a screen for — the settings row separator at `.07`, the segmented
 * control's container at `.09`, the Breathing ring at 45% gold, and the underlined field rule — and
 * the assertion that this phase's two exemptions stay exemptions.
 *
 * The measurement method is deliberately identical to [MsControlBoundaryContrastTest]'s and
 * [MsInsightsContrastTest]'s: same channel function, same floating-point compositing, same
 * rounding. Three tests that disagree by a hundredth would be worse than one.
 */
class MsRemainingScreensContrastTest {

    private companion object {
        const val AA_NON_TEXT = 3.0
        const val AA_TEXT = 4.5

        val LIGHT_ADJACENT = listOf("bg" to LightBg, "card" to LightCard)
        val DARK_ADJACENT = listOf("bg" to DarkBg, "card" to DarkCard)
    }

    private fun channel(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun over(fg: Color, bg: Color) = Color(
        red = fg.red * fg.alpha + bg.red * (1f - fg.alpha),
        green = fg.green * fg.alpha + bg.green * (1f - fg.alpha),
        blue = fg.blue * fg.alpha + bg.blue * (1f - fg.alpha)
    )

    private fun ratio(fg: Color, bg: Color): Double {
        val a = luminance(over(fg, bg))
        val b = luminance(bg)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    private fun assertPasses(
        label: String,
        mark: Color,
        adjacent: List<Pair<String, Color>>,
        floor: Double = AA_NON_TEXT
    ) {
        adjacent.forEach { (name, surface) ->
            val r = ratio(mark, surface)
            assertTrue(
                "$label against $name measured ${round2(r)}:1, below $floor:1",
                r >= floor
            )
        }
    }

    private fun assertFails(label: String, mark: Color, adjacent: List<Pair<String, Color>>) {
        adjacent.forEach { (name, surface) ->
            val r = ratio(mark, surface)
            assertTrue(
                "$label against $name measured ${round2(r)}:1 and would have passed " +
                    "$AA_NON_TEXT:1 — if that is now true, D-7 needs revisiting rather than this " +
                    "test relaxing",
                r < AA_NON_TEXT
            )
        }
    }

    // ── the failing design values (D-7, first table) ─────────────────────────

    /**
     * The underlined field rule the design draws beneath `DATE`, beneath each anchor row and
     * beneath the name field. It has no fill and no ring, so the 1 dp rule **is** the control's
     * only boundary — the same finding, in the same words, that D-4 of the Track and Log spec
     * recorded for Log's From/To underline and that Phase 16 already fixed there.
     */
    @Test
    fun theDesignsUnderlinedFieldRuleFailsTheControlBoundaryFloor() {
        assertEquals(1.41, round2(ratio(LightInk.copy(alpha = 0.16f), LightCard)), 0.001)
        assertEquals(1.41, round2(ratio(LightInk.copy(alpha = 0.16f), LightBg)), 0.001)
        assertEquals(1.57, round2(ratio(DarkInk.copy(alpha = 0.16f), DarkCard)), 0.001)
        assertEquals(1.51, round2(ratio(DarkInk.copy(alpha = 0.16f), DarkBg)), 0.001)

        assertFails("design field underline", LightInk.copy(alpha = 0.16f), LIGHT_ADJACENT)
        assertFails("design field underline", DarkInk.copy(alpha = 0.16f), DARK_ADJACENT)
    }

    /**
     * The design's unselected instrument and range chip border, `rgba(ink,.14)`. `MsChip` already
     * moved off it in Phase 16; this re-measures it because Profile and Report are the last two
     * screens still painting a `FilterChip` at that alpha.
     */
    @Test
    fun theDesignsUnselectedChipBorderFailsTheControlBoundaryFloor() {
        assertEquals(1.35, round2(ratio(LightInk.copy(alpha = 0.14f), LightCard)), 0.001)
        assertEquals(1.48, round2(ratio(DarkInk.copy(alpha = 0.14f), DarkCard)), 0.001)

        assertFails("design chip border", LightInk.copy(alpha = 0.14f), LIGHT_ADJACENT)
        assertFails("design chip border", DarkInk.copy(alpha = 0.14f), DARK_ADJACENT)
    }

    /**
     * Breathing's outer ring and its close pill, both `rgba(gold,.45)` in the design. The ring is
     * the only thing separating the pacing circle from the page — on a full-bleed screen there is
     * no card edge to help — and the close pill is the screen's only exit control that is not
     * system Back.
     *
     * This is the same 45% gold `MsPillButton` moved off in Phase 16, so the pill inherits the fix
     * by using the component. The ring is measured here because nothing else in the app draws one.
     */
    @Test
    fun theDesignsPartialGoldRingAndPillBorderFailTheControlBoundaryFloor() {
        assertEquals(1.60, round2(ratio(LightGold.copy(alpha = 0.45f), LightCard)), 0.001)
        assertEquals(1.57, round2(ratio(LightGold.copy(alpha = 0.45f), LightBg)), 0.001)
        assertEquals(2.61, round2(ratio(DarkGold.copy(alpha = 0.45f), DarkCard)), 0.001)
        assertEquals(2.62, round2(ratio(DarkGold.copy(alpha = 0.45f), DarkBg)), 0.001)

        assertFails("design breathing ring", LightGold.copy(alpha = 0.45f), LIGHT_ADJACENT)
        assertFails("design breathing ring", DarkGold.copy(alpha = 0.45f), DARK_ADJACENT)
    }

    // ── the adopted replacements (D-7, second table) ─────────────────────────

    /**
     * Both replacements are tokens Phase 16 calibrated and pinned. This phase reuses them rather
     * than inventing a third ink alpha or a second gold, which is why these figures are identical
     * to [MsControlBoundaryContrastTest]'s and [MsInsightsContrastTest]'s.
     */
    @Test
    fun theAdoptedBoundariesClearTheFloorInBothThemes() {
        assertPasses("outline", LightPalette.outline, LIGHT_ADJACENT)
        assertPasses("outline", DarkPalette.outline, DARK_ADJACENT)
        assertPasses("gold", LightPalette.gold, LIGHT_ADJACENT)
        assertPasses("gold", DarkPalette.gold, DARK_ADJACENT)

        assertEquals(3.49, round2(ratio(LightPalette.outline, LightCard)), 0.001)
        assertEquals(3.47, round2(ratio(LightPalette.outline, LightBg)), 0.001)
        assertEquals(3.51, round2(ratio(DarkPalette.outline, DarkCard)), 0.001)
        assertEquals(3.47, round2(ratio(DarkPalette.outline, DarkBg)), 0.001)
        assertEquals(3.15, round2(ratio(LightPalette.gold, LightCard)), 0.001)
        assertEquals(3.05, round2(ratio(LightPalette.gold, LightBg)), 0.001)
        assertEquals(8.04, round2(ratio(DarkPalette.gold, DarkCard)), 0.001)
        assertEquals(8.60, round2(ratio(DarkPalette.gold, DarkBg)), 0.001)
    }

    /**
     * D-4's constraint 1, re-checked because Breathing paints a gold ring on a full-bleed page and
     * Report paints a gold-bordered emphasized card: **full light gold is a legal control boundary
     * on `bg` and `card` only.** Every gold boundary this phase paints sits on one of those two.
     */
    @Test
    fun fullLightGoldIsStillNotLegalOnTheDimmestContainer() {
        val dimmest = MindScaleLightColorScheme.surfaceContainerHighest
        assertEquals(2.67, round2(ratio(LightPalette.gold, dimmest)), 0.001)
        assertTrue(ratio(LightPalette.gold, dimmest) < AA_NON_TEXT)
    }

    /**
     * Safety's crisis actions and the toast share one treatment — an ink fill with `onInk`
     * lettering — and D-11 keeps the crisis actions filled rather than making them the design's
     * bare text actions, because prominence is the affordance on that screen. The pair has to clear
     * the *text* floor, not the non-text one, so it is measured against 4.5:1.
     */
    @Test
    fun theInkFillAndOnInkPairClearsTheTextFloorInBothThemes() {
        assertEquals(11.98, round2(ratio(LightOnInk, LightInk)), 0.001)
        assertEquals(13.93, round2(ratio(DarkOnInk, DarkInk)), 0.001)
        assertTrue(ratio(LightOnInk, LightInk) >= AA_TEXT)
        assertTrue(ratio(DarkOnInk, DarkInk) >= AA_TEXT)
    }

    /**
     * Report's error banner and Settings' `Export, then erase everything` both move off Material's
     * `errorContainer` onto `danger` painted on `card`. D-8 of the foundation set those values;
     * this asserts they survive the move to a card that is not the error container.
     */
    @Test
    fun dangerTextClearsTheTextFloorOnCardInBothThemes() {
        assertPasses("danger", LightPalette.danger, LIGHT_ADJACENT, floor = AA_TEXT)
        assertPasses("danger", DarkPalette.danger, DARK_ADJACENT, floor = AA_TEXT)
    }

    // ── the deliberate exemptions (D-7, third table) ─────────────────────────

    /**
     * The two faint ink rules this phase keeps, asserted as *below* 3:1 on purpose.
     *
     * The settings row separator at `.07` is a separator and D-6 of the foundation exempts it
     * outright. The segmented control's container at `.09` is the interesting one: it sits around
     * interactive segments and is still exempt, because **it is not those segments' boundary.** A
     * selected segment carries an ink fill and `onInk` lettering; an unselected one carries the
     * page. The container is the design's quiet frame around a group.
     *
     * A later phase that "fixed" either into a heavy ring would lose the design's quietest rules
     * and break this test on the way.
     */
    @Test
    fun theSeparatorAndTheSegmentedContainerStayFaintAndAreExempt() {
        assertEquals(1.16, round2(ratio(LightPalette.hairlineFaint, LightCard)), 0.001)
        assertEquals(1.18, round2(ratio(DarkPalette.hairlineFaint, DarkCard)), 0.001)
        assertEquals(1.20, round2(ratio(LightPalette.hairline, LightCard)), 0.001)
        assertEquals(1.26, round2(ratio(DarkPalette.hairline, DarkCard)), 0.001)

        assertFails("settings row separator", LightPalette.hairlineFaint, LIGHT_ADJACENT)
        assertFails("settings row separator", DarkPalette.hairlineFaint, DARK_ADJACENT)
        assertFails("segmented control container", LightPalette.hairline, LIGHT_ADJACENT)
        assertFails("segmented control container", DarkPalette.hairline, DARK_ADJACENT)
    }

    /**
     * The exemption above is only legal because the segment's *own* state is carried by something
     * that is not the container. This asserts the second signal exists and is compliant: the
     * selected fill is `ink`, which separates from both adjacent surfaces far above the floor, and
     * its lettering is `onInk`, covered by [theInkFillAndOnInkPairClearsTheTextFloorInBothThemes].
     *
     * Without this, "the container is exempt" would be an excuse rather than a decision.
     */
    @Test
    fun aSelectedSegmentsFillIsItsOwnBoundaryAndClearsTheFloor() {
        assertPasses("selected segment fill", LightPalette.ink, LIGHT_ADJACENT)
        assertPasses("selected segment fill", DarkPalette.ink, DARK_ADJACENT)
    }
}
