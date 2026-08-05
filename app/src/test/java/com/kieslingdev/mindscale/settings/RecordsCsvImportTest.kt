package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.breathing.MAX_BREATHING_SESSION_MILLIS
import com.kieslingdev.mindscale.data.BreathingSession
import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Records CSV coverage (SPEC-import-restore.md, D-1, D-5, D-6). */
class RecordsCsvImportTest {

    private val now = Instant.parse("2026-09-01T00:00:00Z")
    private val ts = Instant.parse("2026-08-02T09:00:00Z")

    private fun parse(text: String) = parseRecordsCsv(text, now)
    private fun rejection(text: String) = (parse(text) as ParseResult.Rejected).message
    private fun accepted(text: String) = (parse(text) as ParseResult.Ok).value

    private fun csv(vararg rows: String) =
        (listOf(RECORDS_CSV_HEADER) + rows).joinToString("\r\n") + "\r\n"

    @Test
    fun readsTheProductionExportBackIntoTheSameRecords() {
        val snapshot = DataSnapshot(
            entries = listOf(
                Entry(2, ts.toEpochMilli(), 7, listOf("work", "poor sleep"), "line 1\n\"quoted\", too", EntryKind.WAKE),
                Entry(1, ts.toEpochMilli() - 1_000, 0, emptyList(), null, null)
            ),
            sleeps = listOf(SleepInterval(3, ts.toEpochMilli() - 40_000, ts.toEpochMilli() - 20_000)),
            markers = listOf(Marker(4, ts.toEpochMilli() - 5_000, "dose, changed")),
            settings = TrackSettings()
        )
        val payload = accepted(encodeRecordsCsv(snapshot))

        assertEquals(2, payload.entries.size)
        assertEquals("line 1\n\"quoted\", too", payload.entries.first().note)
        assertEquals(listOf("work", "poor sleep"), payload.entries.first().chips)
        assertEquals(EntryKind.WAKE, payload.entries.first().kind)
        assertNull(payload.entries[1].note)
        assertNull(payload.entries[1].kind)
        assertTrue(payload.entries[1].chips.isEmpty())
        assertEquals(snapshot.sleeps.map { it.startTs to it.endTs }, payload.sleeps.map { it.startTs to it.endTs })
        assertEquals("dose, changed", payload.markers.single().text)
        // The CSV carries no ids, so Room assigns them on insert.
        assertTrue(payload.entries.all { it.id == 0L })
    }

    @Test
    fun requiresTheExactHeader() {
        assertEquals(ImportMessages.NOT_A_RECORDS_CSV, rejection(""))
        assertEquals(ImportMessages.NOT_A_RECORDS_CSV, rejection("a,b,c\r\n"))
        assertEquals(
            ImportMessages.NOT_A_RECORDS_CSV,
            rejection(RECORDS_CSV_HEADER.replace("record_type", "type") + "\r\n")
        )
    }

    @Test
    fun acceptsBothCrlfAndLfTerminatorsAndAnOptionalFinalOne() {
        val row = "rating,$ts,,3,,,,"
        assertEquals(1, accepted(csv(row)).entries.size)
        assertEquals(1, accepted("$RECORDS_CSV_HEADER\n$row\n").entries.size)
        assertEquals(1, accepted("$RECORDS_CSV_HEADER\r\n$row").entries.size)
    }

    @Test
    fun refusesABareCarriageReturnTerminator() {
        assertTrue(rejection("$RECORDS_CSV_HEADER\rrating,$ts,,3,,,,\r").startsWith("This CSV does not match"))
    }

    @Test
    fun refusesWrongFieldCounts() {
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,3,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,3,,,,,")))
    }

    @Test
    fun handlesRfc4180QuotingIncludingEmbeddedSeparators() {
        val payload = accepted(csv("rating,$ts,,3,,,\"a,b\"\"c\r\nd\",", "marker,$ts,,,,,,\"x\ny\""))
        assertEquals("a,b\"c\r\nd", payload.entries.single().note)
        assertEquals("x\ny", payload.markers.single().text)
    }

    @Test
    fun refusesUnterminatedAndMisplacedQuotes() {
        assertTrue(rejection(csv("rating,$ts,,3,,,\"unterminated,")).startsWith("This CSV does not match"))
        assertTrue(rejection(csv("rating,$ts,,3,,,a\"b,")).startsWith("This CSV does not match"))
        assertTrue(rejection(csv("rating,$ts,,3,,,\"ab\"c,")).startsWith("This CSV does not match"))
    }

    @Test
    fun refusesUnknownRecordTypes() {
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("score,$ts,,3,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("Rating,$ts,,3,,,,")))
    }

    @Test
    fun refusesAValueInAFieldThatDoesNotApply() {
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,$ts,3,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,3,,,,text")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("sleep,$ts,,3,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("marker,$ts,,,,,note,text")))
    }

    @Test
    fun refusesMissingRequiredFields() {
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,,,3,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("marker,$ts,,,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("sleep,,,,,,,")))
    }

    @Test
    fun refusesNonCanonicalIntensitySpellings() {
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,+3,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,03,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,, 3,,,,")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(csv("rating,$ts,,11,,,,")))
    }

    @Test
    fun refusesEmptyChipSegments() {
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,3,,work||home,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,3,,|work,,")))
        assertEquals(listOf("work", "home"), accepted(csv("rating,$ts,,3,,work|home,,")).entries.single().chips)
    }

    @Test
    fun refusesSleepPeriodsThatEndBeforeTheyStart() {
        val earlier = Instant.parse("2026-08-01T09:00:00Z")
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(csv("sleep,$ts,$earlier,,,,,")))
        assertTrue(parse(csv("sleep,$earlier,$ts,,,,,")) is ParseResult.Ok)
        assertNull(accepted(csv("sleep,$ts,,,,,,")).sleeps.single().endTs)
    }

    @Test
    fun refusesUnknownEntryKinds() {
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("rating,$ts,,3,DOZE,,,")))
        assertEquals(EntryKind.SLEEP, accepted(csv("rating,$ts,,3,SLEEP,,,")).entries.single().kind)
    }

    @Test
    fun refusesMoreRowsThanTheFrozenLimit() {
        val rows = Array(MAX_RECORDS_PER_COLLECTION + 1) { "marker,$ts,,,,,,m$it" }
        assertEquals(ImportMessages.TOO_MANY_RECORDS, rejection(csv(*rows)))
    }

    @Test
    fun readsAProductionBreathingRowBackIntoTheSameSession() {
        val snapshot = DataSnapshot(
            entries = emptyList(),
            sleeps = emptyList(),
            markers = emptyList(),
            settings = TrackSettings(),
            breathingSessions = listOf(
                BreathingSession(9, ts.toEpochMilli() - 60_000, ts.toEpochMilli())
            )
        )
        val payload = accepted(encodeRecordsCsv(snapshot))
        val session = payload.breathingSessions.single()
        assertEquals(ts.toEpochMilli() - 60_000, session.startedAt)
        assertEquals(ts.toEpochMilli(), session.endedAt)
        // The CSV carries no ids, so Room assigns them on insert.
        assertEquals(0L, session.id)
    }

    @Test
    fun acceptsABreathingRowIncludingAZeroLengthOne() {
        val row = "breathing,$ts,$ts,,,,,"
        assertEquals(1, accepted(csv(row)).breathingSessions.size)
        val session = accepted(csv(row)).breathingSessions.single()
        assertEquals(ts.toEpochMilli(), session.startedAt)
        assertEquals(ts.toEpochMilli(), session.endedAt)
    }

    @Test
    fun refusesABreathingRowWithAValueInAFieldThatDoesNotApply() {
        val end = ts.plusSeconds(60)
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("breathing,$ts,$end,3,,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("breathing,$ts,$end,,SLEEP,,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("breathing,$ts,$end,,,work,,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("breathing,$ts,$end,,,,note,")))
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("breathing,$ts,$end,,,,,text")))
    }

    @Test
    fun refusesABreathingRowMissingTheEndTimestamp() {
        assertEquals(ImportMessages.invalidCsv(2), rejection(csv("breathing,$ts,,,,,,")))
    }

    @Test
    fun refusesABreathingRowThatEndsBeforeItStarts() {
        val earlier = Instant.parse("2026-08-01T09:00:00Z")
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(csv("breathing,$ts,$earlier,,,,,")))
    }

    @Test
    fun refusesABreathingRowLongerThanTheMaximumSessionLength() {
        val tooLong = ts.plusMillis(MAX_BREATHING_SESSION_MILLIS + 1)
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(csv("breathing,$ts,$tooLong,,,,,")))
        val atLimit = ts.plusMillis(MAX_BREATHING_SESSION_MILLIS)
        assertTrue(parse(csv("breathing,$ts,$atLimit,,,,,")) is ParseResult.Ok)
    }

    @Test
    fun refusesControlCharactersThatWouldCorruptStoredFields() {
        // U+001F is the chips delimiter: one embedded separator would split a stored chip.
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(csv("rating,$ts,,3,,wo\u001Frk,,")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(csv("marker,$ts,,,,,,dose\u0000changed")))
        // Track stores `markerDraft.trim()`, so an untrimmed marker is not a storable value.
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(csv("marker,$ts,,,,,,\" padded \"")))
        // A note may hold TAB, LF, and CR because Track stores multi-line note text.
        assertTrue(parse(csv("rating,$ts,,3,,,\"a\tb\nc\",")) is ParseResult.Ok)
    }
}
