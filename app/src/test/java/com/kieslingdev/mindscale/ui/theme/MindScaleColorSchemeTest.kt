package com.kieslingdev.mindscale.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `docs/specs/SPEC-visual-foundation.md` D-9 and Invariant 4: no `ColorScheme` role is left at a
 * Material default.
 *
 * The starting state was 13 of the roles set and the rest defaulted, which is how a
 * half-populated scheme leaks Material's purple into a warm bone-and-gold app. This is checked
 * by reflection rather than by review, so a role added by a future Material 3 release fails the
 * build instead of silently arriving with a default value.
 */
class MindScaleColorSchemeTest {

    /**
     * Every value either scheme is allowed to hold. A role assigned anything else — including a
     * Material default — is not in this set and fails.
     */
    private val approved: Set<Color> = setOf(
        // Light
        LightBg, LightCard, LightInk, LightOnInk, LightGold, LightGoldText, LightDanger,
        LightInk.copy(alpha = 0.70f), LightInk.copy(alpha = 0.50f), LightInk.copy(alpha = 0.09f),
        Color(0xFFF8F6F1), Color(0xFFF3F0EA), Color(0xFFEFECE6),
        Color(0xFFF2E3C4), Color(0xFFF4EAD7), Color(0xFFF7E2DE), Color(0xFF5C1F17),
        // Dark
        DarkBg, DarkCard, DarkInk, DarkOnInk, DarkGold, DarkGoldText, DarkDanger,
        DarkInk.copy(alpha = 0.70f), DarkInk.copy(alpha = 0.40f), DarkInk.copy(alpha = 0.09f),
        Color(0xFF0B0907), Color(0xFF161310), Color(0xFF201C17), Color(0xFF24211C),
        Color(0xFF49391F), Color(0xFF342B1D), Color(0xFF4A211B), Color(0xFFF7D9D4),
        // Shared
        Color.Transparent, Color(0xFF000000)
    )

    private fun rolesOf(scheme: ColorScheme): Map<String, Color> =
        ColorScheme::class.java.methods
            .filter {
                it.name.startsWith("get") &&
                    it.parameterCount == 0 &&
                    it.returnType == java.lang.Long.TYPE
            }
            .associate { method ->
                val role = method.name.removePrefix("get").substringBefore('-')
                role to Color((method.invoke(scheme) as Long).toULong())
            }

    @Test
    fun bothSchemesExposeTheFullRoleSet() {
        // Guards the reflection itself: if the filter stopped matching, the test would pass
        // vacuously over an empty map. Material 3 exposed 47 roles when Phase 15 was written —
        // 35 plus the 12 `…Fixed` roles that this test caught still holding Material's purple.
        assertTrue(rolesOf(MindScaleLightColorScheme).size >= 47)
        assertEquals(
            rolesOf(MindScaleLightColorScheme).keys,
            rolesOf(MindScaleDarkColorScheme).keys
        )
    }

    @Test
    fun everyLightRoleIsAMindScaleToken() {
        val strays = rolesOf(MindScaleLightColorScheme).filterValues { it !in approved }
        assertEquals(emptyMap<String, Color>(), strays)
    }

    @Test
    fun everyDarkRoleIsAMindScaleToken() {
        val strays = rolesOf(MindScaleDarkColorScheme).filterValues { it !in approved }
        assertEquals(emptyMap<String, Color>(), strays)
    }

    /**
     * D-13: Material's tonal-elevation overlay would tint every raised surface toward `primary`,
     * and the design is near-flat warm neutrals. Killing the tint at the token layer is what
     * makes near-flat elevation hold without auditing every call site.
     */
    @Test
    fun surfaceTintIsTransparentInBothThemes() {
        assertEquals(Color.Transparent, MindScaleLightColorScheme.surfaceTint)
        assertEquals(Color.Transparent, MindScaleDarkColorScheme.surfaceTint)
    }

    /**
     * D-5 and D-9: light and dark are two treatments, not one palette flipped. The inverse pair
     * is the design's selected-segment treatment, and its two `onInk` values are not each
     * other's inverse.
     */
    @Test
    fun theInversePairIsTheDesignsSelectedSegmentTreatment() {
        assertEquals(LightInk, MindScaleLightColorScheme.inverseSurface)
        assertEquals(LightOnInk, MindScaleLightColorScheme.inverseOnSurface)
        assertEquals(DarkInk, MindScaleDarkColorScheme.inverseSurface)
        assertEquals(DarkOnInk, MindScaleDarkColorScheme.inverseOnSurface)
        assertTrue(LightOnInk != DarkOnInk)
    }

    /** D-5: the page behind the simulated device frame is not an app surface. */
    @Test
    fun thePrototypesCanvasColourIsNowhereInTheApp() {
        val canvas = Color(0xFFE9E6DF)
        assertTrue(canvas !in rolesOf(MindScaleLightColorScheme).values)
        assertTrue(canvas !in rolesOf(MindScaleDarkColorScheme).values)
        assertTrue(canvas !in approved)
    }
}
