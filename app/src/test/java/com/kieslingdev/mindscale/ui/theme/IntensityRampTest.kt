package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IntensityRampTest {

    @Test
    fun `light ramp luminance is monotonically non-decreasing from 0 to 10`() {
        val luminances = (0..10).map { intensityColor(it, isDark = false).luminance() }
        for (i in 1 until luminances.size) {
            assertTrue(
                "expected luminance($i) >= luminance(${i - 1}) but was ${luminances[i]} < ${luminances[i - 1]}",
                luminances[i] >= luminances[i - 1]
            )
        }
    }

    @Test
    fun `dark ramp luminance is monotonically non-decreasing from 0 to 10`() {
        val luminances = (0..10).map { intensityColor(it, isDark = true).luminance() }
        for (i in 1 until luminances.size) {
            assertTrue(
                "expected luminance($i) >= luminance(${i - 1}) but was ${luminances[i]} < ${luminances[i - 1]}",
                luminances[i] >= luminances[i - 1]
            )
        }
    }

    @Test
    fun `light and dark ramps use distinct anchor colors`() {
        assertTrue(intensityColor(0, isDark = false) != intensityColor(0, isDark = true))
        assertTrue(intensityColor(10, isDark = false) != intensityColor(10, isDark = true))
    }

    @Test
    fun `negative value throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) { intensityColor(-1, isDark = false) }
    }

    @Test
    fun `11 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) { intensityColor(11, isDark = false) }
    }
}
