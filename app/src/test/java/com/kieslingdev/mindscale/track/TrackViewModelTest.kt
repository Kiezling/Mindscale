package com.kieslingdev.mindscale.track

import androidx.lifecycle.SavedStateHandle
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
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

    private data class Fixture(
        val viewModel: TrackViewModel,
        val entryDao: FakeEntryDao,
        val sleepDao: FakeSleepDao,
        val markerDao: FakeMarkerDao,
        val settingsDao: FakeTrackSettingsDao
    )

    private fun viewModel(
        entryDao: FakeEntryDao = FakeEntryDao(),
        sleepDao: FakeSleepDao = FakeSleepDao(),
        markerDao: FakeMarkerDao = FakeMarkerDao(),
        settingsDao: FakeTrackSettingsDao = FakeTrackSettingsDao(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        now: () -> Long = { fixedNow }
    ): Fixture {
        val vm = TrackViewModel(
            entryDao,
            sleepDao,
            markerDao,
            settingsDao,
            savedStateHandle = savedStateHandle,
            nowProvider = now
        )
        dispatcher.scheduler.runCurrent()
        return Fixture(vm, entryDao, sleepDao, markerDao, settingsDao)
    }

    // ---------------------------------------------------------------------
    // Phase 1 (unchanged behavior)
    // ---------------------------------------------------------------------

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

    // ---------------------------------------------------------------------
    // Phase 2: onset detection
    // ---------------------------------------------------------------------

    @Test
    fun `KeyTapped with no prior entries is treated as onset and opens the chip prompt when askChips is on`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = true))
        val (viewModel, dao) = viewModel(settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.KeyTapped(7))
        dispatcher.scheduler.runCurrent()

        val insertedId = dao.insertCalls.single().id
        val prompt = viewModel.uiState.value.onsetChipPrompt
        assertNotNull(prompt)
        assertEquals(insertedId, prompt!!.entryId)
        assertTrue(prompt.selected.isEmpty())
    }

    @Test
    fun `KeyTapped after a nonzero prior entry is not onset and does not open the chip prompt`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = true))
        val entryDao = FakeEntryDao()
        entryDao.insert(Entry(ts = fixedNow - 100, value = 5))
        val (viewModel, _) = viewModel(entryDao = entryDao, settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.KeyTapped(6))
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.onsetChipPrompt)
    }

    @Test
    fun `KeyTapped after a prior zero-value entry is onset`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = true))
        val entryDao = FakeEntryDao()
        entryDao.insert(Entry(ts = fixedNow - 100, value = 0))
        val (viewModel, _) = viewModel(entryDao = entryDao, settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.KeyTapped(4))
        dispatcher.scheduler.runCurrent()

        assertNotNull(viewModel.uiState.value.onsetChipPrompt)
    }

    @Test
    fun `askChips false suppresses the chip prompt even on a genuine onset`() = runTest {
        val (viewModel, _) = viewModel(settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = false)))

        viewModel.onEvent(TrackEvent.KeyTapped(7))
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.onsetChipPrompt)
    }

    @Test
    fun `a Sleep-armed capture never opens the chip prompt even when askChips is on and it is an onset`() = runTest {
        val (viewModel, _) = viewModel(settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = true, sleepIntroShown = true)))

        viewModel.onEvent(TrackEvent.ArmSleep)
        viewModel.onEvent(TrackEvent.KeyTapped(7))
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.onsetChipPrompt)
    }

    /**
     * D-3 / Invariant 15: a backdated capture is judged only against what preceded it at
     * its own timestamp, never against an entry that exists with a later ts.
     */
    @Test
    fun `BackdateSaveConfirmed onset detection ignores entries with a later timestamp`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = true))
        val entryDao = FakeEntryDao()
        entryDao.insert(Entry(ts = 1_000L, value = 5)) // T1: nonzero
        entryDao.insert(Entry(ts = 3_000L, value = 0)) // T3: newest overall, but after T2
        val (viewModel, dao) = viewModel(entryDao = entryDao, settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.KeyLongPressed(6))
        viewModel.onEvent(TrackEvent.BackdateTimestampChanged(2_000L)) // T2, between T1 and T3
        viewModel.onEvent(TrackEvent.BackdateSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        val backdated = dao.insertCalls.single { it.ts == 2_000L }
        // Not an onset (T1's value=5 precedes it) despite T3 (value=0) existing later.
        assertNull(viewModel.uiState.value.onsetChipPrompt.takeIf { it?.entryId == backdated.id })
    }

    @Test
    fun `OnsetChipToggled adds and removes chips, Submit persists them and clears the prompt`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = true))
        val (viewModel, dao) = viewModel(settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.KeyTapped(7))
        dispatcher.scheduler.runCurrent()
        val insertedId = dao.insertCalls.single().id

        viewModel.onEvent(TrackEvent.OnsetChipToggled("flat"))
        viewModel.onEvent(TrackEvent.OnsetChipToggled("wired"))
        viewModel.onEvent(TrackEvent.OnsetChipToggled("flat")) // toggled back off
        assertEquals(setOf("wired"), viewModel.uiState.value.onsetChipPrompt!!.selected)

        viewModel.onEvent(TrackEvent.OnsetChipsSubmitted)
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.onsetChipPrompt)
        // A chips-only targeted update (not a full-row EntryDao.update) - see EntryDao.kt
        // doc comment: OnsetChipPromptState is frozen to entryId only, so Submit must
        // never risk overwriting other columns from a possibly-stale full-row snapshot.
        val (updatedId, updatedChips) = dao.updateChipsCalls.single()
        assertEquals(insertedId, updatedId)
        assertEquals(listOf("wired"), updatedChips)
        assertEquals(0, dao.updateCalls.size)
    }

    @Test
    fun `OnsetChipsSubmitted with an empty selection still calls updateChips, per the frozen behavior`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = true))
        val (viewModel, dao) = viewModel(settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.KeyTapped(7))
        dispatcher.scheduler.runCurrent()
        val insertedId = dao.insertCalls.single().id

        viewModel.onEvent(TrackEvent.OnsetChipsSubmitted)
        dispatcher.scheduler.runCurrent()

        val (updatedId, updatedChips) = dao.updateChipsCalls.single()
        assertEquals(insertedId, updatedId)
        assertTrue(updatedChips.isEmpty())
    }

    @Test
    fun `OnsetChipsSkipped clears the prompt with no DAO update call`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(askChips = true))
        val (viewModel, dao) = viewModel(settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.KeyTapped(7))
        dispatcher.scheduler.runCurrent()
        viewModel.onEvent(TrackEvent.OnsetChipToggled("flat"))

        viewModel.onEvent(TrackEvent.OnsetChipsSkipped)
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.onsetChipPrompt)
        assertEquals(0, dao.updateCalls.size)
        assertEquals(0, dao.updateChipsCalls.size)
    }

    // ---------------------------------------------------------------------
    // Phase 2: sleep/wake arming and capture
    // ---------------------------------------------------------------------

    @Test
    fun `ArmSleep sets armedCapture, a second ArmSleep clears it, ArmWake switches it`() = runTest {
        val (viewModel, _) = viewModel(settingsDao = FakeTrackSettingsDao(TrackSettings(sleepIntroShown = true)))

        viewModel.onEvent(TrackEvent.ArmSleep)
        dispatcher.scheduler.runCurrent()
        assertEquals(EntryKind.SLEEP, viewModel.uiState.value.armedCapture)

        viewModel.onEvent(TrackEvent.ArmSleep)
        dispatcher.scheduler.runCurrent()
        assertNull(viewModel.uiState.value.armedCapture)

        viewModel.onEvent(TrackEvent.ArmSleep)
        viewModel.onEvent(TrackEvent.ArmWake)
        dispatcher.scheduler.runCurrent()
        assertEquals(EntryKind.WAKE, viewModel.uiState.value.armedCapture)
    }

    @Test
    fun `KeyTapped while armed SLEEP with no open interval opens one, inserts kind SLEEP, and sets the asleep toast`() = runTest {
        val (viewModel, dao) = viewModel(settingsDao = FakeTrackSettingsDao(TrackSettings(sleepIntroShown = true)))

        viewModel.onEvent(TrackEvent.ArmSleep)
        dispatcher.scheduler.runCurrent()
        viewModel.onEvent(TrackEvent.KeyTapped(6))
        dispatcher.scheduler.runCurrent()

        val inserted = dao.insertCalls.single()
        assertEquals(EntryKind.SLEEP, inserted.kind)
        assertNull(viewModel.uiState.value.armedCapture)
        assertEquals("Asleep at 6", viewModel.uiState.value.toast)
        assertNotNull(viewModel.uiState.value.openSleepInterval)
        assertEquals(fixedNow, viewModel.uiState.value.openSleepInterval!!.startTs)
    }

    @Test
    fun `KeyTapped while armed SLEEP with one already open leaves it untouched and sets the already-asleep toast`() = runTest {
        val sleepDao = FakeSleepDao()
        sleepDao.insert(SleepInterval(startTs = 1_000L, endTs = null))
        val (viewModel, dao) = viewModel(
            sleepDao = sleepDao,
            settingsDao = FakeTrackSettingsDao(TrackSettings(sleepIntroShown = true))
        )

        viewModel.onEvent(TrackEvent.ArmSleep)
        dispatcher.scheduler.runCurrent()
        viewModel.onEvent(TrackEvent.KeyTapped(6))
        dispatcher.scheduler.runCurrent()

        assertEquals(EntryKind.SLEEP, dao.insertCalls.single().kind)
        assertEquals(1, sleepDao.insertCalls.size) // no second interval created
        assertTrue(viewModel.uiState.value.toast!!.startsWith("Already asleep since"))
    }

    @Test
    fun `KeyTapped while armed WAKE with an open interval closes it and sets the slept toast`() = runTest {
        val sleepDao = FakeSleepDao()
        sleepDao.insert(SleepInterval(startTs = fixedNow - (2 * 60 * 60 * 1000L), endTs = null))
        val (viewModel, dao) = viewModel(
            sleepDao = sleepDao,
            settingsDao = FakeTrackSettingsDao(TrackSettings(sleepIntroShown = true))
        )

        viewModel.onEvent(TrackEvent.ArmWake)
        dispatcher.scheduler.runCurrent()
        viewModel.onEvent(TrackEvent.KeyTapped(2))
        dispatcher.scheduler.runCurrent()

        assertEquals(EntryKind.WAKE, dao.insertCalls.single().kind)
        assertEquals("Slept 2h", viewModel.uiState.value.toast)
        assertNull(viewModel.uiState.value.openSleepInterval)
    }

    @Test
    fun `KeyTapped while armed WAKE with nothing open still inserts the entry and sets the no-sleep-was-open toast`() = runTest {
        val (viewModel, dao) = viewModel(settingsDao = FakeTrackSettingsDao(TrackSettings(sleepIntroShown = true)))

        viewModel.onEvent(TrackEvent.ArmWake)
        dispatcher.scheduler.runCurrent()
        viewModel.onEvent(TrackEvent.KeyTapped(3))
        dispatcher.scheduler.runCurrent()

        val inserted = dao.insertCalls.single()
        assertEquals(EntryKind.WAKE, inserted.kind)
        assertEquals(3, inserted.value)
        assertEquals("No sleep was open", viewModel.uiState.value.toast)
        assertNull(viewModel.uiState.value.openSleepInterval)
    }

    @Test
    fun `first-ever arm shows the intro toast and persists sleepIntroShown, a fresh ViewModel then shows the normal toast`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(sleepIntroShown = false))
        val (viewModel, _) = viewModel(settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.ArmSleep)
        dispatcher.scheduler.runCurrent()

        assertEquals("Marks time asleep — nothing is counted while you sleep", viewModel.uiState.value.toast)
        assertTrue(settingsDao.updateCalls.last().sleepIntroShown)

        // A brand-new ViewModel instance against the same (already-updated) settingsDao
        // must read sleepIntroShown from "disk", not from any in-memory carryover.
        val fresh = TrackViewModel(FakeEntryDao(), FakeSleepDao(), FakeMarkerDao(), settingsDao, nowProvider = { fixedNow })
        dispatcher.scheduler.runCurrent()
        fresh.onEvent(TrackEvent.ArmSleep)
        dispatcher.scheduler.runCurrent()

        assertEquals("Now tap how you felt going to sleep", fresh.uiState.value.toast)
    }

    // ---------------------------------------------------------------------
    // Phase 2: marker
    // ---------------------------------------------------------------------

    @Test
    fun `MarkerSaveConfirmed with non-blank trimmed text inserts a marker and sets the event-marked toast`() = runTest {
        val (viewModel, _, _, markerDao) = viewModel()

        viewModel.onEvent(TrackEvent.MarkerToggled)
        viewModel.onEvent(TrackEvent.MarkerDraftChanged("  dose change  "))
        viewModel.onEvent(TrackEvent.MarkerSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        val marker = markerDao.insertCalls.single()
        assertEquals("dose change", marker.text)
        assertEquals(fixedNow, marker.ts)
        assertEquals("Event marked", viewModel.uiState.value.toast)
        assertFalse(viewModel.uiState.value.markerOpen)
    }

    @Test
    fun `MarkerSaveConfirmed with blank text closes the input with no DAO call and no toast`() = runTest {
        val (viewModel, _, _, markerDao) = viewModel()

        viewModel.onEvent(TrackEvent.MarkerToggled)
        viewModel.onEvent(TrackEvent.MarkerDraftChanged("   "))
        viewModel.onEvent(TrackEvent.MarkerSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(0, markerDao.insertCalls.size)
        assertNull(viewModel.uiState.value.toast)
        assertFalse(viewModel.uiState.value.markerOpen)
    }

    @Test
    fun `MarkerCancelled closes the input with no DAO call`() = runTest {
        val (viewModel, _, _, markerDao) = viewModel()

        viewModel.onEvent(TrackEvent.MarkerToggled)
        viewModel.onEvent(TrackEvent.MarkerDraftChanged("something"))
        viewModel.onEvent(TrackEvent.MarkerCancelled)
        dispatcher.scheduler.runCurrent()

        assertEquals(0, markerDao.insertCalls.size)
        assertFalse(viewModel.uiState.value.markerOpen)
        assertEquals("", viewModel.uiState.value.markerDraft)
    }

    @Test
    fun `marker visibility and draft restore from SavedStateHandle after ViewModel recreation`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val first = viewModel(savedStateHandle = savedStateHandle).viewModel

        first.onEvent(TrackEvent.MarkerToggled)
        first.onEvent(TrackEvent.MarkerDraftChanged("dose changed at noon"))

        val restored = viewModel(savedStateHandle = savedStateHandle).viewModel

        assertTrue(restored.uiState.value.markerOpen)
        assertEquals("dose changed at noon", restored.uiState.value.markerDraft)
    }

    // ---------------------------------------------------------------------
    // Phase 2: pause / check-in
    // ---------------------------------------------------------------------

    @Test
    fun `CheckinStillUseful updates only checkinAt`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings())
        val (viewModel, _) = viewModel(settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.CheckinStillUseful)
        dispatcher.scheduler.runCurrent()

        val updated = settingsDao.updateCalls.single()
        assertEquals(fixedNow, updated.checkinAt)
        assertFalse(updated.paused)
    }

    @Test
    fun `CheckinPauseRequested updates checkinAt and pauses in the same write`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings())
        val (viewModel, _) = viewModel(settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.CheckinPauseRequested)
        dispatcher.scheduler.runCurrent()

        val updated = settingsDao.updateCalls.single()
        assertEquals(fixedNow, updated.checkinAt)
        assertTrue(updated.paused)
    }

    @Test
    fun `ResumeTracking clears paused`() = runTest {
        val settingsDao = FakeTrackSettingsDao(TrackSettings(paused = true))
        val (viewModel, _) = viewModel(settingsDao = settingsDao)

        viewModel.onEvent(TrackEvent.ResumeTracking)
        dispatcher.scheduler.runCurrent()

        assertFalse(settingsDao.updateCalls.single().paused)
    }

    // checkinAt's default is 0L (epoch); "now - 0 > 60 days" only holds for a realistic
    // wall-clock time, not the shared fixedNow=10_000L used elsewhere in this file (10
    // seconds since epoch). These two tests use a realistic `now` instead, specifically
    // to exercise Invariant 22's "checkinAt=0 means immediately eligible" clarification.
    private val realisticNow = 1_700_000_000_000L // an arbitrary real-world epoch millis

    @Test
    fun `showCheckin is false below 40 entries and true once 40 is reached with checkinAt at its default`() = runTest {
        val entryDao = FakeEntryDao()
        repeat(39) { i -> entryDao.insert(Entry(ts = i.toLong(), value = 1)) }
        val (viewModel, dao) = viewModel(entryDao = entryDao, now = { realisticNow })

        assertFalse(viewModel.uiState.value.showCheckin)

        dao.insert(Entry(ts = 1_000L, value = 1)) // 40th entry
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.showCheckin)
    }

    @Test
    fun `showCheckin goes false immediately after a checkinAt reset`() = runTest {
        val entryDao = FakeEntryDao()
        repeat(40) { i -> entryDao.insert(Entry(ts = i.toLong(), value = 1)) }
        val (viewModel, _) = viewModel(entryDao = entryDao, now = { realisticNow })

        assertTrue(viewModel.uiState.value.showCheckin)

        viewModel.onEvent(TrackEvent.CheckinStillUseful)
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.showCheckin)
    }

    @Test
    fun `showCheckin is true once the 60-day cooldown has genuinely elapsed since checkinAt`() = runTest {
        val entryDao = FakeEntryDao()
        repeat(40) { i -> entryDao.insert(Entry(ts = i.toLong(), value = 1)) }
        val sixtyOneDaysAgo = fixedNow - (61L * 24 * 60 * 60 * 1000)
        val (viewModel, _) = viewModel(
            entryDao = entryDao,
            settingsDao = FakeTrackSettingsDao(TrackSettings(checkinAt = sixtyOneDaysAgo))
        )

        assertTrue(viewModel.uiState.value.showCheckin)
    }

    @Test
    fun `showCheckin stays false while still within the 60-day cooldown`() = runTest {
        val entryDao = FakeEntryDao()
        repeat(40) { i -> entryDao.insert(Entry(ts = i.toLong(), value = 1)) }
        val thirtyDaysAgo = fixedNow - (30L * 24 * 60 * 60 * 1000)
        val (viewModel, _) = viewModel(
            entryDao = entryDao,
            settingsDao = FakeTrackSettingsDao(TrackSettings(checkinAt = thirtyDaysAgo))
        )

        assertFalse(viewModel.uiState.value.showCheckin)
    }

    @Test
    fun `showCheckin respects paused regardless of entry count and cooldown`() = runTest {
        val entryDao = FakeEntryDao()
        repeat(40) { i -> entryDao.insert(Entry(ts = i.toLong(), value = 1)) }
        val (viewModel, _) = viewModel(entryDao = entryDao, settingsDao = FakeTrackSettingsDao(TrackSettings(paused = true)))

        assertFalse(viewModel.uiState.value.showCheckin)
    }

    // ---------------------------------------------------------------------
    // Phase 2: help card
    // ---------------------------------------------------------------------

    @Test
    fun `ToggleHelp flips helpOpen, a successful capture resets it regardless of prior state`() = runTest {
        val (viewModel, _) = viewModel()

        viewModel.onEvent(TrackEvent.ToggleHelp)
        assertTrue(viewModel.uiState.value.helpOpen)

        viewModel.onEvent(TrackEvent.KeyTapped(5))
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.helpOpen)
    }

    // ---------------------------------------------------------------------
    // Phase 2: chip-editable EditEntryState
    // ---------------------------------------------------------------------

    @Test
    fun `EditRequested seeds chips, EditChipToggled mutates the draft, EditSaveConfirmed persists it`() = runTest {
        val (viewModel, dao) = viewModel()
        val entryId = dao.insert(Entry(ts = 1_000L, value = 4, chips = listOf("flat")))
        dispatcher.scheduler.runCurrent()
        val entry = viewModel.uiState.value.recentEntries.single { it.id == entryId }

        viewModel.onEvent(TrackEvent.EditRequested(entry))
        assertEquals(setOf("flat"), viewModel.uiState.value.editDialog!!.chips)

        viewModel.onEvent(TrackEvent.EditChipToggled("wired"))
        viewModel.onEvent(TrackEvent.EditChipToggled("flat"))
        assertEquals(setOf("wired"), viewModel.uiState.value.editDialog!!.chips)

        viewModel.onEvent(TrackEvent.EditSaveConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("wired"), dao.updateCalls.single().chips)
    }
}
