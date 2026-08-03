package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface MarkerDao {
    @Insert
    suspend fun insert(marker: Marker): Long
}
