package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Backup validation coverage (SPEC-import-restore.md, D-2, D-3, D-5, D-6, D-8).
 *
 * Fixtures are produced by the production `encodeBackup`, so the accepted shape is the
 * one MindScale actually writes rather than a hand-copied approximation.
 */
class BackupImportTest {

    private val exportedAt = Instant.parse("2026-08-03T12:34:56Z")
    private val now = Instant.parse("2026-09-01T00:00:00Z")
    private val zone: ZoneId = ZoneId.of("UTC")

    private val snapshot = DataSnapshot(
        entries = listOf(
            Entry(2, Instant.parse("2026-08-02T09:00:00Z").toEpochMilli(), 7, listOf("work"), "line 1\nline 2", EntryKind.WAKE),
            Entry(1, Instant.parse("2026-08-01T09:00:00Z").toEpochMilli(), 0, emptyList(), null, null)
        ),
        sleeps = listOf(
            SleepInterval(3, Instant.parse("2026-08-01T22:00:00Z").toEpochMilli(), Instant.parse("2026-08-02T06:00:00Z").toEpochMilli())
        ),
        markers = listOf(Marker(4, Instant.parse("2026-08-02T10:00:00Z").toEpochMilli(), "dose, changed")),
        settings = TrackSettings(
            themeMode = ThemeMode.DARK,
            anchor8 = "cannot get out of bed",
            onsetChips = listOf("work", "poor sleep"),
            holdDuration = HoldDuration.TWELVE
        ),
        profile = UserProfile(displayName = "Ada L"),
        externalScores = listOf(
            ExternalScore(
                id = 5,
                instrument = ExternalInstrument.PHQ_8,
                total = 12,
                assessedEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                enteredAt = Instant.parse("2026-08-02T00:00:00Z").toEpochMilli()
            )
        )
    )

    private fun v5(): String = encodeBackup(snapshot, exportedAt)

    private fun parse(text: String) = parseBackup(text, now, zone)

    private fun rejection(text: String): String =
        (parse(text) as ParseResult.Rejected).message

    private fun accepted(text: String) = (parse(text) as ParseResult.Ok).value

    @Test
    fun acceptsTheCurrentExportAndPreservesEveryField() {
        val payload = accepted(v5())
        assertEquals(5, payload.version)
        assertEquals(exportedAt, payload.exportedAt)
        assertEquals(listOf(2L, 1L), payload.entries.map { it.id })
        assertEquals("line 1\nline 2", payload.entries.first().note)
        assertEquals(EntryKind.WAKE, payload.entries.first().kind)
        assertEquals(listOf("work"), payload.entries.first().chips)
        assertEquals(snapshot.sleeps, payload.sleeps)
        assertEquals(snapshot.markers, payload.markers)
        assertEquals(ThemeMode.DARK, payload.settings.themeMode)
        assertEquals(HoldDuration.TWELVE, payload.settings.holdDuration)
        assertEquals(listOf("work", "poor sleep"), payload.settings.onsetChips)
        assertEquals("Ada L", payload.profile.displayName)
        assertEquals(snapshot.externalScores, payload.externalScores)
    }

    @Test
    fun restoreThenExportIsByteIdenticalExceptExportedAt() {
        val original = v5()
        val payload = accepted(original)
        val reexported = encodeBackup(
            DataSnapshot(
                entries = payload.entries,
                sleeps = payload.sleeps,
                markers = payload.markers,
                settings = payload.settings,
                profile = payload.profile,
                externalScores = payload.externalScores
            ),
            payload.exportedAt
        )
        assertEquals(original, reexported)
    }

    @Test
    fun defaultsNotStoredInAnyBackupAreReset() {
        val payload = accepted(
            v5().replace("\"paused\": false", "\"paused\": true")
        )
        assertEquals(0L, payload.settings.checkinAt)
        assertEquals(false, payload.settings.sleepIntroShown)
        assertEquals(false, payload.settings.anchorPromptDone)
        assertEquals(true, payload.settings.paused)
    }

    @Test
    fun acceptsVersionThreeWithDocumentedDefaults() {
        val payload = accepted(V3_BACKUP)
        assertEquals(3, payload.version)
        assertEquals(HoldDuration.SIXTEEN, payload.settings.holdDuration)
        assertEquals("", payload.profile.displayName)
        assertTrue(payload.externalScores.isEmpty())
    }

    @Test
    fun acceptsVersionFourWithHoldButNoProfile() {
        val payload = accepted(
            V3_BACKUP
                .replace("\"version\": 3", "\"version\": 4")
                .replace("\"paused\": false", "\"paused\": false, \"holdHours\": 24")
        )
        assertEquals(4, payload.version)
        assertEquals(HoldDuration.TWENTY_FOUR, payload.settings.holdDuration)
        assertEquals("", payload.profile.displayName)
    }

    @Test
    fun refusesAFutureVersionWithItsOwnMessage() {
        assertEquals(
            ImportMessages.NEWER_VERSION,
            rejection(v5().replace("\"version\": 5", "\"version\": 6"))
        )
    }

    @Test
    fun refusesVersionsOlderThanThreeAndUnusableVersions() {
        assertEquals(
            ImportMessages.UNSUPPORTED_VERSION,
            rejection(V3_BACKUP.replace("\"version\": 3", "\"version\": 2"))
        )
        assertEquals(
            ImportMessages.UNSUPPORTED_VERSION,
            rejection(V3_BACKUP.replace("\"version\": 3", "\"version\": 3.5"))
        )
        assertEquals(
            ImportMessages.UNSUPPORTED_VERSION,
            rejection(V3_BACKUP.replace("\"version\": 3,", ""))
        )
    }

    @Test
    fun refusesForeignAndNonObjectFiles() {
        assertEquals(ImportMessages.NOT_A_BACKUP, rejection("""{"format": "other", "version": 5}"""))
        assertEquals(ImportMessages.NOT_A_BACKUP, rejection("""{"version": 5}"""))
        assertEquals(ImportMessages.NOT_A_BACKUP, rejection("[]"))
    }

    @Test
    fun refusesMalformedJsonWithAPosition() {
        assertTrue(rejection("{").startsWith("This backup is not valid JSON (line 1, column"))
    }

    @Test
    fun refusesUnknownExtraAndMissingKeys() {
        assertEquals(
            ImportMessages.BACKUP_SHAPE,
            rejection(v5().replace("\"exportedAt\":", "\"exportedAtUnknown\":"))
        )
        assertEquals(
            ImportMessages.BACKUP_SHAPE,
            rejection(v5().replace("\"version\": 5,", "\"version\": 5, \"extra\": 1,"))
        )
        // A version-5 file may not omit a version-5 collection and be silently defaulted.
        assertEquals(
            ImportMessages.BACKUP_SHAPE,
            rejection(V3_BACKUP.replace("\"version\": 3", "\"version\": 5"))
        )
    }

    @Test
    fun refusesUnknownEnumNames() {
        assertEquals(ImportMessages.BACKUP_SHAPE, rejection(v5().replace("\"DARK\"", "\"NEON\"")))
        assertEquals(ImportMessages.BACKUP_SHAPE, rejection(v5().replace("\"WAKE\"", "\"DOZE\"")))
        assertEquals(ImportMessages.BACKUP_SHAPE, rejection(v5().replace("\"PHQ_8\"", "\"PHQ_9\"")))
        assertEquals(ImportMessages.BACKUP_SHAPE, rejection(v5().replace("\"holdHours\": 12", "\"holdHours\": 10")))
    }

    @Test
    fun refusesNullWhereTheModelIsNotNullable() {
        assertEquals(ImportMessages.BACKUP_SHAPE, rejection(v5().replace("\"intensity\": 7", "\"intensity\": null")))
        assertEquals(ImportMessages.BACKUP_SHAPE, rejection(v5().replace("\"displayName\": \"Ada L\"", "\"displayName\": null")))
    }

    @Test
    fun refusesValuesOutsideFrozenBounds() {
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"intensity\": 7", "\"intensity\": 11")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"intensity\": 7", "\"intensity\": -1")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"id\": 2,", "\"id\": 0,")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"total\": 12", "\"total\": 25")))
        assertTrue(parse(v5().replace("\"total\": 12", "\"total\": 24")) is ParseResult.Ok)
        assertTrue(parse(v5().replace("\"total\": 12", "\"total\": 0")) is ParseResult.Ok)
    }

    @Test
    fun gad7UsesItsOwnFrozenRange() {
        val gad = v5().replace("\"PHQ_8\"", "\"GAD_7\"")
        assertTrue(parse(gad.replace("\"total\": 12", "\"total\": 21")) is ParseResult.Ok)
        assertEquals(
            ImportMessages.UNSTORABLE_VALUE,
            rejection(gad.replace("\"total\": 12", "\"total\": 22"))
        )
    }

    @Test
    fun refusesFutureTimestampsAndAssessmentDates() {
        assertEquals(
            ImportMessages.UNSTORABLE_VALUE,
            rejection(v5().replace("\"2026-08-02T10:00:00Z\"", "\"2030-01-01T00:00:00Z\""))
        )
        assertEquals(
            ImportMessages.UNSTORABLE_VALUE,
            rejection(v5().replace("\"assessedDate\": \"2026-08-01\"", "\"assessedDate\": \"2030-01-01\""))
        )
        // Up to a day of exporting-device clock skew is tolerated, and only that.
        assertTrue(
            parse(v5().replace("\"2026-08-02T10:00:00Z\"", "\"2026-09-01T12:00:00Z\"")) is ParseResult.Ok
        )
    }

    @Test
    fun acceptsTheMicrosecondExportedAtThatTheExporterActuallyWrites() {
        // `encodeBackup` writes `Instant.now()`, which carries microsecond precision on
        // API 26+. A real device export was rejected before this was allowed.
        val real = v5().replace("\"2026-08-03T12:34:56Z\"", "\"2026-08-05T04:03:37.707072Z\"")
        val payload = accepted(real)
        assertEquals(Instant.parse("2026-08-05T04:03:37.707072Z"), payload.exportedAt)
        assertEquals(real, encodeBackup(snapshot, payload.exportedAt))
    }

    @Test
    fun refusesNonCanonicalOrSubMillisecondInstants() {
        assertEquals(
            ImportMessages.BACKUP_SHAPE,
            rejection(v5().replace("\"2026-08-02T10:00:00Z\"", "\"2026-08-02T10:00:00.000Z\""))
        )
        assertEquals(
            ImportMessages.UNSTORABLE_VALUE,
            rejection(v5().replace("\"2026-08-02T10:00:00Z\"", "\"2026-08-02T10:00:00.000000001Z\""))
        )
    }

    @Test
    fun refusesControlCharactersThatWouldCorruptStoredFields() {
        // U+001F is the chips delimiter; one embedded separator would split a stored chip.
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"work\"", "\"wo\\u001frk\"")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"Ada L\"", "\"Ada\\nL\"")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("cannot get out of bed", "cannot\\u0007stop")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("dose, changed", "dose\\u0000changed")))
    }

    @Test
    fun keepsMultiLineNotesBecauseTrackStoresThem() {
        assertEquals("line 1\nline 2", accepted(v5()).entries.first().note)
        assertTrue(parse(v5().replace("line 1\\nline 2", "a\\tb\\r\\nc")) is ParseResult.Ok)
    }

    @Test
    fun refusesValuesTheAppsOwnValidatorsWouldReject() {
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"Ada L\"", "\" Ada L \"")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("dose, changed", " dose ")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"work\", \"poor sleep\"", "\"work\", \"WORK\"")))
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejection(v5().replace("\"work\", \"poor sleep\"", "")))
        assertEquals(
            ImportMessages.UNSTORABLE_VALUE,
            rejection(v5().replace("\"Ada L\"", "\"" + "n".repeat(MAX_DISPLAY_NAME_CODE_POINTS + 1) + "\""))
        )
    }

    @Test
    fun refusesDuplicateIdsAndDuplicateAssessments() {
        assertEquals(ImportMessages.duplicates(1), rejection(v5().replace("\"id\": 1,", "\"id\": 2,")))
        val twoScores = v5().replace(
            "\"externalScores\": [",
            "\"externalScores\": [\n    {\"id\": 9, \"instrument\": \"PHQ_8\", \"total\": 3, " +
                "\"assessedDate\": \"2026-08-01\", \"provenance\": " +
                "\"EXTERNALLY_OBTAINED_USER_ENTERED\", \"enteredAt\": \"2026-08-02T00:00:00Z\"},"
        )
        assertEquals(ImportMessages.duplicates(1), rejection(twoScores))
    }

    @Test
    fun requiresTheFrozenExternalProvenance() {
        assertEquals(
            ImportMessages.BACKUP_SHAPE,
            rejection(v5().replace("\"EXTERNALLY_OBTAINED_USER_ENTERED\"", "\"CALCULATED_BY_MINDSCALE\""))
        )
        assertEquals(
            com.kieslingdev.mindscale.data.ExternalScoreProvenance.EXTERNALLY_OBTAINED_USER_ENTERED,
            accepted(v5()).externalScores.single().provenance
        )
    }

    @Test
    fun refusesOverlappingOrDoublyOpenSleepPeriods() {
        val overlapping = v5().replace(
            "\"sleeps\": [",
            "\"sleeps\": [\n    {\"id\": 7, \"start\": \"2026-08-02T01:00:00Z\", \"end\": \"2026-08-02T09:00:00Z\"},"
        )
        assertEquals(ImportMessages.SLEEP_OVERLAP, rejection(overlapping))

        val twoOpen = v5()
            .replace("\"end\": \"2026-08-02T06:00:00Z\"", "\"end\": null")
            .replace(
                "\"sleeps\": [",
                "\"sleeps\": [\n    {\"id\": 8, \"start\": \"2026-07-01T22:00:00Z\", \"end\": null},"
            )
        assertEquals(ImportMessages.SLEEP_OPEN, rejection(twoOpen))
    }

    @Test
    fun refusesASleepThatEndsBeforeItStarts() {
        assertEquals(
            ImportMessages.UNSTORABLE_VALUE,
            rejection(v5().replace("\"end\": \"2026-08-02T06:00:00Z\"", "\"end\": \"2026-08-01T06:00:00Z\""))
        )
    }

    @Test
    fun refusesMoreRecordsThanTheFrozenLimit() {
        val entry = { id: Int ->
            "{\"id\": $id, \"timestamp\": \"2026-08-01T09:00:00Z\", \"intensity\": 1, " +
                "\"chips\": [], \"note\": null, \"kind\": null}"
        }
        val many = (1..MAX_RECORDS_PER_COLLECTION + 1).joinToString(",", transform = entry)
        val text = V3_BACKUP.replace(
            "\"entries\": [{\"id\": 1, \"timestamp\": \"2026-08-01T09:00:00Z\", \"intensity\": 5, " +
                "\"chips\": [], \"note\": null, \"kind\": null}]",
            "\"entries\": [$many]"
        )
        assertEquals(ImportMessages.TOO_MANY_RECORDS, rejection(text))
    }
}

private val V3_BACKUP = """
{
  "format": "mindscale-backup",
  "version": 3,
  "exportedAt": "2026-08-03T12:34:56Z",
  "entries": [{"id": 1, "timestamp": "2026-08-01T09:00:00Z", "intensity": 5, "chips": [], "note": null, "kind": null}],
  "sleeps": [],
  "markers": [],
  "settings": {"themeMode": "SYSTEM", "hourFormat": "TWELVE", "anchor2": "", "anchor5": "", "anchor8": "", "onsetChips": ["work"], "sleepOn": true, "askChips": false, "hideNotes": false, "paused": false}
}
""".trimIndent()
