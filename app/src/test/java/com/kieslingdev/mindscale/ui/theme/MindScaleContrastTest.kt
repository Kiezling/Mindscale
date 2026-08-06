package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * `docs/specs/SPEC-visual-foundation.md` D-6, D-7, D-8, D-23, and Invariant 5.
 *
 * The prototype's emphasis hierarchy is `rgba(var(--ink-rgb), .35–.45)`. Measured, ink `#17130C`
 * at `.45` over `#FCFBF9` is 2.96:1 — a WCAG AA failure at any text size, applied to 9.5 sp
 * text. This test is the reason that cannot come back: it recomputes every text colour against
 * every surface it can be painted on and fails below 4.5:1.
 */
class MindScaleContrastTest {

    private companion object {
        const val AA_NORMAL_TEXT = 4.5
        const val AA_NON_TEXT = 3.0

        val LIGHT_SURFACES = listOf(
            "bg" to LightBg,
            "card" to LightCard,
            "surfaceContainer" to Color(0xFFF8F6F1),
            "surfaceContainerHigh" to Color(0xFFF3F0EA),
            "surfaceContainerHighest" to Color(0xFFEFECE6)
        )

        val DARK_SURFACES = listOf(
            "bg" to DarkBg,
            "card" to DarkCard,
            "surfaceContainerLowest" to Color(0xFF0B0907),
            "surfaceContainerLow" to Color(0xFF161310),
            "surfaceContainerHigh" to Color(0xFF201C17),
            "surfaceContainerHighest" to Color(0xFF24211C)
        )
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

    private fun assertReadable(
        label: String,
        fg: Color,
        surfaces: List<Pair<String, Color>>,
        floor: Double = AA_NORMAL_TEXT
    ) {
        surfaces.forEach { (name, bg) ->
            val r = ratio(fg, bg)
            assertTrue(
                "$label on $name measured ${round2(r)}:1, below $floor:1",
                r >= floor
            )
        }
    }

    // ── the four-level emphasis scale (D-6) ──────────────────────────────────

    @Test
    fun everyLightEmphasisLevelIsReadableOnEverySurface() {
        assertReadable("inkPrimary", LightPalette.inkPrimary, LIGHT_SURFACES)
        assertReadable("inkSecondary", LightPalette.inkSecondary, LIGHT_SURFACES)
        assertReadable("inkTertiary", LightPalette.inkTertiary, LIGHT_SURFACES)
        assertReadable("inkQuaternary", LightPalette.inkQuaternary, LIGHT_SURFACES)
    }

    @Test
    fun everyDarkEmphasisLevelIsReadableOnEverySurface() {
        assertReadable("inkPrimary", DarkPalette.inkPrimary, DARK_SURFACES)
        assertReadable("inkSecondary", DarkPalette.inkSecondary, DARK_SURFACES)
        assertReadable("inkTertiary", DarkPalette.inkTertiary, DARK_SURFACES)
        assertReadable("inkQuaternary", DarkPalette.inkQuaternary, DARK_SURFACES)
    }

    /** The ordering the design expresses must survive being made compliant. */
    @Test
    fun theEmphasisScaleStaysMonotonic() {
        listOf(LightPalette to LightBg, DarkPalette to DarkBg).forEach { (p, bg) ->
            val levels = listOf(p.inkPrimary, p.inkSecondary, p.inkTertiary, p.inkQuaternary)
                .map { ratio(it, bg) }
            levels.zipWithNext { a, b ->
                assertTrue("emphasis ordering broke: $a then $b", a > b)
            }
        }
    }

    /** The exact figures tabulated in D-6, so the spec cannot drift from the code. */
    @Test
    fun theTabulatedEmphasisRatiosAreReproduced() {
        assertEquals(17.89, round2(ratio(LightPalette.inkPrimary, LightBg)), 0.01)
        assertEquals(9.80, round2(ratio(LightPalette.inkSecondary, LightBg)), 0.01)
        assertEquals(6.85, round2(ratio(LightPalette.inkTertiary, LightBg)), 0.01)
        assertEquals(4.77, round2(ratio(LightPalette.inkQuaternary, LightBg)), 0.01)

        assertEquals(16.96, round2(ratio(DarkPalette.inkPrimary, DarkBg)), 0.01)
        assertEquals(10.95, round2(ratio(DarkPalette.inkSecondary, DarkBg)), 0.01)
        assertEquals(8.60, round2(ratio(DarkPalette.inkTertiary, DarkBg)), 0.01)
        assertEquals(6.53, round2(ratio(DarkPalette.inkQuaternary, DarkBg)), 0.01)
    }

    /**
     * The floor of the whole scale: the faintest text level on the dimmest surface it can be
     * painted on. This is the single number the emphasis scale exists to hold above 4.5.
     */
    @Test
    fun theFaintestLevelOnTheDimmestSurfaceStillClearsAa() {
        assertEquals(4.58, round2(ratio(LightPalette.inkQuaternary, Color(0xFFEFECE6))), 0.01)
        assertEquals(5.94, round2(ratio(DarkPalette.inkQuaternary, Color(0xFF24211C))), 0.01)
    }

    /** The alpha the prototype actually uses, kept here as the reason the scale exists. */
    @Test
    fun theProtoypesOwnAlphaWouldFail() {
        val prototype = LightInk.copy(alpha = 0.45f)
        assertEquals(2.99, round2(ratio(prototype, LightBg)), 0.01)
        assertTrue(ratio(prototype, LightBg) < AA_NORMAL_TEXT)
    }

    // ── gold (D-7) ───────────────────────────────────────────────────────────

    @Test
    fun goldTextIsReadableInBothThemes() {
        assertReadable("light goldText", LightPalette.goldText, LIGHT_SURFACES)
        assertReadable("dark goldText", DarkPalette.goldText, DARK_SURFACES)
        assertEquals(5.35, round2(ratio(LightGoldText, LightBg)), 0.01)
        assertEquals(8.60, round2(ratio(DarkGoldText, DarkBg)), 0.01)
    }

    /**
     * `#886D3E` is the value the calibration reached first, and it passes on `bg` and `card`. It
     * is recorded here as rejected because it drops to 4.14:1 on `surfaceContainerHighest`: a
     * token safe on only two of five surfaces is a token that will eventually be painted on the
     * third (D-7).
     */
    @Test
    fun theLighterGoldCandidateWasRejectedForTheDimmestSurface() {
        val rejected = Color(0xFF886D3E)
        assertTrue(ratio(rejected, LightBg) >= AA_NORMAL_TEXT)
        assertTrue(ratio(rejected, Color(0xFFEFECE6)) < AA_NORMAL_TEXT)
    }

    /** The design's own `--gold-deep`, which is why light-theme gold text diverges. */
    @Test
    fun theDesignsGoldDeepWouldFailAsLightThemeText() {
        val goldDeep = Color(0xFF9A7B44)
        assertEquals(3.84, round2(ratio(goldDeep, LightBg)), 0.01)
        assertTrue(ratio(goldDeep, LightBg) < AA_NORMAL_TEXT)
    }

    /** The non-text gold is held to the 3:1 control-boundary floor, not the text floor. */
    @Test
    fun theNonTextGoldClearsTheControlBoundaryFloor() {
        assertTrue(ratio(LightPalette.gold, LightCard) >= AA_NON_TEXT)
        assertTrue(ratio(DarkPalette.gold, DarkCard) >= AA_NON_TEXT)
    }

    // ── danger (D-8) ─────────────────────────────────────────────────────────

    @Test
    fun dangerIsReadableInBothThemes() {
        assertReadable("light danger", LightPalette.danger, LIGHT_SURFACES)
        assertReadable("dark danger", DarkPalette.danger, DARK_SURFACES)
        assertEquals(6.60, round2(ratio(LightDanger, LightBg)), 0.01)
        assertEquals(5.87, round2(ratio(DarkDanger, DarkBg)), 0.01)
    }

    /** The design has no dark-theme danger colour; reusing the light one fails. */
    @Test
    fun theLightDangerWouldFailOnDark() {
        assertEquals(2.83, round2(ratio(LightDanger, DarkBg)), 0.01)
        assertTrue(ratio(LightDanger, DarkBg) < AA_NORMAL_TEXT)
        assertTrue(DarkPalette.danger != LightPalette.danger)
    }

    // ── on-ink and outline (D-9, D-23) ───────────────────────────────────────

    @Test
    fun theInvertedSelectionTreatmentIsReadableInBothThemes() {
        assertEquals(11.98, round2(ratio(LightPalette.onInk, LightPalette.ink)), 0.01)
        assertEquals(13.93, round2(ratio(DarkPalette.onInk, DarkPalette.ink)), 0.01)
    }

    @Test
    fun theControlBoundaryOutlineClearsThreeToOne() {
        assertTrue(ratio(LightPalette.outline, LightCard) >= AA_NON_TEXT)
        assertTrue(ratio(DarkPalette.outline, DarkCard) >= AA_NON_TEXT)
    }

    /**
     * The decorative hairlines are deliberately *not* held to 3:1 — they are separators, not
     * control boundaries. Recorded as a test so the exemption is a decision rather than an
     * oversight someone later "fixes" into a heavy border.
     */
    @Test
    fun decorativeHairlinesAreExemptAndStayFaint() {
        assertTrue(ratio(LightPalette.hairline, LightCard) < AA_NON_TEXT)
        assertTrue(ratio(DarkPalette.hairline, DarkCard) < AA_NON_TEXT)
    }
}
