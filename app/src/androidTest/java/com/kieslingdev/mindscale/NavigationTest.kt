package com.kieslingdev.mindscale

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.closeSoftKeyboard
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

    /**
     * The Safety card is reachable from Track by one tap and returns to Track on Back.
     * It is an overlay, not a fourth tab: it is a resource, not a place the instrument
     * expects you to spend time (`docs/specs/SPEC-safety-card.md`, D-6).
     */
    @Test
    fun safetyCardOpensFromTrackAndBackReturnsToTrack() {
        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasTestTag("safety_link"))
        composeTestRule.onNodeWithTag("safety_link").performClick()
        composeTestRule.onNodeWithTag("safety_screen").assertExists()
        composeTestRule.onNodeWithTag("main_navigation").assertDoesNotExist()

        pressBack()

        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()
    }

    /**
     * The pacer is an overlay reached by one tap and left by Back, like Safety, Profile,
     * and the Report (`docs/specs/SPEC-paced-breathing.md`, D-7).
     */
    @Test
    fun pacedBreathingOpensFromTrackAndBackReturnsToTrack() {
        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasTestTag("breathing_link"))
        composeTestRule.onNodeWithTag("breathing_link").performClick()
        composeTestRule.onNodeWithTag("breathing_screen").assertExists()
        composeTestRule.onNodeWithTag("main_navigation").assertDoesNotExist()

        pressBack()

        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()
    }

    /**
     * A rotation must not end a session. This is the reason the session lives in the
     * ViewModel and is not wired to `onDispose` or `ON_STOP`, both of which fire here
     * (`docs/specs/SPEC-paced-breathing.md`, D-6).
     */
    @Test
    fun theBreathingDestination_survivesActivityRecreation() {
        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasTestTag("breathing_link"))
        composeTestRule.onNodeWithTag("breathing_link").performClick()
        composeTestRule.onNodeWithTag("breathing_screen").assertExists()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag("breathing_screen").assertExists()
        pressBack()
        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()
    }

    /**
     * The one path that must never exist: logging a high intensity opens no pacer, shows no
     * prompt, and changes nothing about how the link is presented
     * (`docs/specs/SPEC-paced-breathing.md`, D-10, Invariant 3).
     */
    @Test
    fun loggingAHighIntensityNeverOffersThePacer() {
        composeTestRule.onNodeWithTag("numpad_key_10").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("breathing_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()

        composeTestRule.onNodeWithTag("numpad_key_9").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("breathing_screen").assertDoesNotExist()

        // The link is exactly where it always is, reached only by a deliberate tap.
        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasTestTag("breathing_link"))
        composeTestRule.onNodeWithTag("breathing_link").assertExists()
    }

    @Test
    fun safetyCardOpensFromProfileAndBackReturnsToProfile() {
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_screen").assertExists()
        composeTestRule.onNodeWithTag("profile_screen")
            .performScrollToNode(hasTestTag("profile_open_safety"))
        composeTestRule.onNodeWithTag("profile_open_safety").performClick()
        composeTestRule.onNodeWithTag("safety_screen").assertExists()

        pressBack()

        composeTestRule.onNodeWithTag("profile_screen").assertExists()
    }

    @Test
    fun theSafetyDestination_survivesActivityRecreation() {
        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasTestTag("safety_link"))
        composeTestRule.onNodeWithTag("safety_link").performClick()
        composeTestRule.onNodeWithTag("safety_screen").assertExists()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag("safety_screen").assertExists()
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
        closeSoftKeyboard()
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
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_open_settings").performClick()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()

        composeTestRule.onNodeWithTag("overlay_back").performClick()
        composeTestRule.onNodeWithTag("profile_screen").assertExists()
        composeTestRule.onNodeWithTag("overlay_back").performClick()

        composeTestRule.onNodeWithTag("insights_screen").assertExists()
    }

    @Test
    fun settingsOpens_hidesBottomNavigation_andBackReturnsToPriorDestination() {
        composeTestRule.onNodeWithText("Log").performClick()
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_open_settings").performClick()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()
        composeTestRule.onNodeWithTag("main_navigation").assertDoesNotExist()

        composeTestRule.onNodeWithTag("overlay_back").performClick()
        composeTestRule.onNodeWithTag("profile_screen").assertExists()
        composeTestRule.onNodeWithTag("overlay_back").performClick()

        composeTestRule.onNodeWithTag("full_log_screen").assertExists()
        composeTestRule.onNodeWithTag("main_navigation").assertExists()
    }

    @Test
    fun settingsDestination_survivesActivityRecreation() {
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_open_settings").performClick()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag("settings_screen").assertExists()
    }

    @Test
    fun holdSettingIsReachableAndPersistsAcrossRecreation() {
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_open_settings").performClick()
        composeTestRule.onNodeWithText("An entry ends after").performScrollTo()
        composeTestRule.onNodeWithText("24h").performClick()
        composeTestRule.onNodeWithContentDescription("24h, selected").assertExists()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText("An entry ends after").performScrollTo()
        composeTestRule.onNodeWithContentDescription("24h, selected").assertExists()
    }

    @Test
    fun profileAndReportNestedBackStackSurvivesRecreation() {
        composeTestRule.onNodeWithTag("insights_tab").performClick()
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_open_report").performClick()
        composeTestRule.onNodeWithTag("report_screen").assertExists()
        composeTestRule.onNodeWithTag("main_navigation").assertDoesNotExist()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag("report_screen").assertExists()
        composeTestRule.onNodeWithTag("overlay_back").performClick()
        composeTestRule.onNodeWithTag("profile_screen").assertExists()
        composeTestRule.onNodeWithTag("overlay_back").performClick()
        composeTestRule.onNodeWithTag("insights_screen").assertExists()
    }

    @Test
    fun insightsClinicianSummaryReturnsDirectlyToInsights() {
        composeTestRule.onNodeWithTag("insights_tab").performClick()
        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("insights_open_report"))
        composeTestRule.onNodeWithTag("insights_open_report").performClick()
        composeTestRule.onNodeWithTag("report_screen").assertExists()

        composeTestRule.onNodeWithTag("overlay_back").performClick()

        composeTestRule.onNodeWithTag("insights_screen").assertExists()
    }
}
