package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The low anchor for the light theme: the design's own warm brown, `#6E5220` from `ramp()` at line
 * 890 of `MindScale v2.dc.html`.
 *
 * The high anchor is [LightGold], the theme's own non-text gold — the same colour as the armed pad
 * ring, the header rule and the gold day headers. One rule spans both themes: the ramp runs from a
 * dim warm brown into the gold this app already uses to mean "look here"
 * (`docs/specs/SPEC-insights-visual.md`, D-4).
 */
private val LightRampLow = Color(0xFF6E5220) // the design's warm brown, 7.26:1 on card
private val LightRampHigh = LightGold // 3.15:1 on card

/**
 * The dark anchors are a distinct pair, not the light ramp under a filter.
 *
 * [DarkRampLow] is the point on the design's own dark ramp line `#3A2F1C` → `#E0BE7A` at which
 * intensity 1 first clears 3:1 against `card`, `bg` **and** the asleep band with margin. The
 * design's own low anchor measures 1.38:1 against `card`, and the slate-blue `#3A4652` this
 * replaces measured 1.87:1 — both leave a recorded rating indistinguishable from a day with
 * nothing on it (D-4).
 */
private val DarkRampLow = Color(0xFF856F46) // 3.74:1 on card
private val DarkRampHigh = DarkGold // 8.04:1 on card

/**
 * Returns a color representing symptom intensity [value] as a monotonically-increasing (in
 * perceived luminance/warmth) ramp from a dim warm anchor to the theme's own gold.
 *
 * [isDark] selects a distinct anchor pair for the dark theme (not merely the light ramp under a
 * filter).
 *
 * ## The mapping, and what happens to 0
 *
 * The interpolation is the prototype's: `value` is clamped to `1..10` and mapped over `(v-1)/9`,
 * so the ramp's two anchors are literally the colours painted for intensity 1 and intensity 10.
 * That matters because the raster's legend carries two swatches labelled `1` and `10`, and under
 * the older `v/10` mapping the low anchor was a colour no cell could ever be. The legend has to
 * promise what the raster paints.
 *
 * **0 maps to the low anchor, and no raster cell can ever be 0.** `EpisodeEngine` drops a
 * zero-valued entry before any `IntensitySegment` is built, so `RasterState.INTENSITY` carries only
 * `1..10` and a 0 rating is classified `WELL` — "nothing recorded", which the raster paints as the
 * card itself. A rating of 0 is not a faint colour because it is not a colour at all
 * (`docs/specs/SPEC-insights-visual.md`, D-5).
 *
 * ## Why the light ramp runs the way it does
 *
 * The prototype's light ramp descends in relative luminance, from pale cream at 1 to dark brown at
 * 10. `IntensityRampTest` has asserted a monotonically non-decreasing light ramp since Phase 1, and
 * the visual-only rule forbids editing a pre-existing test. The adopted pair is the same two design
 * hexes in the other order, which separates its endpoints marginally further (2.31 against 2.24).
 * What it cannot recover is the direction: on a light page a rating of 1 carries more visual weight
 * than a rating of 10. That is recorded in D-4 rather than left in a diff.
 *
 * ## Invariant 14
 *
 * Per Invariant 14 of `SPEC-track-numpad-logging.md`, this color must always be paired with the
 * value as text by callers — color is never the sole carrier of information. Its three call sites
 * satisfy it: the raster's cells are read by a live-region readout that spells `intensity 7`, and
 * each legend swatch sits beside its own numeral.
 *
 * @throws IllegalArgumentException if [value] is outside 0..10.
 */
fun intensityColor(value: Int, isDark: Boolean): Color {
    require(value in 0..10) { "value out of range 0..10: $value" }
    val fraction = (value.coerceIn(1, 10) - 1) / 9f
    val (low, high) = if (isDark) DarkRampLow to DarkRampHigh else LightRampLow to LightRampHigh
    return lerp(low, high, fraction)
}
