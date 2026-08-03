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
}
