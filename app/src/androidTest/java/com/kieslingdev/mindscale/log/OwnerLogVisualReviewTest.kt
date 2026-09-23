package com.kieslingdev.mindscale.log

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Synthetic records only; screenshots are written to the test app's external files. */
@RunWith(Parameterized::class)
class OwnerLogVisualReviewTest(private val mode: ThemeMode, private val scale: Float) {
    @get:Rule val rule = createComposeRule()

    @Test fun ratingEventAndEditors() {
        val zone = ZoneId.systemDefault()
        val ts = LocalDate.now().atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val rating = Entry(id = 101, ts = ts, value = 6, note = "A quieter afternoon after a busy morning.")
        val marker = Marker(id = 202, ts = ts - 60_000, text = "Dose adjusted after appointment")
        val days = groupLogItems(listOf(LogItem.Rating(rating), LogItem.Event(marker)), zone)
        var state by mutableStateOf(LogUiState(days = days, recordCount = 2, hasAnyRecords = true))
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                MindScaleTheme(themeMode = mode) {
                    Surface(Modifier.fillMaxSize()) { LogScreen(state, {}) }
                }
            }
        }
        save("rows")
        rule.onNodeWithTag("full_log_screen").performScrollToNode(hasTestTag("log_row_marker:202"))
        save("event-row")
        rule.runOnIdle {
            state = state.copy(eventDraft = LogEventDraft(marker.id, marker.text, formatEditTimestamp(marker.ts, zone)))
        }
        rule.onNodeWithTag("full_log_screen").performScrollToNode(hasTestTag("log_inline_event_202"))
        save("event-editor")
        rule.runOnIdle {
            state = state.copy(eventDraft = null, noteDraft = LogNoteDraft(rating.id, rating.note!!))
        }
        rule.onNodeWithTag("full_log_screen").performScrollToNode(hasTestTag("log_inline_note_101"))
        save("note-editor")
    }

    private fun save(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "owner-log-$name-${mode.name}-$scale.png")
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}-{1}")
        fun configurations() = listOf(
            arrayOf<Any>(ThemeMode.LIGHT, 1f), arrayOf<Any>(ThemeMode.DARK, 1f),
            arrayOf<Any>(ThemeMode.LIGHT, 2f), arrayOf<Any>(ThemeMode.DARK, 2f)
        )
    }
}
