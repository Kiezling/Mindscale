package com.kieslingdev.mindscale.breathing

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The copy contract (`docs/specs/SPEC-paced-breathing.md`, D-12; Invariant 13).
 *
 * Every string is compared by exact equality, because the whole string is frozen. The
 * banned-word test is the one that enforces the actual product rule: the evidence base for
 * paced breathing does not survive placebo control, so this feature claims nothing.
 */
class BreathingContentTest {

    @Test
    fun everyFrozenStringIsExactlyItsSpecifiedText() {
        assertEquals("Paced breathing", BreathingCopy.TOP_BAR_TITLE)
        assertEquals("Paced breathing", BreathingCopy.TRACK_LINK)
        assertEquals("Open the paced breathing circle", BreathingCopy.TRACK_LINK_DESCRIPTION)
        assertEquals("Paced breathing", BreathingCopy.SETTING_TITLE)
        assertEquals(
            "A breathing circle you can open. Never offered to you unprompted.",
            BreathingCopy.SETTING_DESCRIPTION
        )
        assertEquals(
            "Through the nose, following the circle. Out for a little longer than in. " +
                "Stop whenever you like.",
            BreathingCopy.INSTRUCTIONS
        )
        assertEquals(
            "MindScale makes no claim about what this does. It is a circle that keeps a pace.",
            BreathingCopy.NO_CLAIM
        )
        assertEquals(
            "Each session is saved with its start and end time and appears in your exports. " +
                "A session is only saved if MindScale stays open until it ends.",
            BreathingCopy.RECORDING
        )
        assertEquals("Choose a length", BreathingCopy.CHOOSE)
        assertEquals("In", BreathingCopy.CUE_IN)
        assertEquals("Out", BreathingCopy.CUE_OUT)
        assertEquals("Breathe in", BreathingCopy.CUE_IN_DESCRIPTION)
        assertEquals("Breathe out", BreathingCopy.CUE_OUT_DESCRIPTION)
        assertEquals("Done", BreathingCopy.DONE)
        assertEquals("Close", BreathingCopy.CLOSE)
        assertEquals("Stop", BreathingCopy.STOP)
        assertEquals("That session could not be saved.", BreathingCopy.SAVE_FAILED)
    }

    @Test
    fun lengthLabelsAreCompactAndTheirSpokenFormsAreGrammatical() {
        assertEquals(listOf("1 min", "3 min", "5 min", "10 min"), BREATHING_LENGTHS_MINUTES.map(BreathingCopy::lengthLabel))
        assertEquals(
            listOf("1 minute", "3 minutes", "5 minutes", "10 minutes"),
            BREATHING_LENGTHS_MINUTES.map(BreathingCopy::lengthDescription)
        )
    }

    /** The prototype's readout was always plural and produced "1 minutes". */
    @Test
    fun theRunningReadoutIsSingularForOneMinute() {
        assertEquals("1 minute", BreathingCopy.runningLength(1))
        assertEquals("3 minutes", BreathingCopy.runningLength(3))
        assertEquals("10 minutes", BreathingCopy.runningLength(10))
    }

    @Test
    fun cuesAndTheirSpokenFormsMatchThePhase() {
        assertEquals("In", BreathingCopy.cueFor(BreathPhase.INHALE))
        assertEquals("Out", BreathingCopy.cueFor(BreathPhase.EXHALE))
        assertEquals("Breathe in", BreathingCopy.cueDescriptionFor(BreathPhase.INHALE))
        assertEquals("Breathe out", BreathingCopy.cueDescriptionFor(BreathPhase.EXHALE))
    }

    /**
     * The load-bearing test. Fincham's promising meta-analysis had mostly-inactive
     * controls; the same team's two placebo-controlled trials were both null, and STARS
     * found a fast pace equal to a slow one. So no string in this feature may claim an
     * effect, cite evidence, or name a protocol.
     */
    @Test
    fun noVisibleStringClaimsAnEffectCitesEvidenceOrNamesAProtocol() {
        val banned = listOf(
            // Efficacy verbs and outcome nouns.
            "calm", "relax", "soothe", "soothing", "reduce", "reduces", "relieve", "relief",
            "helps", "help you", "improve", "benefit", "heal", "regulate", "reset",
            "anxiety", "stress", "panic", "depress", "symptom", "mood", "wellness",
            "therap", "treatment", "effective", "works",
            // Science-backing language.
            "science", "scientific", "evidence", "proven", "study", "studies", "research",
            "clinical", "trial",
            // Protocol names.
            "coherent", "resonance", "resonant", "box breathing", "4-7-8", "478",
            "wim hof", "tummo", "pranayama", "physiological sigh", "diaphragmatic",
            // Anything that would imply a dose or a target rate.
            "recommend", "should", "must", "per minute", "breaths per", "bpm",
            // Forbidden mechanics.
            "hold", "retention", "retain", "fast breathing", "hyperventil"
        )

        visibleStrings().forEach { text ->
            val folded = text.lowercase(Locale.ROOT)
            banned.forEach { word ->
                assertFalse(
                    "Frozen copy must not contain \"$word\": $text",
                    folded.contains(word)
                )
            }
        }
    }

    /** The guard must actually be looking at something. */
    @Test
    fun theBannedWordScanCoversEveryVisibleString() {
        val strings = visibleStrings()
        assertEquals(25, strings.size)
        assertFalse(strings.any { it.isBlank() })
    }

    private fun visibleStrings(): List<String> = listOf(
        BreathingCopy.TOP_BAR_TITLE,
        BreathingCopy.TRACK_LINK,
        BreathingCopy.TRACK_LINK_DESCRIPTION,
        BreathingCopy.SETTING_TITLE,
        BreathingCopy.SETTING_DESCRIPTION,
        BreathingCopy.INSTRUCTIONS,
        BreathingCopy.NO_CLAIM,
        BreathingCopy.RECORDING,
        BreathingCopy.CHOOSE,
        BreathingCopy.CUE_IN,
        BreathingCopy.CUE_OUT,
        BreathingCopy.CUE_IN_DESCRIPTION,
        BreathingCopy.CUE_OUT_DESCRIPTION,
        BreathingCopy.DONE,
        BreathingCopy.CLOSE,
        BreathingCopy.STOP,
        BreathingCopy.SAVE_FAILED
    ) + BREATHING_LENGTHS_MINUTES.map(BreathingCopy::lengthLabel) +
        // Spoken, and identical to the running readout, so both are covered here.
        BREATHING_LENGTHS_MINUTES.map(BreathingCopy::lengthDescription)
}
