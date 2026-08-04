package com.kieslingdev.mindscale.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.EpisodeSourceDao
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.TrackSettingsDao
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

private const val RANGE_KEY = "insights.range"
private const val EXPLORED_KEY = "insights.exploredInstant"
private const val CHART_EXPLORED_KEY = "insights.chartExploredInstant"
private const val ONSET_GAP_BUCKET_KEY = "insights.selectedOnsetGapBucket"
private const val ONSET_HOUR_KEY = "insights.selectedOnsetHour"
private const val HOUR_MILLIS = 3_600_000L

data class InsightsUiState(
    val range: InsightRange = InsightRange.THIRTY_DAYS,
    val loading: Boolean = true,
    val snapshot: InsightsSnapshot? = null,
    val exploredInstantMillis: Long? = null,
    val chartExploredInstantMillis: Long? = null,
    val selectedOnsetGapBucketIndex: Int? = null,
    val selectedOnsetHour: Int? = null,
    val hourFormat: HourFormat = HourFormat.TWELVE,
    val holdDuration: HoldDuration = HoldDuration.SIXTEEN,
    val hideNotes: Boolean = false,
    val error: String? = null
)

private sealed interface InsightsReadResult {
    data class Success(val rows: List<com.kieslingdev.mindscale.data.EpisodeSourceRow>, val settings: TrackSettings) :
        InsightsReadResult
    data class Failure(val error: Throwable) : InsightsReadResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val sourceDao: EpisodeSourceDao,
    private val settingsDao: TrackSettingsDao,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val nowProvider: () -> Instant = Instant::now,
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val initialRange = savedStateHandle.get<String>(RANGE_KEY)
        ?.let { runCatching { InsightRange.valueOf(it) }.getOrNull() }
        ?: InsightRange.THIRTY_DAYS
    private val rangeFlow = MutableStateFlow(initialRange)
    private val retryVersion = MutableStateFlow(0)
    private val timeVersion = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(
        InsightsUiState(
            range = initialRange,
            exploredInstantMillis = savedStateHandle[EXPLORED_KEY],
            chartExploredInstantMillis = savedStateHandle[CHART_EXPLORED_KEY],
            selectedOnsetGapBucketIndex = savedStateHandle.get<Int>(ONSET_GAP_BUCKET_KEY)
                ?.takeIf { it in 0 until 10 },
            selectedOnsetHour = savedStateHandle.get<Int>(ONSET_HOUR_KEY)
                ?.takeIf { it in 0..23 }
        )
    )
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    private var invalidationJob: Job? = null

    init {
        viewModelScope.launch {
            val reads = retryVersion.flatMapLatest {
                combine(sourceDao.observeSource(), settingsDao.observe()) { rows, settings ->
                    InsightsReadResult.Success(rows, settings) as InsightsReadResult
                }.catch { error -> emit(InsightsReadResult.Failure(error)) }
            }
            combine(reads, rangeFlow, timeVersion) { read, range, _ -> read to range }
                .collect { (read, range) ->
                    when (read) {
                        is InsightsReadResult.Failure -> {
                            invalidationJob?.cancel()
                            _uiState.update {
                                it.copy(
                                    loading = false,
                                    error = "Could not read Insights. Your records are still on this device."
                                )
                            }
                        }
                        is InsightsReadResult.Success -> derive(read, range)
                    }
                }
        }
    }

    private suspend fun derive(read: InsightsReadResult.Success, range: InsightRange) {
        try {
            val now = nowProvider()
            val snapshot = withContext(computationDispatcher) {
                deriveInsights(read.rows, read.settings.holdDuration, now, zoneProvider(), range)
            }
            val explored = _uiState.value.exploredInstantMillis
                ?.coerceIn(snapshot.rangeStartMillis, snapshot.nowMillis)
            val chartExplored = _uiState.value.chartExploredInstantMillis
                ?.coerceIn(snapshot.rangeStartMillis, snapshot.nowMillis)
            val selectedOnsetGapBucket = _uiState.value.selectedOnsetGapBucketIndex
                ?.takeIf { snapshot.onsetGapHistogram.isEligible }
                ?.takeIf { it in snapshot.onsetGapHistogram.buckets.indices }
            val selectedOnsetHour = _uiState.value.selectedOnsetHour
                ?.takeIf { snapshot.onsetTimeCounts.isEligible }
                ?.takeIf { hour -> snapshot.onsetTimeCounts.buckets.any { it.hourOfDay == hour } }
            if (explored == null) savedStateHandle.remove<Long>(EXPLORED_KEY)
            else savedStateHandle[EXPLORED_KEY] = explored
            if (chartExplored == null) savedStateHandle.remove<Long>(CHART_EXPLORED_KEY)
            else savedStateHandle[CHART_EXPLORED_KEY] = chartExplored
            if (selectedOnsetGapBucket == null) savedStateHandle.remove<Int>(ONSET_GAP_BUCKET_KEY)
            else savedStateHandle[ONSET_GAP_BUCKET_KEY] = selectedOnsetGapBucket
            if (selectedOnsetHour == null) savedStateHandle.remove<Int>(ONSET_HOUR_KEY)
            else savedStateHandle[ONSET_HOUR_KEY] = selectedOnsetHour
            _uiState.update {
                it.copy(
                    range = range,
                    loading = false,
                    snapshot = snapshot,
                    exploredInstantMillis = explored,
                    chartExploredInstantMillis = chartExplored,
                    selectedOnsetGapBucketIndex = selectedOnsetGapBucket,
                    selectedOnsetHour = selectedOnsetHour,
                    hourFormat = read.settings.hourFormat,
                    holdDuration = read.settings.holdDuration,
                    hideNotes = read.settings.hideNotes,
                    error = null
                )
            }
            scheduleInvalidation(snapshot.nextInvalidationMillis)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            invalidationJob?.cancel()
            _uiState.update {
                it.copy(
                    loading = false,
                    error = "Insights could not be derived from the stored records. Nothing was changed."
                )
            }
        }
    }

    private fun scheduleInvalidation(atMillis: Long?) {
        invalidationJob?.cancel()
        if (atMillis == null) return
        invalidationJob = viewModelScope.launch {
            delay(max(1L, atMillis - nowProvider().toEpochMilli()))
            timeVersion.update { it + 1 }
        }
    }

    fun selectRange(range: InsightRange) {
        savedStateHandle[RANGE_KEY] = range.name
        savedStateHandle.remove<Long>(EXPLORED_KEY)
        savedStateHandle.remove<Long>(CHART_EXPLORED_KEY)
        savedStateHandle.remove<Int>(ONSET_GAP_BUCKET_KEY)
        savedStateHandle.remove<Int>(ONSET_HOUR_KEY)
        _uiState.update {
            it.copy(
                range = range,
                exploredInstantMillis = null,
                chartExploredInstantMillis = null,
                selectedOnsetGapBucketIndex = null,
                selectedOnsetHour = null,
                loading = it.snapshot == null
            )
        }
        rangeFlow.value = range
    }

    fun explore(instantMillis: Long) {
        val snapshot = _uiState.value.snapshot ?: return
        val clamped = instantMillis.coerceIn(snapshot.rangeStartMillis, snapshot.nowMillis)
        savedStateHandle[EXPLORED_KEY] = clamped
        _uiState.update { it.copy(exploredInstantMillis = clamped) }
    }

    fun moveExplorationHour(delta: Int): Boolean {
        val snapshot = _uiState.value.snapshot ?: return false
        val current = _uiState.value.exploredInstantMillis ?: snapshot.nowMillis
        explore(current + delta * HOUR_MILLIS)
        return true
    }

    fun moveExplorationDay(delta: Int): Boolean {
        val snapshot = _uiState.value.snapshot ?: return false
        val zone = zoneProvider()
        val current = Instant.ofEpochMilli(_uiState.value.exploredInstantMillis ?: snapshot.nowMillis)
            .atZone(zone)
        explore(current.plusDays(delta.toLong()).toInstant().toEpochMilli())
        return true
    }

    fun exploreChart(instantMillis: Long) {
        val snapshot = _uiState.value.snapshot ?: return
        val clamped = instantMillis.coerceIn(snapshot.rangeStartMillis, snapshot.nowMillis)
        savedStateHandle[CHART_EXPLORED_KEY] = clamped
        _uiState.update { it.copy(chartExploredInstantMillis = clamped) }
    }

    fun moveChartHour(delta: Int): Boolean {
        val snapshot = _uiState.value.snapshot ?: return false
        val current = _uiState.value.chartExploredInstantMillis ?: snapshot.nowMillis
        exploreChart(current + delta * HOUR_MILLIS)
        return true
    }

    fun moveChartRating(delta: Int): Boolean = moveChartTarget(delta) { snapshot ->
        snapshot.entryChart.segments.mapNotNull(EntryChartSegment::sourceEntryMillis).distinct().sorted()
    }

    fun moveChartMarker(delta: Int): Boolean = moveChartTarget(delta) { snapshot ->
        snapshot.entryChart.markers.map(EntryChartMarker::atMillis).distinct().sorted()
    }

    fun selectOnsetGapBucket(index: Int) {
        val histogram = _uiState.value.snapshot?.onsetGapHistogram ?: return
        if (!histogram.isEligible || index !in histogram.buckets.indices) return
        savedStateHandle[ONSET_GAP_BUCKET_KEY] = index
        _uiState.update { it.copy(selectedOnsetGapBucketIndex = index) }
    }

    fun selectOnsetHour(hour: Int) {
        val counts = _uiState.value.snapshot?.onsetTimeCounts ?: return
        if (!counts.isEligible || counts.buckets.none { it.hourOfDay == hour }) return
        savedStateHandle[ONSET_HOUR_KEY] = hour
        _uiState.update { it.copy(selectedOnsetHour = hour) }
    }

    private fun moveChartTarget(
        delta: Int,
        targets: (InsightsSnapshot) -> List<Long>
    ): Boolean {
        val snapshot = _uiState.value.snapshot ?: return false
        val current = _uiState.value.chartExploredInstantMillis ?: snapshot.nowMillis
        val target = if (delta < 0) targets(snapshot).lastOrNull { it < current }
        else targets(snapshot).firstOrNull { it > current }
        target ?: return false
        exploreChart(target)
        return true
    }

    fun retry() {
        _uiState.update { it.copy(loading = it.snapshot == null, error = null) }
        retryVersion.update { it + 1 }
    }

    fun refreshTime() = timeVersion.update { it + 1 }
}
