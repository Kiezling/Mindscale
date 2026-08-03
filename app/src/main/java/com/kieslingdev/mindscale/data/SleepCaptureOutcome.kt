package com.kieslingdev.mindscale.data

sealed interface SleepCaptureOutcome {
    data object Opened : SleepCaptureOutcome
    data class AlreadyOpen(val since: Long) : SleepCaptureOutcome
    data class Closed(val since: Long, val until: Long) : SleepCaptureOutcome
    data object NothingOpen : SleepCaptureOutcome
}
