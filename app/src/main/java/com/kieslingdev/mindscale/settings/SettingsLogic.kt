package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.DEFAULT_ONSET_CHIPS
import com.kieslingdev.mindscale.data.TrackSettings
import java.util.Locale

// Shared with Phase 12 import so an imported value must satisfy the same limits the
// app's own write paths enforce (docs/specs/SPEC-import-restore.md, D-5).
const val MAX_ANCHOR_CODE_POINTS = 160
const val MAX_CHIP_CODE_POINTS = 32
const val MAX_ONSET_WORDS = 20

data class AnchorDraft(val anchor2: String = "", val anchor5: String = "", val anchor8: String = "")

sealed interface ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>
    data class Invalid(val message: String) : ValidationResult<Nothing>
}

fun validateAnchors(draft: AnchorDraft): ValidationResult<AnchorDraft> {
    val normalized = AnchorDraft(draft.anchor2.trim(), draft.anchor5.trim(), draft.anchor8.trim())
    val tooLong = listOf(normalized.anchor2, normalized.anchor5, normalized.anchor8)
        .any { it.codePointCount() > MAX_ANCHOR_CODE_POINTS }
    return if (tooLong) {
        ValidationResult.Invalid("Each anchor must be 160 characters or fewer.")
    } else {
        ValidationResult.Valid(normalized)
    }
}

fun normalizeOnsetWords(draft: String): ValidationResult<List<String>> {
    val values = draft.split(',', '\n')
        .map(String::trim)
        .filter(String::isNotEmpty)
    val unique = linkedMapOf<String, String>()
    values.forEach { word -> unique.putIfAbsent(word.lowercase(Locale.ROOT), word) }
    val normalized = unique.values.toList()
    return when {
        normalized.isEmpty() -> ValidationResult.Invalid("Keep at least one onset word.")
        normalized.size > MAX_ONSET_WORDS -> ValidationResult.Invalid("Use no more than 20 onset words.")
        normalized.any { it.codePointCount() > MAX_CHIP_CODE_POINTS } ->
            ValidationResult.Invalid("Each onset word must be 32 characters or fewer.")
        else -> ValidationResult.Valid(normalized)
    }
}

private fun String.codePointCount(): Int = codePointCount(0, length)

fun anchorFor(value: Int, settings: TrackSettings): String = when (value) {
    in 1..3 -> settings.anchor2
    in 4..6 -> settings.anchor5
    in 7..10 -> settings.anchor8
    else -> ""
}.trim()

fun vocabularyForEntry(settings: TrackSettings, existing: Collection<String>): List<String> =
    (settings.onsetChips + existing).distinctBy { it.lowercase(Locale.ROOT) }

fun defaultChipDraft(): String = DEFAULT_ONSET_CHIPS.joinToString(", ")
