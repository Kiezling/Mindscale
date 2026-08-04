package com.kieslingdev.mindscale

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
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
