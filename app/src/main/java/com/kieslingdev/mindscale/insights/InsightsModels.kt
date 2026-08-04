package com.kieslingdev.mindscale.insights

import java.time.LocalDate

enum class InsightRange(val shortLabel: String, val spokenLabel: String) {
    ONE_DAY("1D", "1 day"),
    THREE_DAYS("3D", "3 days"),
    SEVEN_DAYS("7D", "7 days"),
    THIRTY_DAYS("30D", "30 days"),
    NINETY_DAYS("90D", "90 days"),
    SIX_MONTHS("6M", "6 months")
}

enum class EpisodeEndReason { EXPLICIT_ZERO, ASSUMED_HOLD, ONGOING }

data class IntensitySegment(
    val startMillis: Long,
    val endMillis: Long,
    val value: Int,
    val sourceEntryId: Long? = null,
    val sourceEntryMillis: Long? = null,
    val chips: List<String> = emptyList(),
    val note: String? = null
)

data class EntryChartSegment(
    val startMillis: Long,
    val endMillis: Long,
    val value: Int,
    val sourceEntryId: Long?,
    val sourceEntryMillis: Long?,
    val chips: List<String>,
    val note: String?
)

data class EntryChartSleep(val startMillis: Long, val endMillis: Long)

data class EntryChartMarker(val id: Long, val atMillis: Long, val text: String)

enum class EntryChartState { NO_DATA, WELL, INTENSITY, ASLEEP }

data class EntryChartReading(
    val state: EntryChartState,
    val value: Int? = null,
    val sourceEntryMillis: Long? = null,
    val chips: List<String> = emptyList(),
    val note: String? = null,
    val markers: List<EntryChartMarker> = emptyList()
)

data class EntryChart(
    val startMillis: Long,
    val endMillis: Long,
    val firstEntryMillis: Long?,
    val segments: List<EntryChartSegment>,
    val sleeps: List<EntryChartSleep>,
    val markers: List<EntryChartMarker>
) {
    fun readingAt(instantMillis: Long): EntryChartReading {
        val lastReadable = (endMillis - 1L).coerceAtLeast(startMillis)
        val instant = instantMillis.coerceIn(startMillis, lastReadable)
        val exactMarkers = markers.filter { it.atMillis == instant }
        if (firstEntryMillis == null || instant < firstEntryMillis) {
            return EntryChartReading(EntryChartState.NO_DATA, markers = exactMarkers)
        }
        if (sleeps.any { instant in it.startMillis until it.endMillis }) {
            return EntryChartReading(EntryChartState.ASLEEP, markers = exactMarkers)
        }
        val segment = segments.firstOrNull { instant in it.startMillis until it.endMillis }
            ?: return EntryChartReading(EntryChartState.WELL, markers = exactMarkers)
        return EntryChartReading(
            state = if (segment.value > 0) EntryChartState.INTENSITY else EntryChartState.WELL,
            value = segment.value.takeIf { it > 0 },
            sourceEntryMillis = segment.sourceEntryMillis,
            chips = segment.chips,
            note = segment.note,
            markers = exactMarkers
        )
    }
}

data class DerivedEpisode(
    val onsetMillis: Long,
    val endMillis: Long,
    val endReason: EpisodeEndReason,
    val peak: Int,
    val awakeDurationMillis: Long,
    val intensityHours: Double,
    val sleepCount: Int,
    val chips: List<String>,
    val segments: List<IntensitySegment>
)

enum class RasterState { NO_DATA, WELL, INTENSITY, ASLEEP, FUTURE }

data class RasterSegment(
    val startMillis: Long,
    val endMillis: Long,
    val startFraction: Float,
    val endFraction: Float,
    val state: RasterState,
    val intensity: Int? = null
)

data class RasterDay(
    val date: LocalDate,
    val startMillis: Long,
    val endMillis: Long,
    val segments: List<RasterSegment>
)

data class InsightFact(val text: String, val detail: String? = null)

data class InsightSummary(
    val episodeCount: Int,
    val typicalLengthMillis: Long?,
    val clearDays: Int,
    val eligibleDays: Int,
    val peak: Int?
)

data class InsightsSnapshot(
    val rangeStartMillis: Long,
    val nowMillis: Long,
    val hasEntries: Boolean,
    val hasRangeData: Boolean,
    val summary: InsightSummary,
    val facts: List<InsightFact>,
    val recentEpisodes: List<DerivedEpisode>,
    val rasterDays: List<RasterDay>,
    val entryChart: EntryChart,
    val nextInvalidationMillis: Long?
) {
    fun rasterStateAt(instantMillis: Long): Pair<RasterState, Int?> {
        val day = rasterDays.firstOrNull { instantMillis in it.startMillis until it.endMillis }
            ?: return RasterState.NO_DATA to null
        val segment = day.segments.firstOrNull { instantMillis in it.startMillis until it.endMillis }
            ?: return RasterState.FUTURE to null
        return segment.state to segment.intensity
    }
}
