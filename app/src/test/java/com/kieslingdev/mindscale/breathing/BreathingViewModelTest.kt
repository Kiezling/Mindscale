package com.kieslingdev.mindscale.breathing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Session recording and the pacing state machine
 * (`docs/specs/SPEC-paced-breathing.md`, D-3, D-5, D-6; Invariants 5, 6, 7).
 *
 * The prototype recorded a session only when the timer ran out, so a five-minute session
 * stopped at four minutes stored nothing. Several tests below exist specifically to pin
 * the rejection of that behaviour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BreathingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeBreathingSessionDao()
    private val startWall = 1_700_000_000_000L

    private lateinit var clock: FakeBreathingClock

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        clock = FakeBreathingClock(
            monotonic = { dispatcher.scheduler.currentTime },
            wall = startWall
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theScreenStartsIdleAndRecordsNothingUntilALengthIsChosen() = runTest {
        val vm = viewModel()
        assertEquals(BreathingStage.Idle, vm.uiState.value.stage)
        dispatcher.scheduler.advanceTimeBy(60_000L)
        dispatcher.scheduler.runCurrent()
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun openingAndClosingWithoutStartingRecordsNothing() = runTest {
        val vm = viewModel()
        vm.leaveScreen()
        dispatcher.scheduler.runCurrent()
        assertEquals(BreathingStage.Idle, vm.uiState.value.stage)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun theCueAdvancesInThenOutAtTheFrozenBoundaries() = runTest {
        val vm = viewModel()
        vm.start(5)
        dispatcher.scheduler.runCurrent()
        assertEquals(BreathPhase.INHALE, phase(vm))

        dispatcher.scheduler.advanceTimeBy(4_499L)
        dispatcher.scheduler.runCurrent()
        assertEquals(BreathPhase.INHALE, phase(vm))

        dispatcher.scheduler.advanceTimeBy(1L)
        dispatcher.scheduler.runCurrent()
        assertEquals(BreathPhase.EXHALE, phase(vm))

        dispatcher.scheduler.advanceTimeBy(6_500L)
        dispatcher.scheduler.runCurrent()
        assertEquals(BreathPhase.INHALE, phase(vm))
    }

    @Test
    fun aCompletedSessionRecordsExactlyTheChosenDuration() = runTest {
        val vm = viewModel()
        vm.start(1)
        dispatcher.scheduler.advanceTimeBy(60_001L)
        dispatcher.scheduler.runCurrent()

        val session = dao.inserted.single()
        assertEquals(startWall, session.startedAt)
        assertEquals(60_000L, session.endedAt - session.startedAt)
        assertEquals(BreathingStage.Finished, vm.uiState.value.stage)
    }

    /** The prototype stored nothing here. This is the departure that D-3 exists for. */
    @Test
    fun stoppingEarlyRecordsTheRealPartialSession() = runTest {
        val vm = viewModel()
        vm.start(5)
        dispatcher.scheduler.advanceTimeBy(240_000L)
        dispatcher.scheduler.runCurrent()
        vm.stop()
        dispatcher.scheduler.runCurrent()

        val session = dao.inserted.single()
        assertEquals(240_000L, session.endedAt - session.startedAt)
        assertEquals(BreathingStage.Finished, vm.uiState.value.stage)
    }

    @Test
    fun navigatingAwayRecordsThePartialSessionAndReturnsToIdle() = runTest {
        val vm = viewModel()
        vm.start(10)
        dispatcher.scheduler.advanceTimeBy(11_000L)
        dispatcher.scheduler.runCurrent()
        vm.leaveScreen()
        dispatcher.scheduler.runCurrent()

        assertEquals(11_000L, dao.inserted.single().let { it.endedAt - it.startedAt })
        assertEquals(BreathingStage.Idle, vm.uiState.value.stage)
    }

    /** No threshold: MindScale does not decide how much breathing counts. */
    @Test
    fun aSessionStoppedImmediatelyIsStillRecorded() = runTest {
        val vm = viewModel()
        vm.start(3)
        vm.stop()
        dispatcher.scheduler.runCurrent()

        val session = dao.inserted.single()
        assertEquals(0L, session.endedAt - session.startedAt)
    }

    @Test
    fun exactlyOneRowIsWrittenPerStartedSessionNoMatterHowOftenStopIsCalled() = runTest {
        val vm = viewModel()
        vm.start(1)
        dispatcher.scheduler.advanceTimeBy(5_000L)
        vm.stop()
        vm.stop()
        vm.leaveScreen()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, dao.inserted.size)
    }

    @Test
    fun stoppingAfterNaturalCompletionDoesNotWriteASecondRow() = runTest {
        val vm = viewModel()
        vm.start(1)
        dispatcher.scheduler.advanceTimeBy(60_001L)
        dispatcher.scheduler.runCurrent()
        vm.stop()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, dao.inserted.size)
        assertEquals(60_000L, dao.inserted.single().let { it.endedAt - it.startedAt })
    }

    @Test
    fun startingWhileAlreadyRunningIsIgnored() = runTest {
        val vm = viewModel()
        vm.start(10)
        dispatcher.scheduler.advanceTimeBy(1_000L)
        vm.start(1)
        dispatcher.scheduler.runCurrent()

        assertEquals(10, (vm.uiState.value.stage as BreathingStage.Running).minutes)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun anUnofferedLengthIsRefused() = runTest {
        viewModel().start(7)
    }

    /**
     * A wall-clock correction mid-session must not corrupt the interval: the duration comes
     * from a monotonic delta, and only the start instant comes from wall time.
     */
    @Test
    fun aWallClockJumpDoesNotChangeTheRecordedDuration() = runTest {
        val vm = viewModel()
        vm.start(1)
        dispatcher.scheduler.advanceTimeBy(30_000L)
        clock.wall = startWall - 3_600_000L // the device clock jumps an hour backwards
        vm.stop()
        dispatcher.scheduler.runCurrent()

        val session = dao.inserted.single()
        assertEquals(startWall, session.startedAt)
        assertEquals(30_000L, session.endedAt - session.startedAt)
    }

    @Test
    fun anOvershootingMonotonicReadingIsClampedToTheChosenLength() = runTest {
        val vm = viewModel()
        vm.start(1)
        dispatcher.scheduler.advanceTimeBy(10_000L)
        // Simulate the ticker being resumed late: the clock reports well past the total.
        clock.monotonicOverride = dispatcher.scheduler.currentTime + 5_000_000L
        vm.stop()
        dispatcher.scheduler.runCurrent()

        assertEquals(60_000L, dao.inserted.single().let { it.endedAt - it.startedAt })
    }

    @Test
    fun everyOfferedLengthRecordsItsExactDuration() = runTest {
        BREATHING_LENGTHS_MINUTES.forEach { minutes ->
            val localDao = FakeBreathingSessionDao()
            val localClock = FakeBreathingClock({ dispatcher.scheduler.currentTime }, startWall)
            val vm = BreathingViewModel(localDao, localClock)
            vm.start(minutes)
            dispatcher.scheduler.advanceTimeBy(minutes * 60_000L + 1L)
            dispatcher.scheduler.runCurrent()

            assertEquals(
                "length $minutes",
                minutes * 60_000L,
                localDao.inserted.single().let { it.endedAt - it.startedAt }
            )
        }
    }

    @Test
    fun aFailedWriteIsReportedRatherThanSilentlyDropped() = runTest {
        dao.failInsert = true
        val vm = viewModel()
        vm.start(1)
        dispatcher.scheduler.advanceTimeBy(1_000L)
        vm.stop()
        dispatcher.scheduler.runCurrent()

        assertEquals(BreathingCopy.SAVE_FAILED, vm.uiState.value.message)
        assertEquals(BreathingStage.Finished, vm.uiState.value.stage)

        vm.dismissMessage()
        assertEquals(null, vm.uiState.value.message)
    }

    @Test
    fun theTickerStopsWhenTheSessionEnds() = runTest {
        val vm = viewModel()
        vm.start(1)
        dispatcher.scheduler.advanceTimeBy(60_001L)
        dispatcher.scheduler.runCurrent()
        val stageAfterCompletion = vm.uiState.value.stage

        dispatcher.scheduler.advanceTimeBy(120_000L)
        dispatcher.scheduler.runCurrent()

        assertEquals(stageAfterCompletion, vm.uiState.value.stage)
        assertEquals(1, dao.inserted.size)
    }

    private fun viewModel() = BreathingViewModel(dao, clock)

    private fun phase(vm: BreathingViewModel) =
        (vm.uiState.value.stage as BreathingStage.Running).phase
}
