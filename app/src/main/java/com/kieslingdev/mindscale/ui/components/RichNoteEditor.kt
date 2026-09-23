package com.kieslingdev.mindscale.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kieslingdev.mindscale.notes.NoteStyle
import com.kieslingdev.mindscale.notes.NoteSpan
import com.kieslingdev.mindscale.notes.RichNote
import com.kieslingdev.mindscale.notes.RichNoteCodec
import com.kieslingdev.mindscale.notes.toggleNoteSpan
import com.kieslingdev.mindscale.notes.transformNoteSpans
import com.kieslingdev.mindscale.ui.theme.MsSpacing

fun richNoteAnnotatedString(value: String): AnnotatedString {
    val note = RichNoteCodec.decode(value)
    return annotatedNoteText(note)
}

private fun annotatedNoteText(note: RichNote): AnnotatedString = AnnotatedString.Builder(note.text).apply {
    val boundaries = (note.spans.flatMap { listOf(it.start, it.end) } + listOf(0, note.text.length)).distinct().sorted()
    boundaries.zipWithNext().forEach { (start, end) ->
        val styles = note.spans.filter { it.start <= start && it.end >= end }.map(NoteSpan::style).toSet()
        if (styles.isNotEmpty() && start < end) addStyle(styleFor(styles), start, end)
    }
}.toAnnotatedString()

private fun styleFor(styles: Set<NoteStyle>) = SpanStyle(
    fontWeight = if (NoteStyle.BOLD in styles) FontWeight.Bold else null,
    fontStyle = if (NoteStyle.ITALIC in styles) FontStyle.Italic else null,
    textDecoration = TextDecoration.combine(
        buildList {
            if (NoteStyle.UNDERLINE in styles) add(TextDecoration.Underline)
            if (NoteStyle.STRIKETHROUGH in styles) add(TextDecoration.LineThrough)
        }
    )
)

private fun safeCommonPrefix(first: String, second: String): Int {
    var length = first.commonPrefixWith(second).length
    if (length > 0 && Character.isHighSurrogate(first[length - 1]) &&
        length < first.length && Character.isLowSurrogate(first[length])
    ) length--
    return length
}

private fun safeCommonSuffix(first: String, second: String, prefix: Int): Int {
    var length = first.commonSuffixWith(second).length.coerceAtMost(first.length - prefix).coerceAtMost(second.length - prefix)
    while (length > 0 && ((first.length - length < first.length && Character.isLowSurrogate(first[first.length - length])) ||
            (second.length - length < second.length && Character.isLowSurrogate(second[second.length - length])))) length--
    return length
}

private fun removeStyleFromRange(spans: List<NoteSpan>, style: NoteStyle, start: Int, end: Int): List<NoteSpan> =
    RichNoteCodec.normalize(spans.flatMap { span ->
        if (span.style != style || span.end <= start || span.start >= end) listOf(span)
        else buildList {
            if (span.start < start) add(span.copy(end = start))
            if (span.end > end) add(span.copy(start = end))
        }
    })

@Composable
fun RichNoteEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var note by remember { mutableStateOf(RichNoteCodec.decode(value)) }
    var selection by remember { mutableStateOf(TextRange(note.text.length)) }
    var composition by remember { mutableStateOf<TextRange?>(null) }
    var typingStyles by remember { mutableStateOf<Map<NoteStyle, Boolean>>(emptyMap()) }
    var editorError by remember { mutableStateOf<String?>(null) }
    var pendingEmissions by remember { mutableStateOf<List<String>>(emptyList()) }
    fun publish(updated: RichNote) {
        val encoded = RichNoteCodec.encode(updated)
        // A text field has a finite persisted size, but its StateFlow acknowledgement can lag
        // behind any number of IME composition updates. Never discard an unacknowledged value.
        pendingEmissions = pendingEmissions + encoded
        onValueChange(encoded)
    }
    LaunchedEffect(value) {
        val acknowledgement = pendingEmissions.indexOf(value)
        if (acknowledgement >= 0) {
            // StateFlow may conflate intermediate writes. A later echoed value acknowledges
            // every local emission before it, so they cannot accumulate indefinitely.
            pendingEmissions = pendingEmissions.drop(acknowledgement + 1)
        } else if (value != RichNoteCodec.encode(note)) {
            note = RichNoteCodec.decode(value)
            selection = TextRange(note.text.length)
            composition = null
            typingStyles = emptyMap()
        }
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MsSpacing.xxxs)) {
            listOf(NoteStyle.BOLD to "B", NoteStyle.ITALIC to "I", NoteStyle.UNDERLINE to "U", NoteStyle.STRIKETHROUGH to "S").forEach { (style, label) ->
                TextButton(enabled = enabled, onClick = {
                    val start = selection.min; val end = selection.max
                    if (start == end) {
                        val inherited = note.spans.any { it.style == style && it.start <= start && it.end > start }
                        val enabled = typingStyles[style] ?: inherited
                        typingStyles = typingStyles + (style to !enabled)
                    }
                    else {
                        typingStyles = emptyMap()
                        val updated = toggleNoteSpan(note, style, start, end)
                        if (updated == note && note.spans.none { it.style == style && it.start <= start && it.end >= end }) {
                            editorError = "Too much formatting. Remove some styles before adding more."
                        } else {
                            editorError = null
                            note = updated
                            publish(updated)
                        }
                    }
                }, modifier = Modifier.heightIn(min = MsSpacing.minTouchTarget).semantics {
                    contentDescription = when (style) {
                        NoteStyle.BOLD -> "Bold"
                        NoteStyle.ITALIC -> "Italic"
                        NoteStyle.UNDERLINE -> "Underline"
                        NoteStyle.STRIKETHROUGH -> "Strikethrough"
                    }
                    selected = typingStyles[style] ?: (selection.min < selection.max && note.spans.any {
                        it.style == style && it.start <= selection.min && it.end >= selection.max
                    })
                }) {
                    Text(
                        text = label,
                        style = when (style) {
                            NoteStyle.BOLD -> MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            NoteStyle.ITALIC -> MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic)
                            NoteStyle.UNDERLINE -> MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline)
                            NoteStyle.STRIKETHROUGH -> MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.LineThrough)
                        }
                    )
                }
            }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MsSpacing.sm))
                .border(
                    MsSpacing.hairline,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(MsSpacing.sm)
                )
                .padding(MsSpacing.sm)
        ) {
        BasicTextField(
            value = TextFieldValue(annotatedNoteText(note), selection, composition),
            onValueChange = { changed ->
                val selectionMoved = changed.selection != selection
                selection = changed.selection
                composition = changed.composition
                val old = note.text
                val newText = changed.text
                if (newText == old) {
                    if (selectionMoved && changed.composition == null &&
                        changed.selection.min != changed.selection.max
                    ) typingStyles = emptyMap()
                    return@BasicTextField
                }
                val prefix = safeCommonPrefix(old, newText)
                val suffix = safeCommonSuffix(old, newText, prefix)
                val removedEnd = old.length - suffix
                val insertedLength = newText.length - prefix - suffix
                val spans = transformNoteSpans(note.spans, prefix, removedEnd, insertedLength)
                val insertedEnd = prefix + insertedLength
                val withoutDisabled = typingStyles.filterValues { !it }.keys.fold(spans) { current, style ->
                    removeStyleFromRange(current, style, prefix, insertedEnd)
                }
                val styled = RichNoteCodec.normalize(withoutDisabled + typingStyles.filterValues { it }.keys.map {
                    NoteSpan(it, prefix, insertedEnd)
                })
                val accepted = if (RichNoteCodec.valid(newText, styled)) styled else spans
                note = RichNote(newText, accepted)
                editorError = if (styled.size > 128) "Too much formatting. Remove some styles before adding more." else null
                publish(note)
            },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = modifier.fillMaxWidth().heightIn(min = MsSpacing.huge).semantics {
                if (isError || editorError != null) error(editorError ?: supportingText ?: "Invalid note")
            },
            decorationBox = { innerTextField ->
                if (note.text.isEmpty()) {
                    Text(
                        text = "Add a note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )
        }
        (editorError ?: supportingText)?.let { Text(it, color = if (isError || editorError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
