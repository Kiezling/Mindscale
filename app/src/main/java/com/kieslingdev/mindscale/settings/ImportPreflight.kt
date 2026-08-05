package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.BackupPayload
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.RecordSnapshot
import com.kieslingdev.mindscale.data.RecordsPayload
import com.kieslingdev.mindscale.data.SleepInterval
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * Everything that must happen before a file is allowed anywhere near Room: bounded
 * reading, strict decoding, conflict resolution, and the exact preview the user confirms
 * (`docs/specs/SPEC-import-restore.md`, D-4, D-5, D-6, D-7).
 */

private const val READ_CHUNK_BYTES = 64 * 1024
private const val BOM = '\uFEFF'

/**
 * Streams at most [MAX_IMPORT_BYTES] and stops at the first byte past the limit, so a
 * hostile or accidental multi-gigabyte file can never be allocated. Decoding reports
 * malformed and unmappable sequences instead of substituting U+FFFD, because silently
 * replacing bytes would change the user's own recorded text.
 */
fun readBoundedUtf8(stream: InputStream): ParseResult<String> {
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(READ_CHUNK_BYTES)
    var total = 0L
    try {
        while (true) {
            val read = stream.read(chunk)
            if (read < 0) break
            total += read
            if (total > MAX_IMPORT_BYTES) return ParseResult.Rejected(ImportMessages.TOO_LARGE)
            buffer.write(chunk, 0, read)
        }
    } catch (error: IOException) {
        return ParseResult.Rejected(ImportMessages.READ_FAILED)
    }

    val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val decoded = try {
        decoder.decode(ByteBuffer.wrap(buffer.toByteArray())).toString()
    } catch (error: CharacterCodingException) {
        return ParseResult.Rejected(ImportMessages.NOT_UTF8)
    }

    // Exactly one leading BOM is stripped. Any other U+FEFF is refused later by the
    // per-field character rules or by the JSON/CSV grammar itself.
    return ParseResult.Ok(if (decoded.startsWith(BOM)) decoded.substring(1) else decoded)
}

/** Natural record identity for a format that carries no ids (D-4). */
private data class RatingKey(
    val ts: Long,
    val value: Int,
    val chips: List<String>,
    val note: String?,
    val kind: EntryKind?
)

private fun Entry.key() = RatingKey(ts, value, chips, note, kind)
private fun SleepInterval.key() = startTs to endTs
private fun Marker.key() = ts to text

/**
 * Refuses in-file duplicates, records that already exist, overlapping sleep periods, and
 * a second open sleep period. Every rule rejects the whole file: skipping rows would be
 * silent partial acceptance, and de-duplicating would silently discard the user's data.
 *
 * The same check runs again inside the mutating transaction, so records that changed
 * while the preview was open cannot slip through.
 */
fun checkRecordConflicts(
    payload: RecordsPayload,
    existing: RecordSnapshot
): ParseResult<RecordsPayload> = try {
    val fileKeys = payload.entries.map { it.key() } +
        payload.sleeps.map { it.key() } +
        payload.markers.map { it.key() }
    val duplicates = fileKeys.size - fileKeys.toSet().size
    if (duplicates > 0) reject(ImportMessages.duplicates(duplicates))

    val storedKeys = (existing.entries.map { it.key() } +
        existing.sleeps.map { it.key() } +
        existing.markers.map { it.key() }).toSet()
    val conflicts = fileKeys.count(storedKeys::contains)
    if (conflicts > 0) reject(ImportMessages.conflicts(conflicts))

    requireSleepStructure(payload.sleeps + existing.sleeps)
    ParseResult.Ok(payload)
} catch (rejection: ImportRejection) {
    ParseResult.Rejected(rejection.reason)
}

/**
 * The exact frozen preview (D-7). It is built only from a fully validated parse and
 * states counts and format facts alone — no severity, comparison, interpretation, or
 * reassurance about what any of it means.
 */
fun previewOf(payload: ImportPayload, existing: RecordCounts): ImportPreview = when (payload) {
    is ImportPayload.Restore -> restorePreview(payload.backup, existing)
    is ImportPayload.Merge -> mergePreview(payload.records)
}

private fun restorePreview(backup: BackupPayload, existing: RecordCounts): ImportPreview {
    val lines = ArrayList<String>()
    lines += "This file is a MindScale backup, version ${backup.version}, " +
        "created ${backup.exportedAt}."
    lines += "It contains ${count(backup.entries.size, ImportMessages::ratings)}, " +
        "${sleepCount(backup.sleeps.size)}, ${markerCount(backup.markers.size)}, and " +
        "${scoreCount(backup.externalScores.size)}, plus your settings and Profile name."
    lines += "Restoring replaces everything currently on this device. MindScale will " +
        "permanently delete ${count(existing.entries, ImportMessages::ratings)}, " +
        "${sleepCount(existing.sleeps)}, ${markerCount(existing.markers)}, and " +
        "${scoreCount(existing.externalScores)}, and will replace your settings and " +
        "Profile name."
    lines += "Check-in time, the sleep introduction flag, and the anchor prompt flag are " +
        "not stored in any backup file. They return to their defaults."
    if (backup.version < 4) {
        lines += "This backup predates the entry-hold setting. The hold returns to 16 " +
            "waking hours."
    }
    if (backup.version < 5) {
        lines += "This backup predates Profile and externally obtained totals. Your " +
            "Profile name will be empty and no totals will be restored."
    }
    if (backup.externalScores.isNotEmpty()) {
        lines += "Totals entered by you from results obtained elsewhere. MindScale did " +
            "not administer or calculate them."
    }
    lines += "Nothing has changed yet."
    return ImportPreview(
        kind = ImportKind.BACKUP_RESTORE,
        title = "Restore from backup",
        lines = lines,
        confirmLabel = "Replace everything"
    )
}

private fun mergePreview(records: RecordsPayload): ImportPreview = ImportPreview(
    kind = ImportKind.RECORDS_MERGE,
    title = "Import records",
    lines = listOf(
        "This file is a MindScale records CSV.",
        "It will add ${count(records.entries.size, ImportMessages::ratings)}, " +
            "${sleepCount(records.sleeps.size)}, and ${markerCount(records.markers.size)}.",
        "Nothing is deleted. Your settings, Profile name, and externally obtained totals " +
            "are not changed. A records CSV does not contain them.",
        "Nothing has changed yet."
    ),
    confirmLabel = "Add these records"
)

private fun count(value: Int, noun: (Int) -> String) = "$value ${noun(value)}"
private fun sleepCount(value: Int) = "$value sleep ${ImportMessages.periods(value)}"
private fun markerCount(value: Int) = "$value marked ${ImportMessages.events(value)}"
private fun scoreCount(value: Int) =
    "$value externally obtained ${ImportMessages.totals(value)}"
