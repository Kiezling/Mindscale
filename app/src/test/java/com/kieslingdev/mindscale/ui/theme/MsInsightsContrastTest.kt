package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * `docs/specs/SPEC-insights-visual.md` D-7 and Invariant 5.
 *
 * `SPEC-track-and-log-visual.md` D-4 found that eight of the design's own borders fail
 * `SPEC-visual-foundation.md` D-23's 3:1 non-text floor, because almost every control in this
 * design is drawn as transparent-with-a-hairline and `card` on `bg` is 1.03:1 apart in light. The
 * same `rgba(ink,.14–.16)` and `rgba(gold,.85–.95)` idioms recur throughout Insights' raster,
 * charts and histograms. This test measures each of them against the tokens Phase 16 already
 * calibrated rather than inventing a second calibration, and it pins the exemptions so they stay
 * decisions rather than becoming oversights.
 *
 * The measurement method is deliberately identical to [MsControlBoundaryContrastTest]'s.
 */
class MsInsightsContrastTest {

    private companion object {
        const val AA_NON_TEXT = 3.0

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

    private fun assertPasses(label: String, mark: Color, adjacent: List<Pair<String, Color>>) {
        adjacent.forEach { (name, surface) ->
            val r = ratio(mark, surface)
            assertTrue(
                "$label against $name measured ${round2(r)}:1, below $AA_NON_TEXT:1",
                r >= AA_NON_TEXT
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
     * `chipStyleBase` at line 1512 of the design authority, which the range chips and — by the
     * same idiom — the histogram cells use. An unselected chip and an unselected histogram cell
     * are both unfilled, so in both cases the border **is** the control's only boundary. This is
     * the same alpha `MsChip` already moved off in Phase 16, re-measured here because Insights is
     * the first screen to put a *cell* on it as well as a chip.
     */
    @Test
    fun theDesignsUnselectedChipAndCellBorderFailsTheControlBoundaryFloor() {
        assertEquals(1.41, round2(ratio(LightInk.copy(alpha = 0.16f), LightCard)), 0.001)
        assertEquals(1.57, round2(ratio(DarkInk.copy(alpha = 0.16f), DarkCard)), 0.001)

        assertFails("design chip/cell border", LightInk.copy(alpha = 0.16f), LIGHT_ADJACENT)
        assertFails("design chip/cell border", DarkInk.copy(alpha = 0.16f), DARK_ADJACENT)
    }

    /**
     * The raster legend's "nothing" swatch at line 1377. The swatch is `card` on `card`: without a
     * boundary it is not a swatch at all, which makes its border a graphical object required to
     * understand the legend rather than a decoration.
     */
    @Test
    fun theDesignsLegendSwatchBorderFailsTheGraphicalObjectFloor() {
        assertEquals(1.35, round2(ratio(LightInk.copy(alpha = 0.14f), LightCard)), 0.001)
        assertEquals(1.48, round2(ratio(DarkInk.copy(alpha = 0.14f), DarkCard)), 0.001)

        assertFails("design legend swatch border", LightInk.copy(alpha = 0.14f), LIGHT_ADJACENT)
        assertFails("design legend swatch border", DarkInk.copy(alpha = 0.14f), DARK_ADJACENT)
    }

    /**
     * The histogram bar fill at lines 1456 and 1469, and the entry chart's step line at line 400.
     * These are not borders — they are the marks the charts exist to draw — and the design paints
     * both at partial gold. Light is where they fail.
     */
    @Test
    fun theDesignsPartialGoldDataMarksFailInLight() {
        assertEquals(2.59, round2(ratio(LightGold.copy(alpha = 0.85f), LightCard)), 0.001)
        assertEquals(2.50, round2(ratio(LightGold.copy(alpha = 0.85f), LightBg)), 0.001)
        assertEquals(2.94, round2(ratio(LightGold.copy(alpha = 0.95f), LightCard)), 0.001)
        assertEquals(2.84, round2(ratio(LightGold.copy(alpha = 0.95f), LightBg)), 0.001)

        assertFails("design bar fill", LightGold.copy(alpha = 0.85f), LIGHT_ADJACENT)
        assertFails("design step line", LightGold.copy(alpha = 0.95f), LIGHT_ADJACENT)

        // Dark passes at both alphas, so this is a light-theme failure specifically. Full gold is
        // adopted anyway, because one value legal in both themes beats an alpha legal in one —
        // the same reasoning D-5 of the Track and Log spec recorded for the armed key ring.
        assertEquals(6.14, round2(ratio(DarkGold.copy(alpha = 0.85f), DarkCard)), 0.001)
        assertEquals(7.38, round2(ratio(DarkGold.copy(alpha = 0.95f), DarkCard)), 0.001)
        assertTrue(ratio(DarkGold.copy(alpha = 0.85f), DarkCard) >= AA_NON_TEXT)
    }

    /** The entry chart's dashed event lines at line 398. They mark something the user recorded. */
    @Test
    fun theDesignsEventLineFailsTheGraphicalObjectFloor() {
        assertEquals(2.24, round2(ratio(LightInk.copy(alpha = 0.35f), LightCard)), 0.001)
        assertEquals(2.97, round2(ratio(DarkInk.copy(alpha = 0.35f), DarkCard)), 0.001)

        assertFails("design event line", LightInk.copy(alpha = 0.35f), LIGHT_ADJACENT)
        assertFails("design event line", DarkInk.copy(alpha = 0.35f), DARK_ADJACENT)
    }

    // ── the adopted replacements (D-7, second table) ─────────────────────────

    /**
     * Both replacements are tokens Phase 16 already calibrated and already pinned. This phase
     * reuses them rather than inventing a third ink alpha or a second gold, which is why these
     * figures are identical to `MsControlBoundaryContrastTest`'s.
     */
    @Test
    fun theAdoptedBoundariesAndDataMarksClearTheFloorInBothThemes() {
        assertPasses("outline", LightPalette.outline, LIGHT_ADJACENT)
        assertPasses("outline", DarkPalette.outline, DARK_ADJACENT)
        assertPasses("gold", LightPalette.gold, LIGHT_ADJACENT)
        assertPasses("gold", DarkPalette.gold, DARK_ADJACENT)

        assertEquals(3.49, round2(ratio(LightPalette.outline, LightCard)), 0.001)
        assertEquals(3.51, round2(ratio(DarkPalette.outline, DarkCard)), 0.001)
        assertEquals(3.15, round2(ratio(LightPalette.gold, LightCard)), 0.001)
        assertEquals(8.04, round2(ratio(DarkPalette.gold, DarkCard)), 0.001)
    }

    /**
     * D-7's carried-forward constraint, re-checked because Insights paints more gold marks than
     * any other screen: full light gold stops being a legal boundary on a container step. Every
     * gold mark this phase paints — bar fills, the step line, day-header labels — sits on `card`
     * or `bg`.
     */
    @Test
    fun fullLightGoldIsStillNotLegalOnTheDimmestContainer() {
        val dimmest = MindScaleLightColorScheme.surfaceContainerHighest
        assertEquals(2.67, round2(ratio(LightPalette.gold, dimmest)), 0.001)
        assertTrue(ratio(LightPalette.gold, dimmest) < AA_NON_TEXT)
    }

    // ── the deliberate exemptions (D-7, third table) ─────────────────────────

    /**
     * The raster's three quiet-ground states. Each is a *named* state — the legend labels it and
     * the live readout speaks it — which is Invariant 14's own remedy and is what makes the
     * exemption legal rather than convenient. `FUTURE` additionally carries 45° hatching, a signal
     * that is not colour at all.
     *
     * Asserted as *below* 3:1 on purpose. A later phase that "fixed" the asleep band into a heavy
     * block would lose the design's quietest surface and break this test on the way.
     */
    @Test
    fun theRastersQuietGroundStaysFaintAndIsExempt() {
        assertEquals(1.39, round2(ratio(LightSleepBand, LightCard)), 0.001)
        assertEquals(1.12, round2(ratio(DarkSleepBand, DarkCard)), 0.001)

        val lightNoData = MindScaleLightColorScheme.surfaceVariant.copy(alpha = 0.45f)
        val darkNoData = MindScaleDarkColorScheme.surfaceVariant.copy(alpha = 0.45f)
        assertEquals(1.06, round2(ratio(lightNoData, LightCard)), 0.001)
        assertEquals(1.03, round2(ratio(darkNoData, DarkCard)), 0.001)

        assertEquals(1.18, round2(ratio(LightInk.copy(alpha = 0.08f), LightCard)), 0.001)
        assertEquals(1.48, round2(ratio(DarkInk.copy(alpha = 0.14f), DarkCard)), 0.001)

        assertFails("sleepBand", LightSleepBand, LIGHT_ADJACENT)
        assertFails("sleepBand", DarkSleepBand, DARK_ADJACENT)
        assertFails("no data", lightNoData, LIGHT_ADJACENT)
        assertFails("no data", darkNoData, DARK_ADJACENT)
        assertFails("future", LightInk.copy(alpha = 0.08f), LIGHT_ADJACENT)
        assertFails("future", DarkInk.copy(alpha = 0.14f), DARK_ADJACENT)
    }

    /**
     * The entry chart's gridlines, sleep columns and zero baseline. Separators and ground beside a
     * `10`/`5`/`0` text axis that is itself at least 4.5:1, so D-6 of the foundation's decorative
     * exemption applies unchanged.
     */
    @Test
    fun theChartsGridAndBaselineStayFaintAndAreExempt() {
        assertEquals(1.20, round2(ratio(LightPalette.hairline, LightCard)), 0.001)
        assertEquals(1.26, round2(ratio(DarkPalette.hairline, DarkCard)), 0.001)
        assertEquals(1.41, round2(ratio(LightPalette.outlineDecorative, LightCard)), 0.001)
        assertEquals(1.57, round2(ratio(DarkPalette.outlineDecorative, DarkCard)), 0.001)

        assertFails("chart gridline", LightPalette.hairline, LIGHT_ADJACENT)
        assertFails("chart gridline", DarkPalette.hairline, DARK_ADJACENT)
        assertFails("chart zero baseline", LightPalette.outlineDecorative, LIGHT_ADJACENT)
        assertFails("chart zero baseline", DarkPalette.outlineDecorative, DARK_ADJACENT)
    }
}
