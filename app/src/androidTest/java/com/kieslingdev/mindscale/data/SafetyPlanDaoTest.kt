package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafetyPlanDaoTest {
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
    fun addingAppendsWithinItsStepAndLeavesOtherStepsAlone() = runBlocking {
        val dao = database.safetyPlanDao()
        dao.addItem(SafetyPlanStep.INTERNAL_COPING, "Shower", null)
        dao.addItem(SafetyPlanStep.INTERNAL_COPING, "Walk to the corner", null)
        dao.addItem(SafetyPlanStep.WARNING_SIGNS, "Sleeping in", null)

        assertEquals(
            listOf(0, 1),
            dao.itemsIn(SafetyPlanStep.INTERNAL_COPING).map { it.position }
        )
        assertEquals(
            listOf("Shower", "Walk to the corner"),
            dao.itemsIn(SafetyPlanStep.INTERNAL_COPING).map { it.text }
        )
        assertEquals(listOf(0), dao.itemsIn(SafetyPlanStep.WARNING_SIGNS).map { it.position })
        assertEquals(3, dao.count())
    }

    @Test
    fun deletingClosesTheGapItLeftInThatStepOnly() = runBlocking {
        val dao = database.safetyPlanDao()
        dao.addItem(SafetyPlanStep.INTERNAL_COPING, "one", null)
        val middle = dao.addItem(SafetyPlanStep.INTERNAL_COPING, "two", null)
        dao.addItem(SafetyPlanStep.INTERNAL_COPING, "three", null)
        val other = dao.addItem(SafetyPlanStep.PROFESSIONALS, "Clinic", "555-0100")

        assertTrue(dao.removeItem(middle))

        val coping = dao.itemsIn(SafetyPlanStep.INTERNAL_COPING)
        assertEquals(listOf("one", "three"), coping.map { it.text })
        assertEquals(listOf(0, 1), coping.map { it.position })
        assertEquals(0, dao.itemById(other)?.position)
    }

    @Test
    fun removingARowThatIsAlreadyGoneReportsFalseRatherThanThrowing() = runBlocking {
        val dao = database.safetyPlanDao()
        assertFalse(dao.removeItem(9_999))
        assertNull(dao.itemById(9_999))
    }

    @Test
    fun updateContentIsTargetedAndReportsAffectedRows() = runBlocking {
        val dao = database.safetyPlanDao()
        val id = dao.addItem(SafetyPlanStep.PEOPLE_FOR_HELP, "Sam", "555-0100")
        val untouched = dao.addItem(SafetyPlanStep.PEOPLE_FOR_HELP, "Jo", null)

        assertEquals(1, dao.updateContent(id, "Sam Rivera", "+1 555 010 0199"))
        assertEquals(0, dao.updateContent(9_999, "ghost", null))

        val stored = dao.itemById(id)
        assertEquals("Sam Rivera", stored?.text)
        assertEquals("+1 555 010 0199", stored?.phone)
        assertEquals(0, stored?.position)
        assertEquals("Jo", dao.itemById(untouched)?.text)
        assertNull(dao.itemById(untouched)?.phone)
    }

    /**
     * The step is stored as its name, so `ORDER BY step` in SQL would sort alphabetically
     * — DISTRACTION before WARNING_SIGNS — which is not the canonical Stanley-Brown
     * order. Grouping happens in Kotlin for exactly this reason (Phase 13, D-4).
     */
    @Test
    fun groupingRestoresCanonicalStanleyBrownOrderRegardlessOfInsertionOrder() = runBlocking {
        val dao = database.safetyPlanDao()
        dao.addItem(SafetyPlanStep.ENVIRONMENT_SAFETY, "spare keys", null)
        dao.addItem(SafetyPlanStep.WARNING_SIGNS, "sleeping in", null)
        dao.addItem(SafetyPlanStep.PROFESSIONALS, "Clinic", "5550100")
        dao.addItem(SafetyPlanStep.DISTRACTION, "the library", null)

        val grouped = dao.observeAll().first().groupedByStep()

        assertEquals(SafetyPlanStep.entries.toList(), grouped.keys.toList())
        assertEquals(listOf("sleeping in"), grouped.getValue(SafetyPlanStep.WARNING_SIGNS).map { it.text })
        assertTrue(grouped.getValue(SafetyPlanStep.INTERNAL_COPING).isEmpty())
        assertEquals(listOf("the library"), grouped.getValue(SafetyPlanStep.DISTRACTION).map { it.text })
        assertEquals(listOf("Clinic"), grouped.getValue(SafetyPlanStep.PROFESSIONALS).map { it.text })
        assertEquals(
            listOf("spare keys"),
            grouped.getValue(SafetyPlanStep.ENVIRONMENT_SAFETY).map { it.text }
        )
    }

    @Test
    fun eraseDeletesEveryPlanLineAndReportsTheCount() = runBlocking {
        val dao = database.safetyPlanDao()
        dao.addItem(SafetyPlanStep.WARNING_SIGNS, "one", null)
        dao.addItem(SafetyPlanStep.PEOPLE_FOR_HELP, "Sam", "555-0100")

        val counts = database.dataControlDao().eraseEverythingAndResetSettings()

        assertEquals(2, counts.safetyPlanItems)
        assertEquals(0, dao.count())
    }

    @Test
    fun aRestoreReplacesThePlanWithTheFilesIdsVerbatim() = runBlocking {
        val control = database.dataControlDao()
        val dao = database.safetyPlanDao()
        dao.addItem(SafetyPlanStep.WARNING_SIGNS, "old line", null)

        val counts = control.replaceEverything(
            BackupPayload(
                version = 6,
                exportedAt = java.time.Instant.parse("2026-08-05T10:00:00Z"),
                entries = emptyList(),
                sleeps = emptyList(),
                markers = emptyList(),
                settings = TrackSettings(),
                profile = UserProfile(),
                externalScores = emptyList(),
                safetyPlan = listOf(
                    SafetyPlanItem(71, SafetyPlanStep.WARNING_SIGNS, 0, "restored sign"),
                    SafetyPlanItem(72, SafetyPlanStep.PEOPLE_FOR_HELP, 0, "Sam", "555-0100")
                )
            )
        )

        assertEquals(2, counts.safetyPlanItems)
        assertEquals(listOf(71L, 72L), control.allSafetyPlanItems().map { it.id })
        assertEquals(listOf("restored sign", "Sam"), control.allSafetyPlanItems().map { it.text })
    }

    /** A records CSV never touches the plan (Phase 13, D-5). */
    @Test
    fun anAdditiveRecordsImportLeavesThePlanUntouched() = runBlocking {
        val control = database.dataControlDao()
        val dao = database.safetyPlanDao()
        val id = dao.addItem(SafetyPlanStep.PROFESSIONALS, "Clinic", "555-0100")

        control.addRecords(
            RecordsPayload(
                entries = listOf(Entry(ts = 1_000, value = 4)),
                sleeps = emptyList(),
                markers = emptyList()
            )
        ) { true }

        assertEquals(1, dao.count())
        assertEquals("Clinic", dao.itemById(id)?.text)
    }
}
