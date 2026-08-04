package com.kieslingdev.mindscale.data

import androidx.room.Dao
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
