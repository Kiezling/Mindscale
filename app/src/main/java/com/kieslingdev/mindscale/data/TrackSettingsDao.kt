package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM track_settings WHERE id = 0")
    suspend fun current(): TrackSettings

    @Query("UPDATE track_settings SET sleepOn = :enabled WHERE id = 0")
    suspend fun setSleepOn(enabled: Boolean): Int

    @Query("SELECT EXISTS(SELECT 1 FROM sleeps WHERE endTs IS NULL)")
    suspend fun hasOpenSleepInterval(): Boolean

    @Transaction
    suspend fun setSleepOnSafely(enabled: Boolean): SleepSettingOutcome {
        if (!enabled && hasOpenSleepInterval()) return SleepSettingOutcome.OpenInterval
        return if (setSleepOn(enabled) == 1) SleepSettingOutcome.Updated else SleepSettingOutcome.MissingSettings
    }

    @Query("UPDATE track_settings SET askChips = :enabled WHERE id = 0")
    suspend fun setAskChips(enabled: Boolean): Int

    @Query("UPDATE track_settings SET paused = :paused WHERE id = 0")
    suspend fun setPaused(paused: Boolean): Int

    @Query("UPDATE track_settings SET checkinAt = :checkinAt, paused = :paused WHERE id = 0")
    suspend fun recordCheckin(checkinAt: Long, paused: Boolean): Int

    @Query("UPDATE track_settings SET sleepIntroShown = :shown WHERE id = 0")
    suspend fun setSleepIntroShown(shown: Boolean): Int

    @Query("UPDATE track_settings SET themeMode = :mode WHERE id = 0")
    suspend fun setAppearance(mode: ThemeMode): Int

    @Query("UPDATE track_settings SET hourFormat = :format WHERE id = 0")
    suspend fun setHourFormat(format: HourFormat): Int

    @Query("UPDATE track_settings SET anchor2 = :anchor2, anchor5 = :anchor5, anchor8 = :anchor8 WHERE id = 0")
    suspend fun setAnchors(anchor2: String, anchor5: String, anchor8: String): Int

    @Query("UPDATE track_settings SET onsetChips = :chips WHERE id = 0")
    suspend fun setOnsetChips(chips: List<String>): Int

    @Query("UPDATE track_settings SET hideNotes = :hidden WHERE id = 0")
    suspend fun setHideNotes(hidden: Boolean): Int

    @Query("UPDATE track_settings SET anchorPromptDone = :done WHERE id = 0")
    suspend fun setAnchorPromptDone(done: Boolean): Int

    @Query("UPDATE track_settings SET holdDuration = :duration WHERE id = 0")
    suspend fun setHoldDuration(duration: HoldDuration): Int

    @Update
    suspend fun update(settings: TrackSettings)
}

sealed interface SleepSettingOutcome {
    data object Updated : SleepSettingOutcome
    data object OpenInterval : SleepSettingOutcome
    data object MissingSettings : SleepSettingOutcome
}
