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
    val updateEditableFieldsCalls = mutableListOf<Long>()
    val updateNoteCalls = mutableListOf<Long>()
    val deleteByIdCalls = mutableListOf<Long>()
    var updateEditableFieldsError: Throwable? = null
    var updateNoteError: Throwable? = null
    var deleteByIdError: Throwable? = null
    var observeByIdError: Throwable? = null
    var insertError: Throwable? = null
    var updateEditableFieldsResult: Int? = null
    var updateNoteResult: Int? = null
    var deleteByIdResult: Int? = null

    override suspend fun insert(entry: Entry): Long {
        insertError?.let { throw it }
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

    override fun observeById(id: Long): Flow<Entry?> = entriesFlow.map { entries ->
        observeByIdError?.let { throw it }
        entries.firstOrNull { it.id == id }
    }

    override fun observeBetween(fromTs: Long?, toTsExclusive: Long?): Flow<List<Entry>> =
        entriesFlow.map { list ->
            list.filter { (fromTs == null || it.ts >= fromTs) && (toTsExclusive == null || it.ts < toTsExclusive) }
                .sortedWith(compareByDescending<Entry> { it.ts }.thenByDescending { it.id })
        }

    override suspend fun mostRecentAtOrBefore(ts: Long): Entry? =
        entriesFlow.value
            .filter { it.ts <= ts }
            .maxWithOrNull(compareBy<Entry> { it.ts }.thenBy { it.id })

    override suspend fun updateChips(entryId: Long, chips: List<String>) {
        updateChipsCalls += entryId to chips
        entriesFlow.value = entriesFlow.value.map { if (it.id == entryId) it.copy(chips = chips) else it }
    }

    override suspend fun updateEditableFields(
        id: Long,
        ts: Long,
        value: Int,
        chips: List<String>
    ): Int {
        updateEditableFieldsCalls += id
        updateEditableFieldsError?.let { throw it }
        updateEditableFieldsResult?.let { return it }
        val original = entriesFlow.value.firstOrNull { it.id == id } ?: return 0
        val updated = original.copy(ts = ts, value = value, chips = chips)
        updateCalls += updated
        entriesFlow.value = entriesFlow.value.map { if (it.id == id) updated else it }
        return 1
    }

    override suspend fun updateNote(id: Long, note: String?): Int {
        updateNoteCalls += id
        updateNoteError?.let { throw it }
        updateNoteResult?.let { return it }
        val original = entriesFlow.value.firstOrNull { it.id == id } ?: return 0
        val updated = original.copy(note = note)
        updateCalls += updated
        entriesFlow.value = entriesFlow.value.map { if (it.id == id) updated else it }
        return 1
    }

    override suspend fun deleteById(id: Long): Int {
        deleteByIdCalls += id
        deleteByIdError?.let { throw it }
        deleteByIdResult?.let { return it }
        val original = entriesFlow.value.firstOrNull { it.id == id } ?: return 0
        deleteCalls += original
        entriesFlow.value = entriesFlow.value.filterNot { it.id == id }
        return 1
    }
}
