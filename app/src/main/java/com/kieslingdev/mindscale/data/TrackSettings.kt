package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_settings")
data class TrackSettings(
    @PrimaryKey val id: Int = 0,
    val sleepOn: Boolean = true,
    val askChips: Boolean = false,
    val paused: Boolean = false,
    val checkinAt: Long = 0L,
    val sleepIntroShown: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hourFormat: HourFormat = HourFormat.TWELVE,
    val anchor2: String = "",
    val anchor5: String = "",
    val anchor8: String = "",
    val onsetChips: List<String> = DEFAULT_ONSET_CHIPS,
    val hideNotes: Boolean = false,
    val anchorPromptDone: Boolean = false,
    val holdDuration: HoldDuration = HoldDuration.SIXTEEN
)
