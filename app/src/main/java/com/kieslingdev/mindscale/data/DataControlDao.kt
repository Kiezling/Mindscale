package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

data class DataSnapshot(
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>,
    val settings: TrackSettings
)

data class EraseCounts(val entries: Int, val sleeps: Int, val markers: Int)

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

    @Query("DELETE FROM entries")
    suspend fun deleteEntries(): Int

    @Query("DELETE FROM sleeps")
    suspend fun deleteSleeps(): Int

    @Query("DELETE FROM markers")
    suspend fun deleteMarkers(): Int

    @Update
    suspend fun resetSettings(defaults: TrackSettings): Int

    @Transaction
    suspend fun snapshot(): DataSnapshot =
        DataSnapshot(allEntries(), allSleeps(), allMarkers(), settings())

    @Transaction
    suspend fun eraseEverythingAndResetSettings(): EraseCounts {
        val counts = EraseCounts(deleteEntries(), deleteSleeps(), deleteMarkers())
        check(resetSettings(TrackSettings()) == 1) { "Canonical settings row is missing" }
        return counts
    }
}
