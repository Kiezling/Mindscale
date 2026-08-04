package com.kieslingdev.mindscale.track

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.settings.SettingsFocus
import com.kieslingdev.mindscale.settings.vocabularyForEntry
import com.kieslingdev.mindscale.ui.theme.intensityColor
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Numpad key order/grouping is frozen by Invariant 12: a 3x3 grid of 1-9,
 * then a visually distinct group of 0 and 10 below it.
 */
private val NumpadRows = listOf(
    listOf(1, 2, 3),
    listOf(4, 5, 6),
    listOf(7, 8, 9)
)
private val NumpadEdgeKeys = listOf(0, 10)

private val EntryDateTimeTwelveHourFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
private val EntryDateTimeTwentyFourHourFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")

/**
 * Collects [TrackViewModel]'s state in a lifecycle-aware way and forwards events;
 * this is the only stateful entry point.
 */
@Composable
fun TrackRoute(
    viewModel: TrackViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: (SettingsFocus) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrackScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

/**
 * Stateless, previewable Track screen. Drives all UI tests.
 */
@Composable
fun TrackScreen(
    uiState: TrackUiState,
    onEvent: (TrackEvent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: (SettingsFocus) -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ToastBanner(uiState.toast)
            }

            // Invariant 21: paused hides the entire capture surface (numpad, help,
            // onset-chip prompt, Sleep/Wake, marker, check-in) but never the history
            // below (Recent list / empty state), which renders unconditionally further
            // down regardless of isPaused.
            if (uiState.isPaused) {
                item {
                    PausedBanner(onEvent = onEvent, onOpenData = { onOpenSettings(SettingsFocus.DATA) })
                }
            } else {
                item {
                    TransientReadoutBanner(uiState.transientReadout)
                }
                if (uiState.showAnchorPrompt) {
                    item {
                        AnchorPrompt(
                            onSet = {
                                onEvent(TrackEvent.AnchorPromptDone)
                                onOpenSettings(SettingsFocus.ANCHORS)
                            },
                            onDismiss = { onEvent(TrackEvent.AnchorPromptDone) }
                        )
                    }
                }
                item {
                    HelpToggle(helpOpen = uiState.helpOpen, onEvent = onEvent)
                }
                if (uiState.helpOpen) {
                    item { HelpCard() }
                }
                item {
                    Numpad(
                        onTap = { value -> onEvent(TrackEvent.KeyTapped(value)) },
                        onLongPress = { value -> onEvent(TrackEvent.KeyLongPressed(value)) }
                    )
                }
                if (uiState.onsetChipPrompt != null) {
                    item {
                        OnsetChipCard(
                            prompt = uiState.onsetChipPrompt,
                            vocabulary = uiState.settings.onsetChips,
                            onEvent = onEvent
                        )
                    }
                }
                if (uiState.sleepOn) {
                    item {
                        SleepWakeRow(uiState = uiState, onEvent = onEvent)
                    }
                }
                item {
                    MarkerSection(uiState = uiState, onEvent = onEvent)
                }
                if (uiState.showCheckin) {
                    item {
                        CheckinBanner(onEvent = onEvent)
                    }
                }
            }

            if (uiState.isEmpty) {
                item { EmptyState() }
            } else {
                items(uiState.recentEntries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        onEvent = onEvent,
                        hideNote = uiState.settings.hideNotes,
                        hourFormat = uiState.settings.hourFormat
                    )
                }
            }
        }
    }

    when (val modal = uiState.activeModal) {
        is TrackModalState.Backdate -> BackdateDialog(modal = modal, onEvent = onEvent)
        is TrackModalState.Edit -> EditDialog(
            modal = modal,
            vocabulary = vocabularyForEntry(uiState.settings, modal.draft.chips.toSet()),
            onEvent = onEvent
        )
        is TrackModalState.Note -> NoteDialog(modal = modal, onEvent = onEvent)
        is TrackModalState.Delete -> DeleteConfirmDialog(modal = modal, onEvent = onEvent)
        null -> Unit
    }
}

@Composable
private fun ToastBanner(toast: String?) {
    if (toast == null) return
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("toast_banner")
    ) {
        Text(
            text = toast,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp)
        )
    }
}

@Composable
private fun PausedBanner(
    onEvent: (TrackEvent) -> Unit,
    onOpenData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("paused_banner")
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Tracking paused", style = MaterialTheme.typography.labelSmall)
            Text(
                text = "Your data is still here and still yours. Nothing is being recorded until you start again.",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(
                onClick = { onEvent(TrackEvent.ResumeTracking) },
                modifier = Modifier
                    .testTag("resume_tracking_button")
                    .semantics { contentDescription = "Start tracking again" }
            ) { Text("Start again") }
            TextButton(onClick = onOpenData, modifier = Modifier.testTag("paused_data_button")) {
                Text("Export or delete")
            }
        }
    }
}

@Composable
private fun HelpToggle(helpOpen: Boolean, onEvent: (TrackEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = { onEvent(TrackEvent.ToggleHelp) },
            modifier = Modifier
                .testTag("help_toggle_button")
                .semantics {
                    contentDescription =
                        if (helpOpen) "Hide what the numbers mean" else "Show what the numbers mean"
                }
        ) { Text("?") }
    }
}

@Composable
private fun HelpCard(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("help_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "0 means it isn't happening right now. 1–3 you notice it. 4–6 it's changing what " +
                    "you do. 7–10 it's most of what's happening.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "The numbers only have to mean the same thing to you each time. That's what makes " +
                    "the chart readable months later.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Log when it starts, when it clearly changes, and when it stops. Nothing recorded " +
                    "means nothing was happening.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun OnsetChipCard(
    prompt: OnsetChipPromptState,
    vocabulary: List<String>,
    onEvent: (TrackEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("onset_chip_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "What was happening?", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vocabulary.forEach { chip ->
                    val selected = chip in prompt.selected
                    FilterChip(
                        selected = selected,
                        onClick = { onEvent(TrackEvent.OnsetChipToggled(chip)) },
                        label = { Text(chip) },
                        modifier = Modifier
                            .testTag("onset_chip_$chip")
                            .semantics {
                                contentDescription = if (selected) "$chip, selected" else "$chip, not selected"
                            }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TextButton(
                    onClick = { onEvent(TrackEvent.OnsetChipsSubmitted) },
                    modifier = Modifier.testTag("onset_chips_submit")
                ) { Text("Submit") }
                TextButton(
                    onClick = { onEvent(TrackEvent.OnsetChipsSkipped) },
                    modifier = Modifier.testTag("onset_chips_skip")
                ) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun AnchorPrompt(onSet: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("anchor_prompt")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Make the numbers yours", style = MaterialTheme.typography.titleSmall)
            Text("A few personal examples can help your ratings stay consistent over time.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onSet, modifier = Modifier.testTag("set_anchors")) { Text("Set anchors") }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_anchors")) { Text("Not now") }
            }
        }
    }
}

@Composable
private fun SleepWakeRow(uiState: TrackUiState, onEvent: (TrackEvent) -> Unit) {
    val armed = uiState.armedCapture
    val openInterval = uiState.openSleepInterval
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val sleepDescription = when {
            armed == EntryKind.SLEEP -> "Sleep armed. Tap a number to log falling asleep."
            openInterval != null -> "Asleep since ${formatClockTime(openInterval.startTs, uiState.settings.hourFormat)}"
            else -> "Mark falling asleep. Then tap how you felt."
        }
        TextButton(
            onClick = { onEvent(TrackEvent.ArmSleep) },
            modifier = Modifier
                .weight(1f)
                .testTag("sleep_button")
                .semantics { contentDescription = sleepDescription }
        ) { Text("Sleep") }

        val wakeDescription = when {
            armed == EntryKind.WAKE -> "Wake armed. Tap a number to log waking up."
            openInterval == null -> "Wake. Disabled: tap Sleep first, nothing is currently open."
            else -> "Mark waking up. Then tap how you feel."
        }
        TextButton(
            onClick = { onEvent(TrackEvent.ArmWake) },
            modifier = Modifier
                .weight(1f)
                .testTag("wake_button")
                .semantics { contentDescription = wakeDescription }
        ) { Text("Wake") }
    }
}

@Composable
private fun MarkerSection(uiState: TrackUiState, onEvent: (TrackEvent) -> Unit) {
    // rememberSaveable buffers the text field across Compose state restoration. The
    // ViewModel also mirrors markerOpen/markerDraft into SavedStateHandle so the marker
    // UI itself is recreated open with the same draft after true process recreation.
    // Keyed on markerOpen so each reopen starts from the ViewModel's freshly-reset "".
    var draftText by rememberSaveable(uiState.markerOpen) { mutableStateOf(uiState.markerDraft) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(
                onClick = { onEvent(TrackEvent.MarkerToggled) },
                modifier = Modifier
                    .testTag("marker_toggle")
                    .semantics {
                        contentDescription =
                            if (uiState.markerOpen) "Close event marker input" else "Mark an event"
                    }
            ) { Text(if (uiState.markerOpen) "Event" else "Mark an event") }
        }
        if (uiState.markerOpen) {
            OutlinedTextField(
                value = draftText,
                onValueChange = { newText ->
                    draftText = newText
                    onEvent(TrackEvent.MarkerDraftChanged(newText))
                },
                label = { Text("Dose change, started therapy, travel…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("marker_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TextButton(
                    onClick = { onEvent(TrackEvent.MarkerSaveConfirmed) },
                    modifier = Modifier.testTag("marker_save")
                ) { Text("Save") }
                TextButton(
                    onClick = { onEvent(TrackEvent.MarkerCancelled) },
                    modifier = Modifier.testTag("marker_cancel")
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun CheckinBanner(onEvent: (TrackEvent) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("checkin_banner")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "A question, once in a while", style = MaterialTheme.typography.labelSmall)
            Text(
                text = "Is keeping this record still useful to you? For some people, watching symptoms " +
                    "closely makes them louder. If that's happening, stopping is a reasonable thing to do.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                TextButton(
                    onClick = { onEvent(TrackEvent.CheckinStillUseful) },
                    modifier = Modifier.testTag("checkin_still_useful")
                ) { Text("Still useful") }
                TextButton(
                    onClick = { onEvent(TrackEvent.CheckinPauseRequested) },
                    modifier = Modifier.testTag("checkin_pause")
                ) { Text("Pause tracking") }
            }
        }
    }
}

@Composable
private fun TransientReadoutBanner(readout: ReadoutState?) {
    if (readout == null) return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${readout.value} · ${readout.band}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Logged ${readout.value}, ${readout.band}" +
                        if (readout.anchor.isBlank()) "" else ", ${readout.anchor}"
                }
            )
            if (readout.anchor.isNotBlank()) {
                Text(
                    readout.anchor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("readout_anchor")
                )
            }
        }
    }
}

@Composable
private fun Numpad(
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NumpadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { value ->
                    NumpadKey(
                        value = value,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onTap = onTap,
                        onLongPress = onLongPress,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
        // 0 and 10 are visually distinct (pill shape, different tone) from the 1-9
        // grid above, per Invariant 12 and the mockup's grouping decision.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadEdgeKeys.forEach { value ->
                NumpadKey(
                    value = value,
                    shape = RoundedCornerShape(50),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onTap = onTap,
                    onLongPress = onLongPress,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                )
            }
        }
    }
}

@Composable
private fun NumpadKey(
    value: Int,
    shape: Shape,
    containerColor: Color,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .pointerInput(value) {
                // detectTapGestures guarantees tap and long-press are mutually
                // exclusive (Invariant 8), and uses the platform's default
                // long-press timeout rather than a hand-rolled duration (D-4).
                detectTapGestures(
                    onTap = { onTap(value) },
                    onLongPress = { onLongPress(value) }
                )
            }
            .testTag("numpad_key_$value")
            .semantics {
                contentDescription = "Log value $value now. Long-press to backdate."
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag("track_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nothing recorded yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp).height(8.dp))
        Text(
            text = "A well day costs nothing and needs no log. Come back whenever " +
                "there's something worth tracking.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/** [entry.value] == 0 or a non-null [entry.kind] renders a text badge (Invariant, UI-ACCESSIBILITY). */
private fun entryKindBadge(entry: Entry): String? {
    if (entry.value != 0 && entry.kind == null) return null
    return when (entry.kind) {
        EntryKind.SLEEP -> "asleep"
        EntryKind.WAKE -> "awake"
        null -> "ended"
    }
}

@Composable
private fun EntryRow(
    entry: Entry,
    onEvent: (TrackEvent) -> Unit,
    hideNote: Boolean,
    hourFormat: HourFormat,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val color = intensityColor(entry.value, isDark)
    val bandText = band(entry.value)
    val formatted = remember(entry.ts, hourFormat) {
        java.time.Instant.ofEpochMilli(entry.ts)
            .atZone(ZoneId.systemDefault())
            .format(
                if (hourFormat == HourFormat.TWENTY_FOUR) EntryDateTimeTwentyFourHourFormatter
                else EntryDateTimeTwelveHourFormatter
            )
    }
    val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White
    val badge = entryKindBadge(entry)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = buildString {
                        append("${entry.value}, $bandText, logged $formatted")
                        if (badge != null) append(", $badge")
                        if (entry.chips.isNotEmpty()) append(", ${entry.chips.joinToString(", ")}")
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(text = entry.value.toString(), color = onColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formatted, style = MaterialTheme.typography.bodyMedium)
                Text(text = bandText, style = MaterialTheme.typography.labelSmall)
                if (badge != null) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("entry_badge_${entry.id}")
                    )
                }
                if (entry.chips.isNotEmpty()) {
                    Text(
                        text = entry.chips.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("entry_chips_${entry.id}")
                    )
                }
                val note = entry.note
                if (!hideNote && !note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(
                onClick = { onEvent(TrackEvent.EditRequested(entry)) },
                modifier = Modifier.semantics {
                    contentDescription = "Edit entry with value ${entry.value}"
                }
            ) { Text("Edit") }
            TextButton(
                onClick = { onEvent(TrackEvent.NoteRequested(entry)) },
                modifier = Modifier.semantics {
                    contentDescription = "Edit note for entry with value ${entry.value}"
                }
            ) { Text("Note") }
            TextButton(
                onClick = { onEvent(TrackEvent.DeleteRequested(entry)) },
                modifier = Modifier.semantics {
                    contentDescription = "Delete entry with value ${entry.value}"
                }
            ) { Text("Delete") }
        }
    }
}

private val ClockTimeTwelveHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val ClockTimeTwentyFourHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatClockTime(epochMillis: Long, hourFormat: HourFormat): String =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(
        if (hourFormat == HourFormat.TWENTY_FOUR) ClockTimeTwentyFourHourFormatter
        else ClockTimeTwelveHourFormatter
    )

@Composable
private fun TimestampEditDialog(
    title: String,
    value: Int,
    dateText: String,
    timeText: String,
    timestampError: String?,
    statusMessage: String?,
    isSaving: Boolean,
    canSave: Boolean,
    saveLabel: String,
    onValueChanged: ((Int) -> Unit)?,
    onDateTextChanged: (String) -> Unit,
    onTimeTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    chips: List<String>? = null,
    onChipToggled: ((String) -> Unit)? = null,
    vocabulary: List<String> = DEFAULT_ONSET_CHIPS,
    onRetryValidation: (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onCancel() },
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (onValueChanged != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { if (value > 0) onValueChanged(value - 1) },
                            enabled = !isSaving,
                            modifier = Modifier.semantics { contentDescription = "Decrease value" }
                        ) { Text("-") }
                        Text(
                            text = "Value: $value",
                            modifier = Modifier
                                .width(96.dp)
                                .padding(horizontal = 8.dp)
                        )
                        TextButton(
                            onClick = { if (value < 10) onValueChanged(value + 1) },
                            enabled = !isSaving,
                            modifier = Modifier.semantics { contentDescription = "Increase value" }
                        ) { Text("+") }
                    }
                } else {
                    Text(text = "Value: $value")
                }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = onDateTextChanged,
                    enabled = !isSaving,
                    singleLine = true,
                    label = { Text("Date (yyyy-MM-dd)") },
                    modifier = Modifier
                        .testTag("track_dialog_date")
                        .focusRequester(focusRequester)
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = onTimeTextChanged,
                    enabled = !isSaving,
                    singleLine = true,
                    label = { Text("Time (HH:mm)") },
                    modifier = Modifier.testTag("track_dialog_time")
                )
                if (chips != null && onChipToggled != null) {
                    Text(
                        text = "What was happening?",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vocabulary.forEach { chip ->
                            val selected = chip in chips
                            FilterChip(
                                selected = selected,
                                onClick = { onChipToggled(chip) },
                                enabled = !isSaving,
                                label = { Text(chip) },
                                modifier = Modifier
                                    .testTag("edit_chip_$chip")
                                    .semantics {
                                        contentDescription =
                                            if (selected) "$chip, selected" else "$chip, not selected"
                                    }
                            )
                        }
                    }
                }
                if (timestampError != null) {
                    Text(
                        text = timestampError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (statusMessage != null) {
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (onRetryValidation != null) {
                    TextButton(onClick = onRetryValidation, enabled = !isSaving) { Text("Retry") }
                }
                if (isSaving) {
                    Text(
                        "Saving",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = canSave && !isSaving) { Text(saveLabel) }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isSaving) { Text("Cancel") }
        }
    )
}

@Composable
private fun BackdateDialog(modal: TrackModalState.Backdate, onEvent: (TrackEvent) -> Unit) {
    TimestampEditDialog(
        title = "Backdate entry",
        value = modal.draft.value,
        dateText = modal.draft.dateText,
        timeText = modal.draft.timeText,
        timestampError = modal.timestampError,
        statusMessage = modal.mutationError,
        isSaving = modal.isSaving,
        canSave = modal.timestampError == null,
        saveLabel = "Save",
        onValueChanged = null,
        onDateTextChanged = { onEvent(TrackEvent.BackdateDateTextChanged(it)) },
        onTimeTextChanged = { onEvent(TrackEvent.BackdateTimeTextChanged(it)) },
        onSave = { onEvent(TrackEvent.BackdateSaveConfirmed) },
        onCancel = { onEvent(TrackEvent.BackdateCancelled) }
    )
}

@Composable
private fun EditDialog(
    modal: TrackModalState.Edit,
    vocabulary: List<String>,
    onEvent: (TrackEvent) -> Unit
) {
    val conflict = modal.validation == RecordValidation.Conflicting
    val checking = modal.validation == RecordValidation.Checking
    val readFailed = modal.validation == RecordValidation.ReadFailed
    val conflictMessage = if (conflict) {
        "This rating changed elsewhere. Saving will replace its current value, time, and chips. " +
            "Cancel and reopen to use the latest record."
    } else null
    val status = listOfNotNull(conflictMessage, modal.mutationError).joinToString("\n").ifEmpty { null }
    TimestampEditDialog(
        title = "Edit entry",
        value = modal.draft.value,
        dateText = modal.draft.dateText,
        timeText = modal.draft.timeText,
        timestampError = modal.timestampError,
        statusMessage = if (checking) "Checking record" else status,
        isSaving = modal.isSaving,
        canSave = modal.timestampError == null && !checking && !readFailed,
        saveLabel = if (conflict) "Save my changes" else "Save",
        onValueChanged = { onEvent(TrackEvent.EditValueChanged(it)) },
        onDateTextChanged = { onEvent(TrackEvent.EditDateTextChanged(it)) },
        onTimeTextChanged = { onEvent(TrackEvent.EditTimeTextChanged(it)) },
        onSave = { onEvent(TrackEvent.EditSaveConfirmed) },
        onCancel = { onEvent(TrackEvent.EditCancelled) },
        chips = modal.draft.chips,
        onChipToggled = { onEvent(TrackEvent.EditChipToggled(it)) },
        vocabulary = vocabulary,
        onRetryValidation = if (readFailed) {
            { onEvent(TrackEvent.DialogValidationRetry) }
        } else null
    )
}

@Composable
private fun NoteDialog(modal: TrackModalState.Note, onEvent: (TrackEvent) -> Unit) {
    val conflict = modal.validation == RecordValidation.Conflicting
    val checking = modal.validation == RecordValidation.Checking
    val readFailed = modal.validation == RecordValidation.ReadFailed
    val conflictMessage = if (conflict) {
        "This note changed elsewhere. Saving will replace the current note. " +
            "Cancel and reopen to use the latest note."
    } else null
    val status = listOfNotNull(conflictMessage, modal.mutationError).joinToString("\n").ifEmpty { null }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = { if (!modal.isSaving) onEvent(TrackEvent.NoteCancelled) },
        title = { Text("Edit note") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = modal.draft.text,
                    onValueChange = { onEvent(TrackEvent.NoteTextChanged(it)) },
                    enabled = !modal.isSaving,
                    label = { Text("Note") },
                    modifier = Modifier
                        .testTag("track_note_text")
                        .focusRequester(focusRequester)
                )
                if (checking) Text("Checking record")
                if (status != null) {
                    Text(
                        status,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (readFailed) {
                    TextButton(
                        onClick = { onEvent(TrackEvent.DialogValidationRetry) },
                        enabled = !modal.isSaving
                    ) { Text("Retry") }
                }
                if (modal.isSaving) {
                    Text(
                        "Saving",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(TrackEvent.NoteSaveConfirmed) },
                enabled = !modal.isSaving && !checking && !readFailed
            ) { Text(if (conflict) "Save my changes" else "Save") }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(TrackEvent.NoteCancelled) },
                enabled = !modal.isSaving
            ) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(modal: TrackModalState.Delete, onEvent: (TrackEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!modal.isSaving) onEvent(TrackEvent.DeleteCancelled) },
        title = { Text("Delete entry?") },
        text = {
            Column {
                Text(
                    "This permanently deletes the entry logged with value ${modal.entry.value}. " +
                        "This cannot be undone."
                )
                if (modal.mutationError != null) {
                    Text(
                        modal.mutationError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (modal.isSaving) {
                    Text(
                        "Saving",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(TrackEvent.DeleteConfirmed) },
                enabled = !modal.isSaving
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(TrackEvent.DeleteCancelled) },
                enabled = !modal.isSaving
            ) { Text("Cancel") }
        }
    )
}
