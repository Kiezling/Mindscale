package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.BackupPayload
import com.kieslingdev.mindscale.data.RecordsPayload

/**
 * Phase 12 import types, limits, and copy. See `docs/specs/SPEC-import-restore.md`.
 *
 * Everything in the import package is pure and Android-free so the whole
 * hostile-file surface is covered by JVM tests. Composables never parse,
 * validate, or resolve conflicts (D-1, D-10).
 */

/** The two frozen, never-blended import semantics (D-1). */
enum class ImportKind { BACKUP_RESTORE, RECORDS_MERGE }

/** Backup format versions MindScale has ever written and can therefore read (D-2). */
const val MIN_BACKUP_VERSION = 3
const val MAX_BACKUP_VERSION = 6

/** Frozen limits (D-5). Enforced before unbounded allocation. */
const val MAX_IMPORT_BYTES = 8L * 1024L * 1024L
const val MAX_TOTAL_RECORDS = 50_000
const val MAX_RECORDS_PER_COLLECTION = 20_000
const val MAX_JSON_DEPTH = 8
const val MAX_JSON_VALUES = 2_000_000
const val MAX_NOTE_CODE_POINTS = 4_000
const val MAX_MARKER_CODE_POINTS = 4_000
const val MAX_CHIPS_PER_ENTRY = 20
const val MAX_DISPLAY_NAME_CODE_POINTS = 80

/** Clock-skew tolerance for timestamps written by the exporting device (D-5). */
const val FUTURE_TOLERANCE_MILLIS = 24L * 60L * 60L * 1000L

/** The exact CSV header the records export writes (D-6). */
const val RECORDS_CSV_HEADER = "record_type,timestamp,end_timestamp,intensity,kind,chips,note,text"

sealed interface ImportPayload {
    data class Restore(val backup: BackupPayload) : ImportPayload
    data class Merge(val records: RecordsPayload) : ImportPayload

    val kind: ImportKind
        get() = when (this) {
            is Restore -> ImportKind.BACKUP_RESTORE
            is Merge -> ImportKind.RECORDS_MERGE
        }
}

/** Immutable preview text. Rendered only from a fully validated parse (D-7). */
data class ImportPreview(
    val kind: ImportKind,
    val title: String,
    val lines: List<String>,
    val confirmLabel: String
)

/** Counts of what is already stored, used only to state exact preview consequences. */
data class RecordCounts(
    val entries: Int = 0,
    val sleeps: Int = 0,
    val markers: Int = 0,
    val externalScores: Int = 0,
    val safetyPlanItems: Int = 0
)

sealed interface ParseResult<out T> {
    data class Ok<T>(val value: T) : ParseResult<T>
    data class Rejected(val message: String) : ParseResult<Nothing>
}

/**
 * Internal control flow for total rejection. Every validation failure throws this and
 * is converted to a [ParseResult.Rejected] at the public boundary, so no partially
 * built payload can ever escape (D-4, D-5).
 */
internal class ImportRejection(val reason: String) : Exception(null, null, false, false)

internal fun reject(reason: String): Nothing = throw ImportRejection(reason)

/**
 * Frozen user-visible copy (D-7, D-12). No message names a file, a path, a URI, or any
 * value from the file; structural messages may name a position, which is structure and
 * not content.
 */
object ImportMessages {
    const val TOO_LARGE = "This file is larger than 8 MB. MindScale did not read it."
    const val NOT_UTF8 = "This file is not valid UTF-8 text."
    const val NOT_A_BACKUP = "This is not a MindScale backup file."
    const val NEWER_VERSION =
        "This backup was made by a newer version of MindScale. Update MindScale, then try again."
    const val UNSUPPORTED_VERSION = "This backup version is not supported."
    const val BACKUP_SHAPE = "This backup does not match the MindScale backup format."
    const val UNSTORABLE_VALUE = "This file contains a value MindScale cannot store."
    const val TOO_MANY_RECORDS = "This file contains more records than MindScale can import."
    const val NOT_A_RECORDS_CSV = "This is not a MindScale records CSV."
    const val SLEEP_OVERLAP = "This file contains overlapping sleep periods. Nothing was imported."
    const val SLEEP_OPEN =
        "This file would leave more than one sleep period open. Nothing was imported."
    const val RECORDS_CHANGED =
        "Your records changed while this preview was open. Nothing was imported."
    const val READ_FAILED = "Could not read that file. Choose it again to retry."
    const val IMPORT_FAILED = "Could not import this file. Nothing on this device was changed."
    const val PREVIEW_LOST = "Choose the file again to see the preview."

    fun invalidJson(line: Int, column: Int) = "This backup is not valid JSON (line $line, column $column)."

    fun invalidCsv(line: Int) = "This CSV does not match the MindScale records format (line $line)."

    fun duplicates(count: Int) =
        "This file contains $count duplicate ${records(count)}. Nothing was imported."

    fun conflicts(count: Int) =
        "$count of these records ${if (count == 1) "is" else "are"} already in MindScale. " +
            "Nothing was imported."

    fun restored(entries: Int, sleeps: Int, markers: Int, scores: Int, planItems: Int): String =
        "Restored $entries ${ratings(entries)}, $sleeps sleep ${periods(sleeps)}, " +
            "$markers marked ${events(markers)}, $scores externally obtained " +
            "${totals(scores)}, and $planItems safety plan ${lines(planItems)}."

    fun added(entries: Int, sleeps: Int, markers: Int): String =
        "Added $entries ${ratings(entries)}, $sleeps sleep ${periods(sleeps)}, and " +
            "$markers marked ${events(markers)}."

    internal fun records(count: Int) = if (count == 1) "record" else "records"
    internal fun ratings(count: Int) = if (count == 1) "rating" else "ratings"
    internal fun periods(count: Int) = if (count == 1) "period" else "periods"
    internal fun events(count: Int) = if (count == 1) "event" else "events"
    internal fun totals(count: Int) = if (count == 1) "total" else "totals"
    internal fun lines(count: Int) = if (count == 1) "line" else "lines"
}
