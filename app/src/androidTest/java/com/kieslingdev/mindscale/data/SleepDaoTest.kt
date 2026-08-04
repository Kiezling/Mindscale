package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepDaoTest {

    private lateinit var database: MindScaleDatabase
    private lateinit var dao: SleepDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, MindScaleDatabase::class.java)
            .addCallback(MindScaleDatabase.seedSettingsCallback)
            .build()
        dao = database.sleepDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun openInterval_returnsNullOnEmptyTable_andMostRecentlyStartedOpenRowOtherwise() = runBlocking {
        assertNull(dao.openInterval())

        dao.insert(SleepInterval(startTs = 1_000L, endTs = 2_000L)) // closed, must be ignored
        val openId = dao.insert(SleepInterval(startTs = 3_000L, endTs = null))

        val open = dao.openInterval()
        assertEquals(openId, open!!.id)
        assertEquals(3_000L, open.startTs)
        assertNull(open.endTs)
    }

    @Test
    fun captureSleep_opensNewInterval_whenNoneOpen() = runBlocking {
        val outcome = dao.captureSleep(atTs = 5_000L)

        assertTrue(outcome is SleepCaptureOutcome.Opened)
        val open = dao.openInterval()
        assertEquals(5_000L, open!!.startTs)
        assertNull(open.endTs)
    }

    @Test
    fun captureSleep_returnsAlreadyOpen_andLeavesExistingIntervalUntouched_whenOneIsOpen() = runBlocking {
        dao.captureSleep(atTs = 1_000L)

        val outcome = dao.captureSleep(atTs = 2_000L)

        assertTrue(outcome is SleepCaptureOutcome.AlreadyOpen)
        assertEquals(1_000L, (outcome as SleepCaptureOutcome.AlreadyOpen).since)
        // No second open interval was created.
        assertEquals(1_000L, dao.openInterval()!!.startTs)
    }

    @Test
    fun captureWake_closesOpenInterval_atTheRecordedTs_whenAboveTheMinimumDuration() = runBlocking {
        dao.captureSleep(atTs = 1_000L)

        // A 2-hour gap is well above the 1-minute floor, so `until` should be the
        // recorded wake ts itself, not the minimum-duration clamp.
        val twoHoursLaterTs = 1_000L + (2 * 60 * 60 * 1000L)
        val outcome = dao.captureWake(atTs = twoHoursLaterTs)

        assertTrue(outcome is SleepCaptureOutcome.Closed)
        val closed = outcome as SleepCaptureOutcome.Closed
        assertEquals(1_000L, closed.since)
        assertEquals(twoHoursLaterTs, closed.until)
        assertNull(dao.openInterval())
    }

    @Test
    fun captureWake_enforcesMinimumOneMinuteDuration_onFastSleepThenWake() = runBlocking {
        dao.captureSleep(atTs = 1_000L)

        // Wake fires only 5ms after Sleep — must still resolve to a 60_000ms interval.
        val outcome = dao.captureWake(atTs = 1_005L) as SleepCaptureOutcome.Closed

        assertEquals(1_000L + 60_000L, outcome.until)
    }

    @Test
    fun captureWake_returnsNothingOpen_andWritesNoRow_whenNoneIsOpen() = runBlocking {
        val outcome = dao.captureWake(atTs = 1_000L)

        assertTrue(outcome is SleepCaptureOutcome.NothingOpen)
        assertNull(dao.openInterval())
    }

    /**
     * Concurrency: "at most one open interval" must hold under concurrent/rapid-repeated
     * calls, not just sequential ones (Invariant 19). This exercises Room's real
     * transaction serialization, not a fake/in-memory Kotlin double, since the guarantee
     * comes from SQLite/Room, not from application-level locking. If captureSleep's
     * "read openInterval(), then write" sequence were not atomic, a race would surface
     * here as more than one `Opened` result (two racers both reading "nothing open"
     * before either commits its insert) — the outcome tally is the assertion, not an
     * incidental detail.
     */
    @Test
    fun concurrentCaptureSleep_resultsInExactlyOneOpenInterval() = runBlocking {
        val callCount = 20
        val outcomes = (1..callCount).map {
            async(Dispatchers.IO) { dao.captureSleep(atTs = 42_000L) }
        }.awaitAll()

        val openedCount = outcomes.count { it is SleepCaptureOutcome.Opened }
        val alreadyOpenCount = outcomes.count { it is SleepCaptureOutcome.AlreadyOpen }
        assertEquals(1, openedCount)
        assertEquals(callCount - 1, alreadyOpenCount)
        assertNotNull(dao.openInterval())
        assertEquals(42_000L, dao.openInterval()!!.startTs)
    }

    /**
     * Same guarantee for captureWake: a non-atomic implementation would let more than
     * one racer observe the same open interval and each report `Closed`.
     */
    @Test
    fun concurrentCaptureWake_closesTheSingleOpenIntervalExactlyOnce() = runBlocking {
        dao.captureSleep(atTs = 1_000L)

        val callCount = 20
        val outcomes = (1..callCount).map { i ->
            async(Dispatchers.IO) { dao.captureWake(atTs = 1_000L + 60_000L + i) }
        }.awaitAll()

        val closedCount = outcomes.count { it is SleepCaptureOutcome.Closed }
        val nothingOpenCount = outcomes.count { it is SleepCaptureOutcome.NothingOpen }
        assertEquals(1, closedCount)
        assertEquals(callCount - 1, nothingOpenCount)
        assertNull(dao.openInterval())
    }

    @Test
    fun observeBetween_countAndDelete_useStartTsAndHalfOpenBounds() = runBlocking {
        val excludedLow = dao.insert(SleepInterval(startTs = 999L, endTs = 1_500L))
        val firstTie = dao.insert(SleepInterval(startTs = 1_000L, endTs = 2_000L))
        val secondTie = dao.insert(SleepInterval(startTs = 1_000L, endTs = 2_500L))
        val excludedHigh = dao.insert(SleepInterval(startTs = 2_000L, endTs = 3_000L))

        assertEquals(listOf(secondTie, firstTie), dao.observeBetween(1_000L, 2_000L).first().map { it.id })
        assertEquals(listOf(excludedHigh, secondTie, firstTie), dao.observeBetween(1_000L, null).first().map { it.id })
        assertEquals(setOf(excludedLow, firstTie, secondTie), dao.observeBetween(null, 2_000L).first().map { it.id }.toSet())
        assertEquals(4, dao.observeCount().first())
        assertEquals(1, dao.deleteById(firstTie))
        assertEquals(0, dao.deleteById(firstTie))
        assertEquals(setOf(excludedLow, secondTie, excludedHigh), dao.observeBetween(null, null).first().map { it.id }.toSet())
    }
}
