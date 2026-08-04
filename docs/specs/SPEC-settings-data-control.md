# MindScale Phase 4 — Settings, anchors, and data control

Status: IMPLEMENTED

Owner: TBD

Date: 2026-08-03

Last verified commit: `87eb817713a459fd231ce77169dfa522b249f641` (`Implement Phase 3 full log`), pushed to `origin/main`

Approval: On 2026-08-03, the user approved D-1 through D-9 and authorized the separate Phase 3 checkpoint commit and push. Phase 4 implementation is authorized against that clean checkpoint.

Implementation: Completed and oracle-verified on 2026-08-03/04 against the API 36 emulator. The Phase 4 worktree is intentionally uncommitted pending a separate user commit request.

## Critical review findings resolved

1. The interim custom top bar initially rendered inside the status-bar inset. Compose semantics tests could activate Settings, but a real touchscreen tap was unreliable. Added explicit status-bar inset padding, then physically re-tested the control on the emulator.
2. The first export implementation correctly withheld success on write failure but discarded its already-encoded payload. Added an in-process retry state so a failed provider write can retry the identical snapshot without rereading changing records.
3. Onset-word case folding initially depended on the device locale. Switched normalization and historical-word union to `Locale.ROOT` so Turkish and other locale rules cannot change persisted de-duplication.

Governing product sources:

- Local Claude Design handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html`, especially Settings, Track readout, paused exit, theme tokens, and data actions
- The same handoff's `SPEC.md`, especially Axioms, Numpad drift fix, Export, Safety, and Strip
- `docs/specs/SPEC-track-phase2-completeness.md`, `docs/specs/SPEC-full-log.md`, and `docs/specs/BACKLOG.md`

## Purpose

Make the settings already persisted by Track reachable, let the user define the personal vocabulary that gives their ratings stable meaning, and provide a dignified local export/pause/erase path. This phase also replaces the template's device-dependent purple palette with the approved app-wide gold/ink light and dark foundation so Settings does not become a one-screen visual exception.

## Goals

1. Add a reachable native Settings overlay that returns to the prior Track or Log destination.
2. Make Sleep/Wake, onset prompts, hidden note previews, appearance, time format, and pause/resume functional preferences.
3. Persist behavioral anchors for 2/5/8 and show the corresponding anchor in the Track readout.
4. Persist a safe, normalized custom onset vocabulary and use it everywhere chips can be chosen or edited.
5. Prompt once, around the fifteenth rating, to define anchors without nagging or requiring action on a well day.
6. Export a complete JSON backup or a raw-record CSV through Android's document picker with no broad storage permission.
7. Allow export-then-erase only after a successful backup write and a separate destructive confirmation.
8. Migrate existing databases additively from Room version 2 to 3 without changing recorded entries, sleeps, or markers.

## Non-goals

- No Insights episode/hold/auto-end/ramp implementation. The prototype's `holdH`, `rampMin`, and `autoEnd` controls remain hidden until the derived episode model exists.
- No Profile screen, external PHQ-8/GAD-7 score storage, clinician report, Safety card, or paced-breathing implementation. Their Settings/Profile controls would be inert in this phase.
- No biometric lock. The prototype's iOS-specific “Face ID” label needs a separate Android security/threat-model spec before adding a Biometric dependency.
- No JSON/CSV import, merge, restore, sample-data replacement, or file-provider sharing. Import needs independent validation, conflict, atomicity, and rollback decisions.
- No custom font download or bundled font asset. Retain the current Compose typography while applying the approved colors and hierarchy.
- No server, account, analytics, telemetry, background upload, or Internet permission.
- No production signing, release publishing, or backup-provider integration changes.
- No deletion of emulator or user data during the manual walkthrough. Destructive behavior is exercised against isolated test databases; manual verification stops at the final confirmation dialog.

## User experience

### Settings entry and navigation

- Track and Log receive an accessible `Settings` action in the shared top app bar. This is an intentional interim entry point until Profile exists; Settings is not added as a primary bottom-navigation tab.
- Settings opens as an overlay destination, hides bottom navigation, and shows a Back action. Back returns to whichever top-level destination opened it.
- The paused Track banner adds `Export or delete`, which opens Settings focused on `Your data`.
- The one-time anchor prompt opens Settings focused on `What the numbers mean to you`.
- Destination, prior destination, and requested focus survive Activity recreation through primitive saveable state.

### Appearance and time

- Appearance offers `System`, `Light`, and `Dark`. Selection applies immediately and persists.
- `System` follows `isSystemInDarkTheme()`; dynamic colors are disabled so MindScale's visual identity and intensity contrast do not vary by wallpaper.
- Light tokens derive from the design source: background `#FCFBF9`, surface `#FFFFFF`, ink `#17130C`, gold `#AE8C4F`, deep gold `#9A7B44`.
- Dark tokens derive from the design source: background `#100E0B`, surface `#191612`, ink `#F4F0E8`, gold/deep gold `#C9A96A`.
- Time format offers `12-hour` and `24-hour`, defaults to `12-hour` to preserve current output, and updates Track/Log display and Track feedback. Editable timestamp fields remain the unambiguous `yyyy-MM-dd HH:mm` 24-hour format.

### Behavioral anchors

- Settings shows one single-line field each for 2, 5, and 8 with the design's explanatory copy. Blank is valid; nonblank values are trimmed and limited to 160 Unicode code points.
- A visible `Save anchors` action commits all three fields atomically. Failure keeps the drafts and reports that nothing was lost.
- Track maps ratings 1–3 to anchor 2, 4–6 to anchor 5, and 7–10 to anchor 8. Rating 0 has no anchor.
- When a mapped anchor is nonblank, the transient readout shows it beneath the number/band. Anchors never change stored ratings or statistics.
- If all anchors are blank, `anchorPromptDone` is false, and total rating count first reaches 15, Track shows one calm prompt. `Set anchors` marks the prompt handled and opens Settings at anchors; `Not now` marks it handled without navigation. It never reappears automatically.

### Onset words

- Settings shows the current onset words as a comma/newline-separated draft plus `Save words` and `Restore defaults`.
- Normalization trims whitespace, discards blanks, performs case-insensitive de-duplication while preserving first spelling/order, permits at most 20 words, and limits each word to 32 Unicode code points.
- An empty normalized list or any over-limit item is rejected inline and leaves the persisted vocabulary unchanged.
- The default list remains: `flat`, `agitated`, `hopeless`, `numb`, `wired`, `foggy`, `alone`, `driving`, `work`, `poor sleep`.
- The same persisted list drives onset prompts, Track edit chips, and Full Log edit chips. Existing chips attached to a particular Entry remain visible/editable even when they are no longer in the current vocabulary; choice lists use the current vocabulary plus that Entry's existing extras.

### Functional preferences

- `Sleep and Wake` controls the capture buttons. Turning it off is rejected while a sleep interval is open, with `Wake first so the interval has an end.` No interval is silently stranded.
- `Ask what was happening` controls onset prompts and remains off by default.
- `Hide notes in lists` folds note previews in Track and Full Log while preserving Note actions and stored text.
- `Pause tracking` / `Start tracking again` updates the existing persisted pause state. Pausing hides only the Track capture surface; Log, Settings, export, and erase remain available.
- Settings does not expose any toggle whose underlying feature is absent.

### Export

- `Export backup` launches `ActivityResultContracts.CreateDocument("application/json")` with `mindscale-backup-YYYYMMDD-HHmmss.json`.
- `Export records` launches `ActivityResultContracts.CreateDocument("text/csv")` with `mindscale-records-YYYYMMDD-HHmmss.csv`.
- Canceling the picker is a neutral cancellation, not an error or success. Writing errors remain on Settings with Retry; the app never claims export succeeded until the stream closes successfully.
- Export uses a transactionally read snapshot. It does not request `READ_MEDIA_*`, legacy external-storage, or all-files-access permission.
- JSON is UTF-8 and contains:
  - `format: "mindscale-backup"`, `version: 3`, and UTC ISO-8601 `exportedAt`;
  - entries with UTC timestamp, intensity, chips, nullable note, and nullable `SLEEP`/`WAKE` kind;
  - sleeps with UTC start and nullable UTC end;
  - markers with UTC timestamp and text;
  - user settings required to restore meaning: anchors, onset words, sleep/onset/note/pause preferences, appearance, and time format.
- CSV is UTF-8 with header `record_type,timestamp,end_timestamp,intensity,kind,chips,note,text`; timestamps are UTC ISO-8601, chips use `|`, and every field follows RFC 4180 quoting. CSV is a raw-record interchange export, not a settings backup.
- Pure deterministic encoders own escaping and ordering; no new serialization dependency is added solely for export.

### Export then erase

- The destructive row reads `Export, then erase everything`.
- Its first action launches JSON export. Erase is not armed if the picker is canceled or writing fails.
- After a successful JSON write, show a separate confirmation dialog naming current rating, sleep, and marker counts and stating that anchors, custom words, and preferences will also reset.
- Confirm performs one Room transaction that deletes all entries, sleeps, and markers and resets the canonical settings row to Phase 4 defaults. Cancel changes nothing.
- After success, Settings remains open in the default System/light-or-dark-following-device state, reports `Everything on this device was erased`, and Track/Log react to empty streams.
- Erase never deletes files outside the app, manipulates Android backup accounts, or claims remote backup copies were deleted.

## Frozen interfaces and data contracts

### Settings model and migration

```kotlin
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class HourFormat { TWELVE, TWENTY_FOUR }

@Entity(tableName = "track_settings")
data class TrackSettings(
    @PrimaryKey val id: Int = 0,
    val sleepOn: Boolean = true,
    val askChips: Boolean = false,
    val paused: Boolean = false,
    val checkinAt: Long = 0L,
    val sleepIntroShown: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hourFormat: HourFormat = HourFormat.TWELVE,
    val anchor2: String = "",
    val anchor5: String = "",
    val anchor8: String = "",
    val onsetChips: List<String> = DEFAULT_ONSET_CHIPS,
    val hideNotes: Boolean = false,
    val anchorPromptDone: Boolean = false
)
```

- Room version becomes 3.
- `MIGRATION_2_3` uses additive `ALTER TABLE` statements only, with non-null defaults matching the model. It never drops/recreates a table or rewrites existing record rows.
- `MindScaleDatabase.build()` registers `MIGRATION_1_2` and `MIGRATION_2_3`; version-1 migration tests must still upgrade through both steps.
- Exported schema `app/schemas/.../3.json` is committed.

### Targeted settings mutations

No production caller may write a stale full `TrackSettings` snapshot. `TrackSettingsDao` exposes affected-row-count targeted methods for:

```kotlin
suspend fun setSleepOn(enabled: Boolean): Int
suspend fun setAskChips(enabled: Boolean): Int
suspend fun setPaused(paused: Boolean): Int
suspend fun recordCheckin(checkinAt: Long, paused: Boolean): Int
suspend fun setSleepIntroShown(shown: Boolean): Int
suspend fun setAppearance(mode: ThemeMode): Int
suspend fun setHourFormat(format: HourFormat): Int
suspend fun setAnchors(anchor2: String, anchor5: String, anchor8: String): Int
suspend fun setOnsetChips(chips: List<String>): Int
suspend fun setHideNotes(hidden: Boolean): Int
suspend fun setAnchorPromptDone(done: Boolean): Int
```

- The legacy full-row `update()` is removed from production use and may be removed entirely after all tests migrate.
- Disabling Sleep/Wake uses a transaction that rechecks `SleepDao.openInterval()` before updating. UI state is never the correctness mechanism.

### Snapshot and erase DAO

```kotlin
data class DataSnapshot(
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>,
    val settings: TrackSettings
)

@Dao
interface DataControlDao {
    @Query("SELECT * FROM entries ORDER BY ts DESC, id DESC")
    suspend fun allEntries(): List<Entry>

    @Query("SELECT * FROM sleeps ORDER BY startTs DESC, id DESC")
    suspend fun allSleeps(): List<SleepInterval>

    @Query("SELECT * FROM markers ORDER BY ts DESC, id DESC")
    suspend fun allMarkers(): List<Marker>

    @Query("SELECT * FROM track_settings WHERE id = 0")
    suspend fun settings(): TrackSettings

    @Query("DELETE FROM entries") suspend fun deleteEntries(): Int
    @Query("DELETE FROM sleeps") suspend fun deleteSleeps(): Int
    @Query("DELETE FROM markers") suspend fun deleteMarkers(): Int
    @Update
    suspend fun resetSettings(defaults: TrackSettings): Int

    @Transaction
    suspend fun snapshot(): DataSnapshot =
        DataSnapshot(allEntries(), allSleeps(), allMarkers(), settings())

    @Transaction
    suspend fun eraseEverythingAndResetSettings(): EraseCounts {
        val counts = EraseCounts(deleteEntries(), deleteSleeps(), deleteMarkers())
        check(resetSettings(TrackSettings()) == 1)
        return counts
    }
}

data class EraseCounts(val entries: Int, val sleeps: Int, val markers: Int)
```

- Snapshot reads all tables in deterministic timestamp/id order and the canonical settings row in one Room transaction.
- Erase counts/deletes all record rows and resets `id=0` in one transaction. Any exception rolls back the whole operation.
- The erase-only `@Update(TrackSettings())` is the sole permitted full-row settings write: it intentionally resets every field to known defaults inside the destructive transaction and cannot carry a stale UI snapshot.

### Settings UI state

```kotlin
data class SettingsUiState(
    val settings: TrackSettings = TrackSettings(),
    val anchorDraft: AnchorDraft = AnchorDraft(),
    val chipDraft: String = "",
    val anchorError: String? = null,
    val chipError: String? = null,
    val exportState: ExportState = ExportState.Idle,
    val eraseConfirmation: EraseConfirmation? = null,
    val message: String? = null
)
```

- Anchor/chip drafts use primitive `SavedStateHandle` values and survive true ViewModel recreation.
- Events are explicit and state-driven; composables never call DAOs or write files.
- Android document launch/write is handled at the Activity/Compose shell boundary. The ViewModel owns snapshot/encoding state and receives explicit picker-canceled/write-succeeded/write-failed results.

## Invariants

1. The canonical settings row remains `id=0`; every read/update/reset filters that id.
2. No settings write can revert an unrelated field from a stale snapshot.
3. All existing Phase 1–3 entries, sleeps, markers, kinds, chips, notes, timestamps, and ids survive migration 2→3 unchanged.
4. An open sleep interval can never be stranded by disabling its capture UI.
5. Custom words are normalized exactly once at the settings boundary; downstream screens consume the persisted normalized order.
6. Removing a word from Settings never mutates historical Entry chips.
7. Anchors are personal labels only; they never alter numeric storage, bands, derived data, or export intensity.
8. The anchor prompt appears at most once automatically and never appears on a well day merely because time passed.
9. Hiding notes affects previews only; note data and editing access remain intact.
10. Theme choice never changes persisted clinical data and never depends on wallpaper dynamic colors.
11. Export is local, user-initiated, and deterministic. No network or broad-storage permission is introduced.
12. A canceled/failed export cannot arm erase or produce a success message.
13. Erase requires both a successful JSON export in the current Settings session and an explicit destructive confirmation.
14. Erase is all-or-nothing and resets personal settings as well as records; no partial-success message is possible.
15. App and settings remain usable with zero records, offline, across rotation, and after process recreation.
16. Phase 3 must be committed as its own verified checkpoint before Phase 4 application code begins; Phase 4 may not silently compound into the uncommitted Phase 3 diff.

## Failure behavior

- Settings read failure: show a calm error and Retry; do not silently display defaults as if they were saved.
- Targeted update returns 0: report the missing canonical settings row and do not claim success.
- Settings mutation exception: retain the relevant draft/control state and allow Retry.
- Invalid anchors/words: inline validation; no partial persistence.
- Disable Sleep/Wake with an open interval: reject without changing the setting.
- Snapshot failure: do not launch the document picker.
- Picker cancellation: return to Idle without toast.
- File open/write/close failure: report failure, retain a retryable encoded snapshot only for the current process, and never arm erase.
- Erase failure: dialog remains retryable and Room rollback preserves all data.
- Theme enum corruption: Room converter throws/read error rather than silently choosing a different persisted value; migration/defaults only write known enum names.

## Android compatibility, privacy, and accessibility

- Keep minimum SDK 26, target SDK 36, compile SDK 36.1, Kotlin, Compose, Material 3, Kotlin DSL, bundled JDK, and Gradle wrapper.
- Use existing dependencies and Activity Result APIs; add no storage permission and no system Gradle/toolchain version.
- Settings uses a lazy list with stable section/item keys and at least 48 dp interactive targets.
- Every segmented choice and switch exposes its label, role, selected/checked state, and explanatory text to accessibility services.
- Error text is associated with its field and is not communicated by color alone.
- Gold/ink light and dark combinations must pass Android lint and manual contrast/readability inspection; rating intensity remains distinguishable by text as well as color.
- App destination/focus, settings drafts, confirmation state, and theme survive rotation; settings drafts survive ViewModel recreation. Do not persist Room entities or encoded export payloads in saved state.
- All work remains local to the device and user-chosen document URI.

## Acceptance criteria

- [x] MIGRATION: version-2 fixtures upgrade to version 3 with records preserved and specified defaults; version 1 upgrades through 2→3; exported schema validates.
- [x] DAO/CONCURRENCY: targeted settings mutations preserve unrelated columns; different-field updates preserve one another; disabling sleep while an interval is open is transactionally rejected.
- [x] UNIT: anchors, word normalization, historical chip union, filenames, JSON, and RFC 4180 CSV behavior are deterministic; existing Track tests retain prompt/time regressions.
- [x] UNIT: cancel/failure never arms erase; success does; failed writes retain the encoded snapshot for retry; missing-row/mutation failures do not report success.
- [x] INSTRUMENTED: snapshot is complete and deterministic; erase removes records and resets settings atomically; no destructive fallback exists.
- [x] UI/ACCESSIBILITY: Settings entry/Back routing, recreation, functional controls, data actions, switch semantics, and destructive confirmation have Compose/device coverage.
- [x] REGRESSION: custom vocabulary drives onset/Track/Log editing; historical extras remain selectable; hidden notes remain editable; anchors never change ratings.
- [x] THEME: Light, Dark, and System apply globally; dynamic wallpaper colors are removed.
- [x] EXPORT: the API 36 picker wrote and validated a version-3 JSON backup and RFC-4180 CSV; cancellation was neutral; write retry is unit-covered.
- [x] ERASE-SAFETY: tests prove no erase before successful JSON export and confirmation; the manual walkthrough saved the prerequisite backup, inspected the final dialog, and canceled.
- [x] ORACLES: `test`, `lint`, `assembleDebug`, `connectedDebugAndroidTest`, `adb devices -l`, install/launch, and `git diff --check` pass on `MindScale_API_36`.
- [x] REVIEW: critical-path review covered migration defaults, settings writes, export completeness/retry, erase atomicity, touch insets, theme identity, and Phase 1–3 regressions.

## Task decomposition

1. After explicit user authorization, commit/push the already-verified Phase 3 checkpoint separately. — oracle: clean `main`, local/upstream commit match
2. Add enums/converters, TrackSettings columns, migration 2→3, schema export, and migration tests. — oracle: focused Room migration tests
3. Replace full-row settings writes with targeted queries and add transactional open-sleep guard. — oracle: DAO concurrency/integrity tests
4. Add deterministic settings validation, word normalization, time formatting, backup/CSV encoders, snapshot, and atomic erase logic. — oracle: JVM + Room tests
5. Add `SettingsViewModel` with saved drafts and honest export/erase state machine. — oracle: ViewModel tests
6. Apply global gold/ink theme modes and central time format; update Track/Log consumers. — oracle: unit + screenshot/UI tests
7. Add Settings overlay/top-bar navigation and the functional Settings sections. — oracle: navigation/Compose accessibility tests
8. Wire anchors, one-time prompt, custom vocabulary, hidden previews, pause/resume, and paused-banner deep link. — oracle: Track/Log regression tests
9. Wire Android CreateDocument JSON/CSV writes and guarded erase confirmation. — oracle: API 36 document/Room tests
10. Run the full oracle suite, isolated destructive tests, non-destructive manual walkthrough, and critical review. — oracle: completion checklist

## Rollout, migration, and rollback

- Rollout requires the Phase 3 commit first, then one additive Room migration from 2 to 3.
- `MIGRATION_2_3` is mandatory; no `fallbackToDestructiveMigration` or manual database deletion is allowed.
- Version-1 users migrate through registered 1→2→3 steps.
- Rollback of an installed version-3 database to Phase 3/version 2 is not supported by Room. Before shipping, rollback is source-control revert plus fresh test install only; after release, forward-fix is required unless a separately tested 3→2 downgrade migration is approved.
- Export format version 3 is independent of Room schema hash; future import must validate `format` and `version` before touching data.

## Open questions and approval gates

- **D-1 (bounded scope, recommended):** Phase 4 includes only functional appearance/time, anchors, vocabulary, Sleep/Wake, onset prompt, hidden-note previews, pause/resume, JSON/CSV export, and guarded erase. Defer inert hold/ramp/auto-end, breathing, biometrics, scores, import, sample data, Profile, Safety, and Report.
- **D-2 (brand foundation, recommended):** apply gold/ink globally now and disable dynamic wallpaper colors; retain current typography until an approved font asset exists.
- **D-3 (reachable Settings, recommended):** use an interim shared top-app-bar Settings action plus deep links from paused/anchor cards; keep Settings out of primary bottom navigation.
- **D-4 (persistence, required):** migrate Room 2→3 additively and use targeted settings updates so concurrent screens cannot revert unrelated preferences.
- **D-5 (export/import boundary, recommended):** ship deterministic JSON backup and CSV records now; defer import until validation/merge/rollback semantics have their own approved spec.
- **D-6 (erase semantics, recommended):** require successful JSON export in the current session plus a separate confirmation; atomically erase all records and reset all settings.
- **D-7 (open sleep safety, recommended):** reject disabling Sleep/Wake while an interval is open rather than hiding the only normal way to close it.
- **D-8 (anchor prompt, recommended):** prompt once when the fifteenth rating exists, only if all anchors are blank; both actions permanently mark the automatic prompt handled.
- **D-9 (Phase 3 checkpoint, required):** no Phase 4 application implementation begins until the user explicitly authorizes committing the verified Phase 3 work as its own checkpoint. Pushing remains separately approval-controlled by `AGENTS.md`.

Approval gates are satisfied. D-1 through D-9 are frozen for implementation; any material scope change requires a spec amendment and renewed user approval.
