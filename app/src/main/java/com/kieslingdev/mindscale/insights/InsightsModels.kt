package com.kieslingdev.mindscale.insights

import com.kieslingdev.mindscale.data.HourFormat
import java.time.LocalDate
import java.util.Locale

const val MIN_ONSET_COUNT = 6
const val MIN_ONSET_TIME_COUNT = 6

const val ONSET_GAP_CAVEAT =
    "Starts are assembled from your ratings using the current waking-hour limit. " +
        "Each gap is elapsed time from one start in this range to the next. " +
        "The bars do not identify a cycle, cause, or prediction."

const val ONSET_TIME_CAVEAT =
    "These are the local times when you recorded a start, not necessarily when it began. " +
        "Driving, meetings, and sleep can make recording happen later. " +
        "Historical starts use your device's current time zone."

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

data class OnsetGapBucket(
    val index: Int,
    val lowerBoundMillisInclusive: Long,
    val upperBoundMillisExclusive: Long?,
    val visibleLabel: String,
    val spokenBoundary: String,
    val count: Int
)

data class OnsetGapHistogram(
    val eligibleOnsetCount: Int,
    val gapCount: Int,
    val minimumOnsetCount: Int = MIN_ONSET_COUNT,
    val buckets: List<OnsetGapBucket>
) {
    val isEligible: Boolean
        get() = eligibleOnsetCount >= minimumOnsetCount
}

fun onsetGapRefusalText(histogram: OnsetGapHistogram): String {
    val needed = (histogram.minimumOnsetCount - histogram.eligibleOnsetCount).coerceAtLeast(0)
    val startWord = if (needed == 1) "start" else "starts"
    val gapWord = if (histogram.gapCount == 1) "gap" else "gaps"
    val prefix = "Needs $needed more recorded $startWord in this range before this chart is shown."
    return if (histogram.eligibleOnsetCount <= 1) {
        "$prefix There are ${histogram.gapCount} onset-to-onset $gapWord to count."
    } else {
        "$prefix These starts make ${histogram.gapCount} onset-to-onset $gapWord."
    }
}

fun onsetGapDenominator(histogram: OnsetGapHistogram): String {
    val gapWord = if (histogram.gapCount == 1) "gap" else "gaps"
    val startWord = if (histogram.eligibleOnsetCount == 1) "start" else "starts"
    return "${histogram.gapCount} onset-to-onset $gapWord from " +
        "${histogram.eligibleOnsetCount} recorded $startWord in this range."
}

fun onsetGapBucketReadout(histogram: OnsetGapHistogram, bucketIndex: Int): String? {
    val bucket = histogram.buckets.getOrNull(bucketIndex) ?: return null
    val gapWord = if (histogram.gapCount == 1) "gap" else "gaps"
    return "${bucket.count} of ${histogram.gapCount} onset-to-onset $gapWord were ${bucket.spokenBoundary}."
}

data class OnsetHourBucket(
    val hourOfDay: Int,
    val count: Int
)

data class OnsetTimeCounts(
    val eligibleOnsetCount: Int,
    val coveredLocalDayCount: Int,
    val minimumOnsetCount: Int = MIN_ONSET_TIME_COUNT,
    val buckets: List<OnsetHourBucket>,
    val fourHourWindowStartHour: Int,
    val fourHourWindowCount: Int
) {
    val isEligible: Boolean
        get() = eligibleOnsetCount >= minimumOnsetCount
}

fun onsetTimeRefusalText(counts: OnsetTimeCounts): String {
    val needed = (counts.minimumOnsetCount - counts.eligibleOnsetCount).coerceAtLeast(0)
    val neededWord = if (needed == 1) "start" else "starts"
    val countWord = if (counts.eligibleOnsetCount == 1) "start" else "starts"
    val countVerb = if (counts.eligibleOnsetCount == 1) "is" else "are"
    return "Needs $needed more recorded $neededWord in this range before this chart is shown. " +
        "There $countVerb ${counts.eligibleOnsetCount} $countWord to count by hour."
}

fun onsetTimeDenominator(counts: OnsetTimeCounts): String {
    val startWord = if (counts.eligibleOnsetCount == 1) "start" else "starts"
    val dayWord = if (counts.coveredLocalDayCount == 1) "day" else "days"
    return "${counts.eligibleOnsetCount} recorded $startWord across " +
        "${counts.coveredLocalDayCount} local calendar $dayWord in this range."
}

fun onsetHourVisibleLabel(hourOfDay: Int, hourFormat: HourFormat): String {
    require(hourOfDay in 0..23) { "Invalid onset hour $hourOfDay" }
    return if (hourFormat == HourFormat.TWENTY_FOUR) {
        String.format(Locale.ROOT, "%02d", hourOfDay)
    } else {
        "${hourOfDay.toTwelveHour()}${if (hourOfDay < 12) "a" else "p"}"
    }
}

fun onsetHourSpokenLabel(hourOfDay: Int, hourFormat: HourFormat): String {
    require(hourOfDay in 0..23) { "Invalid onset hour $hourOfDay" }
    return if (hourFormat == HourFormat.TWENTY_FOUR) {
        String.format(Locale.ROOT, "%02d:00", hourOfDay)
    } else {
        "${hourOfDay.toTwelveHour()}:00 ${if (hourOfDay < 12) "AM" else "PM"}"
    }
}

fun onsetHourBoundary(hourOfDay: Int, hourFormat: HourFormat): String =
    onsetClockWindowBoundary(hourOfDay, 1, hourFormat)

fun onsetTimeBucketReadout(
    counts: OnsetTimeCounts,
    hourOfDay: Int,
    hourFormat: HourFormat
): String? {
    val bucket = counts.buckets.firstOrNull { it.hourOfDay == hourOfDay } ?: return null
    val startWord = if (counts.eligibleOnsetCount == 1) "start" else "starts"
    return "${bucket.count} of ${counts.eligibleOnsetCount} recorded $startWord were recorded " +
        "${onsetHourBoundary(hourOfDay, hourFormat)}."
}

fun onsetTimeFourHourSentence(counts: OnsetTimeCounts, hourFormat: HourFormat): String =
    "${counts.fourHourWindowCount} of ${counts.eligibleOnsetCount} recorded starts in this range were recorded " +
        "${onsetClockWindowBoundary(counts.fourHourWindowStartHour, 4, hourFormat)}."

private fun onsetClockWindowBoundary(startHour: Int, durationHours: Int, hourFormat: HourFormat): String {
    require(startHour in 0..23) { "Invalid onset hour $startHour" }
    require(durationHours > 0) { "Duration must be positive" }
    val endHour = (startHour + durationHours) % 24
    return "from ${onsetHourSpokenLabel(startHour, hourFormat)} up to but not including " +
        onsetHourSpokenLabel(endHour, hourFormat)
}

private fun Int.toTwelveHour(): Int = (this % 12).takeIf { it != 0 } ?: 12

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
    val onsetGapHistogram: OnsetGapHistogram,
    val onsetTimeCounts: OnsetTimeCounts,
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
