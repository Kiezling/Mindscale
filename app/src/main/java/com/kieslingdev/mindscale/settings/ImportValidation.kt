package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.SleepInterval
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Shared value rules for both import formats.
 *
 * The governing principle is that an imported value must be a value MindScale itself
 * could have stored through its own write paths. That is why marker text must already
 * be trimmed and non-blank (Track trims and refuses blank), why a note must be non-blank
 * but need *not* be trimmed (Track stores `text.ifBlank { null }` without trimming), and
 * why onset words must already be normalized. Accepting a value the app's own validator
 * would reject would be exactly the silent reinterpretation the spec forbids.
 *
 * See `docs/specs/SPEC-import-restore.md`, D-5 and D-6.
 */

private val EPOCH_DATE: LocalDate = LocalDate.of(1970, 1, 1)

/** Code-point length, so astral characters count once against a frozen limit. */
internal fun String.codePointLength(): Int = codePointCount(0, length)

/**
 * `note` and marker text may contain TAB/LF/CR because Track stores multi-line text and
 * both encoders round-trip those characters. Nothing else in C0, DEL, or a stray BOM may
 * survive into a stored field. Single-line fields allow no control character at all —
 * entry chips in particular are persisted joined by U+001F, so one embedded separator
 * would silently corrupt the stored list.
 */
internal fun String.hasDisallowedControls(allowLineBreaks: Boolean): Boolean = any { c ->
    when (c) {
        '\t', '\n', '\r' -> !allowLineBreaks
        '\uFEFF' -> true
        else -> c.code < 0x20 || c.code == 0x7F
    }
}

internal fun requireSingleLine(value: String, maxCodePoints: Int): String {
    if (value.hasDisallowedControls(allowLineBreaks = false)) reject(ImportMessages.UNSTORABLE_VALUE)
    if (value != value.trim()) reject(ImportMessages.UNSTORABLE_VALUE)
    if (value.codePointLength() > maxCodePoints) reject(ImportMessages.UNSTORABLE_VALUE)
    return value
}

internal fun requireChip(value: String): String {
    if (value.hasDisallowedControls(allowLineBreaks = false)) reject(ImportMessages.UNSTORABLE_VALUE)
    if (value != value.trim() || value.isEmpty()) reject(ImportMessages.UNSTORABLE_VALUE)
    if (value.codePointLength() > MAX_CHIP_CODE_POINTS) reject(ImportMessages.UNSTORABLE_VALUE)
    return value
}

internal fun requireChips(values: List<String>): List<String> {
    if (values.size > MAX_CHIPS_PER_ENTRY) reject(ImportMessages.UNSTORABLE_VALUE)
    return values.map(::requireChip)
}

/** Track writes `text.ifBlank { null }` without trimming, so leading space is storable. */
internal fun requireNote(value: String): String {
    if (value.hasDisallowedControls(allowLineBreaks = true)) reject(ImportMessages.UNSTORABLE_VALUE)
    if (value.isBlank()) reject(ImportMessages.UNSTORABLE_VALUE)
    if (value.codePointLength() > MAX_NOTE_CODE_POINTS) reject(ImportMessages.UNSTORABLE_VALUE)
    return value
}

/** Track saves `markerDraft.trim()` and skips the insert when it is empty. */
internal fun requireMarkerText(value: String): String {
    if (value.hasDisallowedControls(allowLineBreaks = true)) reject(ImportMessages.UNSTORABLE_VALUE)
    if (value != value.trim() || value.isEmpty()) reject(ImportMessages.UNSTORABLE_VALUE)
    if (value.codePointLength() > MAX_MARKER_CODE_POINTS) reject(ImportMessages.UNSTORABLE_VALUE)
    return value
}

internal fun requireIntensity(value: Int): Int {
    if (value !in 0..10) reject(ImportMessages.UNSTORABLE_VALUE)
    return value
}

internal fun requireRowId(value: Long): Long {
    if (value < 1L) reject(ImportMessages.UNSTORABLE_VALUE)
    return value
}

/**
 * Requires the exact canonical `Instant.toString()` form that both encoders write, plus
 * the frozen range. Used for metadata instants that are never stored in a column.
 *
 * `exportedAt` is the one such field: the exporter writes `Instant.now()`, which on API 26
 * and above carries microsecond precision, so it must not be held to the millisecond rule
 * below or MindScale could not read back its own backup.
 */
internal fun requireInstant(raw: String, now: Instant, shapeFailure: String): Instant {
    val instant = try {
        Instant.parse(raw)
    } catch (error: DateTimeParseException) {
        reject(shapeFailure)
    }
    if (instant.toString() != raw) reject(shapeFailure)
    if (instant.isBefore(Instant.EPOCH)) reject(ImportMessages.UNSTORABLE_VALUE)
    if (instant.toEpochMilli() > now.toEpochMilli() + FUTURE_TOLERANCE_MILLIS) {
        reject(ImportMessages.UNSTORABLE_VALUE)
    }
    return instant
}

/**
 * Adds the millisecond-precision requirement for the instants that become epoch-millis
 * columns. Every such value is written from `Instant.ofEpochMilli(...)`, so canonical form
 * plus millisecond precision is exactly what makes export-after-restore byte-reproducible.
 */
internal fun requireInstantMillis(raw: String, now: Instant, shapeFailure: String): Long {
    val instant = requireInstant(raw, now, shapeFailure)
    if (instant.nano % 1_000_000 != 0) reject(ImportMessages.UNSTORABLE_VALUE)
    return instant.toEpochMilli()
}

internal fun requireAssessedEpochDay(raw: String, now: Instant, zone: ZoneId): Long {
    val date = try {
        LocalDate.parse(raw)
    } catch (error: DateTimeParseException) {
        reject(ImportMessages.BACKUP_SHAPE)
    }
    if (date.toString() != raw) reject(ImportMessages.BACKUP_SHAPE)
    if (date.isBefore(EPOCH_DATE) || date.isAfter(now.atZone(zone).toLocalDate())) {
        reject(ImportMessages.UNSTORABLE_VALUE)
    }
    return date.toEpochDay()
}

internal fun requireSleepBounds(startTs: Long, endTs: Long?) {
    if (endTs != null && endTs < startTs) reject(ImportMessages.UNSTORABLE_VALUE)
}

/**
 * Half-open `[startTs, endTs)` non-overlap plus the single-open-interval invariant that
 * Track and the episode engine both depend on. An open interval spans to the end of time.
 */
internal fun requireSleepStructure(sleeps: List<SleepInterval>) {
    if (sleeps.count { it.endTs == null } > 1) reject(ImportMessages.SLEEP_OPEN)
    var reach = Long.MIN_VALUE
    sleeps.sortedBy { it.startTs }.forEach { interval ->
        if (interval.startTs < reach) reject(ImportMessages.SLEEP_OVERLAP)
        reach = maxOf(reach, interval.endTs ?: Long.MAX_VALUE)
    }
}
