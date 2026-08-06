package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * `docs/specs/SPEC-track-and-log-visual.md` D-4, D-5, D-15, and Invariant 5.
 *
 * Almost every control in this design is drawn as transparent-with-a-hairline, and `card` on `bg`
 * is 1.03:1 apart in light, so in almost every case the hairline **is** the control's only
 * boundary. Eight of the design's own border values therefore fail `SPEC-visual-foundation.md`
 * D-23's 3:1 non-text floor. This test is why they cannot come back: it recomputes each one, pins
 * each measured figure, and asserts that every adopted replacement clears the floor against both
 * adjacent surfaces in both themes.
 *
 * The measurement method is deliberately identical to [MindScaleContrastTest]'s — same channel
 * transfer function, same alpha compositing, same rounding — so the two tests cannot disagree
 * about what a ratio means. Every figure here was produced by this code rather than by hand, which
 * is why the spec's tables and these assertions agree to the decimal.
 */
class MsControlBoundaryContrastTest {

    private companion object {
        /** `SPEC-visual-foundation.md` D-23: the floor for a control's sole boundary. */
        const val AA_NON_TEXT = 3.0

        /** `SPEC-visual-foundation.md` D-23: the floor for normal text. */
        const val AA_NORMAL_TEXT = 4.5

        /** A control boundary sits between two surfaces and must clear the floor against both. */
        val LIGHT_ADJACENT = listOf("bg" to LightBg, "card" to LightCard)
        val DARK_ADJACENT = listOf("bg" to DarkBg, "card" to DarkCard)

        /** The dimmest light container, which is where full gold stops being a legal boundary. */
        val LIGHT_DIMMEST = Color(0xFFEFECE6)
    }

    private fun channel(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    /** Flattens [fg] onto opaque [bg] using [fg]'s alpha, the way the compositor does. */
    private fun over(fg: Color, bg: Color) = Color(
        red = fg.red * fg.alpha + bg.red * (1f - fg.alpha),
        green = fg.green * fg.alpha + bg.green * (1f - fg.alpha),
        blue = fg.blue * fg.alpha + bg.blue * (1f - fg.alpha)
    )

    private fun ratio(fg: Color, bg: Color): Double {
        val a = luminance(over(fg, bg))
        val b = luminance(bg)
        val hi = maxOf(a, b)
        val lo = minOf(a, b)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    private fun assertBoundaryPasses(
        label: String,
        border: Color,
        adjacent: List<Pair<String, Color>>
    ) {
        adjacent.forEach { (name, surface) ->
            val r = ratio(border, surface)
            assertTrue(
                "$label against $name measured ${round2(r)}:1, below $AA_NON_TEXT:1",
                r >= AA_NON_TEXT
            )
        }
    }

    private fun assertBoundaryFails(
        label: String,
        border: Color,
        adjacent: List<Pair<String, Color>>
    ) {
        adjacent.forEach { (name, surface) ->
            val r = ratio(border, surface)
            assertTrue(
                "$label against $name measured ${round2(r)}:1 and would have passed $AA_NON_TEXT:1 " +
                    "— if that is now true, D-4 needs revisiting rather than this test relaxing",
                r < AA_NON_TEXT
            )
        }
    }

    // ── the failing design values (D-4, first table) ─────────────────────────

    /**
     * `keyBase` at line 1204 of the design authority. `1.24:1` is not a boundary a sighted user
     * with reduced contrast sensitivity can find, and it is the only edge a numpad key has: the
     * key's `card` fill against the pad's `bg` is 1.03:1 in light.
     */
    @Test
    fun theDesignsNumpadKeyBorderFailsTheControlBoundaryFloor() {
        assertEquals(1.24, round2(ratio(LightInk.copy(alpha = 0.10f), LightCard)), 0.001)
        assertEquals(1.24, round2(ratio(LightInk.copy(alpha = 0.10f), LightBg)), 0.001)
        assertEquals(1.30, round2(ratio(DarkInk.copy(alpha = 0.10f), DarkCard)), 0.001)
        assertEquals(1.26, round2(ratio(DarkInk.copy(alpha = 0.10f), DarkBg)), 0.001)

        assertBoundaryFails("design key border", LightInk.copy(alpha = 0.10f), LIGHT_ADJACENT)
        assertBoundaryFails("design key border", DarkInk.copy(alpha = 0.10f), DARK_ADJACENT)
    }

    /**
     * The armed key ring, and the reason a per-theme alpha was not the fix.
     *
     * At the design's 55% the ring fails in light at 1.78:1. In dark it fails too, at 2.61:1 — but
     * only because `keyBase` hardcodes the **light** triple `rgba(174,140,79,.55)` on an element
     * whose sibling `padWrapStyle` uses the theme-aware `var(--gold)`. The theme's own dark gold at
     * the same alpha would have reached 3.30:1 and passed.
     *
     * So 55% is salvageable in dark and unsalvageable in light. Full theme gold fixes both and is
     * one value rather than two (D-5).
     */
    @Test
    fun theDesignsArmedKeyRingFailsInLightAndFailsInDarkOnlyBecauseItsGoldIsHardcoded() {
        assertEquals(1.78, round2(ratio(LightGold.copy(alpha = 0.55f), LightCard)), 0.001)
        assertEquals(1.76, round2(ratio(LightGold.copy(alpha = 0.55f), LightBg)), 0.001)
        assertBoundaryFails("design armed ring, light", LightGold.copy(alpha = 0.55f), LIGHT_ADJACENT)

        // What the prototype actually paints in dark: the light gold.
        assertEquals(2.61, round2(ratio(LightGold.copy(alpha = 0.55f), DarkCard)), 0.001)
        assertBoundaryFails(
            "design armed ring, dark as the prototype paints it",
            LightGold.copy(alpha = 0.55f),
            DARK_ADJACENT
        )

        // What the same alpha would have measured had the prototype used its own theme gold.
        assertEquals(3.30, round2(ratio(DarkGold.copy(alpha = 0.55f), DarkCard)), 0.001)
        assertTrue(ratio(DarkGold.copy(alpha = 0.55f), DarkCard) >= AA_NON_TEXT)
    }

    /** `toggleBase`'s at-rest border at line 1719 of the design authority. */
    @Test
    fun theDesignsSleepWakeBorderFailsTheControlBoundaryFloor() {
        assertEquals(1.32, round2(ratio(LightInk.copy(alpha = 0.13f), LightCard)), 0.001)
        assertEquals(1.41, round2(ratio(DarkInk.copy(alpha = 0.13f), DarkCard)), 0.001)

        assertBoundaryFails("design toggle border", LightInk.copy(alpha = 0.13f), LIGHT_ADJACENT)
        assertBoundaryFails("design toggle border", DarkInk.copy(alpha = 0.13f), DARK_ADJACENT)
    }

    /** `chipBtn`'s unselected border at line 1223. An unselected chip has no fill. */
    @Test
    fun theDesignsUnselectedChipBorderFailsTheControlBoundaryFloor() {
        assertEquals(1.35, round2(ratio(LightInk.copy(alpha = 0.14f), LightCard)), 0.001)
        assertEquals(1.48, round2(ratio(DarkInk.copy(alpha = 0.14f), DarkCard)), 0.001)

        assertBoundaryFails("design chip border", LightInk.copy(alpha = 0.14f), LIGHT_ADJACENT)
        assertBoundaryFails("design chip border", DarkInk.copy(alpha = 0.14f), DARK_ADJACENT)
    }

    /** The breathing pill at line 170, and `MsPillButton`'s Phase 15 unselected border. */
    @Test
    fun theDesignsPillBorderFailsTheControlBoundaryFloor() {
        assertEquals(1.60, round2(ratio(LightGold.copy(alpha = 0.45f), LightCard)), 0.001)
        assertEquals(1.57, round2(ratio(LightGold.copy(alpha = 0.45f), LightBg)), 0.001)
        assertEquals(2.61, round2(ratio(DarkGold.copy(alpha = 0.45f), DarkCard)), 0.001)

        assertBoundaryFails("design pill border", LightGold.copy(alpha = 0.45f), LIGHT_ADJACENT)
        assertBoundaryFails("design pill border", DarkGold.copy(alpha = 0.45f), DARK_ADJACENT)
    }

    /**
     * One alpha covers three separate controls: Log's From/To underline at line 257, the marker
     * input's border at line 145, and the help button's ring at line 84. All three are the only
     * edge their control has, and `outlineDecorative` holds exactly this value — which is why D-6
     * of the foundation carves out that an ink border which is the *only* boundary of an
     * interactive control is not decorative and must use `outline` instead.
     */
    @Test
    fun theDesignsSixteenPercentInkFailsAsAControlBoundary() {
        assertEquals(1.41, round2(ratio(LightInk.copy(alpha = 0.16f), LightCard)), 0.001)
        assertEquals(1.57, round2(ratio(DarkInk.copy(alpha = 0.16f), DarkCard)), 0.001)

        assertEquals(LightPalette.outlineDecorative, LightInk.copy(alpha = 0.16f))
        assertEquals(DarkPalette.outlineDecorative, DarkInk.copy(alpha = 0.16f))

        assertBoundaryFails("outlineDecorative", LightPalette.outlineDecorative, LIGHT_ADJACENT)
        assertBoundaryFails("outlineDecorative", DarkPalette.outlineDecorative, DARK_ADJACENT)
    }

    // ── the adopted replacements (D-4, second table) ─────────────────────────

    @Test
    fun theAdoptedInkBoundaryClearsTheFloorAgainstBothAdjacentSurfaces() {
        assertBoundaryPasses("outline", LightPalette.outline, LIGHT_ADJACENT)
        assertBoundaryPasses("outline", DarkPalette.outline, DARK_ADJACENT)

        assertEquals(3.49, round2(ratio(LightPalette.outline, LightCard)), 0.001)
        assertEquals(3.47, round2(ratio(LightPalette.outline, LightBg)), 0.001)
        assertEquals(3.51, round2(ratio(DarkPalette.outline, DarkCard)), 0.001)
        assertEquals(3.47, round2(ratio(DarkPalette.outline, DarkBg)), 0.001)
    }

    @Test
    fun theAdoptedGoldBoundaryClearsTheFloorAgainstBothAdjacentSurfaces() {
        assertBoundaryPasses("gold", LightPalette.gold, LIGHT_ADJACENT)
        assertBoundaryPasses("gold", DarkPalette.gold, DARK_ADJACENT)

        assertEquals(3.15, round2(ratio(LightPalette.gold, LightCard)), 0.001)
        assertEquals(3.05, round2(ratio(LightPalette.gold, LightBg)), 0.001)
        assertEquals(8.04, round2(ratio(DarkPalette.gold, DarkCard)), 0.001)
        assertEquals(8.60, round2(ratio(DarkPalette.gold, DarkBg)), 0.001)
    }

    /**
     * D-4's first constraint. Light gold clears 3:1 on the page and on the card and then fails on
     * the dimmest container, so a gold control boundary is legal only on `bg` and `card`. Nothing
     * in Track or Log paints one on a container step; this test is what stops a later phase from
     * doing so without re-measuring.
     */
    @Test
    fun fullLightGoldIsNotALegalControlBoundaryOnTheDimmestContainer() {
        assertEquals(2.67, round2(ratio(LightPalette.gold, LIGHT_DIMMEST)), 0.001)
        assertTrue(ratio(LightPalette.gold, LIGHT_DIMMEST) < AA_NON_TEXT)
    }

    // ── the decorative exemptions (D-4, third table) ─────────────────────────

    /**
     * `dotStyle`'s ring at line 1240 and the kind badge's border at line 202 stay at the design's
     * alphas. Neither is a control: the entry row is not clickable, its three actions are separate
     * nodes, and a badge is a mark around text that is itself at least 4.5:1.
     *
     * Asserted as *below* 3:1 on purpose. The exemption is a decision, and a later phase that
     * "fixed" these into heavy borders would lose the design's most characteristic mark and would
     * break this test on the way. Note how close the dark dot ring comes at 2.96:1 — it is exempt
     * because it is decorative, not because it is faint.
     */
    @Test
    fun theDecorativeGoldMarksStayFaintAndAreExempt() {
        assertEquals(1.69, round2(ratio(LightGold.copy(alpha = 0.50f), LightCard)), 0.001)
        assertEquals(2.96, round2(ratio(DarkGold.copy(alpha = 0.50f), DarkCard)), 0.001)
        assertEquals(1.50, round2(ratio(LightGold.copy(alpha = 0.40f), LightCard)), 0.001)
        assertEquals(2.33, round2(ratio(DarkGold.copy(alpha = 0.40f), DarkCard)), 0.001)

        assertTrue(ratio(LightGold.copy(alpha = 0.50f), LightCard) < AA_NON_TEXT)
        assertTrue(ratio(DarkGold.copy(alpha = 0.50f), DarkCard) < AA_NON_TEXT)
        assertTrue(ratio(LightGold.copy(alpha = 0.40f), LightCard) < AA_NON_TEXT)
        assertTrue(ratio(DarkGold.copy(alpha = 0.40f), DarkCard) < AA_NON_TEXT)
    }

    /** The row and day separators. `MsHairline` and `ms.hairline` carry these. */
    @Test
    fun theRowSeparatorsStayFaintAndAreExempt() {
        assertTrue(ratio(LightPalette.hairline, LightCard) < AA_NON_TEXT)
        assertTrue(ratio(DarkPalette.hairline, DarkCard) < AA_NON_TEXT)
    }

    // ── the emphasis levels this phase substitutes for failing text alphas ───

    /**
     * Three of the design's text alphas in these two screens are below the 4.5:1 text floor and
     * are replaced by the faintest compliant emphasis level rather than by a new value: the entry
     * dot's zero at `rgba(ink,.4)`, Log's sleep em-dash at `rgba(ink,.28)`, and the unavailable
     * Wake label at `rgba(ink,.28)` (D-8, D-12, D-14).
     */
    @Test
    fun theFailingTextAlphasAreReplacedByCompliantEmphasisLevels() {
        assertEquals(2.58, round2(ratio(LightInk.copy(alpha = 0.40f), LightCard)), 0.001)
        assertEquals(1.88, round2(ratio(LightInk.copy(alpha = 0.28f), LightCard)), 0.001)
        assertEquals(2.35, round2(ratio(DarkInk.copy(alpha = 0.28f), DarkCard)), 0.001)

        assertTrue(ratio(LightInk.copy(alpha = 0.40f), LightBg) < AA_NORMAL_TEXT)
        assertTrue(ratio(LightInk.copy(alpha = 0.28f), LightBg) < AA_NORMAL_TEXT)
        assertTrue(ratio(DarkInk.copy(alpha = 0.28f), DarkBg) < AA_NORMAL_TEXT)

        assertTrue(ratio(LightPalette.inkQuaternary, LightBg) >= AA_NORMAL_TEXT)
        assertTrue(ratio(DarkPalette.inkQuaternary, DarkBg) >= AA_NORMAL_TEXT)
        // The ordering the design expresses survives being made compliant: the receded level is
        // still visibly fainter than the level above it.
        assertTrue(
            ratio(LightPalette.inkQuaternary, LightBg) < ratio(LightPalette.inkTertiary, LightBg)
        )
        assertTrue(
            ratio(DarkPalette.inkQuaternary, DarkBg) < ratio(DarkPalette.inkTertiary, DarkBg)
        )
    }
}
