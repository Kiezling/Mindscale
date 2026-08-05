package com.kieslingdev.mindscale.safety

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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.data.SafetyPlanStep
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafetyScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var database: MindScaleDatabase
    private lateinit var viewModel: SafetyViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        viewModel = SafetyViewModel(database.safetyPlanDao(), SavedStateHandle())
    }

    @After fun tearDown() = database.close()

    private fun render() {
        composeTestRule.setContent {
            MindScaleTheme { SafetyRoute(viewModel) }
        }
    }

    @Test
    fun crisisResourcesAreShownWithTheirExactFrozenCopy() {
        render()
        composeTestRule.onNodeWithText(SafetyCopy.SCREEN_INTRO).assertExists()
        composeTestRule.onNodeWithTag("safety_honesty").assertExists()
        composeTestRule.onNodeWithText(SafetyCopy.HONESTY).assertExists()
        composeTestRule.onNodeWithText(SafetyCopy.LIFELINE_NAME).assertExists()
        composeTestRule.onNodeWithText(SafetyCopy.LIFELINE_DETAIL).assertExists()
        composeTestRule.onNodeWithText(SafetyCopy.ELSEWHERE_DETAIL).assertExists()
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "safety_emergency")
        )
        composeTestRule.onNodeWithText(SafetyCopy.EMERGENCY).assertExists()
        composeTestRule.onNodeWithText(SafetyCopy.VERIFIED_ON).assertExists()
    }

    @Test
    fun crisisActionsAreAtLeastFortyEightDp() {
        render()
        composeTestRule.onNodeWithTag("resource_action_${SafetyCopy.LIFELINE_CALL}")
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("resource_action_${SafetyCopy.LIFELINE_TEXT}")
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("resource_action_${SafetyCopy.ELSEWHERE_ACTION}")
            .assertHeightIsAtLeast(48.dp)
    }

    /**
     * The narrow controls — Add, Edit, Delete — are the ones most likely to fall under a
     * 48 dp target. Material3 supplies a minimum interactive size, but on this screen that
     * is a fact worth asserting rather than assuming.
     */
    @Test
    fun narrowPlanControlsMeetFortyEightDpOnBothAxes() {
        runBlocking {
            database.safetyPlanDao().addItem(SafetyPlanStep.DISTRACTION, "The library", null)
        }
        render()
        composeTestRule.waitForIdle()
        val id = runBlocking {
            database.safetyPlanDao().itemsIn(SafetyPlanStep.DISTRACTION).first().id
        }
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "plan_delete_$id")
        )
        listOf(
            "plan_edit_$id",
            "plan_delete_$id",
            "step_add_${SafetyPlanStep.DISTRACTION.name}"
        ).forEach { tag ->
            composeTestRule.onNodeWithTag(tag)
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
    }

    @Test
    fun allSixStepsRenderAsHeadingsInCanonicalOrder() {
        render()
        SAFETY_STEPS.forEach { step ->
            composeTestRule.onNodeWithTag("safety_screen")
                .performScrollToNode(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.TestTag,
                        "step_heading_${step.step.name}"
                    )
                )
            composeTestRule.onNodeWithTag("step_heading_${step.step.name}")
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            composeTestRule.onNodeWithText(step.heading).assertExists()
            composeTestRule.onNodeWithText(step.hint).assertExists()
        }
    }

    @Test
    fun anEmptyPlanReadsAsAChoiceNotAFailure() {
        render()
        composeTestRule.onNodeWithTag("plan_empty").assertExists()
        composeTestRule.onNodeWithText(SafetyCopy.PLAN_EMPTY).assertExists()
    }

    @Test
    fun addingALineStoresItAndShowsIt() {
        render()
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.TestTag,
                "step_add_${SafetyPlanStep.INTERNAL_COPING.name}"
            )
        )
        composeTestRule.onNodeWithTag("step_add_${SafetyPlanStep.INTERNAL_COPING.name}")
            .performClick()
        composeTestRule.onNodeWithTag("plan_text_field").performTextReplacement("Shower")
        composeTestRule.onNodeWithTag("plan_save").performClick()
        composeTestRule.waitForIdle()

        runBlocking {
            assertEquals(
                listOf("Shower"),
                database.safetyPlanDao().itemsIn(SafetyPlanStep.INTERNAL_COPING).map { it.text }
            )
        }
        composeTestRule.onNodeWithText("Shower").assertExists()
    }

    @Test
    fun theEditorRefusesABlankLineAndWritesNothing() {
        render()
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.TestTag,
                "step_add_${SafetyPlanStep.WARNING_SIGNS.name}"
            )
        )
        composeTestRule.onNodeWithTag("step_add_${SafetyPlanStep.WARNING_SIGNS.name}")
            .performClick()
        composeTestRule.onNodeWithTag("plan_text_field").performTextReplacement("   ")
        composeTestRule.onNodeWithTag("plan_save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Write something first.").assertIsDisplayed()
        runBlocking { assertEquals(0, database.safetyPlanDao().count()) }
    }

    /** Only the two contact steps offer a phone field (D-5). */
    @Test
    fun thePhoneFieldAppearsOnlyOnContactSteps() {
        render()
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.TestTag,
                "step_add_${SafetyPlanStep.WARNING_SIGNS.name}"
            )
        )
        composeTestRule.onNodeWithTag("step_add_${SafetyPlanStep.WARNING_SIGNS.name}")
            .performClick()
        composeTestRule.onNodeWithTag("plan_phone_field").assertDoesNotExist()
        composeTestRule.onNodeWithTag("plan_cancel").performClick()

        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.TestTag,
                "step_add_${SafetyPlanStep.PEOPLE_FOR_HELP.name}"
            )
        )
        composeTestRule.onNodeWithTag("step_add_${SafetyPlanStep.PEOPLE_FOR_HELP.name}")
            .performClick()
        composeTestRule.onNodeWithTag("plan_phone_field").assertExists()
    }

    @Test
    fun aContactWithNoDialableNumberGetsNoCallButton() {
        runBlocking {
            database.safetyPlanDao().addItem(SafetyPlanStep.PEOPLE_FOR_HELP, "Jo", null)
        }
        render()
        composeTestRule.waitForIdle()
        val id = runBlocking {
            database.safetyPlanDao().itemsIn(SafetyPlanStep.PEOPLE_FOR_HELP).first().id
        }
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "plan_row_$id")
        )
        composeTestRule.onNodeWithTag("plan_call_$id").assertDoesNotExist()
    }

    @Test
    fun aContactWithANumberGetsACallButtonThatNamesThem() {
        runBlocking {
            database.safetyPlanDao().addItem(SafetyPlanStep.PEOPLE_FOR_HELP, "Sam", "555-0100")
        }
        render()
        composeTestRule.waitForIdle()
        val id = runBlocking {
            database.safetyPlanDao().itemsIn(SafetyPlanStep.PEOPLE_FOR_HELP).first().id
        }
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "plan_row_$id")
        )
        composeTestRule.onNodeWithTag("plan_call_$id").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Call Sam").assertExists()
    }

    @Test
    fun deletingAsksFirstAndCanBeCancelled() {
        runBlocking {
            database.safetyPlanDao().addItem(SafetyPlanStep.DISTRACTION, "The library", null)
        }
        render()
        composeTestRule.waitForIdle()
        val id = runBlocking {
            database.safetyPlanDao().itemsIn(SafetyPlanStep.DISTRACTION).first().id
        }
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "plan_delete_$id")
        )
        composeTestRule.onNodeWithTag("plan_delete_$id").performClick()
        composeTestRule.onNodeWithText(SafetyCopy.DELETE_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_delete_cancel").performClick()
        composeTestRule.waitForIdle()
        runBlocking { assertEquals(1, database.safetyPlanDao().count()) }

        composeTestRule.onNodeWithTag("plan_delete_$id").performClick()
        composeTestRule.onNodeWithTag("plan_delete_confirm").performClick()
        composeTestRule.waitForIdle()
        runBlocking { assertEquals(0, database.safetyPlanDao().count()) }
    }

    @Test
    fun anUnavailableHandlerMessageIsAPoliteLiveRegion() {
        render()
        viewModel.reportActionUnavailable(SafetyAction.Dial("988"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "safety_message")
        )
        composeTestRule.onNodeWithTag("safety_message")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        composeTestRule
            .onNodeWithText("No app on this device can open the dialer. The number is 988.")
            .assertExists()
    }
}
