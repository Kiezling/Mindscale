package com.kieslingdev.mindscale.safety

import com.kieslingdev.mindscale.data.SafetyPlanStep

/**
 * Every user-visible string on the Safety card, frozen verbatim by
 * `docs/specs/SPEC-safety-card.md`, D-3, D-4, D-6, and D-7.
 *
 * This file is pure, Android-free, and reads nothing the user has recorded. That is
 * structural, not incidental: no rating, episode, count, streak, gap, or date may change
 * one character of what this card says or the order in which it says it (D-11,
 * Invariant 1). MindScale does not assess risk and must not appear to.
 *
 * **Every crisis number and coverage claim below was verified on 2026-08-05** from the
 * operator's own site or the funding agency — not carried forward from memory. Before
 * changing any of it, re-verify from the sources listed in the spec and update
 * [SafetyCopy.VERIFIED_ON] in the same edit.
 */
object SafetyCopy {
    const val SCREEN_INTRO =
        "This card is here whenever you want it. Nothing you record opens it, and " +
            "nothing you record changes what it says."

    const val HONESTY =
        "MindScale keeps this card on your device and does nothing else with it. It " +
            "cannot tell how you are, alert anyone, or get help for you. Only the buttons " +
            "you tap on this screen do anything."

    const val RESOURCES_HEADING = "Always open"

    const val LIFELINE_NAME = "988 — United States and Canada"
    const val LIFELINE_CALL = "Call 988"
    const val LIFELINE_TEXT = "Text 988"
    const val LIFELINE_DETAIL =
        "Calling or texting 988 reaches the 988 Suicide & Crisis Lifeline in the United " +
            "States and the 9-8-8 Suicide Crisis Helpline in Canada. Free and " +
            "confidential, 24 hours a day, every day. In Canada it is available in " +
            "English and French. For TTY in the United States, use your preferred relay " +
            "service or dial 711 then 988."

    const val ELSEWHERE_NAME = "Anywhere else"
    const val ELSEWHERE_ACTION = "Open findahelpline.com"
    const val ELSEWHERE_DETAIL =
        "988 only connects in the United States and Canada. Find A Helpline lists free " +
            "crisis lines in many other countries. This button opens your browser; " +
            "MindScale itself never connects to the internet."

    const val EMERGENCY =
        "If someone is in immediate physical danger, a local emergency number is the " +
            "fastest route. In the United States and Canada that is 911. There is no " +
            "button for it here, so it cannot be dialled by accident."

    const val VERIFIED_ON = "These numbers were checked on 5 August 2026."

    const val PLAN_HEADING = "Your plan"
    const val PLAN_INTRO =
        "Written by you, in advance, for a moment when thinking is hard. The steps are " +
            "in a set order; within each one, put the easiest thing first."
    const val PLAN_EMPTY =
        "Nothing written here yet. You can add to any step at any time, or leave it empty."
    const val STEP_EMPTY = "Nothing written for this step."

    const val TRACK_LINK = "If this is a hard moment"
    const val TRACK_LINK_DESCRIPTION = "Open the Safety card"
    const val PROFILE_ROW = "Safety card"
    const val TOP_BAR_TITLE = "Safety"

    const val ADD_ITEM = "Add"
    const val EDIT_ITEM = "Edit"
    const val DELETE_ITEM = "Delete"
    const val TEXT_FIELD_LABEL = "What to write down"
    const val PHONE_FIELD_LABEL = "Phone number (optional)"
    const val SAVE_ITEM = "Save"
    const val CANCEL = "Cancel"
    const val DELETE_TITLE = "Delete this line?"
    const val DELETE_CONFIRM = "Delete"
    const val LOAD_FAILED = "Could not open your safety plan."
    const val SAVE_FAILED = "Could not save that. Nothing was changed."
    const val RETRY = "Retry"
    const val STALE_ITEM = "That line is no longer here."

    fun deleteMessage(text: String) = "Permanently delete \"$text\" from your plan?"

    fun callContact(name: String) = "Call $name"

    fun dialUnavailable(number: String) =
        "No app on this device can open the dialer. The number is $number."

    fun textUnavailable(number: String) =
        "No app on this device can send a text message. The number is $number."

    fun pageUnavailable(url: String) =
        "No app on this device can open a web page. The address is $url."
}

/** One Stanley-Brown step as it is presented. */
data class SafetyStepContent(
    val step: SafetyPlanStep,
    val heading: String,
    val hint: String
)

/**
 * The six canonical Stanley-Brown steps in canonical order (D-4). The plain-language
 * headings map one-to-one onto the clinical step names recorded in the spec; the order is
 * [SafetyPlanStep.entries] and is asserted by test, not by convention.
 */
val SAFETY_STEPS: List<SafetyStepContent> = listOf(
    // 1. Warning signs
    SafetyStepContent(
        SafetyPlanStep.WARNING_SIGNS,
        "What I notice first",
        "The earliest signs that things are turning, written now while it is easy to think."
    ),
    // 2. Internal coping strategies
    SafetyStepContent(
        SafetyPlanStep.INTERNAL_COPING,
        "What I can do on my own",
        "Things that have helped without needing anyone else. Easiest first."
    ),
    // 3. People and social settings that provide distraction
    SafetyStepContent(
        SafetyPlanStep.DISTRACTION,
        "Where I can go",
        "Places and people that take up attention without needing a conversation."
    ),
    // 4. People whom I can ask for help
    SafetyStepContent(
        SafetyPlanStep.PEOPLE_FOR_HELP,
        "Who I can ask for help",
        "You do not have to explain anything. One word is enough."
    ),
    // 5. Professionals or agencies I can contact
    SafetyStepContent(
        SafetyPlanStep.PROFESSIONALS,
        "Professionals I can contact",
        "Doctor, therapist, clinic, or crisis line."
    ),
    // 6. Making the environment safe
    SafetyStepContent(
        SafetyPlanStep.ENVIRONMENT_SAFETY,
        "Making my space safer",
        "What goes somewhere else for now, and who holds it."
    )
)

/** A button on a crisis resource. The label is what is read and what is tapped. */
data class SafetyResourceAction(
    val label: String,
    val contentDescription: String,
    val action: SafetyAction
)

/**
 * A crisis resource. Order is fixed and never depends on anything inferred about the user
 * (D-3): MindScale has no network, no location permission, and a device locale is a
 * language preference rather than a country of residence, so coverage is stated in plain
 * words instead of guessed.
 */
data class SafetyResource(
    val key: String,
    val name: String,
    val actions: List<SafetyResourceAction>,
    val detail: String
)

const val FIND_A_HELPLINE_URL = "https://findahelpline.com"

val SAFETY_RESOURCES: List<SafetyResource> = listOf(
    SafetyResource(
        key = "lifeline",
        name = SafetyCopy.LIFELINE_NAME,
        actions = listOf(
            SafetyResourceAction(
                SafetyCopy.LIFELINE_CALL, SafetyCopy.LIFELINE_CALL, SafetyAction.Dial("988")
            ),
            SafetyResourceAction(
                SafetyCopy.LIFELINE_TEXT, SafetyCopy.LIFELINE_TEXT, SafetyAction.Text("988")
            )
        ),
        detail = SafetyCopy.LIFELINE_DETAIL
    ),
    SafetyResource(
        key = "elsewhere",
        name = SafetyCopy.ELSEWHERE_NAME,
        actions = listOf(
            SafetyResourceAction(
                SafetyCopy.ELSEWHERE_ACTION,
                SafetyCopy.ELSEWHERE_ACTION,
                SafetyAction.OpenPage(FIND_A_HELPLINE_URL)
            )
        ),
        detail = SafetyCopy.ELSEWHERE_DETAIL
    )
)
