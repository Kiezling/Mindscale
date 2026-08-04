package com.kieslingdev.mindscale.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EpisodeSourceDao
import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.track.FakeTrackSettingsDao
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val hour = 3_600_000L

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun sourceSettingsAndRangeChanges_rederiveOneSnapshot() = runTest {
        val source = FakeEpisodeSourceDao()
        val settings = FakeTrackSettingsDao()
        val vm = viewModel(source, settings, now = { Instant.ofEpochMilli(12 * hour) })
        runCurrent()
        assertFalse(vm.uiState.value.snapshot!!.hasEntries)

        source.rows.value = listOf(entry(1, 0, 4))
        settings.setHourFormat(HourFormat.TWENTY_FOUR)
        vm.selectRange(InsightRange.ONE_DAY)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(InsightRange.ONE_DAY, state.range)
        assertEquals(HourFormat.TWENTY_FOUR, state.hourFormat)
        assertEquals(1, state.snapshot!!.summary.episodeCount)
        assertNull(state.error)
        vm.viewModelScope.cancel()
    }

    @Test
    fun savedRangeAndExploration_restoreAndClamp() = runTest {
        val handle = SavedStateHandle()
        val source = FakeEpisodeSourceDao(listOf(entry(1, 0, 5)))
        val settings = FakeTrackSettingsDao()
        val now = { Instant.ofEpochMilli(6 * hour) }
        var vm = viewModel(source, settings, handle, now)
        runCurrent()
        vm.selectRange(InsightRange.SEVEN_DAYS)
        runCurrent()
        vm.explore(2 * hour)
        vm.exploreChart(3 * hour)

        vm.viewModelScope.cancel()
        vm = viewModel(source, settings, handle, now)
        runCurrent()

        assertEquals(InsightRange.SEVEN_DAYS, vm.uiState.value.range)
        assertEquals(2 * hour, vm.uiState.value.exploredInstantMillis)
        assertEquals(3 * hour, vm.uiState.value.chartExploredInstantMillis)
        vm.viewModelScope.cancel()
    }

    @Test
    fun chartActionsNavigateRatingsAndEventsAndRangeClearsSelection() = runTest {
        val source = FakeEpisodeSourceDao(
            listOf(
                entry(1, 0, 4),
                marker(2, hour, "therapy"),
                entry(3, 2 * hour, 7),
                marker(4, 3 * hour, "dose")
            )
        )
        val vm = viewModel(source, FakeTrackSettingsDao(), now = { Instant.ofEpochMilli(6 * hour) })
        runCurrent()

        assertEquals(true, vm.moveChartRating(-1))
        assertEquals(2 * hour, vm.uiState.value.chartExploredInstantMillis)
        assertEquals(true, vm.moveChartMarker(-1))
        assertEquals(hour, vm.uiState.value.chartExploredInstantMillis)
        assertEquals(true, vm.moveChartMarker(1))
        assertEquals(3 * hour, vm.uiState.value.chartExploredInstantMillis)
        assertEquals(false, vm.moveChartMarker(1))
        assertEquals(true, vm.moveChartHour(-1))
        assertEquals(2 * hour, vm.uiState.value.chartExploredInstantMillis)

        vm.selectRange(InsightRange.ONE_DAY)
        assertNull(vm.uiState.value.chartExploredInstantMillis)
        vm.viewModelScope.cancel()
    }

    @Test
    fun hideNotesSettingFlowsToChartPrivacyState() = runTest {
        val settings = FakeTrackSettingsDao()
        val vm = viewModel(FakeEpisodeSourceDao(listOf(entry(1, 0, 5))), settings, now = { Instant.ofEpochMilli(hour) })
        runCurrent()
        assertFalse(vm.uiState.value.hideNotes)

        settings.setHideNotes(true)
        runCurrent()

        assertEquals(true, vm.uiState.value.hideNotes)
        vm.viewModelScope.cancel()
    }

    @Test
    fun scheduledHoldExpiry_recomputesOngoingEpisodeAsAssumed() = runTest {
        var nowMillis = 7 * hour
        val source = FakeEpisodeSourceDao(listOf(entry(1, 0, 5)))
        val settings = FakeTrackSettingsDao(TrackSettings(holdDuration = com.kieslingdev.mindscale.data.HoldDuration.EIGHT))
        val vm = viewModel(source, settings, now = { Instant.ofEpochMilli(nowMillis) })
        runCurrent()
        assertEquals(EpisodeEndReason.ONGOING, vm.uiState.value.snapshot!!.recentEpisodes.single().endReason)

        nowMillis = 8 * hour
        advanceTimeBy(hour)
        runCurrent()

        assertEquals(EpisodeEndReason.ASSUMED_HOLD, vm.uiState.value.snapshot!!.recentEpisodes.single().endReason)
        vm.viewModelScope.cancel()
    }

    @Test
    fun retryRestartsTerminalSourceCollectionAndRetainsLastSnapshotOnLaterError() = runTest {
        val source = FakeEpisodeSourceDao(listOf(entry(1, 0, 3)), failuresRemaining = 1)
        val vm = viewModel(source, FakeTrackSettingsDao(), now = { Instant.ofEpochMilli(hour) })
        runCurrent()
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.snapshot)

        vm.retry()
        runCurrent()
        assertEquals(2, source.observeCalls)
        assertNotNull(vm.uiState.value.snapshot)
        assertNull(vm.uiState.value.error)

        source.failuresRemaining = 1
        vm.retry()
        runCurrent()
        assertNotNull(vm.uiState.value.snapshot)
        assertNotNull(vm.uiState.value.error)
        vm.viewModelScope.cancel()
    }

    private fun viewModel(
        source: FakeEpisodeSourceDao,
        settings: FakeTrackSettingsDao,
        handle: SavedStateHandle = SavedStateHandle(),
        now: () -> Instant
    ) = InsightsViewModel(
        sourceDao = source,
        settingsDao = settings,
        savedStateHandle = handle,
        nowProvider = now,
        zoneProvider = { ZoneOffset.UTC },
        computationDispatcher = dispatcher
    )

    private fun entry(id: Long, ts: Long, value: Int) =
        EpisodeSourceRow("ENTRY", id, ts, null, value, emptyList())

    private fun marker(id: Long, ts: Long, text: String) =
        EpisodeSourceRow("MARKER", id, ts, null, null, null, text = text)
}

private class FakeEpisodeSourceDao(
    initial: List<EpisodeSourceRow> = emptyList(),
    var failuresRemaining: Int = 0
) : EpisodeSourceDao {
    val rows = MutableStateFlow(initial)
    var observeCalls = 0
    private var nextId = 1L

    override fun observeSource(): Flow<List<EpisodeSourceRow>> = flow {
        observeCalls++
        if (failuresRemaining > 0) {
            failuresRemaining--
            error("read failed")
        }
        rows.collect { emit(it) }
    }

    override suspend fun sourceAtOrBefore(ts: Long): List<EpisodeSourceRow> =
        rows.value.filter { it.ts <= ts }

    override suspend fun currentSettings(): TrackSettings = TrackSettings()

    override suspend fun insertEntry(entry: Entry): Long {
        val id = nextId++
        rows.value += EpisodeSourceRow(
            "ENTRY", id, entry.ts, null, entry.value, entry.chips, note = entry.note
        )
        return id
    }
}
