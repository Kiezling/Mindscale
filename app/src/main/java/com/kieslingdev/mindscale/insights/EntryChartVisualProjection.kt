package com.kieslingdev.mindscale.insights

import kotlin.math.min

private const val DISPLAY_TRANSITION_MILLIS = 15L * 60L * 1000L

/** A display-only transition. It carries no source metadata and is never used for readouts. */
data class EntryChartVisualTransition(
    val startMillis: Long,
    val endMillis: Long,
    val fromValue: Int,
    val toValue: Int
)

/**
 * Returns only transitions that can be smoothed safely. The chart's measured segments remain the
 * source of truth; this projection is deliberately unsuitable for statistics or selection.
 */
fun EntryChart.visualTransitions(plotWidthPx: Float? = null): List<EntryChartVisualTransition> = buildList {
    segments.zipWithNext().forEach { (previous, next) ->
        if (previous.endMillis != next.startMillis ||
            previous.sourceEntryMillis == null || next.sourceEntryMillis == null ||
            previous.sourceEntryMillis == next.sourceEntryMillis ||
            next.sourceEntryMillis != next.startMillis ||
            previous.sourceEntryMillis < startMillis ||
            next.startMillis > endMillis || next.endMillis > endMillis
        ) return@forEach
        // Fifteen minutes disappears at long ranges. Eight screen pixels makes the bend visible,
        // while the preceding measured interval remains a hard boundary for the projection.
        val screenMillis = plotWidthPx?.takeIf { it > 0f }?.let { width ->
            ((endMillis - startMillis).toDouble() * 8.0 / width).toLong()
        } ?: 0L
        val duration = min(maxOf(DISPLAY_TRANSITION_MILLIS, screenMillis),
            next.startMillis - previous.startMillis)
        val start = next.startMillis - duration
        if (duration > 0L && start >= previous.startMillis &&
            sleeps.none { it.startMillis < next.startMillis && it.endMillis > start }
        ) {
            add(EntryChartVisualTransition(start, next.startMillis, previous.value, next.value))
        }
    }
}

/** Monotonic smoothstep samples for Canvas; endpoints are exact recorded values. */
fun EntryChartVisualTransition.samples(sampleCount: Int = 16): List<Pair<Long, Float>> {
    require(sampleCount >= 2)
    val span = (endMillis - startMillis).coerceAtLeast(1L)
    return (0 until sampleCount).map { index ->
        val t = index.toFloat() / (sampleCount - 1)
        val eased = t * t * (3f - 2f * t)
        val value = fromValue + (toValue - fromValue) * eased
        (startMillis + span * index / (sampleCount - 1)) to value
    }
}
