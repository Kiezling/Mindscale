package com.kieslingdev.mindscale.report

import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.insights.InsightRange
import com.kieslingdev.mindscale.insights.SleepCategory
import com.kieslingdev.mindscale.insights.deriveInsights
import com.kieslingdev.mindscale.insights.formatDuration
import com.kieslingdev.mindscale.insights.onsetTimeFourHourSentence
import com.kieslingdev.mindscale.insights.sleepIncompleteText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_REPORT_MARKERS = 6
private const val MAX_REPORT_SCORES = 4
private const val MAX_REPORT_RATINGS = 6

private val ReportFilenameFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

data class ClinicianReport(
    val text: String,
    val range: InsightRange,
    val generatedAt: Instant,
    val zoneId: ZoneId,
    val presentation: ReportPresentation
)

data class ReportMetric(val label: String, val value: String)
data class ReportRating(val time: String, val value: Int)
data class ReportPresentation(
    val rangeText: String,
    val name: String?,
    val metrics: List<ReportMetric>,
    val ratingCount: Int,
    val ratingDays: Int,
    val ratings: List<ReportRating>,
    val events: List<String>,
    val omittedEvents: Int,
    val episodeDetail: String,
    val onsetDetail: String,
    val sleepDetail: String,
    val scores: List<String>,
    val omittedScores: Int,
    val context: String
)

fun clinicianReportFilename(at: Instant): String =
    "mindscale-clinician-summary-${ReportFilenameFormatter.format(at)}.txt"

fun buildClinicianReport(
    source: DataSnapshot,
    range: InsightRange,
    generatedAt: Instant,
    zoneId: ZoneId
): ClinicianReport {
    val rows = buildList {
        source.entries.forEach { entry ->
            add(
                EpisodeSourceRow(
                    recordType = "ENTRY",
                    id = entry.id,
                    ts = entry.ts,
                    endTs = null,
                    value = entry.value,
                    chips = entry.chips,
                    note = entry.note
                )
            )
        }
        source.sleeps.forEach { sleep ->
            add(
                EpisodeSourceRow(
                    recordType = "SLEEP",
                    id = sleep.id,
                    ts = sleep.startTs,
                    endTs = sleep.endTs,
                    value = null,
                    chips = null
                )
            )
        }
        source.markers.forEach { marker ->
            add(
                EpisodeSourceRow(
                    recordType = "MARKER",
                    id = marker.id,
                    ts = marker.ts,
                    endTs = null,
                    value = null,
                    chips = null,
                    text = marker.text
                )
            )
        }
    }.sortedWith(compareBy<EpisodeSourceRow> { it.ts }.thenBy { it.recordType }.thenBy { it.id })
    val insights = deriveInsights(
        rows = rows,
        hold = source.settings.holdDuration,
        now = generatedAt,
        zoneId = zoneId,
        range = range
    )
    val rangeStart = insights.rangeStartMillis
    val nowMillis = insights.nowMillis
    val startDate = Instant.ofEpochMilli(rangeStart).atZone(zoneId).toLocalDate()
    val endDate = generatedAt.atZone(zoneId).toLocalDate()
    val entries = source.entries.filter { it.ts >= rangeStart && it.ts < nowMillis }
    val ratingDays = entries.map { Instant.ofEpochMilli(it.ts).atZone(zoneId).toLocalDate() }.toSet().size
    val markers = source.markers
        .filter { it.ts >= rangeStart && it.ts < nowMillis }
        .sortedWith(compareByDescending<com.kieslingdev.mindscale.data.Marker> { it.ts }.thenByDescending { it.id })
    val includedMarkers = markers.take(MAX_REPORT_MARKERS).sortedWith(compareBy({ it.ts }, { it.id }))
    val scores = source.externalScores
        .filter { LocalDate.ofEpochDay(it.assessedEpochDay) in startDate..endDate }
        .sortedWith(compareByDescending<com.kieslingdev.mindscale.data.ExternalScore> { it.assessedEpochDay }
            .thenByDescending { it.id })
    val includedScores = scores.take(MAX_REPORT_SCORES)
    val recentRatings = entries.sortedWith(compareByDescending<com.kieslingdev.mindscale.data.Entry> { it.ts }.thenByDescending { it.id })
        .take(MAX_REPORT_RATINGS)
        .sortedWith(compareBy({ it.ts }, { it.id }))
        .map { entry ->
            ReportRating(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)
                    .withZone(zoneId).format(Instant.ofEpochMilli(entry.ts)),
                entry.value
            )
        }
    val sleepCounts = insights.sleepCounts
    val sleepDetail = if (!sleepCounts.isEligible) {
        "No completed sleep periods had a recorded Wake in this window."
    } else {
        val nights = sleepCounts.categories.first { it.category == SleepCategory.NIGHT }.count
        val naps = sleepCounts.categories.first { it.category == SleepCategory.NAP }.count
        "${sleepCounts.completedCount} completed sleep ${"period".plural(sleepCounts.completedCount)}: " +
            "$nights ${"night".plural(nights)} (>3 elapsed h), $naps ${"nap".plural(naps)} (≤3 elapsed h)."
    } + (sleepIncompleteText(sleepCounts)?.let { " $it" } ?: "")
    val presentation = ReportPresentation(
        rangeText = "$startDate through $endDate · ${range.spokenLabel}",
        name = source.profile.displayName.trim().takeIf(String::isNotEmpty)?.collapseWhitespace(),
        metrics = listOf(
            ReportMetric("Ratings", "${entries.size} on $ratingDays ${"day".plural(ratingDays)}"),
            ReportMetric("Episodes", "${insights.summary.episodeCount} derived"),
            ReportMetric("Clear days", "${insights.summary.clearDays}/${insights.summary.eligibleDays} eligible"),
            ReportMetric("Peak", insights.summary.peak?.let { "$it/10" } ?: "—"),
            ReportMetric("Median peak", insights.summary.medianPeak?.let { "$it/10" } ?: "—"),
            ReportMetric("Typical length", insights.summary.typicalLengthMillis?.let { "${formatDuration(it)} awake" } ?: "—"),
            ReportMetric("Logged burden", "${String.format(Locale.ROOT, "%.1f", insights.summary.intensityHours)} intensity-h")
        ),
        ratingCount = entries.size,
        ratingDays = ratingDays,
        ratings = recentRatings,
        events = includedMarkers.map { marker ->
            val date = Instant.ofEpochMilli(marker.ts).atZone(zoneId).toLocalDate()
            "$date — ${marker.text.collapseWhitespace()}"
        },
        omittedEvents = markers.size - includedMarkers.size,
        episodeDetail = "Derived spans use the ${source.settings.holdDuration.hours}-hour waking hold; endings may be recorded, held, or ongoing.",
        onsetDetail = if (insights.onsetTimeCounts.isEligible) {
            onsetTimeFourHourSentence(insights.onsetTimeCounts, source.settings.hourFormat)
        } else {
            "Fewer than 6 recorded starts; no time-of-day count shown."
        },
        sleepDetail = sleepDetail,
        scores = includedScores.map { score ->
            "${LocalDate.ofEpochDay(score.assessedEpochDay)} — ${score.instrument.visibleLabel} total ${score.total} (entered from a result obtained elsewhere)"
        },
        omittedScores = scores.size - includedScores.size,
        context = "Gaps reflect when recording was possible. Ratings and event times are user entries; episode spans and intensity-hours are derived from ratings. Times use ${zoneId.id}."
    )

    val text = buildString {
        appendLine("MINDSCALE — CLINICIAN SUMMARY")
        presentation.name?.let { appendLine("Name: $it") }
        appendLine("Window: ${presentation.rangeText}")
        appendLine("Generated: $generatedAt")
        appendLine()
        appendLine("AT A GLANCE")
        presentation.metrics.forEach { appendLine("${it.label}: ${it.value}") }
        appendLine()
        appendLine("RECORDED COURSE")
        if (presentation.ratings.isEmpty()) appendLine("No ratings were recorded in this window.")
        else {
            appendLine("Latest ${presentation.ratings.size} of ${presentation.ratingCount} ratings (0–10):")
            presentation.ratings.forEach { appendLine("${it.time} — ${it.value}/10") }
        }
        appendLine()
        appendLine("EPISODES AND STARTS")
        appendLine(presentation.episodeDetail)
        appendLine(presentation.onsetDetail)
        appendLine()
        appendLine("EVENTS MARKED")
        if (presentation.events.isEmpty()) appendLine("No events were marked in this window.")
        else presentation.events.forEach(::appendLine)
        if (presentation.omittedEvents > 0) appendLine("${presentation.omittedEvents} additional marked ${"event".plural(presentation.omittedEvents)} not shown.")
        appendLine()
        appendLine("SLEEP")
        appendLine(presentation.sleepDetail)
        if (presentation.scores.isNotEmpty()) {
            appendLine()
            appendLine("EXTERNALLY OBTAINED TOTALS")
            presentation.scores.forEach(::appendLine)
            if (presentation.omittedScores > 0) appendLine("${presentation.omittedScores} additional externally obtained ${"total".plural(presentation.omittedScores)} not shown.")
            appendLine("MindScale did not administer or calculate these totals.")
        }
        appendLine()
        appendLine(presentation.context)
        append("This is a record summary, not a clinical assessment. Review the underlying records.")
    }

    return ClinicianReport(text, range, generatedAt, zoneId, presentation)
}

private fun String.plural(count: Int): String = if (count == 1) this else "${this}s"

private fun String.collapseWhitespace(): String = trim().replace(Regex("\\s+"), " ")
