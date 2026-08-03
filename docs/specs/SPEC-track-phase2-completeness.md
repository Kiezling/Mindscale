SPEC-track-phase2-completeness: Track screen completeness (sleep capture, onset chips, marker input, help card, paused/check-in banners)

Status: IMPLEMENTED

Owner: TBD

Date: 2026-08-03

Last verified commit: implemented and oracle-verified 2026-08-03 (test/lint/assembleDebug/connectedDebugAndroidTest all green on `MindScale_API_36`; 38/38 instrumented tests, all unit tests, lint clean, debug APK assembled); a critical-path `architect-reviewer` pass found and fixed two real bugs before sign-off (see "Architect review findings" below); working tree not yet committed as of this verification — see the commit that introduces this spec for the exact hash once committed.

## Architect review findings (fixed before sign-off)

A critical-path `architect-reviewer` pass on this slice — covering migration safety, the transactional sleep-capture design, frozen-interface fidelity, and onset-chip data integrity — found and got fixed two real bugs, plus one non-blocking deviation, before sign-off:

1. **Stale full-row overwrite risk in the onset-chip Submit flow.** `OnsetChipPromptState` is frozen to `entryId: Long` only (no full `Entry` snapshot), and the first implementation worked around that by caching a full `Entry` copy in a private ViewModel field at prompt-open time, then writing `entry.copy(chips = ...)` back via `EntryDao.update()` on Submit. Because the Recent list's Edit/Note actions remain interactive while the onset-chip card is open, a user could edit or note that same entry and then Submit the chip prompt, silently reverting the just-made edit. **Fix:** added a new, narrowly-scoped `EntryDao.updateChips(entryId, chips)` query (`UPDATE entries SET chips = :chips WHERE id = :entryId`) that patches only the `chips` column, eliminating the stale-snapshot mechanism entirely — the private cached-`Entry` field was removed. Verified live during the manual walkthrough: a second capture happened while the onset-chip prompt for an earlier entry was still open, and Submit correctly patched only the original entry's chips.
2. **`markerDraft` did not survive true process recreation.** The first review fix added a `rememberSaveable(uiState.markerOpen)` text buffer, but an independent Codex review before commit found that this alone was insufficient: a recreated `TrackViewModel` reset `markerOpen=false`, so the buffered composable was not restored. **Final fix:** `TrackViewModel` now mirrors both `markerOpen` and `markerDraft` into a platform-backed `SavedStateHandle`, while `MarkerSection` retains `rememberSaveable` as its Compose text-field buffer. A focused unit test recreates the ViewModel from the same saved state and verifies that the marker reopens with its draft intact.
3. **Non-blocking:** `OnsetChipsSubmitted` originally skipped the DAO call when the chip selection was empty, deviating from the frozen "Submit ... calls `EntryDao.update` with exactly those chips" behavior. Fixed to call `updateChips` unconditionally (now trivially safe since it is a targeted patch, not a full-row write).

## Purpose

Extend the Phase 1 Track screen (numpad logging + recent list, `SPEC-track-numpad-logging.md`, IMPLEMENTED) with the five remaining Track-screen elements from the approved 8-phase plan: Sleep/Wake capture, onset chips, a free-text event marker, an in-place "what the numbers mean" help card, and the paused/check-in safety banners. This is still a data-entry and recall instrument only — it adds capture surfaces and their storage, not interpretation, statistics, or a Settings screen.

## Non-goals

- No Settings screen UI (Phase 4). This spec adds the minimal persisted settings needed to make its own features work (see D-1), but no screen exists yet to change them by hand.
- No behavioral-anchor ("a 2 is…", "a 5 is…", "an 8 is…") capture, prompt, or readout. That is a separate, later SPEC.md feature ("DRIFT FIX") not named in this phase's scope — tracked in `docs/specs/BACKLOG.md`.
- No paced-breathing button or exercise (Phase 7, "Report/breathing" per `PROJECT_STATE.md`'s phase list).
- No Full log screen rendering of Marker or Sleep rows (Phase 3). This spec only writes `Marker`/`SleepInterval` rows; nothing outside the Track screen reads them yet.
- No time-weighted/hold/auto-end episode model, AUC, or onset-to-onset statistics (SPEC.md's "Statistics" section). This spec stores raw `SleepInterval`s; consuming them for duration arithmetic is Insights-phase (later) work.
- No "Export or delete" action from the paused banner (see D-6) — that needs Settings-screen data actions (Phase 4).
- No custom/editable onset-chip vocabulary. The ten-chip default list is frozen in code this phase (see Frozen interfaces); making it user-editable is a Phase 4 Settings concern.
- No change to Phase 1's numpad long-press timing (`ViewConfiguration.longPressTimeoutMillis`, per Phase 1's D-4) — unrelated to this phase, left as-is.

## User experience

All additions live on the existing single Track screen, in the order shown in the mockup (`MindScale v2.dc.html`, Track screen block):

1. **Paused banner** (top of screen, replaces the numpad/help/sleep/marker/checkin block entirely — see Invariant 21): shown whenever tracking is paused. Copy: "Tracking paused" / "Your data is still here and still yours. Nothing is being recorded until you start again." with a single "Start again" action. The Recent-entries list (and empty state) remain visible underneath — pausing hides only the *capture* surface, never the user's own history.
2. **Help card**: a small circular "?" button next to the intensity readout area. Tapping toggles a card with three fixed paragraphs (frozen copy, below). Toggling again, or successfully recording a new entry, closes it.
3. **Numpad**: unchanged from Phase 1.
4. **Backdate dialog** ("Something earlier"): unchanged from Phase 1, except its Save path now also runs onset-chip detection and sleep/wake capture (see Invariants 15–20), matching the mockup's single `record()` code path for both immediate taps and backdated saves.
5. **Onset chip prompt**: after a tap or backdate-save that is a genuine onset (see Invariant 15 / D-3 for the exact rule) — and only when the `askChips` setting is on and the capture wasn't a Sleep/Wake tap — a card appears: "What was happening?" with the ten default chips as toggleable pills, and Submit / Skip actions. Skip discards with no write. Submit attaches the selected chips to the just-created entry (a separate update, matching the mockup — the entry itself was already inserted at tap time).
6. **Sleep / Wake buttons**: shown side-by-side when `sleepOn` is true (default). Tapping "Sleep" arms the next numpad key/backdate-save to also open a sleep interval; tapping "Wake" arms the next capture to close the currently-open interval. The pad prompt above the readout changes to "How intense as you fall asleep?" / "How intense on waking?" while armed. The very first time a user arms either button (ever, on this device — tracked by the persisted `TrackSettings.sleepIntroShown`, not in-memory state, so it survives app and device restarts, see Invariant 27), a one-time explanatory toast is shown instead of the usual arm toast: "Marks time asleep — nothing is counted while you sleep."
7. **Marker control**: a single small text link, "Mark an event" (relabeled "Event" while its input is open). Opens a single-line text field (placeholder: "Dose change, started therapy, travel…") with Save/Cancel. Always stamped at the current wall-clock time — no backdating for markers this phase (see D-5).
8. **Check-in banner**: shown at most once every 60 days, and only once the user has logged at least 40 entries total (frozen thresholds, see Invariant 21 and SPEC.md's Safety section). Copy: "A question, once in a while" / "Is keeping this record still useful to you? For some people, watching symptoms closely makes them louder. If that's happening, stopping is a reasonable thing to do." with "Still useful" (resets the 60-day cooldown, does not pause) and "Pause tracking" (resets the cooldown and pauses in one step) actions.
9. **Toast feedback**: a small transient message surface (auto-dismisses ~2.2s, a new toast replaces any showing one) reports the outcomes of Sleep/Wake taps and marker saves — "Asleep at 7", "Slept 6h 20m", "Already asleep since 11:40 PM", "No sleep was open", "Event marked" — matching the mockup's `showToast` copy exactly.
10. **Recent-entries list** (Phase 1, extended): each row now also shows a small pill badge for entries recorded while armed ("asleep" for `kind=SLEEP`, "awake" for `kind=WAKE`) or when the value is 0 ("ended"), and — when present — the entry's chips joined with " · ". The existing inline Edit expansion gains the same chip-toggle row so chips are editable after the fact, not just at onset.

## Frozen interfaces and data contracts

### `Entry` (amends the Phase 1 entity — adds one nullable column)

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ts: Long,
    val value: Int,
    val chips: List<String> = emptyList(),
    val note: String? = null,
    val kind: EntryKind? = null   // NEW — null for an ordinary tap; SLEEP/WAKE when recorded while armed
)

enum class EntryKind { SLEEP, WAKE }
```

### `EntryKindConverter` (new, parallel to Phase 1's `ChipsConverter`)

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.TypeConverter

class EntryKindConverter {
    @TypeConverter
    fun fromKind(kind: EntryKind?): String? = kind?.name

    @TypeConverter
    fun toKind(raw: String?): EntryKind? = raw?.let { EntryKind.valueOf(it) }
}
```

### `EntryDao` amendment (new queries — onset detection and chips-only patch)

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.Dao
import androidx.room.Query

// added to Phase 1's existing EntryDao interface, alongside insert/update/delete/observeRecent/observeCount
@Dao
interface EntryDao {
    // ...Phase 1 members unchanged...

    @Query("SELECT * FROM entries WHERE ts <= :ts ORDER BY ts DESC, id DESC LIMIT 1")
    suspend fun mostRecentAtOrBefore(ts: Long): Entry?

    // Added during implementation (architect-review finding, see "Architect review
    // findings" above) - a chips-only targeted patch so the onset-chip Submit flow never
    // risks a stale full-row overwrite of an entry the Recent list's Edit/Note actions
    // may have changed while the prompt was open. OnsetChipPromptState stays frozen to
    // entryId: Long only; this query is what makes that shape safe to implement.
    @Query("UPDATE entries SET chips = :chips WHERE id = :entryId")
    suspend fun updateChips(entryId: Long, chips: List<String>)
}
```

This is the only query onset detection may use (D-3). It deliberately does **not** read from `EntryDao.observeRecent(10)`/the ViewModel's `recentEntries` — that list is truncated to the newest 10 and is wall-clock-recent, not "recent relative to an arbitrary (possibly backdated) capture timestamp." A backdated capture into the middle of existing history must see only entries at-or-before its own `ts`; entries chronologically after it (including ones already in `recentEntries`) are irrelevant to whether *this* capture is an onset. Ties (an existing entry at the exact same `ts`) break by `id DESC`, matching Phase 1's own `observeRecent` ordering convention, so the rule is fully deterministic.

### `SleepInterval` entity + DAO (new)

`start`/`end` are named `startTs`/`endTs` specifically to avoid `end` being a bare SQL keyword-adjacent column name inside hand-written `@Query` strings and future raw-SQL migrations — `entries.ts` already sets the repo's naming convention (a `...Ts` suffix for epoch-millis columns); this brings `SleepInterval` in line with it.

The "at most one open interval" and "close exactly the right interval" guarantees (see Invariant 19) are owned by this DAO, not by the ViewModel. `captureSleep`/`captureWake` are `@Transaction` methods: Room wraps each one in a single SQLite transaction, and SQLite serializes writers against the same database — so two concurrent calls to `captureSleep` (or two concurrent calls mixing `captureSleep`/`captureWake`) each execute their internal "read `openInterval()`, then write" sequence atomically with respect to each other. The second call to run always observes the first call's completed effect, never a half-applied one. `TrackViewModel.armedCapture` (an in-memory, per-instance field) is only ever a UI convenience for hiding/relabeling buttons — it must never be treated as the source of the "at most one open interval" guarantee, because a rapid double-tap can reach `onEvent` twice before either write completes, and `armedCapture` alone cannot rule that out.

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.*

@Entity(tableName = "sleeps")
data class SleepInterval(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startTs: Long,
    val endTs: Long? = null   // null = still open (asleep)
)

sealed interface SleepCaptureOutcome {
    data object Opened : SleepCaptureOutcome
    data class AlreadyOpen(val since: Long) : SleepCaptureOutcome
    data class Closed(val since: Long, val until: Long) : SleepCaptureOutcome
    data object NothingOpen : SleepCaptureOutcome
}

@Dao
interface SleepDao {
    // Low-level CRUD — used directly by DAO-level tests; not used by TrackViewModel for
    // the sleep/wake capture path (that goes through captureSleep/captureWake below).
    @Insert
    suspend fun insert(interval: SleepInterval): Long

    @Update
    suspend fun update(interval: SleepInterval)

    @Query("SELECT * FROM sleeps WHERE endTs IS NULL ORDER BY startTs DESC LIMIT 1")
    suspend fun openInterval(): SleepInterval?

    // Atomic capture operations — the sole writers TrackViewModel calls for Sleep/Wake taps.
    @Transaction
    suspend fun captureSleep(atTs: Long): SleepCaptureOutcome {
        val open = openInterval()
        if (open != null) return SleepCaptureOutcome.AlreadyOpen(open.startTs)
        insert(SleepInterval(startTs = atTs, endTs = null))
        return SleepCaptureOutcome.Opened
    }

    @Transaction
    suspend fun captureWake(atTs: Long): SleepCaptureOutcome {
        val open = openInterval() ?: return SleepCaptureOutcome.NothingOpen
        val resolvedEnd = maxOf(atTs, open.startTs + 60_000L)
        update(open.copy(endTs = resolvedEnd))
        return SleepCaptureOutcome.Closed(open.startTs, resolvedEnd)
    }
}
```

### `Marker` entity + DAO (new)

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.*

@Entity(tableName = "markers")
data class Marker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ts: Long,
    val text: String
)

@Dao
interface MarkerDao {
    @Insert
    suspend fun insert(marker: Marker): Long
}
```

### `TrackSettings` entity + DAO (new — see D-1 for why this exists now)

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "track_settings")
data class TrackSettings(
    @PrimaryKey val id: Int = 0,
    val sleepOn: Boolean = true,
    val askChips: Boolean = false,
    val paused: Boolean = false,
    val checkinAt: Long = 0L,
    val sleepIntroShown: Boolean = false   // NEW — persisted so the one-time arm toast survives restarts
)

@Dao
interface TrackSettingsDao {
    // Reads/writes exclusively target the canonical row, id = 0. Room's @PrimaryKey only
    // enforces uniqueness of whatever id a row has; it does not stop something from
    // inserting a row with a different id. Nothing in this app ever does — the seed
    // callback below inserts id = 0 once, and update() always writes back a TrackSettings
    // whose id is still 0 (copy() never touches id) — but that is an app-level guarantee,
    // not a schema-enforced one. If a stray row with another id ever existed, this query's
    // WHERE id = 0 clause would still return the canonical row deterministically.
    @Query("SELECT * FROM track_settings WHERE id = 0")
    fun observe(): Flow<TrackSettings>   // non-null: the id = 0 row is seeded before first use, see below

    @Update
    suspend fun update(settings: TrackSettings)
}
```

The canonical row (`TrackSettings()`, i.e. `id = 0`) is inserted by a `RoomDatabase.Callback.onCreate` on both the production and any in-memory test database, so `TrackSettingsDao.observe()` never emits null for the `id = 0` row and no "seed on first collect" race exists in the ViewModel.

### `MindScaleDatabase` (amends Phase 1 — version bump, new entities/converters)

```kotlin
package com.kieslingdev.mindscale.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Entry::class, SleepInterval::class, Marker::class, TrackSettings::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(ChipsConverter::class, EntryKindConverter::class)
abstract class MindScaleDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun sleepDao(): SleepDao
    abstract fun markerDao(): MarkerDao
    abstract fun trackSettingsDao(): TrackSettingsDao
}
```

### `TrackUiState`, `TrackEvent` (amends Phase 1 — new fields/events only; nothing removed)

```kotlin
package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.SleepInterval

data class TrackUiState(
    // -- Phase 1, unchanged --
    val recentEntries: List<Entry> = emptyList(),
    val isEmpty: Boolean = true,
    val transientReadout: ReadoutState? = null,
    val backdateDialog: BackdateDialogState? = null,
    val editDialog: EditEntryState? = null,
    val noteDialog: NoteEditState? = null,
    val pendingDelete: Entry? = null,
    // -- Phase 2, new --
    val helpOpen: Boolean = false,
    val onsetChipPrompt: OnsetChipPromptState? = null,
    val sleepOn: Boolean = true,
    val armedCapture: EntryKind? = null,          // null | SLEEP | WAKE — mirrors Entry.kind
    val openSleepInterval: SleepInterval? = null,
    val markerOpen: Boolean = false,
    val markerDraft: String = "",
    val isPaused: Boolean = false,
    val showCheckin: Boolean = false,
    val toast: String? = null
)

data class OnsetChipPromptState(val entryId: Long, val selected: Set<String> = emptySet())

// EditEntryState amended — adds editable chips, seeded from originalEntry.chips at dialog-open time
data class EditEntryState(
    val originalEntry: Entry,
    val value: Int,
    val timestampMillis: Long,
    val chips: Set<String> = emptySet(),
    val error: String? = null
) {
    val entryId: Long get() = originalEntry.id
}

sealed interface TrackEvent {
    // -- Phase 1, unchanged --
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
    // -- Phase 2, new --
    data class EditChipToggled(val chip: String) : TrackEvent
    data object ToggleHelp : TrackEvent
    data object ArmSleep : TrackEvent
    data object ArmWake : TrackEvent
    data class OnsetChipToggled(val chip: String) : TrackEvent
    data object OnsetChipsSubmitted : TrackEvent
    data object OnsetChipsSkipped : TrackEvent
    data object MarkerToggled : TrackEvent
    data class MarkerDraftChanged(val text: String) : TrackEvent
    data object MarkerSaveConfirmed : TrackEvent
    data object MarkerCancelled : TrackEvent
    data object CheckinStillUseful : TrackEvent
    data object CheckinPauseRequested : TrackEvent
    data object ResumeTracking : TrackEvent
    data object ToastDismissed : TrackEvent
}

class TrackViewModel(
    private val entryDao: com.kieslingdev.mindscale.data.EntryDao,
    private val sleepDao: com.kieslingdev.mindscale.data.SleepDao,
    private val markerDao: com.kieslingdev.mindscale.data.MarkerDao,
    private val settingsDao: com.kieslingdev.mindscale.data.TrackSettingsDao,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle = androidx.lifecycle.SavedStateHandle(),
    private val nowProvider: () -> Long = System::currentTimeMillis
) : androidx.lifecycle.ViewModel() {
    val uiState: kotlinx.coroutines.flow.StateFlow<TrackUiState>
    fun onEvent(event: TrackEvent)
}
```

`TrackRoute`/`TrackScreen` composable signatures are unchanged from Phase 1 (same two functions, same parameters) — only `TrackUiState`/`TrackEvent` grow.

### Frozen default onset-chip vocabulary

```kotlin
val DEFAULT_ONSET_CHIPS = listOf(
    "flat", "agitated", "hopeless", "numb", "wired", "foggy", "alone", "driving", "work", "poor sleep"
)
```

### Frozen help-card copy (exact strings, three paragraphs)

1. "0 means it isn't happening right now. 1–3 you notice it. 4–6 it's changing what you do. 7–10 it's most of what's happening."
2. "The numbers only have to mean the same thing to you each time. That's what makes the chart readable months later."
3. "Log when it starts, when it clearly changes, and when it stops. Nothing recorded means nothing was happening."

### Frozen paused-banner copy

- Eyebrow: "Tracking paused"
- Body: "Your data is still here and still yours. Nothing is being recorded until you start again."
- Action: "Start again"

### Frozen check-in banner copy

- Eyebrow: "A question, once in a while"
- Body: "Is keeping this record still useful to you? For some people, watching symptoms closely makes them louder. If that's happening, stopping is a reasonable thing to do."
- Actions: "Still useful", "Pause tracking"

### Frozen toast copy templates

- Sleep-arm intro (first time ever): "Marks time asleep — nothing is counted while you sleep"
- Sleep armed (subsequent times): "Now tap how you felt going to sleep"
- Wake armed (subsequent times): "Now tap how you feel waking up"
- Sleep recorded: "Asleep at {value}"
- Sleep recorded while already open: "Already asleep since {formatted start time}"
- Wake recorded: "Slept {formatted duration}"
- Wake recorded with nothing open: "No sleep was open"
- Marker saved: "Event marked"

## Invariants

Continues Phase 1's numbering (1–14 unchanged, see `SPEC-track-numpad-logging.md`).

15. **Onset detection is relative to the capture's own timestamp, not wall-clock "now."** For a capture (immediate tap or backdated save) at timestamp `ts` with value `v > 0`: `isOnset := (EntryDao.mostRecentAtOrBefore(ts) == null) || (EntryDao.mostRecentAtOrBefore(ts)!!.value == 0)`. The query (D-3) only considers entries with `entry.ts <= ts`; entries with a later `ts` — including ones that already exist in the table and are chronologically newer by insertion or by wall-clock time — never affect this determination. A backdated capture inserted "in the past" relative to entries that already exist with a later `ts` is classified using only what preceded it at that point in the timeline, exactly as if it had been logged in real time at that moment.
16. **Onset-chip prompt gating**: the prompt opens after a `KeyTapped`/`BackdateSaveConfirmed` capture if and only if `settings.askChips == true` AND the capture is a genuine onset (Invariant 15) AND `armedCapture == null` at the moment of capture. It never opens for a Sleep/Wake-armed capture.
17. **Sleep/Wake arming is exclusive and single-use, but is a UI convenience only — not a correctness mechanism.** `ArmSleep`/`ArmWake` sets `armedCapture` to that kind unless it is already that kind, in which case it disarms (toggle). The next successful `KeyTapped` or `BackdateSaveConfirmed` consumes and clears `armedCapture`, stamping `Entry.kind` accordingly and calling `SleepDao.captureSleep`/`captureWake` (18–20). Any other event (edit, note, delete, dialog open) leaves `armedCapture` untouched. Because `armedCapture` lives only in ViewModel memory, two events processed close enough together may both read it as set before either clears it; this must never be able to produce two open intervals or an inconsistent close — that guarantee comes from `SleepDao`'s transactional methods (Invariant 19), not from `armedCapture` acting as a lock.
18. **The Entry is always recorded when a capture completes**, regardless of whether the sleep-interval side effect succeeds — e.g. tapping Wake with no open interval still inserts `Entry(kind = WAKE)`; only the sleep bookkeeping is a no-op (communicated via toast, Invariant 25).
19. **"At most one open `SleepInterval` (`endTs == null`) may exist at a time" and "Wake closes exactly the interval that was open when it ran" are guaranteed by `SleepDao.captureSleep`/`captureWake` being `@Transaction` methods (see the `SleepDao` frozen interface), not by any ViewModel-level check.** Each method's internal "read `openInterval()`, then write" sequence runs as one atomic SQLite transaction; concurrent callers are serialized by Room/SQLite so the second call to actually execute always observes the first call's already-applied effect. Recording a Sleep capture while one is already open does not create a second one — `captureSleep` returns `AlreadyOpen`, leaving the existing interval untouched. This must hold even under concurrent/rapid-repeated calls, not just sequential ones (see the concurrency acceptance criterion).
20. **Waking sets `endTs = max(recordedTs, openInterval.startTs + 60_000)`** — a minimum one-minute interval, so a fast Sleep→Wake mis-tap sequence cannot produce a negative or zero-length interval.
21. **Paused hides the capture surface, not the history.** While `settings.paused == true`: the numpad, help card, onset-chip prompt, Sleep/Wake buttons, marker control, and check-in banner are all hidden; only the paused banner is shown in their place. The Recent-entries list and empty state are unaffected and remain visible.
22. **Check-in banner visibility** is exactly `!settings.paused && totalEntryCount >= 40 && (now - settings.checkinAt) > 60.days`, where `totalEntryCount` is `EntryDao.observeCount()` (Phase 1's existing `SELECT COUNT(*) FROM entries`, unmodified — no new count query is introduced). Because the default `checkinAt` is `0L` (epoch), `now - 0 > 60.days` is true for any real device clock, so a fresh install's first check-in becomes eligible the moment `totalEntryCount` reaches 40 — no separate "first time" branch exists or is needed. `CheckinStillUseful` sets `checkinAt = now` only. `CheckinPauseRequested` sets `checkinAt = now` and `paused = true` in one settings update.
23. **Marker save trims whitespace**; an empty/whitespace-only draft on `MarkerSaveConfirmed` closes the input with no `Marker` row created and no toast.
24. **Help card auto-closes on a successful capture.** Any `KeyTapped` or `BackdateSaveConfirmed` that records an entry sets `helpOpen = false` as a side effect, matching the mockup; `ToggleHelp` is otherwise independent of every other dialog's open/closed state.
25. **Toasts auto-dismiss after 2.2 seconds**; setting a new toast while one is showing replaces it and resets the dismiss timer (no queueing).
26. **The `id = 0` row in `TrackSettings` is the canonical settings row**, seeded once by a Room creation callback; `TrackSettingsDao.observe()`'s `WHERE id = 0` clause returns it deterministically. The schema does not itself forbid a row existing with a different id (Room's `@PrimaryKey` only enforces per-row uniqueness, not "id must be 0") — no code path in this app ever writes one, but this is an app-level guarantee, not a schema-enforced one (see D-9).
27. **`sleepIntroShown` is read from and written to the persisted `id = 0` `TrackSettings` row, never from in-memory ViewModel state.** The very first `ArmSleep` or `ArmWake` for which `settings.sleepIntroShown == false` sets the one-time intro toast and updates `sleepIntroShown = true` in the same settings write; every subsequent arm (including after a process restart, app reinstall-free relaunch, or device reboot, as long as app data persists) reads `sleepIntroShown == true` and shows the normal arm toast instead. A fresh `TrackViewModel` instance backed by the same database must reproduce this without needing any in-memory carryover.
28. **`chips` on `Entry` is now mutable outside of creation** via the onset-chip Submit action (through the targeted `EntryDao.updateChips`) and via inline chip editing in the Recent-list Edit expansion (through the full-row `EntryDao.update`, since Edit also carries a possibly-changed value/timestamp); this narrows Phase 1's Invariant 4 ("chips is not mutable from any UI surface in this slice") — that Phase 1 constraint is superseded by this spec, everything else in Phase 1's invariant list is unchanged.

## Android compatibility

- Room migration required: `version 1 → 2`. New tables `sleeps` (`id`, `startTs`, `endTs` nullable), `markers` (`id`, `ts`, `text`), `track_settings` (`id`, `sleepOn`, `askChips`, `paused`, `checkinAt`, `sleepIntroShown`); `entries` gets one new nullable `TEXT` column, `kind`, defaulting to `NULL`. Write an explicit `Migration(1, 2)` (no `fallbackToDestructiveMigration`), consistent with Phase 1's own stated rationale for `exportSchema = true` + a committed `app/schemas/` baseline. Commit the regenerated `app/schemas/.../2.json` alongside the migration.
- `SleepDao.captureSleep`/`captureWake` are `@Transaction`-annotated default (non-abstract) methods on a Kotlin `@Dao` interface, calling only other suspend methods declared on that same interface (`openInterval`/`insert`/`update`) — this is a supported Room pattern (Room generates the transaction wrapper around the default method body) and requires no abstract base class.
- The `RoomDatabase.Callback.onCreate` that seeds the canonical `id = 0` `TrackSettings` row must be attached to both the production `Room.databaseBuilder(...)` call site and any `Room.inMemoryDatabaseBuilder(...)` used in instrumented tests, or DAO tests against a fresh in-memory DB will see zero rows.
- No new Gradle dependencies beyond what Phase 1 already added (Room, KSP, lifecycle-viewmodel-compose); no coreLibraryDesugaring needed (Phase 1's `java.time.*` availability reasoning still applies for the sleep-duration formatting).
- The paused banner, check-in banner, help card, onset-chip card, and marker input are all conditionally-rendered blocks inside the same `TrackRoute`/`TrackScreen` composables Phase 1 already wired into `MainActivity`'s `Scaffold` — no new navigation destination, no second `Scaffold`.
- In-progress marker UI (`markerOpen` and `markerDraft`) must survive rotation and true process recreation. `MarkerSection` uses `rememberSaveable` for the Compose text-field buffer, and `TrackViewModel` mirrors both values into `SavedStateHandle` so the conditional marker UI is recreated open with the same draft after the ViewModel itself is rebuilt.

## Acceptance criteria

- [x] `EntryKindConverter` round-trips `null`, `EntryKind.SLEEP`, and `EntryKind.WAKE` without data loss. — UNIT
- [x] `Migration(1, 2)` applied to a v1 database with existing `entries` rows preserves them (id/ts/value/chips/note intact, `kind` reads as null), creates empty `sleeps`/`markers` tables, and inserts the canonical `track_settings` row at `id=0` with the frozen defaults (`sleepOn=true, askChips=false, paused=false, checkinAt=0, sleepIntroShown=false`) via the migration's own seed `INSERT`, not the creation callback (which does not run on an upgrade). — INSTRUMENTED
- [x] `SleepDao.openInterval()` returns null on an empty table, and the most-recently-started row where `endTs IS NULL` otherwise. — INSTRUMENTED
- [x] `MarkerDao.insert` persists `ts`/`text` exactly as given. — INSTRUMENTED
- [x] `TrackSettingsDao.observe()` emits the `id=0` row immediately after database creation (creation callback ran), with the frozen defaults (`sleepOn=true, askChips=false, paused=false, checkinAt=0, sleepIntroShown=false`). — INSTRUMENTED
- [x] `EntryDao.mostRecentAtOrBefore(ts)` returns null when no entry has `ts' <= ts`; returns the single entry when exactly one qualifies; when two entries share the same `ts`, returns the one with the higher `id`. — UNIT
- [x] **Onset is relative to the capture timestamp, not to what already exists in the table (D-3).** Given entries at `T1` (`value=5`) and `T3` (`value=0`) with `T1 < T3`, a `BackdateSaveConfirmed` at `T2` where `T1 < T2 < T3` is classified using `mostRecentAtOrBefore(T2)` (which resolves to the `T1` entry, `value=5`) — so it is **not** an onset — even though `T3` (`value=0`) is the chronologically latest entry in the whole table and would make it look like an onset if the check incorrectly used "the latest entry overall" instead of "the latest entry at-or-before this capture's own timestamp." — UNIT
- [x] `KeyTapped(v>0)` with no prior entries, or with `mostRecentAtOrBefore(ts)!!.value == 0`, is treated as onset; with a nonzero result, it is not. — UNIT
- [x] With `askChips=true`, `armedCapture=null`, and an onset tap: `onsetChipPrompt` is set to that entry's id with an empty selection. With `askChips=false`, or `armedCapture != null`, or a non-onset tap: `onsetChipPrompt` stays null. — UNIT
- [x] `OnsetChipToggled` adds/removes a chip from `onsetChipPrompt.selected`; `OnsetChipsSubmitted` calls `EntryDao.updateChips` with exactly those chips on the target entry id and clears the prompt (unconditionally, even when the selection is empty); `OnsetChipsSkipped` clears the prompt with no DAO call. — UNIT
- [x] `ArmSleep` sets `armedCapture=SLEEP`; a second `ArmSleep` clears it back to null; `ArmWake` behaves the same for `WAKE`; arming one while the other is armed switches (not stacks). — UNIT
- [x] A `KeyTapped` while `armedCapture=SLEEP` and no open interval: inserts `Entry(kind=SLEEP)`, calls `SleepDao.captureSleep(ts)` which inserts a new open `SleepInterval(startTs=ts, endTs=null)` and returns `Opened`, clears `armedCapture`, and sets `toast` to the "Asleep at {value}" template. — UNIT
- [x] A `KeyTapped` while `armedCapture=SLEEP` and an interval is already open: inserts `Entry(kind=SLEEP)`, `captureSleep` returns `AlreadyOpen` and does not touch the open interval, and the ViewModel sets the "already asleep" toast. — UNIT
- [x] A `KeyTapped` while `armedCapture=WAKE` and an interval is open: inserts `Entry(kind=WAKE)`, `captureWake` updates that interval's `endTs` to `max(ts, startTs + 60_000)` and returns `Closed`, and the ViewModel sets the "Slept {duration}" toast. — UNIT
- [x] A `KeyTapped` while `armedCapture=WAKE` and no interval is open: inserts `Entry(kind=WAKE)`, `captureWake` returns `NothingOpen` with no row written, and the ViewModel sets the "No sleep was open" toast. — UNIT, FAILURE
- [x] **Concurrency: "at most one open interval" is enforced by the DAO transaction, not assumed.** Against a real (in-memory) Room database with no existing intervals, launching many (e.g. 20) concurrent coroutines that each call `SleepDao.captureSleep(sameTs)` results in exactly one `SleepInterval` row with `endTs IS NULL` after all complete, exactly one call's result is `Opened`, and every other call's result is `AlreadyOpen`. A parallel test for concurrent `captureWake` calls against a single open interval asserts exactly one `Closed` result, the rest `NothingOpen`, and the interval's final `endTs` is set exactly once (not overwritten to a different value by a losing racer). — INSTRUMENTED
- [x] The first `ArmSleep` or `ArmWake` for which `settings.sleepIntroShown == false` sets the one-time intro toast and persists `sleepIntroShown = true`; a subsequent arm (still `sleepIntroShown == true`) sets the normal arm toast. A **new `TrackViewModel` instance** constructed against the same (already-seeded) database after the first arm reads `sleepIntroShown == true` from disk and shows the normal arm toast on its very first arm — proving the flag is not carried in ViewModel memory. — UNIT
- [x] `MarkerDraftChanged` then `MarkerSaveConfirmed` with non-blank trimmed text inserts a `Marker` at `nowProvider()` and sets the "Event marked" toast; blank/whitespace-only text on `MarkerSaveConfirmed` closes the input with no `MarkerDao` call and no toast. — UNIT, FAILURE
- [x] Recreating `TrackViewModel` from its saved state while the marker input is open restores both `markerOpen=true` and the exact in-progress `markerDraft`; save/cancel clears both saved values. — UNIT, ANDROID-COMPATIBILITY
- [x] `CheckinStillUseful` updates only `checkinAt`; `CheckinPauseRequested` updates `checkinAt` and sets `paused=true` in the same settings write. — UNIT
- [x] `showCheckin` is true only when `!paused && totalEntryCount >= 40 && now - checkinAt > 60 days`, where `totalEntryCount` comes from `EntryDao.observeCount()`; false at exactly 39 entries, false the instant after a `checkinAt` reset, true again once both conditions are independently satisfied. With the settings row at its default (`checkinAt=0`), `showCheckin` becomes true the moment `totalEntryCount` reaches 40 — no separate "first ever check-in" code path exists. — UNIT
- [x] `ResumeTracking` sets `paused=false`. — UNIT
- [x] `ToggleHelp` flips `helpOpen`; a subsequent successful `KeyTapped`/`BackdateSaveConfirmed` resets `helpOpen` to false regardless of prior state. — UNIT
- [x] `EditRequested` seeds `EditEntryState.chips` from the target entry's current chips; `EditChipToggled` mutates the draft set; `EditSaveConfirmed` persists the final chip set via `EntryDao.update`. — UNIT
- [x] `TrackScreen` with `isPaused=true` renders the paused banner and hides the numpad, help button, sleep/wake buttons, marker control, and check-in banner, while still rendering a non-empty `recentEntries` list when present. — INSTRUMENTED
- [x] `TrackScreen` with `showCheckin=true` renders the check-in banner with both "Still useful" and "Pause tracking" actions wired to their respective `onEvent` calls. — INSTRUMENTED
- [x] Tapping the help "?" button toggles the help card open/closed in `TrackScreen`; its rendered text matches the three frozen paragraphs exactly. — INSTRUMENTED
- [x] Each Recent-list row with `kind != null` or `value == 0` renders an accessible badge ("asleep"/"awake"/"ended") that is not conveyed by color alone; a row with non-empty chips renders them as text. — UI-ACCESSIBILITY
- [x] Sleep/Wake buttons, the marker toggle, and onset-chip pills each expose a distinct `contentDescription` reflecting their current state (e.g. Wake's disabled-looking state when nothing is open still describes why, not just "Wake"). — UI-ACCESSIBILITY
- [x] `.\gradlew.bat test` passes with the new unit tests included. — LINT-BUILD
- [x] `.\gradlew.bat lint` passes with no new warnings. — LINT-BUILD
- [x] `.\gradlew.bat assembleDebug` succeeds. — LINT-BUILD
- [x] On the `MindScale_API_36` emulator, with a test build that flips `askChips=true`/`paused=true`/`checkinAt` via direct DAO calls (no Settings UI exists yet — see D-1/D-2): exercise Sleep→(numpad)→Wake→(numpad) end to end and confirm the toasts and Recent-list badges match; open and submit the onset-chip prompt; save and cancel a marker; toggle the help card; confirm the paused banner replaces the capture surface and "Start again" restores it; confirm the check-in banner's both actions behave as specified. — MANUAL. **Verified 2026-08-03**, via a temporary instrumented seed helper run through `adb shell am instrument` directly (Gradle's `connectedDebugAndroidTest` uninstalls the app-under-test afterward, which would have wiped the seeded state before it could be observed) — deleted after the walkthrough, not part of the committed suite. Confirmed: the one-time sleep-intro toast then the normal arm toast on a second arm; "Asleep at 6" / "Slept 1m" (1-minute floor correctly applied for a same-minute Sleep→Wake) with matching "asleep"/"awake" Recent-list badges; help card shows the exact frozen 3-paragraph copy and toggles closed; onset-chip prompt appeared only on the first (genuine-onset) tap, and a second capture that happened *while the prompt was still open* did not retrigger it — Submit correctly patched chips onto the original entry, not the newer one, confirming the architect-review fix; marker Save persisted with no visible error and Cancel discarded; paused banner showed the frozen copy and hid the capture surface while the Recent list (with the earlier chips) stayed visible, and "Start again" restored it; check-in banner appeared at 40 seeded entries with an old `checkinAt`, and both "Still useful" (dismisses) and "Pause tracking" (dismisses into the paused state) worked correctly.

## Task decomposition

1. Add `EntryKind`/`EntryKindConverter`, the `EntryDao.mostRecentAtOrBefore` query, `SleepInterval`/`SleepCaptureOutcome`/`SleepDao` (including the `@Transaction` `captureSleep`/`captureWake` methods), `Marker`/`MarkerDao`, `TrackSettings`/`TrackSettingsDao` to `com.kieslingdev.mindscale.data`; bump `MindScaleDatabase` to version 2 with the new entities/converters and the creation callback. — LINT-BUILD
2. Write `Migration(1, 2)` (additive DDL + explicit `track_settings` seed `INSERT`, since the creation callback does not run on an upgrade) and regenerate/commit `app/schemas/.../2.json`; write the migration-preserves-data instrumented test. — INSTRUMENTED
3. Write instrumented DAO tests for `SleepDao` (including the concurrency test for `captureSleep`/`captureWake`), `MarkerDao`, `TrackSettingsDao`, and `EntryDao.mostRecentAtOrBefore` (in-memory Room, callback attached). — INSTRUMENTED
4. Extend `TrackUiState`/`TrackEvent`/`TrackViewModel` per the frozen shapes above: onset-chip detection (Invariant 15/D-3, backed by `mostRecentAtOrBefore`) and prompt flow, arm/capture flow calling `SleepDao.captureSleep`/`captureWake` and mapping their outcome to a toast, marker flow, checkin/pause flow, help toggle, persisted `sleepIntroShown` toast gating, chip-editable `EditEntryState`. Unit-test each behavior listed in Acceptance criteria. — UNIT
5. Extend `TrackScreen`: paused banner, help card, onset-chip card, Sleep/Wake buttons, marker control, check-in banner, toast surface; extend the Recent-list row (badge + chips + inline chip editing). Wire `rememberSaveable` for the marker text field and `SavedStateHandle` for marker visibility/draft restoration across ViewModel recreation. — LINT-BUILD, INSTRUMENTED
6. Add accessibility semantics for every new interactive element (badges, Sleep/Wake state, chip pills, marker control). — UI-ACCESSIBILITY
7. Update `TrackRoute`'s manual-DI construction in `MainActivity` to build/inject `SleepDao`/`MarkerDao`/`TrackSettingsDao` alongside `EntryDao`. — LINT-BUILD
8. Full oracle pass: `.\gradlew.bat test`, `.\gradlew.bat lint`, `.\gradlew.bat assembleDebug`, `.\gradlew.bat connectedDebugAndroidTest`, then the manual emulator walkthrough (last acceptance criterion) using direct-DAO test hooks to reach `askChips=true`/`paused`/`checkinAt` states no UI can reach yet. — LINT-BUILD, MANUAL
9. `architect-reviewer` critical-path pass — data migration, the transactional sleep-capture design, and the timestamp-relative onset query are exactly the kind of persistence-integrity/concurrency surface Phase 1's own review trigger calls out. — REVIEW

## Rollout, migration, and rollback

- Real users may already have a v1 database from Phase 1 (or will by the time this ships) with real symptom history — unlike Phase 1, this is **not** a from-scratch schema. `fallbackToDestructiveMigration` is explicitly disallowed; `Migration(1, 2)` must be additive only:
  - `ALTER TABLE entries ADD COLUMN kind TEXT DEFAULT NULL`
  - `CREATE TABLE sleeps (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, startTs INTEGER NOT NULL, endTs INTEGER)`
  - `CREATE TABLE markers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ts INTEGER NOT NULL, text TEXT NOT NULL)`
  - `CREATE TABLE track_settings (id INTEGER PRIMARY KEY NOT NULL, sleepOn INTEGER NOT NULL, askChips INTEGER NOT NULL, paused INTEGER NOT NULL, checkinAt INTEGER NOT NULL, sleepIntroShown INTEGER NOT NULL)`
  - `INSERT INTO track_settings (id, sleepOn, askChips, paused, checkinAt, sleepIntroShown) VALUES (0, 1, 0, 0, 0, 0)` — seeds the canonical row for users upgrading from v1, since the `RoomDatabase.Callback.onCreate` seed only fires for a brand-new database file, not an upgraded one.
- Rollback is source-control revert, same caveat Phase 1 already recorded: reverting app code without a matching downgrade migration will crash on next launch if a later phase has shipped schema version 3+. Record any such downgrade migration when that day comes.
- No feature flag/staged rollout exists in this repo; same as Phase 1, "rollout" is "merge and ship the next build."

## Open questions and approval gates

This spec is DRAFT and requires human approval before implementation, per the user's explicit instruction. The following decisions were made in this pass and are recorded rather than left open (per `CLAUDE.md`'s routing guidance), but the spec as a whole is still gated:

- **D-1 (minimal `TrackSettings` persistence now, full Settings screen later): decided.** The paused banner, check-in banner, and onset-chip gate are meaningless without a persisted, observable `paused`/`checkinAt`/`askChips` — but a full Settings *screen* is explicitly Phase 4. Adding the smallest Room table that makes this phase's features real and testable (via direct DAO calls in tests, no UI needed yet) mirrors Phase 1's D-6 precedent (add the column now because the shape is already known) rather than inventing a throwaway mechanism that Phase 4 would have to rip out.
- **D-2 (`askChips` default `false`): decided.** Matches the mockup and SPEC.md's axiom 2 ("costs nothing on a good day") — onset chips ship this phase but stay inert until Phase 4 lets a user opt in. Tests exercise the `true` path directly against `TrackSettingsDao`, not through UI, since no toggle UI exists yet.
- **D-3 (onset-detection rule, simplified, and defined relative to the capture's own timestamp): decided.** `isOnset := value > 0 && (EntryDao.mostRecentAtOrBefore(ts) == null || EntryDao.mostRecentAtOrBefore(ts)!!.value == 0)` — see Invariant 15 and the new `EntryDao.mostRecentAtOrBefore` query. This is a direct, testable read of SPEC.md's axiom ("onset = first non-zero after a 0 or a gap") but deliberately ignores the mockup's fuller "hold/auto-end implies an assumed gap" logic, which depends on the time-weighted episode model this repo hasn't built yet (Insights phase). Flagged so whoever implements the full model later reconciles this simplified rule against it rather than being surprised by a behavior change. Anchoring the query to the capture's own `ts` (rather than to whatever is newest in the table, or to the ViewModel's already-truncated `recentEntries`) is deliberate: a backdated capture must be judged only against what preceded it at that point in the timeline, not against entries that happen to exist with a later timestamp.
- **D-4 (Sleep/Wake capture always records the Entry, even on a sleep-bookkeeping no-op): decided.** Matches the mockup's actual `record()` behavior exactly (Invariant 17) — the intensity reading is real and worth keeping even when the sleep-interval side of the tap fails to make sense (e.g., Wake with nothing open). The toast is the only signal of the no-op; nothing is silently dropped.
- **D-5 (no backdating for markers): decided.** The mockup's marker input has no datetime field (unlike the backdate/edit dialogs, which do) — markers are always stamped `nowProvider()`. If this needs to change later it's a small, isolated addition, not a redesign.
- **D-6 ("Export or delete" on the paused banner): OUT of scope, deferred.** That action needs Settings-screen data actions (export/erase) that are Phase 4 work; shipping a button that does nothing, or that reaches into Phase 4's territory early, is worse than shipping "Start again" alone this phase. Tracked in `docs/specs/BACKLOG.md`.
- **D-7 (default onset-chip vocabulary is fixed in code, not user-editable): decided.** The ten-chip list matches the mockup's default exactly; making it configurable is a Settings-screen (Phase 4) concern with no Track-screen behavior implications worth blocking on now.
- **D-8 (Room creation callback vs. lazy ViewModel-side upsert for the default `TrackSettings` row): decided — callback, plus an explicit seed `INSERT` inside `Migration(1, 2)` for upgrading installs.** A callback guarantees the canonical row exists before any `observe()` collector runs on a *fresh* database, so `TrackSettingsDao.observe(): Flow<TrackSettings>` can be non-null there and every consumer (ViewModel, tests) is spared a "settings not yet seeded" transient state — but `RoomDatabase.Callback.onCreate` only fires when the database file is created for the first time, never on a version upgrade, so an existing Phase-1 user's `Migration(1, 2)` must seed the row itself (see Rollout, migration, and rollback). The lazy-upsert alternative would work but adds a real (if narrow) race and a nullable type everywhere for no benefit.
- **D-9 (sleep-capture atomicity lives in `SleepDao`, not the ViewModel or a repository layer): decided.** `SPEC-track-numpad-logging.md`'s D-3 (no repository layer — distinct from this spec's own D-3 above, which is about onset detection) still holds; this doesn't introduce one. Room's `@Transaction` support on default DAO interface methods gives the exact atomicity needed ("read open interval, then write") without a new abstraction layer, a manual `synchronized` block, or a hand-rolled mutex in the ViewModel (which would only serialize calls within one process/ViewModel instance, not provide the same guarantee Room already gives for free at the database layer). `TrackViewModel.armedCapture` stays a plain, un-synchronized UI field — see Invariant 17.
- **D-10 ("exactly one `TrackSettings` row" restated as "the canonical `id=0` row"): decided.** The original draft claimed the schema guarantees a single row; it doesn't — `@PrimaryKey val id: Int = 0` only makes `id` unique per row, it does not prevent a bug from inserting a row with a different id. Every read/write in this spec exclusively targets `id = 0` by construction, and `observe()`'s `WHERE id = 0` clause returns the canonical row regardless of any stray row that might exist, so behavior stays correct either way — but the spec now says what's actually true rather than overclaiming a schema-level guarantee. Adding a `CHECK (id = 0)` constraint to genuinely enforce it at the schema level is a small, non-blocking hardening option for the implementer, not required by this spec.
- **Not yet decided, non-blocking:** exact visual treatment (colors, iconography, spacing) for the new banners/cards/badges is left to the implementer, per this spec's own framing of non-frozen UI-polish details (same posture as Phase 1's D-6 on color-ramp hex values).
