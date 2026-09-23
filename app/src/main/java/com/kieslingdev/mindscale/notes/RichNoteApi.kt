package com.kieslingdev.mindscale.notes

/** One UTF-16 half-open style range in a note. */
enum class NoteStyle(val code: Char) {
    BOLD('b'), ITALIC('i'), UNDERLINE('u'), STRIKETHROUGH('s');

    companion object {
        fun fromCode(code: String): NoteStyle? = entries.singleOrNull { it.code.toString() == code }
    }
}

data class NoteSpan(val style: NoteStyle, val start: Int, val end: Int)

data class RichNote(val text: String, val spans: List<NoteSpan> = emptyList())

object RichNoteCodec {
    private const val PREFIX = "[[MindScale note v1]]\n"
    private const val MAX_SPANS = 128

    fun decode(value: String): RichNote {
        if (!value.startsWith(PREFIX)) return RichNote(value)
        val metadataEnd = value.indexOf('\n', PREFIX.length)
        if (metadataEnd < 0) return RichNote(value)
        val metadata = value.substring(PREFIX.length, metadataEnd)
        val text = value.substring(metadataEnd + 1)
        if (metadata.isEmpty()) return RichNote(value)
        val spans = metadata.split(';').map { token ->
            val parts = token.split(',')
            if (parts.size != 3) return RichNote(value)
            val style = NoteStyle.fromCode(parts[0]) ?: return RichNote(value)
            val start = parts[1].toIntOrNull() ?: return RichNote(value)
            val end = parts[2].toIntOrNull() ?: return RichNote(value)
            NoteSpan(style, start, end)
        }
        return if (valid(text, spans)) RichNote(text, normalize(spans)) else RichNote(value)
    }

    fun encode(note: RichNote): String {
        val spans = normalize(note.spans)
        require(valid(note.text, spans)) { "Note spans must be valid UTF-16 ranges" }
        if (spans.isEmpty()) return note.text
        return PREFIX + spans.joinToString(";") { "${it.style.code},${it.start},${it.end}" } + "\n" + note.text
    }

    fun plainText(value: String): String = decode(value).text

    internal fun valid(text: String, spans: List<NoteSpan>): Boolean = spans.size <= MAX_SPANS && spans.all {
        it.start >= 0 && it.end <= text.length && it.start < it.end &&
            !Character.isLowSurrogate(text[it.start]) &&
            !Character.isHighSurrogate(text[it.end - 1])
    }

    internal fun normalize(spans: List<NoteSpan>): List<NoteSpan> = spans
        .distinct()
        .groupBy(NoteSpan::style)
        .flatMap { (style, styleSpans) ->
            styleSpans.sortedWith(compareBy(NoteSpan::start, NoteSpan::end)).fold(mutableListOf<NoteSpan>()) { out, span ->
                val previous = out.lastOrNull()
                if (previous != null && previous.end >= span.start) {
                    out[out.lastIndex] = previous.copy(end = maxOf(previous.end, span.end))
                } else {
                    out += span.copy(style = style)
                }
                out
            }
        }
        .sortedWith(compareBy(NoteSpan::start, NoteSpan::end, { it.style.code }))
}

fun toggleNoteSpan(note: RichNote, style: NoteStyle, start: Int, end: Int): RichNote {
    if (start == end || !RichNoteCodec.valid(note.text, listOf(NoteSpan(style, start, end)))) return note
    val result = mutableListOf<NoteSpan>()
    note.spans.filter { it.style != style }.forEach(result::add)
    note.spans.filter { it.style == style }.forEach { span ->
        if (span.end <= start || span.start >= end) {
            result += span
        } else {
            if (span.start < start) result += span.copy(end = start)
            if (span.end > end) result += span.copy(start = end)
        }
    }
    val fullyCovered = note.spans.any { it.style == style && it.start <= start && it.end >= end }
    if (!fullyCovered) result += NoteSpan(style, start, end)
    val normalized = RichNoteCodec.normalize(result)
    return if (RichNoteCodec.valid(note.text, normalized)) note.copy(spans = normalized) else note
}

/** Maps spans through replacement of [oldStart..oldEnd) with [insertedLength] UTF-16 units. */
fun transformNoteSpans(spans: List<NoteSpan>, oldStart: Int, oldEnd: Int, insertedLength: Int): List<NoteSpan> {
    if (oldStart < 0 || oldEnd < oldStart || insertedLength < 0) return spans
    val delta = insertedLength - (oldEnd - oldStart)
    return RichNoteCodec.normalize(spans.mapNotNull { span ->
        val start = when {
            span.start > oldEnd || (oldStart != oldEnd && span.start == oldEnd) -> span.start + delta
            span.start > oldStart -> oldStart
            else -> span.start
        }
        val end = when {
            span.end <= oldStart -> span.end
            span.end >= oldEnd -> span.end + delta
            else -> oldStart
        }
        if (start < end) span.copy(start = start, end = end) else null
    })
}
