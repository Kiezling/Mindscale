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
    val sleepIntroShown: Boolean = false
)
