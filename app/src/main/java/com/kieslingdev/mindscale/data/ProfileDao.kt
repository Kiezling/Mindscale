package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ProfileStats(
    val firstRecordedTs: Long?,
    val ratingCount: Int,
    val sleepCount: Int,
    val markerCount: Int
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun observeProfile(): Flow<UserProfile>

    @Query("SELECT * FROM external_scores ORDER BY assessedEpochDay DESC, id DESC")
    fun observeScores(): Flow<List<ExternalScore>>

    @Query(
        """SELECT
            (SELECT MIN(recordedTs) FROM (
                SELECT ts AS recordedTs FROM entries
                UNION ALL SELECT startTs AS recordedTs FROM sleeps
                UNION ALL SELECT ts AS recordedTs FROM markers
            )) AS firstRecordedTs,
            (SELECT COUNT(*) FROM entries) AS ratingCount,
            (SELECT COUNT(*) FROM sleeps) AS sleepCount,
            (SELECT COUNT(*) FROM markers) AS markerCount"""
    )
    fun observeStats(): Flow<ProfileStats>

    @Query("UPDATE user_profile SET displayName = :displayName WHERE id = 0")
    suspend fun setDisplayName(displayName: String): Int

    @Query(
        "UPDATE user_profile SET displayName = :displayName " +
            "WHERE id = 0 AND displayName = :expectedDisplayName"
    )
    suspend fun setDisplayNameIfUnchanged(displayName: String, expectedDisplayName: String): Int

    @Query("SELECT * FROM external_scores WHERE id = :id")
    suspend fun scoreById(id: Long): ExternalScore?

    @Query(
        "SELECT * FROM external_scores WHERE instrument = :instrument AND assessedEpochDay = :assessedEpochDay LIMIT 1"
    )
    suspend fun scoreOnDate(
        instrument: ExternalInstrument,
        assessedEpochDay: Long
    ): ExternalScore?

    @Insert
    suspend fun insertScore(score: ExternalScore): Long

    @Update
    suspend fun updateScore(score: ExternalScore): Int

    @Query("DELETE FROM external_scores WHERE id = :id")
    suspend fun deleteScore(id: Long): Int
}
