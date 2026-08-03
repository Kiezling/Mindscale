package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Entry

sealed interface TrackEvent {
    data class KeyTapped(val value: Int) : TrackEvent
    data class KeyLongPressed(val value: Int) : TrackEvent
    data class BackdateTimestampChanged(val timestampMillis: Long) : TrackEvent
    data object BackdateSaveConfirmed : TrackEvent
    data object BackdateCancelled : TrackEvent
    data class EditRequested(val entry: Entry) : TrackEvent
    data class EditValueChanged(val value: Int) : TrackEvent
    data class EditTimestampChanged(val timestampMillis: Long) : TrackEvent
    data object EditSaveConfirmed : TrackEvent
    data object EditCancelled : TrackEvent
    data class NoteRequested(val entry: Entry) : TrackEvent
    data class NoteTextChanged(val text: String) : TrackEvent
    data object NoteSaveConfirmed : TrackEvent
    data object NoteCancelled : TrackEvent
    data class DeleteRequested(val entry: Entry) : TrackEvent
    data object DeleteConfirmed : TrackEvent
    data object DeleteCancelled : TrackEvent
    data object ReadoutDismissed : TrackEvent
}
