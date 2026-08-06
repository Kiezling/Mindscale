package com.kieslingdev.mindscale.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.kieslingdev.mindscale.ui.components.MsActionTone
import com.kieslingdev.mindscale.ui.components.MsCard
import com.kieslingdev.mindscale.ui.components.MsDialog
import com.kieslingdev.mindscale.ui.components.MsEyebrow
import com.kieslingdev.mindscale.ui.components.MsFieldSelectionColors
import com.kieslingdev.mindscale.ui.components.MsHairline
import com.kieslingdev.mindscale.ui.components.MsSegmentedControl
import com.kieslingdev.mindscale.ui.components.MsTextAction
import com.kieslingdev.mindscale.ui.components.MsUppercaseText
import com.kieslingdev.mindscale.ui.components.msFieldColors
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.breathing.BreathingCopy
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.ThemeMode
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class SettingsFocus { TOP, ANCHORS, DATA }

private val BACKUP_IMPORT_MIME_TYPES = arrayOf("application/json")
private val RECORDS_IMPORT_MIME_TYPES =
    arrayOf("text/csv", "text/comma-separated-values", "text/plain")

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    focus: SettingsFocus,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) viewModel.documentPickerCanceled()
        else {
            val document = uiState.pendingDocument
            if (document == null) viewModel.documentWriteFailed()
            else {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(document.contents) }
                        } ?: error("Document provider returned no stream")
                    }.onSuccess { viewModel.documentWriteSucceeded() }
                        .onFailure { viewModel.documentWriteFailed() }
                }
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) viewModel.documentPickerCanceled()
        else {
            val document = uiState.pendingDocument
            if (document == null) viewModel.documentWriteFailed()
            else {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(document.contents) }
                        } ?: error("Document provider returned no stream")
                    }.onSuccess { viewModel.documentWriteSucceeded() }
                        .onFailure { viewModel.documentWriteFailed() }
                }
            }
        }
    }

    val pending = uiState.pendingDocument
    LaunchedEffect(pending) {
        if (pending != null) {
            if (pending.kind == ExportKind.RECORDS) csvLauncher.launch(pending.filename)
            else jsonLauncher.launch(pending.filename)
        }
    }

    // Read-only document access. No persistable permission is taken, no storage
    // permission is declared, and content is validated regardless of the declared MIME
    // type because providers routinely mislabel CSV (Phase 12, D-11).
    // Lint cannot follow the deferred opener across the lambda boundary. The stream is
    // opened lazily on the ViewModel's IO context and always closed there by
    // `open().use(::readBoundedUtf8)`, which is covered by SettingsImportViewModelTest.
    @Suppress("Recycle")
    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) viewModel.importPickerCanceled()
        else viewModel.importFileSelected(ImportKind.BACKUP_RESTORE) {
            context.contentResolver.openInputStream(uri) ?: error("No stream")
        }
    }
    @Suppress("Recycle")
    val recordsImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) viewModel.importPickerCanceled()
        else viewModel.importFileSelected(ImportKind.RECORDS_MERGE) {
            context.contentResolver.openInputStream(uri) ?: error("No stream")
        }
    }

    val importLaunch = uiState.importLaunch
    LaunchedEffect(importLaunch) {
        when (importLaunch) {
            ImportKind.BACKUP_RESTORE -> backupImportLauncher.launch(BACKUP_IMPORT_MIME_TYPES)
            ImportKind.RECORDS_MERGE -> recordsImportLauncher.launch(RECORDS_IMPORT_MIME_TYPES)
            null -> return@LaunchedEffect
        }
        viewModel.importLaunchHandled()
    }

    SettingsScreen(uiState = uiState, focus = focus, viewModel = viewModel, modifier = modifier)
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    focus: SettingsFocus,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    // The deep link's contract is "focus the anchors section" and "focus the data section", not
    // "scroll to item 4". The item list is unchanged in count and order by this phase, so the two
    // indices are unchanged too — but `SettingsVisualTest` now asserts the *behaviour* rather than
    // the number, so a later restructure moves the index and keeps the contract (D-8).
    LaunchedEffect(focus) {
        listState.animateScrollToItem(when (focus) {
            SettingsFocus.TOP -> 0
            SettingsFocus.ANCHORS -> 4
            SettingsFocus.DATA -> 12
        })
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("settings_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(MsSpacing.lgPlus),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.lg)
    ) {
        item(key = "appearance") {
            SettingsSection("Appearance") {
                ChoiceRow(
                    values = ThemeMode.entries,
                    selected = uiState.settings.themeMode,
                    label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    onSelected = viewModel::setTheme
                )
            }
        }
        item(key = "time") {
            SettingsSection("Time format") {
                ChoiceRow(
                    values = HourFormat.entries,
                    selected = uiState.settings.hourFormat,
                    label = { if (it == HourFormat.TWELVE) "12-hour" else "24-hour" },
                    onSelected = viewModel::setHourFormat
                )
            }
        }
        item(key = "hold") {
            SettingsSection("An entry ends after") {
                ChoiceRow(
                    values = HoldDuration.entries,
                    selected = uiState.settings.holdDuration,
                    label = { "${it.hours}h" },
                    onSelected = viewModel::setHoldDuration
                )
                BodyText(
                    "Waking hours. Sleep pauses this clock. This changes how Insights treats " +
                        "gaps across your history; your records do not change.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item(key = "divider_before_anchors") { MsHairline() }
        item(key = "anchors") {
            SettingsSection("What the numbers mean to you") {
                BodyText("Personal anchors help the same number mean the same thing months later.")
                // The design draws the three anchors as one card of hairline-separated rows,
                // each headed by its gold numeral. The numeral is already inside the label, so
                // the card is the only thing added (D-8).
                MsCard(modifier = Modifier.fillMaxWidth(), contentPadding = MsSpacing.lgPlus) {
                    MsFieldSelectionColors {
                        AnchorField("2 — noticeable", uiState.anchorDraft.anchor2, viewModel::updateAnchor2, "anchor_2")
                        MsHairline(faint = true)
                        AnchorField("5 — changing what I do", uiState.anchorDraft.anchor5, viewModel::updateAnchor5, "anchor_5")
                        MsHairline(faint = true)
                        AnchorField("8 — most of what is happening", uiState.anchorDraft.anchor8, viewModel::updateAnchor8, "anchor_8")
                    }
                }
                uiState.anchorError?.let { ErrorText(it) }
                MsTextAction(
                    text = "Save anchors",
                    onClick = viewModel::saveAnchors,
                    modifier = Modifier.testTag("save_anchors")
                )
            }
        }
        item(key = "onset_words") {
            SettingsSection("What was happening") {
                BodyText("Separate onset words with commas or new lines.")
                MsFieldSelectionColors {
                    OutlinedTextField(
                        value = uiState.chipDraft,
                        onValueChange = viewModel::updateChipDraft,
                        label = { Text("Onset words") },
                        minLines = 3,
                        colors = msFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("onset_words")
                    )
                }
                uiState.chipError?.let { ErrorText(it) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MsTextAction(
                        text = "Save words",
                        onClick = viewModel::saveOnsetWords,
                        modifier = Modifier.testTag("save_onset_words")
                    )
                    MsTextAction(
                        text = "Restore defaults",
                        onClick = viewModel::restoreDefaultWords,
                        tone = MsActionTone.Muted
                    )
                }
            }
        }
        item(key = "divider_before_preferences") { MsHairline() }
        item(key = "sleep") {
            SettingSwitch(
                "Sleep and Wake",
                "Show capture controls for falling asleep and waking up.",
                uiState.settings.sleepOn,
                viewModel::setSleepOn
            )
        }
        item(key = "chips") {
            SettingSwitch(
                "Ask what was happening",
                "Prompt for onset words after a symptom begins.",
                uiState.settings.askChips,
                viewModel::setAskChips
            )
        }
        item(key = "notes") {
            SettingSwitch(
                "Hide notes in lists",
                "Keep note actions available while folding preview text.",
                uiState.settings.hideNotes,
                viewModel::setHideNotes
            )
        }
        item(key = "breathing") {
            SettingSwitch(
                BreathingCopy.SETTING_TITLE,
                BreathingCopy.SETTING_DESCRIPTION,
                uiState.settings.breathingOn,
                viewModel::setBreathingOn
            )
        }
        item(key = "pause") {
            SettingSwitch(
                if (uiState.settings.paused) "Tracking paused" else "Pause tracking",
                "Recorded data and the Full Log remain available.",
                uiState.settings.paused,
                viewModel::setPaused
            )
        }
        item(key = "divider_before_data") { MsHairline() }
        item(key = "data") {
            SettingsSection("Your data") {
                BodyText("Exports stay local and go only to the document location you choose.")
                BodyText(
                    "Records CSV contains ratings, sleep, marked events, and breathing sessions only. It " +
                        "excludes your Profile name and external PHQ-8/GAD-7 totals. JSON backup includes " +
                        "them; Clinician summary exports the bounded factual summary."
                )
                // The design's `Export everything` card: one row per action, hairline-separated,
                // the destructive one in `danger`. MindScale invents no trailing `JSON`/`CSV`
                // label, because those are strings it does not have (D-16).
                MsCard(modifier = Modifier.fillMaxWidth(), contentPadding = MsSpacing.xxxs) {
                    ActionRow("Export backup", viewModel::requestBackup, "export_backup")
                    MsHairline(faint = true)
                    ActionRow("Export records", viewModel::requestRecordsCsv, "export_records")
                    MsHairline(faint = true)
                    ActionRow(
                        "Export, then erase everything",
                        viewModel::requestExportThenErase,
                        "export_then_erase",
                        danger = true
                    )
                }
                if (uiState.preparingExport) {
                    BodyText("Preparing export…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item(key = "import") {
            SettingsSection("Bring data back") {
                BodyText(
                    "Restoring a backup replaces everything on this device. Importing a " +
                        "records CSV only adds ratings, sleep, marked events, and breathing " +
                        "sessions. MindScale shows exactly what will change and waits for you " +
                        "to confirm."
                )
                MsCard(modifier = Modifier.fillMaxWidth(), contentPadding = MsSpacing.xxxs) {
                    ActionRow(
                        "Restore from backup",
                        viewModel::requestBackupRestore,
                        "import_backup",
                        contentDescription = "Restore from a MindScale JSON backup"
                    )
                    MsHairline(faint = true)
                    ActionRow(
                        "Import records",
                        viewModel::requestRecordsImport,
                        "import_records",
                        contentDescription = "Import a MindScale records CSV"
                    )
                }
                if (uiState.importing) {
                    Text(
                        "Checking that file…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ms.goldText,
                        modifier = Modifier
                            .testTag("import_progress")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                uiState.importError?.let { error ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            error,
                            color = MaterialTheme.ms.danger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_error")
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                        MsTextAction(
                            text = "Dismiss",
                            onClick = viewModel::dismissImportError,
                            tone = MsActionTone.Muted
                        )
                    }
                }
            }
        }
        uiState.readError?.let { error ->
            item(key = "read_error") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ErrorText(error)
                    MsTextAction(text = "Retry", onClick = viewModel::retrySettingsRead)
                }
            }
        }
        uiState.message?.let { message ->
            item(key = "message") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.ms.inkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.retryDocument != null) {
                        MsTextAction(text = "Retry export", onClick = viewModel::retryDocumentWrite)
                    }
                    MsTextAction(
                        text = "Dismiss",
                        onClick = viewModel::dismissMessage,
                        tone = MsActionTone.Muted
                    )
                }
            }
        }
    }

    uiState.pendingImport?.let { pending ->
        MsDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(pending.preview.title) },
            text = {
                // Scrollable so the whole factual preview stays readable at 200% font
                // scale, in landscape, and on small screens. No fixed height cap: the
                // dialog already constrains its body, and capping it pushed the
                // permanent-deletion sentence below the fold at large font sizes while
                // leaving the confirm action visible.
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .testTag("import_preview"),
                    verticalArrangement = Arrangement.spacedBy(MsSpacing.md)
                ) {
                    pending.preview.lines.forEach { Text(it) }
                }
            },
            // Both buttons stay Material `TextButton`s: `assertIsNotEnabled` and every other
            // assertion target lives on that node, and both are disabled while the transaction
            // runs. Only the labels are wrapped (D-13).
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmImport,
                    enabled = !uiState.importing,
                    modifier = Modifier
                        .heightIn(min = MsSpacing.minTouchTarget)
                        .testTag("confirm_import")
                ) { DialogActionLabel(pending.preview.confirmLabel) }
            },
            dismissButton = {
                // Disabled once the mutation is running: the transaction can no longer be
                // cancelled, so offering Cancel would misreport what happened to the data.
                TextButton(
                    onClick = viewModel::cancelImport,
                    enabled = !uiState.importing,
                    modifier = Modifier
                        .heightIn(min = MsSpacing.minTouchTarget)
                        .testTag("cancel_import")
                ) { DialogActionLabel("Cancel") }
            }
        )
    }

    uiState.eraseConfirmation?.let { confirmation ->
        MsDialog(
            onDismissRequest = viewModel::cancelErase,
            title = { Text("Erase everything on this device?") },
            text = {
                Text(
                    "This permanently deletes ${confirmation.entryCount} ratings, " +
                        "${confirmation.sleepCount} sleep intervals, and ${confirmation.markerCount} markers. " +
                        "Your Profile name, all externally obtained totals, your safety plan " +
                        "(${confirmation.safetyPlanItemCount} " +
                        "${if (confirmation.safetyPlanItemCount == 1) "line" else "lines"}), and " +
                        "${confirmation.breathingSessionCount} breathing " +
                        "${if (confirmation.breathingSessionCount == 1) "session" else "sessions"} " +
                        "are also deleted. " +
                        "Anchors, custom words, preferences, drafts, and retained export text reset too."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmErase, modifier = Modifier.testTag("confirm_erase")) {
                    DialogActionLabel("Erase everything")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelErase) { DialogActionLabel("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.md)) {
        MsEyebrow(title)
        content()
    }
}

/** Prose on the page, at the compliant emphasis levels rather than the prototype's alphas. */
@Composable
private fun BodyText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    Text(text, style = style, color = MaterialTheme.ms.inkTertiary)
}

/**
 * The design's segmented control, and layout fix L-6.
 *
 * The prototype's segments size to their content, so `12-HOUR`/`24-HOUR` and `8H`/`12H`/`16H`/`24H`
 * render at visibly different widths. `MsSegmentedControl` equal-weights them, which has been its
 * contract since Phase 15 — this is simply the first screen to call it (D-4).
 *
 * The `horizontalScroll` this replaces is gone: equal-weight segments are all visible at once, so
 * every choice stays reachable without a scroll. What must survive the conversion is listed in
 * D-6, and every item of it is asserted — the visible label in original case, the
 * `"<label>, selected"` description on the node that carries the click, the `selected` state, the
 * callback, the enum's order, and a 48 dp target.
 */
@Composable
private fun <T> ChoiceRow(values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    MsSegmentedControl(
        options = values.map(label),
        selectedIndex = values.indexOf(selected),
        onSelect = { onSelected(values[it]) },
        optionModifier = { index ->
            val value = values[index]
            Modifier.semantics {
                contentDescription =
                    "${label(value)}, ${if (value == selected) "selected" else "not selected"}"
            }
        }
    )
}

@Composable
private fun AnchorField(label: String, value: String, onValueChange: (String) -> Unit, tag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        colors = msFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}

/**
 * The design's action row, at line 1720 of the design authority: a label at the leading edge with
 * the whole row as the target. The destructive one is painted in `danger`, as the design paints
 * `Export, then erase everything` (D-8).
 */
@Composable
private fun ActionRow(
    label: String,
    onClick: () -> Unit,
    tag: String,
    danger: Boolean = false,
    contentDescription: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MsSpacing.minTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = MsSpacing.lg, vertical = MsSpacing.mdPlus)
            .testTag(tag)
            .then(
                if (contentDescription == null) Modifier
                else Modifier.semantics { this.contentDescription = contentDescription }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (danger) MaterialTheme.ms.danger else MaterialTheme.ms.inkPrimary
        )
    }
}

/**
 * The design's preference row: title, description beneath, switch trailing, on a card of
 * hairline-separated siblings. The rows stay one `LazyColumn` item each, so the section reads as a
 * run of cards rather than one continuous card — the cost of keeping the item list's identity, and
 * the reason is that `SettingsFocus` scrolls by item index (D-8).
 */
@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    MsCard(modifier = Modifier.fillMaxWidth(), contentPadding = MsSpacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title. $description" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = MsSpacing.mdPlus)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.ms.inkPrimary
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ms.inkQuaternary
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.ms.danger, style = MaterialTheme.typography.bodySmall)
}

/** A dialog action label, uppercased in place (D-13). */
@Composable
private fun DialogActionLabel(text: String) {
    MsUppercaseText(text = text, style = MaterialTheme.typography.labelMedium)
}
