package com.kieslingdev.mindscale.log

import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val EditTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

data class EpochRange(val fromTs: Long?, val toTsExclusive: Long?)

fun epochRange(filter: LogFilter, zone: ZoneId): EpochRange = EpochRange(
    fromTs = filter.from?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(),
    toTsExclusive = filter.to?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
)

fun combineLogItems(
    entries: List<Entry>,
    sleeps: List<SleepInterval>,
    markers: List<Marker>
): List<LogItem> {
    val items = buildList {
        entries.forEach { add(LogItem.Rating(it)) }
        markers.forEach { add(LogItem.Event(it)) }
        sleeps.forEach { add(LogItem.Sleep(it)) }
    }
    return items.sortedWith(
        compareByDescending<LogItem> { it.timestamp }
            .thenBy { typeOrder(it) }
            .thenByDescending { it.id }
    )
}

private fun typeOrder(item: LogItem): Int = when (item) {
    is LogItem.Rating -> 0
    is LogItem.Event -> 1
    is LogItem.Sleep -> 2
}

fun groupLogItems(items: List<LogItem>, zone: ZoneId): List<LogDay> =
    items.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .entries
        .sortedByDescending { it.key }
        .map { (date, dayItems) -> LogDay(date, dayItems) }

fun formatSleepDuration(interval: SleepInterval): String {
    val endTs = interval.endTs ?: return "sleeping now"
    val totalMinutes = ((endTs - interval.startTs).coerceAtLeast(0L)) / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

fun formatEditTimestamp(timestamp: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(timestamp).atZone(zone).format(EditTimestampFormatter)

fun parseEditTimestamp(text: String, zone: ZoneId): Long? = try {
    LocalDateTime.parse(text, EditTimestampFormatter).atZone(zone).toInstant().toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}
