SPEC-track-numpad-logging: Track screen numpad logging + recent entries list

Status: IMPLEMENTED

Owner: TBD
Date: 2026-08-03
Last verified commit: implemented and oracle-verified 2026-08-03 (test/lint/assembleDebug/connectedDebugAndroidTest all green on MindScale_API_36); see the commit that introduces this spec for the exact hash

## Purpose

Deliver the Phase 1 vertical slice of MindScale v2: a single Track screen where a user presses a numpad key (0–10) to log a symptom-intensity Entry at the current wall-clock time, or long-presses to backdate one, and sees their most recent entries in a list with edit/note/delete actions — all persisted locally via Room. This is the thinnest end-to-end path (UI → ViewModel → Room → UI) that proves the architecture for later phases. It implements no interpretation, no scheduling, no server calls — it is a data-entry and recall instrument only, consistent with MindScale's axioms (event-contingent, zero cost on a well day, episode-as-unit deferred to later phases, no inference, no accounts/network/gamification).

## Non-goals

- No sleep tracking, onset chips UI, marker chips, help card, paused/check-in banners, or breathing exercise. These are later phases of the approved 8-phase plan.
- No episode grouping/aggregation logic — Entry is a flat row; episode-as-unit semantics are a later phase even though the axiom is already true of the product.
- No notifications, reminders, or background work of any kind.
- No navigation framework (nav-compose) — this slice is a single screen with no destinations to route between.
- No repository/use-case layer — ViewModel talks to the DAO directly (see Open questions, decision D-3).
- No network, accounts, analytics, or telemetry.
- No sample-data seeding (see Open questions, decision D-2).
- No DI framework — manual constructor injection only (already-fixed constraint, D-002 equivalent for this feature).

## User experience

Single screen ("Track"):

- **Numpad**: 3×3 grid of keys 1–9 (band groups: mild 1-3, moderate 4-6, severe 7-9), plus a visually distinct row below holding 0 ("ended / nothing happening") and 10 ("critical ceiling"), each occupying its own emphasis (different shape/tone from the 1–9 grid, not just position).
- **Tap** a key → immediately inserts an Entry at `now()` with that value. A transient readout appears (e.g. "7 · severe") and auto-dismisses after a few seconds; it is not persistent chrome.
- **Long-press** a key → opens a backdate dialog showing the picked value, an editable datetime field defaulting to now, and Save/Cancel. Save inserts the Entry at the chosen timestamp; Cancel discards with no side effects. The long-press threshold is the platform's default (`ViewConfiguration.longPressTimeoutMillis`, ≈500ms), not a hand-rolled ~460ms timer (see D-4) — and it must not also fire a tap-insert (mutually exclusive gesture).
- **Recent entries list**: newest 10 entries (`ts DESC`, ties broken by `id DESC`), each row shows a color-coded circular value indicator (monotonic intensity ramp, distinct light/dark variant, value also shown as text — color is never the only signal), formatted date, formatted time, and a truncated note preview if present. Three row actions: **Edit** (reopens value + timestamp inline for editing), **Note** (opens/edits free-text note), **Delete** (see D-1: requires confirmation).
- **Empty state** (zero entries): "Nothing recorded yet" + explanatory copy that a good day costs nothing and requires no log. No sample-data affordance in this slice (D-2). Empty state is rendered whenever the entry count is 0 — it is a normal, permanent-until-first-log state, never an error or loading placeholder.
- No entry is ever required. Nothing on this screen nags, badges, or reminds.

## Frozen interfaces and data contracts

### Entity

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ts: Long,                      // epoch millis, UTC; mutable (backdate/edit)
    val value: Int,                    // 0..10 inclusive
    val chips: List<String> = emptyList(), // unused this slice; always empty from UI; forward-compat for Phase 2
    val note: String? = null
)
```

### TypeConverter (forward-compat for `chips`, D-6)

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.TypeConverter

class ChipsConverter {
    @TypeConverter
    fun fromChips(chips: List<String>): String = chips.joinToString(CHIP_DELIMITER)

    @TypeConverter
    fun toChips(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split(CHIP_DELIMITER)

    private companion object {
        const val CHIP_DELIMITER = "" // ASCII unit separator; never user-typed
    }
}
```

### DAO

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("SELECT * FROM entries ORDER BY ts DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<Entry>>

    @Query("SELECT COUNT(*) FROM entries")
    fun observeCount(): Flow<Int>
}
```

### Database

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Entry::class], version = 1, exportSchema = true)
@TypeConverters(ChipsConverter::class)
abstract class MindScaleDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
```

### Domain helper (frozen, pure)

```kotlin
package com.kieslingdev.mindscale.track

fun band(value: Int): String = when (value) {
    0 -> "ended"
    in 1..3 -> "mild"
    in 4..6 -> "moderate"
    in 7..9 -> "severe"
    10 -> "critical"
    else -> throw IllegalArgumentException("value out of range 0..10: $value")
}
```

### ViewModel UI state, events, and class shape

```kotlin
package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Entry

data class TrackUiState(
    val recentEntries: List<Entry> = emptyList(),
    val isEmpty: Boolean = true,
    val transientReadout: ReadoutState? = null,
    val backdateDialog: BackdateDialogState? = null,
    val editDialog: EditEntryState? = null,
    val noteDialog: NoteEditState? = null,
    val pendingDelete: Entry? = null
)

data class ReadoutState(val value: Int, val band: String, val expiresAtMillis: Long)
data class BackdateDialogState(val value: Int, val timestampMillis: Long, val error: String? = null)

// originalEntry is the full Entry captured at dialog-open time (from
// EditRequested/NoteRequested); save always derives the persisted row from
// this snapshot (id/chips/note, or id/chips/value/ts respectively) plus the
// edited field(s) — it never depends on the entry still being present in
// the current (top-10) recentEntries list, which can change while a dialog
// is open (e.g. a new tap-insert pushing the entry out of the window).
data class EditEntryState(
    val originalEntry: Entry,
    val value: Int,
    val timestampMillis: Long,
    val error: String? = null
) {
    val entryId: Long get() = originalEntry.id
}

data class NoteEditState(val originalEntry: Entry, val text: String) {
    val entryId: Long get() = originalEntry.id
}

sealed interface TrackEvent {
    data class KeyTapped(val value: Int) : TrackEvent
    data class KeyLongPressed(val value: Int) : TrackEvent
    data class BackdateTimestampChanged(val timestampMillis: Long) : TrackEvent
    data object BackdateSaveConfirmed : TrackEvent
    data object BackdateCancelled : TrackEvent
    data class EditRequested(val entry: Entry) : TrackEvent
    data class EditValueChanged(val value: Int) : TrackEvent
    data class EditTimestampChanged(val timestampMillis: Long) : TrackEvent
    data object EditSaveConfirmed : TrackEvent
    data object EditCancelled : TrackEvent
    data class NoteRequested(val entry: Entry) : TrackEvent
    data class NoteTextChanged(val text: String) : TrackEvent
    data object NoteSaveConfirmed : TrackEvent
    data object NoteCancelled : TrackEvent
    data class DeleteRequested(val entry: Entry) : TrackEvent
    data object DeleteConfirmed : TrackEvent
    data object DeleteCancelled : TrackEvent
    data object ReadoutDismissed : TrackEvent
}

class TrackViewModel(
    private val entryDao: com.kieslingdev.mindscale.data.EntryDao,
    private val nowProvider: () -> Long = System::currentTimeMillis
) : androidx.lifecycle.ViewModel() {
    val uiState: kotlinx.coroutines.flow.StateFlow<TrackUiState>
    fun onEvent(event: TrackEvent)
}
```

### Composable entry points

```kotlin
package com.kieslingdev.mindscale.track

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TrackRoute(
    viewModel: TrackViewModel,
    modifier: Modifier = Modifier
)

@Composable
fun TrackScreen(
    uiState: TrackUiState,
    onEvent: (TrackEvent) -> Unit,
    modifier: Modifier = Modifier
)
```

`TrackScreen` is the stateless, previewable composable (drives all UI tests); `TrackRoute` collects `viewModel.uiState` and forwards `viewModel::onEvent`.

## Invariants

1. **No entry is ever required.** The app never displays a banner, badge, or prompt demanding the user log something. Silence = well.
2. **Empty state is a valid terminal state**, not a loading or error placeholder — it renders whenever `observeCount() == 0` and stays until the first Entry exists.
3. **Value range is 0..10 inclusive**, enforced at the UI (numpad only emits these values) and defensively at the ViewModel/DAO boundary (`band()` throws on out-of-range input; ViewModel never calls it or inserts outside 0..10).
4. **`Entry.id` is immutable** once created; `Entry.ts`, `value`, `note` are mutable via Edit/Note actions. `chips` is not mutable from any UI surface in this slice.
5. **`chips` is always `emptyList()`** from every code path this slice writes; the column and converter exist purely for Phase 2 forward-compatibility and must round-trip correctly even though nothing here produces non-empty values.
6. **No future timestamps.** Backdate and Edit dialogs reject a chosen timestamp greater than `nowProvider()`; Save is disabled and an inline error shown until corrected. An entry cannot be logged before it happens.
7. **Delete requires an explicit confirmation dialog** (D-1) — deletion is irreversible and this is mental-health-sensitive history; a single stray tap must not destroy it. The dialog has exactly one confirm + one cancel action, no extra friction (no "type DELETE").
8. **Tap and long-press are mutually exclusive** on every key — a long-press must never also fire an immediate insert.
9. **All Room access happens off the main thread** via `suspend` functions and `Flow`; the ViewModel never blocks the UI thread on persistence.
10. **No network calls, analytics, or telemetry** anywhere in this feature's code paths.
11. **No DI framework** is introduced; `TrackViewModel` takes `EntryDao` via constructor, wired manually at the call site (a ViewModel factory in the composable's host, not a generated graph).
12. **Numpad key order and grouping is frozen**: `[1,2,3,4,5,6,7,8,9]` as a 3×3 grid, then `0` and `10` in a separate, visually distinct group below — this ordering is a product decision from the mockup, not incidental layout.
13. **List ordering is frozen**: `ts DESC, id DESC`, limit 10 by default.
14. **Color is never the sole carrier of value information** — every value indicator is paired with the numeric value as text (accessibility; also protects colorblind users and non-color contexts).

## Android compatibility

- minSdk 26 means `java.time.*` (Instant, ZonedDateTime, DateTimeFormatter) is available natively — no `coreLibraryDesugaring` dependency needed for date/time handling in this feature.
- Room requires adding `androidx.room` runtime/ktx + KSP compiler-plugin dependencies and the KSP Gradle plugin; none currently exist in `gradle/libs.versions.toml` or `app/build.gradle.kts` (confirmed absent). Pin a specific Room version compatible with Kotlin 2.2.10/KSP at implementation time (not frozen here — see Open questions).
- No `androidx.lifecycle:lifecycle-viewmodel-compose` or `androidx.room` entries exist yet in the version catalog; only `lifecycle-runtime-ktx` is present. Both must be added.
- `MainActivity` currently calls `enableEdgeToEdge()` and hosts a bare `Scaffold`; `TrackRoute` must be composed inside that existing `Scaffold`'s content slot and respect its `innerPadding`, not introduce a second Scaffold.
- Rotation/process death: dialog-in-progress state (backdate timestamp being edited, note text being typed) is preserved via `rememberSaveable` (or an equivalent `Saver`) at the Composable layer so a config change or low-memory process death doesn't silently discard partially-entered data the user was actively typing. Persisted Entries themselves survive any process death by construction (Room is the source of truth, re-observed on recomposition).
- Compose BOM 2026.02.01 and the existing Material3 dependency already cover all UI surfaces needed (grid, dialogs, list); no new UI library required.
- No accessibility services/APIs beyond standard Compose semantics (`contentDescription`, `Role`) are needed; do not hardcode a custom long-press duration that could conflict with a user's configured `ViewConfiguration.longPressTimeoutMillis` (accessibility settings can extend this).

## Acceptance criteria

- [ ] `band(v)` returns "ended"/"mild"/"moderate"/"severe"/"critical" for the exact boundaries 0,1,3,4,6,7,9,10 and throws `IllegalArgumentException` for -1 and 11. — UNIT
- [ ] `ChipsConverter` round-trips `emptyList()`, a single chip, and multiple chips without data loss. — UNIT
- [ ] `EntryDao.insert` + `observeRecent` returns inserted rows ordered `ts DESC, id DESC`, limited to the requested count. — INSTRUMENTED
- [ ] `EntryDao.update` changes `ts`/`value`/`note` on an existing row without creating a new `id`. — INSTRUMENTED
- [ ] `EntryDao.delete` removes exactly the targeted row and no others. — INSTRUMENTED
- [ ] `observeCount()` emits 0 on a fresh database and updates reactively after insert/delete. — INSTRUMENTED
- [ ] `TrackViewModel.onEvent(KeyTapped(v))` inserts an Entry at `nowProvider()` with that value and surfaces a `transientReadout` matching `band(v)`. — UNIT
- [ ] `TrackViewModel.onEvent(KeyLongPressed(v))` opens `backdateDialog` with that value and `timestampMillis = nowProvider()`, without inserting anything yet. — UNIT
- [ ] `BackdateSaveConfirmed` inserts at the edited timestamp; `BackdateCancelled` discards with no DAO call. — UNIT
- [ ] Setting a `BackdateTimestampChanged`/`EditTimestampChanged` value greater than `nowProvider()` sets a non-null `error` and does not allow save. — FAILURE
- [ ] `DeleteRequested` sets `pendingDelete` without calling `EntryDao.delete`; only `DeleteConfirmed` calls delete; `DeleteCancelled` clears `pendingDelete` with no DAO call. — UNIT
- [ ] `TrackScreen` renders exactly 12 numpad keys in the order `1,2,3,4,5,6,7,8,9,0,10` with `0`/`10` visually separated from the 3×3 grid. — INSTRUMENTED
- [ ] Tapping a numpad key in `TrackScreen` invokes `onEvent(KeyTapped(...))` exactly once; long-pressing invokes `onEvent(KeyLongPressed(...))` and never also fires `KeyTapped`. — INSTRUMENTED
- [ ] With `recentEntries.isEmpty()` and `isEmpty = true`, `TrackScreen` renders the empty-state copy and no sample-data affordance. — INSTRUMENTED
- [ ] Each entry row exposes an accessible label combining the numeric value and band text (not color alone), and Edit/Note/Delete actions each have a distinct `contentDescription`. — UI-ACCESSIBILITY
- [ ] Requesting Delete opens a confirmation dialog; dismissing/cancelling it leaves the entry list unchanged; confirming removes exactly that row. — INSTRUMENTED
- [ ] Color ramp for the value indicator is monotonically increasing in perceived intensity from 0→10 and has a distinct dark-theme variant (visually verified). — MANUAL
- [ ] `.\gradlew.bat test` passes with the new unit tests included. — LINT-BUILD
- [ ] `.\gradlew.bat lint` passes with no new warnings introduced by this feature's code. — LINT-BUILD
- [ ] `.\gradlew.bat assembleDebug` succeeds. — LINT-BUILD
- [ ] On the `MindScale_API_36` emulator: launch app, tap several keys, confirm readout appears/disappears, long-press to backdate, edit/note/delete an entry, rotate the device mid-edit and confirm in-progress text survives. — MANUAL

## Task decomposition

1. Add Room (`androidx.room:room-runtime`, `room-ktx`, `room-compiler` via KSP) and `androidx.lifecycle:lifecycle-viewmodel-compose` to `gradle/libs.versions.toml` and `app/build.gradle.kts`; apply the KSP plugin. — LINT-BUILD
2. Implement `Entry`, `ChipsConverter`, `EntryDao`, `MindScaleDatabase` in `com.kieslingdev.mindscale.data`; configure `exportSchema = true` with a committed schema directory (`app/schemas`). — LINT-BUILD, UNIT (converter)
3. Write `band()` in `com.kieslingdev.mindscale.track` plus its boundary-value unit tests. — UNIT
4. Write instrumented DAO tests (insert/update/delete/observeRecent/observeCount) using an in-memory Room database. — INSTRUMENTED
5. Implement `TrackUiState`, `TrackEvent`, `TrackViewModel` (direct `EntryDao` injection, no repository layer — D-3) with unit tests covering tap-insert, long-press-open-dialog, backdate save/cancel, future-timestamp rejection, edit save/cancel, note save/cancel, delete-request/confirm/cancel, and readout auto-dismiss timing. — UNIT
6. Implement `TrackScreen` composable: numpad grid + 0/10 group, recent list with color-ramp indicator, empty state, backdate/edit/note dialogs, delete-confirmation dialog; wire `rememberSaveable` for in-progress dialog fields. — LINT-BUILD, INSTRUMENTED
7. Implement the color ramp (light/dark aware) in `ui/theme`, keyed off value 0..10. — LINT-BUILD, MANUAL
8. Add accessibility semantics (`contentDescription` on row actions, combined value+band label) to `TrackScreen`. — UI-ACCESSIBILITY
9. Implement `TrackRoute`, and wire it into `MainActivity`'s existing `Scaffold` content slot with a manually-constructed `MindScaleDatabase`/`EntryDao`/`TrackViewModel` factory (no DI framework). — LINT-BUILD, MANUAL
10. Full oracle pass: `.\gradlew.bat test`, `.\gradlew.bat lint`, `.\gradlew.bat assembleDebug`, then manual emulator walkthrough per the last acceptance criterion. — LINT-BUILD, MANUAL

## Rollout, migration, and rollback

- This is a new database (`version = 1`); there is no prior schema to migrate from. `exportSchema = true` with a committed `app/schemas/` directory is required now so that Phase 2's addition of Sleep/Marker/Breath entities and any future non-empty use of `chips` has a documented baseline schema to diff against (avoids an undocumented-migration failure path later).
- No feature flag / staged rollout mechanism exists in this repo and none is introduced — the Track screen is simply what `MainActivity` shows once implemented. There is exactly one build target; "rollout" is "merge and ship the next build."
- Rollback is source-control revert. The local Room database is per-device and not schema-versioned against a server, so reverting the app code without a matching downgrade migration will crash on next launch if a later phase has already shipped a higher schema version; since this spec establishes version 1 as the first version, no rollback migration is needed yet — record this constraint for whoever writes the Phase 2 migration.
- No data export/import exists in this slice; uninstalling the app or clearing app data deletes all Entries. This is an accepted risk for a local-only pre-release app and is not a per-feature concern to solve here.

## Open questions and approval gates

No blocking gates remain; the following decisions were made by this pass and are recorded rather than left open, per CLAUDE.md's routing guidance:

- **D-1 (delete confirmation dialog): IN, decided.** The mockup has no confirmation, but this is mental-health history the user may regret losing to a mis-tap; deletion is irreversible and there's no undo/trash. A single-tap-to-cancel, single-tap-to-confirm dialog costs one extra tap on an already-rare action (delete) — it does not violate axiom 2 ("costs nothing on a good day"), which is about *logging* friction, not deletion safety.
- **D-2 (sample-data loading): OUT of scope for this slice, deferred.** Seeding demo entries into a store that's also the source of a symptom history creates a data-integrity risk (a user could mistake demo rows for real history, or a bug could leak demo rows into "real" recent-entries queries) disproportionate to what Phase 1 needs to prove. The empty-state copy ships without the "look around with sample data" affordance; revisit once there's a clean way to mark/strip demo rows (e.g., a `isDemo` flag) — that's a Phase 2+ call, not this one.
- **D-3 (no repository layer):** decided — `TrackViewModel` depends on `EntryDao` directly. One entity, one DAO, no DI framework to abstract behind; adding a repository now is speculative layering with nothing to swap it for yet.
- **D-4 (long-press threshold):** decided — use the platform default (`ViewConfiguration.longPressTimeoutMillis`) via Compose's standard long-press gesture handling rather than a hardcoded ~460ms timer, so users with an accessibility-extended long-press timeout aren't fought by the app.
- **D-5 (future-timestamp rejection):** decided — Backdate and Edit reject any timestamp after `nowProvider()`. An event cannot be logged before it happens; this wasn't specified by the mockup but is a direct consequence of `ts` representing "when this happened."
- **D-6 (chips forward-compat): IN, decided.** `chips: List<String>` is part of the `Entry` entity now, persisted via a dedicated `ChipsConverter` (delimiter-joined string using ``, chosen to avoid a new serialization dependency), always empty from this slice's UI. This avoids a Phase 2 schema migration for a column whose shape is already fully knowable.
- **Not yet decided, non-blocking:** the exact Room library patch version (pin the latest stable release compatible with Kotlin 2.2.10/KSP at implementation time — task 1) and the exact color-ramp hex values (UI-polish, task 7) are intentionally left to the implementer, per this spec's own framing of those as non-frozen interface details.
