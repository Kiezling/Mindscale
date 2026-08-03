package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "markers")
data class Marker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ts: Long,
    val text: String
)
