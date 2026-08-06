package com.kieslingdev.mindscale.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale

/**
 * Renders [text] in upper case while describing it in its original case.
 *
 * The design uppercases nearly every label. Uppercasing at the call site would change the
 * semantics text that 111 `onNodeWithText` assertions across 8 connected test files depend on,
 * and breaking them would mean behavior changed — which the visual-only rule forbids
 * (`docs/specs/SPEC-visual-foundation.md`, D-1 and D-11). It is also a real accessibility
 * defect: all-caps text is read letter by letter by some TalkBack configurations.
 *
 * [clearAndSetSemantics] is used rather than [androidx.compose.ui.semantics.semantics] because
 * the `Text` semantics key merges by appending: a plain `semantics { text = ... }` block would
 * leave *both* the uppercased and the original string on the node, which happens to satisfy
 * `onNodeWithText` but leaves a node that reads its own label twice. Clearing replaces the
 * leaf's configuration outright, and a merging ancestor — a clickable navigation tab, for
 * instance — still collects the restored original.
 *
 * [Locale.ROOT] is used deliberately. Under the default locale a Turkish or Azeri device would
 * turn `i` into `İ`, silently changing what the label says.
 */
@Composable
fun MsUppercaseText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text.uppercase(Locale.ROOT),
        modifier = modifier.clearAndSetSemantics { this.text = AnnotatedString(text) },
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}
