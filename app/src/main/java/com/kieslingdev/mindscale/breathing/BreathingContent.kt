package com.kieslingdev.mindscale.breathing

/**
 * Every user-visible string in the paced-breathing feature, frozen verbatim
 * (`docs/specs/SPEC-paced-breathing.md`, D-12).
 *
 * The copy is the product here. The evidence this feature would normally rest on does not
 * survive placebo control -- the team behind the promising meta-analysis then ran two
 * placebo-controlled trials that were both null, and a separate trial found a fast pace
 * equal to a slow one -- so nothing below claims, implies, or hints that paced breathing
 * does anything. No efficacy verb, no science-backing language, no protocol name, no
 * citation, no rate, no effect size.
 *
 * [INSTRUCTIONS] describes a mechanic. [NO_CLAIM] is the explicit refusal to claim.
 * [RECORDING] discloses what MindScale writes, including the one case where it writes
 * nothing. Each is compared by exact string equality in tests.
 */
object BreathingCopy {
    const val TOP_BAR_TITLE = "Paced breathing"

    const val TRACK_LINK = "Paced breathing"
    const val TRACK_LINK_DESCRIPTION = "Open the paced breathing circle"

    const val SETTING_TITLE = "Paced breathing"
    const val SETTING_DESCRIPTION =
        "A breathing circle you can open. Never offered to you unprompted."

    const val INSTRUCTIONS =
        "Through the nose, following the circle. Out for a little longer than in. " +
            "Stop whenever you like."

    const val NO_CLAIM =
        "MindScale makes no claim about what this does. It is a circle that keeps a pace."

    /**
     * The second sentence is the honest disclosure of the one gap in D-6: a session that is
     * interrupted by the app being killed is not written. Stating it here means a user can
     * audit the behaviour instead of discovering a missing row later.
     */
    const val RECORDING =
        "Each session is saved with its start and end time and appears in your exports. " +
            "A session is only saved if MindScale stays open until it ends."

    const val CHOOSE = "Choose a length"

    const val CUE_IN = "In"
    const val CUE_OUT = "Out"

    // Spoken rather than shown. "In" and "Out" read poorly out of context.
    const val CUE_IN_DESCRIPTION = "Breathe in"
    const val CUE_OUT_DESCRIPTION = "Breathe out"

    const val DONE = "Done"
    const val CLOSE = "Close"
    const val STOP = "Stop"

    const val SAVE_FAILED = "That session could not be saved."

    /** Button face: compact, because four of them sit in a row. */
    fun lengthLabel(minutes: Int): String = "$minutes min"

    /** Spoken form of a length button, and the on-screen readout while a session runs. */
    fun lengthDescription(minutes: Int): String =
        if (minutes == 1) "1 minute" else "$minutes minutes"

    /**
     * The chosen length, shown as static text while running. Deliberately not a countdown:
     * a running clock would turn the screen into a compliance monitor. The prototype's
     * version was always plural and read "1 minutes".
     */
    fun runningLength(minutes: Int): String = lengthDescription(minutes)

    fun cueFor(phase: BreathPhase): String = when (phase) {
        BreathPhase.INHALE -> CUE_IN
        BreathPhase.EXHALE -> CUE_OUT
    }

    fun cueDescriptionFor(phase: BreathPhase): String = when (phase) {
        BreathPhase.INHALE -> CUE_IN_DESCRIPTION
        BreathPhase.EXHALE -> CUE_OUT_DESCRIPTION
    }
}
