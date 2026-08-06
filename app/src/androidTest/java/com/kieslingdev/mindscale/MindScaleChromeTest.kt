package com.kieslingdev.mindscale

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import com.kieslingdev.mindscale.breathing.BreathingCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The chrome contract that `docs/specs/SPEC-visual-foundation.md` D-16, D-17, and D-18 restyle.
 *
 * D-1 forbids behavior change, and the top bar and bottom navigation were both rebuilt from
 * scratch in Phase 15 — `NavigationBar` cannot render a tab without an icon slot, so the bar
 * became a plain `Row`. These assertions pin what had to survive that swap, over and above the
 * 187 pre-existing connected tests that already exercise it.
 */
class MindScaleChromeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ── bottom navigation (D-17) ─────────────────────────────────────────────

    /**
     * The labels render upper case and the semantics restore the original (D-11). Each tab is
     * addressed by its content description, which is unique, rather than by its text: the header
     * title and the tab label are the same word on a root destination, so `"Track"` matches two
     * nodes. That duplication predates this phase — the old header rendered `Text("Track")`
     * beside a `NavigationBarItem` labelled `Track` — and it is asserted explicitly below.
     */
    @Test
    fun everyTabIsFindableByItsOriginalCaseAndIsClickable() {
        listOf("Track tab" to "Track", "Log tab" to "Log", "Insights tab" to "Insights")
            .forEach { (description, label) ->
                composeTestRule.onNodeWithContentDescription(description)
                    .assertExists()
                    .assertHasClickAction()
                    .assertTextContains(label)
            }
    }

    @Test
    fun everyTabKeepsItsContentDescription() {
        listOf("Track tab", "Log tab", "Insights tab").forEach { description ->
            composeTestRule.onNodeWithContentDescription(description).assertExists()
        }
    }

    @Test
    fun theNavigationTagsTheConnectedSuiteDependsOnStillResolve() {
        composeTestRule.onNodeWithTag("main_navigation").assertExists()
        composeTestRule.onNodeWithTag("insights_tab").assertExists()
    }

    /**
     * The design's tab is a bare 15/17 px-padded label with no container. D-23 raises the target
     * to 48 dp without changing the painted type.
     */
    @Test
    fun everyTabReachesTheMinimumTouchTarget() {
        listOf("Track tab", "Log tab", "Insights tab").forEach { description ->
            composeTestRule.onNodeWithContentDescription(description)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun theTabsStillSwitchTheRootDestination() {
        composeTestRule.onNodeWithText("Log").performClick()
        composeTestRule.onNodeWithTag("full_log_screen").assertExists()

        composeTestRule.onNodeWithText("Insights").performClick()
        composeTestRule.onNodeWithTag("insights_screen").assertExists()

        composeTestRule.onNodeWithText("Track").performClick()
        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()
    }

    // ── header (D-16) ────────────────────────────────────────────────────────

    /**
     * D-16 keeps all three header slots because removing any of them would remove on-screen
     * content, which D-1 forbids. The prototype's header carries a single centred wordmark
     * because that app has an initials avatar and no destination title.
     */
    @Test
    fun theHeaderKeepsAllThreeOfItsSlots() {
        composeTestRule.onNodeWithText("MindScale").assertExists()
        composeTestRule.onNodeWithTag("profile_action").assertExists().assertHasClickAction()

        // The destination title and the tab label are the same word, so exactly two nodes carry
        // it. Asserting the count pins the duplication as intended rather than accidental.
        assertEquals(
            2,
            composeTestRule.onAllNodesWithText("Track").fetchSemanticsNodes().size
        )
    }

    @Test
    fun theProfileActionReachesTheMinimumTouchTarget() {
        composeTestRule.onNodeWithTag("profile_action")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun theBackControlOnAnOverlayReachesTheMinimumTouchTarget() {
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_screen").assertExists()

        composeTestRule.onNodeWithTag("overlay_back")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .assertHasClickAction()

        // The glyph changed from the word "Back" to the design's chevron, so the control keeps
        // an explicit content description rather than relying on its label.
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    /**
     * Every destination title still renders, including the two frozen ones. On a root
     * destination the title duplicates the tab label, so these count nodes rather than demanding
     * exactly one.
     */
    @Test
    fun theDestinationTitleStillRendersOnEveryOverlay() {
        composeTestRule.onNodeWithTag("profile_action").performClick()
        composeTestRule.onNodeWithTag("profile_screen").assertExists()
        assertTrue(composeTestRule.onAllNodesWithText("Profile").fetchSemanticsNodes().isNotEmpty())
        composeTestRule.onNodeWithTag("overlay_back").performClick()

        composeTestRule.onNodeWithContentDescription("Insights tab").performClick()
        composeTestRule.onNodeWithTag("insights_screen").assertExists()
        // Now both the header title and the selected tab carry it.
        assertEquals(
            2,
            composeTestRule.onAllNodesWithText("Insights").fetchSemanticsNodes().size
        )
    }

    // ── the full-bleed pacer (D-18) ──────────────────────────────────────────

    private fun openBreathing() {
        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasTestTag("breathing_link"))
        composeTestRule.onNodeWithTag("breathing_link").performClick()
        composeTestRule.onNodeWithTag("breathing_screen").assertExists()
    }

    @Test
    fun thePacerRendersWithNoChromeAtAll() {
        openBreathing()

        composeTestRule.onNodeWithTag("main_navigation").assertDoesNotExist()
        composeTestRule.onNodeWithTag("overlay_back").assertDoesNotExist()
    }

    /**
     * The reason removing the top bar is safe under D-1: the exit affordance never lived in the
     * chrome. The screen's own close pill is the design's `closeBreathe` control.
     */
    @Test
    fun thePacerKeepsItsOwnExitControlAndStillLeavesOnBack() {
        openBreathing()

        composeTestRule.onNodeWithTag("breathing_close").assertExists().assertHasClickAction()

        pressBack()

        composeTestRule.onNodeWithTag("numpad_key_1").assertExists()
    }

    /**
     * `BreathingCopy.TOP_BAR_TITLE` is frozen copy. With no top bar it would stop being rendered
     * anywhere, so it becomes the accessibility pane title instead of dying.
     */
    @Test
    fun thePacerAnnouncesItsFrozenTitleAsThePaneTitle() {
        openBreathing()

        composeTestRule.onNodeWithTag("breathing_screen").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.PaneTitle,
                BreathingCopy.TOP_BAR_TITLE
            )
        )
    }

    /** Every other overlay keeps its chrome; only the pacer is full-bleed. */
    @Test
    fun theOtherOverlaysStillHaveTheirChrome() {
        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasTestTag("safety_link"))
        composeTestRule.onNodeWithTag("safety_link").performClick()

        composeTestRule.onNodeWithTag("safety_screen").assertExists()
        composeTestRule.onNodeWithTag("overlay_back").assertExists()
        composeTestRule.onNodeWithTag("main_navigation").assertDoesNotExist()
    }
}
