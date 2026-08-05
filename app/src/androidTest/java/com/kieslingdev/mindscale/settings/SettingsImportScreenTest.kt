package com.kieslingdev.mindscale.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose coverage for the Phase 12 "Bring data back" section
 * (`docs/specs/SPEC-import-restore.md`, D-1, D-7, D-9, D-11).
 */
class SettingsImportScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var database: MindScaleDatabase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        viewModel = SettingsViewModel(
            settingsDao = database.trackSettingsDao(),
            dataControlDao = database.dataControlDao(),
            savedStateHandle = SavedStateHandle(),
            ioContext = Dispatchers.IO
        )
    }

    @After
    fun closeDatabase() = database.close()

    private fun setContent() {
        composeTestRule.setContent {
            MindScaleTheme {
                SettingsRoute(viewModel = viewModel, focus = SettingsFocus.DATA)
            }
        }
    }

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag(tag))
    }

    private fun validCsv(): String =
        "$RECORDS_CSV_HEADER\r\nmarker,2026-08-05T10:00:00Z,,,,,,added event\r\n"

    private fun stream(text: String): suspend () -> InputStream =
        { ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)) }

    @Test
    fun bothImportActions_existAndHaveAccessibleNames() {
        setContent()

        scrollTo("import_backup")
        composeTestRule.onNodeWithTag("import_backup").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_records").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Restore from a MindScale JSON backup").assertExists()
        composeTestRule.onNodeWithContentDescription("Import a MindScale records CSV").assertExists()
    }

    @Test
    fun bothImportActions_haveAtLeast48DpTouchTargets() {
        setContent()

        scrollTo("import_backup")
        composeTestRule.onNodeWithTag("import_backup").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("import_records").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun validRecordsCsv_showsPreviewDialog_withoutChangingTheDatabase() {
        setContent()
        val before = runBlocking { database.dataControlDao().snapshot() }

        viewModel.importFileSelected(ImportKind.RECORDS_MERGE, stream(validCsv()))
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("import_preview")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("import_preview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nothing is deleted.", substring = true).assertExists()
        composeTestRule.onNodeWithText("Add these records").assertExists()

        val after = runBlocking { database.dataControlDao().snapshot() }
        assertEquals(before, after)
    }

    @Test
    fun confirmImport_performsTheImport_andDismissesTheDialog() {
        setContent()

        viewModel.importFileSelected(ImportKind.RECORDS_MERGE, stream(validCsv()))
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("import_preview")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("confirm_import").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("import_preview")).fetchSemanticsNodes().isEmpty()
        }

        val after = runBlocking { database.dataControlDao().snapshot() }
        assertEquals(1, after.markers.size)
        assertTrue(after.markers.any { it.text == "added event" })
    }

    @Test
    fun cancelImport_dismissesTheDialog_andChangesNothing() {
        setContent()
        val before = runBlocking { database.dataControlDao().snapshot() }

        viewModel.importFileSelected(ImportKind.RECORDS_MERGE, stream(validCsv()))
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("import_preview")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("cancel_import").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("import_preview")).fetchSemanticsNodes().isEmpty()
        }

        val after = runBlocking { database.dataControlDao().snapshot() }
        assertEquals(before, after)
    }

    @Test
    fun aRejectedFile_surfacesTheExactFrozenMessage_andShowsNoDialog() {
        setContent()

        viewModel.importFileSelected(ImportKind.RECORDS_MERGE, stream("not,a,mindscale,csv\r\n"))
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("import_error")).fetchSemanticsNodes().isNotEmpty()
        }

        scrollTo("import_error")
        composeTestRule.onNodeWithTag("import_error").assertIsDisplayed()
        composeTestRule.onNodeWithText(ImportMessages.NOT_A_RECORDS_CSV).assertExists()
        composeTestRule.onAllNodes(hasTestTag("import_preview")).assertCountEquals(0)
    }

    @Test
    fun aStreamOpenerThatThrowsIOException_surfacesReadFailed() {
        setContent()

        viewModel.importFileSelected(ImportKind.RECORDS_MERGE) { throw IOException("boom") }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasTestTag("import_error")).fetchSemanticsNodes().isNotEmpty()
        }

        scrollTo("import_error")
        composeTestRule.onNodeWithTag("import_error").assertIsDisplayed()
        composeTestRule.onNodeWithText(ImportMessages.READ_FAILED).assertExists()
    }
}
