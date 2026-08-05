package com.kieslingdev.mindscale.data

import java.time.Instant

/**
 * Validated Phase 12 import payloads and the counts an import reports.
 *
 * `docs/specs/SPEC-import-restore.md` froze these shapes under the `settings` package
 * alongside the parsers. They are declared here instead, unchanged in name, field, and
 * meaning, so that `DataControlDao` can accept them without `data` depending on
 * `settings` — a package cycle, since every parser already depends on these entities.
 * This is a placement amendment only; no frozen behavior, field, or type name changes.
 *
 * A value of either type has already passed every size, version, structural, semantic,
 * duplicate, and conflict rule. Nothing here re-validates, and nothing may construct one
 * from unvalidated input.
 */

data class BackupPayload(
    val version: Int,
    val exportedAt: Instant,
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>,
    val settings: TrackSettings,
    val profile: UserProfile,
    val externalScores: List<ExternalScore>,
    /** Empty for versions 3, 4, and 5, which predate the safety plan (Phase 13, D-9). */
    val safetyPlan: List<SafetyPlanItem> = emptyList(),
    /** Empty for versions 3 through 6, which predate paced breathing (Phase 14, D-9). */
    val breathingSessions: List<BreathingSession> = emptyList()
)

data class RecordsPayload(
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>,
    val breathingSessions: List<BreathingSession> = emptyList()
)

/** The record tables an additive import must be checked against. */
data class RecordSnapshot(
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>,
    val breathingSessions: List<BreathingSession> = emptyList()
)

data class ImportCounts(
    val entries: Int,
    val sleeps: Int,
    val markers: Int,
    val externalScores: Int,
    /** Only a restore ever writes these; a records CSV import always reports zero. */
    val safetyPlanItems: Int = 0,
    /** Written by both paths: a restore replaces them, a records CSV appends them. */
    val breathingSessions: Int = 0
)

/**
 * Thrown inside the import transaction when the stored records no longer match what the
 * confirmed preview described. It rolls the transaction back and is reported distinctly,
 * so the user is told their records changed rather than that the file was bad.
 */
class ImportConflictException : Exception(null, null, false, false)
