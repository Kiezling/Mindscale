package com.kieslingdev.mindscale.log

import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import java.time.LocalDate

sealed interface LogItem {
    val id: Long
    val stableKey: String
    val timestamp: Long

    data class Rating(val entry: Entry) : LogItem {
        override val id: Long = entry.id
        override val stableKey: String = "entry:${entry.id}"
        override val timestamp: Long = entry.ts
    }

    data class Sleep(val interval: SleepInterval) : LogItem {
        override val id: Long = interval.id
        override val stableKey: String = "sleep:${interval.id}"
        override val timestamp: Long = interval.startTs
    }

    data class Event(val marker: Marker) : LogItem {
        override val id: Long = marker.id
        override val stableKey: String = "marker:${marker.id}"
        override val timestamp: Long = marker.ts
    }
}

data class LogDay(val date: LocalDate, val items: List<LogItem>)

data class LogFilter(val from: LocalDate? = null, val to: LocalDate? = null)

data class LogEditDraft(
    val entryId: Long,
    val value: Int,
    val timestampText: String,
    val chips: Set<String>,
    val error: String? = null
)

data class LogNoteDraft(val entryId: Long, val text: String)

data class LogDeleteTarget(
    val item: LogItem,
    val description: String
)

data class LogUiState(
    val appliedFilter: LogFilter = LogFilter(),
    val pendingFilter: LogFilter = LogFilter(),
    val days: List<LogDay> = emptyList(),
    val recordCount: Int = 0,
    val hasAnyRecords: Boolean = false,
    val filterError: String? = null,
    val editDraft: LogEditDraft? = null,
    val noteDraft: LogNoteDraft? = null,
    val deleteTarget: LogDeleteTarget? = null,
    val message: String? = null,
    val readError: String? = null,
    val settings: TrackSettings = TrackSettings()
)
