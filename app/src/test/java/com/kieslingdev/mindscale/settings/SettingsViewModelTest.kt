package com.kieslingdev.mindscale.settings

import androidx.lifecycle.SavedStateHandle
import com.kieslingdev.mindscale.data.DataControlDao
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.track.FakeTrackSettingsDao
import java.time.Instant
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun eraseCannotBeConfirmedUntilBackupWriteSucceeds() = runTest {
        val data = FakeDataControlDao()
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(),
            data,
            SavedStateHandle(),
            nowProvider = { Instant.parse("2026-08-03T12:00:00Z") }
        )
        dispatcher.scheduler.runCurrent()

        vm.confirmErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, data.eraseCalls)

        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        assertNotNull(vm.uiState.value.pendingDocument)
        assertNull(vm.uiState.value.eraseConfirmation)

        vm.documentPickerCanceled()
        vm.confirmErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, data.eraseCalls)

        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        vm.documentWriteSucceeded()
        assertNotNull(vm.uiState.value.eraseConfirmation)

        vm.confirmErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, data.eraseCalls)
        assertNull(vm.uiState.value.eraseConfirmation)
    }

    @Test
    fun anchorAndChipDraftsSurviveViewModelRecreation() = runTest {
        val handle = SavedStateHandle()
        val settings = FakeTrackSettingsDao()
        val data = FakeDataControlDao()
        var vm = SettingsViewModel(settings, data, handle)
        dispatcher.scheduler.runCurrent()
        vm.updateAnchor2("can't get moving")
        vm.updateChipDraft("work, poor sleep")

        vm = SettingsViewModel(settings, data, handle)
        dispatcher.scheduler.runCurrent()

        assertEquals("can't get moving", vm.uiState.value.anchorDraft.anchor2)
        assertEquals("work, poor sleep", vm.uiState.value.chipDraft)
    }

    @Test
    fun failedWriteRetainsEncodedDocumentForRetry() = runTest {
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(),
            FakeDataControlDao(),
            nowProvider = { Instant.parse("2026-08-03T12:00:00Z") }
        )
        dispatcher.scheduler.runCurrent()
        vm.requestBackup()
        dispatcher.scheduler.runCurrent()
        val original = vm.uiState.value.pendingDocument

        vm.documentWriteFailed()
        assertNull(vm.uiState.value.pendingDocument)
        assertEquals(original, vm.uiState.value.retryDocument)

        vm.retryDocumentWrite()
        assertEquals(original, vm.uiState.value.pendingDocument)
        assertNull(vm.uiState.value.retryDocument)
    }

    @Test
    fun holdDurationUsesTargetedWriteAndUpdatesObservedState() = runTest {
        val settings = FakeTrackSettingsDao()
        val vm = SettingsViewModel(settings, FakeDataControlDao())
        dispatcher.scheduler.runCurrent()

        vm.setHoldDuration(HoldDuration.TWENTY_FOUR)
        dispatcher.scheduler.runCurrent()

        assertEquals(HoldDuration.TWENTY_FOUR, vm.uiState.value.settings.holdDuration)
        assertEquals(HoldDuration.TWENTY_FOUR, settings.updateCalls.single().holdDuration)
    }
}

private class FakeDataControlDao : DataControlDao {
    private val entries = mutableListOf(Entry(id = 1, ts = 1_000, value = 4))
    private val sleeps = mutableListOf(SleepInterval(id = 2, startTs = 2_000, endTs = 3_000))
    private val markers = mutableListOf(Marker(id = 3, ts = 4_000, text = "event"))
    private var settings = TrackSettings()
    var eraseCalls = 0

    override suspend fun allEntries(): List<Entry> = entries.toList()
    override suspend fun allSleeps(): List<SleepInterval> = sleeps.toList()
    override suspend fun allMarkers(): List<Marker> = markers.toList()
    override suspend fun settings(): TrackSettings = settings
    override suspend fun deleteEntries(): Int = entries.size.also { entries.clear() }
    override suspend fun deleteSleeps(): Int = sleeps.size.also { sleeps.clear() }
    override suspend fun deleteMarkers(): Int = markers.size.also { markers.clear() }
    override suspend fun resetSettings(defaults: TrackSettings): Int {
        eraseCalls += 1
        settings = defaults
        return 1
    }
}
