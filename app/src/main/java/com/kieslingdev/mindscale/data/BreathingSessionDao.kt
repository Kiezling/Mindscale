package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * The only data source the paced-breathing feature has
 * (`docs/specs/SPEC-paced-breathing.md`, D-10, Invariant 2).
 *
 * Write-only in practice: there is deliberately no observing query and no read used by any
 * screen, so a stored session cannot influence anything MindScale displays. That is what
 * makes "never offered to you unprompted" a property of the type rather than a promise —
 * the pacer has nothing to react to, including its own history.
 *
 * [count] exists for tests and for nothing else. Export, restore, and erase read sessions
 * through [DataControlDao], which is the aggregate the data-control surface already owns.
 */
@Dao
interface BreathingSessionDao {
    @Insert
    suspend fun insert(session: BreathingSession): Long

    @Query("SELECT COUNT(*) FROM breathing_sessions")
    suspend fun count(): Int
}
