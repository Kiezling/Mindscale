package com.kieslingdev.mindscale.safety

import androidx.lifecycle.SavedStateHandle
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.SafetyPlanStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SafetyViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        dao: FakeSafetyPlanDao = FakeSafetyPlanDao(),
        handle: SavedStateHandle = SavedStateHandle()
    ) = SafetyViewModel(dao, handle)

    @Test
    fun everyStepIsPresentAndEmptyOnFirstOpen() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(SafetyPlanStep.entries.toList(), vm.uiState.value.plan.keys.toList())
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun addingAppendsInOrderWithinItsStepOnly() = runTest {
        val dao = FakeSafetyPlanDao()
        val vm = viewModel(dao)
        dispatcher.scheduler.runCurrent()

        vm.startAdd(SafetyPlanStep.INTERNAL_COPING)
        vm.updateTextDraft("Shower")
        vm.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        vm.startAdd(SafetyPlanStep.INTERNAL_COPING)
        vm.updateTextDraft("Walk to the corner")
        vm.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        vm.startAdd(SafetyPlanStep.WARNING_SIGNS)
        vm.updateTextDraft("Sleeping in")
        vm.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val coping = vm.uiState.value.plan.getValue(SafetyPlanStep.INTERNAL_COPING)
        assertEquals(listOf("Shower", "Walk to the corner"), coping.map { it.text })
        assertEquals(listOf(0, 1), coping.map { it.position })
        assertEquals(
            listOf("Sleeping in"),
            vm.uiState.value.plan.getValue(SafetyPlanStep.WARNING_SIGNS).map { it.text }
        )
        assertNull(vm.uiState.value.editor)
    }

    @Test
    fun invalidTextKeepsTheEditorOpenAndWritesNothing() = runTest {
        val dao = FakeSafetyPlanDao()
        val vm = viewModel(dao)
        dispatcher.scheduler.runCurrent()

        vm.startAdd(SafetyPlanStep.WARNING_SIGNS)
        vm.updateTextDraft("   ")
        vm.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Write something first.", vm.uiState.value.editor?.textError)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun aPhoneOnANonContactStepIsRefused() = runTest {
        val dao = FakeSafetyPlanDao()
        val vm = viewModel(dao)
        dispatcher.scheduler.runCurrent()

        vm.startAdd(SafetyPlanStep.WARNING_SIGNS)
        vm.updateTextDraft("Cancelling plans")
        vm.updatePhoneDraft("5550100")
        vm.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("This step does not hold phone numbers.", vm.uiState.value.editor?.phoneError)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun deletingRenumbersTheRemainingLinesOfThatStep() = runTest {
        val dao = FakeSafetyPlanDao()
        dao.rows.value = listOf(
            SafetyPlanItem(1, SafetyPlanStep.INTERNAL_COPING, 0, "one"),
            SafetyPlanItem(2, SafetyPlanStep.INTERNAL_COPING, 1, "two"),
            SafetyPlanItem(3, SafetyPlanStep.INTERNAL_COPING, 2, "three"),
            SafetyPlanItem(4, SafetyPlanStep.WARNING_SIGNS, 0, "sign")
        )
        val vm = viewModel(dao)
        dispatcher.scheduler.runCurrent()

        vm.requestDelete(2)
        vm.confirmDelete()
        dispatcher.scheduler.advanceUntilIdle()

        val coping = vm.uiState.value.plan.getValue(SafetyPlanStep.INTERNAL_COPING)
        assertEquals(listOf("one", "three"), coping.map { it.text })
        assertEquals(listOf(0, 1), coping.map { it.position })
        // The other step is untouched.
        assertEquals(
            listOf(0),
            vm.uiState.value.plan.getValue(SafetyPlanStep.WARNING_SIGNS).map { it.position }
        )
    }

    @Test
    fun aWriteFailureKeepsTheEditorOpenWithWhatWasTyped() = runTest {
        val dao = FakeSafetyPlanDao()
        dao.failWrites = true
        val vm = viewModel(dao)
        dispatcher.scheduler.runCurrent()

        vm.startAdd(SafetyPlanStep.PEOPLE_FOR_HELP)
        vm.updateTextDraft("Sam")
        vm.updatePhoneDraft("555-0100")
        vm.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        val editor = vm.uiState.value.editor
        assertNotNull(editor)
        assertEquals("Sam", editor?.textDraft)
        assertEquals("555-0100", editor?.phoneDraft)
        assertEquals(false, editor?.saving)
        assertEquals("Could not save that. Nothing was changed.", vm.uiState.value.message)
    }

    /** A row deleted elsewhere is never silently recreated by a stale editor. */
    @Test
    fun savingAnEditForARowThatIsGoneReportsStaleAndReinsertsNothing() = runTest {
        val dao = FakeSafetyPlanDao()
        dao.rows.value = listOf(SafetyPlanItem(7, SafetyPlanStep.PROFESSIONALS, 0, "Clinic"))
        val vm = viewModel(dao)
        dispatcher.scheduler.runCurrent()

        vm.startEdit(7)
        vm.updateTextDraft("Clinic line")
        dao.rows.value = emptyList()
        vm.saveEditor()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.editor)
        assertEquals("That line is no longer here.", vm.uiState.value.message)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun theStepLimitAndPlanLimitAreEnforcedBeforeTheEditorOpens() = runTest {
        val dao = FakeSafetyPlanDao()
        dao.rows.value = (0 until MAX_PLAN_ITEMS_PER_STEP).map {
            SafetyPlanItem(it + 1L, SafetyPlanStep.WARNING_SIGNS, it, "sign $it")
        }
        val vm = viewModel(dao)
        dispatcher.scheduler.runCurrent()

        vm.startAdd(SafetyPlanStep.WARNING_SIGNS)
        assertNull(vm.uiState.value.editor)
        assertEquals(
            "This step already holds $MAX_PLAN_ITEMS_PER_STEP lines.",
            vm.uiState.value.message
        )
    }

    /** Phase 7 envelope pattern: only the open editor survives, never the plan itself. */
    @Test
    fun theOpenEditorSurvivesProcessDeathAndThePlanIsNotInSavedState() = runTest {
        val dao = FakeSafetyPlanDao()
        val handle = SavedStateHandle()
        val first = viewModel(dao, handle)
        dispatcher.scheduler.runCurrent()

        first.startAdd(SafetyPlanStep.PEOPLE_FOR_HELP)
        first.updateTextDraft("Sam")
        first.updatePhoneDraft("555-0100")

        val restored = viewModel(dao, handle)
        dispatcher.scheduler.runCurrent()

        val editor = restored.uiState.value.editor
        assertEquals(SafetyPlanStep.PEOPLE_FOR_HELP, editor?.step)
        assertNull(editor?.itemId)
        assertEquals("Sam", editor?.textDraft)
        assertEquals("555-0100", editor?.phoneDraft)
        assertTrue(
            "Only primitive editor keys may be persisted",
            handle.keys().all { it.startsWith("safety.editor.") }
        )
    }

    @Test
    fun cancellingClearsTheSavedEditor() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(handle = handle)
        dispatcher.scheduler.runCurrent()

        vm.startAdd(SafetyPlanStep.DISTRACTION)
        vm.updateTextDraft("The library")
        vm.cancelEdit()

        assertNull(viewModel(handle = handle).uiState.value.editor)
    }

    @Test
    fun aCallActionIsOfferedOnlyForAContactWithADialableNumber() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.runCurrent()

        assertEquals(
            SafetyAction.Dial("5550100"),
            vm.dialActionFor(
                SafetyPlanItem(1, SafetyPlanStep.PEOPLE_FOR_HELP, 0, "Sam", "555-0100")
            )
        )
        assertNull(
            vm.dialActionFor(SafetyPlanItem(2, SafetyPlanStep.PEOPLE_FOR_HELP, 1, "Jo", null))
        )
        assertNull(
            vm.dialActionFor(SafetyPlanItem(3, SafetyPlanStep.PEOPLE_FOR_HELP, 2, "Jo", "+-()"))
        )
        // Not a contact step, so no Call action even if a number somehow got stored.
        assertNull(
            vm.dialActionFor(
                SafetyPlanItem(4, SafetyPlanStep.WARNING_SIGNS, 0, "Sleeping in", "5550100")
            )
        )
    }

    @Test
    fun aMissingHandlerIsReportedAndNothingIsRecorded() = runTest {
        val dao = FakeSafetyPlanDao()
        val vm = viewModel(dao)
        dispatcher.scheduler.runCurrent()

        vm.reportActionUnavailable(SafetyAction.Dial("988"))

        assertEquals(
            "No app on this device can open the dialer. The number is 988.",
            vm.uiState.value.message
        )
        assertTrue("Opening or tapping must write nothing", dao.rows.value.isEmpty())
    }

    @Test
    fun aReadFailureIsSurfacedAndRetryable() = runTest {
        val dao = FakeSafetyPlanDao()
        dao.failReads = true
        val vm = viewModel(dao)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Could not open your safety plan.", vm.uiState.value.readError)
        vm.retry()
        assertNull(vm.uiState.value.readError)
    }
}
