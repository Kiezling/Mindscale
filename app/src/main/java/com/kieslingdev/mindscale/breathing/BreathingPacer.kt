package com.kieslingdev.mindscale.breathing

/**
 * The pacing mechanic, as a pure function of elapsed time
 * (`docs/specs/SPEC-paced-breathing.md`, D-1, D-6).
 *
 * Purity is not tidiness here, it is the rotation strategy: because the current phase is
 * derived from elapsed monotonic milliseconds rather than accumulated in mutable state, a
 * configuration change re-derives it and no session is ended, restarted, or double-counted.
 *
 * The cadence is one frozen constant with no setter, no override, and no alternative. The
 * product source permits ~5-6 breaths per minute with an equal-or-longer exhale, and
 * 4500 ms in plus 6500 ms out is 5.45 breaths per minute with a longer exhale. There is no
 * evidence base that could justify preferring some other figure inside that band -- two
 * placebo-controlled trials of exactly this kind of cadence were null -- so the number is
 * adopted and fixed rather than made configurable.
 */

/**
 * Exactly two phases. There is no hold, no pause between phases, and no third state.
 * "Never breath retention" is therefore a property of this type rather than a promise in a
 * comment: a retention phase cannot be configured, only added, and adding one changes a
 * frozen interface and fails review (Invariant 1).
 */
enum class BreathPhase { INHALE, EXHALE }

/** Where the pacer is right now. Everything the screen draws comes from this. */
data class PacerFrame(
    val phase: BreathPhase,
    val phaseElapsedMillis: Long,
    val phaseTotalMillis: Long,
    val completedCycles: Int
) {
    /** Milliseconds until the next phase boundary; what the ViewModel sleeps for. */
    val phaseRemainingMillis: Long get() = phaseTotalMillis - phaseElapsedMillis
}

object BreathingPacer {
    const val INHALE_MILLIS = 4_500L
    const val EXHALE_MILLIS = 6_500L
    const val CYCLE_MILLIS = INHALE_MILLIS + EXHALE_MILLIS

    /** Resting size of the circle, and the size it contracts to on the exhale. */
    const val REST_SCALE = 0.62f

    /** Size at the top of the inhale. */
    const val FULL_SCALE = 1.0f

    fun frameAt(elapsedMillis: Long): PacerFrame {
        require(elapsedMillis >= 0) { "Elapsed time cannot be negative" }
        val withinCycle = elapsedMillis % CYCLE_MILLIS
        val completedCycles = (elapsedMillis / CYCLE_MILLIS).toInt()
        return if (withinCycle < INHALE_MILLIS) {
            PacerFrame(BreathPhase.INHALE, withinCycle, INHALE_MILLIS, completedCycles)
        } else {
            PacerFrame(
                BreathPhase.EXHALE, withinCycle - INHALE_MILLIS, EXHALE_MILLIS, completedCycles
            )
        }
    }

    fun scaleFor(phase: BreathPhase): Float = when (phase) {
        BreathPhase.INHALE -> FULL_SCALE
        BreathPhase.EXHALE -> REST_SCALE
    }

    fun durationOf(phase: BreathPhase): Long = when (phase) {
        BreathPhase.INHALE -> INHALE_MILLIS
        BreathPhase.EXHALE -> EXHALE_MILLIS
    }

    /**
     * How long the circle should take to reach its new size, honouring the device's
     * reduced-motion preference (D-13).
     *
     * When the platform animator scale is zero the user has asked for no animation, so the
     * circle snaps and the cue text plus its live region carry the pace on their own. The
     * decision is a pure function so it is unit-tested rather than eyeballed on a device.
     */
    fun animationMillisFor(phaseMillis: Long, animatorScale: Float): Int =
        if (animatorScale <= 0f) 0 else phaseMillis.toInt()
}

/**
 * The lengths offered, ascending, with nothing preselected anywhere in the UI. "User-chosen
 * duration, no default dose" is a frozen product constraint, and a highlighted chip would
 * be a default dose with extra steps (D-2).
 */
val BREATHING_LENGTHS_MINUTES: List<Int> = listOf(1, 3, 5, 10)

/** The longest session the app can produce; re-asserted on every import (Invariant 6). */
const val MAX_BREATHING_SESSION_MILLIS = 600_000L
