package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    // Low-level CRUD — used directly by DAO-level tests; TrackViewModel never calls
    // these for the sleep/wake capture path (it calls captureSleep/captureWake below).
    @Insert
    suspend fun insert(interval: SleepInterval): Long

    @Update
    suspend fun update(interval: SleepInterval)

    @Query("SELECT * FROM sleeps WHERE endTs IS NULL ORDER BY startTs DESC LIMIT 1")
    suspend fun openInterval(): SleepInterval?

    @Query(
        """SELECT * FROM sleeps
           WHERE (:fromTs IS NULL OR startTs >= :fromTs)
             AND (:toTsExclusive IS NULL OR startTs < :toTsExclusive)
           ORDER BY startTs DESC, id DESC"""
    )
    fun observeBetween(fromTs: Long?, toTsExclusive: Long?): Flow<List<SleepInterval>>

    @Query("SELECT COUNT(*) FROM sleeps")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM sleeps WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Atomic capture operations — the sole writers `TrackViewModel` calls for Sleep/Wake
     * taps. `@Transaction` wraps each method's "read [openInterval], then write" sequence
     * in a single SQLite transaction, so Room/SQLite serializes concurrent callers: the
     * second call to actually execute always observes the first call's already-applied
     * effect. "At most one open interval" (Invariant 19 of
     * SPEC-track-phase2-completeness.md) is guaranteed here, not by any caller-side check.
     */
    @Transaction
    suspend fun captureSleep(atTs: Long): SleepCaptureOutcome {
        val open = openInterval()
        if (open != null) return SleepCaptureOutcome.AlreadyOpen(open.startTs)
        insert(SleepInterval(startTs = atTs, endTs = null))
        return SleepCaptureOutcome.Opened
    }

    @Transaction
    suspend fun captureWake(atTs: Long): SleepCaptureOutcome {
        val open = openInterval() ?: return SleepCaptureOutcome.NothingOpen
        val resolvedEnd = maxOf(atTs, open.startTs + 60_000L)
        update(open.copy(endTs = resolvedEnd))
        return SleepCaptureOutcome.Closed(open.startTs, resolvedEnd)
    }
}
