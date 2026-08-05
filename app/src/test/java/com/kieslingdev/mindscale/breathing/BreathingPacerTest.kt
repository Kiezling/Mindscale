package com.kieslingdev.mindscale.breathing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cadence contract (`docs/specs/SPEC-paced-breathing.md`, D-1, D-2; Invariant 1).
 *
 * The first test is the one that matters: it is the structural proof that MindScale cannot
 * pace fast breathing and cannot hold a breath, because there are exactly two phases and
 * their durations are constants.
 */
class BreathingPacerTest {

    @Test
    fun thereAreExactlyTwoPhasesAndNeitherIsAHold() {
        assertEquals(2, BreathPhase.entries.size)
        assertEquals(BreathPhase.INHALE, BreathPhase.entries[0])
        assertEquals(BreathPhase.EXHALE, BreathPhase.entries[1])
    }

    @Test
    fun theCadenceIsTheFrozenOneAndSatisfiesTheProductConstraints() {
        assertEquals(4_500L, BreathingPacer.INHALE_MILLIS)
        assertEquals(6_500L, BreathingPacer.EXHALE_MILLIS)
        assertEquals(11_000L, BreathingPacer.CYCLE_MILLIS)

        // ~5-6 breaths per minute, with an equal-or-longer exhale.
        val breathsPerMinute = 60_000.0 / BreathingPacer.CYCLE_MILLIS
        assertTrue("$breathsPerMinute br/min is outside 5..6", breathsPerMinute in 5.0..6.0)
        assertTrue(BreathingPacer.EXHALE_MILLIS >= BreathingPacer.INHALE_MILLIS)
    }

    @Test
    fun frameAtReturnsTheCorrectPhaseAtEveryBoundary() {
        assertFrame(0L, BreathPhase.INHALE, phaseElapsed = 0L, cycles = 0)
        assertFrame(4_499L, BreathPhase.INHALE, phaseElapsed = 4_499L, cycles = 0)
        // The instant the inhale ends is the first instant of the exhale, not its last.
        assertFrame(4_500L, BreathPhase.EXHALE, phaseElapsed = 0L, cycles = 0)
        assertFrame(10_999L, BreathPhase.EXHALE, phaseElapsed = 6_499L, cycles = 0)
        assertFrame(11_000L, BreathPhase.INHALE, phaseElapsed = 0L, cycles = 1)
        assertFrame(15_500L, BreathPhase.EXHALE, phaseElapsed = 0L, cycles = 1)
    }

    @Test
    fun frameAtStaysCorrectAtTheLongestOfferedLength() {
        // 599 999 ms is one millisecond short of the ten-minute maximum.
        val frame = BreathingPacer.frameAt(599_999L)
        assertEquals(54, frame.completedCycles)
        assertEquals(BreathPhase.EXHALE, frame.phase)
        assertEquals(599_999L % 11_000L - 4_500L, frame.phaseElapsedMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun frameAtRefusesNegativeElapsedTime() {
        BreathingPacer.frameAt(-1L)
    }

    @Test
    fun phaseRemainingCountsDownToTheNextBoundary() {
        assertEquals(4_500L, BreathingPacer.frameAt(0L).phaseRemainingMillis)
        assertEquals(1L, BreathingPacer.frameAt(4_499L).phaseRemainingMillis)
        assertEquals(6_500L, BreathingPacer.frameAt(4_500L).phaseRemainingMillis)
    }

    @Test
    fun theCircleExpandsOnTheInhaleAndContractsOnTheExhale() {
        assertEquals(BreathingPacer.FULL_SCALE, BreathingPacer.scaleFor(BreathPhase.INHALE), 0f)
        assertEquals(BreathingPacer.REST_SCALE, BreathingPacer.scaleFor(BreathPhase.EXHALE), 0f)
        assertTrue(BreathingPacer.REST_SCALE < BreathingPacer.FULL_SCALE)
    }

    @Test
    fun durationOfMatchesTheFrozenConstants() {
        assertEquals(4_500L, BreathingPacer.durationOf(BreathPhase.INHALE))
        assertEquals(6_500L, BreathingPacer.durationOf(BreathPhase.EXHALE))
    }

    @Test
    fun reducedMotionSnapsTheCircleInsteadOfAnimatingIt() {
        assertEquals(0, BreathingPacer.animationMillisFor(4_500L, animatorScale = 0f))
        assertEquals(0, BreathingPacer.animationMillisFor(6_500L, animatorScale = 0f))
        assertEquals(4_500, BreathingPacer.animationMillisFor(4_500L, animatorScale = 1f))
        assertEquals(6_500, BreathingPacer.animationMillisFor(6_500L, animatorScale = 0.5f))
    }

    @Test
    fun exactlyFourLengthsAreOfferedInAscendingOrder() {
        assertEquals(listOf(1, 3, 5, 10), BREATHING_LENGTHS_MINUTES)
        assertEquals(
            BREATHING_LENGTHS_MINUTES.max() * 60_000L,
            MAX_BREATHING_SESSION_MILLIS
        )
    }

    private fun assertFrame(
        elapsed: Long,
        phase: BreathPhase,
        phaseElapsed: Long,
        cycles: Int
    ) {
        val frame = BreathingPacer.frameAt(elapsed)
        assertEquals("phase at $elapsed", phase, frame.phase)
        assertEquals("phase elapsed at $elapsed", phaseElapsed, frame.phaseElapsedMillis)
        assertEquals("cycles at $elapsed", cycles, frame.completedCycles)
        assertEquals(
            "phase total at $elapsed",
            BreathingPacer.durationOf(phase),
            frame.phaseTotalMillis
        )
    }
}
