package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings

data class TrackUiState(
    val recentEntries: List<Entry> = emptyList(),
    val isEmpty: Boolean = true,
    val transientReadout: ReadoutState? = null,
    val activeModal: TrackModalState? = null,
    // -- Phase 2 --
    val helpOpen: Boolean = false,
    val onsetChipPrompt: OnsetChipPromptState? = null,
    val sleepOn: Boolean = true,
    val armedCapture: EntryKind? = null,
    val openSleepInterval: SleepInterval? = null,
    val markerOpen: Boolean = false,
    val markerDraft: String = "",
    val isPaused: Boolean = false,
    val showCheckin: Boolean = false,
    val toast: String? = null,
    val settings: TrackSettings = TrackSettings(),
    val showAnchorPrompt: Boolean = false
)

data class ReadoutState(
    val value: Int,
    val band: String,
    val expiresAtMillis: Long,
    val anchor: String = ""
)

data class OnsetChipPromptState(val entryId: Long, val selected: Set<String> = emptySet())

sealed interface TrackModalState {
    data class Backdate(
        val draft: BackdateDraft,
        val timestampError: String? = null,
        val isSaving: Boolean = false,
        val mutationError: String? = null
    ) : TrackModalState

    data class Edit(
        val draft: EditEntryDraft,
        val validation: RecordValidation = RecordValidation.Checking,
        val timestampError: String? = null,
        val isSaving: Boolean = false,
        val mutationError: String? = null
    ) : TrackModalState

    data class Note(
        val draft: NoteEntryDraft,
        val validation: RecordValidation = RecordValidation.Checking,
        val isSaving: Boolean = false,
        val mutationError: String? = null
    ) : TrackModalState

    data class Delete(
        val entry: Entry,
        val isSaving: Boolean = false,
        val mutationError: String? = null
    ) : TrackModalState
}

data class BackdateDraft(
    val value: Int,
    val dateText: String,
    val timeText: String,
    val captureKind: EntryKind?
)

data class EditEntryDraft(
    val entryId: Long,
    val baselineTimestampMillis: Long,
    val baselineValue: Int,
    val baselineChips: List<String>,
    val value: Int,
    val dateText: String,
    val timeText: String,
    val chips: List<String>
)

data class NoteEntryDraft(
    val entryId: Long,
    val baselineText: String,
    val text: String
)

sealed interface RecordValidation {
    data object Checking : RecordValidation
    data object Current : RecordValidation
    data object Conflicting : RecordValidation
    data object ReadFailed : RecordValidation
}
