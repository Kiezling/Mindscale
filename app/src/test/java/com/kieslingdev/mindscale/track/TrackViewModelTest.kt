package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fixedNow = 10_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(dao: FakeEntryDao = FakeEntryDao()): Pair<TrackViewModel, FakeEntryDao> {
        val vm = TrackViewModel(dao, nowProvider = { fixedNow })
        dispatcher.scheduler.runCurrent()
        return vm to dao
    }

    @Test
    fun `KeyTapped inserts entry at nowProvider and shows matching readout`() = runTest {
        val (viewModel, dao) = viewModel()

        viewModel.onEvent(TrackEvent.KeyTapped(7))
        dispatcher.scheduler.runCurrent()

        assertEquals(1, dao.insertCalls.size)
        assertEquals(fixedNow, dao.insertCalls[0].ts)
        assertEquals(7, dao.insertCalls[0].value)

        val readout = viewModel.uiState.value.transientReadout
        assertNotNull(readout)
        assertEquals(7, readout!!.value)
        assertEquals(band(7), readout.band)
    }

    @Test
    fun `transientReadout auto-clears after its expiry without another event`() = runTest {
        val (viewModel, _) = viewModel()

        viewModel.onEvent(TrackEvent.KeyTapped(5))
        dispatcher.scheduler.runCurrent()
        assertNotNull(viewModel.uiState.value.transientReadout)

        dispatcher.scheduler.advanceTimeBy(3_001)
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.transientReadout)
    }

    @Test
    fun `ReadoutDismissed clears the readout early`() = runTest {
        val (viewModel, _) = viewModel()

        viewModel.onEvent(TrackEvent.KeyTapped(5))
        dispatcher.scheduler.runCurrent()
        assertNotNull(viewModel.uiState.value.transientReadout)

        viewModel.onEvent(TrackEvent.ReadoutDismissed)
        assertNull(viewModel.uiState.value.transientReadout)
    }

    @Test
    fun `KeyLongPressed opens backdateDialog without inserting`() = runTest {
        val (viewModel, dao) = viewModel()

        viewModel.onEvent(TrackEvent.KeyLongPressed(6))
        dispatcher.scheduler.runCurrent()

        assertEquals(0, dao.insertCalls.size)
        val dialog = viewModel.uiState.value.backdateDialog
        assertNotNull(dialog)
        assertEquals(6, dialog!!.value)
        assertEquals(fixedNow, dialog.timestampMillis)
    }

    @Test
    fun `BackdateSaveConfirmed inserts at the edited timestamp and clears dialog`() = runTest {
        val (viewModel, dao) = viewModel()

        viewModel.onEvent(TrackEvent.KeyLongPressed(4))
        viewModel.onEvent(TrackEvent.BackdateTimestampChanged(fixedNow - 5_000))
        viewModel.onEvent(TrackEvent.BackdateSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, dao.insertCalls.size)
        assertEquals(fixedNow - 5_000, dao.insertCalls[0].ts)
        assertEquals(4, dao.insertCalls[0].value)
        assertNull(viewModel.uiState.value.backdateDialog)
    }

    @Test
    fun `BackdateCancelled discards the dialog with no DAO call`() = runTest {
        val (viewModel, dao) = viewModel()

        viewModel.onEvent(TrackEvent.KeyLongPressed(4))
        viewModel.onEvent(TrackEvent.BackdateCancelled)
        dispatcher.scheduler.runCurrent()

        assertEquals(0, dao.insertCalls.size)
        assertNull(viewModel.uiState.value.backdateDialog)
    }

    @Test
    fun `BackdateTimestampChanged in the future sets a non-null error and blocks save`() = runTest {
        val (viewModel, dao) = viewModel()

        viewModel.onEvent(TrackEvent.KeyLongPressed(4))
        viewModel.onEvent(TrackEvent.BackdateTimestampChanged(fixedNow + 1))

        val dialog = viewModel.uiState.value.backdateDialog
        assertNotNull(dialog!!.error)

        viewModel.onEvent(TrackEvent.BackdateSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(0, dao.insertCalls.size)
        assertNotNull(viewModel.uiState.value.backdateDialog)
    }

    @Test
    fun `EditTimestampChanged in the future sets a non-null error and blocks save`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 1_000L, value = 3))
        dispatcher.scheduler.runCurrent()
        val entry = viewModel.uiState.value.recentEntries.single { it.id == entryId }

        viewModel.onEvent(TrackEvent.EditRequested(entry))
        viewModel.onEvent(TrackEvent.EditTimestampChanged(fixedNow + 1))

        val dialog = viewModel.uiState.value.editDialog
        assertNotNull(dialog!!.error)

        viewModel.onEvent(TrackEvent.EditSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(0, dao.updateCalls.size)
        assertNotNull(viewModel.uiState.value.editDialog)
    }

    @Test
    fun `EditSaveConfirmed updates value and timestamp while preserving note`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 1_000L, value = 4, note = "existing note"))
        dispatcher.scheduler.runCurrent()
        val entry = viewModel.uiState.value.recentEntries.single { it.id == entryId }

        viewModel.onEvent(TrackEvent.EditRequested(entry))
        viewModel.onEvent(TrackEvent.EditValueChanged(9))
        viewModel.onEvent(TrackEvent.EditTimestampChanged(fixedNow - 100))
        viewModel.onEvent(TrackEvent.EditSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, dao.updateCalls.size)
        val updated = dao.updateCalls.single()
        assertEquals(entryId, updated.id)
        assertEquals(9, updated.value)
        assertEquals(fixedNow - 100, updated.ts)
        assertEquals("existing note", updated.note)
        assertNull(viewModel.uiState.value.editDialog)
    }

    @Test
    fun `EditSaveConfirmed preserves note and chips when the entry has scrolled out of recentEntries`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 500L, value = 4, note = "old note", chips = listOf("chip-a")))
        dispatcher.scheduler.runCurrent()
        val originalEntry = dao.insertCalls.first { it.id == entryId }

        // Push the target entry out of the top-10 recentEntries window with newer inserts.
        repeat(10) { i -> dao.insert(Entry(ts = 1_000L + i, value = 1)) }
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.recentEntries.none { it.id == entryId })

        // EditRequested carries the full Entry, so the dialog can save correctly
        // even though the entry is no longer present in recentEntries.
        viewModel.onEvent(TrackEvent.EditRequested(originalEntry))
        viewModel.onEvent(TrackEvent.EditValueChanged(8))
        viewModel.onEvent(TrackEvent.EditTimestampChanged(fixedNow - 50))
        viewModel.onEvent(TrackEvent.EditSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        val updated = dao.updateCalls.single()
        assertEquals(entryId, updated.id)
        assertEquals(8, updated.value)
        assertEquals(fixedNow - 50, updated.ts)
        assertEquals("old note", updated.note)
        assertEquals(listOf("chip-a"), updated.chips)
    }

    @Test
    fun `EditCancelled discards the dialog with no DAO call`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 1_000L, value = 4))
        dispatcher.scheduler.runCurrent()
        val entry = viewModel.uiState.value.recentEntries.single { it.id == entryId }

        viewModel.onEvent(TrackEvent.EditRequested(entry))
        viewModel.onEvent(TrackEvent.EditCancelled)
        dispatcher.scheduler.runCurrent()

        assertEquals(0, dao.updateCalls.size)
        assertNull(viewModel.uiState.value.editDialog)
    }

    @Test
    fun `Note events open, edit, save and cancel via entryDao update`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 1_000L, value = 4))
        dispatcher.scheduler.runCurrent()
        val entry = viewModel.uiState.value.recentEntries.single { it.id == entryId }

        viewModel.onEvent(TrackEvent.NoteRequested(entry))
        assertEquals(entryId, viewModel.uiState.value.noteDialog!!.entryId)

        viewModel.onEvent(TrackEvent.NoteTextChanged("feeling okay"))
        viewModel.onEvent(TrackEvent.NoteSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, dao.updateCalls.size)
        assertEquals("feeling okay", dao.updateCalls.single().note)
        assertNull(viewModel.uiState.value.noteDialog)

        viewModel.onEvent(TrackEvent.NoteRequested(entry))
        viewModel.onEvent(TrackEvent.NoteCancelled)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, dao.updateCalls.size)
        assertNull(viewModel.uiState.value.noteDialog)
    }

    @Test
    fun `NoteSaveConfirmed preserves the edit when the entry has scrolled out of recentEntries`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 500L, value = 4))
        dispatcher.scheduler.runCurrent()
        val originalEntry = dao.insertCalls.first { it.id == entryId }

        // Push the target entry out of the top-10 recentEntries window with newer inserts.
        repeat(10) { i -> dao.insert(Entry(ts = 1_000L + i, value = 1)) }
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.recentEntries.none { it.id == entryId })

        // NoteRequested carries the full Entry, so the dialog can save correctly
        // even though the entry is no longer present in recentEntries.
        viewModel.onEvent(TrackEvent.NoteRequested(originalEntry))
        viewModel.onEvent(TrackEvent.NoteTextChanged("newly added note"))
        viewModel.onEvent(TrackEvent.NoteSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        val updated = dao.updateCalls.single()
        assertEquals(entryId, updated.id)
        assertEquals("newly added note", updated.note)
        assertEquals(4, updated.value)
        assertEquals(500L, updated.ts)
    }

    @Test
    fun `DeleteRequested sets pendingDelete without calling delete`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 1_000L, value = 4))
        dispatcher.scheduler.runCurrent()
        val entry = viewModel.uiState.value.recentEntries.single { it.id == entryId }

        viewModel.onEvent(TrackEvent.DeleteRequested(entry))

        assertEquals(entry, viewModel.uiState.value.pendingDelete)
        assertEquals(0, dao.deleteCalls.size)
    }

    @Test
    fun `DeleteCancelled clears pendingDelete with no DAO call`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 1_000L, value = 4))
        dispatcher.scheduler.runCurrent()
        val entry = viewModel.uiState.value.recentEntries.single { it.id == entryId }

        viewModel.onEvent(TrackEvent.DeleteRequested(entry))
        viewModel.onEvent(TrackEvent.DeleteCancelled)

        assertNull(viewModel.uiState.value.pendingDelete)
        assertEquals(0, dao.deleteCalls.size)
    }

    @Test
    fun `DeleteConfirmed calls delete exactly once and clears pendingDelete`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 1_000L, value = 4))
        dispatcher.scheduler.runCurrent()
        val entry = viewModel.uiState.value.recentEntries.single { it.id == entryId }

        viewModel.onEvent(TrackEvent.DeleteRequested(entry))
        viewModel.onEvent(TrackEvent.DeleteConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, dao.deleteCalls.size)
        assertEquals(entry, dao.deleteCalls.single())
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun `uiState reflects observeRecent and observeCount from the DAO`() = runTest {
        val (viewModel, dao) = viewModel()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertTrue(viewModel.uiState.value.recentEntries.isEmpty())

        dao.insert(Entry(ts = 1_000L, value = 2))
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isEmpty)
        assertEquals(1, viewModel.uiState.value.recentEntries.size)
    }
}
