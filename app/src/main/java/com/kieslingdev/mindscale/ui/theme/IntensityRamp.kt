package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Low-intensity anchor for the light theme: a muted, cool neutral.
 * High-intensity anchor for the light theme: the brand gold.
 *
 * These are intentionally not-yet-final brand hex values (see
 * docs/specs/SPEC-track-numpad-logging.md, "Not yet decided, non-blocking").
 */
private val LightRampLow = Color(0xFF6B7A8F) // muted slate-blue, low perceived intensity
private val LightRampHigh = Color(0xFFAE8C4F) // brand gold, light-theme variant

/**
 * Dark-theme anchors are a distinct pair, not the light ramp under a filter:
 * a darker desaturated low anchor (reads correctly on a dark surface) and a
 * brighter gold high anchor tuned for dark-theme legibility/contrast.
 */
private val DarkRampLow = Color(0xFF3A4652) // dark muted slate, low perceived intensity
private val DarkRampHigh = Color(0xFFC9A96A) // brand gold, dark-theme variant

/**
 * Returns a color representing symptom intensity [value] (0..10 inclusive) as a
 * monotonically-increasing (in perceived luminance/warmth) ramp from a muted "low"
 * anchor to a warm gold "high" anchor.
 *
 * [isDark] selects a distinct anchor pair for the dark theme (not merely the light
 * ramp under a filter).
 *
 * Per Invariant 14 of the governing spec, this color must always be paired with the
 * numeric value as text by callers — color is never the sole carrier of information.
 *
 * @throws IllegalArgumentException if [value] is outside 0..10.
 */
fun intensityColor(value: Int, isDark: Boolean): Color {
    require(value in 0..10) { "value out of range 0..10: $value" }
    val fraction = value / 10f
    val (low, high) = if (isDark) DarkRampLow to DarkRampHigh else LightRampLow to LightRampHigh
    return lerp(low, high, fraction)
}
