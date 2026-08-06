package com.kieslingdev.mindscale.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The MindScale palette, taken literally from the design authority
 * (`MindScale v2.dc.html` lines 14 and 15) and recorded in
 * `docs/specs/SPEC-visual-foundation.md` D-5, D-7, and D-8.
 *
 * Light and dark are two different treatments, not one palette flipped. [LightOnInk] and
 * [DarkOnInk] are the clearest evidence: warm bone on light, warm near-black on dark. Selected
 * segments and chips invert to an ink fill with on-ink text in both themes, and the two fills
 * are not each other's inverse.
 *
 * The prototype's `--canvas: #E9E6DF` is deliberately absent. It is the HTML page behind the
 * simulated device frame, not an app surface; adopting it would put a colour on screen that the
 * app never shows (D-5).
 */

// ── light ─────────────────────────────────────────────────────────────────────

val LightBg = Color(0xFFFCFBF9)
val LightCard = Color(0xFFFFFFFF)
val LightInk = Color(0xFF17130C)
val LightOnInk = Color(0xFFE3CE9F)

/**
 * The non-text gold, verbatim from the design: the header rule, the armed-pad ring, chip and
 * card borders. 3.15:1 against [LightCard], which clears the 3:1 floor for a control boundary.
 * It is never used for text — see [LightGoldText].
 */
val LightGold = Color(0xFFAE8C4F)

/**
 * The light-theme gold for text.
 *
 * The design's `--gold-deep` `#9A7B44` measures 3.84:1 on [LightBg], and gold text appears at
 * 9.5–10.5 sp, which is normal text under WCAG and requires 4.5:1.
 *
 * This is `#AE8C4F` scaled to 72% luminance along the same hue: the lightest value on that ramp
 * that clears 4.5:1 on *every* surface in the light scheme, not just on [LightBg] and
 * [LightCard]. Measured 5.35:1 on [LightBg], 5.54:1 on [LightCard], and 4.70:1 on the dimmest
 * container. A lighter candidate, `#886D3E`, passes on bg and card at 4.72:1 but drops to 4.14:1
 * on `surfaceContainerHighest` — a token with a "safe on these two surfaces only" caveat is a
 * token that will eventually be painted on the third (D-7).
 */
val LightGoldText = Color(0xFF7D6539)

/** Verbatim from the design. 6.60:1 on [LightBg] (D-8). */
val LightDanger = Color(0xFF9C3B2E)

/** `sleepC` at line 1326 of the design authority. */
val LightSleepBand = Color(0xFFDEDAD2)

// ── dark ──────────────────────────────────────────────────────────────────────

val DarkBg = Color(0xFF100E0B)
val DarkCard = Color(0xFF191612)
val DarkInk = Color(0xFFF4F0E8)
val DarkOnInk = Color(0xFF2A2114)

/** Verbatim from the design, and needs no text divergence: 8.60:1 on [DarkBg] (D-7). */
val DarkGold = Color(0xFFC9A96A)
val DarkGoldText = DarkGold

/**
 * The design has no dark-theme danger colour; reusing its `#9C3B2E` measures 2.83:1 on [DarkBg]
 * and fails. This is an omission in the source, not a value to copy. Measured 5.87:1 on
 * [DarkBg] and 5.50:1 on [DarkCard] (D-8).
 */
val DarkDanger = Color(0xFFD17466)

/** `sleepC` at line 1326 of the design authority. */
val DarkSleepBand = Color(0xFF24211C)

// ── shared ────────────────────────────────────────────────────────────────────

/** The chart crosshair stroke, line 405 of the design authority. Identical in both themes. */
val Crosshair = Color(0xFFBE9C5C)
