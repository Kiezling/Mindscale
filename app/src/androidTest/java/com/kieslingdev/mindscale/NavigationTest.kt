package com.kieslingdev.mindscale

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun trackLaunches_logOpens_andBackReturnsToTrack() {
        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()
        composeTestRule.onNodeWithText("Log").performClick()
        composeTestRule.onNodeWithTag("full_log_screen").assertExists()

        pressBack()

        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()
    }

    @Test
    fun selectedLogDestination_survivesActivityRecreation() {
        composeTestRule.onNodeWithText("Log").performClick()
        composeTestRule.onNodeWithTag("full_log_screen").assertExists()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag("full_log_screen").assertExists()
    }

    @Test
    fun backdateDialogRawDraft_survivesActivityRecreation() {
        composeTestRule.onNodeWithTag("numpad_key_7").performTouchInput { longClick() }
        composeTestRule.onNodeWithTag("track_dialog_date").performTextReplacement("2026-0")
        composeTestRule.onNodeWithTag("track_dialog_time").performTextReplacement("1")

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText("Backdate entry").assertExists()
        composeTestRule.onNodeWithTag("track_dialog_date").assertTextContains("2026-0")
        composeTestRule.onNodeWithTag("track_dialog_time").assertTextContains("1")
        pressBack()
    }

    @Test
    fun editAndNoteDialogRawDrafts_surviveActivityRecreation() {
        composeTestRule.onNodeWithTag("numpad_key_8").performClick()
        val editAction = hasContentDescription("Edit entry with value 8")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(editAction, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodes(
            editAction,
            useUnmergedTree = true
        ).onFirst().performScrollTo().performClick()
        composeTestRule.onNodeWithTag("track_dialog_date").performTextReplacement("2026-0")
        composeTestRule.onNodeWithTag("track_dialog_time").performTextReplacement("1")

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText("Edit entry").assertExists()
        composeTestRule.onNodeWithTag("track_dialog_date").assertTextContains("2026-0")
        composeTestRule.onNodeWithTag("track_dialog_time").assertTextContains("1")
        pressBack()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("track_dialog_date")
                .fetchSemanticsNodes().isEmpty()
        }

        val noteAction = hasContentDescription("Edit note for entry with value 8")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(noteAction, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodes(
            noteAction,
            useUnmergedTree = true
        ).onFirst().performScrollTo().performClick()
        val noteDraft = "  first line\nsecond line  "
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("track_note_text")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("track_note_text").performTextReplacement(noteDraft)

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText("Edit note").assertExists()
        composeTestRule.onNodeWithTag("track_note_text").assertTextContains(noteDraft)
        pressBack()
    }

    @Test
    fun insightsOpens_survivesRecreation_andBackReturnsToTrack() {
        composeTestRule.onNodeWithTag("insights_tab").performClick()
        composeTestRule.onNodeWithTag("insights_screen").assertExists()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithTag("insights_screen").assertExists()

        pressBack()
        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()
    }

    @Test
    fun settingsOpenedFromInsights_returnsToInsights() {
        composeTestRule.onNodeWithTag("insights_tab").performClick()
        composeTestRule.onNodeWithTag("settings_action").performClick()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()

        composeTestRule.onNodeWithTag("settings_back").performClick()

        composeTestRule.onNodeWithTag("insights_screen").assertExists()
    }

    @Test
    fun settingsOpens_hidesBottomNavigation_andBackReturnsToPriorDestination() {
        composeTestRule.onNodeWithText("Log").performClick()
        composeTestRule.onNodeWithTag("settings_action").performClick()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()
        composeTestRule.onNodeWithTag("main_navigation").assertDoesNotExist()

        composeTestRule.onNodeWithTag("settings_back").performClick()

        composeTestRule.onNodeWithTag("full_log_screen").assertExists()
        composeTestRule.onNodeWithTag("main_navigation").assertExists()
    }

    @Test
    fun settingsDestination_survivesActivityRecreation() {
        composeTestRule.onNodeWithTag("settings_action").performClick()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag("settings_screen").assertExists()
    }

    @Test
    fun holdSettingIsReachableAndPersistsAcrossRecreation() {
        composeTestRule.onNodeWithTag("settings_action").performClick()
        composeTestRule.onNodeWithText("An entry ends after").performScrollTo()
        composeTestRule.onNodeWithText("24h").performClick()
        composeTestRule.onNodeWithContentDescription("24h, selected").assertExists()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText("An entry ends after").performScrollTo()
        composeTestRule.onNodeWithContentDescription("24h, selected").assertExists()
    }
}
