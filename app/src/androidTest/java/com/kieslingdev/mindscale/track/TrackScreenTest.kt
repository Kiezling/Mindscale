package com.kieslingdev.mindscale.track

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val EXPECTED_KEY_ORDER = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10)

class TrackScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        uiState: TrackUiState = TrackUiState(),
        onEvent: (TrackEvent) -> Unit = {}
    ) {
        composeTestRule.setContent {
            MindScaleTheme {
                TrackScreen(uiState = uiState, onEvent = onEvent)
            }
        }
    }

    @Test
    fun numpad_renders12KeysInFrozenOrderAndGrouping() {
        setContent()

        EXPECTED_KEY_ORDER.forEach { value ->
            composeTestRule.onNodeWithTag("numpad_key_$value").assertExists()
        }

        val bounds = EXPECTED_KEY_ORDER.map { value ->
            value to composeTestRule.onNodeWithTag("numpad_key_$value").fetchSemanticsNode().boundsInRoot
        }
        // The 3x3 grid (1..9) must all sit above the 0/10 pill row.
        val gridMaxBottom = bounds.filter { it.first in 1..9 }.maxOf { it.second.bottom }
        val edgeMinTop = bounds.filter { it.first == 0 || it.first == 10 }.minOf { it.second.top }
        assertTrue(
            "0/10 group must render below the 1-9 grid",
            edgeMinTop >= gridMaxBottom
        )
        // 0 must render to the left of 10 (frozen order within the edge group).
        val zeroLeft = bounds.first { it.first == 0 }.second.left
        val tenLeft = bounds.first { it.first == 10 }.second.left
        assertTrue("0 must render before 10", zeroLeft < tenLeft)
    }

    @Test
    fun tappingKey_invokesKeyTappedExactlyOnce() {
        val events = mutableListOf<TrackEvent>()
        setContent(onEvent = { events += it })

        composeTestRule.onNodeWithTag("numpad_key_7").performClick()

        assertEquals(1, events.count { it == TrackEvent.KeyTapped(7) })
        assertEquals(0, events.count { it == TrackEvent.KeyLongPressed(7) })
    }

    @Test
    fun longPressingKey_invokesKeyLongPressedAndNeverKeyTapped() {
        val events = mutableListOf<TrackEvent>()
        setContent(onEvent = { events += it })

        composeTestRule.onNodeWithTag("numpad_key_5").performTouchInput { longClick() }

        assertEquals(1, events.count { it == TrackEvent.KeyLongPressed(5) })
        assertEquals(0, events.count { it == TrackEvent.KeyTapped(5) })
    }

    @Test
    fun emptyState_rendersNoSampleDataAffordance_whenIsEmptyTrue() {
        setContent(uiState = TrackUiState(recentEntries = emptyList(), isEmpty = true))

        composeTestRule.onNodeWithTag("track_empty_state").assertExists()
        composeTestRule.onNodeWithText("Nothing recorded yet").assertExists()
    }
}
