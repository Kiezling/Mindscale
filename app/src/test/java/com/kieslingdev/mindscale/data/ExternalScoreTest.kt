package com.kieslingdev.mindscale.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExternalScoreTest {
    @Test
    fun instrumentBoundsAreValidationOnlyAndInclusive() {
        assertEquals(0, score(ExternalInstrument.PHQ_8, 0).total)
        assertEquals(24, score(ExternalInstrument.PHQ_8, 24).total)
        assertEquals(0, score(ExternalInstrument.GAD_7, 0).total)
        assertEquals(21, score(ExternalInstrument.GAD_7, 21).total)
        assertThrows(IllegalArgumentException::class.java) { score(ExternalInstrument.PHQ_8, -1) }
        assertThrows(IllegalArgumentException::class.java) { score(ExternalInstrument.PHQ_8, 25) }
        assertThrows(IllegalArgumentException::class.java) { score(ExternalInstrument.GAD_7, -1) }
        assertThrows(IllegalArgumentException::class.java) { score(ExternalInstrument.GAD_7, 22) }
    }

    @Test
    fun provenanceIsAlwaysExplicit() {
        assertEquals(
            ExternalScoreProvenance.EXTERNALLY_OBTAINED_USER_ENTERED,
            score(ExternalInstrument.PHQ_8, 8).provenance
        )
    }

    private fun score(instrument: ExternalInstrument, total: Int) =
        ExternalScore(instrument = instrument, total = total, assessedEpochDay = 0, enteredAt = 1)
}
