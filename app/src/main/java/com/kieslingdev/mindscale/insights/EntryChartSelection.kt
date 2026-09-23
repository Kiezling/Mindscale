package com.kieslingdev.mindscale.insights

import kotlin.math.abs
import kotlin.math.roundToLong

/** Resolve touch against recorded targets in rendered pixels; ties favor a rating. */
internal fun chartInstantFromPosition(
    x: Float,
    width: Float,
    chart: EntryChart,
    eventSnapPixels: Float
): Long {
    val safeWidth = width.coerceAtLeast(1f)
    val span = (chart.endMillis - chart.startMillis).coerceAtLeast(1L)
    val touch = x.coerceIn(0f, safeWidth)
    val raw = chart.startMillis + (span * (touch / safeWidth).coerceAtMost(0.999999f)).roundToLong()
    data class Target(val at: Long, val isRating: Boolean, val distance: Double)
    fun distance(at: Long): Double = abs((at - chart.startMillis).toDouble() / span * safeWidth - touch)
    val ratings = chart.segments.asSequence()
        .filter { it.sourceEntryMillis == it.startMillis }
        .map { Target(it.startMillis, true, distance(it.startMillis)) }
    val events = chart.markers.asSequence()
        .map { Target(it.atMillis, false, distance(it.atMillis)) }
    val nearest = (ratings + events).filter { it.distance <= eventSnapPixels }
        .minWithOrNull(compareBy<Target> { it.distance }.thenByDescending { it.isRating }.thenBy { it.at })
    return nearest?.at ?: raw
}
