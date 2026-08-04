# MindScale Phase 6 — Insights entry chart

Status: IMPLEMENTED — VERIFIED LOCALLY

Owner: Codex

Date: 2026-08-04

Approval: The user explicitly approved Phase 5 and the next bounded phase, including implementation, verification, commit, and push.

Governing product sources:

- `docs/specs/SPEC-insights-foundation.md`, especially the frozen step-function, range, hold, sleep, grammar, restoration, and accessibility rules
- Local Claude Design handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html`, especially the `What you recorded` chart, sleep bands, event lines, pointer readout, and range behavior
- The handoff `SPEC.md`, especially Axioms, Statistics, Insight grammar, Views, and Data model
- `docs/specs/SPEC-settings-data-control.md`, especially hidden-note previews and local-only data control

## Purpose

Add the next bounded Insights view: a native, entry-level step chart that shows the effective recorded state over the selected range, pauses the line during sleep, and locates user-recorded events. It must reuse the Phase 5 derivation semantics rather than create a second interpretation of ratings.

## Product-source reconciliation

Already implemented:

- Phase 5 provides the third Insights destination, six shared local-calendar ranges, a deterministic hold/sleep-aware episode engine, reactive Room source projection, restoration, time invalidation, and accessible Canvas exploration.
- Entries already retain timestamps, values, chips, notes, and optional Sleep/Wake kinds; sleeps and free-text markers are raw local Room records.
- `hideNotes` already controls note previews in Track and Full Log.

Conflicts resolved for this phase:

1. The HTML defaults to a smoothed `Line` mode and exposes a `Line/Steps` choice plus a ramp duration. The governing product `SPEC.md` says the trend must be a step function and Phase 5 D-4 froze that rule. Native Phase 6 is step-only and adds no chart-mode or ramp setting.
2. The HTML uses fixed millisecond range arithmetic. Native rendering uses the Phase 5 local-calendar range start and an epoch timeline ending at injected `now`, preserving 23/25-hour local days.
3. The HTML tooltip is pointer-hover shaped. Native Phase 6 uses a persistent visible readout driven by tap, horizontal drag, and accessibility actions; no information is hover-only.
4. The HTML chart source does not guarantee the same transactional snapshot as episodes. Native Phase 6 expands the existing Room UNION projection to include marker and entry-detail columns, so entries, sleeps, and markers arrive in one reactive SQLite read.
5. The HTML can reveal note previews regardless of the native privacy preference. Native chart readout suppresses note text when `hideNotes` is enabled while retaining the stored note and other chart facts.

## Goals

1. Add one `What you recorded` chart below the raster and above episode facts.
2. Draw only piecewise-constant states: horizontal holds and vertical changes, with no interpolation.
3. Use the exact Phase 5 same-timestamp, awake-time hold, explicit-zero, assumed-end, future-fact, and sleep-union rules.
4. Apply the selected `1D`, `3D`, `7D`, `30D`, `90D`, or `6M` range to the chart, its overlays, ticks, and exploration.
5. Draw normalized sleep as background bands and stop the state line while asleep.
6. Draw in-range markers as dotted event lines and make their timestamp/text discoverable without precise touch.
7. Show the source rating timestamp, value, chips, and permitted note preview for the explored effective state.
8. Keep the implementation native, local, deterministic, lifecycle-aware, and dependency-free beyond the existing stack.

## Non-goals

- No smoothed line, curve, ramp, mode toggle, interpolation, rolling average, entry-weighted mean, or new setting.
- No histogram, onset-time chart, sleep comparison, correlation, trigger detection, prediction, causal language, or significance claim.
- No editing, deleting, or creating records from the chart.
- No persisted chart points, Episode entity, derived cache, Room migration, backup-format change, CSV change, or data backfill.
- No Report/Profile/Safety/breathing/import work.
- No WebView, JavaScript, bitmap screenshot, chart library, navigation framework, DI framework, background worker, account, server, analytics, or permission.
- No toolchain, SDK, Gradle, AGP, Kotlin, Compose, Java, or dependency upgrade.

## User experience

### Placement and shared range

- `What you recorded` appears in the existing Insights list after `Days and hours` and before `Episodes`.
- The existing range chips remain the only range control. A change recomputes the raster, chart, summaries, facts, and episode rows from the same immutable source snapshot and clears both exploration selections.
- Empty/loading/error behavior remains owned by the existing Insights state. No chart shell appears before the first Entry. If entries exist outside the selected range, the chart may show an honest empty baseline/readout while the page retains `No ratings in this range`.

### Chart rendering

- Horizontal time maps linearly from `rangeStartMillis` to `nowMillis`; vertical position maps integer intensity 0–10 with labeled grid lines at 0, 5, and 10.
- Before the first Entry, no state line is drawn. It is not treated as zero.
- After measurement begins, effective zero is drawn at 0, including silence after an explicit zero or assumed hold expiry.
- Positive states use the Phase 5 hold cap. Each later Entry changes the line immediately; a positive hold expiry drops the line to zero.
- Same-timestamp Entries collapse to the highest id before charting, matching episode/onset behavior.
- The line is absent within normalized sleep spans. A sleep band remains visible behind the gap. Touching/overlapping raw sleeps appear as one band.
- Sleep/Wake-kind Entries remain ordinary rating state changes; the separate SleepInterval controls only the sleep band/gap.
- Markers within `[rangeStart, now)` render as dotted vertical lines in deterministic `(timestamp, id)` order.
- Four time ticks are based on actual instants. `1D`/`3D` favor time labels; longer ranges favor local dates. DST changes never assume fixed-length calendar days.
- The legend has adjacent text for `recorded intensity`, `asleep`, and `event`.
- The caveat reads: `Ratings stay flat until another rating or the waking-hour limit. The line stops during sleep. Dotted lines are events you marked.` The selected hold is stated in the readout/detail or caveat without implying diagnosis.

### Exploration and readout

- Default visible copy is `Touch or drag to read the chart`.
- Tap or predominantly horizontal drag selects an instant and updates a persistent live-region readout. Vertical swipes remain owned by the Insights list.
- The readout always includes local date, local time, and UTC offset. It then says one of: `no data yet`, `asleep`, `nothing recorded`, or `intensity N`.
- When an effective state comes from an Entry, the readout includes `recorded <date/time>` plus its chips in stored order. It includes the note preview only when `hideNotes` is false.
- When the selection is exactly an event timestamp, the readout includes every marker there in id order. Touch hit-testing may snap to the nearest visible event line within a bounded visual target; it may not associate a marker merely because it occurred nearby in time.
- Chart exploration survives Activity/process recreation as one epoch-millisecond primitive. It is separate from raster exploration so reading one chart does not rewrite the other chart's readout.

## Frozen source and model contracts

### Unified Room projection

`EpisodeSourceRow` expands additively at the Kotlin projection layer:

```kotlin
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
```

- `observeSource()` and `sourceAtOrBefore()` UNION `ENTRY`, `SLEEP`, and `MARKER` rows with identical columns and `ORDER BY ts ASC, recordType ASC, id ASC`.
- Entry rows project `note`; marker rows project `text`; unrelated columns are null.
- The query mentioning all three tables makes any entry/sleep/marker insert, targeted edit, or delete invalidate the same Flow.
- `sourceAtOrBefore()` includes markers for interface consistency. Onset classification ignores valid markers and remains transactionally coupled only to the inserted Entry/settings snapshot.
- Allowed discriminators are exactly `ENTRY`, `SLEEP`, and `MARKER`. Unknown types fail derivation visibly.
- Existing database tables and columns do not change. Room remains version 4 and schema JSON 4 remains byte-for-byte unchanged.

### Chart projection

The pure engine adds immutable chart models equivalent to:

```kotlin
data class EntryChartSegment(
    val startMillis: Long,
    val endMillis: Long,
    val value: Int,
    val sourceEntryId: Long?,
    val sourceEntryMillis: Long?,
    val chips: List<String>,
    val note: String?
)

data class EntryChartSleep(val startMillis: Long, val endMillis: Long)
data class EntryChartMarker(val id: Long, val atMillis: Long, val text: String)

data class EntryChart(
    val startMillis: Long,
    val endMillis: Long,
    val segments: List<EntryChartSegment>,
    val sleeps: List<EntryChartSleep>,
    val markers: List<EntryChartMarker>
)
```

- All chart items are clipped to `[rangeStart, now)`; source Entry timestamps may precede the range so a carried state can be described honestly.
- Positive segments retain their effective source Entry metadata. Explicit-zero baseline segments retain the zero Entry metadata. Baseline created by assumed expiry has null source metadata.
- Segments are half-open, ordered, non-overlapping, positive-duration, and exclude sleep. Adjacent segments may merge only when value and all source metadata are equal.
- Sleep bands are normalized, ordered, non-overlapping, positive-duration, and clipped.
- Markers are instantaneous, ordered by `(atMillis, id)`, clipped, and never alter intensity, episodes, AUC, or sleep arithmetic.
- `InsightsSnapshot` owns one `entryChart` derived from the same `BuiltModel` used by every Phase 5 output.

### UI state and actions

`InsightsUiState` adds `chartExploredInstantMillis: Long?`. `InsightsViewModel` persists it in `SavedStateHandle`, clamps it to the current chart window, clears it on range change, and adds deterministic actions for:

- earlier/later elapsed hour;
- previous/next effective Entry change;
- previous/next marker event.

Actions return `false` only when no snapshot/target exists. They select exact source/event instants and update the same visible/spoken readout used by touch.

## Invariants

1. One source snapshot, hold, `now`, zone, and range produce one deterministic raster/chart/episode snapshot.
2. Chart intensity never disagrees with Phase 5 episode state at the same awake instant.
3. No state exists before the first effective Entry; unlogged awake time after measurement begins is zero.
4. Every positive chart hold uses configured awake hours; sleep consumes no hold time.
5. The state line never crosses sleep visually or arithmetically.
6. A marker is context only. It never changes an episode boundary, chart value, or displayed causal claim.
7. Same-timestamp Entry ties use highest id; same-timestamp markers remain separately visible in id order.
8. The chart is step-only. There is no stored or runtime path to interpolation.
9. Range clipping cannot mutate or truncate raw Room records.
10. Hidden notes remain stored and editable; only the chart preview is suppressed.
11. Selection is epoch-based, preserving repeated-hour DST identity.
12. Pointer handling must not prevent normal vertical scrolling.
13. No displayed string diagnoses, predicts, attributes cause, or claims an association.
14. No Room schema, backup, CSV, toolchain, permission, dependency, or architecture change is introduced.

## Failure behavior

- Missing/invalid Entry value, invalid sleep, missing marker text column, or unknown discriminator uses the existing visible derivation error/Retry path and does not rewrite raw data.
- A blank persisted marker is rendered as `event` rather than discarded or repaired; the raw blank remains available in Full Log.
- If chart derivation fails after a successful snapshot, keep the last immutable snapshot with the existing stale/error banner.
- If a selected source record is later edited/deleted, recomputation keeps the selected instant clamped but derives fresh readout metadata; it never shows stale note/chip/event text.
- Pointer positions outside bounds clamp to the chart window. Empty target lists make previous/next accessibility actions unavailable without moving selection.

## Accessibility and Android compatibility

- Continue native Kotlin, Jetpack Compose, Material 3, Kotlin DSL, min SDK 26, target SDK 36, compile SDK 36.1, bundled JDK, and Gradle wrapper.
- The chart is one focusable semantic surface at least 48 dp high; decorative Canvas primitives are hidden from the semantics tree.
- Its content description is `Recorded intensity step chart`; state description is the visible readout.
- Custom actions expose earlier/later hour, previous/next rating, and previous/next event. Action labels distinguish this chart from raster actions.
- The visible readout is a polite live region and contains every non-color fact needed to interpret the selection.
- Grid labels, legend, caveat, and readout remain readable at 150% font scaling without being painted into a bitmap.
- Intensity line/area, sleep bands, dotted event lines, and selection crosshair differ by geometry/pattern as well as color in light/dark and grayscale conditions.
- Touch event snapping uses a physical-size threshold; precise pointer placement is never the only way to read an event because accessibility actions can select each event.
- Chart pointer handling recognizes taps and horizontal drags while yielding vertical motion to the parent list.
- Existing range/tab/Settings navigation and edge-to-edge behavior remain unchanged.

## Test strategy

### Pure unit tests

- Step projection before first Entry, explicit zero, positive changes, assumed hold drop, ongoing state, and carried-in state.
- Same-timestamp highest-id Entry metadata and same-timestamp marker id ordering.
- Sleep splits chart segments and normalized bands never overlap/double-render.
- Entry kinds participate as values; markers never affect episodes or AUC.
- Range clipping for segments, sleeps, markers, and out-of-range source metadata.
- Future entries/sleeps/markers remain excluded and schedule the existing next invalidation.
- DST spring/fall range mapping remains instant-correct.
- Chart lookup returns no-data/asleep/well/intensity plus exact marker/source metadata.

### Room/instrumented tests

- The unified source emits Entry note, Sleep, and Marker text in deterministic order and reacts to mutation/deletion in all three tables.
- Transactional ordinary capture remains green with markers present.
- Database version remains 4; migration/schema/export tests remain unchanged and green.

### ViewModel tests

- Chart selection saves/restores independently of raster selection and clears on range change.
- Earlier/later hour and previous/next rating/event actions clamp and select exact instants.
- Source edits/deletes recompute readout data without stale metadata.
- `hideNotes` updates immediately without modifying snapshot records.

### Compose/UI tests

- Chart placement, title, axes/legend/caveat, and empty/range/error integration.
- Tap and horizontal drag update the chart callback/readout; vertical swipe scrolls past the chart.
- Semantic state and every custom action use the one focusable chart surface.
- Hidden-note mode omits note preview; chips and event text remain available.
- 12/24-hour readout includes UTC offset; light/dark and 150% font retain critical content.

### Full oracles and manual verification

- Run `./gradlew.bat test`, `lint`, and `assembleDebug`.
- Run `adb devices -l` and `connectedDebugAndroidTest` on the intended API 36 emulator.
- Install/launch and manually inspect populated and empty chart states, range changes, touch exploration, vertical scrolling, event discovery, sleep gaps, note privacy, light/dark, 150% font, and TalkBack custom actions.
- Run `git diff --check` and confirm no schema JSON, permission, dependency, WebView, or JavaScript change.

## Acceptance criteria

- [x] SPEC: this approved spec and D-1 through D-8 were recorded before application-code edits.
- [x] SOURCE: one reactive Room projection supplies Entries/Sleeps/Markers including Entry note and Marker text; transactional onset behavior remains green.
- [x] ENGINE: pure tests prove step, source metadata, hold, sleep-gap, marker, range, future, tie, and DST behavior.
- [x] UI: one native step chart renders labeled 0/5/10 axes, step state, sleep bands, event lines, ticks, legend, caveat, and persistent readout.
- [x] RANGE: all six existing range choices drive the chart from the same immutable snapshot and clear chart exploration.
- [x] PRIVACY: `hideNotes` suppresses chart note previews without mutating data or hiding chips/events.
- [x] ACCESSIBILITY: the chart is a single coherent focus target with visible/live state and working time/rating/event actions; vertical scroll, 48 dp minimum, large font, light/dark, and non-color geometry pass.
- [x] PERSISTENCE: Room stays at schema 4, JSON stays format 4, CSV stays unchanged, and no destructive migration exists.
- [x] REGRESSION: all Phase 1–5 unit/instrumented tests remain green.
- [x] ORACLES: wrapper test/lint/assemble, connected tests, device identity, launch, and diff check pass.
- [x] REVIEW: final review covered step/hold consistency, sleep splitting, source snapshot, range/DST, note privacy, gesture ownership, semantics, and grammar; the only test corrections were stale scroll assumptions and a drag gesture that began in the y-axis gutter.

## Decisions

- **D-1 (bounded scope):** Phase 6 adds only the step chart with sleep/event overlays and exploration. Histogram, onset-time, sleep-comparison, and Report work remain deferred.
- **D-2 (step-only):** follow the product `SPEC.md` and Phase 5 D-4. Do not implement the HTML's default smoothed line, mode toggle, or ramp.
- **D-3 (single derivation):** chart state is projected inside the Phase 5 engine from the same normalized Entries/Sleeps/hold model; no parallel chart interpretation is allowed.
- **D-4 (source consistency):** extend `EpisodeSourceDao`'s UNION with marker/note/text projection instead of combining independent Flows or adding a repository.
- **D-5 (persistence):** Room and backup remain version 4. The change is query/model/UI-only and creates no stored derived data.
- **D-6 (privacy):** chart note preview obeys the existing `hideNotes` setting; chips and marker text remain visible because they are distinct first-class record context.
- **D-7 (accessibility):** one Canvas semantic surface, persistent visible/live readout, and time/rating/event custom actions replace hover and per-point semantic nodes.
- **D-8 (stacked delivery):** because Phase 5 is pushed but not merged, `agent/phase6-entry-chart` is based on Phase 5 publication head `c8a7bdb17a12aba58dba59927ffc81506d5b42dc`; review/merge must preserve that dependency.

Approval gate is satisfied by the user's explicit authorization for the next phase. D-1 through D-8 are frozen; a material interface or scope change requires a documented spec amendment before implementation continues.
