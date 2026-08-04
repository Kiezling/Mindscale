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
class TrackSettingsDaoTest {

    private lateinit var database: MindScaleDatabase
    private lateinit var dao: TrackSettingsDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, MindScaleDatabase::class.java)
            .addCallback(MindScaleDatabase.seedSettingsCallback)
            .build()
        dao = database.trackSettingsDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun observe_emitsCanonicalRowWithFrozenDefaults_immediatelyAfterCreation() = runBlocking {
        val settings = dao.observe().first()

        assertEquals(0, settings.id)
        assertTrue(settings.sleepOn)
        assertFalse(settings.askChips)
        assertFalse(settings.paused)
        assertEquals(0L, settings.checkinAt)
        assertFalse(settings.sleepIntroShown)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(HourFormat.TWELVE, settings.hourFormat)
        assertEquals(DEFAULT_ONSET_CHIPS, settings.onsetChips)
        assertFalse(settings.hideNotes)
        assertFalse(settings.anchorPromptDone)
    }

    @Test
    fun update_persistsChangesAndObserveReflectsThem() = runBlocking {
        val current = dao.observe().first()
        dao.update(current.copy(askChips = true, paused = true, checkinAt = 99_999L, sleepIntroShown = true))

        val updated = dao.observe().first()
        assertTrue(updated.askChips)
        assertTrue(updated.paused)
        assertEquals(99_999L, updated.checkinAt)
        assertTrue(updated.sleepIntroShown)
        // sleepOn untouched by this update.
        assertTrue(updated.sleepOn)
    }

    @Test
    fun targetedUpdates_preserveUnrelatedFields() = runBlocking {
        assertEquals(1, dao.setAnchors("low", "middle", "high"))
        assertEquals(1, dao.setAppearance(ThemeMode.DARK))
        assertEquals(1, dao.setHideNotes(true))

        val updated = dao.current()
        assertEquals("low", updated.anchor2)
        assertEquals("middle", updated.anchor5)
        assertEquals("high", updated.anchor8)
        assertEquals(ThemeMode.DARK, updated.themeMode)
        assertTrue(updated.hideNotes)
        assertTrue(updated.sleepOn)
        assertFalse(updated.askChips)
    }

    @Test
    fun disablingSleep_isRejectedWhileAnIntervalIsOpen() = runBlocking {
        database.sleepDao().insert(SleepInterval(startTs = 100L))
        assertEquals(SleepSettingOutcome.OpenInterval, dao.setSleepOnSafely(false))
        assertTrue(dao.current().sleepOn)
        database.sleepDao().captureWake(200L)
        assertEquals(SleepSettingOutcome.Updated, dao.setSleepOnSafely(false))
        assertFalse(dao.current().sleepOn)
    }
}
