package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * `docs/specs/SPEC-insights-visual.md` D-4, D-5, D-6, and Invariants 6 through 8.
 *
 * `SPEC-visual-foundation.md` D-24 deferred the intensity ramp's colour mapping to this phase and
 * asked for two things explicitly: resolve the 0-versus-1 low anchor, and re-check whether the
 * prototype's warm low end is safe as a fill. This test is the arithmetic behind both answers, and
 * every figure in D-4's tables is produced here rather than by hand.
 *
 * The measurement method is deliberately identical to [MindScaleContrastTest]'s and
 * [MsControlBoundaryContrastTest]'s — same channel transfer function, same alpha compositing, same
 * rounding — so the three cannot disagree about what a ratio means.
 *
 * This test does **not** replace [IntensityRampTest], which is pre-existing, unedited, and still
 * the authority on the ramp's four structural properties. It is cited here because it is what
 * decides D-4: the prototype's light ramp descends in luminance and that test forbids it.
 */
class MsIntensityRampContrastTest {

    private companion object {
        /** `SPEC-visual-foundation.md` D-23: the floor for a mark that carries information. */
        const val AA_NON_TEXT = 3.0

        // The prototype's own `ramp()` anchors, line 890 of the design authority.
        val PROTO_LIGHT_LOW = Color(0xFFF0E4CC)
        val PROTO_LIGHT_HIGH = Color(0xFF6E5220)
        val PROTO_DARK_LOW = Color(0xFF3A2F1C)
        val PROTO_DARK_HIGH = Color(0xFFE0BE7A)

        /** The slate-blue low anchors this phase replaces, unchanged since Phase 1. */
        val SHIPPED_LIGHT_LOW = Color(0xFF6B7A8F)
        val SHIPPED_DARK_LOW = Color(0xFF3A4652)
    }

    private fun channel(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0

    /** The prototype maps `1..10` over `(v-1)/9`, which is the mapping D-4 adopts. */
    private fun protoRamp(value: Int, low: Color, high: Color): Color =
        lerp(low, high, (value.coerceIn(1, 10) - 1) / 9f)

    // ── D-4, finding 1: the prototype's low anchors are invisible in BOTH themes ──

    /**
     * D-24 flagged the light anchor. The dark one fails as well, which the brief did not
     * anticipate and which is the reason this phase raises both rather than one.
     *
     * `card` is the fill the raster paints for `RasterState.WELL` — "nothing recorded" — so a low
     * anchor that cannot be told apart from `card` renders a day the user *did* log as a day they
     * did not. That is not a cosmetic weakness in a symptom tracker.
     */
    @Test
    fun theDesignsLowAnchorsAreInvisibleAgainstNothingRecordedInBothThemes() {
        assertEquals(1.26, round2(ratio(PROTO_LIGHT_LOW, LightCard)), 0.001)
        assertEquals(1.22, round2(ratio(PROTO_LIGHT_LOW, LightBg)), 0.001)
        assertEquals(1.11, round2(ratio(PROTO_LIGHT_LOW, LightSleepBand)), 0.001)

        assertEquals(1.38, round2(ratio(PROTO_DARK_LOW, DarkCard)), 0.001)
        assertEquals(1.47, round2(ratio(PROTO_DARK_LOW, DarkBg)), 0.001)
        assertEquals(1.22, round2(ratio(PROTO_DARK_LOW, DarkSleepBand)), 0.001)

        listOf(
            Triple("design light low", PROTO_LIGHT_LOW, listOf(LightCard, LightBg, LightSleepBand)),
            Triple("design dark low", PROTO_DARK_LOW, listOf(DarkCard, DarkBg, DarkSleepBand))
        ).forEach { (label, anchor, surfaces) ->
            surfaces.forEach { surface ->
                assertTrue(
                    "$label measured ${round2(ratio(anchor, surface))}:1 and would have passed " +
                        "$AA_NON_TEXT:1 — if that is now true, D-4 needs revisiting rather than " +
                        "this test relaxing",
                    ratio(anchor, surface) < AA_NON_TEXT
                )
            }
        }
    }

    /**
     * D-4, finding 2, and the decision's actual cause.
     *
     * The prototype's light ramp runs pale to dark, so its relative luminance **descends**.
     * [IntensityRampTest] has asserted a monotonically non-decreasing light ramp since Phase 1, it
     * is a pre-existing file, and D-1 forbids editing it. So the design's light direction is not
     * available to this phase, whatever its merits.
     */
    @Test
    fun theDesignsLightRampDescendsInLuminanceAndWouldFailThePreExistingRampTest() {
        val first = luminance(protoRamp(1, PROTO_LIGHT_LOW, PROTO_LIGHT_HIGH))
        val last = luminance(protoRamp(10, PROTO_LIGHT_LOW, PROTO_LIGHT_HIGH))

        assertTrue(
            "the design's light ramp is expected to descend, but 1 measured $first and 10 $last",
            last < first
        )

        // The dark ramp ascends, which is why only the light direction is contested.
        assertTrue(
            luminance(protoRamp(10, PROTO_DARK_LOW, PROTO_DARK_HIGH)) >
                luminance(protoRamp(1, PROTO_DARK_LOW, PROTO_DARK_HIGH))
        )
    }

    /**
     * D-4, finding 3, restated after this test corrected it.
     *
     * The design's raw light pair separates its endpoints at **5.77:1**, far wider than anything
     * compliant — but it buys that width by putting one end at 1.26:1 against `card`, which is to
     * say by making it invisible. Raise that anchor to `#A28C65`, the palest point on the design's
     * own line that clears 3:1, and the design's direction separates at 2.24:1.
     *
     * So the cost is not the constraint's; it is compliance's. Against the compliant version of
     * the design's own direction, the adopted ramp separates marginally *further*.
     */
    @Test
    fun compliancePaysForEndpointSeparationAndTheAdoptedDirectionPaysNoMore() {
        val designAsDrawn = ratio(PROTO_LIGHT_LOW, PROTO_LIGHT_HIGH)
        val designMadeCompliant = ratio(Color(0xFFA28C65), PROTO_LIGHT_HIGH)
        val adopted = ratio(intensityColor(1, isDark = false), intensityColor(10, isDark = false))

        assertEquals(5.77, round2(designAsDrawn), 0.001)
        assertEquals(2.24, round2(designMadeCompliant), 0.001)
        assertEquals(2.31, round2(adopted), 0.001)

        // The raised anchor is the palest point on the design's own line that clears the floor.
        assertEquals(3.24, round2(ratio(Color(0xFFA28C65), LightCard)), 0.001)
        assertTrue(ratio(Color(0xFFA28C65), LightCard) >= AA_NON_TEXT)

        assertTrue(
            "the width the design's raw pair shows is bought with an invisible low end",
            designAsDrawn > adopted && ratio(PROTO_LIGHT_LOW, LightCard) < AA_NON_TEXT
        )
        assertTrue(
            "against a compliant version of the design's own direction, the adopted ramp should " +
                "separate at least as far",
            adopted >= designMadeCompliant
        )
    }

    /**
     * D-4, finding 4. The slate low anchor this phase replaces is compliant in light and has never
     * been compliant in dark. Found by measuring rather than by the brief.
     */
    @Test
    fun theShippedSlateDarkLowAnchorFailsAndTheLightOneDoesNot() {
        assertEquals(1.87, round2(ratio(SHIPPED_DARK_LOW, DarkCard)), 0.001)
        assertEquals(2.00, round2(ratio(SHIPPED_DARK_LOW, DarkBg)), 0.001)
        assertTrue(ratio(SHIPPED_DARK_LOW, DarkCard) < AA_NON_TEXT)

        assertEquals(4.37, round2(ratio(SHIPPED_LIGHT_LOW, LightCard)), 0.001)
        assertTrue(ratio(SHIPPED_LIGHT_LOW, LightCard) >= AA_NON_TEXT)
    }

    // ── the adopted ramp (D-4) ───────────────────────────────────────────────

    /**
     * Invariant 6: **every** intensity clears the floor against the surface that means "nothing
     * recorded" and against the page behind it, in its own theme. Every step is checked, not just
     * the anchors, because `lerp` interpolates in Oklab and an intermediate is not a value this
     * test can assume.
     */
    @Test
    fun everyIntensityClearsTheFloorAgainstCardAndBg() {
        listOf(
            Triple(false, LightCard, LightBg),
            Triple(true, DarkCard, DarkBg)
        ).forEach { (dark, card, bg) ->
            (1..10).forEach { value ->
                val colour = intensityColor(value, dark)
                listOf("card" to card, "bg" to bg).forEach { (name, surface) ->
                    val r = ratio(colour, surface)
                    assertTrue(
                        "intensity $value (dark=$dark) against $name measured ${round2(r)}:1, " +
                            "below $AA_NON_TEXT:1",
                        r >= AA_NON_TEXT
                    )
                }
            }
        }
    }

    /** The anchor figures D-4's adopted table records. `lerp` returns the anchors exactly at the ends. */
    @Test
    fun theAdoptedAnchorsMeasureWhatTheSpecRecords() {
        assertEquals(7.26, round2(ratio(intensityColor(1, isDark = false), LightCard)), 0.001)
        assertEquals(7.02, round2(ratio(intensityColor(1, isDark = false), LightBg)), 0.001)
        assertEquals(3.15, round2(ratio(intensityColor(10, isDark = false), LightCard)), 0.001)
        assertEquals(3.05, round2(ratio(intensityColor(10, isDark = false), LightBg)), 0.001)

        assertEquals(3.74, round2(ratio(intensityColor(1, isDark = true), DarkCard)), 0.001)
        assertEquals(4.00, round2(ratio(intensityColor(1, isDark = true), DarkBg)), 0.001)
        assertEquals(8.04, round2(ratio(intensityColor(10, isDark = true), DarkCard)), 0.001)
        assertEquals(8.60, round2(ratio(intensityColor(10, isDark = true), DarkBg)), 0.001)
    }

    /**
     * One rule for both themes: intensity 10 is painted **the theme's own gold** — the same colour
     * as the armed pad ring, the header rule, the day headers and the episode peak. The slate-blue
     * was the last non-brand hue in the app and it is gone.
     */
    @Test
    fun theHighAnchorIsTheThemesOwnGoldInBothThemes() {
        assertEquals(LightPalette.gold, intensityColor(10, isDark = false))
        assertEquals(DarkPalette.gold, intensityColor(10, isDark = true))
    }

    /**
     * D-5. `0` and `1` render the same colour, and no raster cell can ever be `0`:
     * `EpisodeEngine` drops a zero-valued entry before any `IntensitySegment` exists, so
     * `RasterState.INTENSITY` carries only `1..10` and a `0` rating is `WELL`, which the raster
     * paints as the card itself.
     *
     * The identity is asserted rather than assumed because the two legend swatches are labelled
     * `1` and `10`, and under the prototype's `(v-1)/9` mapping the swatch labelled `1` must be
     * literally the ramp's low anchor. The legend has to promise what the raster paints.
     */
    @Test
    fun zeroAndOneRenderTheSameColourAndOneIsTheLowAnchor() {
        assertEquals(intensityColor(0, isDark = false), intensityColor(1, isDark = false))
        assertEquals(intensityColor(0, isDark = true), intensityColor(1, isDark = true))

        // And the two themes still differ at both ends, which IntensityRampTest also asserts.
        assertNotEquals(intensityColor(0, isDark = false), intensityColor(0, isDark = true))
        assertNotEquals(intensityColor(10, isDark = false), intensityColor(10, isDark = true))
    }

    /**
     * D-6, consequence 2, asserted so it is a recorded decision rather than an oversight.
     *
     * A ten-step ramp cannot sit 3:1 from every other raster category at once. On a light page the
     * ramp spans the luminance band between its anchors, and no single colour is 3:1 from every
     * point of it except one lighter than `card` or darker than the darkest step. Light intensity
     * 10 against the asleep band is where that shows, and it is legal because "asleep" is a named
     * state the legend labels and the live readout speaks — Invariant 14's own remedy.
     */
    @Test
    fun theRampCannotClearTheFloorAgainstEveryCategoryAndTheExemptionIsDeliberate() {
        assertEquals(5.21, round2(ratio(intensityColor(1, isDark = false), LightSleepBand)), 0.001)
        assertEquals(2.26, round2(ratio(intensityColor(10, isDark = false), LightSleepBand)), 0.001)
        assertEquals(3.33, round2(ratio(intensityColor(1, isDark = true), DarkSleepBand)), 0.001)
        assertEquals(7.15, round2(ratio(intensityColor(10, isDark = true), DarkSleepBand)), 0.001)

        assertTrue(
            "if light intensity 10 now clears 3:1 against the asleep band, D-6's exemption is " +
                "no longer needed and should be removed rather than this test relaxed",
            ratio(intensityColor(10, isDark = false), LightSleepBand) < AA_NON_TEXT
        )
    }
}
