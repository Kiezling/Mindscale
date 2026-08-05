package com.kieslingdev.mindscale.safety

import com.kieslingdev.mindscale.data.SafetyPlanDao
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.SafetyPlanStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-in for the plan table. Only the abstract members are implemented, so the
 * `@Transaction` default bodies — including the position renumbering that keeps a step's
 * positions exactly `0 until size` — run here exactly as they run in production.
 */
class FakeSafetyPlanDao : SafetyPlanDao {
    val rows = MutableStateFlow<List<SafetyPlanItem>>(emptyList())
    var failWrites = false
    var failReads = false
    private var nextId = 0L

    private fun ordered(list: List<SafetyPlanItem>) =
        list.sortedWith(compareBy({ it.position }, { it.id }))

    override fun observeAll(): Flow<List<SafetyPlanItem>> = rows.map { list ->
        if (failReads) error("read failed")
        ordered(list)
    }

    override suspend fun itemsIn(step: SafetyPlanStep): List<SafetyPlanItem> =
        ordered(rows.value.filter { it.step == step })

    override suspend fun itemById(id: Long): SafetyPlanItem? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun count(): Int = rows.value.size

    override suspend fun insert(item: SafetyPlanItem): Long {
        if (failWrites) error("insert failed")
        val id = if (item.id == 0L) ++nextId else item.id
        rows.value = rows.value + item.copy(id = id)
        return id
    }

    override suspend fun updateContent(id: Long, text: String, phone: String?): Int {
        if (failWrites) error("update failed")
        val existing = rows.value.firstOrNull { it.id == id } ?: return 0
        rows.value = rows.value.map {
            if (it.id == id) existing.copy(text = text, phone = phone) else it
        }
        return 1
    }

    override suspend fun setPosition(id: Long, position: Int): Int {
        val existing = rows.value.firstOrNull { it.id == id } ?: return 0
        rows.value = rows.value.map { if (it.id == id) existing.copy(position = position) else it }
        return 1
    }

    override suspend fun deleteById(id: Long): Int {
        if (failWrites) error("delete failed")
        val before = rows.value.size
        rows.value = rows.value.filterNot { it.id == id }
        return before - rows.value.size
    }
}
