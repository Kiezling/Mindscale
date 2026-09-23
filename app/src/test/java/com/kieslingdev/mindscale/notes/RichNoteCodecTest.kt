package com.kieslingdev.mindscale.notes

import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.settings.ParseResult
import com.kieslingdev.mindscale.settings.encodeBackup
import com.kieslingdev.mindscale.settings.encodeRecordsCsv
import com.kieslingdev.mindscale.settings.parseBackup
import com.kieslingdev.mindscale.settings.parseRecordsCsv
import com.kieslingdev.mindscale.ui.components.richNoteAnnotatedString
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichNoteCodecTest {
    @Test fun plainAndEnvelopeRoundTrip() {
        val note = RichNote("word 🙂", listOf(NoteSpan(NoteStyle.BOLD, 0, 4)))
        val encoded = RichNoteCodec.encode(note)
        assertEquals(note, RichNoteCodec.decode(encoded))
        assertEquals("word 🙂", RichNoteCodec.plainText(encoded))
    }

    @Test fun legacyAndMalformedRemainLiteral() {
        assertEquals("hello", RichNoteCodec.decode("hello").text)
        listOf(
            "[[MindScale note v1]]\nb,0,nope\nhello",
            "[[MindScale note v1]]\nx,0,1\nhello",
            "[[MindScale note v1]]\nb,-1,1\nhello",
            "[[MindScale note v1]]\nb,0,9\nhello",
            "[[MindScale note v1]]\nb,0,1\n🙂"
        ).forEach { malformed -> assertEquals(malformed, RichNoteCodec.decode(malformed).text) }
    }

    @Test fun toggleAddsAndRemovesSelection() {
        val base = RichNote("hello")
        val styled = toggleNoteSpan(base, NoteStyle.BOLD, 0, 4)
        assertEquals(listOf(NoteSpan(NoteStyle.BOLD, 0, 4)), styled.spans)
        assertEquals(base, toggleNoteSpan(styled, NoteStyle.BOLD, 0, 4))
    }

    @Test fun overlappingSameStyleRangesNormalize() {
        val encoded = RichNoteCodec.encode(
            RichNote("abcdef", listOf(NoteSpan(NoteStyle.ITALIC, 0, 3), NoteSpan(NoteStyle.ITALIC, 2, 6)))
        )
        assertEquals(listOf(NoteSpan(NoteStyle.ITALIC, 0, 6)), RichNoteCodec.decode(encoded).spans)
    }

    @Test fun singleLineAndMultilineEnvelopeRoundTrip() {
        listOf("single line", "first\nsecond").forEach { text ->
            val note = RichNote(text, listOf(NoteSpan(NoteStyle.UNDERLINE, 0, text.length)))
            assertEquals(note, RichNoteCodec.decode(RichNoteCodec.encode(note)))
        }
    }

    @Test fun interleavedStylesStillMergeAndToggleAsOneRange() {
        val note = RichNote("abcdef", listOf(
            NoteSpan(NoteStyle.BOLD, 0, 4),
            NoteSpan(NoteStyle.ITALIC, 1, 3),
            NoteSpan(NoteStyle.BOLD, 2, 6)
        ))
        val decoded = RichNoteCodec.decode(RichNoteCodec.encode(note))
        assertEquals(listOf(NoteSpan(NoteStyle.BOLD, 0, 6), NoteSpan(NoteStyle.ITALIC, 1, 3)), decoded.spans)
        assertEquals(
            listOf(
                NoteSpan(NoteStyle.BOLD, 0, 2),
                NoteSpan(NoteStyle.ITALIC, 1, 3),
                NoteSpan(NoteStyle.BOLD, 4, 6)
            ),
            toggleNoteSpan(decoded, NoteStyle.BOLD, 2, 4).spans
        )
    }

    @Test fun editMappingPreservesStylesBeforeInsideAndAfter() {
        val spans = listOf(NoteSpan(NoteStyle.BOLD, 0, 2), NoteSpan(NoteStyle.ITALIC, 2, 4), NoteSpan(NoteStyle.UNDERLINE, 4, 6))
        val inserted = transformNoteSpans(spans, 2, 2, 1)
        assertEquals(listOf(NoteSpan(NoteStyle.BOLD, 0, 2), NoteSpan(NoteStyle.ITALIC, 2, 5), NoteSpan(NoteStyle.UNDERLINE, 5, 7)), inserted)
        val deleted = transformNoteSpans(inserted, 3, 5, 0)
        assertEquals(listOf(NoteSpan(NoteStyle.BOLD, 0, 2), NoteSpan(NoteStyle.ITALIC, 2, 3), NoteSpan(NoteStyle.UNDERLINE, 3, 5)), deleted)
    }

    @Test fun rendererCombinesUnderlineAndStrikethrough() {
        val text = richNoteAnnotatedString(RichNoteCodec.encode(RichNote("both", listOf(
            NoteSpan(NoteStyle.UNDERLINE, 0, 4), NoteSpan(NoteStyle.STRIKETHROUGH, 0, 4)
        ))))
        assertEquals(1, text.spanStyles.size)
        assertTrue(text.spanStyles.single().item.textDecoration!!.contains(androidx.compose.ui.text.style.TextDecoration.Underline))
        assertTrue(text.spanStyles.single().item.textDecoration!!.contains(androidx.compose.ui.text.style.TextDecoration.LineThrough))
    }

    @Test fun backupAndCsvRetainFormattedNoteVerbatim() {
        val note = RichNoteCodec.encode(RichNote("line one\nline two", listOf(
            NoteSpan(NoteStyle.BOLD, 0, 4), NoteSpan(NoteStyle.ITALIC, 5, 13)
        )))
        val snapshot = DataSnapshot(
            entries = listOf(Entry(1, Instant.parse("2026-09-01T12:00:00Z").toEpochMilli(), 5, listOf("work"), note, EntryKind.WAKE)),
            sleeps = emptyList(), markers = emptyList(), settings = TrackSettings()
        )
        val backup = parseBackup(encodeBackup(snapshot, Instant.parse("2026-09-02T12:00:00Z")), Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC) as ParseResult.Ok
        val csv = parseRecordsCsv(encodeRecordsCsv(snapshot), Instant.parse("2026-09-03T12:00:00Z")) as ParseResult.Ok
        assertEquals(note, backup.value.entries.single().note)
        assertEquals(note, csv.value.entries.single().note)
    }
}
