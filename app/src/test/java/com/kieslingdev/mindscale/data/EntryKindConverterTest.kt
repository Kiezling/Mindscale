package com.kieslingdev.mindscale.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryKindConverterTest {

    private val converter = EntryKindConverter()

    @Test
    fun `round-trips null`() {
        assertNull(converter.fromKind(null))
        assertNull(converter.toKind(null))
    }

    @Test
    fun `round-trips SLEEP`() {
        val raw = converter.fromKind(EntryKind.SLEEP)
        assertEquals(EntryKind.SLEEP, converter.toKind(raw))
    }

    @Test
    fun `round-trips WAKE`() {
        val raw = converter.fromKind(EntryKind.WAKE)
        assertEquals(EntryKind.WAKE, converter.toKind(raw))
    }
}
