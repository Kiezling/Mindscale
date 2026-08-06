package com.kieslingdev.mindscale.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.kieslingdev.mindscale.ui.components.MsActionTone
import com.kieslingdev.mindscale.ui.components.MsCard
import com.kieslingdev.mindscale.ui.components.MsChip
import com.kieslingdev.mindscale.ui.components.MsDialog
import com.kieslingdev.mindscale.ui.components.MsEyebrow
import com.kieslingdev.mindscale.ui.components.MsFieldSelectionColors
import com.kieslingdev.mindscale.ui.components.MsHairline
import com.kieslingdev.mindscale.ui.components.MsTextAction
import com.kieslingdev.mindscale.ui.components.MsUppercaseText
import com.kieslingdev.mindscale.ui.components.msFieldColors
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.safety.SafetyCopy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProfileRoute(
    viewModel: ReportProfileViewModel,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSafety: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(uiState, viewModel, onOpenReport, onOpenSettings, modifier, onOpenSafety)
}

@Composable
fun ProfileScreen(
    uiState: ReportProfileUiState,
    viewModel: ReportProfileViewModel,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSafety: () -> Unit = {}
) {
    val pendingDelete = uiState.pendingDeleteScoreId?.let { id -> uiState.scores.firstOrNull { it.id == id } }
    LazyColumn(
        modifier = modifier.testTag("profile_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(MsSpacing.lgPlus),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus)
    ) {
        item(key = "identity") {
            ProfileSection("Your profile") {
                MsFieldSelectionColors {
                    OutlinedTextField(
                        value = uiState.nameDraft,
                        onValueChange = viewModel::updateNameDraft,
                        label = { Text("Display name (optional)") },
                        supportingText = { Text("Stored only on this device. Exports you create can contain your name.") },
                        singleLine = true,
                        colors = msFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("profile_name")
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MsTextAction(
                        text = "Save name",
                        onClick = { viewModel.saveName(false) },
                        enabled = uiState.nameDirty,
                        modifier = Modifier.testTag("profile_name_save")
                    )
                    if (uiState.nameConflict) {
                        MsTextAction(
                            text = "Replace saved name",
                            onClick = { viewModel.saveName(true) },
                            modifier = Modifier.testTag("profile_name_replace")
                        )
                    }
                }
            }
        }
        item(key = "stats") {
            ProfileSection("Recording facts") {
                val since = uiState.stats.firstRecordedTs?.let {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                } ?: "Nothing recorded yet"
                Text(
                    "Recording since: $since",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.inkSecondary
                )
                Text(
                    "${uiState.stats.ratingCount} ratings · ${uiState.stats.sleepCount} sleep periods · ${uiState.stats.markerCount} marked events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.inkSecondary
                )
            }
        }
        item(key = "actions") {
            // These two must stay above the fold at 100% font: `NavigationTest` and
            // `ProfileReportScreenTest` both click them without scrolling first (D-9,
            // Invariant 11).
            ProfileSection("Use your records") {
                MsCard(modifier = Modifier.fillMaxWidth(), contentPadding = MsSpacing.xxxs) {
                    NavigationRow("Clinician summary", onOpenReport, "profile_open_report")
                    MsHairline(faint = true)
                    NavigationRow("Settings", onOpenSettings, "profile_open_settings")
                }
            }
        }
        item(key = "safety") {
            ProfileSection("Safety") {
                MsCard(modifier = Modifier.fillMaxWidth(), contentPadding = MsSpacing.xxxs) {
                    NavigationRow(SafetyCopy.PROFILE_ROW, onOpenSafety, "profile_open_safety")
                }
            }
        }
        item(key = "score_intro") {
            MsHairline()
            Column(
                Modifier.padding(top = MsSpacing.lgPlus),
                verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)
            ) {
                MsEyebrow("Totals from elsewhere")
                Text(
                    "Enter a total only when PHQ-8 or GAD-7 was completed elsewhere. " +
                        "MindScale does not show the questions, administer either questionnaire, calculate a total, or interpret it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.inkSecondary
                )
                Text(
                    "Total entered by you from a result obtained elsewhere. MindScale did not administer or calculate it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ms.inkQuaternary
                )
            }
        }
        item(key = "score_form") {
            ProfileSection(
                if (uiState.editingScoreId == null) "Add an external total" else "Edit external total",
                modifier = Modifier.testTag("score_form")
            ) {
                MsCard(modifier = Modifier.fillMaxWidth(), contentPadding = MsSpacing.lgPlus) {
                    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExternalInstrument.entries.forEach { instrument ->
                                MsChip(
                                    text = instrument.visibleLabel,
                                    selected = uiState.scoreInstrument == instrument,
                                    onClick = { viewModel.selectInstrument(instrument) },
                                    modifier = Modifier
                                        .testTag("score_instrument_${instrument.name}")
                                        .semantics { selected = uiState.scoreInstrument == instrument }
                                )
                            }
                        }
                        MsFieldSelectionColors {
                            OutlinedTextField(
                                value = uiState.scoreDateDraft,
                                onValueChange = viewModel::updateScoreDate,
                                label = { Text("Assessment date") },
                                supportingText = {
                                    Text(uiState.scoreDateError ?: "YYYY-MM-DD · required · not later than today")
                                },
                                singleLine = true,
                                isError = uiState.scoreDateError != null,
                                colors = msFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("score_date")
                            )
                            OutlinedTextField(
                                value = uiState.scoreTotalDraft,
                                onValueChange = viewModel::updateScoreTotal,
                                label = { Text("${uiState.scoreInstrument.visibleLabel} total from the external result") },
                                supportingText = {
                                    Text(uiState.scoreTotalError ?: "Whole number 0–${uiState.scoreInstrument.maxTotal}")
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = uiState.scoreTotalError != null,
                                colors = msFieldColors(),
                                modifier = Modifier.fillMaxWidth().testTag("score_total")
                            )
                        }
                        uiState.scoreFormError?.let { LiveMessage(it, isError = true, tag = "score_error") }
                        // L-5. The prototype crowds this row's trailing `ADD` against the right
                        // edge. The gaps are even and the last action gains a trailing gutter
                        // equal to the inter-element gap. The gutter sits on a wrapper `Box`, not
                        // on the action's own modifier, so the control's reported bounds stay its
                        // touch area and the gutter is measurable from outside — the same
                        // technique Phase 16 used for L-4 (D-3).
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MsTextAction(
                                text = if (uiState.editingScoreId == null) "Add total" else "Save changes",
                                onClick = viewModel::saveScore,
                                modifier = Modifier.testTag("score_save")
                            )
                            if (uiState.editingScoreId != null) {
                                Box(modifier = Modifier.padding(end = MsSpacing.lgPlus)) {
                                    MsTextAction(
                                        text = "Cancel edit",
                                        onClick = viewModel::cancelScoreEdit,
                                        tone = MsActionTone.Muted,
                                        modifier = Modifier.testTag("score_edit_cancel")
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.padding(end = MsSpacing.lgPlus))
                            }
                        }
                    }
                }
            }
        }
        if (uiState.scores.isEmpty()) {
            item(key = "scores_empty") {
                Text(
                    "No externally obtained totals stored.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.inkTertiary,
                    modifier = Modifier.testTag("scores_empty")
                )
            }
        } else {
            item(key = "scores_title") { MsEyebrow("Stored external totals") }
            items(uiState.scores, key = { "score:${it.id}" }) { score ->
                MsCard(modifier = Modifier.fillMaxWidth(), contentPadding = MsSpacing.lg) {
                    Column(
                        Modifier.fillMaxWidth().testTag("score_row_${score.id}"),
                        verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
                    ) {
                        Text(
                            "${score.instrument.visibleLabel} total ${score.total}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.ms.inkPrimary
                        )
                        Text(
                            LocalDate.ofEpochDay(score.assessedEpochDay).toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.ms.goldText
                        )
                        Text(
                            "Entered by you from a result obtained elsewhere. MindScale did not administer or calculate it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.ms.inkQuaternary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MsTextAction(
                                text = "Edit",
                                onClick = { viewModel.editScore(score.id) },
                                modifier = Modifier.testTag("score_edit_${score.id}")
                            )
                            MsTextAction(
                                text = "Delete",
                                onClick = { viewModel.requestDeleteScore(score.id) },
                                tone = MsActionTone.Danger,
                                modifier = Modifier.testTag("score_delete_${score.id}")
                            )
                        }
                    }
                }
            }
        }
        uiState.message?.let { message ->
            item(key = "profile_message:$message") { LiveMessage(message, isError = false, tag = "profile_message") }
        }
        uiState.error?.let { error ->
            item(key = "profile_error") {
                ProfileSection("Could not load Profile") {
                    LiveMessage(error, isError = true, tag = "profile_error")
                    MsTextAction(text = "Retry", onClick = viewModel::retry)
                }
            }
        }
    }

    if (pendingDelete != null) {
        MsDialog(
            onDismissRequest = viewModel::cancelDeleteScore,
            title = { Text("Delete external total?") },
            text = {
                Text(
                    "Delete ${pendingDelete.instrument.visibleLabel} total ${pendingDelete.total} from " +
                        "${LocalDate.ofEpochDay(pendingDelete.assessedEpochDay)}?"
                )
            },
            // The buttons stay Material `TextButton`s so every assertion target stays on the same
            // node; only the labels are wrapped (D-13).
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteScore, modifier = Modifier.testTag("score_delete_confirm")) {
                    DialogActionLabel("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteScore, modifier = Modifier.testTag("score_delete_cancel")) {
                    DialogActionLabel("Cancel")
                }
            }
        )
    }
}

/**
 * The design's row on a card: a label at the leading edge with the whole row as the target
 * (`dark-settings-bottom.png`'s `Export everything` pattern). MindScale invents no trailing
 * string, because the design's `JSON` / `CSV` / `Keeps your data` labels are copy it does not
 * have (D-16).
 */
@Composable
private fun NavigationRow(label: String, onClick: () -> Unit, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MsSpacing.minTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = MsSpacing.lg, vertical = MsSpacing.mdPlus)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.ms.inkPrimary
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)) {
        MsEyebrow(title)
        content()
    }
}

/** A dialog action label, uppercased in place (D-13). */
@Composable
private fun DialogActionLabel(text: String) {
    MsUppercaseText(text = text, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun LiveMessage(message: String, isError: Boolean, tag: String) {
    Text(
        message,
        color = if (isError) MaterialTheme.ms.danger else MaterialTheme.ms.goldText,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag(tag).semantics { liveRegion = LiveRegionMode.Polite }
    )
}
