package com.kieslingdev.mindscale.log

import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogTimelineTest {

    @Test
    fun `mixed items sort newest first with frozen type and id tie breakers`() {
        val items = combineLogItems(
            entries = listOf(Entry(id = 1, ts = 1_000, value = 4), Entry(id = 2, ts = 1_000, value = 5)),
            sleeps = listOf(SleepInterval(id = 9, startTs = 1_000, endTs = 2_000)),
            markers = listOf(Marker(id = 4, ts = 1_000, text = "dose change"))
        )

        assertEquals(listOf("entry:2", "entry:1", "marker:4", "sleep:9"), items.map { it.stableKey })
    }

    @Test
    fun `groups use local calendar days and keep newest day first`() {
        val zone = ZoneId.of("America/Chicago")
        val late = LocalDate.of(2026, 3, 8).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val early = LocalDate.of(2026, 3, 9).atTime(0, 30).atZone(zone).toInstant().toEpochMilli()

        val groups = groupLogItems(
            combineLogItems(
                listOf(Entry(id = 1, ts = late, value = 2), Entry(id = 2, ts = early, value = 3)),
                emptyList(),
                emptyList()
            ),
            zone
        )

        assertEquals(listOf(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 8)), groups.map { it.date })
    }

    @Test
    fun `inclusive local date becomes DST-safe half-open epoch range`() {
        val zone = ZoneId.of("America/Chicago")
        val day = LocalDate.of(2026, 3, 8)

        val range = epochRange(LogFilter(from = day, to = day), zone)

        assertEquals(23L * 60 * 60 * 1000, range.toTsExclusive!! - range.fromTs!!)
        assertNull(epochRange(LogFilter(), zone).fromTs)
        assertNull(epochRange(LogFilter(), zone).toTsExclusive)
    }

    @Test
    fun `sleep duration is normalized and open interval never fabricates an end`() {
        assertEquals("43m", formatSleepDuration(SleepInterval(startTs = 0, endTs = 43 * 60_000L)))
        assertEquals("6h", formatSleepDuration(SleepInterval(startTs = 0, endTs = 6 * 60 * 60_000L)))
        assertEquals("7h 59m", formatSleepDuration(SleepInterval(startTs = 0, endTs = (7 * 60 + 59) * 60_000L)))
        assertEquals("8h", formatSleepDuration(SleepInterval(startTs = 0, endTs = 8 * 60 * 60_000L)))
        assertEquals("sleeping now", formatSleepDuration(SleepInterval(startTs = 0, endTs = null)))
    }
}
