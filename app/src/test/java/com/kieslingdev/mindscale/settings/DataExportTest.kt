package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.BreathingSession
import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataExportTest {
    private val snapshot = DataSnapshot(
        entries = listOf(
            Entry(2, 2_000, 7, listOf("work", "a|b"), "line 1\n\"quoted\"", EntryKind.WAKE)
        ),
        sleeps = listOf(SleepInterval(3, 1_000, 5_000)),
        markers = listOf(Marker(4, 3_000, "dose, changed")),
        settings = TrackSettings(anchor8 = "can't get out of bed", onsetChips = listOf("work")),
        profile = UserProfile(displayName = "Ada \"A\""),
        externalScores = listOf(
            ExternalScore(
                id = 5,
                instrument = ExternalInstrument.PHQ_8,
                total = 12,
                assessedEpochDay = java.time.LocalDate.of(2026, 8, 1).toEpochDay(),
                enteredAt = Instant.parse("2026-08-02T00:00:00Z").toEpochMilli()
            )
        )
    )

    @Test
    fun backupIsVersionedEscapedAndDeterministic() {
        val at = Instant.parse("2026-08-03T12:34:56Z")
        val first = encodeBackup(snapshot, at)
        assertEquals(first, encodeBackup(snapshot, at))
        assertTrue(first.contains("\"format\": \"mindscale-backup\""))
        assertTrue(first.contains("\"version\": 7"))
        assertTrue(first.contains("\"holdHours\": 16"))
        assertTrue(first.contains("line 1\\n\\\"quoted\\\""))
        assertTrue(first.contains("\"kind\": \"WAKE\""))
        assertTrue(first.contains("\"profile\": {\"displayName\": \"Ada \\\"A\\\"\"}"))
        assertTrue(first.contains("\"instrument\": \"PHQ_8\""))
        assertTrue(first.contains("\"assessedDate\": \"2026-08-01\""))
        assertTrue(first.contains("\"provenance\": \"EXTERNALLY_OBTAINED_USER_ENTERED\""))
        assertTrue(first.contains("\"breathingOn\": true"))
        assertEquals("mindscale-backup-20260803-123456.json", backupFilename(at))
    }

    @Test
    fun csvUsesRfc4180QuotingAndCrlf() {
        val csv = encodeRecordsCsv(snapshot)
        assertTrue(csv.startsWith("record_type,timestamp,end_timestamp,intensity,kind,chips,note,text\r\n"))
        assertTrue(csv.contains("\"line 1\n\"\"quoted\"\"\""))
        assertTrue(csv.contains("\"dose, changed\""))
        assertTrue(csv.endsWith("\r\n"))
        assertTrue(!csv.contains("PHQ_8"))
        assertTrue(!csv.contains("Ada"))
        assertEquals("mindscale-records-20260803-123456.csv", recordsFilename(Instant.parse("2026-08-03T12:34:56Z")))
    }

    @Test
    fun aSnapshotWithNoBreathingSessionsProducesTheSameCsvAsBeforePhase14() {
        // `snapshot` has no breathing sessions, so this pins that adding the feature did
        // not change a single byte of an export that carries none.
        assertEquals(
            "record_type,timestamp,end_timestamp,intensity,kind,chips,note,text\r\n" +
                "rating,1970-01-01T00:00:02Z,,7,WAKE,work|a|b,\"line 1\n\"\"quoted\"\"\",\r\n" +
                "sleep,1970-01-01T00:00:01Z,1970-01-01T00:00:05Z,,,,,\r\n" +
                "marker,1970-01-01T00:00:03Z,,,,,,\"dose, changed\"\r\n",
            encodeRecordsCsv(snapshot)
        )
    }

    @Test
    fun encodeRecordsCsvWritesABreathingRowOnTheUnchangedHeader() {
        val withBreathing = snapshot.copy(
            breathingSessions = listOf(BreathingSession(id = 9, startedAt = 10_000, endedAt = 70_000))
        )
        val csv = encodeRecordsCsv(withBreathing)
        assertTrue(csv.startsWith("$RECORDS_CSV_HEADER\r\n"))
        assertTrue(csv.contains("breathing,1970-01-01T00:00:10Z,1970-01-01T00:01:10Z,,,,,\r\n"))
    }

    @Test
    fun encodeBackupOrdersBreathingSessionsByStartedAtThenIdDescending() {
        val withBreathing = snapshot.copy(
            breathingSessions = listOf(
                BreathingSession(id = 1, startedAt = 1_000, endedAt = 2_000),
                BreathingSession(id = 3, startedAt = 5_000, endedAt = 6_000),
                BreathingSession(id = 2, startedAt = 5_000, endedAt = 7_000)
            )
        )
        val at = Instant.parse("2026-08-03T12:34:56Z")
        val encoded = encodeBackup(withBreathing, at)
        val ids = Regex("\"breathingSessions\": \\[([\\s\\S]*?)\\]").find(encoded)!!.groupValues[1]
        val order = Regex("\"id\": (\\d+)").findAll(ids).map { it.groupValues[1].toInt() }.toList()
        assertEquals(listOf(3, 2, 1), order)
    }
}
