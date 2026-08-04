package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EpisodeSourceDao
import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.TrackSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FakeEpisodeSourceDao(
    private val entryDao: FakeEntryDao,
    private val settingsDao: FakeTrackSettingsDao
) : EpisodeSourceDao {
    override fun observeSource(): Flow<List<EpisodeSourceRow>> = entryDao.observeRecent(Int.MAX_VALUE).map { entries ->
        entries.sortedWith(compareBy<Entry> { it.ts }.thenBy { it.id }).map(::toSourceRow)
    }

    override suspend fun sourceAtOrBefore(ts: Long): List<EpisodeSourceRow> =
        entryDao.observeRecent(Int.MAX_VALUE).first()
            .filter { it.ts <= ts }
            .sortedWith(compareBy<Entry> { it.ts }.thenBy { it.id })
            .map(::toSourceRow)

    override suspend fun currentSettings(): TrackSettings = settingsDao.current()

    override suspend fun insertEntry(entry: Entry): Long = entryDao.insert(entry)

    private fun toSourceRow(entry: Entry) = EpisodeSourceRow(
        recordType = "ENTRY",
        id = entry.id,
        ts = entry.ts,
        endTs = null,
        value = entry.value,
        chips = entry.chips
    )
}
