package com.kieslingdev.mindscale.log

import androidx.lifecycle.SavedStateHandle
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.track.FakeEntryDao
import com.kieslingdev.mindscale.track.FakeMarkerDao
import com.kieslingdev.mindscale.track.FakeSleepDao
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class LogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.of("America/Chicago")
    private val now = LocalDate.of(2026, 8, 3).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private data class Fixture(
        val vm: LogViewModel,
        val entries: FakeEntryDao,
        val sleeps: FakeSleepDao,
        val markers: FakeMarkerDao
    )

    private fun fixture(handle: SavedStateHandle = SavedStateHandle()): Fixture {
        val entries = FakeEntryDao()
        val sleeps = FakeSleepDao()
        val markers = FakeMarkerDao()
        val vm = LogViewModel(entries, sleeps, markers, handle, { zone }, { now })
        dispatcher.scheduler.runCurrent()
        return Fixture(vm, entries, sleeps, markers)
    }

    @Test
    fun `combines all three DAO streams into one record count`() = runTest {
        val f = fixture()
        f.entries.insert(Entry(ts = now - 3_000, value = 5))
        f.sleeps.insert(SleepInterval(startTs = now - 2_000, endTs = now - 1_000))
        f.markers.insert(Marker(ts = now, text = "dose change"))
        dispatcher.scheduler.runCurrent()

        assertEquals(3, f.vm.uiState.value.recordCount)
        assertEquals(3, f.vm.uiState.value.days.single().items.size)
    }

    @Test
    fun `event edit changes text and timestamp without changing id or other markers`() = runTest {
        val f = fixture()
        val id = f.markers.insert(Marker(ts = now - 120_000, text = "before"))
        val otherId = f.markers.insert(Marker(ts = now - 60_000, text = "other"))
        dispatcher.scheduler.runCurrent()

        f.vm.onEvent(LogEvent.EventEditToggled(id))
        f.vm.onEvent(LogEvent.EventTextChanged("after"))
        f.vm.onEvent(LogEvent.EventTimestampChanged("2026-08-03 11:30"))
        f.vm.onEvent(LogEvent.EventSaveRequested)
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(id), f.markers.updateEditableFieldsCalls)
        assertEquals("after", f.markers.getById(id)?.text)
        assertEquals(11, java.time.Instant.ofEpochMilli(f.markers.getById(id)!!.ts).atZone(zone).hour)
        assertEquals("other", f.markers.getById(otherId)?.text)
        assertNull(f.vm.uiState.value.eventDraft)
    }

    @Test
    fun `event invalid future and failed writes keep draft for retry`() = runTest {
        val f = fixture()
        val id = f.markers.insert(Marker(ts = now - 60_000, text = "before"))
        dispatcher.scheduler.runCurrent()
        f.vm.onEvent(LogEvent.EventEditToggled(id))
        f.vm.onEvent(LogEvent.EventTextChanged("changed"))
        f.vm.onEvent(LogEvent.EventTimestampChanged("2026-08-03 13:00"))
        f.vm.onEvent(LogEvent.EventSaveRequested)
        assertNotNull(f.vm.uiState.value.eventDraft?.error)
        assertTrue(f.markers.updateEditableFieldsCalls.isEmpty())

        f.vm.onEvent(LogEvent.EventTimestampChanged("2026-08-03 11:00"))
        f.markers.updateError = IllegalStateException("disk")
        f.vm.onEvent(LogEvent.EventSaveRequested)
        dispatcher.scheduler.runCurrent()
        assertEquals("changed", f.vm.uiState.value.eventDraft?.text)
        assertNotNull(f.vm.uiState.value.eventDraft?.error)
        f.markers.updateError = null
        f.vm.onEvent(LogEvent.EventSaveRequested)
        dispatcher.scheduler.runCurrent()
        assertEquals("changed", f.markers.getById(id)?.text)
        assertNull(f.vm.uiState.value.eventDraft)
    }

    @Test
    fun `event draft restores and missing row closes editor`() = runTest {
        val handle = SavedStateHandle()
        val f = fixture(handle)
        val id = f.markers.insert(Marker(ts = now - 60_000, text = "before"))
        dispatcher.scheduler.runCurrent()
        f.vm.onEvent(LogEvent.EventEditToggled(id))
        f.vm.onEvent(LogEvent.EventTextChanged("restored draft"))
        val restored = LogViewModel(f.entries, f.sleeps, f.markers, handle, { zone }, { now })
        assertEquals("restored draft", restored.uiState.value.eventDraft?.text)
        f.markers.deleteById(id)
        f.vm.onEvent(LogEvent.EventSaveRequested)
        dispatcher.scheduler.runCurrent()
        assertNull(f.vm.uiState.value.eventDraft)
        assertEquals("That record no longer exists", f.vm.uiState.value.message)
    }

    @Test
    fun `delete note clears only note after Save and keeps rating`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now - 60_000, value = 7, note = "private", chips = listOf("foggy")))
        dispatcher.scheduler.runCurrent()
        f.vm.onEvent(LogEvent.NoteToggled(id))
        f.vm.onEvent(LogEvent.NoteDeleteRequested)
        assertEquals("", f.vm.uiState.value.noteDraft?.text)
        f.vm.onEvent(LogEvent.NoteSaved)
        dispatcher.scheduler.runCurrent()
        val saved = f.entries.observeRecent().first().single()
        assertEquals(id, saved.id)
        assertEquals(7, saved.value)
        assertEquals(listOf("foggy"), saved.chips)
        assertNull(saved.note)
    }

    @Test
    fun `invalid pending range keeps last valid applied filter`() = runTest {
        val f = fixture()
        val validFrom = LocalDate.of(2026, 7, 1)
        f.vm.onEvent(LogEvent.FromChanged(validFrom))
        f.vm.onEvent(LogEvent.ToChanged(LocalDate.of(2026, 6, 1)))

        val state = f.vm.uiState.value
        assertEquals(validFrom, state.appliedFilter.from)
        assertNull(state.appliedFilter.to)
        assertNotNull(state.filterError)
    }

    @Test
    fun `inline value edit uses targeted mutation and closes editor`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now - 1_000, value = 4, note = "preserve", chips = listOf("flat")))
        dispatcher.scheduler.runCurrent()

        f.vm.onEvent(LogEvent.EditToggled(id))
        f.vm.onEvent(LogEvent.EditValueSelected(7))
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(id), f.entries.updateEditableFieldsCalls)
        assertEquals("preserve", f.entries.observeRecent().first().single().note)
        assertNull(f.vm.uiState.value.editDraft)
    }

    @Test
    fun `chip and timestamp edits commit targeted fields while editor stays open`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now - 1_000, value = 4, note = "preserve"))
        dispatcher.scheduler.runCurrent()

        f.vm.onEvent(LogEvent.EditToggled(id))
        f.vm.onEvent(LogEvent.EditChipToggled("foggy"))
        dispatcher.scheduler.runCurrent()
        f.vm.onEvent(LogEvent.EditTimestampTextChanged("2026-08-03 11:00"))
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(id, id), f.entries.updateEditableFieldsCalls)
        val updated = f.entries.observeRecent().first().single()
        assertEquals(listOf("foggy"), updated.chips)
        assertEquals("preserve", updated.note)
        assertNotNull(f.vm.uiState.value.editDraft)
        assertNull(f.vm.uiState.value.editDraft?.error)
    }

    // R-3: every Full Log edit gesture revalidates the timestamp before writing.
    @Test
    fun `future timestamp blocks rating and chip taps without a write`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now - 1_000, value = 4))
        dispatcher.scheduler.runCurrent()
        f.vm.onEvent(LogEvent.EditToggled(id))
        f.vm.onEvent(LogEvent.EditTimestampTextChanged("2026-08-03 13:00"))
        f.vm.onEvent(LogEvent.EditValueSelected(7))
        f.vm.onEvent(LogEvent.EditChipToggled("foggy"))
        dispatcher.scheduler.runCurrent()

        assertTrue(f.entries.updateEditableFieldsCalls.isEmpty())
        assertNotNull(f.vm.uiState.value.editDraft)
        assertNotNull(f.vm.uiState.value.editDraft?.error)
    }

    @Test
    fun `missing row during edit closes stale draft and reports it`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now - 1_000, value = 4))
        dispatcher.scheduler.runCurrent()
        f.vm.onEvent(LogEvent.EditToggled(id))
        f.entries.deleteById(id)

        f.vm.onEvent(LogEvent.EditValueSelected(7))
        dispatcher.scheduler.runCurrent()

        assertNull(f.vm.uiState.value.editDraft)
        assertEquals("That record no longer exists", f.vm.uiState.value.message)
    }

    @Test
    fun `note save uses note-only mutation`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now - 1_000, value = 4, chips = listOf("wired")))
        dispatcher.scheduler.runCurrent()

        f.vm.onEvent(LogEvent.NoteToggled(id))
        f.vm.onEvent(LogEvent.NoteTextChanged("new note"))
        f.vm.onEvent(LogEvent.NoteSaved)
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(id), f.entries.updateNoteCalls)
        assertEquals("new note", f.entries.observeRecent().first().single().note)
        assertEquals(listOf("wired"), f.entries.observeRecent().first().single().chips)
    }

    @Test
    fun `note write failure keeps the draft available for retry`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now - 1_000, value = 4))
        dispatcher.scheduler.runCurrent()
        f.vm.onEvent(LogEvent.NoteToggled(id))
        f.vm.onEvent(LogEvent.NoteTextChanged("keep this draft"))
        f.entries.updateNoteError = IllegalStateException("disk unavailable")

        f.vm.onEvent(LogEvent.NoteSaved)
        dispatcher.scheduler.runCurrent()

        assertEquals(LogNoteDraft(id, "keep this draft"), f.vm.uiState.value.noteDraft)
        assertEquals("Could not save that note. Please try again.", f.vm.uiState.value.message)
    }

    // R-1/R-3: Full Log note limits use Unicode code points and preserve invalid drafts.
    @Test
    fun `note accepts 4000 astral code points and rejects 4001 plus controls`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now - 1_000, value = 4))
        dispatcher.scheduler.runCurrent()
        f.vm.onEvent(LogEvent.NoteToggled(id))
        f.vm.onEvent(LogEvent.NoteTextChanged("😀".repeat(4_000)))
        f.vm.onEvent(LogEvent.NoteSaved)
        dispatcher.scheduler.runCurrent()
        assertEquals("😀".repeat(4_000), f.entries.observeRecent().first().single().note)

        f.vm.onEvent(LogEvent.NoteToggled(id))
        f.vm.onEvent(LogEvent.NoteTextChanged("😀".repeat(4_001)))
        f.vm.onEvent(LogEvent.NoteSaved)
        assertNotNull(f.vm.uiState.value.noteDraft)
        assertNotNull(f.vm.uiState.value.noteDraft?.error)

        f.vm.onEvent(LogEvent.NoteTextChanged("keep\uFEFFdraft\u0001"))
        f.vm.onEvent(LogEvent.NoteSaved)
        assertEquals("keep\uFEFFdraft\u0001", f.vm.uiState.value.noteDraft?.text)
        assertTrue(f.entries.updateNoteCalls.size == 1)
    }

    @Test
    fun `saved state restores open note draft`() = runTest {
        val handle = SavedStateHandle()
        val first = fixture(handle)
        val id = first.entries.insert(Entry(ts = now, value = 3))
        dispatcher.scheduler.runCurrent()
        first.vm.onEvent(LogEvent.NoteToggled(id))
        first.vm.onEvent(LogEvent.NoteTextChanged("unfinished"))

        val restored = LogViewModel(FakeEntryDao(), FakeSleepDao(), FakeMarkerDao(), handle, { zone }, { now })
        dispatcher.scheduler.runCurrent()

        assertEquals(LogNoteDraft(id, "unfinished"), restored.uiState.value.noteDraft)
    }

    @Test
    fun `edit and note panels are mutually exclusive`() = runTest {
        val f = fixture()
        val id = f.entries.insert(Entry(ts = now, value = 3))
        dispatcher.scheduler.runCurrent()

        f.vm.onEvent(LogEvent.EditToggled(id))
        assertNotNull(f.vm.uiState.value.editDraft)
        assertNull(f.vm.uiState.value.noteDraft)

        f.vm.onEvent(LogEvent.NoteToggled(id))
        assertNull(f.vm.uiState.value.editDraft)
        assertNotNull(f.vm.uiState.value.noteDraft)
    }

    @Test
    fun `delete dispatches every record type only to its matching DAO`() = runTest {
        val f = fixture()
        val entryId = f.entries.insert(Entry(ts = now - 2_000, value = 5))
        val sleepId = f.sleeps.insert(SleepInterval(startTs = now - 1_000, endTs = now))
        val markerId = f.markers.insert(Marker(ts = now, text = "therapy"))
        dispatcher.scheduler.runCurrent()
        val items = f.vm.uiState.value.days.single().items

        items.forEach { item ->
            f.vm.onEvent(LogEvent.DeleteRequested(item))
            f.vm.onEvent(LogEvent.DeleteConfirmed)
            dispatcher.scheduler.runCurrent()
        }

        assertEquals(0, f.vm.uiState.value.recordCount)
        assertEquals(listOf(entryId), f.entries.deleteByIdCalls)
        assertEquals(listOf(sleepId), f.sleeps.deleteByIdCalls)
        assertEquals(listOf(markerId), f.markers.deleteByIdCalls)
    }

    @Test
    fun `delete failure keeps confirmation target available for retry`() = runTest {
        val f = fixture()
        f.sleeps.insert(SleepInterval(startTs = now - 2_000, endTs = now - 1_000))
        dispatcher.scheduler.runCurrent()
        val sleepItem = f.vm.uiState.value.days.single().items.single() as LogItem.Sleep
        f.sleeps.deleteByIdError = IllegalStateException("disk unavailable")

        f.vm.onEvent(LogEvent.DeleteRequested(sleepItem))
        f.vm.onEvent(LogEvent.DeleteConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(sleepItem.id), f.sleeps.deleteByIdCalls)
        assertNotNull(f.vm.uiState.value.deleteTarget)
        assertEquals("Could not delete that record. Please try again.", f.vm.uiState.value.message)
    }
}
