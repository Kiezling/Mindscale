package com.kieslingdev.mindscale.breathing

import android.os.SystemClock

/**
 * Two clocks, because a session needs both and they answer different questions
 * (`docs/specs/SPEC-paced-breathing.md`, D-5).
 *
 * [wallMillis] answers "when did this happen", which is what belongs in an export next to
 * every other dated MindScale record. [monotonicMillis] answers "how long has it been",
 * which wall time cannot: an NTP correction or a manual time change mid-session would
 * otherwise produce a negative or absurd interval.
 *
 * The interface exists so the pacer and the ViewModel are testable on the JVM against
 * virtual time; [SystemBreathingClock] is the only Android-typed edge of this feature.
 */
interface BreathingClock {
    fun wallMillis(): Long
    fun monotonicMillis(): Long
}

class SystemBreathingClock : BreathingClock {
    override fun wallMillis(): Long = System.currentTimeMillis()

    /** Monotonic and unaffected by clock changes; counts while the device is awake. */
    override fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
}
