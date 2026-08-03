package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Entry

data class TrackUiState(
    val recentEntries: List<Entry> = emptyList(),
    val isEmpty: Boolean = true,
    val transientReadout: ReadoutState? = null,
    val backdateDialog: BackdateDialogState? = null,
    val editDialog: EditEntryState? = null,
    val noteDialog: NoteEditState? = null,
    val pendingDelete: Entry? = null
)

data class ReadoutState(val value: Int, val band: String, val expiresAtMillis: Long)
data class BackdateDialogState(val value: Int, val timestampMillis: Long, val error: String? = null)

/**
 * [originalEntry] is the full [Entry] captured at dialog-open time (from
 * [TrackEvent.EditRequested]). Save always derives the persisted row from this
 * captured snapshot (id/chips/note) plus the edited [value]/[timestampMillis] -
 * it never depends on the entry still being present in `recentEntries`'
 * top-10 window, which can change (e.g. a new insert) while the dialog is open.
 */
data class EditEntryState(
    val originalEntry: Entry,
    val value: Int,
    val timestampMillis: Long,
    val error: String? = null
) {
    val entryId: Long get() = originalEntry.id
}

/**
 * [originalEntry] is the full [Entry] captured at dialog-open time (from
 * [TrackEvent.NoteRequested]); see [EditEntryState] for why saves must not
 * depend on a fresh `recentEntries` lookup.
 */
data class NoteEditState(val originalEntry: Entry, val text: String) {
    val entryId: Long get() = originalEntry.id
}
