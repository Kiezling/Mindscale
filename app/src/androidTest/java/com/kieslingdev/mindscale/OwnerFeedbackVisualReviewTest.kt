package com.kieslingdev.mindscale

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.track.*
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Synthetic, database-free visual evidence for owner-feedback criteria 1–5. */
@RunWith(Parameterized::class)
class OwnerFeedbackVisualReviewTest(private val mode: ThemeMode, private val scale: Float) {
    @get:Rule val rule = createComposeRule()

    @Test fun trackHeadersAndNoteToolbar() {
        var title by mutableStateOf("Track")
        val entry = Entry(id = 42, ts = 1789992000000L, value = 6, note = "A quieter afternoon after a busy morning.")
        var state by mutableStateOf(TrackUiState(recentEntries = listOf(entry), isEmpty = false))
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                MindScaleTheme(themeMode = mode) {
                    Surface(Modifier.fillMaxSize()) {
                        Column {
                            MindScaleHeader(true, title, {}, {})
                            TrackScreen(state, {}, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        listOf("Track", "Log", "Insights").forEach { page ->
            rule.runOnIdle { title = page }
            val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
            val brand = rule.onNodeWithTag("header_wordmark").fetchSemanticsNode().boundsInRoot
            val profile = rule.onNodeWithTag("profile_action").fetchSemanticsNode().boundsInRoot
            assertEquals(root.center.x, brand.center.x, 1.5f)
            assertTrue(brand.left >= root.left && brand.right <= root.right)
            assertTrue(!brand.overlaps(profile))
        }
        rule.runOnIdle { title = "Track" }
        save("track-top")
        rule.onNodeWithTag("track_screen").performScrollToNode(hasTestTag("help_toggle_button"))
        val help = rule.onNodeWithTag("help_toggle_button").fetchSemanticsNode().boundsInRoot
        val sleep = rule.onNodeWithTag("sleep_button").fetchSemanticsNode().boundsInRoot
        assertTrue(help.top >= sleep.bottom)
        rule.onNodeWithTag("track_screen").performScrollToNode(hasTestTag("marker_toggle"))
        rule.onNodeWithTag("marker_toggle").assertHeightIsAtLeast(48.dp).assertHasClickAction()
        save("track-actions")
        rule.onNodeWithTag("track_screen").performScrollToNode(hasText("Recent Logs"))
        save("track-recent")
        rule.onNodeWithTag("track_screen").performScrollToNode(hasTestTag("track_export_logs"))
        rule.onNodeWithTag("track_import_logs").assertHasClickAction()
        rule.onNodeWithTag("track_export_logs").assertHasClickAction()
        save("track-backup-footer")
        rule.onNodeWithTag("track_screen").performScrollToNode(hasTestTag("help_toggle_button"))
        rule.runOnIdle { state = state.copy(helpOpen = true) }
        rule.onNodeWithTag("track_screen").performScrollToNode(hasText("When you go to sleep", substring = true))
        save("track-help")
        rule.runOnIdle {
            val note = requireNotNull(entry.note)
            state = state.copy(activeModal = TrackModalState.Note(
                NoteEntryDraft(entry.id, note, note), RecordValidation.Current
            ))
        }
        val editor = rule.onNodeWithTag("track_note_text").fetchSemanticsNode().boundsInRoot
        val bold = rule.onNodeWithContentDescription("Bold").fetchSemanticsNode().boundsInRoot
        assertTrue(bold.bottom <= editor.top)
        rule.onNodeWithTag("track_delete_note").assertExists()
        save("track-note")
    }

    private fun save(name: String) {
        rule.waitForIdle()
        // Compose idleness does not include the platform dialog window's enter animation.
        android.os.SystemClock.sleep(350)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "owner-$name-${mode.name}-$scale.png")
        instrumentation.uiAutomation.takeScreenshot().useBitmap { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private inline fun Bitmap.useBitmap(block: (Bitmap) -> Unit) {
        try { block(this) } finally { recycle() }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}-{1}")
        fun configurations() = listOf(
            arrayOf<Any>(ThemeMode.LIGHT, 1f), arrayOf<Any>(ThemeMode.DARK, 1f),
            arrayOf<Any>(ThemeMode.LIGHT, 2f), arrayOf<Any>(ThemeMode.DARK, 2f)
        )
    }
}
