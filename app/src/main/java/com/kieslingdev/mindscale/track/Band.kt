package com.kieslingdev.mindscale.track

fun band(value: Int): String = when (value) {
    0 -> "ended"
    in 1..3 -> "mild"
    in 4..6 -> "moderate"
    in 7..9 -> "severe"
    10 -> "critical"
    else -> throw IllegalArgumentException("value out of range 0..10: $value")
}
