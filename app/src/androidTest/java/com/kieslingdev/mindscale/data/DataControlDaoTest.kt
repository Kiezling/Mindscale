package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataControlDaoTest {
    private lateinit var database: MindScaleDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun snapshotIsDeterministicallyOrderedAndComplete() = runBlocking {
        database.entryDao().insert(Entry(ts = 10, value = 1))
        database.entryDao().insert(Entry(ts = 20, value = 2))
        database.sleepDao().insert(SleepInterval(startTs = 30, endTs = 40))
        database.markerDao().insert(Marker(ts = 50, text = "event"))
        database.trackSettingsDao().setAnchors("low", "mid", "high")
        database.profileDao().setDisplayName("Ada")
        database.profileDao().insertScore(
            ExternalScore(
                instrument = ExternalInstrument.PHQ_8,
                total = 8,
                assessedEpochDay = 20,
                enteredAt = 60
            )
        )

        val snapshot = database.dataControlDao().snapshot()
        assertEquals(listOf(20L, 10L), snapshot.entries.map(Entry::ts))
        assertEquals(1, snapshot.sleeps.size)
        assertEquals(1, snapshot.markers.size)
        assertEquals("mid", snapshot.settings.anchor5)
        assertEquals("Ada", snapshot.profile.displayName)
        assertEquals(listOf(8), snapshot.externalScores.map(ExternalScore::total))
    }

    @Test
    fun eraseDeletesAllRecordsAndResetsSettings() = runBlocking {
        database.entryDao().insert(Entry(ts = 10, value = 1))
        database.sleepDao().insert(SleepInterval(startTs = 30, endTs = 40))
        database.markerDao().insert(Marker(ts = 50, text = "event"))
        database.trackSettingsDao().setPaused(true)
        database.trackSettingsDao().setAppearance(ThemeMode.DARK)
        database.trackSettingsDao().setHoldDuration(HoldDuration.TWENTY_FOUR)
        database.profileDao().setDisplayName("Ada")
        database.profileDao().insertScore(
            ExternalScore(
                instrument = ExternalInstrument.GAD_7,
                total = 9,
                assessedEpochDay = 20,
                enteredAt = 60
            )
        )

        assertEquals(EraseCounts(1, 1, 1, 1), database.dataControlDao().eraseEverythingAndResetSettings())
        val snapshot = database.dataControlDao().snapshot()
        assertEquals(0, snapshot.entries.size)
        assertEquals(0, snapshot.sleeps.size)
        assertEquals(0, snapshot.markers.size)
        assertEquals(ThemeMode.SYSTEM, snapshot.settings.themeMode)
        assertEquals(HoldDuration.SIXTEEN, snapshot.settings.holdDuration)
        assertFalse(snapshot.settings.paused)
        assertEquals("", snapshot.profile.displayName)
        assertEquals(0, snapshot.externalScores.size)
    }
}
