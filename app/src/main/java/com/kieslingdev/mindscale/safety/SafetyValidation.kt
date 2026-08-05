package com.kieslingdev.mindscale.safety

import com.kieslingdev.mindscale.data.SafetyPlanStep
import com.kieslingdev.mindscale.data.allowsPhone

/**
 * The one place a safety-plan value is judged storable
 * (`docs/specs/SPEC-safety-card.md`, D-5).
 *
 * The Safety screen and the backup importer both call these functions, so a line typed by
 * the user and a line read out of a backup file are held to exactly the same rules. Pure
 * and Android-free, so every rule is covered by JVM tests.
 */

const val MAX_PLAN_TEXT_CODE_POINTS = 200
const val MAX_PLAN_PHONE_CODE_POINTS = 40
const val MAX_PLAN_ITEMS_PER_STEP = 10
const val MAX_PLAN_ITEMS_TOTAL = 60

/** Characters a phone number may contain. Letters are refused rather than stripped. */
private const val PHONE_CHARACTERS = "0123456789+-().# "

/** Renders as a break but survives a naive `\n`/`\r` check. */
private const val LINE_SEPARATOR = ' '
private const val PARAGRAPH_SEPARATOR = ' '

sealed interface PlanFieldError {
    data object Empty : PlanFieldError
    data object TooLong : PlanFieldError
    data object MultiLine : PlanFieldError
    data object BadPhone : PlanFieldError
    data object PhoneNotAllowed : PlanFieldError
    data object StepFull : PlanFieldError
    data object PlanFull : PlanFieldError
}

/**
 * Why a purpose-built type instead of `kotlin.Result`: `Result`'s failure channel requires
 * a `Throwable`, and a value the user mistyped is not an exception. This matches the
 * `ParseResult` shape Phase 12 already uses for the same reason.
 */
sealed interface PlanFieldResult<out T> {
    data class Valid<T>(val value: T) : PlanFieldResult<T>
    data class Invalid(val error: PlanFieldError) : PlanFieldResult<Nothing>
}

fun PlanFieldError.message(): String = when (this) {
    PlanFieldError.Empty -> "Write something first."
    PlanFieldError.TooLong -> "That is longer than $MAX_PLAN_TEXT_CODE_POINTS characters."
    PlanFieldError.MultiLine -> "Keep this to one line."
    PlanFieldError.BadPhone -> "That does not look like a phone number."
    PlanFieldError.PhoneNotAllowed -> "This step does not hold phone numbers."
    PlanFieldError.StepFull -> "This step already holds $MAX_PLAN_ITEMS_PER_STEP lines."
    PlanFieldError.PlanFull -> "Your plan already holds $MAX_PLAN_ITEMS_TOTAL lines."
}

/**
 * Trims and accepts one non-blank single line. Nothing is repaired: a value that breaks a
 * rule is refused so the user sees what they typed, rather than having it silently stored
 * as something they did not write.
 */
fun validatePlanText(raw: String): PlanFieldResult<String> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return PlanFieldResult.Invalid(PlanFieldError.Empty)
    if (trimmed.any {
            it == '\n' || it == '\r' || it == LINE_SEPARATOR || it == PARAGRAPH_SEPARATOR
        }
    ) {
        return PlanFieldResult.Invalid(PlanFieldError.MultiLine)
    }
    if (trimmed.codePointLength() > MAX_PLAN_TEXT_CODE_POINTS) {
        return PlanFieldResult.Invalid(PlanFieldError.TooLong)
    }
    return PlanFieldResult.Valid(trimmed)
}

/**
 * Blank means "no number" and is valid on every step. A number is valid only on the two
 * steps that name a person; anywhere else it is refused rather than dropped, so an
 * imported file cannot quietly lose a field it claimed to carry.
 */
fun validatePlanPhone(raw: String, step: SafetyPlanStep): PlanFieldResult<String?> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return PlanFieldResult.Valid(null)
    if (!step.allowsPhone) return PlanFieldResult.Invalid(PlanFieldError.PhoneNotAllowed)
    if (trimmed.codePointLength() > MAX_PLAN_PHONE_CODE_POINTS) {
        return PlanFieldResult.Invalid(PlanFieldError.TooLong)
    }
    if (trimmed.any { it !in PHONE_CHARACTERS } || trimmed.none { it.isDigit() }) {
        return PlanFieldResult.Invalid(PlanFieldError.BadPhone)
    }
    return PlanFieldResult.Valid(trimmed)
}

internal fun String.codePointLength(): Int = codePointCount(0, length)
