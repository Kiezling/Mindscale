package com.kieslingdev.mindscale.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryChartVisualProjectionTest {
    @Test fun increasingDecreasingAndEqualChangesUseExactEndpointsAndMonotonicEasing() {
        val chart = chart(
            segment(0, 60 * MINUTE, 2, 0), segment(60 * MINUTE, 120 * MINUTE, 8, 60 * MINUTE),
            segment(120 * MINUTE, 180 * MINUTE, 3, 120 * MINUTE), segment(180 * MINUTE, 240 * MINUTE, 3, 180 * MINUTE)
        )
        val transitions = chart.visualTransitions()
        assertEquals(3, transitions.size)
        assertEquals(45 * MINUTE, transitions[0].startMillis)
        assertEquals(60 * MINUTE, transitions[0].endMillis)
        assertEquals(2f, transitions[0].samples().first().second, 0f)
        assertEquals(8f, transitions[0].samples().last().second, 0f)
        assertTrue(transitions.all { it.endMillis <= chart.endMillis })
        assertTrue(transitions[0].samples().zipWithNext().all { it.first.second <= it.second.second })
        assertTrue(transitions[1].samples().zipWithNext().all { it.first.second >= it.second.second })
        assertTrue(transitions[2].samples().all { it.second == 3f })
    }

    @Test fun shortIntervalsUseTheWholeAvailableInterval() {
        val chart = chart(segment(0, 5 * MINUTE, 2, 0), segment(5 * MINUTE, 10 * MINUTE, 9, 5 * MINUTE))
        assertEquals(0L, chart.visualTransitions().single().startMillis)
        assertEquals(5 * MINUTE, chart.visualTransitions().single().endMillis)
    }

    @Test fun carriedRangeSleepGapAndExpiredHoldAreNotSmoothed() {
        val carried = chart(segment(0, 60 * MINUTE, 2, -60 * MINUTE), segment(60 * MINUTE, 120 * MINUTE, 8, 60 * MINUTE))
        assertTrue(carried.visualTransitions().isEmpty())
        val gap = chart(segment(0, 30 * MINUTE, 2, 0), segment(40 * MINUTE, 100 * MINUTE, 8, 40 * MINUTE))
        assertTrue(gap.visualTransitions().isEmpty())
        val sleep = chart(
            segment(0, 60 * MINUTE, 2, 0), segment(60 * MINUTE, 120 * MINUTE, 8, 60 * MINUTE),
            sleeps = listOf(EntryChartSleep(50 * MINUTE, 70 * MINUTE))
        )
        assertTrue(sleep.visualTransitions().isEmpty())
        val expired = chart(segment(0, 60 * MINUTE, 2, 0), segment(60 * MINUTE, 120 * MINUTE, 0, 60 * MINUTE, sourceMillis = null))
        assertTrue(expired.visualTransitions().isEmpty())
        assertFalse(EntryChartVisualTransition(0, 1, 0, 10).samples().any { it.second !in 0f..10f })
    }

    @Test fun futureOrRangeCutoffSourceDoesNotCreateAVisualTransition() {
        val chart = chart(
            segment(0, 60 * MINUTE, 2, 0),
            segment(60 * MINUTE, 300 * MINUTE, 8, 60 * MINUTE)
        )

        assertTrue(chart.visualTransitions().isEmpty())
    }

    @Test fun wideRangeUsesVisiblePixelsButNeverLeavesContiguousMeasuredInterval() {
        val day = 24 * 60 * MINUTE
        val chart = EntryChart(0, 90 * day, 0,
            listOf(segment(0, 45 * day, 2, 0), segment(45 * day, 90 * day, 8, 45 * day)),
            emptyList(), emptyList())
        val transition = chart.visualTransitions(plotWidthPx = 300f).single()
        val projectedPixels = (transition.endMillis - transition.startMillis).toDouble() / (90 * day) * 300
        assertTrue(projectedPixels >= 7.9)
        assertEquals(2f, transition.samples().first().second, 0f)
        assertEquals(8f, transition.samples().last().second, 0f)
        assertTrue(transition.startMillis >= chart.segments.first().startMillis)
    }

    private fun chart(vararg segments: EntryChartSegment, sleeps: List<EntryChartSleep> = emptyList()) =
        EntryChart(0, 240 * MINUTE, 0, segments.toList(), sleeps, emptyList())
    private fun segment(start: Long, end: Long, value: Int, source: Long, sourceId: Long? = 1, sourceMillis: Long? = source) = EntryChartSegment(start, end, value, sourceId, sourceMillis, emptyList(), null)
    private companion object { const val MINUTE = 60_000L }
}
