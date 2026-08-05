package com.kieslingdev.mindscale.breathing

import com.kieslingdev.mindscale.data.BreathingSession
import com.kieslingdev.mindscale.data.BreathingSessionDao

class FakeBreathingSessionDao : BreathingSessionDao {
    val inserted = mutableListOf<BreathingSession>()
    var failInsert = false
    private var nextId = 0L

    override suspend fun insert(session: BreathingSession): Long {
        if (failInsert) error("insert failed")
        inserted += session.copy(id = ++nextId)
        return nextId
    }

    override suspend fun count(): Int = inserted.size
}

/**
 * A clock whose monotonic reading is driven by the coroutine test scheduler, so `delay`
 * inside the pacer loop and "how long has this session run" stay in step under virtual
 * time. [wallMillis] is separate and can be moved independently, which is how the
 * clock-jump test works.
 */
class FakeBreathingClock(
    private val monotonic: () -> Long,
    var wall: Long = 1_700_000_000_000L
) : BreathingClock {
    /** Set to force a value for the *next* monotonic read only; used to test clamping. */
    var monotonicOverride: Long? = null

    override fun wallMillis(): Long = wall

    override fun monotonicMillis(): Long = monotonicOverride?.also { monotonicOverride = null }
        ?: monotonic()
}
