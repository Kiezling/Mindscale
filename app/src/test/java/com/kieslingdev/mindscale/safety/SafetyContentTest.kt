package com.kieslingdev.mindscale.safety

import com.kieslingdev.mindscale.data.SafetyPlanStep
import com.kieslingdev.mindscale.data.allowsPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Literal-text coverage of the Safety card, the same pattern Phase 11 used for the
 * clinician-summary grammar. Every string is compared whole, never by substring: this copy
 * is frozen by `docs/specs/SPEC-safety-card.md`, D-3 and D-4, and a paraphrase during a
 * later refactor is exactly the failure these assertions exist to catch.
 */
class SafetyContentTest {

    @Test
    fun stepsAreTheSixCanonicalStanleyBrownStepsInCanonicalOrder() {
        assertEquals(6, SAFETY_STEPS.size)
        assertEquals(SafetyPlanStep.entries.toList(), SAFETY_STEPS.map { it.step })

        // 1. Warning signs
        assertEquals(SafetyPlanStep.WARNING_SIGNS, SAFETY_STEPS[0].step)
        assertEquals("What I notice first", SAFETY_STEPS[0].heading)
        assertEquals(
            "The earliest signs that things are turning, written now while it is easy to think.",
            SAFETY_STEPS[0].hint
        )

        // 2. Internal coping strategies
        assertEquals(SafetyPlanStep.INTERNAL_COPING, SAFETY_STEPS[1].step)
        assertEquals("What I can do on my own", SAFETY_STEPS[1].heading)
        assertEquals(
            "Things that have helped without needing anyone else. Easiest first.",
            SAFETY_STEPS[1].hint
        )

        // 3. People and social settings that provide distraction
        assertEquals(SafetyPlanStep.DISTRACTION, SAFETY_STEPS[2].step)
        assertEquals("Where I can go", SAFETY_STEPS[2].heading)
        assertEquals(
            "Places and people that take up attention without needing a conversation.",
            SAFETY_STEPS[2].hint
        )

        // 4. People whom I can ask for help
        assertEquals(SafetyPlanStep.PEOPLE_FOR_HELP, SAFETY_STEPS[3].step)
        assertEquals("Who I can ask for help", SAFETY_STEPS[3].heading)
        assertEquals(
            "You do not have to explain anything. One word is enough.",
            SAFETY_STEPS[3].hint
        )

        // 5. Professionals or agencies I can contact
        assertEquals(SafetyPlanStep.PROFESSIONALS, SAFETY_STEPS[4].step)
        assertEquals("Professionals I can contact", SAFETY_STEPS[4].heading)
        assertEquals("Doctor, therapist, clinic, or crisis line.", SAFETY_STEPS[4].hint)

        // 6. Making the environment safe
        assertEquals(SafetyPlanStep.ENVIRONMENT_SAFETY, SAFETY_STEPS[5].step)
        assertEquals("Making my space safer", SAFETY_STEPS[5].heading)
        assertEquals(
            "What goes somewhere else for now, and who holds it.",
            SAFETY_STEPS[5].hint
        )
    }

    @Test
    fun onlyTheTwoContactStepsHoldPhoneNumbers() {
        assertEquals(
            listOf(SafetyPlanStep.PEOPLE_FOR_HELP, SafetyPlanStep.PROFESSIONALS),
            SafetyPlanStep.entries.filter { it.allowsPhone }
        )
    }

    @Test
    fun crisisResourceCopyIsExactlyAsVerifiedOn2026_08_05() {
        assertEquals(
            "988 — United States and Canada",
            SafetyCopy.LIFELINE_NAME
        )
        assertEquals("Call 988", SafetyCopy.LIFELINE_CALL)
        assertEquals("Text 988", SafetyCopy.LIFELINE_TEXT)
        assertEquals(
            "Calling or texting 988 reaches the 988 Suicide & Crisis Lifeline in the " +
                "United States and the 9-8-8 Suicide Crisis Helpline in Canada. Free and " +
                "confidential, 24 hours a day, every day. In Canada it is available in " +
                "English and French. For TTY in the United States, use your preferred " +
                "relay service or dial 711 then 988.",
            SafetyCopy.LIFELINE_DETAIL
        )
        assertEquals("Anywhere else", SafetyCopy.ELSEWHERE_NAME)
        assertEquals("Open findahelpline.com", SafetyCopy.ELSEWHERE_ACTION)
        assertEquals(
            "988 only connects in the United States and Canada. Find A Helpline lists " +
                "free crisis lines in many other countries. This button opens your " +
                "browser; MindScale itself never connects to the internet.",
            SafetyCopy.ELSEWHERE_DETAIL
        )
        assertEquals(
            "If someone is in immediate physical danger, a local emergency number is the " +
                "fastest route. In the United States and Canada that is 911. There is no " +
                "button for it here, so it cannot be dialled by accident.",
            SafetyCopy.EMERGENCY
        )
        assertEquals("These numbers were checked on 5 August 2026.", SafetyCopy.VERIFIED_ON)
        assertEquals("https://findahelpline.com", FIND_A_HELPLINE_URL)
    }

    @Test
    fun screenCopyIsExact() {
        assertEquals(
            "This card is here whenever you want it. Nothing you record opens it, and " +
                "nothing you record changes what it says.",
            SafetyCopy.SCREEN_INTRO
        )
        assertEquals(
            "MindScale keeps this card on your device and does nothing else with it. It " +
                "cannot tell how you are, alert anyone, or get help for you. Only the " +
                "buttons you tap on this screen do anything.",
            SafetyCopy.HONESTY
        )
        assertEquals("Always open", SafetyCopy.RESOURCES_HEADING)
        assertEquals("Your plan", SafetyCopy.PLAN_HEADING)
        assertEquals(
            "Written by you, in advance, for a moment when thinking is hard. The steps " +
                "are in a set order; within each one, put the easiest thing first.",
            SafetyCopy.PLAN_INTRO
        )
        assertEquals(
            "Nothing written here yet. You can add to any step at any time, or leave it empty.",
            SafetyCopy.PLAN_EMPTY
        )
        assertEquals("Nothing written for this step.", SafetyCopy.STEP_EMPTY)
        assertEquals("If this is a hard moment", SafetyCopy.TRACK_LINK)
        assertEquals("Open the Safety card", SafetyCopy.TRACK_LINK_DESCRIPTION)
        assertEquals("Safety card", SafetyCopy.PROFILE_ROW)
        assertEquals("Safety", SafetyCopy.TOP_BAR_TITLE)
    }

    @Test
    fun resourcesAreTwoInFixedOrderWithActionsBeforeDetail() {
        assertEquals(listOf("lifeline", "elsewhere"), SAFETY_RESOURCES.map { it.key })

        val lifeline = SAFETY_RESOURCES[0]
        assertEquals(
            listOf(SafetyAction.Dial("988"), SafetyAction.Text("988")),
            lifeline.actions.map { it.action }
        )
        assertEquals(SafetyCopy.LIFELINE_DETAIL, lifeline.detail)

        val elsewhere = SAFETY_RESOURCES[1]
        assertEquals(
            listOf(SafetyAction.OpenPage(FIND_A_HELPLINE_URL)),
            elsewhere.actions.map { it.action }
        )
    }

    /**
     * There is deliberately no button for an emergency number: a pre-filled emergency
     * dialer one accidental tap away, on a screen someone opened while distressed, is a
     * foreseeable harm (D-3). The sentence stays; the action does not.
     */
    @Test
    fun noResourceActionDialsAnEmergencyNumber() {
        val emergencyNumbers = setOf("911", "112", "999", "000")
        SAFETY_RESOURCES.flatMap { it.actions }.forEach { action ->
            val number = when (val target = action.action) {
                is SafetyAction.Dial -> target.number
                is SafetyAction.Text -> target.number
                is SafetyAction.OpenPage -> ""
            }
            assertFalse(
                "No Safety card action may dial an emergency number",
                number in emergencyNumbers
            )
        }
    }

    /**
     * MindScale must never claim it monitors, alerts, or responds. The card may say what
     * it *cannot* do, so only affirmative claim wording is banned.
     */
    @Test
    fun cardCopyMakesNoMonitoringOrEfficacyClaim() {
        val banned = listOf(
            "we will", "we can", "mindscale will notify", "mindscale notifies",
            "we notify", "automatically", "detects", "detect ", "monitors",
            "is monitoring", "proven", "clinically", "evidence-based",
            "science-backed", "most protective", "guarantee"
        )
        val copy = (
            listOf(
                SafetyCopy.SCREEN_INTRO, SafetyCopy.HONESTY, SafetyCopy.RESOURCES_HEADING,
                SafetyCopy.LIFELINE_NAME, SafetyCopy.LIFELINE_DETAIL,
                SafetyCopy.ELSEWHERE_NAME, SafetyCopy.ELSEWHERE_DETAIL,
                SafetyCopy.EMERGENCY, SafetyCopy.VERIFIED_ON, SafetyCopy.PLAN_HEADING,
                SafetyCopy.PLAN_INTRO, SafetyCopy.PLAN_EMPTY, SafetyCopy.STEP_EMPTY
            ) + SAFETY_STEPS.map { it.heading } + SAFETY_STEPS.map { it.hint }
            ).joinToString("\n").lowercase()

        banned.forEach { phrase ->
            assertFalse("Safety copy must not contain \"$phrase\"", copy.contains(phrase))
        }
        assertTrue(
            "The card must say plainly that it cannot alert anyone",
            SafetyCopy.HONESTY.contains("cannot tell how you are, alert anyone, or get help for you")
        )
    }
}
