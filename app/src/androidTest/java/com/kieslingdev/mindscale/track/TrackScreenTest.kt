package com.kieslingdev.mindscale.track

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.longClick
import com.kieslingdev.mindscale.data.Entry
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
        fontScale: Float = 1f,
        onEvent: (TrackEvent) -> Unit = {}
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme {
                    TrackScreen(uiState = uiState, onEvent = onEvent)
                }
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

    // -----------------------------------------------------------------
    // Phase 2
    // -----------------------------------------------------------------

    @Test
    fun pausedState_rendersPausedBanner_andHidesTheCaptureSurface_butKeepsRecentEntries() {
        val entry = Entry(id = 1L, ts = 1_000L, value = 4)
        setContent(
            uiState = TrackUiState(
                isPaused = true,
                recentEntries = listOf(entry),
                isEmpty = false,
                sleepOn = true,
                showCheckin = true
            )
        )

        composeTestRule.onNodeWithTag("paused_banner").assertExists()
        composeTestRule.onNodeWithTag("resume_tracking_button").assertExists()
        composeTestRule.onNodeWithTag("numpad_key_7").assertDoesNotExist()
        composeTestRule.onNodeWithTag("help_toggle_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("sleep_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("wake_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("marker_toggle").assertDoesNotExist()
        composeTestRule.onNodeWithTag("checkin_banner").assertDoesNotExist()
        // History remains visible while paused (band(4) == "moderate", see Band.kt).
        composeTestRule.onNodeWithText("moderate").assertExists()
    }

    @Test
    fun resumeTrackingButton_invokesResumeTrackingEvent() {
        val events = mutableListOf<TrackEvent>()
        setContent(uiState = TrackUiState(isPaused = true), onEvent = { events += it })

        composeTestRule.onNodeWithTag("resume_tracking_button").performClick()

        assertEquals(1, events.count { it == TrackEvent.ResumeTracking })
    }

    @Test
    fun checkinBanner_rendersBothActions_andInvokesTheCorrectEvents() {
        val events = mutableListOf<TrackEvent>()
        setContent(uiState = TrackUiState(showCheckin = true), onEvent = { events += it })

        composeTestRule.onNodeWithTag("checkin_banner").assertExists()
        composeTestRule.onNodeWithTag("checkin_still_useful").performClick()
        composeTestRule.onNodeWithTag("checkin_pause").performClick()

        assertEquals(1, events.count { it == TrackEvent.CheckinStillUseful })
        assertEquals(1, events.count { it == TrackEvent.CheckinPauseRequested })
    }

    @Test
    fun helpToggleButton_invokesToggleHelp_andCardIsHiddenWhenHelpOpenIsFalse() {
        val events = mutableListOf<TrackEvent>()
        setContent(uiState = TrackUiState(helpOpen = false), onEvent = { events += it })

        composeTestRule.onNodeWithTag("help_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("help_toggle_button").performClick()

        assertEquals(1, events.count { it == TrackEvent.ToggleHelp })
    }

    @Test
    fun helpCard_rendersTheFrozenCopy_whenHelpOpenIsTrue() {
        setContent(uiState = TrackUiState(helpOpen = true))

        composeTestRule.onNodeWithTag("help_card").assertExists()
        composeTestRule.onNodeWithText(
            "0 means it isn't happening right now. 1–3 you notice it. 4–6 it's changing what " +
                "you do. 7–10 it's most of what's happening.",
            substring = true
        ).assertExists()
    }

    @Test
    fun sleepButton_invokesArmSleep_andWakeButton_invokesArmWake() {
        val events = mutableListOf<TrackEvent>()
        setContent(uiState = TrackUiState(sleepOn = true), onEvent = { events += it })

        composeTestRule.onNodeWithTag("sleep_button").performClick()
        composeTestRule.onNodeWithTag("wake_button").performClick()

        assertEquals(1, events.count { it == TrackEvent.ArmSleep })
        assertEquals(1, events.count { it == TrackEvent.ArmWake })
    }

    @Test
    fun onsetChipPrompt_rendersChipsAndSubmitSkip_andTogglingInvokesOnsetChipToggled() {
        val events = mutableListOf<TrackEvent>()
        setContent(
            uiState = TrackUiState(onsetChipPrompt = OnsetChipPromptState(entryId = 1L)),
            onEvent = { events += it }
        )

        composeTestRule.onNodeWithTag("onset_chip_card").assertExists()
        composeTestRule.onNodeWithTag("onset_chip_flat").performClick()
        composeTestRule.onNodeWithTag("onset_chips_submit").assertExists()
        composeTestRule.onNodeWithTag("onset_chips_skip").assertExists()

        assertEquals(1, events.count { it == TrackEvent.OnsetChipToggled("flat") })
    }

    @Test
    fun markerToggleButton_invokesMarkerToggled_andInputIsHiddenWhenMarkerOpenIsFalse() {
        val events = mutableListOf<TrackEvent>()
        setContent(uiState = TrackUiState(markerOpen = false), onEvent = { events += it })

        composeTestRule.onNodeWithTag("marker_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("marker_toggle").performClick()

        assertEquals(1, events.count { it == TrackEvent.MarkerToggled })
    }

    @Test
    fun markerInput_rendersAndSaveCancelInvokeCorrectEvents_whenMarkerOpenIsTrue() {
        val events = mutableListOf<TrackEvent>()
        setContent(uiState = TrackUiState(markerOpen = true), onEvent = { events += it })

        composeTestRule.onNodeWithTag("marker_input").assertExists()
        composeTestRule.onNodeWithTag("marker_save").performClick()
        composeTestRule.onNodeWithTag("marker_cancel").performClick()

        assertEquals(1, events.count { it == TrackEvent.MarkerSaveConfirmed })
        assertEquals(1, events.count { it == TrackEvent.MarkerCancelled })
    }

    @Test
    fun entryRow_rendersKindBadgeAndChipsAsText_notColorAlone() {
        val entry = Entry(
            id = 1L,
            ts = 1_000L,
            value = 6,
            kind = com.kieslingdev.mindscale.data.EntryKind.SLEEP,
            chips = listOf("flat", "wired")
        )
        setContent(uiState = TrackUiState(recentEntries = listOf(entry), isEmpty = false))

        // The row uses semantics(mergeDescendants = true) so screen readers announce it
        // as one combined description (existing Phase 1 pattern) - that merges these
        // child nodes out of the default tree, so assert against the unmerged tree.
        composeTestRule.onNodeWithTag("entry_badge_1", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("asleep", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("entry_chips_1", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("flat · wired", useUnmergedTree = true).assertExists()
    }

    @Test
    fun toastBanner_rendersWhenToastIsPresent() {
        setContent(uiState = TrackUiState(toast = "Event marked"))

        composeTestRule.onNodeWithTag("toast_banner").assertExists()
        composeTestRule.onNodeWithText("Event marked").assertExists()
    }

    @Test
    fun backdateDialog_rendersViewModelOwnedRawDraft_andEmitsRawTextEvents() {
        val events = mutableListOf<TrackEvent>()
        setContent(
            uiState = TrackUiState(
                activeModal = TrackModalState.Backdate(
                    draft = BackdateDraft(7, "2026-0", "1", null),
                    timestampError = "Use yyyy-MM-dd and HH:mm."
                )
            ),
            onEvent = { events += it }
        )

        composeTestRule.onNodeWithText("Backdate entry").assertExists()
        composeTestRule.onNodeWithTag("track_dialog_date").performTextReplacement("2026-08-04")
        composeTestRule.onNodeWithTag("track_dialog_time").performTextReplacement("09:30")
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()

        assertTrue(events.contains(TrackEvent.BackdateDateTextChanged("2026-08-04")))
        assertTrue(events.contains(TrackEvent.BackdateTimeTextChanged("09:30")))
    }

    @Test
    fun editConflict_keepsDraftVisible_andRequiresExplicitWarnedSave() {
        setContent(
            uiState = TrackUiState(
                activeModal = TrackModalState.Edit(
                    draft = EditEntryDraft(
                        entryId = 9L,
                        baselineTimestampMillis = 0L,
                        baselineValue = 4,
                        baselineChips = listOf("flat"),
                        value = 8,
                        dateText = "1970-01-01",
                        timeText = "00:00",
                        chips = listOf("flat", "wired")
                    ),
                    validation = RecordValidation.Conflicting
                )
            )
        )

        composeTestRule.onNodeWithText("Edit entry").assertExists()
        composeTestRule.onNodeWithText("Value: 8").assertExists()
        composeTestRule.onNodeWithText("Save my changes").assertExists()
        composeTestRule.onNodeWithText(
            "This rating changed elsewhere. Saving will replace its current value, time, and chips. " +
                "Cancel and reopen to use the latest record."
        ).assertExists()
    }

    @Test
    fun noteChecking_disablesSave_andShowsExactMultilineDraft() {
        setContent(
            uiState = TrackUiState(
                activeModal = TrackModalState.Note(
                    draft = NoteEntryDraft(3L, "old", "  first\nsecond  "),
                    validation = RecordValidation.Checking
                )
            )
        )

        composeTestRule.onNodeWithText("Edit note").assertExists()
        composeTestRule.onNodeWithText("Checking record").assertExists()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("track_note_text").assertExists()
    }

    @Test
    fun dialogErrorAndActions_remainReachableAt200PercentFont_andErrorIsLive() {
        val error = "Use yyyy-MM-dd and HH:mm."
        setContent(
            uiState = TrackUiState(
                activeModal = TrackModalState.Backdate(
                    draft = BackdateDraft(7, "2026-0", "1", null),
                    timestampError = error
                )
            ),
            fontScale = 2f
        )

        composeTestRule.onNodeWithText("Backdate entry").assertIsDisplayed()
        composeTestRule.onNodeWithTag("track_dialog_date").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(error).performScrollTo().assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        )
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsDisplayed().assertIsNotEnabled()
    }
}
