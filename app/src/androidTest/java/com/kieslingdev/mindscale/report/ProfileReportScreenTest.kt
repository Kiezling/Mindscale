package com.kieslingdev.mindscale.report

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.kieslingdev.mindscale.MainActivity
import com.kieslingdev.mindscale.MindScaleApplication
import com.kieslingdev.mindscale.data.MindScaleDatabase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileReportScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()
    private lateinit var database: MindScaleDatabase

    @Before
    fun resetLocalData() {
        runBlocking {
            database = (composeTestRule.activity.application as MindScaleApplication).container.database
            database.dataControlDao().eraseEverythingAndResetSettings()
        }
    }

    @Test
    fun externalTotalFormRejectsImpossibleValue_thenAddsAndDeletesValidatedTotal() {
        openProfile()
        composeTestRule.onNodeWithText("Totals from elsewhere").assertExists()
        composeTestRule.onNodeWithText("MindScale does not show the questions", substring = true).assertExists()
        scrollTo("score_form")
        composeTestRule.onNodeWithTag("score_total").performTextReplacement("25")
        closeSoftKeyboard()
        composeTestRule.onNodeWithTag("score_save").assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithText("PHQ-8 total must be 0–24.").assertExists()
        assertTrue(
            composeTestRule.onNodeWithTag("score_total").fetchSemanticsNode()
                .config.contains(SemanticsProperties.Error)
        )
        assertFalse(
            composeTestRule.onNodeWithTag("score_date").fetchSemanticsNode()
                .config.contains(SemanticsProperties.Error)
        )

        composeTestRule.onNodeWithTag("score_total").performTextReplacement("24")
        composeTestRule.onNodeWithTag("score_date")
            .performTextReplacement(LocalDate.now(ZoneId.systemDefault()).toString())
        closeSoftKeyboard()
        composeTestRule.onNodeWithTag("score_save").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { database.profileDao().observeScores().first().isNotEmpty() }
        }
        val scoreId = runBlocking { database.profileDao().observeScores().first().single().id }
        scrollTo("score_row_$scoreId")
        composeTestRule.onNodeWithText("PHQ-8 total 24").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entered by you from a result obtained elsewhere", substring = true).assertExists()
        composeTestRule.onNodeWithTag("score_delete_$scoreId").assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithText("Delete PHQ-8 total 24", substring = true).assertExists()
        composeTestRule.onNodeWithTag("score_delete_confirm").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { database.profileDao().observeScores().first().isEmpty() }
        }
    }

    @Test
    fun profileAndExternalScoreDraftsSurviveActivityRecreation() {
        openProfile()
        composeTestRule.onNodeWithTag("profile_name").performTextReplacement("Ada draft")
        scrollTo("score_form")
        composeTestRule.onNodeWithTag("score_instrument_GAD_7").performClick().assertIsSelected()
        composeTestRule.onNodeWithTag("score_date").performTextReplacement("2026-08-03")
        composeTestRule.onNodeWithTag("score_total").performTextReplacement("11")
        closeSoftKeyboard()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithTag("profile_screen").assertExists()
        // Profile restores its scroll position, so the name field is not composed until
        // we scroll back to it. Do not assume it is on screen after recreation.
        scrollTo("profile_name")
        composeTestRule.onNodeWithTag("profile_name").assertTextContains("Ada draft")
        scrollTo("score_form")
        composeTestRule.onNodeWithTag("score_instrument_GAD_7").assertIsSelected()
        composeTestRule.onNodeWithTag("score_date").assertTextContains("2026-08-03")
        composeTestRule.onNodeWithTag("score_total").assertTextContains("11")
    }

    @Test
    fun clinicianSummaryShowsPrivacyCopyExactDisclaimerRangeAndAccessibleActions() {
        openProfile()
        composeTestRule.onNodeWithTag("profile_open_report").performClick()
        composeTestRule.onNodeWithTag("report_screen").assertExists()
        composeTestRule.onNodeWithText("Nothing leaves MindScale until you choose Copy, Share, or Save.", substring = true)
            .assertExists()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("report_text")).fetchSemanticsNodes().isNotEmpty()
        }
        val reportText = composeTestRule.onNodeWithTag("report_text").fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString(separator = "") { it.text }
        assertTrue(reportText.contains("does not diagnose"))
        assertTrue(reportText.contains("MindScale did not administer, calculate, or interpret PHQ-8 or GAD-7 totals."))
        composeTestRule.onNodeWithTag("report_range_THIRTY_DAYS").assertIsSelected()
        composeTestRule.onNodeWithTag("report_range_NINETY_DAYS").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("report_range_NINETY_DAYS").assertIsSelected()
                true
            }.getOrDefault(false)
        }
        scrollToReport("report_actions")
        composeTestRule.onNodeWithTag("report_copy").assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithTag("report_share").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("report_save").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Clinician summary copied. It is now outside MindScale.").assertExists()
    }

    private fun openProfile() {
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_screen").assertExists()
    }

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag("profile_screen").performScrollToNode(hasTestTag(tag))
    }

    private fun scrollToReport(tag: String) {
        composeTestRule.onNodeWithTag("report_screen").performScrollToNode(hasTestTag(tag))
    }
}
