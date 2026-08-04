package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {
    @Insert
    suspend fun insert(marker: Marker): Long

    @Query(
        """SELECT * FROM markers
           WHERE (:fromTs IS NULL OR ts >= :fromTs)
             AND (:toTsExclusive IS NULL OR ts < :toTsExclusive)
           ORDER BY ts DESC, id DESC"""
    )
    fun observeBetween(fromTs: Long?, toTsExclusive: Long?): Flow<List<Marker>>

    @Query("SELECT COUNT(*) FROM markers")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
