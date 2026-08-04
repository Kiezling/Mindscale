package com.kieslingdev.mindscale.log

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LogScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.now()
    private val baseTs = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

    private fun setContent(state: LogUiState, events: MutableList<LogEvent> = mutableListOf()) {
        composeTestRule.setContent {
            MindScaleTheme { LogScreen(state, onEvent = { events += it }) }
        }
    }

    @Test
    fun mixedTimeline_rendersEveryRecordType_valueBadgesChipsAndNote() {
        val rating = Entry(
            id = 1,
            ts = baseTs,
            value = 6,
            chips = listOf("flat"),
            note = "Still there at bedtime.",
            kind = EntryKind.SLEEP
        )
        val items = combineLogItems(
            entries = listOf(rating, Entry(id = 2, ts = baseTs - 1_000, value = 0)),
            sleeps = listOf(
                SleepInterval(id = 3, startTs = baseTs - 2_000, endTs = baseTs + 8 * 60 * 60_000L),
                SleepInterval(id = 4, startTs = baseTs - 3_000, endTs = null)
            ),
            markers = listOf(Marker(id = 5, ts = baseTs - 4_000, text = "Dose increased"))
        )
        setContent(LogUiState(days = groupLogItems(items, zone), recordCount = items.size, hasAnyRecords = true))

        composeTestRule.onNodeWithText("6").assertExists()
        composeTestRule.onNodeWithText("went to sleep · flat").assertExists()
        composeTestRule.onNodeWithText("Still there at bedtime.").assertExists()
        composeTestRule.onNodeWithText("0").assertExists()
        composeTestRule.onNodeWithText("ended").assertExists()
        composeTestRule.onNodeWithText("sleeping now").assertExists()
        composeTestRule.onNodeWithText("Dose increased").assertExists()
    }

    @Test
    fun inlineEditPanel_rendersFrozenControls_andEmitsImmediateEditEvents() {
        val entry = Entry(id = 1, ts = baseTs, value = 5, chips = listOf("wired"))
        val days = groupLogItems(listOf(LogItem.Rating(entry)), zone)
        val events = mutableListOf<LogEvent>()

        setContent(
            LogUiState(
                days = days,
                recordCount = 1,
                hasAnyRecords = true,
                editDraft = LogEditDraft(1, 5, formatEditTimestamp(baseTs, zone), setOf("wired"))
            ),
            events
        )
        composeTestRule.onNodeWithTag("log_inline_edit_1").assertExists()
        composeTestRule.onNodeWithTag("log_edit_value_10").performClick()
        composeTestRule.onNodeWithTag("log_edit_chip_wired").performClick()
        composeTestRule.onNodeWithTag("log_edit_timestamp").performTextReplacement("2026-08-03 08:00")

        assertEquals(LogEvent.EditValueSelected(10), events[0])
        assertEquals(LogEvent.EditChipToggled("wired"), events[1])
        assertEquals(LogEvent.EditTimestampTextChanged("2026-08-03 08:00"), events.last())
    }

    @Test
    fun inlineNotePanel_rendersFrozenControls_andEmitsSaveCancel() {
        val entry = Entry(id = 1, ts = baseTs, value = 5)
        val events = mutableListOf<LogEvent>()
        setContent(
            LogUiState(
                days = groupLogItems(listOf(LogItem.Rating(entry)), zone),
                recordCount = 1,
                hasAnyRecords = true,
                noteDraft = LogNoteDraft(1, "draft")
            ),
            events
        )
        composeTestRule.onNodeWithTag("log_inline_note_1").assertExists()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf(LogEvent.NoteSaved, LogEvent.NoteCancelled), events)
    }

    @Test
    fun fromToAllAndValidation_areReachable() {
        val events = mutableListOf<LogEvent>()
        setContent(LogUiState(filterError = "From must be on or before To."), events)

        composeTestRule.onNodeWithTag("log_from_button").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithTag("log_to_button").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithTag("log_all_button").performClick()

        composeTestRule.onNodeWithTag("log_filter_error").assertExists()
        assertEquals(listOf(LogEvent.ClearFilter), events)
    }

    @Test
    fun filterAndDeleteControls_emitExpectedEvents_andDeleteRequiresConfirmation() {
        val entry = Entry(id = 1, ts = baseTs, value = 5)
        val events = mutableListOf<LogEvent>()
        val item = LogItem.Rating(entry)
        setContent(
            LogUiState(
                days = groupLogItems(listOf(item), zone),
                recordCount = 1,
                hasAnyRecords = true,
                deleteTarget = LogDeleteTarget(item, "rating 5")
            ),
            events
        )

        composeTestRule.onNodeWithTag("log_all_button").performClick()
        composeTestRule.onNodeWithText("Delete rating 5?").assertExists()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(LogEvent.ClearFilter, events.first())
        assertEquals(LogEvent.DeleteCancelled, events.last())
    }

    @Test
    fun emptyState_rendersDatabaseEmptyCopy() {
        setContent(LogUiState())
        composeTestRule.onNodeWithText("No records yet").assertExists()
    }

    @Test
    fun emptyState_rendersFilteredEmptyCopy() {
        setContent(
            LogUiState(
                appliedFilter = LogFilter(from = day),
                pendingFilter = LogFilter(from = day),
                hasAnyRecords = true
            )
        )
        composeTestRule.onNodeWithText("No records in this date range").assertExists()
    }
}
