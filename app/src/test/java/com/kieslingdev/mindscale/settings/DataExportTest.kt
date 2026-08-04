package com.kieslingdev.mindscale.settings

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
        assertTrue(first.contains("\"version\": 5"))
        assertTrue(first.contains("\"holdHours\": 16"))
        assertTrue(first.contains("line 1\\n\\\"quoted\\\""))
        assertTrue(first.contains("\"kind\": \"WAKE\""))
        assertTrue(first.contains("\"profile\": {\"displayName\": \"Ada \\\"A\\\"\"}"))
        assertTrue(first.contains("\"instrument\": \"PHQ_8\""))
        assertTrue(first.contains("\"assessedDate\": \"2026-08-01\""))
        assertTrue(first.contains("\"provenance\": \"EXTERNALLY_OBTAINED_USER_ENTERED\""))
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
}
