package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

data class DataSnapshot(
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>,
    val settings: TrackSettings,
    val profile: UserProfile = UserProfile(),
    val externalScores: List<ExternalScore> = emptyList()
)

data class EraseCounts(
    val entries: Int,
    val sleeps: Int,
    val markers: Int,
    val externalScores: Int = 0
)

@Dao
interface DataControlDao {
    @Query("SELECT * FROM entries ORDER BY ts DESC, id DESC")
    suspend fun allEntries(): List<Entry>

    @Query("SELECT * FROM sleeps ORDER BY startTs DESC, id DESC")
    suspend fun allSleeps(): List<SleepInterval>

    @Query("SELECT * FROM markers ORDER BY ts DESC, id DESC")
    suspend fun allMarkers(): List<Marker>

    @Query("SELECT * FROM track_settings WHERE id = 0")
    suspend fun settings(): TrackSettings

    @Query("SELECT * FROM user_profile WHERE id = 0")
    suspend fun profile(): UserProfile

    @Query("SELECT * FROM external_scores ORDER BY assessedEpochDay DESC, id DESC")
    suspend fun allExternalScores(): List<ExternalScore>

    @Query("DELETE FROM entries")
    suspend fun deleteEntries(): Int

    @Query("DELETE FROM sleeps")
    suspend fun deleteSleeps(): Int

    @Query("DELETE FROM markers")
    suspend fun deleteMarkers(): Int

    @Query("DELETE FROM external_scores")
    suspend fun deleteExternalScores(): Int

    @Update
    suspend fun resetSettings(defaults: TrackSettings): Int

    @Update
    suspend fun resetProfile(defaults: UserProfile): Int

    @Transaction
    suspend fun snapshot(): DataSnapshot =
        DataSnapshot(
            allEntries(),
            allSleeps(),
            allMarkers(),
            settings(),
            profile(),
            allExternalScores()
        )

    @Insert
    suspend fun insertEntries(entries: List<Entry>): List<Long>

    @Insert
    suspend fun insertSleeps(sleeps: List<SleepInterval>): List<Long>

    @Insert
    suspend fun insertMarkers(markers: List<Marker>): List<Long>

    @Insert
    suspend fun insertExternalScores(scores: List<ExternalScore>): List<Long>

    @Query("SELECT COUNT(*) FROM sleeps WHERE endTs IS NULL")
    suspend fun openSleepCount(): Int

    @Query("SELECT COUNT(*) FROM track_settings WHERE id = 0")
    suspend fun settingsRowCount(): Int

    @Query("SELECT COUNT(*) FROM user_profile WHERE id = 0")
    suspend fun profileRowCount(): Int

    /** Only the record tables an additive import is checked against (Phase 12, D-4). */
    @Transaction
    suspend fun recordSnapshot(): RecordSnapshot =
        RecordSnapshot(allEntries(), allSleeps(), allMarkers())

    /**
     * Replaces every record, the canonical settings row, the canonical Profile row, and
     * all externally obtained totals with an already-validated backup.
     *
     * Ids come from the file verbatim. That is safe only because the destination tables
     * are emptied inside this same transaction, and it is what keeps external-score audit
     * identity and byte-reproducible re-export intact. Every check below throws, and a
     * throw inside a Room transaction rolls the whole thing back, so the caller either
     * gets the complete import or a database untouched by it.
     *
     * See `docs/specs/SPEC-import-restore.md`, D-1, D-3, and D-10.
     */
    @Transaction
    suspend fun replaceEverything(payload: BackupPayload): ImportCounts {
        deleteEntries()
        deleteSleeps()
        deleteMarkers()
        deleteExternalScores()
        check(resetSettings(payload.settings.copy(id = 0)) == 1) {
            "Canonical settings row is missing"
        }
        check(resetProfile(payload.profile.copy(id = 0)) == 1) {
            "Canonical profile row is missing"
        }
        val counts = insertRecords(
            payload.entries, payload.sleeps, payload.markers, payload.externalScores
        )
        verifyInvariants(counts, payload.entries.size, payload.sleeps.size, payload.markers.size)
        check(counts.externalScores == payload.externalScores.size) { "External total count changed" }
        return counts
    }

    /**
     * Appends already-validated records and touches nothing else. The duplicate, conflict,
     * and sleep-structure rules are re-checked here against the current tables so records
     * that changed while the preview was open cannot slip past preflight (D-4, D-10).
     */
    @Transaction
    suspend fun addRecords(
        payload: RecordsPayload,
        recheck: (RecordSnapshot) -> Boolean
    ): ImportCounts {
        if (!recheck(recordSnapshot())) throw ImportConflictException()
        val counts = insertRecords(payload.entries, payload.sleeps, payload.markers, emptyList())
        verifyInvariants(counts, payload.entries.size, payload.sleeps.size, payload.markers.size)
        return counts
    }

    private suspend fun insertRecords(
        entries: List<Entry>,
        sleeps: List<SleepInterval>,
        markers: List<Marker>,
        scores: List<ExternalScore>
    ): ImportCounts {
        val entryIds = insertEntries(entries)
        val sleepIds = insertSleeps(sleeps)
        val markerIds = insertMarkers(markers)
        val scoreIds = insertExternalScores(scores)
        val inserted = entryIds + sleepIds + markerIds + scoreIds
        check(inserted.all { it > 0L }) { "An imported row was not inserted" }
        return ImportCounts(entryIds.size, sleepIds.size, markerIds.size, scoreIds.size)
    }

    private suspend fun verifyInvariants(counts: ImportCounts, entries: Int, sleeps: Int, markers: Int) {
        check(counts.entries == entries && counts.sleeps == sleeps && counts.markers == markers) {
            "Imported row count does not match the approved count"
        }
        check(settingsRowCount() == 1) { "Canonical settings row is missing" }
        check(profileRowCount() == 1) { "Canonical profile row is missing" }
        check(openSleepCount() <= 1) { "More than one sleep interval is open" }
    }

    @Transaction
    suspend fun eraseEverythingAndResetSettings(): EraseCounts {
        val counts = EraseCounts(
            deleteEntries(),
            deleteSleeps(),
            deleteMarkers(),
            deleteExternalScores()
        )
        check(resetSettings(TrackSettings()) == 1) { "Canonical settings row is missing" }
        check(resetProfile(UserProfile()) == 1) { "Canonical profile row is missing" }
        return counts
    }
}
