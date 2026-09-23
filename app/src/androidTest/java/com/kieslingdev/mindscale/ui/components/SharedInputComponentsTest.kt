package com.kieslingdev.mindscale.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.RootMatchers.isDialog
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.notes.NoteStyle
import com.kieslingdev.mindscale.notes.RichNoteCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class SharedInputComponentsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun richNoteToolbarFormatsSelectedTextAndRendersItWhenReopened() {
        var stored by mutableStateOf("")
        var reopened by mutableStateOf(false)
        compose.setContent {
            if (reopened) {
                RichNoteEditor(stored, {}, Modifier.testTag("reopened_note"))
            } else {
                RichNoteEditor(stored, { stored = it }, Modifier.testTag("note"))
            }
        }
        compose.onNodeWithTag("note").performTextReplacement("note")
        compose.onNodeWithTag("note").performTextInputSelection(TextRange(0, 4))
        compose.onNodeWithContentDescription("Bold").performClick()

        assertEquals(NoteStyle.BOLD, RichNoteCodec.decode(stored).spans.single().style)
        reopened = true
        compose.onNodeWithTag("reopened_note").assertTextEquals("note")
        assertEquals("note", RichNoteCodec.plainText(stored))
    }

    @Test fun collapsedBoldStylesEverySequentiallyTypedCharacter() {
        var upstreamValue by mutableStateOf("")
        val delayedEchoes = mutableListOf<String>()
        compose.setContent {
            RichNoteEditor(upstreamValue, { delayedEchoes += it }, Modifier.testTag("typing_note"))
        }
        compose.onNodeWithContentDescription("Bold").performClick()
        "A calmer day".map(Char::toString).forEach { chunk ->
            compose.onNodeWithTag("typing_note").performTextInput(chunk)
        }

        // Deliver every stale StateFlow value after typing. Each is an acknowledgement, not an
        // instruction to roll the editor back to an earlier character.
        delayedEchoes.forEach { echo ->
            compose.runOnIdle { upstreamValue = echo }
            compose.waitForIdle()
            compose.onNodeWithTag("typing_note").assertTextEquals("A calmer day")
        }

        val note = RichNoteCodec.decode(delayedEchoes.last())
        assertEquals("A calmer day", note.text)
        assertEquals(
            listOf(com.kieslingdev.mindscale.notes.NoteSpan(NoteStyle.BOLD, 0, note.text.length)),
            note.spans
        )
    }

    @Test fun wheelsAndCalendarOnlyCallBackAfterConfirmation() {
        var date: String? = null
        var time: String? = null
        compose.setContent {
            MsDateTimeFields(
                dateText = "2026-09-17",
                timeText = "10:30",
                onDateChanged = { date = it },
                onTimeChanged = { time = it },
                hourFormat = HourFormat.TWENTY_FOUR,
                tagPrefix = "shared"
            )
        }

        compose.onNodeWithTag("shared_time").performClick()
        onView(withContentDescription("Hour")).inRoot(isDialog()).perform(swipeUp())
        assertEquals(null, time)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(null, time)

        compose.onNodeWithTag("shared_time").performClick()
        onView(withContentDescription("Hour")).inRoot(isDialog()).perform(swipeUp())
        compose.onNodeWithText("OK").performClick()
        assertNotEquals("10:30", time)

        compose.onNodeWithTag("shared_date").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(null, date)
        compose.onNodeWithTag("shared_date").performClick()
        compose.onNodeWithText("OK").performClick()
        assertEquals("2026-09-17", date)
    }

    @Test fun darkTwoHundredPercentTimeWheelRemainsVisibleAndScrollable() {
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    MsDateTimeFields(
                        dateText = "2026-09-17",
                        timeText = "10:30",
                        onDateChanged = {},
                        onTimeChanged = {},
                        hourFormat = HourFormat.TWELVE
                    )
                }
            }
        }
        compose.onNodeWithTag("timestamp_time").performClick()
        compose.onNodeWithTag("time_hour").assertIsDisplayed()
        compose.onNodeWithTag("time_minute").assertIsDisplayed()
        compose.onNodeWithTag("time_ampm").assertIsDisplayed()
    }

}
