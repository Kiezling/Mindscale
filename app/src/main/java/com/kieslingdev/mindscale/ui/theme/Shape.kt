package com.kieslingdev.mindscale.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The design's radii, passed to `MaterialTheme` for the first time
 * (`docs/specs/SPEC-visual-foundation.md`, D-12).
 *
 * Counts are occurrences in the design authority. A 10 dp and a 12 dp radius each appear once or
 * twice and are folded into `small` and `medium`; the difference is invisible at these sizes and
 * a seven-value shape scale is worth more than an exact transcription of one-off values.
 */
val MindScaleShapes = Shapes(
    // Histogram bars and chart marks (7 uses).
    extraSmall = RoundedCornerShape(2.dp),
    // Small inset panels (3 uses).
    small = RoundedCornerShape(9.dp),
    // The card radius, and the dominant shape of the whole design (19 uses).
    medium = RoundedCornerShape(14.dp),
    // The dialog and the emphasized card (2 uses).
    large = RoundedCornerShape(16.dp),
    // The numpad wrapper.
    extraLarge = RoundedCornerShape(24.dp)
)

/** The two shapes with no Material slot. */
object MsShapes {
    /** Pills, chips, segments, and toggle tracks (15 uses). */
    val pill = RoundedCornerShape(999.dp)

    /** Numpad keys, entry-row dots, and the breathing circle (10 uses). */
    val circle = CircleShape
}
