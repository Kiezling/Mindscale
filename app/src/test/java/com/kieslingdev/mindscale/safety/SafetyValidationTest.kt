package com.kieslingdev.mindscale.safety

import com.kieslingdev.mindscale.data.SafetyPlanStep
import org.junit.Assert.assertEquals
import org.junit.Test

class SafetyValidationTest {

    private fun text(raw: String) = validatePlanText(raw)
    private fun phone(raw: String, step: SafetyPlanStep) = validatePlanPhone(raw, step)

    @Test
    fun textIsTrimmedAndAccepted() {
        assertEquals(
            PlanFieldResult.Valid("Call Sam"),
            text("  Call Sam  ")
        )
    }

    @Test
    fun blankAndWhitespaceOnlyTextIsRefused() {
        assertEquals(PlanFieldResult.Invalid(PlanFieldError.Empty), text(""))
        assertEquals(PlanFieldResult.Invalid(PlanFieldError.Empty), text("   \t  "))
    }

    @Test
    fun multiLineTextIsRefusedIncludingUnicodeSeparators() {
        assertEquals(PlanFieldResult.Invalid(PlanFieldError.MultiLine), text("one\ntwo"))
        assertEquals(PlanFieldResult.Invalid(PlanFieldError.MultiLine), text("one\rtwo"))
        assertEquals(PlanFieldResult.Invalid(PlanFieldError.MultiLine), text("one two"))
        assertEquals(PlanFieldResult.Invalid(PlanFieldError.MultiLine), text("one two"))
    }

    @Test
    fun textLengthBoundaryIsExact() {
        val exact = "a".repeat(MAX_PLAN_TEXT_CODE_POINTS)
        assertEquals(PlanFieldResult.Valid(exact), text(exact))
        assertEquals(
            PlanFieldResult.Invalid(PlanFieldError.TooLong),
            text("a".repeat(MAX_PLAN_TEXT_CODE_POINTS + 1))
        )
    }

    /** Surrogate pairs count as one character, not two. */
    @Test
    fun textLengthCountsCodePointsNotUtf16Units() {
        val emoji = "🙂".repeat(MAX_PLAN_TEXT_CODE_POINTS)
        assertEquals(PlanFieldResult.Valid(emoji), text(emoji))
        assertEquals(
            PlanFieldResult.Invalid(PlanFieldError.TooLong),
            text("🙂".repeat(MAX_PLAN_TEXT_CODE_POINTS + 1))
        )
    }

    @Test
    fun blankPhoneMeansNoNumberOnEveryStep() {
        SafetyPlanStep.entries.forEach { step ->
            assertEquals(PlanFieldResult.Valid(null), phone("", step))
            assertEquals(PlanFieldResult.Valid(null), phone("   ", step))
        }
    }

    @Test
    fun formattedNumbersAreAcceptedOnContactSteps() {
        assertEquals(
            PlanFieldResult.Valid("+1 (555) 010-0199"),
            phone("  +1 (555) 010-0199 ", SafetyPlanStep.PEOPLE_FOR_HELP)
        )
        assertEquals(
            PlanFieldResult.Valid("555.0100"),
            phone("555.0100", SafetyPlanStep.PROFESSIONALS)
        )
    }

    @Test
    fun aNumberOnANonContactStepIsRefusedRatherThanDropped() {
        listOf(
            SafetyPlanStep.WARNING_SIGNS,
            SafetyPlanStep.INTERNAL_COPING,
            SafetyPlanStep.DISTRACTION,
            SafetyPlanStep.ENVIRONMENT_SAFETY
        ).forEach { step ->
            assertEquals(
                PlanFieldResult.Invalid(PlanFieldError.PhoneNotAllowed),
                phone("5550100", step)
            )
        }
    }

    @Test
    fun lettersAndDigitlessNumbersAreRefused() {
        assertEquals(
            PlanFieldResult.Invalid(PlanFieldError.BadPhone),
            phone("call Sam", SafetyPlanStep.PEOPLE_FOR_HELP)
        )
        assertEquals(
            PlanFieldResult.Invalid(PlanFieldError.BadPhone),
            phone("+-()", SafetyPlanStep.PEOPLE_FOR_HELP)
        )
    }

    @Test
    fun phoneLengthBoundaryIsExact() {
        val exact = "1".repeat(MAX_PLAN_PHONE_CODE_POINTS)
        assertEquals(
            PlanFieldResult.Valid(exact),
            phone(exact, SafetyPlanStep.PEOPLE_FOR_HELP)
        )
        assertEquals(
            PlanFieldResult.Invalid(PlanFieldError.TooLong),
            phone("1".repeat(MAX_PLAN_PHONE_CODE_POINTS + 1), SafetyPlanStep.PEOPLE_FOR_HELP)
        )
    }
}
