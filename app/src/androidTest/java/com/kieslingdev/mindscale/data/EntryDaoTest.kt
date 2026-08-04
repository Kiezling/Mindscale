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

    @Test
    fun mostRecentAtOrBefore_returnsNull_whenNoEntryQualifies() = runBlocking {
        dao.insert(Entry(ts = 5_000L, value = 3))

        assertEquals(null, dao.mostRecentAtOrBefore(1_000L))
    }

    @Test
    fun mostRecentAtOrBefore_returnsTheSingleQualifyingEntry() = runBlocking {
        val id = dao.insert(Entry(ts = 1_000L, value = 5))

        val result = dao.mostRecentAtOrBefore(1_000L)

        assertEquals(id, result!!.id)
    }

    @Test
    fun mostRecentAtOrBefore_breaksSameTsTiesByHigherId() = runBlocking {
        dao.insert(Entry(ts = 1_000L, value = 1))
        val secondId = dao.insert(Entry(ts = 1_000L, value = 2))

        val result = dao.mostRecentAtOrBefore(1_000L)

        assertEquals(secondId, result!!.id)
    }

    /**
     * D-3 / Invariant 15 of SPEC-track-phase2-completeness.md: onset detection must be
     * relative to the capture's own timestamp, not to whatever is newest in the whole
     * table. A backdated capture between two existing entries must see only the entry
     * that precedes it in time, never one that follows it — even though that later entry
     * is the chronologically newest row overall.
     */
    @Test
    fun mostRecentAtOrBefore_ignoresEntriesWithLaterTs_evenWhenTheyAreNewestOverall() = runBlocking {
        val t1Id = dao.insert(Entry(ts = 1_000L, value = 5))
        dao.insert(Entry(ts = 3_000L, value = 0)) // T3: chronologically newest, but after T2
        val t2 = 2_000L // backdated capture between T1 and T3

        val result = dao.mostRecentAtOrBefore(t2)

        assertEquals(t1Id, result!!.id)
        assertEquals(5, result.value)
    }

    @Test
    fun updateChips_patchesOnlyTheChipsColumn_leavingOtherFieldsUntouched() = runBlocking {
        val id = dao.insert(Entry(ts = 1_000L, value = 6, note = "unrelated note"))

        dao.updateChips(id, listOf("flat", "wired"))

        val row = dao.observeRecent(limit = 10).first().single { it.id == id }
        assertEquals(listOf("flat", "wired"), row.chips)
        assertEquals(1_000L, row.ts)
        assertEquals(6, row.value)
        assertEquals("unrelated note", row.note)
    }

    @Test
    fun observeBetween_usesHalfOpenBounds_andDeterministicTieOrder() = runBlocking {
        val lowerExcluded = dao.insert(Entry(ts = 999L, value = 1))
        val firstTie = dao.insert(Entry(ts = 1_000L, value = 2))
        val secondTie = dao.insert(Entry(ts = 1_000L, value = 3))
        val upperExcluded = dao.insert(Entry(ts = 2_000L, value = 4))

        val bounded = dao.observeBetween(1_000L, 2_000L).first()
        assertEquals(listOf(secondTie, firstTie), bounded.map { it.id })
        assertEquals(listOf(upperExcluded, secondTie, firstTie), dao.observeBetween(1_000L, null).first().map { it.id })
        assertEquals(setOf(lowerExcluded, firstTie, secondTie), dao.observeBetween(null, 2_000L).first().map { it.id }.toSet())
        assertEquals(4, dao.observeBetween(null, null).first().size)
    }

    @Test
    fun targetedMutations_preserveUnrelatedColumns_andMissingIdsReturnZero() = runBlocking {
        val id = dao.insert(
            Entry(
                ts = 1_000L,
                value = 4,
                chips = listOf("flat"),
                note = "keep me",
                kind = EntryKind.SLEEP
            )
        )

        assertEquals(1, dao.updateEditableFields(id, 2_000L, 7, listOf("wired")))
        var row = dao.observeBetween(null, null).first().single()
        assertEquals("keep me", row.note)
        assertEquals(EntryKind.SLEEP, row.kind)

        assertEquals(1, dao.updateNote(id, "changed"))
        row = dao.observeBetween(null, null).first().single()
        assertEquals(2_000L, row.ts)
        assertEquals(7, row.value)
        assertEquals(listOf("wired"), row.chips)
        assertEquals(EntryKind.SLEEP, row.kind)

        assertEquals(0, dao.updateNote(99_999L, "missing"))
        assertEquals(1, dao.deleteById(id))
        assertEquals(0, dao.deleteById(id))
    }
}
