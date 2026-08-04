package com.kieslingdev.mindscale.track

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.MindScaleDatabase
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDialogSavedStateTest {

    private lateinit var database: MindScaleDatabase
    private val restoredOwners = mutableListOf<TestOwner>()
    private val viewModelJobs = mutableListOf<Job>()

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MindScaleDatabase::class.java)
            .addCallback(MindScaleDatabase.seedSettingsCallback)
            .build()
        runBlocking { database.trackSettingsDao().observe().first() }
    }

    @After
    fun closeDatabase() {
        restoredOwners.forEach { owner -> onMain { owner.destroy() } }
        restoredOwners.clear()
        runBlocking { viewModelJobs.forEach { it.join() } }
        viewModelJobs.clear()
        val drained = CountDownLatch(2)
        database.queryExecutor.execute { drained.countDown() }
        database.transactionExecutor.execute { drained.countDown() }
        check(drained.await(5, TimeUnit.SECONDS)) { "Room executors did not drain before teardown" }
        database.close()
    }

    @Test
    fun allRestorableTrackDialogs_roundTripThroughBundleWithNewOwnerAndViewModelStore() = runBlocking {
        val entryId = database.entryDao().insert(
            Entry(ts = 0L, value = 4, chips = listOf("flat"), note = "old note")
        )
        val entry = Entry(entryId, 0L, 4, listOf("flat"), "old note")

        val backdate = roundTrip { viewModel ->
            viewModel.onEvent(TrackEvent.ArmWake)
            viewModel.onEvent(TrackEvent.KeyLongPressed(7))
            viewModel.onEvent(TrackEvent.BackdateDateTextChanged("1970-0"))
            viewModel.onEvent(TrackEvent.BackdateTimeTextChanged("1"))
        }.uiState.value.activeModal as TrackModalState.Backdate
        assertEquals(7, backdate.draft.value)
        assertEquals("1970-0", backdate.draft.dateText)
        assertEquals("1", backdate.draft.timeText)
        assertEquals(EntryKind.WAKE, backdate.draft.captureKind)

        val edit = roundTrip { viewModel ->
            viewModel.onEvent(TrackEvent.EditRequested(entry))
            viewModel.onEvent(TrackEvent.EditValueChanged(8))
            viewModel.onEvent(TrackEvent.EditDateTextChanged("1970-0"))
            viewModel.onEvent(TrackEvent.EditChipToggled("wired"))
        }.uiState.value.activeModal as TrackModalState.Edit
        assertEquals(entryId, edit.draft.entryId)
        assertEquals(4, edit.draft.baselineValue)
        assertEquals(listOf("flat"), edit.draft.baselineChips)
        assertEquals(8, edit.draft.value)
        assertEquals("1970-0", edit.draft.dateText)
        assertEquals(listOf("flat", "wired"), edit.draft.chips)

        val noteText = "  first line\nsecond line  "
        val note = roundTrip { viewModel ->
            viewModel.onEvent(TrackEvent.NoteRequested(entry))
            viewModel.onEvent(TrackEvent.NoteTextChanged(noteText))
        }.uiState.value.activeModal as TrackModalState.Note
        assertEquals(entryId, note.draft.entryId)
        assertEquals("old note", note.draft.baselineText)
        assertEquals(noteText, note.draft.text)
        assertNotNull(note.validation)
    }

    private fun roundTrip(mutate: (TrackViewModel) -> Unit): TrackViewModel {
        val firstOwner = onMain { TestOwner(null) }
        val first = onMain { createViewModel(firstOwner) }
        onMain { mutate(first) }
        val saved = onMain { firstOwner.saveAndDestroy() }

        val restoredOwner = onMain { TestOwner(saved) }
        restoredOwners += restoredOwner
        return onMain { createViewModel(restoredOwner) }
    }

    private fun createViewModel(owner: TestOwner): TrackViewModel {
        val factory = object : AbstractSavedStateViewModelFactory(owner, null) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T = TrackViewModel(
                entryDao = database.entryDao(),
                sleepDao = database.sleepDao(),
                markerDao = database.markerDao(),
                settingsDao = database.trackSettingsDao(),
                savedStateHandle = handle,
                nowProvider = { 300_000L },
                episodeSourceDao = database.episodeSourceDao(),
                zoneProvider = { ZoneId.of("UTC") }
            ) as T
        }
        return ViewModelProvider(owner, factory)[TrackViewModel::class.java].also { viewModel ->
            viewModelJobs += viewModel.viewModelScope.coroutineContext[Job]!!
        }
    }

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<T>()
        val failure = AtomicReference<Throwable?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                result.set(block())
            } catch (error: Throwable) {
                failure.set(error)
            }
        }
        failure.get()?.let { throw it }
        return result.get()
    }

    private class TestOwner(restoredState: Bundle?) :
        SavedStateRegistryOwner,
        ViewModelStoreOwner,
        LifecycleOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
        override val viewModelStore: ViewModelStore = ViewModelStore()

        init {
            controller.performAttach()
            controller.performRestore(restoredState)
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        fun saveAndDestroy(): Bundle {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            val output = Bundle()
            controller.performSave(output)
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
            return output
        }

        fun destroy() {
            if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            }
            viewModelStore.clear()
        }
    }
}
