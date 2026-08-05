package com.kieslingdev.mindscale.settings

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A byte-for-byte copy of a real API 36 device export, kept as a regression fixture so a
 * parser change can never again leave MindScale unable to read its own backup. This is how
 * the microsecond `exportedAt` defect was found: `encodeBackup` writes `Instant.now()`,
 * which is finer than millisecond precision on a real device.
 */
class RealDeviceBackupTest {

    @Test
    fun parsesAnActualDeviceExport() {
        val result = parseBackup(
            REAL_EXPORT,
            Instant.parse("2026-08-05T04:20:00Z"),
            ZoneId.of("UTC")
        )
        val payload = (result as ParseResult.Ok).value
        assertEquals(5, payload.version)
        assertEquals(Instant.parse("2026-08-05T04:03:37.707072Z"), payload.exportedAt)
        assertEquals(listOf(4L, 3L, 2L, 1L), payload.entries.map { it.id })
        assertEquals(listOf(10, 0, 3, 7), payload.entries.map { it.value })
        assertEquals("Ada L", payload.profile.displayName)
        assertEquals(12, payload.externalScores.single().total)
    }
}

private val REAL_EXPORT = """
{
  "format": "mindscale-backup",
  "version": 5,
  "exportedAt": "2026-08-05T04:03:37.707072Z",
  "entries": [
    {"id": 4, "timestamp": "2026-08-05T04:00:31.843Z", "intensity": 10, "chips": [], "note": null, "kind": null},
    {"id": 3, "timestamp": "2026-08-05T04:00:30.063Z", "intensity": 0, "chips": [], "note": null, "kind": null},
    {"id": 2, "timestamp": "2026-08-05T04:00:29.284Z", "intensity": 3, "chips": [], "note": null, "kind": null},
    {"id": 1, "timestamp": "2026-08-05T04:00:28.185Z", "intensity": 7, "chips": [], "note": null, "kind": null}
  ],
  "sleeps": [],
  "markers": [],
  "settings": {
    "themeMode": "DARK",
    "hourFormat": "TWENTY_FOUR",
    "anchor2": "",
    "anchor5": "",
    "anchor8": "",
    "onsetChips": ["flat", "agitated", "hopeless", "numb", "wired", "foggy", "alone", "driving", "work", "poor sleep"],
    "sleepOn": true,
    "askChips": false,
    "hideNotes": false,
    "paused": false,
    "holdHours": 8
  },
  "profile": {"displayName": "Ada L"},
  "externalScores": [
    {"id": 1, "instrument": "PHQ_8", "total": 12, "assessedDate": "2026-08-04", "provenance": "EXTERNALLY_OBTAINED_USER_ENTERED", "enteredAt": "2026-08-05T04:02:27.981Z"}
  ]
}
""".trimIndent()
