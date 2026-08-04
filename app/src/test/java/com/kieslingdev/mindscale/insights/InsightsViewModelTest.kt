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
    fun onsetGapBucketSelectionRestoresClearsOnRangeAndRejectsSparseData() = runTest {
        val handle = SavedStateHandle()
        val source = FakeEpisodeSourceDao(episodeRows(listOf(0L, 2 * hour, 4 * hour, 6 * hour, 8 * hour, 10 * hour)))
        val now = { Instant.ofEpochMilli(12 * hour) }
        var vm = viewModel(source, FakeTrackSettingsDao(), handle, now)
        runCurrent()

        vm.selectOnsetGapBucket(3)
        assertEquals(3, vm.uiState.value.selectedOnsetGapBucketIndex)

        vm.viewModelScope.cancel()
        vm = viewModel(source, FakeTrackSettingsDao(), handle, now)
        runCurrent()
        assertEquals(3, vm.uiState.value.selectedOnsetGapBucketIndex)

        vm.selectOnsetGapBucket(10)
        assertEquals(3, vm.uiState.value.selectedOnsetGapBucketIndex)

        vm.selectRange(InsightRange.ONE_DAY)
        assertNull(vm.uiState.value.selectedOnsetGapBucketIndex)
        vm.viewModelScope.cancel()

        val sparse = viewModel(
            FakeEpisodeSourceDao(episodeRows(listOf(0L, 2 * hour, 4 * hour))),
            FakeTrackSettingsDao(),
            SavedStateHandle(mapOf("insights.selectedOnsetGapBucket" to 2)),
            now
        )
        runCurrent()
        assertNull(sparse.uiState.value.selectedOnsetGapBucketIndex)
        sparse.selectOnsetGapBucket(2)
        assertNull(sparse.uiState.value.selectedOnsetGapBucketIndex)
        sparse.viewModelScope.cancel()
    }

    @Test
    fun onsetGapSelectionSurvivesCountRefreshButClearsWhenEligibilityFalls() = runTest {
        val source = FakeEpisodeSourceDao(episodeRows(listOf(0L, 2 * hour, 4 * hour, 6 * hour, 8 * hour, 10 * hour)))
        val vm = viewModel(source, FakeTrackSettingsDao(), now = { Instant.ofEpochMilli(12 * hour) })
        runCurrent()
        vm.selectOnsetGapBucket(0)

        source.rows.value = episodeRows(listOf(0L, hour, 3 * hour, 5 * hour, 7 * hour, 9 * hour, 11 * hour))
        runCurrent()
        assertEquals(0, vm.uiState.value.selectedOnsetGapBucketIndex)
        assertEquals(6, vm.uiState.value.snapshot!!.onsetGapHistogram.buckets[0].count)

        source.rows.value = episodeRows(listOf(0L, 2 * hour, 4 * hour))
        runCurrent()
        assertNull(vm.uiState.value.selectedOnsetGapBucketIndex)
        vm.viewModelScope.cancel()
    }

    @Test
    fun onsetHourSelectionRestoresRefreshesAndClearsIndependently() = runTest {
        val handle = SavedStateHandle()
        val source = FakeEpisodeSourceDao(
            episodeRows(listOf(0L, 2 * hour, 4 * hour, 6 * hour, 8 * hour, 10 * hour))
        )
        val now = { Instant.ofEpochMilli(12 * hour) }
        var vm = viewModel(source, FakeTrackSettingsDao(), handle, now)
        runCurrent()

        vm.selectOnsetGapBucket(0)
        vm.selectOnsetHour(4)
        assertEquals(0, vm.uiState.value.selectedOnsetGapBucketIndex)
        assertEquals(4, vm.uiState.value.selectedOnsetHour)

        vm.viewModelScope.cancel()
        vm = viewModel(source, FakeTrackSettingsDao(), handle, now)
        runCurrent()
        assertEquals(4, vm.uiState.value.selectedOnsetHour)

        source.rows.value = episodeRows(listOf(hour, 2 * hour, 3 * hour, 4 * hour, 5 * hour, 6 * hour, 7 * hour))
        runCurrent()
        assertEquals(4, vm.uiState.value.selectedOnsetHour)
        assertEquals(1, vm.uiState.value.snapshot!!.onsetTimeCounts.buckets[4].count)

        vm.selectOnsetHour(24)
        assertEquals(4, vm.uiState.value.selectedOnsetHour)

        source.rows.value = episodeRows(listOf(0L, 2 * hour, 4 * hour))
        runCurrent()
        assertNull(vm.uiState.value.selectedOnsetHour)
        vm.viewModelScope.cancel()
    }

    @Test
    fun rangeChangeClearsOnsetHourAndInvalidRestoredHourIsDiscarded() = runTest {
        val rows = episodeRows(listOf(0L, 2 * hour, 4 * hour, 6 * hour, 8 * hour, 10 * hour))
        val handle = SavedStateHandle(mapOf("insights.selectedOnsetHour" to 23))
        val vm = viewModel(FakeEpisodeSourceDao(rows), FakeTrackSettingsDao(), handle, now = { Instant.ofEpochMilli(12 * hour) })
        runCurrent()
        assertEquals(23, vm.uiState.value.selectedOnsetHour)

        vm.selectRange(InsightRange.ONE_DAY)
        assertNull(vm.uiState.value.selectedOnsetHour)
        vm.viewModelScope.cancel()

        val invalid = viewModel(
            FakeEpisodeSourceDao(rows),
            FakeTrackSettingsDao(),
            SavedStateHandle(mapOf("insights.selectedOnsetHour" to 99)),
            now = { Instant.ofEpochMilli(12 * hour) }
        )
        runCurrent()
        assertNull(invalid.uiState.value.selectedOnsetHour)
        invalid.viewModelScope.cancel()
    }

    @Test
    fun sleepCategorySelectionRestoresRefreshesAndClearsIndependently() = runTest {
        val handle = SavedStateHandle()
        val source = FakeEpisodeSourceDao(
            episodeRows(listOf(0L, 2 * hour, 4 * hour, 6 * hour, 8 * hour, 10 * hour)) +
                sleep(100, 0, 4 * hour)
        )
        val now = { Instant.ofEpochMilli(12 * hour) }
        var vm = viewModel(source, FakeTrackSettingsDao(), handle, now)
        runCurrent()

        vm.selectOnsetHour(4)
        vm.selectSleepCategory(1)
        assertEquals(4, vm.uiState.value.selectedOnsetHour)
        assertEquals(1, vm.uiState.value.selectedSleepCategoryIndex)

        vm.viewModelScope.cancel()
        vm = viewModel(source, FakeTrackSettingsDao(), handle, now)
        runCurrent()
        assertEquals(1, vm.uiState.value.selectedSleepCategoryIndex)
        assertEquals(4, vm.uiState.value.selectedOnsetHour)

        source.rows.value = episodeRows(listOf(0L, 2 * hour, 4 * hour, 6 * hour, 8 * hour, 10 * hour)) +
            listOf(sleep(100, 0, 2 * hour), sleep(101, 5 * hour, 9 * hour))
        runCurrent()
        assertEquals(1, vm.uiState.value.selectedSleepCategoryIndex)
        assertEquals(2, vm.uiState.value.snapshot!!.sleepCounts.completedCount)

        vm.selectSleepCategory(2)
        assertEquals(1, vm.uiState.value.selectedSleepCategoryIndex)
        source.rows.value = episodeRows(listOf(0L, 2 * hour, 4 * hour, 6 * hour, 8 * hour, 10 * hour))
        runCurrent()
        assertNull(vm.uiState.value.selectedSleepCategoryIndex)
        assertEquals(4, vm.uiState.value.selectedOnsetHour)
        vm.viewModelScope.cancel()
    }

    @Test
    fun rangeClearsSleepSelectionAndInvalidRestoredCategoryIsDiscarded() = runTest {
        val rows = listOf(entry(1, 0, 0), sleep(2, 0, 4 * hour))
        val handle = SavedStateHandle(mapOf("insights.selectedSleepCategory" to 0))
        val vm = viewModel(FakeEpisodeSourceDao(rows), FakeTrackSettingsDao(), handle, now = { Instant.ofEpochMilli(8 * hour) })
        runCurrent()
        assertEquals(0, vm.uiState.value.selectedSleepCategoryIndex)

        vm.selectRange(InsightRange.ONE_DAY)
        assertNull(vm.uiState.value.selectedSleepCategoryIndex)
        vm.viewModelScope.cancel()

        val invalid = viewModel(
            FakeEpisodeSourceDao(rows),
            FakeTrackSettingsDao(),
            SavedStateHandle(mapOf("insights.selectedSleepCategory" to 7)),
            now = { Instant.ofEpochMilli(8 * hour) }
        )
        runCurrent()
        assertNull(invalid.uiState.value.selectedSleepCategoryIndex)
        invalid.viewModelScope.cancel()
    }

    @Test
    fun scheduledFutureSleepEndRecomputesIncompleteAsCompleted() = runTest {
        var nowMillis = 4 * hour
        val source = FakeEpisodeSourceDao(listOf(entry(1, 0, 0), sleep(2, 0, 5 * hour)))
        val vm = viewModel(source, FakeTrackSettingsDao(), now = { Instant.ofEpochMilli(nowMillis) })
        runCurrent()
        assertEquals(1, vm.uiState.value.snapshot!!.sleepCounts.incompleteCount)
        assertEquals(0, vm.uiState.value.snapshot!!.sleepCounts.completedCount)

        nowMillis = 5 * hour + 1
        advanceTimeBy(hour)
        runCurrent()

        assertEquals(0, vm.uiState.value.snapshot!!.sleepCounts.incompleteCount)
        assertEquals(1, vm.uiState.value.snapshot!!.sleepCounts.completedCount)
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

    private fun sleep(id: Long, start: Long, end: Long?) =
        EpisodeSourceRow("SLEEP", id, start, end, null, null)

    private fun episodeRows(onsets: List<Long>): List<EpisodeSourceRow> = buildList {
        var id = 1L
        onsets.forEach { onset ->
            add(entry(id++, onset, 5))
            add(entry(id++, onset + hour / 2, 0))
        }
    }
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
