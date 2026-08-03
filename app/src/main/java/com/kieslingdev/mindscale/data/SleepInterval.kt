package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * `startTs`/`endTs` (not `start`/`end`) to match `entries.ts`'s `...Ts` naming
 * convention for epoch-millis columns, and to avoid `end` reading like a bare SQL
 * keyword in hand-written `@Query` strings and migrations.
 */
@Entity(tableName = "sleeps")
data class SleepInterval(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startTs: Long,
    val endTs: Long? = null   // null = still open (asleep)
)
