package com.kieslingdev.mindscale.track

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.breathing.BreathingCopy
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.safety.SafetyCopy
import com.kieslingdev.mindscale.settings.SettingsFocus
import com.kieslingdev.mindscale.settings.vocabularyForEntry
import com.kieslingdev.mindscale.ui.components.MsActionTone
import com.kieslingdev.mindscale.ui.components.MsCard
import com.kieslingdev.mindscale.ui.components.MsChip
import com.kieslingdev.mindscale.ui.components.MsCircularHeaderButton
import com.kieslingdev.mindscale.ui.components.MsDialog
import com.kieslingdev.mindscale.ui.components.MsEyebrow
import com.kieslingdev.mindscale.ui.components.MsHairline
import com.kieslingdev.mindscale.ui.components.MsPillButton
import com.kieslingdev.mindscale.ui.components.MsTextAction
import com.kieslingdev.mindscale.ui.components.MsToastPill
import com.kieslingdev.mindscale.ui.components.MsUppercaseText
import com.kieslingdev.mindscale.ui.theme.MsShapes
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Numpad key order/grouping is frozen by Invariant 12: a 3x3 grid of 1-9,
 * then a visually distinct group of 0 and 10 below it.
 *
 * `docs/specs/SPEC-track-and-log-visual.md` D-7 records how that survives this phase. What
 * Invariant 12 freezes is order and grouping, and both are intact. What changes is the *means* of
 * distinction: the design draws all twelve keys from one `border-radius:50%` rule (line 1204) and
 * sets the last group apart by **position**, so `0` and `10` are now identical circles on the only
 * row that breaks the 3-column alignment, rather than pills in a different tone.
 */
private val NumpadRows = listOf(
    listOf(1, 2, 3),
    listOf(4, 5, 6),
    listOf(7, 8, 9)
)
private val NumpadEdgeKeys = listOf(0, 10)

// ── one-off geometry from the design authority, per D-18 ──────────────────────

/** `padWrapStyle`, line 1218: `max-width:288px`. Caps the pad; it shrinks on a narrow device. */
private val PadMaxWidth = 288.dp

/** The pad's `grid-gap`, line 100. Also the column rhythm L-1's centred row must keep (D-6). */
private val PadKeyGap = MsSpacing.lg

/** `padWrapStyle`'s armed `box-shadow: 0 0 0 4px`, rendered as a concentric ring (D-5, D-13). */
private val PadArmedSpread = MsSpacing.xxs

/** The pad wrapper's 24 dp radius, line 1218, plus the spread so the outer ring stays concentric. */
private val PadOuterRadius = 28.dp

/** Sleep/Wake and the marker block, lines 133 and 144: `max-width:256px`. */
private val CenteredColumnMaxWidth = 256.dp

/** `toggleBase`, line 1222: `height:44px`. Painted at 44 dp; the touch target is 48 dp (D-23). */
private val ToggleHeight = 44.dp

/** `dotStyle`, line 1240: a 42 dp circle. */
private val EntryDotSize = 42.dp

/**
 * The edit dialog's value slot. Not from the design — the prototype has no stepper — but the
 * minimum that keeps `Value: 10` from shifting the `+` control relative to `Value: 8`.
 */
private val DialogValueSlotWidth = 96.dp

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
    onOpenSettings: (SettingsFocus) -> Unit = {},
    onOpenSafety: () -> Unit = {},
    onOpenBreathing: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrackScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenSettings = onOpenSettings,
        onOpenSafety = onOpenSafety,
        onOpenBreathing = onOpenBreathing,
        modifier = modifier
    )
}

/**
 * Stateless, previewable Track screen. Drives all UI tests.
 *
 * The list carries no uniform `verticalArrangement`. Each item supplies the design's own top
 * margin instead, because a uniform gap would push the recent-entry rows apart where the design
 * runs them tight against their hairlines (D-13, D-18).
 */
@Composable
fun TrackScreen(
    uiState: TrackUiState,
    onEvent: (TrackEvent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: (SettingsFocus) -> Unit = {},
    onOpenSafety: () -> Unit = {},
    onOpenBreathing: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("track_screen"),
            contentPadding = PaddingValues(MsSpacing.lgPlus)
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
                // The design puts the readout and the help toggle on one row, lines 73-86. They
                // were two separate items; combining them is layout only. One consequence is
                // recorded rather than discovered later: when the anchor prompt is showing, the
                // help toggle now sits above it rather than below (D-13).
                item {
                    ReadoutAndHelpRow(
                        readout = uiState.transientReadout,
                        helpOpen = uiState.helpOpen,
                        onEvent = onEvent
                    )
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
                if (uiState.helpOpen) {
                    item { HelpCard() }
                }
                item {
                    Numpad(
                        armed = uiState.armedCapture,
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

            // Shown only when the setting is on and tracking is not paused. A breathing
            // session writes a dated record, and pause means the capture surface is
            // hidden, so offering a control that creates records during a pause would
            // contradict what pause means (`docs/specs/SPEC-paced-breathing.md`, D-7).
            // Both conditions are settings the user set, never anything recorded.
            if (uiState.settings.breathingOn && !uiState.isPaused) {
                item(key = "breathing_link") {
                    BreathingLink(onOpenBreathing = onOpenBreathing)
                }
            }

            // Unconditional, including while paused, and last so it never competes with
            // logging. Its presence depends on nothing recorded — that is the whole point
            // (`docs/specs/SPEC-safety-card.md`, D-6).
            item(key = "safety_link") {
                SafetyLink(onOpenSafety = onOpenSafety)
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

/**
 * The design's toast (D-10, D-20): an ink pill that hugs its text, centred, rather than a
 * full-bleed surface. The `TrackEvent.ToastDismissed` contract, the timing, and the strings are
 * untouched — this changes one `Surface` into one `Box`.
 */
@Composable
private fun ToastBanner(toast: String?) {
    if (toast == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = MsSpacing.sm),
        horizontalArrangement = Arrangement.Center
    ) {
        MsToastPill(text = toast, modifier = Modifier.testTag("toast_banner"))
    }
}

/**
 * The design's prompt row, lines 73-86: the readout on the left and the 26 dp help toggle at the
 * trailing edge, over the hairline at line 97 that separates the prompt from the pad.
 */
@Composable
private fun ReadoutAndHelpRow(
    readout: ReadoutState?,
    helpOpen: Boolean,
    onEvent: (TrackEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = MsSpacing.xxs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MsSpacing.xxxs)
            ) {
                if (readout != null) TransientReadout(readout)
            }
            HelpToggle(helpOpen = helpOpen, onEvent = onEvent)
        }
        MsHairline(modifier = Modifier.padding(top = MsSpacing.lg, bottom = MsSpacing.lgPlus))
    }
}

/**
 * The design renders this as a 26 sp number beside an 11 sp tracked gold band label — two
 * elements. MindScale's copy composes them into one string, and `SPEC-visual-foundation.md`
 * Invariant 3 requires every visible string to stay byte-identical, so this is **one** `Text`
 * holding one two-span `AnnotatedString` (D-9).
 *
 * Its semantics are set outright rather than added to: [clearAndSetSemantics] restores the
 * original mixed-case string and re-applies the content description the node already had, so
 * neither the uppercased band nor the span split reaches a screen reader.
 */
@Composable
private fun TransientReadout(readout: ReadoutState) {
    val palette = MaterialTheme.ms
    val valueStyle = MaterialTheme.typography.displayLarge
    val bandStyle = MaterialTheme.typography.labelLarge
    val plain = "${readout.value} · ${readout.band}"
    val spoken = "Logged ${readout.value}, ${readout.band}" +
        if (readout.anchor.isBlank()) "" else ", ${readout.anchor}"
    val rendered = buildAnnotatedString {
        withStyle(valueStyle.toSpanStyle().copy(color = palette.inkPrimary)) {
            append(readout.value.toString())
        }
        withStyle(bandStyle.toSpanStyle().copy(color = palette.goldText)) {
            append(" · ")
            append(readout.band.uppercase(Locale.ROOT))
        }
    }
    Text(
        text = rendered,
        style = valueStyle,
        modifier = Modifier.clearAndSetSemantics {
            text = AnnotatedString(plain)
            contentDescription = spoken
        }
    )
    if (readout.anchor.isNotBlank()) {
        Text(
            text = readout.anchor,
            style = MaterialTheme.typography.bodySmall,
            color = palette.inkTertiary,
            modifier = Modifier.testTag("readout_anchor")
        )
    }
}

/**
 * The always-available way into the Safety card. It is never a dialog, never triggered,
 * and never gated on a rating, pattern, or count — MindScale does not assess risk.
 *
 * Unlike almost every other action in this design, line 249 sets **no** `text-transform`: the
 * safety link is a plain sentence, not a tracked label, so it does not go through
 * [MsTextAction]. It is painted at [com.kieslingdev.mindscale.ui.theme.MindScalePalette.inkQuaternary]
 * rather than the design's `rgba(ink,.35)`, which measures 2.2:1.
 */
@Composable
private fun SafetyLink(onOpenSafety: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = MsSpacing.xxl, bottom = MsSpacing.xxs),
        horizontalArrangement = Arrangement.Center
    ) {
        PlainTextLink(
            text = SafetyCopy.TRACK_LINK,
            onClick = onOpenSafety,
            modifier = Modifier
                .testTag("safety_link")
                .semantics { contentDescription = SafetyCopy.TRACK_LINK_DESCRIPTION }
        )
    }
}

/**
 * The only way into the paced-breathing circle. It is never a dialog, never a prompt, and
 * never appears in response to a rating, an episode, a count, or anything else recorded —
 * MindScale does not decide that someone should breathe
 * (`docs/specs/SPEC-paced-breathing.md`, D-7, D-10).
 *
 * The design's own control at line 170 is an outlined gold pill with a tracked uppercase label,
 * which is exactly [MsPillButton].
 */
@Composable
private fun BreathingLink(onOpenBreathing: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = MsSpacing.xl),
        horizontalArrangement = Arrangement.Center
    ) {
        MsPillButton(
            text = BreathingCopy.TRACK_LINK,
            onClick = onOpenBreathing,
            modifier = Modifier
                .testTag("breathing_link")
                .semantics { contentDescription = BreathingCopy.TRACK_LINK_DESCRIPTION }
        )
    }
}

/** A bare clickable sentence at the design's faintest compliant emphasis, raised to a 48 dp target. */
@Composable
private fun PlainTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = MsSpacing.minTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = MsSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.ms.inkQuaternary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PausedBanner(
    onEvent: (TrackEvent) -> Unit,
    onOpenData: () -> Unit,
    modifier: Modifier = Modifier
) {
    MsCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MsSpacing.sm, bottom = MsSpacing.xxs)
            .testTag("paused_banner")
    ) {
        // The design's `gap:14px`, line 61.
        Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.lg)) {
            MsEyebrow(text = "Tracking paused")
            Text(
                text = "Your data is still here and still yours. Nothing is being recorded until you start again.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.ms.inkSecondary
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus)) {
                MsTextAction(
                    text = "Start again",
                    onClick = { onEvent(TrackEvent.ResumeTracking) },
                    modifier = Modifier
                        .testTag("resume_tracking_button")
                        .semantics { contentDescription = "Start tracking again" }
                )
                MsTextAction(
                    text = "Export or delete",
                    onClick = onOpenData,
                    tone = MsActionTone.Muted,
                    modifier = Modifier.testTag("paused_data_button")
                )
            }
        }
    }
}

/** The design's 26 dp circular help toggle, line 84. Painted at 26 dp with a 48 dp target. */
@Composable
private fun HelpToggle(helpOpen: Boolean, onEvent: (TrackEvent) -> Unit) {
    MsCircularHeaderButton(
        label = "?",
        onClick = { onEvent(TrackEvent.ToggleHelp) },
        size = MsSpacing.helpButton,
        textStyle = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .testTag("help_toggle_button")
            .semantics {
                contentDescription =
                    if (helpOpen) "Hide what the numbers mean" else "Show what the numbers mean"
            }
    )
}

@Composable
private fun HelpCard(modifier: Modifier = Modifier) {
    val palette = MaterialTheme.ms
    MsCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = MsSpacing.mdPlus)
            .testTag("help_card"),
        contentPadding = MsSpacing.lgPlus
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)) {
            Text(
                text = "0 means it isn't happening right now. 1–3 you notice it. 4–6 it's changing what " +
                    "you do. 7–10 it's most of what's happening.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkSecondary
            )
            Text(
                text = "The numbers only have to mean the same thing to you each time. That's what makes " +
                    "the chart readable months later.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkSecondary
            )
            Text(
                text = "Log when it starts, when it clearly changes, and when it stops. Nothing recorded " +
                    "means nothing was happening.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkTertiary
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
    Row(
        modifier = modifier.fillMaxWidth().padding(top = MsSpacing.lgPlus),
        horizontalArrangement = Arrangement.Center
    ) {
        MsCard(
            emphasized = true,
            contentPadding = MsSpacing.lgPlus,
            modifier = Modifier
                .widthIn(max = PadMaxWidth)
                .testTag("onset_chip_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)) {
                MsEyebrow(text = "What was happening?")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MsSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)
                ) {
                    vocabulary.forEach { chip ->
                        val selected = chip in prompt.selected
                        MsChip(
                            text = chip,
                            selected = selected,
                            onClick = { onEvent(TrackEvent.OnsetChipToggled(chip)) },
                            modifier = Modifier
                                .testTag("onset_chip_$chip")
                                .semantics {
                                    contentDescription = if (selected) "$chip, selected" else "$chip, not selected"
                                }
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MsSpacing.xxl, Alignment.CenterHorizontally)
                ) {
                    MsTextAction(
                        text = "Submit",
                        onClick = { onEvent(TrackEvent.OnsetChipsSubmitted) },
                        modifier = Modifier.testTag("onset_chips_submit")
                    )
                    MsTextAction(
                        text = "Skip",
                        onClick = { onEvent(TrackEvent.OnsetChipsSkipped) },
                        tone = MsActionTone.Muted,
                        modifier = Modifier.testTag("onset_chips_skip")
                    )
                }
            }
        }
    }
}

/**
 * MindScale's own card — the prototype has no anchor prompt — so its treatment is derived from the
 * design's emphasized-card idiom rather than copied (D-13).
 */
@Composable
private fun AnchorPrompt(onSet: () -> Unit, onDismiss: () -> Unit) {
    MsCard(
        emphasized = true,
        contentPadding = MsSpacing.lgPlus,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MsSpacing.lgPlus)
            .testTag("anchor_prompt")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)) {
            MsEyebrow(text = "Make the numbers yours")
            Text(
                text = "A few personal examples can help your ratings stay consistent over time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.ms.inkSecondary
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)) {
                MsTextAction(
                    text = "Set anchors",
                    onClick = onSet,
                    modifier = Modifier.testTag("set_anchors")
                )
                MsTextAction(
                    text = "Not now",
                    onClick = onDismiss,
                    tone = MsActionTone.Muted,
                    modifier = Modifier.testTag("dismiss_anchors")
                )
            }
        }
    }
}

/** The three visual states `sleepStyle` and `wakeStyle` paint, lines 1719-1720 (D-14). */
private enum class ToggleVisual { Rest, Armed, Filled }

@Composable
private fun SleepWakeRow(uiState: TrackUiState, onEvent: (TrackEvent) -> Unit) {
    val armed = uiState.armedCapture
    val openInterval = uiState.openSleepInterval
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = MsSpacing.xxlPlus),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.widthIn(max = CenteredColumnMaxWidth),
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.lg)
        ) {
            val sleepDescription = when {
                armed == EntryKind.SLEEP -> "Sleep armed. Tap a number to log falling asleep."
                openInterval != null -> "Asleep since ${formatClockTime(openInterval.startTs, uiState.settings.hourFormat)}"
                else -> "Mark falling asleep. Then tap how you felt."
            }
            SleepWakeToggle(
                label = "Sleep",
                visual = when {
                    armed == EntryKind.SLEEP -> ToggleVisual.Armed
                    openInterval != null -> ToggleVisual.Filled
                    else -> ToggleVisual.Rest
                },
                receded = false,
                onClick = { onEvent(TrackEvent.ArmSleep) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("sleep_button")
                    .semantics { contentDescription = sleepDescription }
            )

            val wakeDescription = when {
                armed == EntryKind.WAKE -> "Wake armed. Tap a number to log waking up."
                openInterval == null -> "Wake. Disabled: tap Sleep first, nothing is currently open."
                else -> "Mark waking up. Then tap how you feel."
            }
            SleepWakeToggle(
                label = "Wake",
                visual = if (armed == EntryKind.WAKE) ToggleVisual.Armed else ToggleVisual.Rest,
                // The design dims an unavailable Wake to `rgba(ink,.28)`, which measures 1.9:1 and
                // fails as text. Two compliant emphasis levels preserve its ordering instead. The
                // control stays *enabled* either way, exactly as before: its content description
                // already states the condition, and changing enablement would be behavioral (D-14).
                receded = openInterval == null,
                onClick = { onEvent(TrackEvent.ArmWake) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("wake_button")
                    .semantics { contentDescription = wakeDescription }
            )
        }
    }
}

@Composable
private fun SleepWakeToggle(
    label: String,
    visual: ToggleVisual,
    receded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.ms
    Box(
        modifier = modifier
            .heightIn(min = MsSpacing.minTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // A minimum rather than a fixed height, so a label that wraps at 200% font grows
                // the pill instead of being clipped by it (D-17).
                .heightIn(min = ToggleHeight)
                .clip(MsShapes.pill)
                .background(if (visual == ToggleVisual.Filled) palette.ink else Color.Transparent)
                .border(
                    width = MsSpacing.hairline,
                    color = when (visual) {
                        ToggleVisual.Armed -> palette.gold
                        ToggleVisual.Filled -> palette.ink
                        ToggleVisual.Rest -> palette.outline
                    },
                    shape = MsShapes.pill
                )
                .padding(horizontal = MsSpacing.sm, vertical = MsSpacing.smPlus),
            contentAlignment = Alignment.Center
        ) {
            MsUppercaseText(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = when (visual) {
                    ToggleVisual.Filled -> palette.onInk
                    ToggleVisual.Armed -> palette.goldText
                    ToggleVisual.Rest -> if (receded) palette.inkQuaternary else palette.inkTertiary
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MarkerSection(uiState: TrackUiState, onEvent: (TrackEvent) -> Unit) {
    // rememberSaveable buffers the text field across Compose state restoration. The
    // ViewModel also mirrors markerOpen/markerDraft into SavedStateHandle so the marker
    // UI itself is recreated open with the same draft after true process recreation.
    // Keyed on markerOpen so each reopen starts from the ViewModel's freshly-reset "".
    var draftText by rememberSaveable(uiState.markerOpen) { mutableStateOf(uiState.markerDraft) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = MsSpacing.xl)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            MsTextAction(
                text = if (uiState.markerOpen) "Event" else "Mark an event",
                onClick = { onEvent(TrackEvent.MarkerToggled) },
                tone = MsActionTone.Muted,
                modifier = Modifier
                    .testTag("marker_toggle")
                    .semantics {
                        contentDescription =
                            if (uiState.markerOpen) "Close event marker input" else "Mark an event"
                    }
            )
        }
        if (uiState.markerOpen) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MsSpacing.lg),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = CenteredColumnMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(MsSpacing.md)
                ) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { newText ->
                            draftText = newText
                            onEvent(TrackEvent.MarkerDraftChanged(newText))
                        },
                        label = { Text("Dose change, started therapy, travel…") },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("marker_input")
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            MsSpacing.xlPlus,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        MsTextAction(
                            text = "Save",
                            onClick = { onEvent(TrackEvent.MarkerSaveConfirmed) },
                            modifier = Modifier.testTag("marker_save")
                        )
                        MsTextAction(
                            text = "Cancel",
                            onClick = { onEvent(TrackEvent.MarkerCancelled) },
                            tone = MsActionTone.Muted,
                            modifier = Modifier.testTag("marker_cancel")
                        )
                    }
                }
            }
        }
    }
}

/** The design's hairline-separated block, lines 157-166, rather than a card. */
@Composable
private fun CheckinBanner(onEvent: (TrackEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MsSpacing.xlPlus)
            .testTag("checkin_banner")
    ) {
        MsHairline()
        Column(
            modifier = Modifier.padding(top = MsSpacing.lgPlus),
            verticalArrangement = Arrangement.spacedBy(MsSpacing.md)
        ) {
            MsEyebrow(text = "A question, once in a while")
            Text(
                text = "Is keeping this record still useful to you? For some people, watching symptoms " +
                    "closely makes them louder. If that's happening, stopping is a reasonable thing to do.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.ms.inkSecondary
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MsSpacing.xl)) {
                MsTextAction(
                    text = "Still useful",
                    onClick = { onEvent(TrackEvent.CheckinStillUseful) },
                    modifier = Modifier.testTag("checkin_still_useful")
                )
                MsTextAction(
                    text = "Pause tracking",
                    onClick = { onEvent(TrackEvent.CheckinPauseRequested) },
                    tone = MsActionTone.Muted,
                    modifier = Modifier.testTag("checkin_pause")
                )
            }
        }
    }
}

/**
 * The design's pad, lines 99-105 and 1204/1218 (D-4 through D-7).
 *
 * The armed state is three simultaneous marks, and two of them are presence-versus-absence rather
 * than colour, which is what satisfies D-23's "never colour alone": the wrapper gains a border
 * where it had none, and a 4 dp ring appears outside it. Both rings reserve their space in every
 * state so arming cannot resize the keys.
 *
 * The column width is measured once and every key is laid out at it, which is what makes L-1's
 * "keeping both keys' size" exact rather than approximate. Weighted spacers were rejected: four
 * children at weights 0.5/1/1/0.5 consume three gaps instead of two and would render `0` and `10`
 * narrower than `1` through `9` (D-6).
 */
@Composable
private fun Numpad(
    armed: EntryKind?,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.ms
    val isArmed = armed != null
    Row(
        modifier = modifier.fillMaxWidth().padding(top = MsSpacing.xxxs),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = PadArmedSpread,
                    color = if (isArmed) palette.gold.copy(alpha = 0.09f) else Color.Transparent,
                    shape = RoundedCornerShape(PadOuterRadius)
                )
                .padding(PadArmedSpread)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = PadMaxWidth)
                    .border(
                        width = MsSpacing.hairline,
                        color = if (isArmed) palette.gold else Color.Transparent,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .padding(MsSpacing.lgPlus)
            ) {
                BoxWithConstraints {
                    val columnWidth = (maxWidth - PadKeyGap * 2) / 3
                    // Never below the touch-target floor, even on an implausibly narrow device.
                    // All twelve keys take the same value, so the shared width survives the clamp.
                    val keySize = maxOf(columnWidth, MsSpacing.minTouchTarget)
                    Column(verticalArrangement = Arrangement.spacedBy(PadKeyGap)) {
                        NumpadRows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(PadKeyGap)) {
                                row.forEach { value ->
                                    NumpadKey(
                                        value = value,
                                        size = keySize,
                                        armed = isArmed,
                                        onTap = onTap,
                                        onLongPress = onLongPress
                                    )
                                }
                            }
                        }
                        // L-1: `0` and `10` remain their own group below the 3x3 grid, in that
                        // order, and are now centred on the pad's axis instead of leaving the
                        // prototype's two-thirds-empty final row (D-6, D-7).
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                PadKeyGap,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            NumpadEdgeKeys.forEach { value ->
                                NumpadKey(
                                    value = value,
                                    size = keySize,
                                    armed = isArmed,
                                    onTap = onTap,
                                    onLongPress = onLongPress
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadKey(
    value: Int,
    size: Dp,
    armed: Boolean,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.ms
    Box(
        modifier = modifier
            .size(size)
            .clip(MsShapes.circle)
            .background(palette.card)
            .border(
                width = MsSpacing.hairline,
                // The design's `rgba(ink,.1)` measures 1.24:1 and its armed `rgba(gold,.55)`
                // 1.78:1, against a 3:1 floor for a control's only boundary (D-4).
                color = if (armed) palette.gold else palette.outline,
                shape = MsShapes.circle
            )
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
            style = MaterialTheme.typography.displaySmall,
            color = palette.inkPrimary
        )
    }
}

/** The design's empty state, lines 174-182: a hairline, an eyebrow, and left-aligned prose. */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MsSpacing.xxlPlus, bottom = MsSpacing.xs)
            .testTag("track_empty_state")
    ) {
        MsHairline()
        Column(
            modifier = Modifier.padding(top = MsSpacing.xlPlus),
            verticalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)
        ) {
            MsEyebrow(text = "Nothing recorded yet")
            Text(
                text = "A well day costs nothing and needs no log. Come back whenever " +
                    "there's something worth tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.ms.inkSecondary
            )
        }
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

/**
 * The design's recent row, lines 190-215 (D-8, D-9).
 *
 * The dot is the design's: a 42 dp circle with **no fill**, a faint gold ring, and the numeral in
 * ink. That is why Track no longer calls `intensityColor` — `IntensityRamp.kt` is untouched and
 * keeps its three Insights callers, so the ramp's colour mapping is still the Phase 17 decision
 * D-24 reserved. Invariant 14 is strengthened rather than weakened: the value is still a numeral
 * and is no longer *also* encoded as a fill.
 */
@Composable
private fun EntryRow(
    entry: Entry,
    onEvent: (TrackEvent) -> Unit,
    hideNote: Boolean,
    hourFormat: HourFormat,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.ms
    val bandText = band(entry.value)
    val formatted = remember(entry.ts, hourFormat) {
        java.time.Instant.ofEpochMilli(entry.ts)
            .atZone(ZoneId.systemDefault())
            .format(
                if (hourFormat == HourFormat.TWENTY_FOUR) EntryDateTimeTwentyFourHourFormatter
                else EntryDateTimeTwelveHourFormatter
            )
    }
    val badge = entryKindBadge(entry)

    Column(modifier = modifier.fillMaxWidth()) {
        MsHairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MsSpacing.mdPlus)
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
                    .size(EntryDotSize)
                    .clip(MsShapes.circle)
                    // Decorative: the row is not clickable and its three actions are separate
                    // nodes, so this ring is a mark rather than a control boundary (D-4).
                    .border(MsSpacing.hairline, palette.gold.copy(alpha = 0.5f), MsShapes.circle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.value.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    // The design recedes a zero to `rgba(ink,.4)`, which measures 2.6:1. This is
                    // the faintest level that still clears the text floor (D-8).
                    color = if (entry.value == 0) palette.inkQuaternary else palette.inkPrimary
                )
            }
            Spacer(modifier = Modifier.width(MsSpacing.lg))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
            ) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.inkPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
                ) {
                    MsUppercaseText(
                        text = bandText,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkQuaternary
                    )
                    if (badge != null) {
                        KindBadge(
                            text = badge,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .testTag("entry_badge_${entry.id}")
                        )
                    }
                    if (entry.chips.isNotEmpty()) {
                        Text(
                            text = entry.chips.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkTertiary,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .testTag("entry_chips_${entry.id}")
                        )
                    }
                }
                val note = entry.note
                if (!hideNote && !note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        EntryRowActions(entry = entry, onEvent = onEvent)
    }
}

/**
 * L-2: one baseline, even gaps, a consistent gutter (D-9).
 *
 * The prototype stacks these three vertically in a right-hand column with a ragged edge. They are
 * horizontal here, and on their own full-width line rather than in that column, because three
 * 48 dp targets plus gaps need about 186 dp — which on a 411 dp device leaves 137 dp for the row
 * content and single digits at 200% font on a narrow one. Giving the content the whole width stops
 * it wrapping, so the row is no taller at 100% font and strictly shorter at 200%.
 *
 * `FlowRow` rather than `Row` so the pathological narrow case wraps instead of clipping. Each
 * action keeps its content description on the same node that carries its click action, because
 * `NavigationTest` finds these in the unmerged tree and clicks what it finds (D-16).
 */
@Composable
private fun EntryRowActions(entry: Entry, onEvent: (TrackEvent) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
    ) {
        MsTextAction(
            text = "Edit",
            onClick = { onEvent(TrackEvent.EditRequested(entry)) },
            tone = MsActionTone.Muted,
            modifier = Modifier.semantics {
                contentDescription = "Edit entry with value ${entry.value}"
            }
        )
        MsTextAction(
            text = "Note",
            onClick = { onEvent(TrackEvent.NoteRequested(entry)) },
            tone = MsActionTone.Muted,
            modifier = Modifier.semantics {
                contentDescription = "Edit note for entry with value ${entry.value}"
            }
        )
        MsTextAction(
            text = "Delete",
            onClick = { onEvent(TrackEvent.DeleteRequested(entry)) },
            // The design paints Delete at the same faint ink as its siblings and turns it red only
            // on `:hover`, which Android has no analogue for. A permanently red Delete would
            // emphasize destruction more than the design does (D-9).
            tone = MsActionTone.Muted,
            modifier = Modifier.semantics {
                contentDescription = "Delete entry with value ${entry.value}"
            }
        )
    }
}

/**
 * The design's kind badge, line 202: a 999 dp pill with a faint gold ring and a tracked uppercase
 * gold label. The ring is decorative — the badge is not interactive — so it keeps the design's
 * alpha (D-4).
 */
@Composable
private fun KindBadge(text: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.ms
    Box(
        modifier = modifier
            .clip(MsShapes.pill)
            .border(MsSpacing.hairline, palette.gold.copy(alpha = 0.4f), MsShapes.pill)
            .padding(horizontal = MsSpacing.sm, vertical = MsSpacing.xxxs),
        contentAlignment = Alignment.Center
    ) {
        MsUppercaseText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = palette.goldText
        )
    }
}

private val ClockTimeTwelveHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val ClockTimeTwentyFourHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatClockTime(epochMillis: Long, hourFormat: HourFormat): String =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(
        if (hourFormat == HourFormat.TWENTY_FOUR) ClockTimeTwentyFourHourFormatter
        else ClockTimeTwelveHourFormatter
    )

/**
 * A dialog action label, uppercased in place (D-3).
 *
 * The button stays a Material [TextButton] and only its label is wrapped, so the `Disabled`
 * semantics property stays on the node `TrackScreenTest` asserts it on three times. The colour is
 * left [Color.Unspecified] so the label inherits `TextButton`'s content colour — `goldText` when
 * enabled, faded when not.
 */
@Composable
private fun DialogActionLabel(text: String) {
    MsUppercaseText(text = text, style = MaterialTheme.typography.labelMedium)
}

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
    val palette = MaterialTheme.ms
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    MsDialog(
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
                            style = MaterialTheme.typography.titleLarge,
                            // A minimum rather than a fixed width, so the label stops the +/-
                            // controls jumping as the number's width changes while still growing
                            // at 200% font instead of wrapping inside a fixed slot (D-17).
                            modifier = Modifier
                                .widthIn(min = DialogValueSlotWidth)
                                .padding(horizontal = MsSpacing.sm)
                        )
                        TextButton(
                            onClick = { if (value < 10) onValueChanged(value + 1) },
                            enabled = !isSaving,
                            modifier = Modifier.semantics { contentDescription = "Increase value" }
                        ) { Text("+") }
                    }
                } else {
                    Text(text = "Value: $value", style = MaterialTheme.typography.titleLarge)
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
                    MsEyebrow(
                        text = "What was happening?",
                        modifier = Modifier.padding(top = MsSpacing.sm)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MsSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MsSpacing.xs),
                        modifier = Modifier.padding(top = MsSpacing.xs)
                    ) {
                        vocabulary.forEach { chip ->
                            val selected = chip in chips
                            MsChip(
                                text = chip,
                                selected = selected,
                                onClick = { onChipToggled(chip) },
                                enabled = !isSaving,
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
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.danger,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (statusMessage != null) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.danger,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (onRetryValidation != null) {
                    TextButton(onClick = onRetryValidation, enabled = !isSaving) {
                        DialogActionLabel("Retry")
                    }
                }
                if (isSaving) {
                    Text(
                        "Saving",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkTertiary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = canSave && !isSaving) {
                DialogActionLabel(saveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isSaving) {
                DialogActionLabel("Cancel")
            }
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
    val palette = MaterialTheme.ms
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
    MsDialog(
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
                if (checking) {
                    Text(
                        text = "Checking record",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkTertiary
                    )
                }
                if (status != null) {
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.danger,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (readFailed) {
                    TextButton(
                        onClick = { onEvent(TrackEvent.DialogValidationRetry) },
                        enabled = !modal.isSaving
                    ) { DialogActionLabel("Retry") }
                }
                if (modal.isSaving) {
                    Text(
                        "Saving",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkTertiary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(TrackEvent.NoteSaveConfirmed) },
                enabled = !modal.isSaving && !checking && !readFailed
            ) { DialogActionLabel(if (conflict) "Save my changes" else "Save") }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(TrackEvent.NoteCancelled) },
                enabled = !modal.isSaving
            ) { DialogActionLabel("Cancel") }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(modal: TrackModalState.Delete, onEvent: (TrackEvent) -> Unit) {
    val palette = MaterialTheme.ms
    MsDialog(
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
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.danger,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                if (modal.isSaving) {
                    Text(
                        "Saving",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkTertiary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(TrackEvent.DeleteConfirmed) },
                enabled = !modal.isSaving
            ) { DialogActionLabel("Delete") }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(TrackEvent.DeleteCancelled) },
                enabled = !modal.isSaving
            ) { DialogActionLabel("Cancel") }
        }
    )
}
