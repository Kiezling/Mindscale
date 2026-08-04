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
    val value: Int
)

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
