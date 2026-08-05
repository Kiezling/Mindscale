package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * The only data source the Safety card has (`docs/specs/SPEC-safety-card.md`, D-11).
 * It exposes the user's own plan and nothing recorded, so no future edit can make the
 * card's content depend on a rating, episode, or count without changing this interface.
 *
 * Every mutation is targeted by id — the Phase 3 contract that replaced full-row writes —
 * and every mutation that can change a step's shape runs in one transaction so positions
 * are never observed non-contiguous.
 */
@Dao
interface SafetyPlanDao {
    @Query("SELECT * FROM safety_plan_items ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<SafetyPlanItem>>

    @Query("SELECT * FROM safety_plan_items WHERE step = :step ORDER BY position ASC, id ASC")
    suspend fun itemsIn(step: SafetyPlanStep): List<SafetyPlanItem>

    @Query("SELECT * FROM safety_plan_items WHERE id = :id")
    suspend fun itemById(id: Long): SafetyPlanItem?

    @Query("SELECT COUNT(*) FROM safety_plan_items")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: SafetyPlanItem): Long

    @Query("UPDATE safety_plan_items SET text = :text, phone = :phone WHERE id = :id")
    suspend fun updateContent(id: Long, text: String, phone: String?): Int

    @Query("UPDATE safety_plan_items SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int): Int

    @Query("DELETE FROM safety_plan_items WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Appends to the end of a step. The position is derived inside the transaction rather
     * than passed in, so two concurrent adds cannot land on the same position.
     */
    @Transaction
    suspend fun addItem(step: SafetyPlanStep, text: String, phone: String?): Long =
        insert(
            SafetyPlanItem(
                step = step,
                position = itemsIn(step).size,
                text = text,
                phone = phone
            )
        )

    /**
     * Deletes one line and closes the gap it left, so a step's positions stay exactly
     * `0 until size` (Invariant 5/6). Returns false when the row was already gone, which
     * is a stale-row outcome the caller reports rather than an error.
     */
    @Transaction
    suspend fun removeItem(id: Long): Boolean {
        val item = itemById(id) ?: return false
        if (deleteById(id) != 1) return false
        itemsIn(item.step).forEachIndexed { index, row ->
            if (row.position != index) setPosition(row.id, index)
        }
        return true
    }
}
