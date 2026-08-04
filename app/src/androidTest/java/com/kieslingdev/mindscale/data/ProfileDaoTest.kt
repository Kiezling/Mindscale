package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDaoTest {
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
    fun targetedProfileAndScoreMutationsAreOrderedAndChecked() = runBlocking {
        val dao = database.profileDao()
        assertEquals(1, dao.setDisplayName("Ada"))
        assertEquals(0, dao.setDisplayNameIfUnchanged("Lost update", "Wrong baseline"))
        assertEquals(1, dao.setDisplayNameIfUnchanged("Grace", "Ada"))
        assertEquals("Grace", dao.observeProfile().first().displayName)
        val older = dao.insertScore(score(ExternalInstrument.PHQ_8, 7, 10))
        val newer = dao.insertScore(score(ExternalInstrument.GAD_7, 8, 11))
        assertNotEquals(0, older)
        assertEquals(listOf(newer, older), dao.observeScores().first().map(ExternalScore::id))
        val row = requireNotNull(dao.scoreById(older))
        assertEquals(1, dao.updateScore(row.copy(total = 9)))
        assertEquals(9, dao.scoreById(older)?.total)
        assertEquals(1, dao.deleteScore(newer))
        assertEquals(0, dao.deleteScore(newer))
    }

    @Test
    fun uniqueInstrumentAndAssessmentDatePreventsSilentOverwrite() = runBlocking {
        val dao = database.profileDao()
        dao.insertScore(score(ExternalInstrument.PHQ_8, 7, 10))
        assertThrows(Exception::class.java) {
            runBlocking { dao.insertScore(score(ExternalInstrument.PHQ_8, 8, 10)) }
        }
        assertEquals(listOf(7), dao.observeScores().first().map(ExternalScore::total))
    }

    private fun score(instrument: ExternalInstrument, total: Int, day: Long) = ExternalScore(
        instrument = instrument,
        total = total,
        assessedEpochDay = day,
        enteredAt = day
    )
}
