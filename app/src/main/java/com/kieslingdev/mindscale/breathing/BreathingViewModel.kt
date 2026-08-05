package com.kieslingdev.mindscale.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.BreathingSession
import com.kieslingdev.mindscale.data.BreathingSessionDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the screen is doing. There is no fourth state and no hold (D-1). */
sealed interface BreathingStage {
    data object Idle : BreathingStage

    data class Running(
        val minutes: Int,
        val phase: BreathPhase,
        val phaseMillis: Long
    ) : BreathingStage

    data object Finished : BreathingStage
}

data class BreathingUiState(
    val stage: BreathingStage = BreathingStage.Idle,
    val message: String? = null
)

/**
 * State for the paced-breathing circle (`docs/specs/SPEC-paced-breathing.md`, D-3, D-5,
 * D-6, D-10; Invariants 2, 5, 7).
 *
 * It takes [BreathingSessionDao] and [BreathingClock] and nothing else. There is
 * deliberately no `EntryDao`, `SleepDao`, `MarkerDao`, `EpisodeSourceDao`,
 * `TrackSettingsDao`, `ProfileDao`, `DataControlDao`, or episode engine here, so no later
 * edit can make this screen react to a rating, an episode, a streak, or a count without
 * changing this constructor and failing review. That is not a style preference: Schmidt
 * (2000) found breathing retraining trending toward poorer outcomes in panic and Barlow
 * discourages respiratory control as a safety behaviour, so a pacer that appeared after a
 * high log would be both an inference MindScale does not make and a plausible harm.
 *
 * [BreathingSessionDao] has no observing query either, so the pacer cannot even react to
 * its own history -- there is no streak, count, or "last session" for it to read.
 *
 * The session deliberately does not follow the composition or the Activity lifecycle: both
 * `onDispose` and `ON_STOP` fire on a rotation and would end and record a session because
 * the user turned the phone. It ends on a real end only -- the chosen duration elapsing,
 * [stop], or [leaveScreen] -- and because the phase is a pure function of elapsed monotonic
 * time, a rotation simply re-derives it.
 */
class BreathingViewModel(
    private val sessionDao: BreathingSessionDao,
    private val clock: BreathingClock
) : ViewModel() {

    /** The in-flight session. Non-null exactly while the pacer is running. */
    private class RunningSession(
        val minutes: Int,
        val startedAtWall: Long,
        val startedAtMonotonic: Long
    ) {
        val totalMillis: Long = minutes * 60_000L
    }

    private val _uiState = MutableStateFlow(BreathingUiState())
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow()

    private var session: RunningSession? = null
    private var ticker: Job? = null

    /**
     * Begins a session of [minutes]. Nothing starts until the user picks a length, and
     * nothing is preselected, so there is no default dose (D-2).
     */
    fun start(minutes: Int) {
        if (session != null) return
        require(minutes in BREATHING_LENGTHS_MINUTES) { "Unsupported breathing length" }

        val started = RunningSession(
            minutes = minutes,
            startedAtWall = clock.wallMillis(),
            startedAtMonotonic = clock.monotonicMillis()
        )
        session = started
        publish(started, elapsedMillis = 0L)

        ticker = viewModelScope.launch {
            while (true) {
                val elapsed = elapsedOf(started)
                if (elapsed >= started.totalMillis) break
                publish(started, elapsed)
                val frame = BreathingPacer.frameAt(elapsed)
                // Sleep to whichever comes first: the next phase boundary or the end of the
                // session. Sleeping rather than polling keeps this to one recomposition per
                // phase instead of one per frame.
                delay(minOf(frame.phaseRemainingMillis, started.totalMillis - elapsed))
            }
            finish(BreathingStage.Finished)
        }
    }

    /** The user tapped Stop. The partial session is real and is recorded (D-3). */
    fun stop() = finish(BreathingStage.Finished)

    /**
     * The user navigated away from the breathing screen. A session that was running is
     * recorded for what it actually was, and the screen returns to its idle state so
     * reopening it does not resume something the user left.
     */
    fun leaveScreen() = finish(BreathingStage.Idle)

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private fun publish(started: RunningSession, elapsedMillis: Long) {
        val frame = BreathingPacer.frameAt(elapsedMillis)
        _uiState.update {
            it.copy(
                stage = BreathingStage.Running(
                    minutes = started.minutes,
                    phase = frame.phase,
                    phaseMillis = frame.phaseTotalMillis
                )
            )
        }
    }

    /**
     * Ends whatever is running and writes exactly one row for it. Every caller runs on the
     * main thread, so taking and clearing [session] here is what makes "exactly once per
     * started session" hold (Invariant 5): a second [stop], or a [stop] racing the ticker's
     * own completion, finds nothing left to record.
     *
     * When no session is running this only moves the screen to [nextStage] and writes
     * nothing -- opening the card and closing it again records nothing at all (Invariant 7).
     */
    private fun finish(nextStage: BreathingStage) {
        val started = session
        session = null
        ticker?.cancel()
        ticker = null

        if (started == null) {
            _uiState.update { it.copy(stage = nextStage) }
            return
        }

        // Clamped, so a scheduling overshoot cannot produce a session longer than the
        // length the user chose, and a monotonic clock cannot run backwards (D-5).
        val elapsed = elapsedOf(started).coerceIn(0L, started.totalMillis)
        val record = BreathingSession(
            startedAt = started.startedAtWall,
            endedAt = started.startedAtWall + elapsed
        )

        _uiState.update { it.copy(stage = nextStage) }

        viewModelScope.launch {
            try {
                sessionDao.insert(record)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                // The user is told rather than left with a silently missing row.
                _uiState.update { it.copy(message = BreathingCopy.SAVE_FAILED) }
            }
        }
    }

    private fun elapsedOf(started: RunningSession): Long =
        clock.monotonicMillis() - started.startedAtMonotonic
}
