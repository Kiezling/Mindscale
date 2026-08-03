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

    /**
     * The only query onset detection may use (Invariant 15 / D-3 of
     * SPEC-track-phase2-completeness.md). Deliberately scoped to `ts <= :ts` so a
     * backdated capture is judged only against what preceded it at that point in the
     * timeline, never against entries that exist with a later `ts`. Ties break by
     * `id DESC`, matching [observeRecent]'s ordering convention.
     */
    @Query("SELECT * FROM entries WHERE ts <= :ts ORDER BY ts DESC, id DESC LIMIT 1")
    suspend fun mostRecentAtOrBefore(ts: Long): Entry?

    /**
     * Targeted chips-only patch, used by the onset-chip Submit flow. `OnsetChipPromptState`
     * is frozen to `entryId: Long` only (no full Entry snapshot) - a full-row `update()`
     * from any entry copy captured earlier would risk silently reverting an Edit/Note
     * made to that same entry in the meantime, since the Recent list and its Edit/Note
     * actions remain interactive while the onset-chip card is open. This avoids that
     * class of bug entirely by never touching any column but `chips`.
     */
    @Query("UPDATE entries SET chips = :chips WHERE id = :entryId")
    suspend fun updateChips(entryId: Long, chips: List<String>)
}
