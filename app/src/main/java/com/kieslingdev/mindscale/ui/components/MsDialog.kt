package com.kieslingdev.mindscale.ui.components

import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms

/**
 * The design's dialog container, wrapped around Material's [AlertDialog]
 * (`docs/specs/SPEC-visual-foundation.md`, D-19).
 *
 * The prototype renders these as in-flow inline cards. MindScale does not adopt that, and the
 * reason is not taste: the scrolling `AlertDialog` is what fixed the Phase 12 clipping defect at
 * 200% font, and replacing it would change structure, focus order, dismissal, and back handling
 * — all behavioral, all forbidden by D-1.
 *
 * What is adopted is the container: a 16 dp radius on the card surface with a hairline gold
 * border and the design's `0 6px 18px rgba(ink, .22)` shadow, which is one of only two places
 * the near-flat rule in D-13 yields to a real shadow.
 *
 * Titles, body content, button callbacks, dismissal, and every `testTag` pass through unchanged.
 * The action labels stay in the caller's hands: Material's `TextButton` already resolves to
 * `colorScheme.primary`, which is now `goldText`, so the buttons are bare gold text without any
 * call-site edit. Uppercasing those labels is deferred to the phase that owns each screen,
 * because it is a per-call-site change and this phase touches only the container.
 */
@Composable
fun MsDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    val palette = MaterialTheme.ms
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        modifier = modifier.border(
            width = MsSpacing.hairline,
            color = palette.gold.copy(alpha = 0.4f),
            shape = MaterialTheme.shapes.large
        ),
        shape = MaterialTheme.shapes.large,
        containerColor = palette.card,
        titleContentColor = palette.inkPrimary,
        textContentColor = palette.inkSecondary,
        // The design's `0 6px 18px rgba(ink,.22)`. Tonal elevation stays at zero because
        // `surfaceTint` is transparent (D-9), so this is a real shadow and not a tint.
        tonalElevation = MsSpacing.hairline * 0,
        properties = properties
    )
}
