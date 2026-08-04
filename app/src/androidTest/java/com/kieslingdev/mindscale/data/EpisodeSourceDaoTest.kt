package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kieslingdev.mindscale.insights.InsightRange
import com.kieslingdev.mindscale.insights.deriveInsights
import java.time.Instant
import java.time.ZoneOffset
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
    fun observeSource_projectsEntriesSleepsAndMarkersInDeterministicOrder_andReactsToMutations() = runBlocking {
        val entryId = database.entryDao().insert(
            Entry(ts = 200, value = 4, chips = listOf("work"), note = "private context")
        )
        val sleepId = database.sleepDao().insert(SleepInterval(startTs = 100, endTs = 300))
        val markerId = database.markerDao().insert(Marker(ts = 150, text = "dose change"))

        var rows = dao.observeSource().first()
        assertEquals(listOf("SLEEP", "MARKER", "ENTRY"), rows.map(EpisodeSourceRow::recordType))
        assertEquals(listOf(100L, 150L, 200L), rows.map(EpisodeSourceRow::ts))
        assertEquals(300L, rows[0].endTs)
        assertEquals("dose change", rows[1].text)
        assertEquals(listOf("work"), rows[2].chips)
        assertEquals("private context", rows[2].note)

        database.entryDao().updateEditableFields(entryId, 50, 7, listOf("meal"))
        rows = dao.observeSource().first()
        assertEquals(listOf("ENTRY", "SLEEP", "MARKER"), rows.map(EpisodeSourceRow::recordType))
        assertEquals(7, rows[0].value)

        database.entryDao().deleteById(entryId)
        database.sleepDao().deleteById(sleepId)
        database.markerDao().deleteById(markerId)
        assertTrue(dao.observeSource().first().isEmpty())
    }

    @Test
    fun insertOrdinaryAndClassify_usesHoldExplicitZeroAndCaptureTimestamp() = runBlocking {
        database.trackSettingsDao().setAskChips(true)
        database.trackSettingsDao().setHoldDuration(HoldDuration.EIGHT)
        database.markerDao().insert(Marker(ts = 500, text = "context only"))
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

    @Test
    fun onsetTimeCountsReuseRoomProjectionAndIgnoreMarkerMutation() = runBlocking {
        val hour = 3_600_000L
        repeat(6) { index ->
            database.entryDao().insert(Entry(ts = index * 2L * hour, value = 5))
            database.entryDao().insert(Entry(ts = index * 2L * hour + hour / 2, value = 0))
        }
        val before = deriveInsights(
            rows = dao.observeSource().first(),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(12 * hour),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        ).onsetTimeCounts

        val markerId = database.markerDao().insert(Marker(ts = hour, text = "private context"))
        val afterInsert = deriveInsights(
            rows = dao.observeSource().first(),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(12 * hour),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        ).onsetTimeCounts
        database.markerDao().deleteById(markerId)
        val afterDelete = deriveInsights(
            rows = dao.observeSource().first(),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(12 * hour),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        ).onsetTimeCounts

        assertTrue(before.isEligible)
        assertEquals(before, afterInsert)
        assertEquals(before, afterDelete)
    }
}
