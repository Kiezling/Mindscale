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

private val ReportFilenameFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

data class ClinicianReport(
    val text: String,
    val range: InsightRange,
    val generatedAt: Instant,
    val zoneId: ZoneId
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
    val lineSeparator = "\n"

    val text = buildString {
        appendLine("MINDSCALE — USER-RECORDED CLINICIAN SUMMARY")
        source.profile.displayName.trim().takeIf(String::isNotEmpty)?.let {
            appendLine("Name: ${it.collapseWhitespace()}")
        }
        appendLine("Window: $startDate through $endDate · ${range.spokenLabel}")
        appendLine("Generated: ${generatedAt} · current zone ${zoneId.id}")
        appendLine()
        appendLine("MindScale stores and arranges user-recorded information. It does not diagnose,")
        appendLine("interpret, administer questionnaires, or provide a clinical assessment.")
        appendLine("Times reflect when recording was possible.")
        appendLine()

        appendLine("RECORDED COURSE")
        if (entries.isEmpty()) {
            appendLine("No ratings were recorded in this window.")
        } else {
            appendLine("${entries.size} ${"rating".plural(entries.size)} were recorded on $ratingDays " +
                "local calendar ${"day".plural(ratingDays)} in this window.")
            appendLine("${insights.summary.episodeCount} derived ${"episode span".plural(insights.summary.episodeCount)} " +
                "touched this window using the configured ${source.settings.holdDuration.hours}-hour waking hold.")
            if (insights.summary.eligibleDays > 0) {
                appendLine("${insights.summary.clearDays} of ${insights.summary.eligibleDays} eligible local days " +
                    "had no derived intensity above 0.")
            }
            appendLine("Recorded intensity-hours: ${String.format(Locale.ROOT, "%.1f", insights.summary.intensityHours)} " +
                "(recorded intensity multiplied by awake hours in this window).")
        }
        appendLine()

        appendLine("EVENTS MARKED")
        if (markers.isEmpty()) {
            appendLine("No events were marked in this window.")
        } else {
            includedMarkers.forEach { marker ->
                val date = Instant.ofEpochMilli(marker.ts).atZone(zoneId).toLocalDate()
                appendLine("$date — ${marker.text.collapseWhitespace()}")
            }
            val omitted = markers.size - includedMarkers.size
            if (omitted > 0) appendLine("$omitted additional marked ${"event".plural(omitted)} not shown.")
        }
        appendLine()

        appendLine("EPISODE STRUCTURE")
        if (insights.summary.episodeCount == 0) {
            appendLine("No derived episode spans touched this window.")
        } else {
            insights.summary.typicalLengthMillis?.let {
                appendLine("Middle closed derived episode length: ${formatDuration(it)} of waking time.")
            } ?: appendLine("No closed derived episode length is available in this window.")
            insights.summary.peak?.let {
                appendLine("Highest recorded intensity in a derived episode: $it of 10.")
            }
            appendLine("An ending may be a recorded 0, the configured waking hold, or still ongoing.")
        }
        appendLine()

        appendLine("TIME OF DAY")
        if (insights.onsetTimeCounts.isEligible) {
            appendLine(onsetTimeFourHourSentence(insights.onsetTimeCounts, source.settings.hourFormat))
            appendLine("Start times use the device's current time zone and reflect when recording was possible.")
            appendLine("They do not establish when symptoms began.")
        } else {
            appendLine("Fewer than 6 recorded starts are in this window, so no time-of-day count is shown.")
        }
        appendLine()

        appendLine("SLEEP")
        val sleepCounts = insights.sleepCounts
        if (!sleepCounts.isEligible) {
            appendLine("No completed sleep periods had a recorded Wake in this window.")
        } else {
            val nights = sleepCounts.categories.first { it.category == SleepCategory.NIGHT }.count
            val naps = sleepCounts.categories.first { it.category == SleepCategory.NAP }.count
            appendLine("${sleepCounts.completedCount} completed sleep ${"period".plural(sleepCounts.completedCount)} " +
                "had a recorded Wake in this window: $nights ${"night".plural(nights)} over 3 elapsed hours " +
                "and $naps ${"nap".plural(naps)} of 3 elapsed hours or less.")
        }
        sleepIncompleteText(sleepCounts)?.let(::appendLine)
        appendLine("These counts do not establish whether sleep affected later records or later records affected sleep.")
        appendLine()

        appendLine("EXTERNALLY OBTAINED TOTALS")
        if (scores.isEmpty()) {
            appendLine("No externally obtained PHQ-8 or GAD-7 totals are stored in this window.")
        } else {
            includedScores.forEach { score ->
                val date = LocalDate.ofEpochDay(score.assessedEpochDay)
                appendLine("$date — ${score.instrument.visibleLabel} total ${score.total} — " +
                    "entered by the user from a result obtained elsewhere.")
            }
            val omitted = scores.size - includedScores.size
            if (omitted > 0) appendLine("$omitted additional externally obtained ${"total".plural(omitted)} not shown.")
        }
        appendLine("MindScale did not administer, calculate, or interpret PHQ-8 or GAD-7 totals.")
        appendLine()
        appendLine("Generated locally on this device. This text may contain sensitive health information.")
        append("Review the underlying records before relying on this summary.")
    }.replace("\n", lineSeparator)

    return ClinicianReport(text, range, generatedAt, zoneId)
}

private fun String.plural(count: Int): String = if (count == 1) this else "${this}s"

private fun String.collapseWhitespace(): String = trim().replace(Regex("\\s+"), " ")
