package com.kieslingdev.mindscale

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
