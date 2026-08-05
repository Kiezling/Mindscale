package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.RecordsPayload
import com.kieslingdev.mindscale.data.SleepInterval
import java.time.Instant

/**
 * Validates a MindScale records CSV (`docs/specs/SPEC-import-restore.md`, D-1, D-5, D-6).
 *
 * The CSV carries no ids, no settings, no Profile, and no external totals, so importing
 * one can only append ratings, sleeps, and markers. Parsing is strict RFC 4180 with one
 * deliberate leniency: a bare LF row terminator is accepted alongside CRLF, because
 * line-ending rewriting by editors and transfer tools cannot change any field value.
 */

private val CSV_HEADER_FIELDS = RECORDS_CSV_HEADER.split(",")

private const val TYPE = 0
private const val TIMESTAMP = 1
private const val END_TIMESTAMP = 2
private const val INTENSITY = 3
private const val KIND = 4
private const val CHIPS = 5
private const val NOTE = 6
private const val TEXT = 7

private class CsvRow(val line: Int, val fields: List<String>)

fun parseRecordsCsv(
    text: String,
    now: Instant = Instant.now()
): ParseResult<RecordsPayload> = try {
    ParseResult.Ok(readRecords(text, now))
} catch (rejection: ImportRejection) {
    ParseResult.Rejected(rejection.reason)
}

private fun readRecords(text: String, now: Instant): RecordsPayload {
    val rows = CsvReader(text).readRows()
    val header = rows.firstOrNull() ?: reject(ImportMessages.NOT_A_RECORDS_CSV)
    if (header.fields != CSV_HEADER_FIELDS) reject(ImportMessages.NOT_A_RECORDS_CSV)

    val entries = ArrayList<Entry>()
    val sleeps = ArrayList<SleepInterval>()
    val markers = ArrayList<Marker>()

    rows.drop(1).forEach { row ->
        if (row.fields.size != CSV_HEADER_FIELDS.size) reject(ImportMessages.invalidCsv(row.line))
        when (row.fields[TYPE]) {
            "rating" -> entries += readRating(row, now)
            "sleep" -> sleeps += readSleep(row, now)
            "marker" -> markers += readMarker(row, now)
            else -> reject(ImportMessages.invalidCsv(row.line))
        }
    }

    if (entries.size > MAX_RECORDS_PER_COLLECTION || sleeps.size > MAX_RECORDS_PER_COLLECTION ||
        markers.size > MAX_RECORDS_PER_COLLECTION ||
        entries.size + sleeps.size + markers.size > MAX_TOTAL_RECORDS
    ) {
        reject(ImportMessages.TOO_MANY_RECORDS)
    }

    return RecordsPayload(entries = entries, sleeps = sleeps, markers = markers)
}

private fun readRating(row: CsvRow, now: Instant): Entry {
    row.requireEmpty(END_TIMESTAMP, TEXT)
    return Entry(
        ts = requireInstantMillis(row.required(TIMESTAMP), now, ImportMessages.invalidCsv(row.line)),
        value = requireIntensity(row.requiredInt(INTENSITY)),
        chips = requireChips(row.chips()),
        note = row.optional(NOTE)?.let(::requireNote),
        kind = row.optional(KIND)?.let { raw ->
            EntryKind.entries.firstOrNull { it.name == raw }
                ?: reject(ImportMessages.invalidCsv(row.line))
        }
    )
}

private fun readSleep(row: CsvRow, now: Instant): SleepInterval {
    row.requireEmpty(INTENSITY, KIND, CHIPS, NOTE, TEXT)
    val startTs =
        requireInstantMillis(row.required(TIMESTAMP), now, ImportMessages.invalidCsv(row.line))
    val endTs = row.optional(END_TIMESTAMP)
        ?.let { requireInstantMillis(it, now, ImportMessages.invalidCsv(row.line)) }
    requireSleepBounds(startTs, endTs)
    return SleepInterval(startTs = startTs, endTs = endTs)
}

private fun readMarker(row: CsvRow, now: Instant): Marker {
    row.requireEmpty(END_TIMESTAMP, INTENSITY, KIND, CHIPS, NOTE)
    return Marker(
        ts = requireInstantMillis(row.required(TIMESTAMP), now, ImportMessages.invalidCsv(row.line)),
        text = requireMarkerText(row.required(TEXT))
    )
}

/** A field that does not apply to this row type must be empty; a value there is ambiguous. */
private fun CsvRow.requireEmpty(vararg indexes: Int) {
    if (indexes.any { fields[it].isNotEmpty() }) reject(ImportMessages.invalidCsv(line))
}

private fun CsvRow.required(index: Int): String =
    fields[index].ifEmpty { reject(ImportMessages.invalidCsv(line)) }

/**
 * The exporter writes `entry.note.orEmpty()`, and Track stores `text.ifBlank { null }`,
 * so an empty note column can only have come from a null note. That makes the mapping
 * unambiguous rather than a reinterpretation.
 */
private fun CsvRow.optional(index: Int): String? = fields[index].ifEmpty { null }

private fun CsvRow.requiredInt(index: Int): Int {
    val raw = required(index)
    val parsed = raw.toIntOrNull() ?: reject(ImportMessages.invalidCsv(line))
    // Refuse any non-canonical spelling such as "+5" or "007".
    if (parsed.toString() != raw) reject(ImportMessages.invalidCsv(line))
    return parsed
}

private fun CsvRow.chips(): List<String> {
    val raw = fields[CHIPS]
    if (raw.isEmpty()) return emptyList()
    val parts = raw.split('|')
    if (parts.any(String::isEmpty)) reject(ImportMessages.invalidCsv(line))
    return parts
}

private class CsvReader(private val text: String) {

    private var index = 0
    private var line = 1

    fun readRows(): List<CsvRow> {
        val rows = ArrayList<CsvRow>()
        while (index < text.length) {
            val startLine = line
            rows += CsvRow(startLine, readRecord(startLine))
            if (rows.size > MAX_TOTAL_RECORDS + 1) reject(ImportMessages.TOO_MANY_RECORDS)
            if (index >= text.length) break
            consumeTerminator(startLine)
        }
        return rows
    }

    private fun readRecord(startLine: Int): List<String> {
        val fields = ArrayList<String>()
        while (true) {
            fields += if (index < text.length && text[index] == '"') {
                readQuotedField(startLine)
            } else {
                readPlainField(startLine)
            }
            if (index < text.length && text[index] == ',') {
                // Refuse a ninth field immediately rather than building an unbounded row.
                if (fields.size >= CSV_HEADER_FIELDS.size) reject(ImportMessages.invalidCsv(startLine))
                index++
            } else {
                return fields
            }
        }
    }

    private fun readPlainField(startLine: Int): String {
        val start = index
        while (index < text.length) {
            when (text[index]) {
                ',', '\r', '\n' -> return text.substring(start, index)
                // A quote may only open a field, never appear inside an unquoted one.
                '"' -> reject(ImportMessages.invalidCsv(startLine))
                else -> index++
            }
        }
        return text.substring(start, index)
    }

    private fun readQuotedField(startLine: Int): String {
        index++
        val builder = StringBuilder()
        while (true) {
            if (index >= text.length) reject(ImportMessages.invalidCsv(startLine))
            val c = text[index]
            if (c == '"') {
                if (index + 1 < text.length && text[index + 1] == '"') {
                    builder.append('"')
                    index += 2
                    continue
                }
                index++
                // After a closing quote only a separator, a terminator, or EOF may follow.
                if (index < text.length &&
                    text[index] != ',' && text[index] != '\r' && text[index] != '\n'
                ) {
                    reject(ImportMessages.invalidCsv(startLine))
                }
                return builder.toString()
            }
            if (c == '\n') line++
            builder.append(c)
            index++
        }
    }

    private fun consumeTerminator(startLine: Int) {
        when (text[index]) {
            '\n' -> index++
            '\r' -> {
                // A bare CR terminator is refused; only CRLF and LF are row terminators.
                if (index + 1 >= text.length || text[index + 1] != '\n') {
                    reject(ImportMessages.invalidCsv(startLine))
                }
                index += 2
            }
            else -> reject(ImportMessages.invalidCsv(startLine))
        }
        line++
    }
}
