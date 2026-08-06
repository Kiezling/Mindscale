package com.kieslingdev.mindscale.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kieslingdev.mindscale.R

/**
 * Instrument Sans, bundled (`docs/specs/SPEC-visual-foundation.md`, D-10).
 *
 * Three OFL-licensed **static** instances — Regular 400, Medium 500, SemiBold 600 — bundled as
 * plain TTFs, from the upstream `Instrument/instrument-sans` project that `google/fonts`
 * packages.
 *
 * Static instances rather than the single variable file, and the reason is `minSdk`. The
 * variable font can only be pinned to a weight through `fontVariationSettings`, which the
 * platform honours from **API 28**. MindScale's minimum is 26, so on API 26 and 27 all three
 * families would have resolved to the variable font's default instance and the entire weight
 * hierarchy — the 500 identity, the 400 prose, the 600 selected tab — would have silently
 * collapsed into one weight. Lint flagged the attribute; the collapse is what actually mattered.
 * Three files cost 260 KB against the variable font's 194 KB, which is the right trade.
 *
 * There is no new Gradle dependency and no runtime network access — Downloadable Fonts is
 * rejected precisely because it would introduce the network dependency MindScale does not have.
 * The licence ships at `docs/design/InstrumentSans-OFL.txt`.
 */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold)
)

/**
 * The prototype's `font-variant-numeric: tabular-nums`. Applied to every style that carries a
 * figure — display, headline, title, and label — and deliberately not to body prose, where
 * tabular digits are wider than they need to be.
 */
private const val TABULAR = "tnum"

private fun ms(
    size: Double,
    weight: FontWeight,
    lineHeight: Double,
    trackingEm: Double = 0.0,
    tabular: Boolean = true
) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = trackingEm.em,
    fontFeatureSettings = if (tabular) TABULAR else null
)

/**
 * The design's type scale, mapped onto all fifteen Material 3 slots.
 *
 * **Units.** The prototype renders in a 402 CSS-px-wide iOS frame, where one CSS px is one iOS
 * point, which is the same physical construct as one Android dp. The rule is `1 CSS px = 1 sp`,
 * rounded to the nearest 0.5.
 *
 * **Weight.** The identity is 500, and it is the weight of every label, title, action, and
 * numeral — 108 of the 113 explicit weights in the source. Body paragraphs specify no weight and
 * therefore render at 400, and the numpad glyph is explicitly 400. Weight 600 survives on
 * exactly one element, the selected bottom-navigation tab.
 *
 * **Tracking.** CSS `letter-spacing` is absolute px; these are expressed in `em` so that
 * tracking scales with the user's font-size setting instead of staying fixed while the glyphs
 * grow. Each value is the source px divided by the source font size.
 *
 * The 9–10.5 sp label tier is smaller than common Android practice. It is kept because it is the
 * design's deliberate idiom and WCAG sets no minimum font size, and it is made safe rather than
 * merely small: it is `sp` and scales with the user's setting, it is painted at an emphasis
 * level measuring at least 4.77:1, and it never carries information alone.
 */
val MindScaleTypography = Typography(
    // Track readout number, 26px/500.
    displayLarge = ms(26.0, FontWeight.Medium, 28.6),
    // Dialog value, 24px/500.
    displayMedium = ms(24.0, FontWeight.Medium, 24.0),
    // Numpad key glyph, 21px/400 — the design's only deliberate 400 outside prose.
    displaySmall = ms(21.0, FontWeight.Normal, 21.0),

    headlineLarge = ms(19.0, FontWeight.Medium, 22.8),
    headlineMedium = ms(17.0, FontWeight.Medium, 20.4),
    // Entry-row dot, 16px/500.
    headlineSmall = ms(16.0, FontWeight.Medium, 19.2),

    titleLarge = ms(15.0, FontWeight.Medium, 19.5),
    titleMedium = ms(13.5, FontWeight.Medium, 18.9),
    // Settings row label, 13px/500.
    titleSmall = ms(13.0, FontWeight.Medium, 18.2),

    // Prose. No tabular figures, and weight 400 as the source leaves it.
    bodyLarge = ms(13.5, FontWeight.Normal, 21.6, tabular = false),
    bodyMedium = ms(13.0, FontWeight.Normal, 20.8, tabular = false),
    bodySmall = ms(12.5, FontWeight.Normal, 20.6, tabular = false),

    // Pill and segment labels, 10.5px/500 tracked 2.4px.
    labelLarge = ms(10.5, FontWeight.Medium, 13.7, trackingEm = 0.229),
    // Bare gold text actions, 9.5px/500 tracked 1.8px.
    labelMedium = ms(9.5, FontWeight.Medium, 12.4, trackingEm = 0.189),
    // Eyebrow labels, 9px/500 tracked 2.2px.
    labelSmall = ms(9.0, FontWeight.Medium, 11.7, trackingEm = 0.244)
)

/** The two styles the design uses that have no Material slot. */
object MindScaleTextStyles {
    /** The header wordmark: 12px/500 tracked 6.5px. */
    val wordmark = ms(12.0, FontWeight.Medium, 15.6, trackingEm = 0.542)

    /** The toast pill: 11.5px/500 tracked 1.4px. */
    val toast = ms(11.5, FontWeight.Medium, 15.0, trackingEm = 0.122)

    /**
     * Onset chips: 11.5px/500, untracked and — unlike almost every other label in the design —
     * **not** uppercased. `chipBtn` at line 1223 of the design authority sets no
     * `text-transform`, because a chip carries the user's own words.
     */
    val chip = ms(11.5, FontWeight.Medium, 15.0, tabular = false)
}
