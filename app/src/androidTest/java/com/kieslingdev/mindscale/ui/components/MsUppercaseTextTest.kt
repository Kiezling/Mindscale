package com.kieslingdev.mindscale.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The load-bearing contract of the visual overhaul.
 *
 * `docs/specs/SPEC-visual-foundation.md` D-11 makes case a presentation concern so that
 * uppercasing every label does not change the semantics text the connected suite asserts on.
 * Every later phase depends on this holding, so it is proved here before anything is converted.
 */
class MsUppercaseTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun isFoundByItsOriginalCase() {
        composeTestRule.setContent { MsUppercaseText("Track") }

        composeTestRule.onNodeWithText("Track").assertExists()
    }

    @Test
    fun theUppercasedFormIsNotInTheSemanticsTree() {
        composeTestRule.setContent { MsUppercaseText("Track") }

        // If the uppercased string were also present, the node would read its own label twice.
        assertTrue(
            composeTestRule.onAllNodesWithText("TRACK").fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun exactlyOneTextValueSurvivesTheOverride() {
        composeTestRule.setContent { MsUppercaseText("Track") }

        val texts = composeTestRule.onNodeWithText("Track")
            .fetchSemanticsNode()
            .config
            .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }

        assertEquals(listOf("Track"), texts.map { it.text })
    }

    /**
     * The shape `NavigationTest` actually uses: `onNodeWithText("Log").performClick()` against a
     * bottom-navigation tab, where the clickable ancestor merges its descendants.
     */
    @Test
    fun insideAMergingSelectableAncestor_isStillFoundAndClickedByOriginalCase() {
        var selections = 0
        composeTestRule.setContent {
            Column(
                Modifier
                    .selectable(selected = false, onClick = { selections++ })
                    .semantics { contentDescription = "Log tab" }
            ) {
                MsUppercaseText("Log")
            }
        }

        composeTestRule.onNodeWithText("Log").performClick()

        composeTestRule.runOnIdle { assertEquals(1, selections) }
    }

    /**
     * A content description on the merging ancestor must not be displaced by the restored text,
     * because four connected assertions match on content description alone.
     */
    @Test
    fun aContentDescriptionOnTheAncestorSurvivesAlongsideTheRestoredText() {
        composeTestRule.setContent {
            Column(
                Modifier
                    .selectable(selected = true, onClick = {})
                    .semantics { contentDescription = "Insights tab" }
            ) {
                MsUppercaseText("Insights")
            }
        }

        composeTestRule.onNodeWithContentDescription("Insights tab").assertExists()
        composeTestRule.onNodeWithText("Insights").assertExists()
    }

    /**
     * `Locale.ROOT`, not the default locale: under a Turkish default `"Insights".uppercase()`
     * produces `İNSİGHTS`, and a label that changes meaning with the device locale is a defect.
     */
    @Test
    fun theRestoredTextIsUnaffectedByTheDefaultLocale() {
        val original = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            composeTestRule.setContent { MsUppercaseText("Insights") }

            composeTestRule.onNodeWithText("Insights").assertExists()
        } finally {
            java.util.Locale.setDefault(original)
        }
    }
}
