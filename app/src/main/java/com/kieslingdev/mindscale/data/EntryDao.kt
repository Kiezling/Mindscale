package com.kieslingdev.mindscale.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("SELECT * FROM entries ORDER BY ts DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<Entry>>

    @Query("SELECT COUNT(*) FROM entries")
    fun observeCount(): Flow<Int>
}
