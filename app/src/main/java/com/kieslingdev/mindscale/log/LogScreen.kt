package com.kieslingdev.mindscale.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.settings.vocabularyForEntry
import com.kieslingdev.mindscale.ui.components.MsActionTone
import com.kieslingdev.mindscale.ui.components.MsCard
import com.kieslingdev.mindscale.ui.components.MsChip
import com.kieslingdev.mindscale.ui.components.MsDialog
import com.kieslingdev.mindscale.ui.components.MsEyebrow
import com.kieslingdev.mindscale.ui.components.MsHairline
import com.kieslingdev.mindscale.ui.components.MsTextAction
import com.kieslingdev.mindscale.ui.components.MsUppercaseText
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val RowTimeTwelveHourFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val RowTimeTwentyFourHourFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
private val FilterDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

// ── one-off geometry from the design authority, per D-18 ──────────────────────

/** The row's `grid-template-columns:34px 1fr auto`, line 269. */
private val RowNumeralColumnWidth = 34.dp

/** The note preview's and inline panels' indent, lines 288 and 291: `46px`, clearing the numeral. */
private val RowContentIndent = 46.dp

@Composable
fun LogRoute(viewModel: LogViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LogScreen(state, viewModel::onEvent, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    uiState: LogUiState,
    onEvent: (LogEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var picking by remember { mutableStateOf<FilterBound?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("full_log_screen"),
        contentPadding = PaddingValues(MsSpacing.lgPlus)
    ) {
        item {
            FilterHeader(
                state = uiState,
                onFrom = { picking = FilterBound.FROM },
                onTo = { picking = FilterBound.TO },
                onClear = { onEvent(LogEvent.ClearFilter) }
            )
        }

        uiState.message?.let { message ->
            item {
                // The design has no equivalent banner. An ink surface was rejected rather than
                // restyled: every `MsTextAction` tone is designed for a `bg` or `card` backdrop, and
                // gold on ink is unreadable, so `Dismiss` could not have lived on one
                // (`docs/specs/SPEC-track-and-log-visual.md`, D-12).
                MsCard(
                    contentPadding = MsSpacing.lgPlus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MsSpacing.sm)
                        .testTag("log_message")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.ms.inkSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        MsTextAction(
                            text = "Dismiss",
                            onClick = { onEvent(LogEvent.MessageDismissed) },
                            tone = MsActionTone.Muted
                        )
                    }
                }
            }
        }

        uiState.readError?.let { error ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MsSpacing.sm)
                        .testTag("log_read_error"),
                    verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
                ) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.ms.danger
                    )
                    MsTextAction(text = "Retry", onClick = { onEvent(LogEvent.Retry) })
                }
            }
        }

        if (uiState.days.isEmpty() && uiState.readError == null) {
            item {
                LogEmptyState(
                    filtered = uiState.hasAnyRecords && uiState.appliedFilter != LogFilter()
                )
            }
        } else {
            uiState.days.forEach { day ->
                item(key = "day:${day.date}") {
                    DayHeader(day.date)
                }
                items(day.items, key = { it.stableKey }) { item ->
                    LogItemRow(
                        item = item,
                        editDraft = uiState.editDraft?.takeIf { item is LogItem.Rating && it.entryId == item.id },
                        noteDraft = uiState.noteDraft?.takeIf { item is LogItem.Rating && it.entryId == item.id },
                        settings = uiState.settings,
                        onEvent = onEvent
                    )
                }
            }
        }
    }

    picking?.let { bound ->
        val selected = when (bound) {
            FilterBound.FROM -> uiState.pendingFilter.from
            FilterBound.TO -> uiState.pendingFilter.to
        }
        LogDatePicker(
            title = if (bound == FilterBound.FROM) "From" else "To",
            selected = selected,
            onSelected = { date ->
                when (bound) {
                    FilterBound.FROM -> onEvent(LogEvent.FromChanged(date))
                    FilterBound.TO -> onEvent(LogEvent.ToChanged(date))
                }
                picking = null
            },
            onDismiss = { picking = null }
        )
    }

    uiState.deleteTarget?.let { target ->
        MsDialog(
            onDismissRequest = { onEvent(LogEvent.DeleteCancelled) },
            title = { Text("Delete ${target.description}?") },
            text = { Text("This permanently deletes this record. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onEvent(LogEvent.DeleteConfirmed) }) {
                    DialogActionLabel("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(LogEvent.DeleteCancelled) }) {
                    DialogActionLabel("Cancel")
                }
            }
        )
    }
}

private enum class FilterBound { FROM, TO }

/**
 * A dialog action label, uppercased in place (D-3). The button stays a Material [TextButton] so its
 * enabled and disabled semantics are untouched; only the label is wrapped, and the colour is left
 * unspecified so it inherits `TextButton`'s content colour.
 */
@Composable
private fun DialogActionLabel(text: String) {
    MsUppercaseText(text = text, style = MaterialTheme.typography.labelMedium)
}

/**
 * The design's filter row, lines 256-261, and layout fix L-4 (D-11).
 *
 * The design pairs an eyebrow with each date input. **No eyebrow is added here**: MindScale's
 * fields are buttons whose single label is either the formatted date or the word `From`/`To`, and
 * adding a second string per field would be a copy addition.
 *
 * L-4: the prototype jams `ALL` against the right edge. It gains a trailing gutter equal to the gap
 * between the three elements, so it is inset from the row's right edge by the same amount that
 * separates it from the To field.
 */
@Composable
private fun FilterHeader(
    state: LogUiState,
    onFrom: () -> Unit,
    onTo: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus),
            verticalAlignment = Alignment.Bottom
        ) {
            UnderlinedFilterField(
                label = state.pendingFilter.from?.format(FilterDateFormatter) ?: "From",
                isSet = state.pendingFilter.from != null,
                onClick = onFrom,
                modifier = Modifier
                    .weight(1f)
                    .testTag("log_from_button")
                    .semantics { contentDescription = "Choose From date" }
            )
            UnderlinedFilterField(
                label = state.pendingFilter.to?.format(FilterDateFormatter) ?: "To",
                isSet = state.pendingFilter.to != null,
                onClick = onTo,
                modifier = Modifier
                    .weight(1f)
                    .testTag("log_to_button")
                    .semantics { contentDescription = "Choose To date" }
            )
            MsTextAction(
                text = "All",
                onClick = onClear,
                tone = MsActionTone.Muted,
                // L-4: the trailing gutter the prototype leaves out.
                modifier = Modifier
                    .padding(end = MsSpacing.lgPlus)
                    .testTag("log_all_button")
            )
        }
        state.filterError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.ms.danger,
                modifier = Modifier.testTag("log_filter_error")
            )
        }
        Text(
            text = "${state.recordCount} ${if (state.recordCount == 1) "record" else "records"} · ratings, sleep and events",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.ms.inkQuaternary,
            modifier = Modifier.testTag("log_record_count")
        )
    }
}

/**
 * The design's date field, line 257: no fill and no ring, just a 1 dp bottom rule.
 *
 * The rule is the control's only boundary, so it is `outline` rather than the design's
 * `rgba(ink,.16)`, which measures 1.41:1 against a 3:1 floor (D-4).
 */
@Composable
private fun UnderlinedFilterField(
    label: String,
    isSet: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.ms
    Column(
        modifier = modifier
            .heightIn(min = MsSpacing.minTouchTarget)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSet) palette.inkSecondary else palette.inkQuaternary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = MsSpacing.xxs)
        )
        // Not `MsHairline`: that paints the decorative separator alpha, and this rule is the
        // control's only boundary, so it must carry the 3:1 `outline` token instead (D-4).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MsSpacing.hairline)
                .background(palette.outline)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogDatePicker(
    title: String,
    selected: LocalDate?,
    onSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = selected?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val date = pickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    onSelected(date)
                }
            ) { DialogActionLabel("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { DialogActionLabel("Cancel") } }
    ) {
        DatePicker(
            state = pickerState,
            title = { Text(title, modifier = Modifier.padding(MsSpacing.lgPlus)) }
        )
    }
}

/**
 * The design's day header, line 265: a 10 px tracked uppercase label in gold.
 *
 * The design's right-hand `g.meta` summary is deliberately not added — MindScale has no such
 * string, and a time-weighted one would be an inference (D-12, D-19).
 */
@Composable
private fun DayHeader(date: LocalDate) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DayFormatter)
    }
    MsUppercaseText(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.ms.goldText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MsSpacing.lg, bottom = MsSpacing.xs)
            .testTag("log_day_$date")
    )
}

@Composable
private fun LogItemRow(
    item: LogItem,
    editDraft: LogEditDraft?,
    noteDraft: LogNoteDraft?,
    settings: com.kieslingdev.mindscale.data.TrackSettings,
    onEvent: (LogEvent) -> Unit
) {
    val palette = MaterialTheme.ms
    Column(modifier = Modifier.fillMaxWidth().testTag("log_row_${item.stableKey}")) {
        MsHairline()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = MsSpacing.mdPlus),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)
        ) {
            Text(
                text = when (item) {
                    is LogItem.Rating -> item.entry.value.toString()
                    is LogItem.Sleep -> "—"
                    is LogItem.Event -> "×"
                },
                style = MaterialTheme.typography.titleLarge,
                // The design recedes the sleep em-dash to `rgba(ink,.28)` and a zero rating to
                // `rgba(ink,.4)`; both measure below the 4.5:1 text floor, so this uses the
                // faintest compliant level instead (D-12).
                color = when {
                    item is LogItem.Event -> palette.goldText
                    item is LogItem.Sleep -> palette.inkQuaternary
                    item is LogItem.Rating && item.entry.value == 0 -> palette.inkQuaternary
                    else -> palette.inkPrimary
                },
                modifier = Modifier.widthIn(min = RowNumeralColumnWidth)
            )
            Column(
                modifier = Modifier.weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = itemDescription(item, settings.hourFormat, settings.hideNotes)
                    },
                verticalArrangement = Arrangement.spacedBy(MsSpacing.xxxs)
            ) {
                Text(
                    formatTime(item.timestamp, settings.hourFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkSecondary
                )
                val meta = itemMeta(item)
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkQuaternary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        // L-2's treatment, applied to Log's rows for the same reason: one baseline, even gaps, and
        // their own full-width line so three 48 dp targets never squeeze the row content (D-9, D-12).
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
        ) {
            if (item is LogItem.Rating) {
                MsTextAction(
                    text = "Edit",
                    onClick = { onEvent(LogEvent.EditToggled(item.id)) },
                    tone = MsActionTone.Muted,
                    modifier = Modifier.semantics {
                        contentDescription = "Edit rating ${item.entry.value}"
                    }
                )
                MsTextAction(
                    text = "Note",
                    onClick = { onEvent(LogEvent.NoteToggled(item.id)) },
                    tone = MsActionTone.Muted,
                    modifier = Modifier.semantics {
                        contentDescription = "Edit note for rating ${item.entry.value}"
                    }
                )
            }
            MsTextAction(
                text = "Delete",
                onClick = { onEvent(LogEvent.DeleteRequested(item)) },
                tone = MsActionTone.Muted,
                modifier = Modifier.semantics { contentDescription = "Delete ${deleteType(item)}" }
            )
        }
        if (item is LogItem.Rating && !settings.hideNotes && !item.entry.note.isNullOrBlank()) {
            Text(
                item.entry.note.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkTertiary,
                modifier = Modifier
                    .padding(start = RowContentIndent, bottom = MsSpacing.sm)
                    .testTag("log_note_${item.id}")
            )
        }
        if (editDraft != null) {
            InlineEditPanel(
                editDraft,
                vocabularyForEntry(settings, editDraft.chips),
                onEvent
            )
        }
        if (noteDraft != null) InlineNotePanel(noteDraft, onEvent)
    }
}

@Composable
private fun InlineEditPanel(
    draft: LogEditDraft,
    vocabulary: List<String>,
    onEvent: (LogEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(start = RowContentIndent, bottom = MsSpacing.mdPlus)
            .testTag("log_inline_edit_${draft.entryId}"),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)
        ) {
            (0..10).forEach { value ->
                MsChip(
                    text = value.toString(),
                    selected = value == draft.value,
                    onClick = { onEvent(LogEvent.EditValueSelected(value)) },
                    modifier = Modifier.testTag("log_edit_value_$value")
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)
        ) {
            vocabulary.forEach { chip ->
                MsChip(
                    text = chip,
                    selected = chip in draft.chips,
                    onClick = { onEvent(LogEvent.EditChipToggled(chip)) },
                    modifier = Modifier.testTag("log_edit_chip_$chip")
                )
            }
        }
        OutlinedTextField(
            value = draft.timestampText,
            onValueChange = { onEvent(LogEvent.EditTimestampTextChanged(it)) },
            label = { Text("Time (yyyy-MM-dd HH:mm)") },
            supportingText = draft.error?.let { { Text(it) } },
            isError = draft.error != null,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().testTag("log_edit_timestamp")
        )
    }
}

@Composable
private fun InlineNotePanel(draft: LogNoteDraft, onEvent: (LogEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(start = RowContentIndent, bottom = MsSpacing.mdPlus)
            .testTag("log_inline_note_${draft.entryId}"),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)
    ) {
        OutlinedTextField(
            value = draft.text,
            onValueChange = { onEvent(LogEvent.NoteTextChanged(it)) },
            label = { Text("Note") },
            minLines = 3,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().testTag("log_note_field")
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)) {
            MsTextAction(text = "Save", onClick = { onEvent(LogEvent.NoteSaved) })
            MsTextAction(
                text = "Cancel",
                onClick = { onEvent(LogEvent.NoteCancelled) },
                tone = MsActionTone.Muted
            )
        }
    }
}

/** The design's empty-state idiom: an eyebrow over left-aligned prose, not a centred column. */
@Composable
private fun LogEmptyState(filtered: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MsSpacing.xxlPlus)
            .testTag("log_empty_state"),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)
    ) {
        MsEyebrow(text = if (filtered) "No records in this date range" else "No records yet")
        if (!filtered) {
            Text(
                "A well day costs nothing and needs no log.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.ms.inkSecondary
            )
        }
    }
}

private fun formatTime(timestamp: Long, hourFormat: HourFormat): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(
        if (hourFormat == HourFormat.TWENTY_FOUR) RowTimeTwentyFourHourFormatter
        else RowTimeTwelveHourFormatter
    )

private fun itemMeta(item: LogItem): String = when (item) {
    is LogItem.Rating -> buildList {
        when (item.entry.kind) {
            EntryKind.SLEEP -> add("went to sleep")
            EntryKind.WAKE -> add("woke up")
            null -> if (item.entry.value == 0) add("ended")
        }
        if (item.entry.chips.isNotEmpty()) add(item.entry.chips.joinToString(" · "))
    }.joinToString(" · ")
    is LogItem.Sleep -> if (item.interval.endTs == null) "sleeping now" else "slept ${formatSleepDuration(item.interval)}"
    is LogItem.Event -> item.marker.text
}

private fun itemDescription(item: LogItem, hourFormat: HourFormat, hideNotes: Boolean): String = when (item) {
    is LogItem.Rating -> buildString {
        append("Rating ${item.entry.value}, ${formatTime(item.timestamp, hourFormat)}")
        val meta = itemMeta(item)
        if (meta.isNotBlank()) append(", $meta")
        if (!hideNotes) item.entry.note?.takeIf { it.isNotBlank() }?.let { append(", note $it") }
    }
    is LogItem.Sleep -> "Sleep interval, ${formatTime(item.timestamp, hourFormat)}, ${itemMeta(item)}"
    is LogItem.Event -> "Event, ${formatTime(item.timestamp, hourFormat)}, ${item.marker.text}"
}

private fun deleteType(item: LogItem): String = when (item) {
    is LogItem.Rating -> "rating ${item.entry.value}"
    is LogItem.Sleep -> "sleep interval"
    is LogItem.Event -> "event"
}
