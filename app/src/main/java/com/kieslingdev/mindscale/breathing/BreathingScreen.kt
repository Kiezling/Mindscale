package com.kieslingdev.mindscale.breathing

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Every actionable element on this screen, per D-13. */
private val MinTarget = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)

private val CircleSize = 224.dp

@Composable
fun BreathingRoute(viewModel: BreathingViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BreathingScreen(
        uiState = uiState,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        modifier = modifier
    )
}

/**
 * The paced-breathing circle (`docs/specs/SPEC-paced-breathing.md`).
 *
 * Every string comes from [BreathingCopy] and every timing from [BreathingPacer], so the
 * frozen copy and the frozen cadence are asserted once by test rather than retyped here.
 * Nothing on this screen is conditional on anything the user recorded, and nothing about
 * opening it is written anywhere.
 *
 * The screen is deliberately not the owner of the session clock: it renders whatever phase
 * the ViewModel publishes, so a rotation re-renders rather than restarting or ending
 * anything (D-6).
 */
@Composable
fun BreathingScreen(
    uiState: BreathingUiState,
    onStart: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val running = uiState.stage as? BreathingStage.Running
    val view = LocalView.current
    val context = LocalContext.current

    // The device's reduced-motion preference. Read once per composition rather than
    // observed: a user who changes it mid-session is not a case worth a ContentObserver.
    val animatorScale = remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        )
    }

    // A ten-minute session must not be ended by a thirty-second display timeout. Needs no
    // permission, and is released as soon as the pacer is not running.
    DisposableEffect(view, running != null) {
        view.keepScreenOn = running != null
        onDispose { view.keepScreenOn = false }
    }

    val targetScale = running
        ?.let { BreathingPacer.scaleFor(it.phase) }
        ?: BreathingPacer.REST_SCALE
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = BreathingPacer.animationMillisFor(
                phaseMillis = running?.phaseMillis ?: 0L,
                animatorScale = animatorScale
            )
        ),
        label = "breathing_circle"
    )

    val cue = when (val stage = uiState.stage) {
        BreathingStage.Idle -> BreathingCopy.CHOOSE
        BreathingStage.Finished -> BreathingCopy.DONE
        is BreathingStage.Running -> BreathingCopy.cueFor(stage.phase)
    }
    val cueDescription = when (val stage = uiState.stage) {
        BreathingStage.Idle -> BreathingCopy.CHOOSE
        BreathingStage.Finished -> BreathingCopy.DONE
        is BreathingStage.Running -> BreathingCopy.cueDescriptionFor(stage.phase)
    }

    LazyColumn(
        modifier = modifier.testTag("breathing_screen"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "circle") {
            Box(
                modifier = Modifier.size(CircleSize),
                contentAlignment = Alignment.Center
            ) {
                // Decorative. Everything a TalkBack user needs is on the cue text below,
                // so the animation carries no information of its own (D-13).
                Box(
                    modifier = Modifier
                        .size(CircleSize)
                        .clearAndSetSemantics { }
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(CircleSize)
                        .scale(scale)
                        .clearAndSetSemantics { }
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        .testTag("breathing_circle")
                )
            }
        }

        item(key = "cue") {
            // The pacing signal for anyone not watching the circle. About eleven polite
            // announcements a minute is not chatter here - it is the pace.
            Text(
                text = cue,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("breathing_cue")
                    .semantics {
                        contentDescription = cueDescription
                        liveRegion = LiveRegionMode.Polite
                    }
            )
        }

        if (running != null) {
            item(key = "length") {
                // Static, never a countdown: a running clock would make this a compliance
                // monitor rather than a pace.
                Text(
                    text = BreathingCopy.runningLength(running.minutes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("breathing_running_length")
                )
            }
        } else {
            item(key = "lengths") {
                // Four choices, ascending, none selected. There is no default dose.
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BREATHING_LENGTHS_MINUTES.chunked(2).forEach { row ->
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { minutes ->
                                OutlinedButton(
                                    onClick = { onStart(minutes) },
                                    modifier = MinTarget
                                        .testTag("breathing_length_$minutes")
                                        .semantics {
                                            contentDescription =
                                                BreathingCopy.lengthDescription(minutes)
                                        }
                                ) { Text(BreathingCopy.lengthLabel(minutes)) }
                            }
                        }
                    }
                }
            }
        }

        item(key = "close") {
            OutlinedButton(
                onClick = onStop,
                modifier = MinTarget.testTag("breathing_close")
            ) {
                Text(if (running != null) BreathingCopy.STOP else BreathingCopy.CLOSE)
            }
        }

        item(key = "instructions") {
            Text(
                text = BreathingCopy.INSTRUCTIONS,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("breathing_instructions")
            )
        }

        item(key = "no_claim") {
            Text(
                text = BreathingCopy.NO_CLAIM,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("breathing_no_claim")
            )
        }

        item(key = "recording") {
            Text(
                text = BreathingCopy.RECORDING,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("breathing_recording")
            )
        }

        uiState.message?.let { message ->
            item(key = "message") {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breathing_message")
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}
