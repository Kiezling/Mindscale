package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackSettingsDao {
    // Reads/writes exclusively target the canonical row, id = 0. Room's @PrimaryKey only
    // enforces uniqueness of whatever id a row has; it does not stop something from
    // inserting a row with a different id. Nothing in this app ever does — the row is
    // seeded once (MindScaleDatabase's creation callback for a fresh install, or
    // Migration(1, 2)'s seed INSERT for an upgrade) and update() always writes back a
    // TrackSettings whose id is still 0 (copy() never touches id). If a stray row with
    // another id ever existed, this query's WHERE id = 0 clause would still return the
    // canonical row deterministically.
    @Query("SELECT * FROM track_settings WHERE id = 0")
    fun observe(): Flow<TrackSettings>

    @Update
    suspend fun update(settings: TrackSettings)
}
