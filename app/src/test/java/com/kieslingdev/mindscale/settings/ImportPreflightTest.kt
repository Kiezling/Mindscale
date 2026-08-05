package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.BackupPayload
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.SafetyPlanStep
import com.kieslingdev.mindscale.data.RecordSnapshot
import com.kieslingdev.mindscale.data.RecordsPayload
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bounded reading, conflict rules, and preview copy (SPEC-import-restore.md, D-4..D-7). */
class ImportPreflightTest {

    private fun read(bytes: ByteArray) = readBoundedUtf8(ByteArrayInputStream(bytes))

    @Test
    fun readsUpToTheLimitAndStopsPastIt() {
        val atLimit = ByteArray(MAX_IMPORT_BYTES.toInt()) { 'a'.code.toByte() }
        assertEquals(MAX_IMPORT_BYTES.toInt(), (read(atLimit) as ParseResult.Ok).value.length)

        val overLimit = ByteArray(MAX_IMPORT_BYTES.toInt() + 1) { 'a'.code.toByte() }
        assertEquals(ImportMessages.TOO_LARGE, (read(overLimit) as ParseResult.Rejected).message)
    }

    @Test
    fun refusesInvalidUtf8RatherThanSubstitutingReplacementCharacters() {
        val malformed = byteArrayOf(0x7B, 0xC3.toByte(), 0x28, 0x7D)
        assertEquals(ImportMessages.NOT_UTF8, (read(malformed) as ParseResult.Rejected).message)
        val truncated = byteArrayOf(0xE2.toByte(), 0x82.toByte())
        assertEquals(ImportMessages.NOT_UTF8, (read(truncated) as ParseResult.Rejected).message)
    }

    @Test
    fun stripsExactlyOneLeadingByteOrderMark() {
        val text = (read("﻿{}".toByteArray(Charsets.UTF_8)) as ParseResult.Ok).value
        assertEquals("{}", text)
        val doubled = (read("﻿﻿{}".toByteArray(Charsets.UTF_8)) as ParseResult.Ok).value
        assertEquals("﻿{}", doubled)
        // The survivor is then refused by the grammar rather than silently accepted.
        assertTrue(parseBackup(doubled) is ParseResult.Rejected)
    }

    @Test
    fun refusesDuplicatesWithinTheFileWithoutDeduplicating() {
        val rating = Entry(ts = 1_000, value = 4, chips = listOf("work"), note = "n", kind = null)
        val payload = RecordsPayload(listOf(rating, rating.copy()), emptyList(), emptyList())
        assertEquals(
            ImportMessages.duplicates(1),
            (checkRecordConflicts(payload, emptySnapshot()) as ParseResult.Rejected).message
        )
    }

    @Test
    fun refusesRecordsThatAlreadyExistWithoutSkippingThem() {
        val stored = Entry(id = 9, ts = 1_000, value = 4)
        val payload = RecordsPayload(listOf(Entry(ts = 1_000, value = 4)), emptyList(), emptyList())
        assertEquals(
            ImportMessages.conflicts(1),
            (checkRecordConflicts(payload, RecordSnapshot(listOf(stored), emptyList(), emptyList()))
                as ParseResult.Rejected).message
        )
    }

    @Test
    fun treatsEveryNaturalFieldAsPartOfIdentity() {
        val stored = Entry(id = 9, ts = 1_000, value = 4, chips = listOf("work"), note = "n", kind = EntryKind.WAKE)
        val snapshot = RecordSnapshot(listOf(stored), emptyList(), emptyList())
        val differsByNote = Entry(ts = 1_000, value = 4, chips = listOf("work"), note = "other", kind = EntryKind.WAKE)
        val differsByKind = Entry(ts = 1_000, value = 4, chips = listOf("work"), note = "n", kind = EntryKind.SLEEP)
        val differsByChips = Entry(ts = 1_000, value = 4, chips = emptyList(), note = "n", kind = EntryKind.WAKE)
        listOf(differsByNote, differsByKind, differsByChips).forEach { candidate ->
            val result = checkRecordConflicts(
                RecordsPayload(listOf(candidate), emptyList(), emptyList()), snapshot
            )
            assertTrue(result is ParseResult.Ok)
        }
    }

    @Test
    fun refusesSleepPeriodsThatOverlapStoredOnesButAllowsTouchingOnes() {
        val stored = RecordSnapshot(emptyList(), listOf(SleepInterval(1, 1_000, 2_000)), emptyList())
        val overlapping = RecordsPayload(emptyList(), listOf(SleepInterval(startTs = 1_500, endTs = 2_500)), emptyList())
        assertEquals(
            ImportMessages.SLEEP_OVERLAP,
            (checkRecordConflicts(overlapping, stored) as ParseResult.Rejected).message
        )
        val touching = RecordsPayload(emptyList(), listOf(SleepInterval(startTs = 2_000, endTs = 3_000)), emptyList())
        assertTrue(checkRecordConflicts(touching, stored) is ParseResult.Ok)
    }

    @Test
    fun refusesASecondOpenSleepPeriod() {
        val storedOpen = RecordSnapshot(emptyList(), listOf(SleepInterval(1, 1_000, null)), emptyList())
        val alsoOpen = RecordsPayload(emptyList(), listOf(SleepInterval(startTs = 5_000, endTs = null)), emptyList())
        assertEquals(
            ImportMessages.SLEEP_OPEN,
            (checkRecordConflicts(alsoOpen, storedOpen) as ParseResult.Rejected).message
        )
        val twoOpenInFile = RecordsPayload(
            emptyList(),
            listOf(SleepInterval(startTs = 5_000, endTs = null), SleepInterval(startTs = 9_000, endTs = null)),
            emptyList()
        )
        assertEquals(
            ImportMessages.SLEEP_OPEN,
            (checkRecordConflicts(twoOpenInFile, emptySnapshot()) as ParseResult.Rejected).message
        )
    }

    @Test
    fun restorePreviewStatesExactCountsAndEveryDisclosedDefault() {
        val preview = previewOf(
            ImportPayload.Restore(backup(version = 6, scores = 1, planItems = 2)),
            RecordCounts(entries = 3, sleeps = 2, markers = 1, externalScores = 4, safetyPlanItems = 5)
        )
        assertEquals("Restore from backup", preview.title)
        assertEquals("Replace everything", preview.confirmLabel)
        val text = preview.lines.joinToString("\n")
        assertTrue(text.contains("version 6, created 2026-08-03T12:34:56Z"))
        assertTrue(text.contains("It contains 1 rating, 1 sleep period, 1 marked event, and 1 externally obtained total, plus your settings, Profile name, and safety plan."))
        assertTrue(text.contains("permanently delete 3 ratings, 2 sleep periods, 1 marked event, and 4 externally obtained totals, and will replace your settings, Profile name, and safety plan."))
        assertTrue(text.contains("Check-in time, the sleep introduction flag, and the anchor prompt flag are not stored in any backup file."))
        assertTrue(text.contains("This backup contains 2 safety plan lines. Your current safety plan of 5 lines is permanently deleted."))
        assertTrue(text.contains("MindScale did not administer or calculate them."))
        assertTrue(text.endsWith("Nothing has changed yet."))
        assertFalse(text.contains("predates"))
    }

    /**
     * A restore that silently emptied a safety plan would be the worst kind of quiet data
     * loss, so a pre-version-6 backup says so before anything is written (Phase 13, D-9).
     */
    @Test
    fun aBackupWithoutASafetyPlanSaysSoBeforeAnythingIsWritten() {
        val text = previewOf(
            ImportPayload.Restore(backup(version = 5)),
            RecordCounts(safetyPlanItems = 1)
        ).lines.joinToString("\n")
        assertTrue(
            text.contains(
                "This backup predates the safety plan. Your safety plan of 1 line is " +
                    "permanently deleted and nothing replaces it."
            )
        )
        // A pre-version-6 file must not be described as containing a safety plan.
        assertTrue(text.contains("plus your settings and Profile name."))
        assertFalse(text.contains("plus your settings, Profile name, and safety plan."))
    }

    @Test
    fun olderVersionsDiscloseTheirOwnDefaults() {
        val three = previewOf(ImportPayload.Restore(backup(version = 3)), RecordCounts()).lines.joinToString("\n")
        assertTrue(three.contains("The hold returns to 16 waking hours."))
        assertTrue(three.contains("Your Profile name will be empty and no totals will be restored."))

        val four = previewOf(ImportPayload.Restore(backup(version = 4)), RecordCounts()).lines.joinToString("\n")
        assertFalse(four.contains("The hold returns to"))
        assertTrue(four.contains("Your Profile name will be empty and no totals will be restored."))
    }

    @Test
    fun mergePreviewPromisesNoDeletionAndNoSettingsChange() {
        val preview = previewOf(
            ImportPayload.Merge(
                RecordsPayload(
                    listOf(Entry(ts = 1, value = 1), Entry(ts = 2, value = 2)),
                    emptyList(),
                    listOf(Marker(ts = 3, text = "m"))
                )
            ),
            RecordCounts(entries = 99)
        )
        assertEquals("Import records", preview.title)
        assertEquals("Add these records", preview.confirmLabel)
        val text = preview.lines.joinToString("\n")
        assertTrue(text.contains("It will add 2 ratings, 0 sleep periods, and 1 marked event."))
        assertTrue(text.contains("Nothing is deleted."))
        assertTrue(text.contains("A records CSV does not contain them."))
        assertFalse(text.contains("delete "))
    }

    @Test
    fun noImportCopyUsesInterpretiveOrClinicalLanguage() {
        val banned = listOf(
            "better", "worse", "improved", "declined", "higher", "lower", "symptom-free",
            "typical", "normal", "abnormal", "mild", "moderate", "severe", "risk",
            "diagnos", "treatment", "should", "recommend", "safe", "don't worry"
        )
        val copy = buildList {
            addAll(previewOf(ImportPayload.Restore(backup(version = 3, scores = 1)), RecordCounts(1, 1, 1, 1)).lines)
            addAll(previewOf(ImportPayload.Restore(backup(version = 5, scores = 1)), RecordCounts(1, 1, 1, 1)).lines)
            addAll(previewOf(ImportPayload.Merge(RecordsPayload(emptyList(), emptyList(), emptyList())), RecordCounts()).lines)
            addAll(previewOf(ImportPayload.Restore(backup(version = 6, scores = 1)), RecordCounts(1, 1, 1, 1, 1)).lines)
            add(ImportMessages.restored(1, 1, 1, 1, 1))
            add(ImportMessages.added(2, 2, 2))
            addAll(
                listOf(
                    ImportMessages.TOO_LARGE, ImportMessages.NOT_UTF8, ImportMessages.NOT_A_BACKUP,
                    ImportMessages.NEWER_VERSION, ImportMessages.UNSUPPORTED_VERSION,
                    ImportMessages.BACKUP_SHAPE, ImportMessages.UNSTORABLE_VALUE,
                    ImportMessages.TOO_MANY_RECORDS, ImportMessages.NOT_A_RECORDS_CSV,
                    ImportMessages.SLEEP_OVERLAP, ImportMessages.SLEEP_OPEN,
                    ImportMessages.RECORDS_CHANGED, ImportMessages.READ_FAILED,
                    ImportMessages.IMPORT_FAILED, ImportMessages.PREVIEW_LOST,
                    ImportMessages.duplicates(2), ImportMessages.conflicts(2),
                    ImportMessages.invalidJson(1, 1), ImportMessages.invalidCsv(1)
                )
            )
        }.joinToString("\n").lowercase()
            // "safe" is banned as reassurance ("your data is safe"). Phase 13 introduced
            // "safety plan" as the name of a stored thing, which is a noun, not a claim,
            // so it is removed before the scan rather than weakening the ban itself.
            .replace("safety plan", "")

        banned.forEach { word ->
            assertFalse("Import copy must not contain \"$word\"", copy.contains(word))
        }
    }

    private fun emptySnapshot() = RecordSnapshot(emptyList(), emptyList(), emptyList())

    private fun backup(version: Int, scores: Int = 0, planItems: Int = 0) = BackupPayload(
        safetyPlan = (0 until planItems).map {
            SafetyPlanItem(
                id = 40L + it,
                step = SafetyPlanStep.WARNING_SIGNS,
                position = it,
                text = "sign $it"
            )
        },
        version = version,
        exportedAt = Instant.parse("2026-08-03T12:34:56Z"),
        entries = listOf(Entry(id = 1, ts = 1_000, value = 4)),
        sleeps = listOf(SleepInterval(id = 2, startTs = 2_000, endTs = 3_000)),
        markers = listOf(Marker(id = 3, ts = 4_000, text = "event")),
        settings = TrackSettings(),
        profile = UserProfile(),
        externalScores = (1..scores).map {
            ExternalScore(
                id = it.toLong(),
                instrument = ExternalInstrument.PHQ_8,
                total = 4,
                assessedEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
                enteredAt = 5_000
            )
        }
    )
}
