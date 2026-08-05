package com.kieslingdev.mindscale.settings

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.MindScaleDatabase
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Restore preflight against a **real** Room database rather than a fake DAO.
 *
 * The JVM ViewModel tests use an in-memory fake, so they cannot see behaviour that only
 * appears with Room's generated `@Transaction` wrappers. Running the same preflight here
 * closes that gap for the one path that reads Room before publishing a preview.
 */
@RunWith(AndroidJUnit4::class)
class BackupRestorePreflightTest {

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

    private fun await(predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(50)
        }
        throw AssertionError(
            "Timed out. importing=${viewModel.uiState.value.importing}, " +
                "pendingImport=${viewModel.uiState.value.pendingImport != null}, " +
                "importError=${viewModel.uiState.value.importError}"
        )
    }

    @Test
    fun aValidBackupPublishesAPreviewAgainstRealRoomState() = runBlocking {
        database.entryDao().insert(Entry(ts = 1_000, value = 5))

        viewModel.importFileSelected(ImportKind.BACKUP_RESTORE) {
            ByteArrayInputStream(REAL_EXPORT.toByteArray(Charsets.UTF_8))
        }
        await { !viewModel.uiState.value.importing }

        val state = viewModel.uiState.value
        assertNull("Preflight must not reject a real export: ${state.importError}", state.importError)
        val pending = assertNotNull(state.pendingImport).let { state.pendingImport!! }
        assertEquals(ImportKind.BACKUP_RESTORE, pending.kind)
        val text = pending.preview.lines.joinToString("\n")
        assertTrue(text, text.contains("version 5"))
        assertTrue(text, text.contains("It contains 4 ratings"))
        assertTrue(text, text.contains("permanently delete 1 rating"))
        assertTrue(text, text.contains("Check-in time, the sleep introduction flag"))
    }

    @Test
    fun confirmingReplacesEverythingInRealRoom() = runBlocking {
        database.entryDao().insert(Entry(ts = 1_000, value = 5))

        viewModel.importFileSelected(ImportKind.BACKUP_RESTORE) {
            ByteArrayInputStream(REAL_EXPORT.toByteArray(Charsets.UTF_8))
        }
        await { viewModel.uiState.value.pendingImport != null }

        viewModel.confirmImport()
        await { viewModel.uiState.value.pendingImport == null && !viewModel.uiState.value.importing }

        assertNull(viewModel.uiState.value.importError)
        val snapshot = database.dataControlDao().snapshot()
        assertEquals(listOf(4L, 3L, 2L, 1L), snapshot.entries.map { it.id })
        assertEquals("Ada L", snapshot.profile.displayName)
        assertEquals(12, snapshot.externalScores.single().total)
        assertEquals(8, snapshot.settings.holdDuration.hours)
    }
}

private val REAL_EXPORT = """
{
  "format": "mindscale-backup",
  "version": 5,
  "exportedAt": "2026-08-05T04:03:37.707072Z",
  "entries": [
    {"id": 4, "timestamp": "2026-08-05T04:00:31.843Z", "intensity": 10, "chips": [], "note": null, "kind": null},
    {"id": 3, "timestamp": "2026-08-05T04:00:30.063Z", "intensity": 0, "chips": [], "note": null, "kind": null},
    {"id": 2, "timestamp": "2026-08-05T04:00:29.284Z", "intensity": 3, "chips": [], "note": null, "kind": null},
    {"id": 1, "timestamp": "2026-08-05T04:00:28.185Z", "intensity": 7, "chips": [], "note": null, "kind": null}
  ],
  "sleeps": [],
  "markers": [],
  "settings": {
    "themeMode": "DARK",
    "hourFormat": "TWENTY_FOUR",
    "anchor2": "",
    "anchor5": "",
    "anchor8": "",
    "onsetChips": ["flat", "agitated", "hopeless", "numb", "wired", "foggy", "alone", "driving", "work", "poor sleep"],
    "sleepOn": true,
    "askChips": false,
    "hideNotes": false,
    "paused": false,
    "holdHours": 8
  },
  "profile": {"displayName": "Ada L"},
  "externalScores": [
    {"id": 1, "instrument": "PHQ_8", "total": 12, "assessedDate": "2026-08-04", "provenance": "EXTERNALLY_OBTAINED_USER_ENTERED", "enteredAt": "2026-08-05T04:02:27.981Z"}
  ]
}
""".trimIndent()
