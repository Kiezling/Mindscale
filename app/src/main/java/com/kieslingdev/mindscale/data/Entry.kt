package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ts: Long,                      // epoch millis, UTC; mutable (backdate/edit)
    val value: Int,                    // 0..10 inclusive
    val chips: List<String> = emptyList(), // set via onset-chip prompt or inline edit (Phase 2)
    val note: String? = null,
    val kind: EntryKind? = null        // null for an ordinary tap; SLEEP/WAKE when recorded while armed (Phase 2)
)
