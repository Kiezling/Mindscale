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
    // -- Phase 2 --
    data class EditChipToggled(val chip: String) : TrackEvent
    data object ToggleHelp : TrackEvent
    data object ArmSleep : TrackEvent
    data object ArmWake : TrackEvent
    data class OnsetChipToggled(val chip: String) : TrackEvent
    data object OnsetChipsSubmitted : TrackEvent
    data object OnsetChipsSkipped : TrackEvent
    data object MarkerToggled : TrackEvent
    data class MarkerDraftChanged(val text: String) : TrackEvent
    data object MarkerSaveConfirmed : TrackEvent
    data object MarkerCancelled : TrackEvent
    data object CheckinStillUseful : TrackEvent
    data object CheckinPauseRequested : TrackEvent
    data object ResumeTracking : TrackEvent
    data object ToastDismissed : TrackEvent
}
