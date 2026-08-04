package com.kieslingdev.mindscale.settings

import com.kieslingdev.mindscale.data.TrackSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {
    @Test
    fun normalizeOnsetWords_trimsDeduplicatesAndPreservesFirstSpelling() {
        val result = normalizeOnsetWords(" Flat, work\nflat,  poor sleep ")
        assertEquals(
            listOf("Flat", "work", "poor sleep"),
            (result as ValidationResult.Valid).value
        )
    }

    @Test
    fun normalizeOnsetWords_rejectsEmptyTooManyAndOverlong() {
        assertTrue(normalizeOnsetWords("  ,\n") is ValidationResult.Invalid)
        assertTrue(normalizeOnsetWords((1..21).joinToString(",") { "word$it" }) is ValidationResult.Invalid)
        assertTrue(normalizeOnsetWords("x".repeat(33)) is ValidationResult.Invalid)
    }

    @Test
    fun anchorsMapToFrozenRangesAndZeroHasNone() {
        val settings = TrackSettings(anchor2 = "low", anchor5 = "middle", anchor8 = "high")
        assertEquals("", anchorFor(0, settings))
        assertEquals("low", anchorFor(1, settings))
        assertEquals("low", anchorFor(3, settings))
        assertEquals("middle", anchorFor(4, settings))
        assertEquals("middle", anchorFor(6, settings))
        assertEquals("high", anchorFor(7, settings))
        assertEquals("high", anchorFor(10, settings))
    }

    @Test
    fun vocabularyIncludesHistoricalExtrasWithoutDuplicatingCurrentWords() {
        val settings = TrackSettings(onsetChips = listOf("flat", "work"))
        assertEquals(listOf("flat", "work", "Old word"), vocabularyForEntry(settings, listOf("FLAT", "Old word")))
    }
}
