package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.kieslingdev.mindscale.insights.isOnsetAt
import kotlinx.coroutines.flow.Flow

data class EpisodeSourceRow(
    val recordType: String,
    val id: Long,
    val ts: Long,
    val endTs: Long?,
    val value: Int?,
    val chips: List<String>?,
    val note: String? = null,
    val text: String? = null
)

data class OrdinaryCaptureResult(
    val entryId: Long,
    val isOnset: Boolean,
    val promptEnabled: Boolean,
    val settingsAvailable: Boolean,
    val classificationAvailable: Boolean
)

@Dao
interface EpisodeSourceDao {
    @Query(
        """SELECT 'ENTRY' AS recordType, id, ts, NULL AS endTs, value, chips, note, NULL AS text
           FROM entries
           UNION ALL
           SELECT 'SLEEP' AS recordType, id, startTs AS ts, endTs,
                  NULL AS value, NULL AS chips, NULL AS note, NULL AS text
           FROM sleeps
           UNION ALL
           SELECT 'MARKER' AS recordType, id, ts, NULL AS endTs,
                  NULL AS value, NULL AS chips, NULL AS note, text
           FROM markers
           ORDER BY ts ASC, recordType ASC, id ASC"""
    )
    fun observeSource(): Flow<List<EpisodeSourceRow>>

    @Query(
        """SELECT 'ENTRY' AS recordType, id, ts, NULL AS endTs, value, chips, note, NULL AS text
           FROM entries WHERE ts <= :ts
           UNION ALL
           SELECT 'SLEEP' AS recordType, id, startTs AS ts, endTs,
                  NULL AS value, NULL AS chips, NULL AS note, NULL AS text
           FROM sleeps WHERE startTs <= :ts
           UNION ALL
           SELECT 'MARKER' AS recordType, id, ts, NULL AS endTs,
                  NULL AS value, NULL AS chips, NULL AS note, text
           FROM markers WHERE ts <= :ts
           ORDER BY ts ASC, recordType ASC, id ASC"""
    )
    suspend fun sourceAtOrBefore(ts: Long): List<EpisodeSourceRow>

    @Query("SELECT * FROM track_settings WHERE id = 0")
    suspend fun currentSettings(): TrackSettings?

    @Insert
    suspend fun insertEntry(entry: Entry): Long

    @Transaction
    suspend fun insertOrdinaryAndClassify(entry: Entry): OrdinaryCaptureResult {
        require(entry.kind == null && entry.value in 0..10)
        val settings = currentSettings()
        val hold = settings?.holdDuration ?: HoldDuration.SIXTEEN
        val classification = runCatching {
            entry.value > 0 && isOnsetAt(sourceAtOrBefore(entry.ts), entry.ts, hold)
        }
        val entryId = insertEntry(entry)
        val isOnset = classification.getOrDefault(false)
        return OrdinaryCaptureResult(
            entryId = entryId,
            isOnset = isOnset,
            promptEnabled = isOnset && settings?.askChips == true,
            settingsAvailable = settings != null,
            classificationAvailable = classification.isSuccess
        )
    }
}
