package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.DataSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FilenameFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

fun backupFilename(at: Instant): String = "mindscale-backup-${FilenameFormatter.format(at)}.json"
fun recordsFilename(at: Instant): String = "mindscale-records-${FilenameFormatter.format(at)}.csv"

fun encodeBackup(snapshot: DataSnapshot, exportedAt: Instant): String = buildString {
    append("{\n")
    append("  \"format\": \"mindscale-backup\",\n")
    append("  \"version\": 5,\n")
    append("  \"exportedAt\": ").appendJson(exportedAt.toString()).append(",\n")
    append("  \"entries\": [")
    snapshot.entries.forEachIndexed { index, entry ->
        if (index > 0) append(',')
        append("\n    {\"id\": ${entry.id}, \"timestamp\": ")
            .appendJson(Instant.ofEpochMilli(entry.ts).toString())
            .append(", \"intensity\": ${entry.value}, \"chips\": ")
            .appendJsonArray(entry.chips)
            .append(", \"note\": ").appendNullableJson(entry.note)
            .append(", \"kind\": ").appendNullableJson(entry.kind?.name).append('}')
    }
    if (snapshot.entries.isNotEmpty()) append('\n').append("  ")
    append("],\n  \"sleeps\": [")
    snapshot.sleeps.forEachIndexed { index, sleep ->
        if (index > 0) append(',')
        append("\n    {\"id\": ${sleep.id}, \"start\": ")
            .appendJson(Instant.ofEpochMilli(sleep.startTs).toString())
            .append(", \"end\": ")
            .appendNullableJson(sleep.endTs?.let { Instant.ofEpochMilli(it).toString() }).append('}')
    }
    if (snapshot.sleeps.isNotEmpty()) append('\n').append("  ")
    append("],\n  \"markers\": [")
    snapshot.markers.forEachIndexed { index, marker ->
        if (index > 0) append(',')
        append("\n    {\"id\": ${marker.id}, \"timestamp\": ")
            .appendJson(Instant.ofEpochMilli(marker.ts).toString())
            .append(", \"text\": ").appendJson(marker.text).append('}')
    }
    if (snapshot.markers.isNotEmpty()) append('\n').append("  ")
    val s = snapshot.settings
    append("],\n  \"settings\": {")
    append("\n    \"themeMode\": ").appendJson(s.themeMode.name).append(',')
    append("\n    \"hourFormat\": ").appendJson(s.hourFormat.name).append(',')
    append("\n    \"anchor2\": ").appendJson(s.anchor2).append(',')
    append("\n    \"anchor5\": ").appendJson(s.anchor5).append(',')
    append("\n    \"anchor8\": ").appendJson(s.anchor8).append(',')
    append("\n    \"onsetChips\": ").appendJsonArray(s.onsetChips).append(',')
    append("\n    \"sleepOn\": ${s.sleepOn},")
    append("\n    \"askChips\": ${s.askChips},")
    append("\n    \"hideNotes\": ${s.hideNotes},")
    append("\n    \"paused\": ${s.paused},")
    append("\n    \"holdHours\": ${s.holdDuration.hours}")
    append("\n  },\n")
    append("  \"profile\": {\"displayName\": ").appendJson(snapshot.profile.displayName).append("},\n")
    append("  \"externalScores\": [")
    snapshot.externalScores
        .sortedWith(compareByDescending<com.kieslingdev.mindscale.data.ExternalScore> { it.assessedEpochDay }
            .thenByDescending { it.id })
        .forEachIndexed { index, score ->
            if (index > 0) append(',')
            append("\n    {\"id\": ${score.id}, \"instrument\": ")
                .appendJson(score.instrument.name)
                .append(", \"total\": ${score.total}, \"assessedDate\": ")
                .appendJson(LocalDate.ofEpochDay(score.assessedEpochDay).toString())
                .append(", \"provenance\": ").appendJson(score.provenance.name)
                .append(", \"enteredAt\": ")
                .appendJson(Instant.ofEpochMilli(score.enteredAt).toString())
                .append('}')
        }
    if (snapshot.externalScores.isNotEmpty()) append('\n').append("  ")
    append("]\n}")
}

fun encodeRecordsCsv(snapshot: DataSnapshot): String = buildString {
    append("record_type,timestamp,end_timestamp,intensity,kind,chips,note,text\r\n")
    snapshot.entries.forEach { entry ->
        appendCsvRow(
            "rating", Instant.ofEpochMilli(entry.ts).toString(), "", entry.value.toString(),
            entry.kind?.name.orEmpty(), entry.chips.joinToString("|"), entry.note.orEmpty(), ""
        )
    }
    snapshot.sleeps.forEach { sleep ->
        appendCsvRow(
            "sleep", Instant.ofEpochMilli(sleep.startTs).toString(),
            sleep.endTs?.let { Instant.ofEpochMilli(it).toString() }.orEmpty(), "", "", "", "", ""
        )
    }
    snapshot.markers.forEach { marker ->
        appendCsvRow("marker", Instant.ofEpochMilli(marker.ts).toString(), "", "", "", "", "", marker.text)
    }
}

private fun StringBuilder.appendCsvRow(vararg values: String) {
    append(values.joinToString(",") { value ->
        if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }).append("\r\n")
}

private fun StringBuilder.appendJson(value: String): StringBuilder = append('"').apply {
    value.forEach { c ->
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
        }
    }
}.append('"')

private fun StringBuilder.appendNullableJson(value: String?): StringBuilder =
    if (value == null) append("null") else appendJson(value)

private fun StringBuilder.appendJsonArray(values: List<String>): StringBuilder = apply {
    append('[')
    values.forEachIndexed { index, value ->
        if (index > 0) append(", ")
        appendJson(value)
    }
    append(']')
}
