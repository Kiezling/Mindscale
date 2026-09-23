package com.kieslingdev.mindscale.insights

import org.junit.Assert.assertEquals
import org.junit.Test

class EntryChartSelectionTest {
    private val chart = EntryChart(
        startMillis = 0, endMillis = 1_000, firstEntryMillis = 0,
        segments = listOf(
            EntryChartSegment(0, 500, 2, 1, 0, emptyList(), null),
            EntryChartSegment(500, 1_000, 7, 2, 500, emptyList(), null)
        ),
        sleeps = emptyList(),
        markers = listOf(EntryChartMarker(3, 510, "near rating"))
    )

    @Test fun nearestRenderedTargetKeepsRatingSelectableBesideEvent() {
        assertEquals(500, chartInstantFromPosition(50f, 100f, chart, 8f))
        assertEquals(510, chartInstantFromPosition(51f, 100f, chart, 8f))
        assertEquals(500, chartInstantFromPosition(50.5f, 100f, chart, 8f))
    }

    @Test fun eventsOutsideSmallPixelToleranceDoNotSnap() {
        assertEquals(750, chartInstantFromPosition(75f, 100f, chart, 8f))
        assertEquals(50, chartInstantFromPosition(50f, 1000f, chart, 8f))
        assertEquals(510, chartInstantFromPosition(510f, 1000f, chart, 8f))
    }
}
