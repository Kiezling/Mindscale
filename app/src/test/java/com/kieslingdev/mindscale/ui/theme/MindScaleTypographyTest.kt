package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `docs/specs/SPEC-visual-foundation.md` D-10.
 *
 * The starting state was 1 of the 15 Material text styles overridden, and that one named
 * `FontFamily.Default`, which resolves to Roboto. The app therefore rendered its entire brand in
 * the system font.
 */
class MindScaleTypographyTest {

    private val allStyles: Map<String, TextStyle> = with(MindScaleTypography) {
        mapOf(
            "displayLarge" to displayLarge,
            "displayMedium" to displayMedium,
            "displaySmall" to displaySmall,
            "headlineLarge" to headlineLarge,
            "headlineMedium" to headlineMedium,
            "headlineSmall" to headlineSmall,
            "titleLarge" to titleLarge,
            "titleMedium" to titleMedium,
            "titleSmall" to titleSmall,
            "bodyLarge" to bodyLarge,
            "bodyMedium" to bodyMedium,
            "bodySmall" to bodySmall,
            "labelLarge" to labelLarge,
            "labelMedium" to labelMedium,
            "labelSmall" to labelSmall
        )
    }

    @Test
    fun allFifteenSlotsAreSet() {
        assertEquals(15, allStyles.size)
        allStyles.forEach { (name, style) ->
            assertNotNull("$name has no font family", style.fontFamily)
            assertNotNull("$name has no weight", style.fontWeight)
        }
    }

    @Test
    fun noSlotFallsBackToTheSystemFont() {
        allStyles.forEach { (name, style) ->
            assertTrue(
                "$name still resolves to the system font",
                style.fontFamily != FontFamily.Default
            )
            assertEquals("$name is not Instrument Sans", InstrumentSans, style.fontFamily)
        }
    }

    /**
     * D-10: every size is `sp` so it scales with the user's font setting, and every tracking is
     * `em` so it scales with the size instead of staying fixed while the glyphs grow.
     */
    @Test
    fun everySizeIsScalableAndEveryTrackingIsProportional() {
        allStyles.forEach { (name, style) ->
            assertEquals("$name size is not sp", TextUnitType.Sp, style.fontSize.type)
            assertEquals("$name line height is not sp", TextUnitType.Sp, style.lineHeight.type)
            assertEquals("$name tracking is not em", TextUnitType.Em, style.letterSpacing.type)
        }
    }

    /** The design's `font-variant-numeric: tabular-nums`, on every style that carries a figure. */
    @Test
    fun figureCarryingStylesUseTabularNumerals() {
        val numeric = allStyles.filterKeys { !it.startsWith("body") }
        assertEquals(12, numeric.size)
        numeric.forEach { (name, style) ->
            assertEquals("$name is not tabular", "tnum", style.fontFeatureSettings)
        }
    }

    /** Prose is deliberately proportional: tabular digits are wider than prose needs. */
    @Test
    fun proseIsNotTabular() {
        listOf("bodyLarge", "bodyMedium", "bodySmall").forEach {
            assertEquals(null, allStyles.getValue(it).fontFeatureSettings)
        }
    }

    /**
     * D-10: the identity is weight 500 for chrome, 400 for prose and the numpad glyph, and 600
     * survives on exactly one element — the selected bottom-navigation tab, which sets it at the
     * call site rather than through a slot.
     */
    @Test
    fun theWeightIdiomMatchesTheDesign() {
        listOf("bodyLarge", "bodyMedium", "bodySmall", "displaySmall").forEach {
            assertEquals("$it should be 400", FontWeight.Normal, allStyles.getValue(it).fontWeight)
        }
        (allStyles - setOf("bodyLarge", "bodyMedium", "bodySmall", "displaySmall")).forEach {
            assertEquals("${it.key} should be 500", FontWeight.Medium, it.value.fontWeight)
        }
    }

    @Test
    fun theTwoStylesWithNoMaterialSlotAreSet() {
        assertEquals(InstrumentSans, MindScaleTextStyles.wordmark.fontFamily)
        assertEquals(InstrumentSans, MindScaleTextStyles.toast.fontFamily)
        assertEquals(12f, MindScaleTextStyles.wordmark.fontSize.value, 0.001f)
        assertEquals(11.5f, MindScaleTextStyles.toast.fontSize.value, 0.001f)
    }

    /** The px-to-sp mapping from D-10, spot-checked against the design authority. */
    @Test
    fun theDesignsSizesSurviveTheUnitConversion() {
        assertEquals(26f, MindScaleTypography.displayLarge.fontSize.value, 0.001f)
        assertEquals(21f, MindScaleTypography.displaySmall.fontSize.value, 0.001f)
        assertEquals(13f, MindScaleTypography.titleSmall.fontSize.value, 0.001f)
        assertEquals(10.5f, MindScaleTypography.labelLarge.fontSize.value, 0.001f)
        assertEquals(9f, MindScaleTypography.labelSmall.fontSize.value, 0.001f)
    }
}
