package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One completed run of the paced-breathing circle
 * (`docs/specs/SPEC-paced-breathing.md`, D-3, D-4, D-5).
 *
 * An interval, deliberately shaped like [SleepInterval] rather than like the prototype's
 * `{id, ts, minutes}`. The stored fact is the time the pacer actually ran; the length the
 * user picked is an intention, and keeping it would make the export legible as compliance
 * with a dose, which is an interpretation MindScale leaves to the person reading it.
 *
 * There is no breath-by-breath data, no phase log, no completion flag, and no note. Two
 * timestamps are the whole record.
 *
 * `endedAt` is always `startedAt` plus a monotonic elapsed measurement clamped to the
 * chosen length, so `0 <= endedAt - startedAt <= MAX_BREATHING_SESSION_MILLIS` holds by
 * construction and a wall-clock jump mid-session cannot corrupt the interval (Invariant 6).
 */
@Entity(tableName = "breathing_sessions")
data class BreathingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long
)
