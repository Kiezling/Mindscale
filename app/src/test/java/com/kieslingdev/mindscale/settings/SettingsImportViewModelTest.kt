package com.kieslingdev.mindscale.settings

import androidx.lifecycle.SavedStateHandle
import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import com.kieslingdev.mindscale.track.FakeTrackSettingsDao
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

/** Import state machine, atomicity, and saved-state discipline (SPEC-import-restore.md). */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-09-01T00:00:00Z")

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        data: FakeDataControlDao,
        handle: SavedStateHandle = SavedStateHandle(),
        onDataReplaced: () -> Unit = {}
    ) = SettingsViewModel(
        settingsDao = FakeTrackSettingsDao(),
        dataControlDao = data,
        savedStateHandle = handle,
        nowProvider = { now },
        onEraseCompleted = {},
        zoneProvider = { ZoneId.of("UTC") },
        onDataReplaced = onDataReplaced,
        ioContext = dispatcher
    )

    private fun stream(text: String): suspend () -> InputStream =
        { ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)) }

    private fun backupText(): String = encodeBackup(
        DataSnapshot(
            entries = listOf(Entry(11, Instant.parse("2026-08-02T09:00:00Z").toEpochMilli(), 7)),
            sleeps = listOf(SleepInterval(12, Instant.parse("2026-08-01T22:00:00Z").toEpochMilli(), Instant.parse("2026-08-02T06:00:00Z").toEpochMilli())),
            markers = listOf(Marker(13, Instant.parse("2026-08-02T10:00:00Z").toEpochMilli(), "restored event")),
            settings = TrackSettings(themeMode = ThemeMode.DARK, anchor2 = "restored anchor", onsetChips = listOf("calm"), holdDuration = HoldDuration.EIGHT),
            profile = UserProfile(displayName = "Ada L"),
            externalScores = listOf(
                ExternalScore(14, ExternalInstrument.GAD_7, 9, LocalDate.of(2026, 8, 1).toEpochDay(), enteredAt = Instant.parse("2026-08-02T00:00:00Z").toEpochMilli())
            )
        ),
        Instant.parse("2026-08-03T12:34:56Z")
    )

    private fun csvText(): String =
        "$RECORDS_CSV_HEADER\r\nmarker,2026-08-05T10:00:00Z,,,,,,added event\r\n"

    @Test
    fun previewIsShownBeforeAnythingChangesAndConfirmReplacesEverything() = runTest {
        val data = FakeDataControlDao()
        var replacedCallbacks = 0
        val vm = viewModel(data, onDataReplaced = { replacedCallbacks += 1 })
        dispatcher.scheduler.runCurrent()

        vm.requestBackupRestore()
        assertEquals(ImportKind.BACKUP_RESTORE, vm.uiState.value.importLaunch)
        vm.importLaunchHandled()

        vm.importFileSelected(ImportKind.BACKUP_RESTORE, stream(backupText()))
        dispatcher.scheduler.advanceUntilIdle()

        val pending = assertNotNull(vm.uiState.value.pendingImport).let { vm.uiState.value.pendingImport!! }
        assertEquals(ImportKind.BACKUP_RESTORE, pending.kind)
        assertTrue(pending.preview.lines.any { it.contains("permanently delete 1 rating, 1 sleep period, 1 marked event") })
        // Nothing has been written yet.
        assertEquals(listOf(1L), data.entries.map { it.id })
        assertEquals("", data.profile.displayName)

        vm.confirmImport()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.pendingImport)
        assertNull(vm.uiState.value.importError)
        assertEquals(ImportMessages.restored(1, 1, 1, 1, 0, 0), vm.uiState.value.message)
        assertEquals(listOf(11L), data.entries.map { it.id })
        assertEquals(listOf(12L), data.sleeps.map { it.id })
        assertEquals(listOf(13L), data.markers.map { it.id })
        assertEquals(listOf(14L), data.externalScores.map { it.id })
        assertEquals("Ada L", data.profile.displayName)
        assertEquals(ThemeMode.DARK, data.settings.themeMode)
        assertEquals(HoldDuration.EIGHT, data.settings.holdDuration)
        assertEquals(1, replacedCallbacks)
        // Stale settings drafts are reseeded from the restored values, never left behind.
        assertEquals("restored anchor", vm.uiState.value.anchorDraft.anchor2)
        assertEquals("calm", vm.uiState.value.chipDraft)
    }

    @Test
    fun csvImportAddsRecordsAndLeavesSettingsProfileAndTotalsAlone() = runTest {
        val data = FakeDataControlDao()
        data.profile = UserProfile(displayName = "Kept")
        data.settings = TrackSettings(themeMode = ThemeMode.LIGHT)
        val vm = viewModel(data)
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.RECORDS_MERGE, stream(csvText()))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(
            vm.uiState.value.pendingImport!!.preview.lines.any { it.contains("Nothing is deleted.") }
        )

        vm.confirmImport()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportMessages.added(0, 0, 1, 0), vm.uiState.value.message)
        assertEquals(2, data.markers.size)
        assertEquals(1, data.entries.size)
        assertEquals("Kept", data.profile.displayName)
        assertEquals(ThemeMode.LIGHT, data.settings.themeMode)
    }

    @Test
    fun aRejectedFileReportsWhyAndChangesNothing() = runTest {
        val data = FakeDataControlDao()
        val vm = viewModel(data)
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.BACKUP_RESTORE, stream("""{"format": "other"}"""))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.pendingImport)
        assertEquals(ImportMessages.NOT_A_BACKUP, vm.uiState.value.importError)
        assertEquals(1, data.entries.size)
        assertFalse(vm.uiState.value.importing)

        // A file that is not JSON at all fails structurally, before any format claim.
        vm.importFileSelected(ImportKind.BACKUP_RESTORE, stream("not json"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ImportMessages.invalidJson(1, 1), vm.uiState.value.importError)
        assertEquals(1, data.entries.size)

        vm.dismissImportError()
        assertNull(vm.uiState.value.importError)
    }

    @Test
    fun pickerCancellationIsNeutral() = runTest {
        val vm = viewModel(FakeDataControlDao())
        dispatcher.scheduler.runCurrent()
        vm.requestRecordsImport()
        vm.importPickerCanceled()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.importLaunch)
        assertNull(vm.uiState.value.importError)
        assertNull(vm.uiState.value.message)
        assertFalse(vm.uiState.value.importing)
    }

    @Test
    fun anUnreadableUriIsAFactualRetryableError() = runTest {
        val vm = viewModel(FakeDataControlDao())
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.BACKUP_RESTORE) { throw IOException("provider failed") }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ImportMessages.READ_FAILED, vm.uiState.value.importError)

        // Retrying with a good file works; nothing is latched.
        vm.importFileSelected(ImportKind.BACKUP_RESTORE, stream(backupText()))
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.pendingImport)
    }

    @Test
    fun cancellingThePreviewChangesNothing() = runTest {
        val data = FakeDataControlDao()
        val vm = viewModel(data)
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.BACKUP_RESTORE, stream(backupText()))
        dispatcher.scheduler.advanceUntilIdle()
        vm.cancelImport()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.pendingImport)
        assertEquals(listOf(1L), data.entries.map { it.id })
        assertEquals("", data.profile.displayName)
    }

    @Test
    fun noRawFileContentEverReachesSavedStateAndAPendingPreviewIsNotRestored() = runTest {
        val handle = SavedStateHandle()
        val data = FakeDataControlDao()
        val vm = viewModel(data, handle)
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.BACKUP_RESTORE, stream(backupText()))
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.pendingImport)

        handle.keys().forEach { key ->
            val value = handle.get<Any?>(key)
            assertTrue(
                "Saved state must hold primitives only, not $key = $value",
                value is String || value is Boolean
            )
            if (value is String) {
                assertFalse("Saved state must not contain file content", value.contains("mindscale-backup"))
                assertFalse("Saved state must not contain health data", value.contains("Ada L"))
                assertFalse(value.contains("restored event"))
            }
        }
        assertEquals(ImportKind.BACKUP_RESTORE.name, handle.get<String>("settings.import.kind"))
        assertEquals(true, handle.get<Boolean>("settings.import.previewPending"))

        // A rebuilt ViewModel discards the payload and asks for the file again.
        val rebuilt = viewModel(data, handle)
        dispatcher.scheduler.runCurrent()
        assertNull(rebuilt.uiState.value.pendingImport)
        assertEquals(ImportMessages.PREVIEW_LOST, rebuilt.uiState.value.importError)
        assertEquals(listOf(1L), data.entries.map { it.id })
    }

    @Test
    fun aFailedTransactionReportsFailureAndLeavesRecordsAlone() = runTest {
        val data = FakeDataControlDao()
        data.failInsert = true
        val vm = viewModel(data)
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.RECORDS_MERGE, stream(csvText()))
        dispatcher.scheduler.advanceUntilIdle()
        vm.confirmImport()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportMessages.IMPORT_FAILED, vm.uiState.value.importError)
        assertNull(vm.uiState.value.pendingImport)
        assertNull(vm.uiState.value.message)
        assertEquals(1, data.markers.size)
    }

    @Test
    fun recordsChangingWhileThePreviewIsOpenAbortsTheImport() = runTest {
        val data = FakeDataControlDao()
        val vm = viewModel(data)
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.RECORDS_MERGE, stream(csvText()))
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.pendingImport)

        // The same record is captured on the device while the preview is on screen.
        data.markers += Marker(id = 99, ts = Instant.parse("2026-08-05T10:00:00Z").toEpochMilli(), text = "added event")

        vm.confirmImport()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportMessages.RECORDS_CHANGED, vm.uiState.value.importError)
        assertEquals(2, data.markers.size)
    }

    @Test
    fun cancellingAfterConfirmingCannotMisreportACompletedMutation() = runTest {
        val data = FakeDataControlDao()
        val vm = viewModel(data)
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.RECORDS_MERGE, stream(csvText()))
        dispatcher.scheduler.advanceUntilIdle()

        // Confirm, then cancel while the transaction is still in flight.
        vm.confirmImport()
        vm.cancelImport()
        assertTrue("Cancel must not clear a confirmed import", vm.uiState.value.importing)
        assertNotNull(vm.uiState.value.pendingImport)

        dispatcher.scheduler.advanceUntilIdle()

        // The mutation completed, so the user is told exactly that — not that it was
        // cancelled while their records changed underneath them.
        assertEquals(ImportMessages.added(0, 0, 1, 0), vm.uiState.value.message)
        assertNull(vm.uiState.value.pendingImport)
        assertFalse(vm.uiState.value.importing)
        assertEquals(2, data.markers.size)
    }

    @Test
    fun aSecondImportCannotStartWhileOneIsAwaitingConfirmation() = runTest {
        val vm = viewModel(FakeDataControlDao())
        dispatcher.scheduler.runCurrent()

        vm.importFileSelected(ImportKind.BACKUP_RESTORE, stream(backupText()))
        dispatcher.scheduler.advanceUntilIdle()

        vm.requestRecordsImport()
        assertNull(vm.uiState.value.importLaunch)
        assertEquals(ImportKind.BACKUP_RESTORE, vm.uiState.value.pendingImport?.kind)
    }
}
