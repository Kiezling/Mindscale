package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [EntryDao] test double so `TrackViewModel` unit tests can run
 * under plain `test` (JVM) without a real/instrumented Room database.
 */
class FakeEntryDao : EntryDao {

    private val entriesFlow = MutableStateFlow<List<Entry>>(emptyList())
    private var nextId = 1L

    val insertCalls = mutableListOf<Entry>()
    val updateCalls = mutableListOf<Entry>()
    val deleteCalls = mutableListOf<Entry>()
    val updateChipsCalls = mutableListOf<Pair<Long, List<String>>>()

    override suspend fun insert(entry: Entry): Long {
        val id = nextId++
        val stored = entry.copy(id = id)
        insertCalls += stored
        entriesFlow.value = entriesFlow.value + stored
        return id
    }

    override suspend fun update(entry: Entry) {
        updateCalls += entry
        entriesFlow.value = entriesFlow.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun delete(entry: Entry) {
        deleteCalls += entry
        entriesFlow.value = entriesFlow.value.filterNot { it.id == entry.id }
    }

    override fun observeRecent(limit: Int): Flow<List<Entry>> =
        entriesFlow.map { list ->
            list.sortedWith(compareByDescending<Entry> { it.ts }.thenByDescending { it.id })
                .take(limit)
        }

    override fun observeCount(): Flow<Int> = entriesFlow.map { it.size }

    override suspend fun mostRecentAtOrBefore(ts: Long): Entry? =
        entriesFlow.value
            .filter { it.ts <= ts }
            .maxWithOrNull(compareBy<Entry> { it.ts }.thenBy { it.id })

    override suspend fun updateChips(entryId: Long, chips: List<String>) {
        updateChipsCalls += entryId to chips
        entriesFlow.value = entriesFlow.value.map { if (it.id == entryId) it.copy(chips = chips) else it }
    }
}
