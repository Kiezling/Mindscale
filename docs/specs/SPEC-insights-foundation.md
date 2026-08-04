# MindScale Phase 5 — Insights foundation

Status: IMPLEMENTED — VERIFIED LOCALLY

Owner: TBD

Date: 2026-08-03

Approval: On 2026-08-03, the user approved the complete Phase 5 specification and D-1 through D-10 and authorized implementation.

Governing product sources:

- Local Claude Design handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html`, especially the Insights screen, range selector, episode model, raster, Settings hold control, and empty state
- The same handoff's `SPEC.md`, especially Axioms, Data model, Statistics, Insight grammar, Views, and Export
- `docs/specs/SPEC-track-phase2-completeness.md`, especially D-3's explicitly temporary onset rule
- `docs/specs/SPEC-full-log.md` and `docs/specs/SPEC-settings-data-control.md`

## Purpose

Build the first native Insights slice around MindScale's defining measurement model: episodes derived from event-contingent ratings, with symptom burden weighted by awake time rather than by entry count. Add a third top-level Insights destination, the primary day/hour raster, descriptive episode summaries, and the minimum setting required to make assumed episode endings honest and adjustable.

This phase deliberately does not reproduce the handoff's entire Insights page. It establishes one reviewed derivation engine and one primary visualization before later phases add more charts or clinician-facing output.

## Product-source reconciliation

Functionality already implemented:

- Phase 1/2 records editable 0–10 `Entry` rows, including explicit zero endings, chips, notes, and Sleep/Wake kinds.
- Phase 2 records raw `SleepInterval` rows and uses a deliberately simplified onset rule: a positive ordinary capture is an onset only when no prior entry exists or the immediately prior entry is zero.
- Phase 3 provides a complete mixed Full Log, targeted mutations, deterministic ordering, and a process-scoped application container.
- Phase 4 provides global gold/ink theming, time format, local export/erase, settings persistence, and Room schema 3.

Conflicts and native decisions proposed by this spec:

1. The handoff `SPEC.md` requires a capped last-observation-carried-forward hold measured in awake time. The HTML also exposes an `End entries on their own` switch that can disable the cap. Disabling it would violate the stated statistics rule and let one rating persist for days. Phase 5 therefore keeps auto-end always enabled and exposes only the approved hold length.
2. The handoff `SPEC.md` requires a step function and forbids interpolation across well days. The HTML defaults to a smoothed `Line` mode and a configurable ramp. Phase 5 does not add `chartMode` or `rampMin`; any later trend chart must be step-only unless the product rationale is explicitly superseded.
3. The HTML's sample-data action conflicts with the no-synthetic-seed rule. The native empty state contains no sample-data loader.
4. The HTML uses fixed 24-hour millisecond day arithmetic. Native range and raster days use `java.time` local-day boundaries so 23-hour and 25-hour DST days are correct.
5. The HTML can include an episode's pre-range duration and AUC in selected-range totals. Native totals are clipped to the selected window and disclose carried-in episodes separately.

## Goals

1. Add a native `Insights` top-level destination beside Track and Log without adding a navigation framework.
2. Derive deterministic episodes, awake duration, peak, explicit/assumed/ongoing resolution, and intensity-hours from existing Entries and SleepIntervals.
3. Reconcile Track's onset-chip prompt with the same hold-aware episode semantics, including backdated captures.
4. Add `1D`, `3D`, `7D`, `30D`, `90D`, and `6M` range choices that apply to every Phase 5 insight.
5. Render the primary one-row-per-local-day, hours-across raster with sleep, well, symptom intensity, no-data, and future states kept distinct.
6. Show restrained descriptive summaries and recent episode rows without inference or causal language.
7. Add a persisted 8/12/16/24 waking-hour hold choice through an additive Room 3→4 migration.
8. Keep derivation local, reactive, lifecycle-aware, testable as pure Kotlin, and free of chart, navigation, DI, analytics, or background-work dependencies.

## Non-goals

- No smoothed line chart, ramp setting, chart-mode setting, entry-level step chart, sleep bands over a trend, or marker overlay. A later step-chart phase may consume the same engine.
- No onset-to-onset histogram, time-of-day onset chart/sentence, periodicity analysis, automated correlation, trigger detection, prediction, p-values, confidence language, or statistical-significance claims.
- No short-night/long-night comparison, nap comparison, or claim about the direction of sleep effects.
- No clinician one-pager, Report, Profile, PHQ-8/GAD-7 storage, Safety card, or breathing tool.
- No persisted `Episode` entity or derived-statistics cache. Episodes remain deterministic projections of raw local records and current settings.
- No import, restore, merge, sample data, account, server, analytics, telemetry, Internet permission, notification, alarm, or periodic background worker.
- No edit/delete behavior on Insights. Raw facts remain maintainable through Track and Full Log.
- No toolchain, SDK, Gradle, AGP, Kotlin, Compose, Java, or dependency upgrade.

## User experience

### Navigation and restoration

- The bottom navigation contains `Track`, `Log`, and `Insights`; Track remains the launch destination.
- Selecting Insights preserves Track and Log ViewModel state.
- System Back from Insights returns to Track, matching Log's existing top-level behavior. Back from Track retains normal Activity behavior.
- The shared Settings action is available from Insights. Back from Settings returns to Insights when Insights opened it.
- The selected top-level destination, selected insight range, and the currently explored raster instant survive Activity recreation using primitive saveable/SavedStateHandle values.

### Range selector

- Choices are `1D`, `3D`, `7D`, `30D`, `90D`, and `6M`; `30D` is the default.
- Day ranges mean local calendar days including today: `1D` starts at today's local start, `3D` starts at the local start two dates ago, and so on. `6M` starts at the local start of the date six calendar months before today. Every window ends at the injected current instant.
- If the first ever Entry is later than the selected start, the raster still shows earlier selected dates as `no data`; statistics begin at the first Entry and never relabel pre-instrument time as well.
- Changing ranges clears the current raster exploration selection and recomputes every summary from the same immutable source snapshot.

### Empty, loading, and error states

- Before the first Entry, Insights says `Nothing to draw yet` and `This page shows only what you recorded — no estimates and no guesses. It fills in as you log.`
- No synthetic/sample-data action is shown.
- Initial load has a calm progress state. A Room read or derivation failure keeps the destination available, says the records remain on the device, and offers Retry.
- An empty selected range after older records exist says `No ratings in this range`; it does not imply the database is empty.

### Summary and episode facts

- The summary strip shows:
  - `Episodes`: derived episodes intersecting the selected window;
  - `Typical length`: median awake duration among episodes with a known explicit or assumed end; ongoing episodes are excluded;
  - `Clear days`: eligible local days with zero positive-intensity awake time, displayed as `clear/eligible`;
  - `Peak`: highest recorded positive intensity among episodes intersecting the window, or `—`.
- An episode that began before the selected window but overlaps it is counted once and disclosed as `carried into this range` in the facts. Range totals clip its awake duration and AUC to the window.
- Episode facts use record-restatement grammar only, including:
  - episodes intersecting the range and how many began within it;
  - median/shortest/longest closed awake duration with the closed-episode denominator;
  - clear days and longest clear stretch;
  - highest peak and median peak with the episode denominator;
  - total `intensity-hours` in the selected window, explained as intensity multiplied by awake hours;
  - count of assumed endings and the selected waking-hour cap.
- Facts with no valid denominator are omitted or use a plain `Not enough closed episodes yet`; they never divide by zero or substitute an entry-weighted mean.
- The recent episode list shows at most eight intersecting episodes, newest onset first. Each row shows onset date/time, peak, full awake duration, explicit/assumed/ongoing status, sleep count inside the episode, and distinct chips in first-recorded order. The selected range does not truncate the per-episode detail label, but range totals remain clipped.

### Day/hour raster

- The raster is the Phase 5 primary pattern view: one row per selected local calendar date, midnight-to-midnight in that date's zone offset, with horizontal position proportional to elapsed time inside that specific local day.
- Visual states are distinct:
  - `no data`: before the first Entry;
  - `well`: awake time with effective intensity 0 after measurement began;
  - intensity 1–10: the existing accessible gold intensity ramp;
  - `asleep`: normalized SleepInterval coverage;
  - `future`: time after now on the current date.
- Sleep takes visual precedence over an intensity because symptom duration and AUC are paused while asleep; the underlying Entry is still preserved and visible in Track/Log.
- Tap or drag explores a day/hour and updates a persistent textual readout: local date, local hour, and `no data`, `nothing recorded`, `asleep`, or numeric intensity. No hover-only behavior is allowed.
- The legend includes text for every state. Color is never the only carrier of meaning.
- Rendering is native Compose (`Canvas`/layout primitives) using existing dependencies, never HTML, JavaScript, WebView, an image of the prototype, or a new chart library.

### Settings

- Settings adds `An entry ends after` with `8h`, `12h`, `16h`, and `24h` choices and the explanation `Waking hours. Sleep pauses this clock. This changes how Insights treats gaps across your history; your records do not change.`
- The default and migration value is 16 waking hours.
- Selection persists immediately through a targeted settings mutation and causes Insights to recompute.
- There is no `End entries on their own`, `Change is drawn over`, or `Line/Steps` control in this phase.

## Frozen derivation model

### Inputs and ordering

The pure engine accepts:

```kotlin
data class EpisodeInput(
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val hold: HoldDuration,
    val now: Instant,
    val zoneId: ZoneId
)

enum class HoldDuration(val hours: Int) {
    EIGHT(8), TWELVE(12), SIXTEEN(16), TWENTY_FOUR(24)
}

enum class EpisodeEndReason { EXPLICIT_ZERO, ASSUMED_HOLD, ONGOING }
```

- Entries are ordered by `(ts ASC, id ASC)`. State derivation collapses multiple entries at one timestamp to the highest-id row; lower-id same-timestamp rows contribute no duration, episode, peak, chips, or AUC but remain raw records in Track/Log/export.
- Every Entry kind participates as an intensity state change. `SLEEP` and `WAKE` are labels on real ratings, not a second symptom series.
- Sleep intervals are half-open `[startTs, endTs)`, ordered by `(startTs ASC, id ASC)`, clipped to `now`, and unioned when they overlap or touch. An open interval ends at `now` for the current derivation only; no end is persisted.
- Source facts whose timestamp/start is after `now` are excluded from the current projection without mutation; this handles a device clock moving backward. The next future source timestamp becomes a recomputation boundary.
- Invalid persisted sleep rows with `endTs <= startTs` are not silently repaired. Derivation fails into the honest Retry/error state and raw data remains untouched.
- The engine is deterministic for an injected `now` and `zoneId`, performs no I/O, and uses an O(E log E + S log S) sort followed by linear sweeps; it must not scan every sleep for every entry or raster cell.

### Effective state and hold rule

1. Before the first Entry, state is `no data`, not zero.
2. An Entry with value 0 sets effective intensity to zero and explicitly ends any open episode at its timestamp.
3. A positive Entry sets effective intensity to its value. It persists until the earliest of:
   - the next Entry;
   - the instant at which the configured number of awake hours has elapsed since that Entry;
   - analysis `now`.
4. Time covered by the normalized sleep union consumes zero hold time and contributes zero intensity-hours. `addAwake(start, hold)` advances through wall time while skipping sleep.
5. When a positive state reaches its hold cap before another Entry, it becomes zero and the episode ends with `ASSUMED_HOLD`. A later positive Entry begins a new episode.
6. Consecutive positive Entries before the cap belong to one episode. Intensity changes at each Entry; each new positive Entry starts a fresh hold clock for its current state.
7. Sleep alone neither starts nor ends an episode.
8. A positive state still active at `now` ends with `ONGOING` in the projection only.
9. Awake duration is the union of episode wall-time excluding normalized sleep. AUC is the sum of `intensity × awake hours` over each constant-intensity segment.
10. Peaks use recorded integer Entry values, not interpolated values. Chips are the case-preserving, first-seen union of Entry chips within the episode.
11. AUC is accumulated from exact millisecond durations and converted/rounded only for display; the UI shows intensity-hours to one decimal place and never sums already-rounded segment labels.

### Range projection

```kotlin
data class DerivedEpisode(
    val onset: Instant,
    val end: Instant,
    val endReason: EpisodeEndReason,
    val peak: Int,
    val awakeDuration: Duration,
    val intensityHours: Double,
    val sleepCount: Int,
    val chips: List<String>
)
```

- The engine derives enough history before the selected window to know the effective state at the boundary.
- Window statistics intersect each episode segment with `[windowStart, now)` before calculating range awake duration and range AUC.
- An episode is `intersecting` when any positive-intensity awake segment overlaps the window. It is `started in range` when `onset` is within `[windowStart, now)`.
- A sleep is counted inside an episode when any positive-duration part lies strictly after onset and before episode end. Unioned sleep coverage counts as one sleep span even if overlapping raw rows produced it.
- Clear-day eligibility starts on the local date containing the first Entry, is clipped to the selected range, excludes future time, and requires some awake time. A clear eligible day has zero positive AUC.
- Local dates use `LocalDate.atStartOfDay(zoneId)` and the next local date's start; no day is assumed to contain exactly 86,400,000 milliseconds.

## Frozen persistence and DAO contracts

### Reactive episode source

Add a narrow DAO projection rather than a persisted episode table:

```kotlin
data class EpisodeSourceRow(
    val recordType: String,
    val id: Long,
    val ts: Long,
    val endTs: Long?,
    val value: Int?,
    val chips: List<String>?
)

@Dao
interface EpisodeSourceDao {
    @Query(
        """SELECT 'ENTRY' AS recordType, id, ts, NULL AS endTs, value, chips
           FROM entries
           UNION ALL
           SELECT 'SLEEP' AS recordType, id, startTs AS ts, endTs,
                  NULL AS value, NULL AS chips
           FROM sleeps
           ORDER BY ts ASC, recordType ASC, id ASC"""
    )
    fun observeSource(): Flow<List<EpisodeSourceRow>>

    @Query(
        """SELECT 'ENTRY' AS recordType, id, ts, NULL AS endTs, value, chips
           FROM entries WHERE ts <= :ts
           UNION ALL
           SELECT 'SLEEP' AS recordType, id, startTs AS ts, endTs,
                  NULL AS value, NULL AS chips
           FROM sleeps WHERE startTs <= :ts
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

data class OrdinaryCaptureResult(
    val entryId: Long,
    val isOnset: Boolean,
    val promptEnabled: Boolean,
    val settingsAvailable: Boolean,
    val classificationAvailable: Boolean
)
```

- `observeSource()` is one Room query mentioning both source tables so each emission is a consistent SQLite read snapshot. It returns only raw columns needed by the engine and reacts to Entry/Sleep insert, edit, and delete.
- `recordType` is restricted to `ENTRY` and `SLEEP`; unknown values are a derivation error, not silently ignored.
- `insertOrdinaryAndClassify` runs settings read, classification, and insertion in one Room transaction. `isOnsetAt` is the shared pure engine's state-at-time operation. It uses only source facts at or before the capture's own timestamp and the current persisted hold. Existing same-timestamp rows precede the new auto-increment id.
- `promptEnabled` snapshots the persisted `askChips` gate in that same transaction, so a concurrent settings write cannot classify under one settings row and gate under another.
- A missing canonical settings row falls back to `SIXTEEN` only for onset classification so the user's rating is never lost; `settingsAvailable=false` lets Track report that Settings are unavailable and `promptEnabled=false` suppresses the prompt whose opt-in state cannot be read. It does not create a second settings row.
- Implementation clarification (2026-08-04): invalid source data can make classification fail even when SQLite remains writable. `classificationAvailable=false` suppresses the prompt and lets Track say that the rating was saved but classification was unavailable. Classification is fail-closed for prompting, while the ordinary rating is still inserted by the same transaction. This adds failure observability without changing D-6's source or concurrency semantics.
- Track ordinary captures use this transaction. Sleep/Wake-armed captures retain their existing Entry insert plus `SleepDao.captureSleep`/`captureWake` behavior and never open the onset prompt.
- `EntryDao.mostRecentAtOrBefore` may remain for compatibility/tests but is no longer the production onset classifier after Phase 5.

### Room migration 3→4

`TrackSettings` adds:

```kotlin
val holdDuration: HoldDuration = HoldDuration.SIXTEEN
```

- Room version becomes 4.
- `MIGRATION_3_4` uses exactly one additive column: `holdDuration TEXT NOT NULL DEFAULT 'SIXTEEN'`.
- `SettingsConverters` stores `HoldDuration.name` and uses `HoldDuration.valueOf`; corrupt values fail the read instead of changing meaning silently.
- Existing Entries, SleepIntervals, Markers, ids, timestamps, kinds, chips, notes, and every Phase 4 setting remain byte-for-byte/logically unchanged.
- `MindScaleDatabase.build()` registers 1→2, 2→3, and 3→4; version-1 and version-2 tests still migrate through the full chain.
- Fresh-database seed SQL includes `holdDuration='SIXTEEN'`.
- Exported Room schema 4 is committed. No destructive migration or database deletion is permitted.
- A targeted `TrackSettingsDao.setHoldDuration(duration): Int` is the only normal write for this field. Production UI never writes a full stale settings row.

### Backup/export implications

- JSON backup format advances from version 3 to version 4 and adds numeric `settings.holdHours` so the interpretation of assumed endings is not lost.
- `format` remains `mindscale-backup`; timestamps and deterministic ordering remain unchanged.
- CSV is a raw-record export and remains byte-schema-compatible with Phase 4; it gains no derived episode rows or settings columns.
- Snapshot and retry invariants from Phase 4 remain: a failed provider write retries the identical encoded version-4 payload.
- Atomic erase resets `holdDuration` to `SIXTEEN` with every other setting.
- Import remains unsupported; future import must explicitly validate backup version 4.

## ViewModel and time behavior

```kotlin
enum class InsightRange { ONE_DAY, THREE_DAYS, SEVEN_DAYS, THIRTY_DAYS, NINETY_DAYS, SIX_MONTHS }

data class InsightsUiState(
    val range: InsightRange = InsightRange.THIRTY_DAYS,
    val loading: Boolean = true,
    val snapshot: InsightsSnapshot? = null,
    val exploredInstantEpochMillis: Long? = null,
    val error: String? = null
)
```

- `InsightsViewModel` combines `EpisodeSourceDao.observeSource()` with `TrackSettingsDao.observe()`, performs derivation off the main thread, and exposes immutable state through `StateFlow`.
- Range and raster exploration store only enum names and an epoch-millisecond instant in `SavedStateHandle`; no Room entity, episode object, or chart bitmap enters saved state. An instant, rather than local hour alone, distinguishes the repeated hour on a fall-back DST day.
- The engine returns the next time-derived invalidation instant: the next future source fact, active hold expiry, or local midnight. The ViewModel schedules only an in-process coroutine recomputation while collected/started. It also recomputes when source/settings change and when collection resumes.
- An open SleepInterval pauses hold expiry; a database wake update triggers the next calculation. No AlarmManager, WorkManager, service, notification, permission, or wake lock is introduced.
- Retry must create a new Room collection, not merely clear the displayed error after a terminal Flow failure.
- Inject `Clock`, `ZoneId`/zone provider, and coroutine dispatchers or equivalent test seams. Production uses the device zone and current clock.

## Invariants

1. Insights never diagnoses, predicts, recommends, attributes cause, or claims a pattern about the world.
2. All displayed claims restate local recorded facts or deterministic duration arithmetic; denominators accompany aggregations.
3. Entry-weighted means are never calculated or displayed.
4. Every positive segment is time-weighted by awake duration; sleep contributes no hold time and no AUC.
5. Auto-end is always enabled. Every positive state is capped at the selected 8/12/16/24 awake hours.
6. The trend model is piecewise constant. No interpolation or ramp changes stored or displayed values.
7. Episodes are derived only; raw Entry, SleepInterval, and Marker rows are never rewritten by Insights.
8. Explicit zero, assumed hold expiry, and ongoing state remain distinct.
9. Same source rows, hold, now, and zone always produce byte-for-byte/equality-equivalent derived output.
10. Same-timestamp Entry behavior is deterministic and consistent with higher-id-as-latest semantics.
11. Overlapping/touching sleeps are unioned for arithmetic and never double-subtracted.
12. Pre-first-entry time is no data; post-first-entry awake silence after a zero/assumed end is well.
13. Selected-range totals are clipped to the range; carried-in episode history is never silently charged to the selected range.
14. DST day length and offset changes use zone rules, not fixed milliseconds.
15. Phase 2 onset prompting uses the Phase 5 engine at the capture timestamp and cannot disagree with episode boundaries caused by hold expiry.
16. Ordinary capture classification and insert are one transaction; a rating is still recorded if settings are missing.
17. Insights updates after Track/Log edits or deletes, sleep changes, hold changes, hold expiry, local midnight, and lifecycle resume.
18. The raster uses stable local-date keys and never list index as Compose identity.
19. No sample data, persisted derived cache, new framework, chart library, navigation library, or background scheduler is introduced.
20. Phase 1–4 behavior remains unchanged except for the intentional hold-aware onset correction and the new hold setting/export field.
21. Changing hold duration re-derives history but never adds/removes chips or rewrites any historical Entry; an onset prompt is a capture-time interaction, not a retroactive mutation.

## Failure behavior

- Room source failure: keep Insights reachable, state that records remain stored locally, and offer a working Retry that restarts collection.
- Invalid source discriminator or invalid sleep interval: fail derivation visibly; never drop, clamp, rewrite, or partially chart the offending fact.
- Corrupt `HoldDuration`: the Room converter/read fails rather than silently choosing a different meaning.
- Settings mutation returns 0 or throws: retain the prior selected setting from Room, show a concise error, and do not claim success.
- Derivation exception: retain the last successfully rendered immutable snapshot when safe, show a non-alarming stale/error banner, and allow Retry. On first load with no prior snapshot, show the full error state.
- Clock/zone change: discard time-derived projection and recompute from raw facts; do not alter stored timestamps.
- Raster pointer leaves bounds: clamp exploration to a valid instant inside the selected window; future and pre-first-entry cells read honestly.
- Backup encoding/write failure: preserve all Phase 4 retry and erase-gating behavior with the version-4 payload.

## Accessibility and Android compatibility

- Continue native Kotlin, Jetpack Compose, Material 3, Kotlin DSL, minimum SDK 26, target SDK 36, compile SDK 36.1, Android Studio bundled JDK, and the project Gradle wrapper.
- Bottom navigation exposes a distinct `Insights tab` label, selected state, and at least 48 dp target.
- Range choices expose button/tab role, full spoken label (`30 days`, `6 months`), selected state, and 48 dp targets; horizontal scrolling cannot make the active choice unreachable.
- Summary values are read with their labels and denominators, not as disconnected numbers.
- The raster card is one focusable semantic exploration surface at least 48 dp high. Its state description contains the selected local date, time, UTC offset when needed to disambiguate a repeated hour, and textual state. Custom accessibility actions move through real elapsed-hour bins and previous/next local dates while clamping to valid day boundaries; touch drag/tap updates the same persistent readout.
- The raster readout is visible text and a polite live region. TalkBack use does not require interpreting color or precise pointer placement.
- Legend swatches have adjacent text; intensity 1 and 10, well, asleep, no data, and future remain distinguishable in light/dark mode and under grayscale/high-contrast inspection.
- Episode rows have a single coherent spoken description containing onset, peak, duration, end reason, sleep count, and chips. Decorative Canvas elements are hidden from the semantics tree.
- Font scaling, screen magnification, keyboard/D-pad traversal, dark theme, rotation, and edge-to-edge insets remain usable. Text is not embedded inside a bitmap.
- The Insights lazy list and raster rows use stable keys; recomposition does not reset range or exploration state.

## Test strategy

### Pure unit tests

- Episode ordering and same-timestamp higher-id final-state behavior.
- Same-timestamp rows collapsed out of duration do not create zero-duration episodes or contribute peaks/chips/AUC.
- Explicit zero, consecutive positive changes, assumed hold expiry, later restart, and ongoing episodes.
- Hold choices 8/12/16/24 and reset of the hold clock at every positive Entry.
- Awake-time addition across zero, one, multiple, overlapping, touching, and open sleep intervals.
- AUC, awake duration, peak, chips, sleep count, and explicit/assumed/ongoing end reason.
- Backdated onset classification before/after an assumed gap, with later database Entries excluded.
- Range clipping for carried-in episodes; pre-range AUC/duration does not leak into totals.
- Local-day windows and raster segmentation across America/Chicago spring-forward and fall-back transitions.
- Clear-day eligibility, current partial day, entirely sleeping day, pre-first-entry date, and future cells.
- Median and duration formatting edge cases, including minute carry.
- Next-recompute instant for a future source fact, active hold, open sleep, local midnight, and no active state.
- Insight grammar strings include counts/denominators and contain no banned inferential terms.

### Room/instrumented tests

- Version 3 migrates to 4 with all records/settings preserved and `SIXTEEN`; version 1/2 migrate through 4; schema validation passes.
- Fresh creation seeds exactly one id=0 settings row with `SIXTEEN`.
- `setHoldDuration` changes only that column, preserves concurrent unrelated settings, and returns 0 for a missing canonical row.
- `observeSource()` emits Entry and Sleep facts from one deterministic projection and reacts to insert/edit/delete in both tables.
- `insertOrdinaryAndClassify` atomically inserts and correctly classifies immediate/backdated captures under explicit-zero and assumed-hold cases; same-timestamp ties are deterministic.
- JSON version 4 contains `holdHours`; CSV remains raw-only; erase resets the hold setting atomically.

### ViewModel tests

- Source/settings changes, range changes, scheduled hold expiry, midnight, resume, and Retry all recompute correctly.
- A terminal collection error is recoverable and does not falsely imply data loss.
- Range and raster exploration primitives restore through a new ViewModel/SavedStateHandle.
- Missing settings never loses an ordinary capture and surfaces the settings problem.
- Existing onset prompt cases remain green, with new assumed-gap cases replacing the Phase 2 simplification.

### Compose/UI/accessibility tests

- Insights is the third tab, Track still launches first, Back returns to Track, Settings returns to Insights, and destination survives recreation.
- Empty, filtered-empty, loading, error, stale-with-banner, and populated states render the specified copy.
- Every range is selectable and affects summary/raster rows.
- Summary and episode rows expose coherent descriptions and denominators.
- Raster tap/drag and custom semantics actions update the same visible/spoken readout for no data, well, intensity, asleep, and future.
- Light/dark theme, large font, and 12/24-hour formats render without clipped critical content.

### Regression and manual verification

- Phase 1–4 JVM/instrumented suites remain green, including migrations, export retry, erase guard, Full Log edits, Track capture, Settings restoration, and navigation.
- Manual API 36 walkthrough uses a temporary test-only seed or isolated fixture, never production sample-data behavior. Verify range changes, a DST-adjacent fixture, raster exploration by touch and TalkBack/custom action, an assumed ending, hold setting recomputation, rotation, process recreation, and return from Settings.
- Inspect the installed app in both light and dark themes; verify no WebView/JavaScript/chart dependency or new permission is present.

## Acceptance criteria

- [x] SPEC/APPROVAL: the user approved D-1 through D-10 and the complete Phase 5 scope before any application-code edit. — APPROVED 2026-08-03
- [x] MIGRATION: Room 3→4 is additive and non-destructive; 1→4, 2→4, and 3→4 tests preserve records/settings and export schema 4 validates.
- [x] ENGINE: pure tests prove deterministic episode, hold, sleep-union, AUC, clipping, clear-day, tie, DST, and next-invalidation behavior.
- [x] ONSET: ordinary immediate/backdated capture uses the transactional hold-aware classifier; tests prove onset prompts now agree with assumed episode gaps and preserve all existing gates.
- [x] SETTINGS: 8/12/16/24 waking-hour choices persist with targeted writes, default to 16, recompute Insights, survive recreation, export, and erase reset.
- [x] NAVIGATION: Track/Log/Insights and Settings return behavior work and restore without a new navigation framework.
- [x] INSIGHTS: all six ranges drive the summary, facts, recent episodes, and day/hour raster from one immutable projection.
- [x] GRAMMAR: all displayed insight sentences are descriptive restatements with visible denominators; no entry mean, diagnosis, prediction, causal claim, correlation, or significance language exists.
- [x] RASTER: local/DST-safe raster states and pointer readout are correct; no-data, well, 1–10, asleep, and future are textually accessible and not color-only.
- [x] ACCESSIBILITY: tab/range/summary/episode/raster semantics, explicit 48 dp targets, custom TalkBack actions/readout, vertical-scroll ownership, 150% font scaling, and light/dark readability have focused coverage.
- [x] EXPORT: deterministic JSON backup version 4 includes `holdHours`; Phase 4 CSV, retry, cancellation, and export-first erase invariants remain green.
- [x] REGRESSION: all Phase 1–4 unit and connected tests pass unchanged except explicitly superseded onset expectations.
- [x] ORACLES: `./gradlew.bat test`, `lint`, `assembleDebug`, `connectedDebugAndroidTest`, `adb devices -l`, install/launch, and `git diff --check` pass on `MindScale_API_36`.
- [x] MANUAL: API 36 walkthrough verified native touch exploration/readout and vertical scroll, hold selection/recomputation, tested recreation/Settings return, light/dark output, 150% font scaling, and TalkBack-enabled focus/navigation without destructive user-data actions; custom raster actions are additionally invoked by the connected semantics test.
- [x] REVIEW: the critical-path review covered episode math, sleep/hold concurrency, transactional onset classification, DST/range clipping, schema/export migration, lifecycle timers, insight grammar, and accessibility; it fixed clipped carried duration, future sleep-end invalidation, vertical gesture interception, future contrast, duplicate ongoing copy, and 48 dp targets.

## Task decomposition after approval

1. [x] Add pure source normalization, awake-time math, episode derivation, range projection, and exhaustive JVM tests. — UNIT
2. [x] Add `HoldDuration`, migration 3→4, schema 4, targeted mutation, seed/export/erase updates, and migration tests. — MIGRATION, INSTRUMENTED
3. [x] Add `EpisodeSourceDao` and transactional ordinary-capture classifier; reconcile Track and tests. — DAO, CONCURRENCY, REGRESSION
4. [x] Add Insights models/ViewModel with range restoration, lifecycle-aware recomputation, scheduled invalidation, and Retry. — UNIT
5. [x] Add the third destination and Settings return behavior without a navigation dependency. — INSTRUMENTED
6. [x] Add summary, facts, recent episode rows, and accessible empty/loading/error states. — UI, ACCESSIBILITY
7. [x] Add the native day/hour raster, pointer exploration, textual readout, custom semantics actions, theme/time-format support, and focused tests. — UI, ACCESSIBILITY
8. [x] Run the full oracle suite and API 36 manual walkthrough. — LINT-BUILD, MANUAL
9. [x] Run one critical-path architecture/product review and resolve all blocking findings. — REVIEW

## Rollout, migration, and rollback

- Rollout is source merge after every acceptance criterion passes and the user reviews the native result.
- Version 4 requires `MIGRATION_3_4`; no `fallbackToDestructiveMigration`, database deletion, table recreation, or record rewrite is allowed.
- Before public release, rollback is a source-control revert plus a fresh test install. After a version-4 database reaches users, forward-fix is required unless an independently approved/tested 4→3 downgrade migration exists.
- Derived episodes require no data backfill and create no rollback-owned rows.
- Backup format 4 is forward-only for the currently unimplemented importer; raw CSV compatibility is unchanged.

## Decisions and approval gates

- **D-1 (bounded scope, recommended):** Phase 5 ships the episode engine, hold-aware onset reconciliation, third destination, six ranges, summary/facts/recent episodes, and primary raster. Defer step chart, histograms, onset-time description, sleep comparison, Report, Profile, Safety, and breathing.
- **D-2 (derived storage, required):** episodes and raster cells are pure projections; add no Episode table or cached-derived-data migration.
- **D-3 (auto-end conflict, recommended):** follow `SPEC.md`'s mandatory capped hold. Auto-end is always on; do not expose or persist the HTML's disabling switch.
- **D-4 (trend conflict, recommended):** follow `SPEC.md`'s step-function rule. Do not add `rampMin`, smoothed line mode, or interpolation. The trend chart itself remains deferred.
- **D-5 (hold persistence, recommended):** add one `HoldDuration` enum column with 8/12/16/24 choices and a 16-hour default through Room 3→4.
- **D-6 (onset reconciliation, required):** supersede Phase 2 D-3's simplified prior-row rule with transactionally inserted, capture-timestamp-relative, hold/sleep-aware classification from the shared engine.
- **D-7 (range semantics, recommended):** use local calendar-day/month windows ending at now, DST-safe day boundaries, clipped totals, and explicit carried-in episode disclosure.
- **D-8 (source consistency, recommended):** observe Entries and Sleeps through one Room UNION projection; keep manual DI and add no repository/framework.
- **D-9 (backup meaning, required):** Room schema becomes 4 and JSON backup format becomes 4 with numeric `holdHours`; CSV remains raw and unchanged.
- **D-10 (raster accessibility, recommended):** use one native focusable exploration surface with persistent visible/spoken readout and previous/next day/hour accessibility actions; never create thousands of per-cell semantic nodes.

Approval gate is satisfied. D-1 through D-10 are frozen; any material scope or interface change requires a spec amendment before implementation continues.
