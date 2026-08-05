package com.kieslingdev.mindscale.breathing

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `docs/specs/SPEC-paced-breathing.md`, D-2, D-12, D-13.
 *
 * The screen is rendered against a real Room database and a real ViewModel, so what these
 * tests exercise is the same path the installed app takes.
 */
@RunWith(AndroidJUnit4::class)
class BreathingScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var database: MindScaleDatabase
    private lateinit var viewModel: BreathingViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        viewModel = BreathingViewModel(database.breathingSessionDao(), SystemBreathingClock())
    }

    @After fun tearDown() = database.close()

    private fun render() {
        composeTestRule.setContent {
            MindScaleTheme { BreathingRoute(viewModel) }
        }
    }

    @Test
    fun theIdleScreenShowsEveryFrozenStringAndPreselectsNothing() {
        render()

        composeTestRule.onNodeWithText(BreathingCopy.CHOOSE).assertIsDisplayed()
        BREATHING_LENGTHS_MINUTES.forEach { minutes ->
            composeTestRule.onNodeWithTag("breathing_length_$minutes").assertIsDisplayed()
        }
        composeTestRule.onNodeWithText(BreathingCopy.CLOSE).assertIsDisplayed()

        composeTestRule.onNodeWithTag("breathing_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("breathing_recording"))
        composeTestRule.onNodeWithText(BreathingCopy.INSTRUCTIONS).assertIsDisplayed()
        composeTestRule.onNodeWithText(BreathingCopy.NO_CLAIM).assertIsDisplayed()
        composeTestRule.onNodeWithText(BreathingCopy.RECORDING).assertIsDisplayed()

        // Nothing is running, so no session exists and no length is marked as chosen.
        composeTestRule.onNodeWithTag("breathing_running_length").assertDoesNotExist()
        assertEquals(0, runBlocking { database.breathingSessionDao().count() })
    }

    @Test
    fun pickingALengthStartsThePacerAndShowsTheChosenLengthNotACountdown() {
        render()
        composeTestRule.onNodeWithTag("breathing_length_3").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(BreathingCopy.CUE_IN).assertIsDisplayed()
        composeTestRule.onNodeWithText("3 minutes").assertIsDisplayed()
        composeTestRule.onNodeWithText(BreathingCopy.STOP).assertIsDisplayed()
        // While running there is nothing to pick, so there is nothing to mis-tap.
        composeTestRule.onNodeWithTag("breathing_length_3").assertDoesNotExist()
    }

    @Test
    fun stoppingRecordsTheSessionAndReturnsToTheDoneState() {
        render()
        composeTestRule.onNodeWithTag("breathing_length_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(BreathingCopy.STOP).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(BreathingCopy.DONE).assertIsDisplayed()
        composeTestRule.onNodeWithText(BreathingCopy.CLOSE).assertIsDisplayed()
        composeTestRule.onNodeWithTag("breathing_length_1").assertIsDisplayed()
        assertEquals(1, runBlocking { database.breathingSessionDao().count() })
    }

    @Test
    fun openingAndClosingWithoutPickingALengthRecordsNothing() {
        render()
        composeTestRule.onNodeWithText(BreathingCopy.CLOSE).performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, runBlocking { database.breathingSessionDao().count() })
    }

    /** The pacing signal for anyone who is not watching the circle. */
    @Test
    fun theCueIsAPoliteLiveRegionWithSpokenBreatheInAndOut() {
        render()
        composeTestRule.onNodeWithTag("breathing_cue").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        )

        composeTestRule.onNodeWithTag("breathing_length_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("breathing_cue").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf(BreathingCopy.CUE_IN_DESCRIPTION)
            )
        )
    }

    /** The animation carries no information a screen reader would otherwise miss. */
    @Test
    fun theCircleItselfIsDecorativeAndCarriesNoSemantics() {
        render()
        composeTestRule.onNodeWithTag("breathing_circle").assertDoesNotExist()
    }

    @Test
    fun everyControlMeetsTheMinimumTouchTarget() {
        render()
        BREATHING_LENGTHS_MINUTES.forEach { minutes ->
            composeTestRule.onNodeWithTag("breathing_length_$minutes")
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
        composeTestRule.onNodeWithTag("breathing_close")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun theLengthButtonsCarryTheirSpokenDescriptions() {
        render()
        BREATHING_LENGTHS_MINUTES.forEach { minutes ->
            composeTestRule.onNodeWithTag("breathing_length_$minutes").assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf(BreathingCopy.lengthDescription(minutes))
                )
            )
        }
    }
}
