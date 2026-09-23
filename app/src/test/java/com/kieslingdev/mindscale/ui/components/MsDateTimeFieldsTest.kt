package com.kieslingdev.mindscale.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MsDateTimeFieldsTest {
    @Test fun civilDateConversionUsesUtcMidnight() {
        assertEquals("2026-09-17", civilDateFromUtcMillis(utcMillisFromCivilDate("2026-09-17")))
    }

    @Test fun wheelParsingClampsLegacyTimeWithoutChangingItUntilConfirmation() {
        assertEquals(23 to 0, wheelTimeOrMidnight("99:-2"))
        assertEquals(0 to 59, wheelTimeOrMidnight("-1:99"))
        assertEquals(0 to 0, wheelTimeOrMidnight("not a time"))
    }

    @Test fun timeOutputIsAlwaysCanonicalTwentyFourHour() {
        assertEquals("00:05", timeText(0, 5))
        assertEquals("12:00", timeText(12, 0))
        assertEquals("23:59", timeText(23, 59))
    }
}
