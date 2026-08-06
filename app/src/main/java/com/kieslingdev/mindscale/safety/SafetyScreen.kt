package com.kieslingdev.mindscale.safety

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.kieslingdev.mindscale.ui.components.MsActionTone
import com.kieslingdev.mindscale.ui.components.MsCard
import com.kieslingdev.mindscale.ui.components.MsDialog
import com.kieslingdev.mindscale.ui.components.MsFieldSelectionColors
import com.kieslingdev.mindscale.ui.components.MsHairline
import com.kieslingdev.mindscale.ui.components.MsPillButton
import com.kieslingdev.mindscale.ui.components.MsTextAction
import com.kieslingdev.mindscale.ui.components.MsUppercaseText
import com.kieslingdev.mindscale.ui.components.msFieldColors
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.allowsPhone

/** Every actionable element on this screen, per D-10. */
private val MinTarget = Modifier.heightIn(min = MsSpacing.minTouchTarget)

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
        contentPadding = PaddingValues(MsSpacing.lgPlus),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus)
    ) {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.md)) {
                Text(
                    SafetyCopy.SCREEN_INTRO,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.ms.inkPrimary
                )
                Text(
                    SafetyCopy.HONESTY,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.inkTertiary,
                    modifier = Modifier.testTag("safety_honesty")
                )
            }
        }

        item(key = "resources_heading") {
            SectionHeading(SafetyCopy.RESOURCES_HEADING, tag = "resources_heading")
        }

        items(SAFETY_RESOURCES, key = { "resource:${it.key}" }) { resource ->
            MsCard(
                modifier = Modifier.fillMaxWidth().testTag("resource_${resource.key}"),
                contentPadding = MsSpacing.lgPlus
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)) {
                    // Name, then actions, then detail. Reaching help must not require
                    // reading a paragraph first — visually or in TalkBack order (D-3).
                    Text(
                        resource.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.ms.inkPrimary
                    )
                    // The one place this overhaul refuses the design's bare text action. These
                    // are the controls a person in crisis has to find, and a 9 sp gold label is a
                    // weaker affordance than a filled pill. `selected = true` is the design's own
                    // ink-fill-with-`onInk` treatment used as emphasis rather than as state, and
                    // the pair measures 11.98:1 light and 13.93:1 dark (D-11).
                    resource.actions.forEach { action ->
                        MsPillButton(
                            text = action.label,
                            onClick = { run(action.action) },
                            selected = true,
                            modifier = MinTarget
                                .fillMaxWidth()
                                .testTag("resource_action_${action.label}")
                                .semantics { contentDescription = action.contentDescription }
                        )
                    }
                    Text(
                        resource.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.ms.inkTertiary
                    )
                }
            }
        }

        item(key = "emergency") {
            Text(
                SafetyCopy.EMERGENCY,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.ms.inkSecondary,
                modifier = Modifier.testTag("safety_emergency")
            )
        }

        item(key = "verified") {
            Text(
                SafetyCopy.VERIFIED_ON,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.ms.inkQuaternary,
                modifier = Modifier.testTag("safety_verified")
            )
        }

        item(key = "plan_heading") {
            Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.md)) {
                MsHairline()
                SectionHeading(SafetyCopy.PLAN_HEADING, tag = "plan_heading")
                Text(
                    SafetyCopy.PLAN_INTRO,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.inkSecondary
                )
                if (uiState.isEmpty) {
                    Text(
                        SafetyCopy.PLAN_EMPTY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.ms.inkTertiary,
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
                Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)) {
                    SectionHeading(
                        stepContent.heading,
                        tag = "step_heading_${stepContent.step.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ms.inkQuaternary
                    )
                    Text(
                        stepContent.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ms.inkQuaternary
                    )
                    if (items.isEmpty()) {
                        Text(
                            SafetyCopy.STEP_EMPTY,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.ms.inkQuaternary,
                            modifier = Modifier.testTag("step_empty_${stepContent.step.name}")
                        )
                    }
                    items.forEach { item ->
                        PlanRow(item, viewModel, onDial = ::run)
                    }
                    MsTextAction(
                        text = SafetyCopy.ADD_ITEM,
                        onClick = { viewModel.startAdd(stepContent.step) },
                        modifier = MinTarget.testTag("step_add_${stepContent.step.name}")
                            .semantics {
                                contentDescription = "Add to ${stepContent.heading}"
                            }
                    )
                }
            }
        }

        uiState.message?.let { message ->
            item(key = "message:$message") {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.goldText,
                    modifier = Modifier
                        .testTag("safety_message")
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }

        uiState.readError?.let { error ->
            item(key = "read_error") {
                Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.ms.danger,
                        modifier = Modifier
                            .testTag("safety_error")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    MsTextAction(
                        text = SafetyCopy.RETRY,
                        onClick = viewModel::retry,
                        modifier = MinTarget.testTag("safety_retry")
                    )
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
                ) { DialogActionLabel(SafetyCopy.DELETE_CONFIRM) }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelDelete,
                    modifier = MinTarget.testTag("plan_delete_cancel")
                ) { DialogActionLabel(SafetyCopy.CANCEL) }
            }
        )
    }
}

/**
 * A section heading, uppercased and tracked as the design draws it.
 *
 * The tag and the `heading()` role sit on a **merging wrapper** rather than on the text node,
 * because [MsUppercaseText] uses `clearAndSetSemantics` and would otherwise wipe both off the leaf
 * it is applied to. `mergeDescendants = true` puts the restored original-case string back on the
 * same node that carries the tag and the role, so `onNodeWithTag`, `keyIsDefined(Heading)` and
 * `onNodeWithText` all still resolve exactly as `SafetyScreenTest` asserts.
 */
@Composable
private fun SectionHeading(
    text: String,
    tag: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelLarge,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.ms.inkSecondary
) {
    Box(
        modifier = Modifier
            .testTag(tag)
            .semantics(mergeDescendants = true) { heading() }
    ) {
        MsUppercaseText(text = text, style = style, color = color)
    }
}

@Composable
private fun PlanRow(
    item: SafetyPlanItem,
    viewModel: SafetyViewModel,
    onDial: (SafetyAction) -> Unit
) {
    val dial = viewModel.dialActionFor(item)
    MsCard(
        modifier = Modifier.fillMaxWidth().testTag("plan_row_${item.id}"),
        contentPadding = MsSpacing.mdPlus
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)) {
            Text(
                item.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.ms.inkPrimary
            )
            item.phone?.let { phone ->
                Text(
                    phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.inkTertiary
                )
            }
            // A number with nothing dialable left in it gets no button at all, rather
            // than one that cannot work (D-7).
            if (dial != null) {
                MsPillButton(
                    text = SafetyCopy.callContact(item.text),
                    onClick = { onDial(dial) },
                    modifier = MinTarget
                        .testTag("plan_call_${item.id}")
                        .semantics { contentDescription = SafetyCopy.callContact(item.text) }
                )
            }
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                MsTextAction(
                    text = SafetyCopy.EDIT_ITEM,
                    onClick = { viewModel.startEdit(item.id) },
                    modifier = MinTarget.testTag("plan_edit_${item.id}")
                )
                MsTextAction(
                    text = SafetyCopy.DELETE_ITEM,
                    onClick = { viewModel.requestDelete(item.id) },
                    tone = MsActionTone.Danger,
                    modifier = MinTarget.testTag("plan_delete_${item.id}")
                )
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
            MsFieldSelectionColors {
                Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)) {
                    OutlinedTextField(
                        value = editor.textDraft,
                        onValueChange = viewModel::updateTextDraft,
                        label = { Text(SafetyCopy.TEXT_FIELD_LABEL) },
                        isError = editor.textError != null,
                        supportingText = { editor.textError?.let { Text(it) } },
                        singleLine = true,
                        colors = msFieldColors(),
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
                            colors = msFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("plan_phone_field")
                        )
                    }
                }
            }
        },
        // The button stays a Material `TextButton` so its enabled/disabled semantics are
        // untouched — `plan_save` is disabled while the editor saves. Only the label is
        // wrapped (D-13).
        confirmButton = {
            TextButton(
                onClick = viewModel::saveEditor,
                enabled = !editor.saving,
                modifier = MinTarget.widthIn(min = MsSpacing.minTouchTarget).testTag("plan_save")
            ) { DialogActionLabel(SafetyCopy.SAVE_ITEM) }
        },
        dismissButton = {
            TextButton(
                onClick = viewModel::cancelEdit,
                modifier = MinTarget.widthIn(min = MsSpacing.minTouchTarget).testTag("plan_cancel")
            ) { DialogActionLabel(SafetyCopy.CANCEL) }
        }
    )
}

/**
 * A dialog action label, uppercased in place (D-13). The button stays a Material [TextButton] so
 * its enabled and disabled semantics are untouched; only the label is wrapped, and the colour is
 * left unspecified so it inherits `TextButton`'s content colour, which D-9 of the foundation
 * resolves to `goldText`.
 */
@Composable
private fun DialogActionLabel(text: String) {
    MsUppercaseText(text = text, style = MaterialTheme.typography.labelMedium)
}
