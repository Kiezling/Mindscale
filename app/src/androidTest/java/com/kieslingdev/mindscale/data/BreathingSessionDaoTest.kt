package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `docs/specs/SPEC-paced-breathing.md`, D-4, D-9, D-10.
 *
 * The DAO is deliberately tiny. The interesting property is what is absent: there is no
 * observing query, so nothing in the app can read a session back to decide what to show.
 */
@RunWith(AndroidJUnit4::class)
class BreathingSessionDaoTest {
    private lateinit var database: MindScaleDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
    }

    @After fun closeDatabase() = database.close()

    @Test
    fun sessionsRoundTripWithGeneratedIdsAndTheirExactInterval() = runBlocking {
        val dao = database.breathingSessionDao()
        val first = dao.insert(BreathingSession(startedAt = 1_000L, endedAt = 61_000L))
        val second = dao.insert(BreathingSession(startedAt = 100_000L, endedAt = 100_000L))

        assertTrue(first > 0L)
        assertTrue(second > first)
        assertEquals(2, dao.count())

        val stored = database.dataControlDao().allBreathingSessions()
        // Newest first, matching every other collection the data-control surface returns.
        assertEquals(listOf(100_000L, 1_000L), stored.map { it.startedAt })
        assertEquals(listOf(0L, 60_000L), stored.map { it.endedAt - it.startedAt })
    }

    /** Tapping a length and stopping at once is a real thing a person can do. */
    @Test
    fun aZeroLengthSessionIsStorable() = runBlocking {
        val dao = database.breathingSessionDao()
        dao.insert(BreathingSession(startedAt = 5_000L, endedAt = 5_000L))
        assertEquals(1, dao.count())
    }

    @Test
    fun theFreshDatabaseStartsWithNoSessionsAndPacedBreathingOn() = runBlocking {
        assertEquals(0, database.breathingSessionDao().count())
        assertTrue(database.trackSettingsDao().current().breathingOn)
    }

    @Test
    fun theBreathingSettingIsATargetedWriteAgainstTheCanonicalRow() = runBlocking {
        val settings = database.trackSettingsDao()
        assertEquals(1, settings.setBreathingOn(false))
        assertEquals(false, settings.current().breathingOn)
        // Nothing else on the row moved.
        assertEquals(TrackSettings().holdDuration, settings.current().holdDuration)
        assertEquals(TrackSettings().themeMode, settings.current().themeMode)

        assertEquals(1, settings.setBreathingOn(true))
        assertTrue(settings.current().breathingOn)
    }
}
