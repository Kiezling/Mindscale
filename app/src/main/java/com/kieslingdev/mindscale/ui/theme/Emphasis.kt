package com.kieslingdev.mindscale.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The MindScale tokens that Material's colour roles cannot express, read as `MaterialTheme.ms`.
 *
 * Material's ladder assumes containers step monotonically away from the background. MindScale's
 * card is *lighter* than the page in light and *darker*-then-lighter in dark, so the card lands
 * on `surfaceContainerLowest` in one theme and `surfaceContainer` in the other. Reading a role
 * that changes meaning between themes is exactly the drift this phase exists to end, so every
 * MindScale surface reads [card] and [bg] here instead, and the Material ladder is populated
 * separately and coherently for any stray Material component
 * (`docs/specs/SPEC-visual-foundation.md`, D-9).
 *
 * ## The emphasis scale (D-6)
 *
 * The prototype expresses its whole hierarchy as `rgba(var(--ink-rgb), a)` across 30 distinct
 * values from `.03` to `.62`, concentrated at `.35`, `.38`, `.40`, and `.45`. Measured, ink
 * `#17130C` at `.45` over `#FCFBF9` is 2.96:1 — a WCAG AA failure at any text size, applied to
 * 9.5 sp text. `SPEC-safety-card.md` D-13 and `SPEC-paced-breathing.md` D-14 already rejected
 * these alphas on their own screens; D-6 generalizes that rejection.
 *
 * Four levels replace the thirty, preserving the prototype's visible ordering at values that
 * pass AA for normal text on every surface in both themes. Measured light on bg / light on card
 * / dark on bg / dark on card:
 *
 * - [inkPrimary]     17.89 / 18.50 / 16.96 / 15.86
 * - [inkSecondary]    9.80 / 10.00 / 10.95 / 10.38
 * - [inkTertiary]     6.75 /  6.86 /  8.53 /  8.23
 * - [inkQuaternary]   4.77 /  4.85 /  6.53 /  6.34
 *
 * `MindScaleContrastTest` recomputes every one of these against every surface in the ladder and
 * fails if any drops below 4.5:1.
 */
@Immutable
data class MindScalePalette(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val onInk: Color,

    /**
     * The non-text gold: the header rule, the armed-pad ring, chip and card borders. Never used
     * for text — see [goldText] and D-7.
     */
    val gold: Color,

    /** The gold that text may be painted in. At least 4.5:1 on both [bg] and [card] (D-7). */
    val goldText: Color,

    /** At least 4.5:1 on both [bg] and [card]. The dark value is not the light one (D-8). */
    val danger: Color,

    val sleepBand: Color,
    val crosshair: Color,

    /** Full ink. Headings, values, the text a reader is meant to land on first. */
    val inkPrimary: Color,

    /** Replaces the design's `.55`–`.62`. Supporting copy that is still read. */
    val inkSecondary: Color,

    /** Replaces the design's `.45`–`.52`. Hints and secondary detail. */
    val inkTertiary: Color,

    /**
     * Replaces the design's `.30`–`.42` — the eyebrow labels and faint captions. The floor of
     * the scale, and the level that diverges most visibly from the prototype, which painted
     * these at 2.4:1 to 2.7:1.
     */
    val inkQuaternary: Color,

    /**
     * Non-text ink alphas, kept verbatim from the design because WCAG imposes no minimum on a
     * decorative separator. An ink border that is the *only* boundary of an interactive control
     * is not decorative and must use [outline] instead (D-6, D-23).
     */
    val hairline: Color,
    val hairlineFaint: Color,
    val outlineDecorative: Color,

    /**
     * The boundary colour for a control whose border is its only edge. At least 3:1 over [card]
     * in both themes: 3.49:1 light, 3.51:1 dark (D-23).
     */
    val outline: Color
)

private fun paletteOf(
    bg: Color,
    card: Color,
    ink: Color,
    onInk: Color,
    gold: Color,
    goldText: Color,
    danger: Color,
    sleepBand: Color,
    outlineAlpha: Float
) = MindScalePalette(
    bg = bg,
    card = card,
    ink = ink,
    onInk = onInk,
    gold = gold,
    goldText = goldText,
    danger = danger,
    sleepBand = sleepBand,
    crosshair = Crosshair,
    inkPrimary = ink,
    inkSecondary = ink.copy(alpha = 0.80f),
    inkTertiary = ink.copy(alpha = 0.70f),
    inkQuaternary = ink.copy(alpha = 0.60f),
    hairline = ink.copy(alpha = 0.09f),
    hairlineFaint = ink.copy(alpha = 0.07f),
    outlineDecorative = ink.copy(alpha = 0.16f),
    outline = ink.copy(alpha = outlineAlpha)
)

val LightPalette = paletteOf(
    bg = LightBg,
    card = LightCard,
    ink = LightInk,
    onInk = LightOnInk,
    gold = LightGold,
    goldText = LightGoldText,
    danger = LightDanger,
    sleepBand = LightSleepBand,
    outlineAlpha = 0.50f
)

val DarkPalette = paletteOf(
    bg = DarkBg,
    card = DarkCard,
    ink = DarkInk,
    onInk = DarkOnInk,
    gold = DarkGold,
    goldText = DarkGoldText,
    danger = DarkDanger,
    sleepBand = DarkSleepBand,
    outlineAlpha = 0.40f
)

internal val LocalMindScalePalette = staticCompositionLocalOf { LightPalette }

/** The MindScale palette for the current theme. */
val MaterialTheme.ms: MindScalePalette
    @Composable
    @ReadOnlyComposable
    get() = LocalMindScalePalette.current
