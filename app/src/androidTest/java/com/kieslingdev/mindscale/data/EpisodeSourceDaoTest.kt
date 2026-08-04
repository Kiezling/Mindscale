package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodeSourceDaoTest {
    private lateinit var database: MindScaleDatabase
    private lateinit var dao: EpisodeSourceDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        dao = database.episodeSourceDao()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun observeSource_projectsEntriesAndSleepsInDeterministicOrder_andReactsToMutations() = runBlocking {
        val entryId = database.entryDao().insert(Entry(ts = 200, value = 4, chips = listOf("work")))
        val sleepId = database.sleepDao().insert(SleepInterval(startTs = 100, endTs = 300))

        var rows = dao.observeSource().first()
        assertEquals(listOf("SLEEP", "ENTRY"), rows.map(EpisodeSourceRow::recordType))
        assertEquals(listOf(100L, 200L), rows.map(EpisodeSourceRow::ts))
        assertEquals(300L, rows[0].endTs)
        assertEquals(listOf("work"), rows[1].chips)

        database.entryDao().updateEditableFields(entryId, 50, 7, listOf("meal"))
        rows = dao.observeSource().first()
        assertEquals(listOf("ENTRY", "SLEEP"), rows.map(EpisodeSourceRow::recordType))
        assertEquals(7, rows[0].value)

        database.entryDao().deleteById(entryId)
        database.sleepDao().deleteById(sleepId)
        assertTrue(dao.observeSource().first().isEmpty())
    }

    @Test
    fun insertOrdinaryAndClassify_usesHoldExplicitZeroAndCaptureTimestamp() = runBlocking {
        database.trackSettingsDao().setAskChips(true)
        database.trackSettingsDao().setHoldDuration(HoldDuration.EIGHT)
        val hour = 3_600_000L

        val first = dao.insertOrdinaryAndClassify(Entry(ts = 0, value = 5))
        val continuing = dao.insertOrdinaryAndClassify(Entry(ts = hour, value = 6))
        val afterAssumedGap = dao.insertOrdinaryAndClassify(Entry(ts = 10 * hour, value = 4))
        dao.insertOrdinaryAndClassify(Entry(ts = 11 * hour, value = 0))
        val afterExplicitZero = dao.insertOrdinaryAndClassify(Entry(ts = 12 * hour, value = 3))

        assertTrue(first.isOnset)
        assertTrue(first.promptEnabled)
        assertFalse(continuing.isOnset)
        assertTrue(afterAssumedGap.isOnset)
        assertTrue(afterExplicitZero.isOnset)
        assertTrue(listOf(first, continuing, afterAssumedGap, afterExplicitZero).all { it.settingsAvailable })
        assertTrue(listOf(first, continuing, afterAssumedGap, afterExplicitZero).all { it.classificationAvailable })
        assertEquals(5, database.entryDao().observeCount().first())
    }

    @Test
    fun insertOrdinaryAndClassify_excludesLaterFacts_andUsesHigherIdForTimestampTies() = runBlocking {
        val hour = 3_600_000L
        database.trackSettingsDao().setHoldDuration(HoldDuration.EIGHT)
        database.entryDao().insert(Entry(ts = 20 * hour, value = 8))
        database.entryDao().insert(Entry(ts = 10 * hour, value = 5))
        database.entryDao().insert(Entry(ts = 10 * hour, value = 0))

        val tiedAfterZero = dao.insertOrdinaryAndClassify(Entry(ts = 10 * hour, value = 4))
        val backdatedBeforeFuture = dao.insertOrdinaryAndClassify(Entry(ts = 19 * hour, value = 3))

        assertTrue(tiedAfterZero.isOnset)
        assertTrue(backdatedBeforeFuture.isOnset)
        assertEquals(5, database.entryDao().observeCount().first())
    }

    @Test
    fun missingSettings_stillInsertsRating_butSuppressesPrompt() = runBlocking {
        database.openHelper.writableDatabase.execSQL("DELETE FROM track_settings WHERE id = 0")

        val result = dao.insertOrdinaryAndClassify(Entry(ts = 100, value = 7))

        assertTrue(result.isOnset)
        assertFalse(result.settingsAvailable)
        assertFalse(result.promptEnabled)
        assertTrue(result.classificationAvailable)
        assertEquals(1, database.entryDao().observeCount().first())
    }
}
