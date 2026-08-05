package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.SafetyPlanStep
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Backup version 6 — the safety plan (`docs/specs/SPEC-safety-card.md`, D-5, D-9).
 *
 * Phase 12's total-rejection policy is unchanged here: every violation refuses the whole
 * file. Nothing is renumbered, trimmed, or repaired, because a "helpful" repair would
 * store an order or a value the user never wrote.
 */
class SafetyBackupTest {

    private val exportedAt = Instant.parse("2026-08-05T10:00:00Z")

    private val plan = listOf(
        SafetyPlanItem(41, SafetyPlanStep.WARNING_SIGNS, 0, "Sleeping in"),
        SafetyPlanItem(42, SafetyPlanStep.WARNING_SIGNS, 1, "Cancelling plans"),
        SafetyPlanItem(43, SafetyPlanStep.PEOPLE_FOR_HELP, 0, "Sam", "+1 (555) 010-0199"),
        SafetyPlanItem(44, SafetyPlanStep.ENVIRONMENT_SAFETY, 0, "Sam keeps the spare keys")
    )

    private fun snapshot(plan: List<SafetyPlanItem> = this.plan) = DataSnapshot(
        entries = listOf(Entry(id = 1, ts = 1_000, value = 4)),
        sleeps = listOf(SleepInterval(id = 2, startTs = 2_000, endTs = 3_000)),
        markers = listOf(Marker(id = 3, ts = 4_000, text = "dose change")),
        settings = TrackSettings(),
        profile = UserProfile(displayName = "Ada"),
        externalScores = emptyList(),
        safetyPlan = plan
    )

    @Test
    fun exportIsVersionSevenAndOrderedByCanonicalStepThenPosition() {
        // Deliberately shuffled: the encoder, not the caller, owns the order.
        val encoded = encodeBackup(snapshot(plan.reversed()), exportedAt)

        assertTrue(encoded.contains("\"version\": 7,"))
        val steps = Regex("\"step\": \"([A-Z_]+)\"").findAll(encoded).map { it.groupValues[1] }
        assertEquals(
            listOf("WARNING_SIGNS", "WARNING_SIGNS", "PEOPLE_FOR_HELP", "ENVIRONMENT_SAFETY"),
            steps.toList()
        )
        assertTrue(
            encoded.contains(
                "{\"id\": 43, \"step\": \"PEOPLE_FOR_HELP\", \"position\": 0, " +
                    "\"text\": \"Sam\", \"phone\": \"+1 (555) 010-0199\"}"
            )
        )
        assertTrue(
            encoded.contains(
                "{\"id\": 41, \"step\": \"WARNING_SIGNS\", \"position\": 0, " +
                    "\"text\": \"Sleeping in\", \"phone\": null}"
            )
        )
    }

    @Test
    fun anEmptyPlanStillWritesTheKey() {
        val encoded = encodeBackup(snapshot(emptyList()), exportedAt)
        assertTrue(encoded.contains("\"safetyPlan\": []"))
    }

    @Test
    fun aVersionSevenBackupRoundTripsByteForByte() {
        val encoded = encodeBackup(snapshot(), exportedAt)
        val parsed = parseBackup(encoded) as ParseResult.Ok
        assertEquals(7, parsed.value.version)
        assertEquals(plan, parsed.value.safetyPlan)
        assertEquals(encoded, encodeBackup(snapshot(parsed.value.safetyPlan), exportedAt))
    }

    @Test
    fun versionsThreeFourAndFiveRestoreWithAnEmptyPlan() {
        listOf(3, 4, 5).forEach { version ->
            val parsed = parseBackup(legacyBackup(version)) as ParseResult.Ok
            assertEquals("version $version", emptyList<SafetyPlanItem>(), parsed.value.safetyPlan)
        }
    }

    @Test
    fun aVersionEightBackupIsRejectedAsNewer() {
        val newer = encodeBackup(snapshot(), exportedAt).replace("\"version\": 7,", "\"version\": 8,")
        val rejected = parseBackup(newer) as ParseResult.Rejected
        assertEquals(ImportMessages.NEWER_VERSION, rejected.message)
    }

    @Test
    fun aVersionSevenFileMissingTheSafetyPlanKeyIsRejected() {
        val encoded = encodeBackup(snapshot(emptyList()), exportedAt)
            .replace(",\n  \"safetyPlan\": []", "")
        val rejected = parseBackup(encoded) as ParseResult.Rejected
        assertEquals(ImportMessages.BACKUP_SHAPE, rejected.message)
    }

    @Test
    fun anUnknownStepNameIsRejected() {
        val encoded = encodeBackup(snapshot(), exportedAt)
            .replace("\"step\": \"WARNING_SIGNS\"", "\"step\": \"WARNING\"", ignoreCase = false)
        val rejected = parseBackup(encoded) as ParseResult.Rejected
        assertEquals(ImportMessages.BACKUP_SHAPE, rejected.message)
    }

    @Test
    fun nonContiguousPositionsAreRejectedRatherThanRenumbered() {
        val gapped = listOf(
            SafetyPlanItem(41, SafetyPlanStep.WARNING_SIGNS, 0, "Sleeping in"),
            SafetyPlanItem(42, SafetyPlanStep.WARNING_SIGNS, 2, "Cancelling plans")
        )
        val rejected = parseBackup(encodeBackup(snapshot(gapped), exportedAt)) as ParseResult.Rejected
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejected.message)
    }

    @Test
    fun duplicatePlanIdsAreRejected() {
        val duplicated = listOf(
            SafetyPlanItem(41, SafetyPlanStep.WARNING_SIGNS, 0, "Sleeping in"),
            SafetyPlanItem(41, SafetyPlanStep.INTERNAL_COPING, 0, "Shower")
        )
        val rejected =
            parseBackup(encodeBackup(snapshot(duplicated), exportedAt)) as ParseResult.Rejected
        assertEquals(ImportMessages.duplicates(1), rejected.message)
    }

    @Test
    fun aPhoneOnANonContactStepIsRejected() {
        val encoded = encodeBackup(snapshot(), exportedAt).replace(
            "{\"id\": 41, \"step\": \"WARNING_SIGNS\", \"position\": 0, " +
                "\"text\": \"Sleeping in\", \"phone\": null}",
            "{\"id\": 41, \"step\": \"WARNING_SIGNS\", \"position\": 0, " +
                "\"text\": \"Sleeping in\", \"phone\": \"5550100\"}"
        )
        val rejected = parseBackup(encoded) as ParseResult.Rejected
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejected.message)
    }

    @Test
    fun anOverLongPlanTextIsRejected() {
        val long = listOf(SafetyPlanItem(41, SafetyPlanStep.WARNING_SIGNS, 0, "a".repeat(201)))
        val rejected = parseBackup(encodeBackup(snapshot(long), exportedAt)) as ParseResult.Rejected
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejected.message)
    }

    @Test
    fun aBlankPlanTextIsRejected() {
        val blank = listOf(SafetyPlanItem(41, SafetyPlanStep.WARNING_SIGNS, 0, "   "))
        val rejected = parseBackup(encodeBackup(snapshot(blank), exportedAt)) as ParseResult.Rejected
        assertEquals(ImportMessages.UNSTORABLE_VALUE, rejected.message)
    }

    @Test
    fun tooManyLinesInOneStepIsRejected() {
        val overflowing = (0..10).map {
            SafetyPlanItem(50L + it, SafetyPlanStep.INTERNAL_COPING, it, "line $it")
        }
        val rejected =
            parseBackup(encodeBackup(snapshot(overflowing), exportedAt)) as ParseResult.Rejected
        assertEquals(ImportMessages.TOO_MANY_RECORDS, rejected.message)
    }

    /** A safety plan is not a symptom record, so the records CSV is unchanged (D-5). */
    @Test
    fun theRecordsCsvIsUnaffectedByThePlan() {
        assertEquals(
            encodeRecordsCsv(snapshot(emptyList())),
            encodeRecordsCsv(snapshot())
        )
    }

    /** A version-3 backup, byte-shaped like the ones Phase 4 wrote. */
    private fun legacyBackup(version: Int): String {
        val holdLine = if (version >= 4) ",\n    \"holdHours\": 16" else ""
        val profileAndScores = if (version >= 5) {
            ",\n  \"profile\": {\"displayName\": \"\"},\n  \"externalScores\": []"
        } else {
            ""
        }
        return """
            {
              "format": "mindscale-backup",
              "version": $version,
              "exportedAt": "2026-08-05T10:00:00Z",
              "entries": [],
              "sleeps": [],
              "markers": [],
              "settings": {
                "themeMode": "SYSTEM",
                "hourFormat": "TWELVE",
                "anchor2": "",
                "anchor5": "",
                "anchor8": "",
                "onsetChips": ["flat"],
                "sleepOn": true,
                "askChips": false,
                "hideNotes": false,
                "paused": false$holdLine
              }$profileAndScores
            }
        """.trimIndent()
    }
}
