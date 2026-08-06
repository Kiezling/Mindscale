package com.kieslingdev.mindscale.ui.components

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.kieslingdev.mindscale.ui.theme.ms

/**
 * The design's palette applied to Material's `OutlinedTextField`
 * (`docs/specs/SPEC-remaining-screens-visual.md`, D-9 and its implementation record).
 *
 * **Why the field stays an `OutlinedTextField` at all.** The design draws every input as a bare
 * bottom rule, and Phase 16 built exactly that for Log's From/To — but those are *display*
 * controls that open a picker, not editable fields. A real editable field carries a floating
 * label, an error state, supporting text, IME options and a text-selection handle, and the
 * connected suite asserts several of them directly: `ProfileReportScreenTest` reads
 * `SemanticsProperties.Error` off `score_total` and `assertTextContains` off three fields, and
 * `SafetyScreenTest` drives `plan_text_field` with `performTextReplacement`. Reimplementing that
 * on `BasicTextField` to win a bottom rule would be a behavioural change wearing a visual costume,
 * which D-1 forbids. This is the same reasoning D-19 of the foundation used to keep `AlertDialog`.
 *
 * What does change is every colour Material would otherwise supply from its own scheme: the
 * container goes transparent so the field sits on the page or the card as the design's does, the
 * border carries the 3:1 `outline` token rather than Material's `outlineVariant`, and the focused
 * border and cursor are the brand gold.
 */
@Composable
fun msFieldColors(): TextFieldColors {
    val palette = MaterialTheme.ms
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.inkPrimary,
        unfocusedTextColor = palette.inkPrimary,
        disabledTextColor = palette.inkQuaternary,
        errorTextColor = palette.inkPrimary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        cursorColor = palette.gold,
        errorCursorColor = palette.danger,
        focusedBorderColor = palette.gold,
        unfocusedBorderColor = palette.outline,
        disabledBorderColor = palette.hairline,
        errorBorderColor = palette.danger,
        focusedLabelColor = palette.goldText,
        unfocusedLabelColor = palette.inkQuaternary,
        disabledLabelColor = palette.inkQuaternary,
        errorLabelColor = palette.danger,
        focusedSupportingTextColor = palette.inkQuaternary,
        unfocusedSupportingTextColor = palette.inkQuaternary,
        disabledSupportingTextColor = palette.inkQuaternary,
        errorSupportingTextColor = palette.danger
    )
}

/**
 * The brand's text-selection handle and highlight, which Material otherwise derives from
 * `colorScheme.primary` with a 0.4 alpha. Scoped rather than global so nothing outside a field is
 * affected.
 */
@Composable
fun MsFieldSelectionColors(content: @Composable () -> Unit) {
    val palette = MaterialTheme.ms
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = palette.gold,
            backgroundColor = palette.gold.copy(alpha = 0.30f)
        ),
        content = content
    )
}
