package com.kieslingdev.mindscale.safety

/**
 * What a tap on the Safety card asks the platform to do — always a hand-off, never a call
 * (`docs/specs/SPEC-safety-card.md`, D-7).
 *
 * This is a pure description. Turning it into an `Intent` happens at exactly one Android
 * edge, `SafetyIntents.intentFor`, so the rule that MindScale never places a call is one
 * reviewable function rather than a habit spread across composables.
 */
sealed interface SafetyAction {
    /** Opens the dialer pre-filled. The user still taps the call button themselves. */
    data class Dial(val number: String) : SafetyAction

    /** Opens the messaging app with no body. The user writes and sends. */
    data class Text(val number: String) : SafetyAction

    /** Opens the user's browser. MindScale itself never opens a socket. */
    data class OpenPage(val url: String) : SafetyAction
}

object SafetyActions {
    /** Characters a dialer meaningfully accepts; everything else is presentation. */
    private const val DIALABLE = "0123456789+*#,;"

    /**
     * Reduces a number the user typed however they liked — `(555) 010-0199`,
     * `+1 555 010 0199` — to what a `tel:` URI should carry.
     *
     * Returns null when nothing dialable remains, and a caller that gets null shows no
     * Call button at all rather than an action that cannot work.
     */
    fun dialString(raw: String): String? {
        val kept = raw.filter { it in DIALABLE }
        return if (kept.any { it.isDigit() }) kept else null
    }

    /** Shown when no app on the device can handle the hand-off (D-7). */
    fun unavailableMessage(action: SafetyAction): String = when (action) {
        is SafetyAction.Dial -> SafetyCopy.dialUnavailable(action.number)
        is SafetyAction.Text -> SafetyCopy.textUnavailable(action.number)
        is SafetyAction.OpenPage -> SafetyCopy.pageUnavailable(action.url)
    }
}
