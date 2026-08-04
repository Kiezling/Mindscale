package com.kieslingdev.mindscale.log

import java.time.LocalDate

sealed interface LogEvent {
    data class FromChanged(val date: LocalDate?) : LogEvent
    data class ToChanged(val date: LocalDate?) : LogEvent
    data object ClearFilter : LogEvent
    data class EditToggled(val entryId: Long) : LogEvent
    data class EditValueSelected(val value: Int) : LogEvent
    data class EditChipToggled(val chip: String) : LogEvent
    data class EditTimestampTextChanged(val text: String) : LogEvent
    data class NoteToggled(val entryId: Long) : LogEvent
    data class NoteTextChanged(val text: String) : LogEvent
    data object NoteSaved : LogEvent
    data object NoteCancelled : LogEvent
    data class DeleteRequested(val item: LogItem) : LogEvent
    data object DeleteConfirmed : LogEvent
    data object DeleteCancelled : LogEvent
    data object MessageDismissed : LogEvent
    data object Retry : LogEvent
}
