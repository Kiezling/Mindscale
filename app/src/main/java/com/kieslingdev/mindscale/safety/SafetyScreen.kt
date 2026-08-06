package com.kieslingdev.mindscale.safety

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kieslingdev.mindscale.ui.components.MsDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.allowsPhone

/** Every actionable element on this screen, per D-10. */
private val MinTarget = Modifier.heightIn(min = 48.dp)

@Composable
fun SafetyRoute(viewModel: SafetyViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SafetyScreen(uiState, viewModel, modifier)
}

/**
 * The Safety card (`docs/specs/SPEC-safety-card.md`).
 *
 * Every string comes from [SafetyCopy] and every step from [SAFETY_STEPS], so the frozen
 * wording and the canonical Stanley-Brown ordering are asserted once by test rather than
 * retyped here. Nothing on this screen is conditional on anything the user recorded.
 *
 * One `LazyColumn` with no height-capped container anywhere: at 200% font in landscape
 * this content must scroll rather than clip. Phase 12 shipped a dialog that hid its own
 * deletion warning at 200% font, and this is a worse screen on which to repeat that.
 */
@Composable
fun SafetyScreen(
    uiState: SafetyUiState,
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun run(action: SafetyAction) {
        try {
            context.startActivity(intentFor(action))
        } catch (missing: ActivityNotFoundException) {
            // No handler on this device. The number stays on screen so it can be dialled
            // by hand, and nothing about the attempt is recorded (D-7, D-8).
            viewModel.reportActionUnavailable(action)
        }
    }

    LazyColumn(
        modifier = modifier.testTag("safety_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(SafetyCopy.SCREEN_INTRO, style = MaterialTheme.typography.bodyLarge)
                Text(
                    SafetyCopy.HONESTY,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("safety_honesty")
                )
            }
        }

        item(key = "resources_heading") {
            SectionHeading(SafetyCopy.RESOURCES_HEADING, tag = "resources_heading")
        }

        items(SAFETY_RESOURCES, key = { "resource:${it.key}" }) { resource ->
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("resource_${resource.key}")
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name, then actions, then detail. Reaching help must not require
                    // reading a paragraph first — visually or in TalkBack order (D-3).
                    Text(resource.name, style = MaterialTheme.typography.titleMedium)
                    resource.actions.forEach { action ->
                        Button(
                            onClick = { run(action.action) },
                            modifier = MinTarget
                                .fillMaxWidth()
                                .testTag("resource_action_${action.label}")
                                .semantics { contentDescription = action.contentDescription }
                        ) { Text(action.label) }
                    }
                    Text(
                        resource.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "emergency") {
            Text(
                SafetyCopy.EMERGENCY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("safety_emergency")
            )
        }

        item(key = "verified") {
            Text(
                SafetyCopy.VERIFIED_ON,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("safety_verified")
            )
        }

        item(key = "plan_heading") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider()
                SectionHeading(SafetyCopy.PLAN_HEADING, tag = "plan_heading")
                Text(SafetyCopy.PLAN_INTRO, style = MaterialTheme.typography.bodyMedium)
                if (uiState.isEmpty) {
                    Text(
                        SafetyCopy.PLAN_EMPTY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("plan_empty")
                    )
                }
            }
        }

        // Canonical Stanley-Brown order, straight from SAFETY_STEPS. Never sorted,
        // filtered, reordered, or hidden here (D-4, Invariant 2).
        SAFETY_STEPS.forEach { stepContent ->
            val items = uiState.plan[stepContent.step].orEmpty()
            item(key = "step:${stepContent.step.name}") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeading(
                        stepContent.heading,
                        tag = "step_heading_${stepContent.step.name}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        stepContent.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (items.isEmpty()) {
                        Text(
                            SafetyCopy.STEP_EMPTY,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("step_empty_${stepContent.step.name}")
                        )
                    }
                    items.forEach { item ->
                        PlanRow(item, viewModel, onDial = ::run)
                    }
                    TextButton(
                        onClick = { viewModel.startAdd(stepContent.step) },
                        modifier = MinTarget.testTag("step_add_${stepContent.step.name}")
                            .semantics {
                                contentDescription = "Add to ${stepContent.heading}"
                            }
                    ) { Text(SafetyCopy.ADD_ITEM) }
                }
            }
        }

        uiState.message?.let { message ->
            item(key = "message:$message") {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .testTag("safety_message")
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }

        uiState.readError?.let { error ->
            item(key = "read_error") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .testTag("safety_error")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    TextButton(
                        onClick = viewModel::retry,
                        modifier = MinTarget.testTag("safety_retry")
                    ) { Text(SafetyCopy.RETRY) }
                }
            }
        }
    }

    uiState.editor?.let { editor -> PlanEditorDialog(editor, viewModel) }

    val pendingDelete = uiState.pendingDeleteId
        ?.let { id -> uiState.plan.values.flatten().firstOrNull { it.id == id } }
    if (pendingDelete != null) {
        MsDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(SafetyCopy.DELETE_TITLE) },
            text = { Text(SafetyCopy.deleteMessage(pendingDelete.text)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    modifier = MinTarget.testTag("plan_delete_confirm")
                ) { Text(SafetyCopy.DELETE_CONFIRM) }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelDelete,
                    modifier = MinTarget.testTag("plan_delete_cancel")
                ) { Text(SafetyCopy.CANCEL) }
            }
        )
    }
}

@Composable
private fun SectionHeading(
    text: String,
    tag: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    Text(
        text,
        style = style,
        modifier = Modifier.testTag(tag).semantics { heading() }
    )
}

@Composable
private fun PlanRow(
    item: SafetyPlanItem,
    viewModel: SafetyViewModel,
    onDial: (SafetyAction) -> Unit
) {
    val dial = viewModel.dialActionFor(item)
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().testTag("plan_row_${item.id}")
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.text, style = MaterialTheme.typography.bodyLarge)
            item.phone?.let { phone ->
                Text(
                    phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // A number with nothing dialable left in it gets no button at all, rather
            // than one that cannot work (D-7).
            if (dial != null) {
                OutlinedButton(
                    onClick = { onDial(dial) },
                    modifier = MinTarget
                        .testTag("plan_call_${item.id}")
                        .semantics { contentDescription = SafetyCopy.callContact(item.text) }
                ) { Text(SafetyCopy.callContact(item.text)) }
            }
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { viewModel.startEdit(item.id) },
                    modifier = MinTarget.testTag("plan_edit_${item.id}")
                ) { Text(SafetyCopy.EDIT_ITEM) }
                TextButton(
                    onClick = { viewModel.requestDelete(item.id) },
                    modifier = MinTarget.testTag("plan_delete_${item.id}")
                ) { Text(SafetyCopy.DELETE_ITEM) }
            }
        }
    }
}

@Composable
private fun PlanEditorDialog(editor: PlanEditor, viewModel: SafetyViewModel) {
    val heading = SAFETY_STEPS.first { it.step == editor.step }.heading
    MsDialog(
        onDismissRequest = viewModel::cancelEdit,
        title = { Text(heading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.textDraft,
                    onValueChange = viewModel::updateTextDraft,
                    label = { Text(SafetyCopy.TEXT_FIELD_LABEL) },
                    isError = editor.textError != null,
                    supportingText = { editor.textError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_text_field")
                )
                if (editor.step.allowsPhone) {
                    OutlinedTextField(
                        value = editor.phoneDraft,
                        onValueChange = viewModel::updatePhoneDraft,
                        label = { Text(SafetyCopy.PHONE_FIELD_LABEL) },
                        isError = editor.phoneError != null,
                        supportingText = { editor.phoneError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("plan_phone_field")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::saveEditor,
                enabled = !editor.saving,
                modifier = MinTarget.widthIn(min = 48.dp).testTag("plan_save")
            ) { Text(SafetyCopy.SAVE_ITEM) }
        },
        dismissButton = {
            TextButton(
                onClick = viewModel::cancelEdit,
                modifier = MinTarget.widthIn(min = 48.dp).testTag("plan_cancel")
            ) { Text(SafetyCopy.CANCEL) }
        }
    )
}
