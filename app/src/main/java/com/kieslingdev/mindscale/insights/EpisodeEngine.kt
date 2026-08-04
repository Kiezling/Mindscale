package com.kieslingdev.mindscale.insights

import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.HoldDuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val HOUR_MILLIS = 3_600_000L

private data class EntryFact(
    val id: Long,
    val ts: Long,
    val value: Int,
    val chips: List<String>
)

private data class SleepSpan(
    val start: Long,
    val end: Long,
    val open: Boolean
)

private data class EpisodeBuilder(
    val onset: Long,
    var end: Long,
    var peak: Int = 0,
    var awakeMillis: Long = 0L,
    var intensityHours: Double = 0.0,
    val chips: LinkedHashSet<String> = linkedSetOf(),
    val segments: MutableList<IntensitySegment> = mutableListOf()
)

fun isOnsetAt(rows: List<EpisodeSourceRow>, atMillis: Long, hold: HoldDuration): Boolean {
    val model = buildModel(rows, hold, atMillis)
    return model.episodes.none {
        it.endReason == EpisodeEndReason.ONGOING && it.onsetMillis <= atMillis
    }
}

fun deriveInsights(
    rows: List<EpisodeSourceRow>,
    hold: HoldDuration,
    now: Instant,
    zoneId: ZoneId,
    range: InsightRange
): InsightsSnapshot {
    val nowMillis = now.toEpochMilli()
    val model = buildModel(rows, hold, nowMillis)
    val today = now.atZone(zoneId).toLocalDate()
    val startDate = when (range) {
        InsightRange.ONE_DAY -> today
        InsightRange.THREE_DAYS -> today.minusDays(2)
        InsightRange.SEVEN_DAYS -> today.minusDays(6)
        InsightRange.THIRTY_DAYS -> today.minusDays(29)
        InsightRange.NINETY_DAYS -> today.minusDays(89)
        InsightRange.SIX_MONTHS -> today.minusMonths(6)
    }
    val rangeStart = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val intersecting = model.episodes.filter { episodeIntersects(it, rangeStart, nowMillis, model.sleeps) }
    val closed = intersecting.filter { it.endReason != EpisodeEndReason.ONGOING }
    val clear = clearDayStats(
        startDate = startDate,
        today = today,
        zoneId = zoneId,
        nowMillis = nowMillis,
        firstEntry = model.entries.firstOrNull()?.ts,
        sleeps = model.sleeps,
        segments = model.segments
    )
    val peakValues = intersecting.map { it.peak }
    val rangeAuc = intersecting.sumOf { episode ->
        episode.segments.sumOf { segment ->
            val start = max(segment.startMillis, rangeStart)
            val end = min(segment.endMillis, nowMillis)
            if (end <= start) 0.0
            else segment.value * awakeMillis(start, end, model.sleeps).toDouble() / HOUR_MILLIS
        }
    }
    val started = intersecting.count { it.onsetMillis >= rangeStart }
    val carried = intersecting.size - started
    val assumed = intersecting.count { it.endReason == EpisodeEndReason.ASSUMED_HOLD }
    val durations = closed.map { episode ->
        episode.segments.sumOf { segment ->
            val start = max(segment.startMillis, rangeStart)
            val end = min(segment.endMillis, nowMillis)
            awakeMillis(start, end, model.sleeps)
        }
    }
    val facts = buildList {
        add(
            InsightFact(
                text = "${intersecting.size} ${if (intersecting.size == 1) "episode" else "episodes"} touched this range.",
                detail = "$started began here${if (carried > 0) " · $carried carried in" else ""}"
            )
        )
        if (durations.isNotEmpty()) {
            add(
                InsightFact(
                    text = "Half lasted ${formatDuration(median(durations))} or less of waking time.",
                    detail = "shortest ${formatDuration(durations.min())} · longest ${formatDuration(durations.max())} · ${durations.size} closed"
                )
            )
        } else if (intersecting.isNotEmpty()) {
            add(InsightFact("Not enough closed episodes yet."))
        }
        add(
            InsightFact(
                text = "${clear.clearDays} of ${clear.eligibleDays} eligible days had no symptom time.",
                detail = if (clear.longestRun > 1) "longest clear stretch ${clear.longestRun} days" else null
            )
        )
        if (peakValues.isNotEmpty()) {
            add(
                InsightFact(
                    text = "Highest recorded was ${peakValues.max()}. Median episode peak was ${medianInt(peakValues)}.",
                    detail = "${peakValues.size} ${if (peakValues.size == 1) "episode" else "episodes"}"
                )
            )
        }
        add(
            InsightFact(
                text = "Total logged burden ${String.format(Locale.ROOT, "%.1f", rangeAuc)} intensity-hours.",
                detail = "intensity multiplied by awake hours in this range"
            )
        )
        if (assumed > 0) {
            add(
                InsightFact(
                    text = "$assumed ${if (assumed == 1) "episode has" else "episodes have"} an assumed ending.",
                    detail = "no later rating after ${hold.hours} waking hours"
                )
            )
        }
    }
    val raster = buildRaster(startDate, today, zoneId, nowMillis, model.entries.firstOrNull()?.ts, model.sleeps, model.segments)
    val nextMidnight = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val candidates = listOfNotNull(model.futureBoundary, model.nextHoldExpiry, nextMidnight)
        .filter { it > nowMillis }
    return InsightsSnapshot(
        rangeStartMillis = rangeStart,
        nowMillis = nowMillis,
        hasEntries = model.entries.isNotEmpty(),
        hasRangeData = intersecting.isNotEmpty() || model.entries.any { it.ts >= rangeStart },
        summary = InsightSummary(
            episodeCount = intersecting.size,
            typicalLengthMillis = durations.takeIf { it.isNotEmpty() }?.let(::median),
            clearDays = clear.clearDays,
            eligibleDays = clear.eligibleDays,
            peak = peakValues.maxOrNull()
        ),
        facts = facts,
        recentEpisodes = intersecting.sortedByDescending { it.onsetMillis }.take(8),
        rasterDays = raster,
        nextInvalidationMillis = candidates.minOrNull()
    )
}

private data class BuiltModel(
    val entries: List<EntryFact>,
    val sleeps: List<SleepSpan>,
    val episodes: List<DerivedEpisode>,
    val segments: List<IntensitySegment>,
    val futureBoundary: Long?,
    val nextHoldExpiry: Long?
)

private fun buildModel(rows: List<EpisodeSourceRow>, hold: HoldDuration, nowMillis: Long): BuiltModel {
    val futureBoundary = rows.asSequence()
        .flatMap { sequenceOf(it.ts, it.endTs).filterNotNull() }
        .filter { it > nowMillis }
        .minOrNull()
    val entries = rows.filter { it.recordType == "ENTRY" && it.ts <= nowMillis }.map { row ->
        val value = requireNotNull(row.value) { "Entry ${row.id} has no value" }
        require(value in 0..10) { "Entry ${row.id} has invalid value $value" }
        EntryFact(row.id, row.ts, value, row.chips.orEmpty())
    }.groupBy { it.ts }.values.map { sameTime -> sameTime.maxBy { it.id } }.sortedWith(
        compareBy<EntryFact> { it.ts }.thenBy { it.id }
    )
    rows.filter { it.recordType != "ENTRY" && it.recordType != "SLEEP" }
        .firstOrNull()?.let { error("Unknown episode source ${it.recordType}") }
    val sleeps = normalizeSleeps(rows.filter { it.recordType == "SLEEP" }, nowMillis)
    val sleepActiveNow = rows.any {
        it.recordType == "SLEEP" && it.ts <= nowMillis && (it.endTs == null || it.endTs > nowMillis)
    }
    val episodes = mutableListOf<DerivedEpisode>()
    var current: EpisodeBuilder? = null
    var nextHoldExpiry: Long? = null
    val holdMillis = hold.hours * HOUR_MILLIS
    entries.forEachIndexed { index, entry ->
        if (entry.value == 0) return@forEachIndexed
        val next = entries.getOrNull(index + 1)
        val nextTs = next?.ts ?: nowMillis
        val holdEnd = addAwake(entry.ts, holdMillis, sleeps)
        val segmentEnd = min(nowMillis, min(nextTs, holdEnd))
        val builder = current ?: EpisodeBuilder(onset = entry.ts, end = entry.ts).also { current = it }
        builder.peak = max(builder.peak, entry.value)
        builder.chips.addAll(entry.chips)
        if (segmentEnd > entry.ts) {
            val awake = awakeMillis(entry.ts, segmentEnd, sleeps)
            builder.awakeMillis += awake
            builder.intensityHours += entry.value * awake.toDouble() / HOUR_MILLIS
            builder.segments += IntensitySegment(entry.ts, segmentEnd, entry.value)
        }
        builder.end = segmentEnd
        when {
            next != null && holdEnd < next.ts -> {
                episodes += builder.finish(holdEnd, EpisodeEndReason.ASSUMED_HOLD, sleeps)
                current = null
            }
            next != null && next.value == 0 -> {
                episodes += builder.finish(next.ts, EpisodeEndReason.EXPLICIT_ZERO, sleeps)
                current = null
            }
            next == null && holdEnd <= nowMillis -> {
                episodes += builder.finish(holdEnd, EpisodeEndReason.ASSUMED_HOLD, sleeps)
                current = null
            }
            next == null -> {
                episodes += builder.finish(nowMillis, EpisodeEndReason.ONGOING, sleeps)
                if (!sleepActiveNow) nextHoldExpiry = holdEnd
                current = null
            }
        }
    }
    val allSegments = episodes.flatMap { it.segments }.sortedBy { it.startMillis }
    return BuiltModel(entries, sleeps, episodes, allSegments, futureBoundary, nextHoldExpiry)
}

private fun EpisodeBuilder.finish(endMillis: Long, reason: EpisodeEndReason, sleeps: List<SleepSpan>): DerivedEpisode {
    val resolvedEnd = max(onset, endMillis)
    return DerivedEpisode(
        onsetMillis = onset,
        endMillis = resolvedEnd,
        endReason = reason,
        peak = peak,
        awakeDurationMillis = awakeMillis,
        intensityHours = intensityHours,
        sleepCount = sleeps.count { overlapMillis(onset, resolvedEnd, it.start, it.end) > 0L },
        chips = chips.toList(),
        segments = segments.toList()
    )
}

private fun normalizeSleeps(rows: List<EpisodeSourceRow>, nowMillis: Long): List<SleepSpan> {
    val raw = rows.mapNotNull { row ->
        if (row.ts > nowMillis) return@mapNotNull null
        val rawEnd = row.endTs
        if (rawEnd != null) require(rawEnd > row.ts) { "Sleep ${row.id} ends before it starts" }
        val end = min(rawEnd ?: nowMillis, nowMillis)
        if (end <= row.ts) null else SleepSpan(row.ts, end, rawEnd == null)
    }.sortedBy { it.start }
    val merged = mutableListOf<SleepSpan>()
    raw.forEach { span ->
        val last = merged.lastOrNull()
        if (last != null && span.start <= last.end) {
            merged[merged.lastIndex] = SleepSpan(last.start, max(last.end, span.end), last.open || span.open)
        } else merged += span
    }
    return merged
}

private fun addAwake(start: Long, wantedMillis: Long, sleeps: List<SleepSpan>): Long {
    var cursor = start
    var remaining = wantedMillis
    sleeps.forEach { sleep ->
        if (sleep.end <= cursor) return@forEach
        if (sleep.start > cursor) {
            val awakeGap = sleep.start - cursor
            if (awakeGap >= remaining) return cursor + remaining
            remaining -= awakeGap
        }
        cursor = max(cursor, sleep.end)
    }
    return cursor + remaining
}

private fun awakeMillis(start: Long, end: Long, sleeps: List<SleepSpan>): Long {
    if (end <= start) return 0L
    var result = end - start
    overlappingSleeps(sleeps, start, end).forEach { sleep ->
        result -= overlapMillis(start, end, sleep.start, sleep.end)
    }
    return max(0L, result)
}

private fun overlapMillis(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long =
    max(0L, min(aEnd, bEnd) - max(aStart, bStart))

private fun episodeIntersects(
    episode: DerivedEpisode,
    rangeStart: Long,
    nowMillis: Long,
    sleeps: List<SleepSpan>
): Boolean = episode.segments.any {
    val start = max(it.startMillis, rangeStart)
    val end = min(it.endMillis, nowMillis)
    awakeMillis(start, end, sleeps) > 0L
} || (episode.endReason == EpisodeEndReason.ONGOING && episode.onsetMillis in rangeStart..nowMillis)

private data class ClearStats(val clearDays: Int, val eligibleDays: Int, val longestRun: Int)

private fun clearDayStats(
    startDate: LocalDate,
    today: LocalDate,
    zoneId: ZoneId,
    nowMillis: Long,
    firstEntry: Long?,
    sleeps: List<SleepSpan>,
    segments: List<IntensitySegment>
): ClearStats {
    if (firstEntry == null) return ClearStats(0, 0, 0)
    var date = startDate
    var clear = 0
    var eligible = 0
    var currentRun = 0
    var longest = 0
    while (!date.isAfter(today)) {
        val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val start = max(dayStart, firstEntry)
        val end = min(dayEnd, nowMillis)
        val awake = awakeMillis(start, end, sleeps)
        if (end > start && awake > 0L) {
            eligible++
            val positive = segments.any { segment ->
                val overlapStart = max(start, segment.startMillis)
                val overlapEnd = min(end, segment.endMillis)
                awakeMillis(overlapStart, overlapEnd, sleeps) > 0L
            }
            if (!positive) {
                clear++
                currentRun++
                longest = max(longest, currentRun)
            } else currentRun = 0
        } else currentRun = 0
        date = date.plusDays(1)
    }
    return ClearStats(clear, eligible, longest)
}

private fun buildRaster(
    startDate: LocalDate,
    today: LocalDate,
    zoneId: ZoneId,
    nowMillis: Long,
    firstEntry: Long?,
    sleeps: List<SleepSpan>,
    intensitySegments: List<IntensitySegment>
): List<RasterDay> {
    val result = mutableListOf<RasterDay>()
    var date = startDate
    while (!date.isAfter(today)) {
        val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val boundaries = sortedSetOf(dayStart, dayEnd)
        if (nowMillis in (dayStart + 1) until dayEnd) boundaries += nowMillis
        if (firstEntry != null && firstEntry in (dayStart + 1) until dayEnd) boundaries += firstEntry
        overlappingSleeps(sleeps, dayStart, dayEnd).forEach {
            boundaries += max(dayStart, it.start)
            boundaries += min(dayEnd, it.end)
        }
        overlappingIntensity(intensitySegments, dayStart, dayEnd).forEach {
            boundaries += max(dayStart, it.startMillis)
            boundaries += min(dayEnd, it.endMillis)
        }
        val duration = (dayEnd - dayStart).toDouble()
        val rawSegments = boundaries.zipWithNext().mapNotNull { (start, end) ->
            if (end <= start) return@mapNotNull null
            val midpoint = start + (end - start) / 2
            val (state, value) = when {
                midpoint >= nowMillis -> RasterState.FUTURE to null
                firstEntry == null || midpoint < firstEntry -> RasterState.NO_DATA to null
                sleepAt(sleeps, midpoint) != null -> RasterState.ASLEEP to null
                else -> intensityAt(intensitySegments, midpoint)?.let { RasterState.INTENSITY to it.value }
                    ?: (RasterState.WELL to null)
            }
            RasterSegment(
                startMillis = start,
                endMillis = end,
                startFraction = ((start - dayStart) / duration).toFloat(),
                endFraction = ((end - dayStart) / duration).toFloat(),
                state = state,
                intensity = value
            )
        }
        val merged = mutableListOf<RasterSegment>()
        rawSegments.forEach { segment ->
            val last = merged.lastOrNull()
            if (last != null && last.state == segment.state && last.intensity == segment.intensity) {
                merged[merged.lastIndex] = last.copy(
                    endMillis = segment.endMillis,
                    endFraction = segment.endFraction
                )
            } else merged += segment
        }
        result += RasterDay(date, dayStart, dayEnd, merged)
        date = date.plusDays(1)
    }
    return result
}

private fun sleepAt(sleeps: List<SleepSpan>, instant: Long): SleepSpan? {
    var low = 0
    var high = sleeps.lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        val span = sleeps[mid]
        when {
            instant < span.start -> high = mid - 1
            instant >= span.end -> low = mid + 1
            else -> return span
        }
    }
    return null
}

private fun intensityAt(segments: List<IntensitySegment>, instant: Long): IntensitySegment? {
    var low = 0
    var high = segments.lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        val segment = segments[mid]
        when {
            instant < segment.startMillis -> high = mid - 1
            instant >= segment.endMillis -> low = mid + 1
            else -> return segment
        }
    }
    return null
}

private fun overlappingSleeps(sleeps: List<SleepSpan>, start: Long, end: Long): Sequence<SleepSpan> =
    sleeps.asSequence().dropWhile { it.end <= start }.takeWhile { it.start < end }

private fun overlappingIntensity(
    segments: List<IntensitySegment>,
    start: Long,
    end: Long
): Sequence<IntensitySegment> = segments.asSequence()
    .dropWhile { it.endMillis <= start }
    .takeWhile { it.startMillis < end }

fun rangeStart(range: InsightRange, now: Instant, zoneId: ZoneId): Instant {
    val today = now.atZone(zoneId).toLocalDate()
    val date = when (range) {
        InsightRange.ONE_DAY -> today
        InsightRange.THREE_DAYS -> today.minusDays(2)
        InsightRange.SEVEN_DAYS -> today.minusDays(6)
        InsightRange.THIRTY_DAYS -> today.minusDays(29)
        InsightRange.NINETY_DAYS -> today.minusDays(89)
        InsightRange.SIX_MONTHS -> today.minusMonths(6)
    }
    return date.atStartOfDay(zoneId).toInstant()
}

fun formatDuration(millis: Long): String {
    val minutes = max(1L, (millis + 30_000L) / 60_000L)
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        remainder == 0L -> "${hours}h"
        else -> "${hours}h ${remainder}m"
    }
}

private fun median(values: List<Long>): Long {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle]
    else (sorted[middle - 1] / 2L) + (sorted[middle] / 2L) +
        ((sorted[middle - 1] % 2L + sorted[middle] % 2L) / 2L)
}

private fun medianInt(values: List<Int>): String {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle].toString()
    else String.format(Locale.ROOT, "%.1f", (sorted[middle - 1] + sorted[middle]) / 2.0)
}
