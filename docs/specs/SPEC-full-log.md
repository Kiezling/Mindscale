# MindScale Phase 3 — Full Log

Status: IMPLEMENTED

Owner: TBD

Last updated: 2026-08-03

Governing product sources:

- Claude Design project `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`, `MindScale v2.dc.html`, Full Log screen
- Local handoff bundle `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\`, with `MindScale v2.dc.html` as the primary design and `SPEC.md` as product rationale
- The same project's `SPEC.md`, especially Data model, Statistics, Insight grammar, Views, Export, and Safety
- `docs/specs/SPEC-track-numpad-logging.md` and `docs/specs/SPEC-track-phase2-completeness.md`

Approved by the user on 2026-08-03. Implementation must remain within this scope.

## Purpose

Add the first second-screen experience to the native Android app: a complete, locally stored chronological log that lets the user inspect and maintain every recorded rating, sleep interval, and marker. It must make the data legible without interpreting it.

The design source specifically corrects a prior defect: every rating number, including ratings captured with Sleep/Wake, must be visible; rating rows must expose Edit, Note, and Delete; notes must render inline. The native implementation also brings Phase 2's stored `SleepInterval` and `Marker` rows into a user-visible history for the first time.

## Goals

1. Add native Track and Log top-level destinations with accessible bottom navigation.
2. Show every matching `Entry`, `SleepInterval`, and `Marker` in one deterministic, day-grouped timeline.
3. Support inclusive local-date From/To filters plus a one-tap All reset.
4. Let users edit, note, and delete rating entries using the established Track behavior.
5. Let users delete sleep intervals and markers with confirmation.
6. Keep the screen reactive to Room changes and correct across rotation/process restoration.
7. Establish a small provider-neutral application container before a second screen multiplies Activity-owned DAO wiring.

## Non-goals

- No Insights, chart, episode derivation, AUC, time-weighted daily score, trend, prediction, correlation, or clinical interpretation. The approved episode/hold/awake-time model does not exist yet; displaying a guessed `time-weighted` number would violate the product's statistics and interpretation rules.
- No import, CSV/JSON export, clinician report, export-and-delete flow, or destructive database reset. The mockup's bottom Import/Export controls remain deferred to the approved data-actions phase; this screen must not show inert controls.
- No Settings, Profile, Safety card, breathing tool, brand-theme overhaul, behavioral-anchor prompt, or custom chip vocabulary.
- No editing of sleep intervals or markers. The design source exposes Delete only for those rows. Backdatable marker editing can be specified later if requested.
- No navigation framework dependency solely for two top-level destinations. Deep links, nested flows, and a true back stack are not required by this phase.
- No database schema-version change. Phase 3 uses the existing v2 tables.
- No paging dependency. Date-bounded Room streams and Compose lazy rendering are sufficient for the expected local-only history; revisit only with measured evidence.

## User experience

### Top-level navigation

- A Material 3 `NavigationBar` contains `Track` and `Log` only. Unimplemented destinations are not shown.
- Track remains the launch destination.
- Selecting Log shows the Full Log without destroying Track's ViewModel state.
- System Back from Log returns to Track. System Back from Track follows normal Activity behavior.
- The selected destination survives configuration change and process state restoration through `rememberSaveable`; it is UI navigation state, not database state.

### Filter header

- The Log begins with accessible `From` and `To` buttons. Each opens a native Material 3 date picker.
- Either bound may be unset. An unset From means no lower bound; an unset To means no upper bound.
- Both bounds are inclusive local calendar dates. The query boundary for To is the start of the next local date (`toExclusive`), computed with `java.time` in the device's current zone so DST days are handled correctly.
- `All` clears both bounds.
- If From is later than To, show an inline validation message, keep the last valid applied range/results, and disable applying the invalid selection.
- The summary reads `N records · ratings, sleep and events`, where N counts rendered timeline rows, not days and not only ratings.

### Timeline

- Rows are grouped under local-date headings such as `Today`, `Yesterday`, or the locale-formatted date.
- Groups and rows are newest first.
- Every rating row shows its numeric value `0` through `10`; a Sleep/Wake rating also shows an accessible `asleep`/`awake` badge, and a zero shows `ended`.
- Rating chips render as text and notes render inline. No meaning is communicated by color alone.
- Rating rows expose right-aligned Edit, Note, and Delete actions. Edit expands an inline panel beneath that row with 0–10 choices, chip choices, and date/time; Note expands a mutually exclusive inline panel with the existing note and Save/Cancel. This follows the handoff source's Full Log structure instead of inventing a modal layout.
- Selecting a new rating commits it and closes Edit. Chip choices commit independently while Edit remains open. A valid date/time change commits independently while Edit remains open. Note is the only draft requiring explicit Save; Cancel discards it. Editing preserves `kind` and `note`; noting changes only `note`.
- A sleep-interval row uses an accessible sleep indicator, its start time, and either a normalized duration (`8h`, `7h 5m`, `43m`) or `sleeping now` for an open interval. Never render `7h 60m`. It exposes Delete only.
- A marker row uses an accessible event indicator, its time, full marker text, and Delete only.
- Deleting any row requires a confirmation dialog that names the record type and makes permanence clear. Cancel is harmless. Deleting a `SleepInterval` does not silently delete its separate Sleep/Wake rating entries; deleting an Entry does not silently rewrite interval bookkeeping.
- When no rows match, show a calm empty state that distinguishes `No records yet` from `No records in this date range`.
- The footer Import/Export controls from the web mockup are omitted until they are functional and approved.

## Frozen data and interface contracts

### DAO queries

Use nullable epoch-millisecond boundaries. All upper bounds are exclusive.

```kotlin
@Query(
    """SELECT * FROM entries
       WHERE (:fromTs IS NULL OR ts >= :fromTs)
         AND (:toTsExclusive IS NULL OR ts < :toTsExclusive)
       ORDER BY ts DESC, id DESC"""
)
fun observeBetween(fromTs: Long?, toTsExclusive: Long?): Flow<List<Entry>>

@Query(
    """SELECT * FROM sleeps
       WHERE (:fromTs IS NULL OR startTs >= :fromTs)
         AND (:toTsExclusive IS NULL OR startTs < :toTsExclusive)
       ORDER BY startTs DESC, id DESC"""
)
fun observeBetween(fromTs: Long?, toTsExclusive: Long?): Flow<List<SleepInterval>>

@Query(
    """SELECT * FROM markers
       WHERE (:fromTs IS NULL OR ts >= :fromTs)
         AND (:toTsExclusive IS NULL OR ts < :toTsExclusive)
       ORDER BY ts DESC, id DESC"""
)
fun observeBetween(fromTs: Long?, toTsExclusive: Long?): Flow<List<Marker>>

// SleepDao
@Query("SELECT COUNT(*) FROM sleeps")
fun observeCount(): Flow<Int>

// MarkerDao
@Query("SELECT COUNT(*) FROM markers")
fun observeCount(): Flow<Int>
```

`EntryDao.observeCount()` already exists; the two new count streams let `hasAnyRecords` distinguish a genuinely empty database from an empty filtered result. Sleep intervals belong to the day/range containing `startTs`; an interval that began before From is not duplicated into later days merely because it overlaps them.

Add targeted Entry mutations and use them from both Track and Log so one operation cannot revert unrelated columns from a stale full-row snapshot:

```kotlin
@Query("UPDATE entries SET ts = :ts, value = :value, chips = :chips WHERE id = :id")
suspend fun updateEditableFields(id: Long, ts: Long, value: Int, chips: List<String>): Int

@Query("UPDATE entries SET note = :note WHERE id = :id")
suspend fun updateNote(id: Long, note: String?): Int

@Query("DELETE FROM entries WHERE id = :id")
suspend fun deleteById(id: Long): Int

@Query("DELETE FROM sleeps WHERE id = :id")
suspend fun deleteById(id: Long): Int

@Query("DELETE FROM markers WHERE id = :id")
suspend fun deleteById(id: Long): Int
```

Each mutation returns the affected-row count. A result of 0 is a recoverable stale-selection condition: close the dialog, refresh from Room, and show a brief `That record no longer exists` message. It must not crash or recreate a deleted record.

### Timeline model

```kotlin
sealed interface LogItem {
    val stableKey: String
    val timestamp: Long

    data class Rating(val entry: Entry) : LogItem {
        override val stableKey = "entry:${entry.id}"
        override val timestamp = entry.ts
    }

    data class Sleep(val interval: SleepInterval) : LogItem {
        override val stableKey = "sleep:${interval.id}"
        override val timestamp = interval.startTs
    }

    data class Event(val marker: Marker) : LogItem {
        override val stableKey = "marker:${marker.id}"
        override val timestamp = marker.ts
    }
}

data class LogDay(
    val date: LocalDate,
    val items: List<LogItem>
)

data class LogFilter(
    val from: LocalDate? = null,
    val to: LocalDate? = null
)

data class LogUiState(
    val appliedFilter: LogFilter = LogFilter(),
    val pendingFilter: LogFilter = LogFilter(),
    val days: List<LogDay> = emptyList(),
    val recordCount: Int = 0,
    val hasAnyRecords: Boolean = false,
    val filterError: String? = null,
    val editEntry: Entry? = null,
    val noteEntry: Entry? = null,
    val deleteTarget: LogItem? = null,
    val message: String? = null
)
```

`stableKey` is namespaced (`entry:<id>`, `sleep:<id>`, `marker:<id>`). Unified sorting is by `timestamp DESC`; ties are deterministically ordered Rating, Event, Sleep, then `id DESC`. This intentionally places a Sleep/Wake rating immediately before its same-timestamp interval bookkeeping row.

### Application container

Introduce no DI framework. A minimal process-scoped container is frozen as:

```kotlin
interface AppContainer {
    val database: MindScaleDatabase
}

class DefaultAppContainer(context: Context) : AppContainer

class MindScaleApplication : Application {
    val container: AppContainer
}
```

The manifest names `MindScaleApplication`. `MainActivity` obtains one database from the container and constructs Track/Log ViewModels with explicit factories. Tests may supply fakes directly to ViewModels; they do not need the Android application container.

### Shared entry editor

Reusable Entry editing controls must accept immutable state plus callbacks; they do not own DAOs or launch persistence work. Track and Log may have separate ViewModels, but both must call the targeted DAO mutations above. Shared value/chip/time/note controls may be presented in the Track screen's existing surface and the Log's required inline surface; do not copy a second subtly different validation or persistence implementation.

## Invariants

1. The Full Log never invents, infers, diagnoses, predicts, or labels a pattern.
2. All records remain local in the existing Room database.
3. An empty filter means all records; a bound is interpreted in the device's current timezone as a local calendar-day boundary.
4. Every included Room row produces exactly one timeline row. Sleep/Wake rating Entries and SleepIntervals are distinct stored facts and therefore distinct rows.
5. Unified order is deterministic even when timestamps collide.
6. Every Entry value, including 0, Sleep, and Wake entries, is visibly rendered as a number.
7. Entry edit/note operations modify only their declared columns and preserve unrelated fields.
8. Deleting one row never cascades to a different table in this phase.
9. Invalid pending date bounds never replace the last valid applied query.
10. A deleted/stale target is handled as a recoverable message, never an insert or crash.
11. Open sleep intervals render without fabricating an end time or duration.
12. Duration formatting carries minutes into hours and never prints 60 minutes.
13. The LazyColumn uses stable, namespaced keys and does not use list index as identity.
14. Configuration/process recreation restores selected destination, pending/applied filter, and any open dialog draft required to continue safely. Persist only primitive restoration values (epoch days, record type/id, text, numeric value, chip strings) in `SavedStateHandle` or saveable state; do not place Room entities in saved state and do not persist UI-only state in Room.
15. Track behavior and Phase 1/2 data integrity remain unchanged except for the safer targeted Entry mutation implementation.
16. No new database migration, navigation framework, paging library, DI framework, or system Gradle installation is introduced.

## Failure behavior

- Room read failure: keep the screen available, show a non-alarming error with Retry, and do not imply records were lost.
- Mutation failure: keep the user's dialog/draft open when retry is meaningful; show a concise failure message; never optimistically claim success.
- Stale affected-row count 0: close the stale dialog and tell the user the record no longer exists.
- Invalid date range: show inline validation and retain last valid results.
- Date/time conversion edge: use `ZoneId.systemDefault()` and half-open epoch ranges; tests must include a DST transition zone.
- Open sleep interval: show `sleeping now`; no duration arithmetic against an assumed wake.

## Accessibility and Android compatibility

- Continue min SDK 26, target SDK 36, compile SDK 36.1, Kotlin, Compose, Material 3, Kotlin DSL, bundled JDK, and the project Gradle wrapper.
- Use existing dependencies unless an approved spec revision proves a concrete need.
- All navigation items, filter controls, row types, state badges, and row actions have distinct accessible names and at least 48 dp touch targets.
- A row's spoken description includes type, value/text or duration, local date/time, chips, note, and Sleep/Wake/ended state as applicable.
- Preserve Dynamic Type/font scaling, dark theme, keyboard navigation, and screen-reader traversal order.
- Do not rely on color, glyph shape, or position alone.

## Acceptance criteria

- [x] DAO instrumented tests prove each `observeBetween` query handles unbounded, lower-only, upper-only, and both-bound ranges; boundary equality follows `[fromTs, toTsExclusive)`; tie ordering is deterministic. — INSTRUMENTED
- [x] DAO instrumented tests prove targeted Entry mutations preserve unrelated columns and report 0 for a missing id; Sleep/Marker deletes affect only their own table. — INSTRUMENTED
- [x] Unit tests prove ratings, sleep intervals, and markers combine into deterministic newest-first `LogDay` groups with namespaced stable keys and the specified tie precedence. — UNIT
- [x] Unit tests prove local-date boundaries across a DST transition and prove an interval is filtered/grouped by `startTs`. — UNIT
- [x] Unit tests prove duration formatting for 43m, 6h, 7h 59m, 8h, and an open interval; `60m` never appears. — UNIT
- [x] Unit tests prove invalid pending From/To retains the prior applied filter/results and exposes an error. — UNIT
- [x] Unit tests prove each edit/note/delete event calls only the targeted DAO mutation and handles affected-row count 0 without inserting or crashing. — UNIT
- [x] UI tests prove Track is the launch destination, Log navigation works, Back from Log returns to Track, and the selected destination survives recreation. — INSTRUMENTED
- [x] UI tests render a mixed same-day group containing ordinary 0–10 values, Sleep/Wake rating badges, inline chips/note, an open and closed sleep interval, and a marker; every required value/action has accessible semantics. — UI-ACCESSIBILITY
- [x] UI tests prove From/To/All controls, validation, all-record and filtered-empty states, delete confirmation/cancel, mutually exclusive inline Edit/Note panels, Edit's immediate value/chip/time commits, and Note Save/Cancel. — INSTRUMENTED
- [x] Existing Track unit and instrumented tests remain green after shared editor/targeted-mutation refactoring. — REGRESSION
- [x] `./gradlew.bat test`, `lint`, `assembleDebug`, and `connectedDebugAndroidTest` all pass on `MindScale_API_36`. — LINT-BUILD
- [x] Manual API 36 walkthrough with seeded mixed data verifies navigation, all 0–10 numbers, Sleep/Wake badges, notes/chips, normalized sleep duration, marker, date filtering, edit/note/delete, rotation, and process recreation. — MANUAL

## Task decomposition

1. Add and test bounded observable DAO queries plus targeted mutation/delete queries. No schema bump. — INSTRUMENTED
2. Add pure filter-boundary, duration-formatting, unification, sorting, and local-day-grouping functions with unit tests. — UNIT
3. Add `LogItem`, `LogDay`, `LogFilter`, `LogUiState`, events, and `LogViewModel`; restore filters/dialog drafts safely. — UNIT
4. Extract the existing Entry edit/note/delete UI into shared state-driven composables and move Track to targeted DAO mutations without changing its behavior. — REGRESSION
5. Implement `LogScreen`/`LogRoute` with filters, summary, lazy day groups, mixed rows, empty/error states, and confirmation dialogs. — INSTRUMENTED, UI-ACCESSIBILITY
6. Add the minimal `AppContainer`/`MindScaleApplication`; wire explicit Track and Log factories. — LINT-BUILD
7. Add saveable Track/Log top-level navigation and Back behavior without adding Navigation Compose. — INSTRUMENTED
8. Run the full oracle suite and manual emulator walkthrough. — LINT-BUILD, MANUAL
9. Run one critical-path architecture review focused on cross-table ordering, date boundaries, stale-row mutations, state restoration, and Track regressions. — REVIEW

## Rollout and rollback

- Database version stays at 2; no migration or destructive fallback is allowed.
- Rollout is source merge into the next debug/release build after all acceptance criteria pass.
- Rollback is source-control revert. Because this phase does not alter persisted schema, rollback to the Phase 2 commit does not require a database migration.

## Decisions and approval gates

- **D-1 (Full Log contents): decided.** Render all three existing record types. Entries, interval bookkeeping, and markers are separate facts; showing only Entries would leave Phase 2 data invisible.
- **D-2 (daily statistics): deferred.** Omit the mockup's `time-weighted` day header until the approved awake-time/hold/episode model exists. A guessed metric would be worse than no metric.
- **D-3 (Import/Export): deferred.** Omit inert buttons. Raw CSV/JSON remains a product requirement, but implementation belongs with the already-deferred data-actions work.
- **D-4 (navigation): decided.** Use saveable two-destination state, not a new navigation dependency. Revisit when a nested/back-stack requirement exists.
- **D-5 (application container): decided.** Add a minimal process-scoped container now because two ViewModels need the same database and MainActivity already manually wires four DAOs. Do not add Hilt/Koin or a repository layer.
- **D-6 (date semantics): decided.** Inclusive local dates are converted to half-open epoch ranges. Sleep rows are owned by `startTs`.
- **D-7 (delete isolation): decided.** Delete exactly the selected row. Cross-table repair or cascade behavior requires a separate explicit data-integrity design.
- **D-8 (Entry mutations): decided.** Replace full-row writes on user edit/note paths with affected-row-count targeted queries to prevent stale snapshots from reverting unrelated fields.
- **D-9 (visual scope): decided — option A.** Reproduce the Full Log's hierarchy, density, alignment, and interaction layout using the existing native Material 3 theme. Defer the shared gold/ink light/dark token foundation until the later global brand phase, when it can be applied consistently to every screen. Do not implement a one-screen-only pseudo-theme.
- **D-10 (native safety divergence): decided.** The prototype deletes immediately, but the already-approved native Track contract requires confirmation for permanent deletion. Full Log keeps confirmation for all record types; this is an intentional native safety improvement, not a missed visual detail.

Approval recorded: the user approved D-1 through D-10 and the complete Phase 3 scope on 2026-08-03.

## Implementation and verification record

Implemented and verified on 2026-08-03.

- Added the mixed, date-filterable Full Log; Track/Log bottom navigation; saveable navigation/filter/draft state; the process-scoped app container; bounded DAO streams; and targeted mutations without changing Room schema version 2.
- Critical-path review fixed three issues before sign-off: Retry originally could not restart a failed Room stream; merged row semantics could absorb distinct action semantics; and note/edit/delete UI could close before Room confirmed a successful mutation. Empty-state selection was also corrected to distinguish an empty database from an empty filtered range.
- Final JVM suite: 80 tests, 0 failures, 0 errors, 0 skipped.
- Final API 36 suite: 51 instrumented tests, 0 failures, 0 skipped on `MindScale_API_36(AVD) - 16` (`emulator-5554`).
- `lint` passed with 0 errors and 22 pre-existing template/dependency-version warnings; `assembleDebug` passed; `git diff --check` passed with line-ending notices only.
- Manual walkthrough used a temporary one-shot instrumentation seeder that was removed immediately afterward. It verified the installed app with five mixed rows, correct ordering, chips/note, marker, Sleep badge, normalized `1h 30m` interval, inline rating edit persistence, accessible distinct actions, native From date picker, rotation retention, clean process relaunch, and Back-to-Track behavior. The emulator's stylus handwriting panel intercepted adb-injected note keystrokes, so note Save/Cancel persistence is evidenced by the passing Compose and ViewModel tests rather than claimed as a successful adb text-entry gesture.
