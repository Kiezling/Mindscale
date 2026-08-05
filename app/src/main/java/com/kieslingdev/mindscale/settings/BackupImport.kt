package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.BackupPayload
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.ExternalScoreProvenance
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Validates a MindScale JSON backup of version 3, 4, or 5 (`docs/specs/SPEC-import-restore.md`,
 * D-2, D-3, D-5, D-6, D-8).
 *
 * Every rule is total: the first violation rejects the whole file and no partially built
 * payload escapes. Object key sets are compared for exact equality, so unknown, extra,
 * and missing keys are all refused by one check, and fields a version could not contain
 * take the documented defaults that the preview discloses verbatim.
 */

private const val BACKUP_FORMAT = "mindscale-backup"

private val BASE_ROOT_KEYS =
    setOf("format", "version", "exportedAt", "entries", "sleeps", "markers", "settings")
private val V5_ROOT_KEYS = BASE_ROOT_KEYS + setOf("profile", "externalScores")

private val ENTRY_KEYS = setOf("id", "timestamp", "intensity", "chips", "note", "kind")
private val SLEEP_KEYS = setOf("id", "start", "end")
private val MARKER_KEYS = setOf("id", "timestamp", "text")
private val PROFILE_KEYS = setOf("displayName")
private val SCORE_KEYS =
    setOf("id", "instrument", "total", "assessedDate", "provenance", "enteredAt")

private val BASE_SETTINGS_KEYS = setOf(
    "themeMode", "hourFormat", "anchor2", "anchor5", "anchor8",
    "onsetChips", "sleepOn", "askChips", "hideNotes", "paused"
)
private val HOLD_SETTINGS_KEYS = BASE_SETTINGS_KEYS + "holdHours"

fun parseBackup(
    text: String,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault()
): ParseResult<BackupPayload> = try {
    ParseResult.Ok(readBackup(text, now, zone))
} catch (rejection: ImportRejection) {
    ParseResult.Rejected(rejection.reason)
} catch (syntax: JsonSyntaxException) {
    ParseResult.Rejected(ImportMessages.invalidJson(syntax.line, syntax.column))
}

private fun readBackup(text: String, now: Instant, zone: ZoneId): BackupPayload {
    val root = parseJson(text) as? JsonValue.Obj ?: reject(ImportMessages.NOT_A_BACKUP)
    if (root.str("format", ImportMessages.NOT_A_BACKUP) != BACKUP_FORMAT) {
        reject(ImportMessages.NOT_A_BACKUP)
    }

    val version = (root.members["version"] as? JsonValue.Num)?.asIntOrNull()
        ?: reject(ImportMessages.UNSUPPORTED_VERSION)
    if (version > MAX_BACKUP_VERSION) reject(ImportMessages.NEWER_VERSION)
    if (version < MIN_BACKUP_VERSION) reject(ImportMessages.UNSUPPORTED_VERSION)

    val hasProfileAndScores = version >= 5
    root.requireExactKeys(if (hasProfileAndScores) V5_ROOT_KEYS else BASE_ROOT_KEYS)

    // Metadata only, never stored in a column, and written by `Instant.now()` at
    // microsecond precision — so it keeps its exact parsed value (see `requireInstant`).
    val exportedAt = requireInstant(
        root.str("exportedAt", ImportMessages.BACKUP_SHAPE), now, ImportMessages.BACKUP_SHAPE
    )

    val entryValues = root.arr("entries")
    val sleepValues = root.arr("sleeps")
    val markerValues = root.arr("markers")
    val scoreValues = if (hasProfileAndScores) root.arr("externalScores") else emptyList()
    requireCounts(entryValues.size, sleepValues.size, markerValues.size, scoreValues.size)

    val entries = entryValues.map { readEntry(it, now) }
    val sleeps = sleepValues.map { readSleep(it, now) }
    val markers = markerValues.map { readMarker(it, now) }
    val scores = scoreValues.map { readScore(it, now, zone) }

    requireUniqueIds(entries.map(Entry::id))
    requireUniqueIds(sleeps.map(SleepInterval::id))
    requireUniqueIds(markers.map(Marker::id))
    requireUniqueIds(scores.map(ExternalScore::id))
    requireUniqueAssessments(scores)
    requireSleepStructure(sleeps)

    return BackupPayload(
        version = version,
        exportedAt = exportedAt,
        entries = entries,
        sleeps = sleeps,
        markers = markers,
        settings = readSettings(root.obj("settings"), version),
        // Absent before version 5. The preview discloses the empty name verbatim (D-2).
        profile = if (hasProfileAndScores) readProfile(root.obj("profile")) else UserProfile(),
        externalScores = scores
    )
}

private fun readEntry(value: JsonValue, now: Instant): Entry {
    val obj = value.asObj()
    obj.requireExactKeys(ENTRY_KEYS)
    return Entry(
        id = requireRowId(obj.long("id")),
        ts = requireInstantMillis(
            obj.str("timestamp", ImportMessages.BACKUP_SHAPE), now, ImportMessages.BACKUP_SHAPE
        ),
        value = requireIntensity(obj.int("intensity")),
        chips = requireChips(obj.stringArray("chips")),
        note = obj.nullableStr("note")?.let(::requireNote),
        kind = obj.nullableStr("kind")?.let { raw ->
            EntryKind.entries.firstOrNull { it.name == raw } ?: reject(ImportMessages.BACKUP_SHAPE)
        }
    )
}

private fun readSleep(value: JsonValue, now: Instant): SleepInterval {
    val obj = value.asObj()
    obj.requireExactKeys(SLEEP_KEYS)
    val startTs = requireInstantMillis(
        obj.str("start", ImportMessages.BACKUP_SHAPE), now, ImportMessages.BACKUP_SHAPE
    )
    val endTs = obj.nullableStr("end")?.let {
        requireInstantMillis(it, now, ImportMessages.BACKUP_SHAPE)
    }
    requireSleepBounds(startTs, endTs)
    return SleepInterval(id = requireRowId(obj.long("id")), startTs = startTs, endTs = endTs)
}

private fun readMarker(value: JsonValue, now: Instant): Marker {
    val obj = value.asObj()
    obj.requireExactKeys(MARKER_KEYS)
    return Marker(
        id = requireRowId(obj.long("id")),
        ts = requireInstantMillis(
            obj.str("timestamp", ImportMessages.BACKUP_SHAPE), now, ImportMessages.BACKUP_SHAPE
        ),
        text = requireMarkerText(obj.str("text", ImportMessages.BACKUP_SHAPE))
    )
}

private fun readProfile(obj: JsonValue.Obj): UserProfile {
    obj.requireExactKeys(PROFILE_KEYS)
    return UserProfile(
        displayName = requireSingleLine(
            obj.str("displayName", ImportMessages.BACKUP_SHAPE), MAX_DISPLAY_NAME_CODE_POINTS
        )
    )
}

/**
 * Provenance is required, must be exactly the frozen external value, and is re-asserted
 * rather than trusted. MindScale performs no arithmetic, comparison, or interpretation on
 * an imported total (D-8).
 */
private fun readScore(value: JsonValue, now: Instant, zone: ZoneId): ExternalScore {
    val obj = value.asObj()
    obj.requireExactKeys(SCORE_KEYS)
    val instrument = ExternalInstrument.entries
        .firstOrNull { it.name == obj.str("instrument", ImportMessages.BACKUP_SHAPE) }
        ?: reject(ImportMessages.BACKUP_SHAPE)
    if (obj.str("provenance", ImportMessages.BACKUP_SHAPE) !=
        ExternalScoreProvenance.EXTERNALLY_OBTAINED_USER_ENTERED.name
    ) {
        reject(ImportMessages.BACKUP_SHAPE)
    }
    val total = obj.int("total")
    if (total !in 0..instrument.maxTotal) reject(ImportMessages.UNSTORABLE_VALUE)
    return ExternalScore(
        id = requireRowId(obj.long("id")),
        instrument = instrument,
        total = total,
        assessedEpochDay = requireAssessedEpochDay(
            obj.str("assessedDate", ImportMessages.BACKUP_SHAPE), now, zone
        ),
        provenance = ExternalScoreProvenance.EXTERNALLY_OBTAINED_USER_ENTERED,
        enteredAt = requireInstantMillis(
            obj.str("enteredAt", ImportMessages.BACKUP_SHAPE), now, ImportMessages.BACKUP_SHAPE
        )
    )
}

/**
 * `checkinAt`, `sleepIntroShown`, and `anchorPromptDone` are in no backup version, and
 * `holdHours` is in no version-3 backup. All four take their model defaults and every one
 * of them is disclosed in the preview before anything is written (D-2, D-7).
 */
private fun readSettings(obj: JsonValue.Obj, version: Int): TrackSettings {
    obj.requireExactKeys(if (version >= 4) HOLD_SETTINGS_KEYS else BASE_SETTINGS_KEYS)
    val holdDuration = if (version >= 4) {
        HoldDuration.entries.firstOrNull { it.hours == obj.int("holdHours") }
            ?: reject(ImportMessages.BACKUP_SHAPE)
    } else {
        TrackSettings().holdDuration
    }
    return TrackSettings(
        id = 0,
        sleepOn = obj.bool("sleepOn"),
        askChips = obj.bool("askChips"),
        paused = obj.bool("paused"),
        checkinAt = TrackSettings().checkinAt,
        sleepIntroShown = TrackSettings().sleepIntroShown,
        themeMode = ThemeMode.entries
            .firstOrNull { it.name == obj.str("themeMode", ImportMessages.BACKUP_SHAPE) }
            ?: reject(ImportMessages.BACKUP_SHAPE),
        hourFormat = HourFormat.entries
            .firstOrNull { it.name == obj.str("hourFormat", ImportMessages.BACKUP_SHAPE) }
            ?: reject(ImportMessages.BACKUP_SHAPE),
        anchor2 = requireSingleLine(obj.str("anchor2", ImportMessages.BACKUP_SHAPE), MAX_ANCHOR_CODE_POINTS),
        anchor5 = requireSingleLine(obj.str("anchor5", ImportMessages.BACKUP_SHAPE), MAX_ANCHOR_CODE_POINTS),
        anchor8 = requireSingleLine(obj.str("anchor8", ImportMessages.BACKUP_SHAPE), MAX_ANCHOR_CODE_POINTS),
        onsetChips = requireOnsetWords(obj.stringArray("onsetChips")),
        hideNotes = obj.bool("hideNotes"),
        anchorPromptDone = TrackSettings().anchorPromptDone,
        holdDuration = holdDuration
    )
}

/** Must already satisfy the Phase 4 normalization contract; nothing is repaired here. */
private fun requireOnsetWords(values: List<String>): List<String> {
    if (values.isEmpty() || values.size > MAX_ONSET_WORDS) reject(ImportMessages.UNSTORABLE_VALUE)
    val words = values.map(::requireChip)
    val folded = words.map { it.lowercase(Locale.ROOT) }
    if (folded.toSet().size != folded.size) reject(ImportMessages.UNSTORABLE_VALUE)
    return words
}

private fun requireCounts(entries: Int, sleeps: Int, markers: Int, scores: Int) {
    if (entries > MAX_RECORDS_PER_COLLECTION || sleeps > MAX_RECORDS_PER_COLLECTION ||
        markers > MAX_RECORDS_PER_COLLECTION || scores > MAX_RECORDS_PER_COLLECTION
    ) {
        reject(ImportMessages.TOO_MANY_RECORDS)
    }
    if (entries + sleeps + markers + scores > MAX_TOTAL_RECORDS) {
        reject(ImportMessages.TOO_MANY_RECORDS)
    }
}

private fun requireUniqueIds(ids: List<Long>) {
    val duplicates = ids.size - ids.toSet().size
    if (duplicates > 0) reject(ImportMessages.duplicates(duplicates))
}

private fun requireUniqueAssessments(scores: List<ExternalScore>) {
    val keys = scores.map { it.instrument to it.assessedEpochDay }
    val duplicates = keys.size - keys.toSet().size
    if (duplicates > 0) reject(ImportMessages.duplicates(duplicates))
}

private fun JsonValue.asObj(): JsonValue.Obj =
    this as? JsonValue.Obj ?: reject(ImportMessages.BACKUP_SHAPE)

private fun JsonValue.Obj.requireExactKeys(expected: Set<String>) {
    if (members.keys != expected) reject(ImportMessages.BACKUP_SHAPE)
}

private fun JsonValue.Obj.str(key: String, failure: String): String =
    (members[key] as? JsonValue.Str)?.value ?: reject(failure)

private fun JsonValue.Obj.nullableStr(key: String): String? = when (val value = members[key]) {
    is JsonValue.Str -> value.value
    JsonValue.Null -> null
    else -> reject(ImportMessages.BACKUP_SHAPE)
}

private fun JsonValue.Obj.bool(key: String): Boolean =
    (members[key] as? JsonValue.Bool)?.value ?: reject(ImportMessages.BACKUP_SHAPE)

private fun JsonValue.Obj.long(key: String): Long =
    (members[key] as? JsonValue.Num)?.asLongOrNull() ?: reject(ImportMessages.BACKUP_SHAPE)

private fun JsonValue.Obj.int(key: String): Int =
    (members[key] as? JsonValue.Num)?.asIntOrNull() ?: reject(ImportMessages.BACKUP_SHAPE)

private fun JsonValue.Obj.obj(key: String): JsonValue.Obj =
    (members[key] as? JsonValue.Obj) ?: reject(ImportMessages.BACKUP_SHAPE)

private fun JsonValue.Obj.arr(key: String): List<JsonValue> =
    (members[key] as? JsonValue.Arr)?.items ?: reject(ImportMessages.BACKUP_SHAPE)

private fun JsonValue.Obj.stringArray(key: String): List<String> =
    arr(key).map { (it as? JsonValue.Str)?.value ?: reject(ImportMessages.BACKUP_SHAPE) }
