package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.SleepDao
import com.kieslingdev.mindscale.data.SleepInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [SleepDao] test double. `captureSleep`/`captureWake` are NOT overridden -
 * they are inherited default method bodies from the `SleepDao` interface itself, so this
 * fake automatically exercises the exact same "read openInterval(), then write" logic
 * production code runs; only [insert]/[update]/[openInterval] need fake storage. Real
 * transactional/concurrency behavior is verified separately against a real Room database
 * in the instrumented `SleepDaoTest`.
 */
class FakeSleepDao : SleepDao {

    private val intervals = mutableListOf<SleepInterval>()
    private val intervalsFlow = MutableStateFlow<List<SleepInterval>>(emptyList())
    private var nextId = 1L

    val insertCalls = mutableListOf<SleepInterval>()
    val updateCalls = mutableListOf<SleepInterval>()
    val deleteByIdCalls = mutableListOf<Long>()
    var deleteByIdError: Throwable? = null

    override suspend fun insert(interval: SleepInterval): Long {
        val id = nextId++
        val stored = interval.copy(id = id)
        insertCalls += stored
        intervals += stored
        intervalsFlow.value = intervals.toList()
        return id
    }

    override suspend fun update(interval: SleepInterval) {
        updateCalls += interval
        val index = intervals.indexOfFirst { it.id == interval.id }
        if (index >= 0) intervals[index] = interval
        intervalsFlow.value = intervals.toList()
    }

    override suspend fun openInterval(): SleepInterval? =
        intervals.filter { it.endTs == null }.maxByOrNull { it.startTs }

    override fun observeBetween(fromTs: Long?, toTsExclusive: Long?): Flow<List<SleepInterval>> =
        intervalsFlow.map { list ->
            list.filter {
                (fromTs == null || it.startTs >= fromTs) &&
                    (toTsExclusive == null || it.startTs < toTsExclusive)
            }.sortedWith(compareByDescending<SleepInterval> { it.startTs }.thenByDescending { it.id })
        }

    override fun observeCount(): Flow<Int> = intervalsFlow.map { it.size }

    override suspend fun deleteById(id: Long): Int {
        deleteByIdCalls += id
        deleteByIdError?.let { throw it }
        val removed = intervals.removeAll { it.id == id }
        if (!removed) return 0
        intervalsFlow.value = intervals.toList()
        return 1
    }
}
