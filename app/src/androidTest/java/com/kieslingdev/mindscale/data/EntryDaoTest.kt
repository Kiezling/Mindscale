package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryDaoTest {

    private lateinit var database: MindScaleDatabase
    private lateinit var dao: EntryDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, MindScaleDatabase::class.java)
            .build()
        dao = database.entryDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndObserveRecent_returnsRowsOrderedByTsThenIdDescending_limited() = runBlocking {
        // Two rows share the same ts to exercise the id DESC tiebreaker.
        val idA = dao.insert(Entry(ts = 1_000L, value = 3))
        val idB = dao.insert(Entry(ts = 1_000L, value = 4))
        val idC = dao.insert(Entry(ts = 2_000L, value = 5))
        val idD = dao.insert(Entry(ts = 500L, value = 1))

        val recent = dao.observeRecent(limit = 3).first()

        assertEquals(3, recent.size)
        // Expected order: ts=2000 (idC), then ts=1000 tie broken by id DESC (idB before idA).
        assertEquals(idC, recent[0].id)
        assertEquals(idB, recent[1].id)
        assertEquals(idA, recent[2].id)
        // idD (ts=500) is excluded by the limit.
        assertTrue(recent.none { it.id == idD })
    }

    @Test
    fun update_changesFieldsOnExistingRow_withoutCreatingNewId() = runBlocking {
        val id = dao.insert(Entry(ts = 1_000L, value = 5, note = "original"))
        val inserted = dao.observeRecent(limit = 10).first().single { it.id == id }

        val updated = inserted.copy(ts = 2_000L, value = 8, note = "edited")
        dao.update(updated)

        val rows = dao.observeRecent(limit = 10).first()
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals(id, row.id)
        assertEquals(2_000L, row.ts)
        assertEquals(8, row.value)
        assertEquals("edited", row.note)
    }

    @Test
    fun delete_removesExactlyTargetedRow() = runBlocking {
        val idA = dao.insert(Entry(ts = 1_000L, value = 1))
        val idB = dao.insert(Entry(ts = 2_000L, value = 2))
        val idC = dao.insert(Entry(ts = 3_000L, value = 3))

        val target = dao.observeRecent(limit = 10).first().single { it.id == idB }
        dao.delete(target)

        val remaining = dao.observeRecent(limit = 10).first()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.id == idA })
        assertTrue(remaining.any { it.id == idC })
        assertTrue(remaining.none { it.id == idB })
    }

    @Test
    fun observeCount_emitsZeroInitially_andUpdatesReactivelyAfterInsertAndDelete() = runBlocking {
        assertEquals(0, dao.observeCount().first())

        val id = dao.insert(Entry(ts = 1_000L, value = 7))
        assertEquals(1, dao.observeCount().first())

        val second = dao.insert(Entry(ts = 2_000L, value = 9))
        assertEquals(2, dao.observeCount().first())

        val inserted = dao.observeRecent(limit = 10).first().single { it.id == id }
        dao.delete(inserted)
        assertEquals(1, dao.observeCount().first())

        val remaining = dao.observeRecent(limit = 10).first().single { it.id == second }
        dao.delete(remaining)
        assertEquals(0, dao.observeCount().first())
    }
}
