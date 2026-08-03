package com.kieslingdev.mindscale.track

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BandTest {

    @Test
    fun `0 returns ended`() {
        assertEquals("ended", band(0))
    }

    @Test
    fun `1 returns mild`() {
        assertEquals("mild", band(1))
    }

    @Test
    fun `3 returns mild`() {
        assertEquals("mild", band(3))
    }

    @Test
    fun `4 returns moderate`() {
        assertEquals("moderate", band(4))
    }

    @Test
    fun `6 returns moderate`() {
        assertEquals("moderate", band(6))
    }

    @Test
    fun `7 returns severe`() {
        assertEquals("severe", band(7))
    }

    @Test
    fun `9 returns severe`() {
        assertEquals("severe", band(9))
    }

    @Test
    fun `10 returns critical`() {
        assertEquals("critical", band(10))
    }

    @Test
    fun `negative 1 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) { band(-1) }
    }

    @Test
    fun `11 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) { band(11) }
    }
}
