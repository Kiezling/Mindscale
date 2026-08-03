package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.SleepDao
import com.kieslingdev.mindscale.data.SleepInterval

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
    private var nextId = 1L

    val insertCalls = mutableListOf<SleepInterval>()
    val updateCalls = mutableListOf<SleepInterval>()

    override suspend fun insert(interval: SleepInterval): Long {
        val id = nextId++
        val stored = interval.copy(id = id)
        insertCalls += stored
        intervals += stored
        return id
    }

    override suspend fun update(interval: SleepInterval) {
        updateCalls += interval
        val index = intervals.indexOfFirst { it.id == interval.id }
        if (index >= 0) intervals[index] = interval
    }

    override suspend fun openInterval(): SleepInterval? =
        intervals.filter { it.endTs == null }.maxByOrNull { it.startTs }
}
