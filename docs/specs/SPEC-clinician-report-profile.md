# MindScale Phase 11 — clinician summary and Profile foundation

Status: **IMPLEMENTED — VERIFIED LOCALLY**

Owner: Codex under the user's full Phase 11 delegation

Date: 2026-08-04

Starting commit: `9d8cf4bd2dbd3834e2094c81f63e838d6348bdb5`

Approval: On 2026-08-04, the user granted full Phase 11 ownership and authorized product decisions, specification, implementation, verification, commits, pushes, PR operations, merge, and final synchronization without another routine review gate. D-1 through D-12 below are frozen before application-code edits.

## Purpose

Add a bounded, factual clinician summary that helps a user discuss their own MindScale records without turning MindScale into a clinical assessment or interpretive tool. Add the smallest Profile foundation needed to hold an optional local display name and optional totals obtained from PHQ-8 or GAD-7 assessments completed elsewhere. Keep every record and derivation local unless the user explicitly copies, shares, or saves an export.

## Product-source reconciliation

The handoff calls the clinician one-pager a 60-second “boundary object,” prioritizes course, marked events, episode structure, time-of-day counts, sleep, and function/interference, and permits optional PHQ-8/GAD-7 totals only when entered from elsewhere. The prototype also contains recent-versus-prior “higher/lower,” “symptom-free,” post-wake sleep comparisons, and clinical-sounding score presentation; those are not governing behavior because they conflict with MindScale's measurement-only role and prior frozen Decisions D-005 through D-010.

Phase 11 therefore:

- uses the existing descriptive episode, onset-time, and sleep-count contracts without a new classifier;
- labels the output **Clinician summary**, not evidence, a diagnosis, or an assessment;
- treats “one page” as a deliberately bounded plain-text/scrolling summary, because a text file cannot guarantee one physical printed page;
- omits function/interference because MindScale has no direct source field for it and must not infer it;
- omits “since last visit” because no visit anchor exists; the user selects one of the existing factual Insights windows;
- omits every comparison, threshold, severity label, direction-of-effect statement, recommendation, questionnaire item, and score calculation.

The original PHQ-8 and GAD-7 literature defines possible total ranges of 0–24 and 0–21. Those bounds are validation only: MindScale neither presents the items nor adds item responses.

## Goals

1. Add restorable Profile and Report overlays without a navigation framework.
2. Store an optional display name and externally obtained, user-entered PHQ-8/GAD-7 totals with required assessment date and explicit provenance.
3. Produce one deterministic, capped, non-inferential clinician summary from an immutable local snapshot.
4. Support explicit Copy, Share, and Save-as-text actions with clear privacy warnings and no storage permission.
5. Migrate Room and JSON backup additively; make erase/reset atomic and complete for the new data.
6. Preserve accessibility, local-only operation, current toolchain, and all previous behavior outside the frozen navigation adjustment.

## Non-goals

- No PHQ-8/GAD-7 questions, administration, response capture, arithmetic, scoring, interpretation, thresholds, severity bands, trends, alerts, recommendations, or diagnostic/treatment claims.
- No PHQ-9, self-harm item, custom instrument, score graph, automated reminder, recurring schedule, or score import.
- No account, sign-in, server, sync, analytics, telemetry, cloud backup, clinician portal, network request, or new permission.
- No JSON/CSV import or restore. The later import backlog must explicitly support backup version 5 before it can mutate Room.
- No Safety card, paced breathing, function/interference model, visit model, PDF/printing engine, or guaranteed physical pagination.
- No new navigation, DI, serialization, chart, sharing, or date-picker dependency; no unrelated toolchain change.
- No change to rating, episode, onset, night/nap, hold, clear-day, or sleep source semantics.

## Frozen navigation and ownership

- `AppDestination` adds `PROFILE` and `REPORT`. Track, Full Log, and Insights remain the only bottom destinations.
- The top action on Track, Full Log, and Insights becomes `Profile`; Settings remains available from Profile and from existing contextual settings links.
- Profile owns the optional name, externally obtained totals, the Clinician summary entry point, and the Settings entry point.
- Insights adds one `Clinician summary` action. Both Profile and Insights open Report.
- Report uses the single `InsightRange` currently owned and restored by `InsightsViewModel`; it exposes the same six fixed range choices and changing Report's choice changes the shared Insights choice. The default remains 30 days.
- A saveable primitive destination stack owns overlay navigation. `Insights → Report → Back` returns to Insights; `Profile → Report → Back` returns to Profile; `Profile → Settings → Back` returns to Profile; a second Back returns to the originating bottom destination. Back from bare Log/Insights still returns to Track.
- Bottom navigation is hidden on Profile, Report, and Settings. No inert or dead Profile/Report control is shown.

## Frozen persistence contracts

### Room schema version 5

Add exactly these entities:

```kotlin
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    val displayName: String = ""
)

enum class ExternalInstrument { PHQ_8, GAD_7 }
enum class ExternalScoreProvenance { EXTERNALLY_OBTAINED_USER_ENTERED }

@Entity(
    tableName = "external_scores",
    indices = [Index(value = ["instrument", "assessedEpochDay"], unique = true)]
)
data class ExternalScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instrument: ExternalInstrument,
    val total: Int,
    val assessedEpochDay: Long,
    val provenance: ExternalScoreProvenance =
        ExternalScoreProvenance.EXTERNALLY_OBTAINED_USER_ENTERED,
    val enteredAt: Long
)
```

- `user_profile` has exactly one canonical row with `id = 0`, seeded on fresh creation and migration.
- `MIGRATION_4_5` creates both tables, the unique `(instrument, assessedEpochDay)` index, and the canonical empty Profile row. It drops, rewrites, or reclassifies no version-4 row.
- Room converters persist enum names exactly. Unknown/corrupt enum text fails the read; the UI reports a non-destructive read error.
- Production mutations are targeted by canonical profile id or external-score id. Affected-row count is checked; stale ids do not report success.

### Name validation

- Display name is optional, trimmed, single-line, and at most 80 Unicode code points. Blank saves as the empty string.
- Control characters and embedded line breaks are rejected. Initials are derived in memory from at most the first two nonblank words and are never stored.
- Exact privacy copy: `Stored only on this device. Exports you create can contain your name.`

### External-total validation and provenance

- Each row represents exactly one instrument total obtained outside MindScale: `PHQ-8` total 0–24 or `GAD-7` total 0–21.
- Instrument, integer total, and ISO local assessment date are required. The date must parse as a real `LocalDate` and must not be later than today in the device's current zone.
- Exactly one row may exist for an instrument on an assessment date. A duplicate is rejected with retained drafts and a link/action to edit the existing row; it is never silently overwritten.
- `enteredAt` is the local capture instant used only for stable audit/order behavior; the assessment date remains the displayed clinical date.
- Provenance is required and fixed to `EXTERNALLY_OBTAINED_USER_ENTERED`; the UI and every report/export render it as: `Total entered by you from a result obtained elsewhere. MindScale did not administer or calculate it.`
- There is no free-text source/clinic field in this slice: it would collect more identifying information without being needed to preserve the external provenance boundary.
- History is newest assessment date first, then newest id. Add and edit use the same validated form. Delete requires confirmation and targets one id.

## Frozen clinician-summary snapshot and grammar

### Snapshot and derivation

- A `@Transaction` report snapshot reads entries, sleeps, markers, settings, canonical profile, and external scores. Profile screen counts may be reactive projections, but a generated report uses one immutable transaction result.
- Generation runs off the main thread through pure Kotlin and consumes the existing `deriveInsights` engine for the selected `InsightRange`, configured hold, captured `generatedAt`, and captured current `ZoneId`.
- The displayed report, Copy payload, Share payload, and Save payload for one generated state are byte-identical UTF-8 plain text. Once an external provider is launched, its pending bytes are retained across recomposition/recreation and are not silently regenerated from later Room changes.
- Report order is fixed: header/disclaimer, Recorded course, Events marked, Episode structure, Time of day, Sleep, Externally obtained totals, footer.
- The report includes at most the six newest in-range markers, rendered chronologically within that selected set, and at most the four newest in-range external totals. Exact omitted counts are appended. Marker whitespace is collapsed to one line so record text cannot inject report headings.
- Profile name is included only when nonblank. Rating notes, onset chips, anchors, and settings other than hold/hour format are not included.

### Exact required language

The encoder uses these literal headings and sentences; plural/count/date substitutions do not change their meaning:

```text
MINDSCALE — USER-RECORDED CLINICIAN SUMMARY
MindScale stores and arranges user-recorded information. It does not diagnose,
interpret, administer questionnaires, or provide a clinical assessment.
Times reflect when recording was possible.

RECORDED COURSE
N ratings were recorded on D local calendar days in this window.
N derived episode spans touched this window using the configured H-hour waking hold.
N of D eligible local days had no derived intensity above 0.
Recorded intensity-hours: X (recorded intensity multiplied by awake hours in this window).

EVENTS MARKED
...

EPISODE STRUCTURE
Middle closed derived episode length: X of waking time.
Highest recorded intensity in a derived episode: X of 10.
An ending may be a recorded 0, the configured waking hold, or still ongoing.

TIME OF DAY
X of N recorded starts in this range were recorded from ... up to but not including ... .
Start times use the device's current time zone and reflect when recording was possible.
They do not establish when symptoms began.

SLEEP
N completed sleep periods had a recorded Wake in this window: X nights over 3 elapsed
hours and Y naps of 3 elapsed hours or less.
These counts do not establish whether sleep affected later records or later records
affected sleep.

EXTERNALLY OBTAINED TOTALS
YYYY-MM-DD — PHQ-8 total N — entered by the user from a result obtained elsewhere.
MindScale did not administer, calculate, or interpret PHQ-8 or GAD-7 totals.

Generated locally on this device. This text may contain sensitive health information.
Review the underlying records before relying on this summary.
```

- Empty/sparse variants state only exact absence/counts: `No ratings were recorded in this window.`, `No events were marked in this window.`, `Fewer than 6 recorded starts are in this window, so no time-of-day count is shown.`, `No completed sleep periods had a recorded Wake in this window.`, or `No externally obtained PHQ-8 or GAD-7 totals are stored in this window.`
- Time-of-day eligibility, four-hour window tie-breaking, boundaries, and denominator are exactly Phase 9. Night/nap boundary, Wake attribution, exact durations, and incomplete exclusion are exactly Phase 10.
- Never emit `better`, `worse`, `improved`, `declined`, comparative `higher/lower`, `symptom-free`, `typical`, `normal`, `abnormal`, `mild`, `moderate`, `severe`, `positive`, `negative`, `risk`, `diagnosis`, `treatment`, causal language, prediction, recommendation, or a clinical threshold.
- No statement connects external totals to MindScale ratings, episodes, sleep, markers, dates other than their own assessment date, or one another.

## Privacy, external actions, and file behavior

- All profile/score/report reads and writes are offline Room operations. No network API or analytics event exists.
- Report UI always shows: `This summary can contain sensitive health information. Nothing leaves MindScale until you choose Copy, Share, or Save.`
- Copy is an explicit tap, uses the platform clipboard, marks content sensitive where supported, and reports success/failure without clearing or changing records.
- Share is an explicit tap that launches an Android `ACTION_SEND` `text/plain` chooser containing the captured text. MindScale never selects a recipient or confirms delivery; chooser cancellation is neutral.
- Save launches Activity Result `CreateDocument("text/plain")` with `mindscale-clinician-summary-<UTC yyyyMMdd-HHmmss>.txt`. Picker cancellation is neutral. URI open/write/flush/close failure reports `Could not save the clinician summary. Choose Save to try again.` and retains the captured bytes for retry.
- Copy, Share, and Save require no storage, contacts, network, or account permission. System providers/recipient apps are outside MindScale's local-only boundary only after the user's explicit action.

## Backup, CSV, erase/reset, import, and rollback

- JSON backup becomes deterministic `mindscale-backup` version 5. It preserves every version-4 field unchanged and adds `profile` and `externalScores`, sorted deterministically. External rows include id, instrument, total, ISO assessment date, provenance, and entered-at instant.
- The existing records CSV remains byte-compatible for all version-4 snapshots and intentionally contains only ratings, sleeps, and markers. Profile and external totals are excluded; the Settings copy states that complete backup and clinician-summary text are the respective ways to export them.
- Import/restore remains absent. A later import spec must accept/version-check v5, validate the canonical profile and every external-score invariant, handle duplicate instrument/date rows, and roll back atomically on any failure.
- Export-first Erase reads a version-5 snapshot, then one Room transaction deletes entries, sleeps, markers, and external scores and resets TrackSettings and UserProfile. Any missing canonical row or failed reset aborts the transaction; partial deletion is prohibited.
- Fresh reset restores an empty display name and no external scores. Score drafts, delete confirmation, report bytes, and navigation are UI state and do not survive a completed erase.
- A version-5 database cannot be opened by the old version-4 binary. No destructive downgrade fallback or 5→4 migration is added. Operational rollback is forward-fix only; installing the older binary must fail closed rather than delete/mutate v5 data. Uninstall/reinstall remains destructive and is never presented as a safe rollback.

## Accessibility, restoration, concurrency, and failure behavior

- All icon-only/profile actions have explicit accessible names. Every actionable row/button is at least 48 dp. Instrument choice exposes selected state redundantly; totals and dates have persistent visible labels, input purpose, and error association.
- Validation/write/read/copy/save status is visible and announced with a polite live region. Deletion confirmation names instrument, date, and total. Decorative initials are hidden from accessibility.
- Profile, report, history, and forms use lazy/vertical layouts that remain usable in light/dark themes, 200% font scale, landscape, and with the IME visible; no horizontal text clipping is accepted.
- Saveable primitives restore destination stack, selected range (through Insights), unsaved name draft, score instrument/date/total draft, editing id, pending delete id, and pending Save bytes/file name. Restored ids are revalidated against reactive Room state; stale ids close safely with retained/new-entry drafts and a concise message.
- A dirty name draft is not overwritten by a later database emission. If the stored name changes concurrently, Save requires the user to confirm replacing the newer value; no full-row stale write is used.
- Add/edit/delete/profile writes run in `viewModelScope`, are cancellable before the transaction starts, and publish success only after Room reports the expected row count. Unique-constraint, stale-row, and write failures retain drafts and expose retry.
- Report derivation publishes one whole immutable state. A source/snapshot/derivation failure leaves records untouched and shows `Could not build the clinician summary. Your records are still on this device.` with Retry.
- Share chooser absence reports a non-destructive error. Clipboard and file-provider exceptions are caught. Picker cancellation is not an error and does not clear pending text needed for a later retry.

## Test and verification contract

### JVM and pure report tests

- Exact fixture text for empty, sparse, and populated reports, including all headings/caveats and a banned-word scan.
- All six ranges, current-zone/DST boundaries, rating-day count, engine parity, hold wording, sparse onset refusal, sleep boundary/incomplete exclusion, marker/scores cap and omitted counts, whitespace normalization, ordering, and byte determinism.
- PHQ-8 0/24 accepted and -1/25 rejected; GAD-7 0/21 accepted and -1/22 rejected; invalid/non-integer/future dates, invalid restored enum/id, duplicate instrument/date, name/control/length limits, and stale mutations.
- ViewModel loading/success/read failure/derivation failure/retry, draft restoration, concurrent name conflict, score add/edit/delete failure retention, pending Save-byte retention, and whole-state publication.
- JSON v5 exact fixtures/escaping/order and v4-field regression; records CSV byte compatibility for the same version-4 snapshot.

### Room and instrumented tests

- Exported schema 5 validates. Fresh create seeds both canonical rows. Migrations 1→5, 2→5, 3→5, and 4→5 preserve all old rows/settings and create valid empty Profile/score state.
- Profile/score validation path, unique instrument/date constraint, deterministic order, targeted edit/delete affected rows, transactional v5 snapshot, export-first erase completeness, reset defaults, and rollback/no-destructive-fallback configuration.
- Navigation from each root, nested Report/Profile/Settings Back, process recreation, shared range restoration, stale restored ids, Settings contextual links, and bottom-bar visibility.
- Compose semantics cover Profile, form fields/errors, selected instrument, score history, confirmation, Report sections, range controls, Copy/Share/Save, live errors, 48 dp targets, and sensitive-data copy.
- Activity-result tests cover Save success, cancel, open/write failure and retry. Intent tests cover Share action/MIME/payload and unavailable chooser without sending externally.

### Full oracles and installed-app walkthrough

Run exactly:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

On the intended API 36 emulator inspect empty and populated Profile/Report, valid/invalid/external-score edit/delete flows, all ranges, Copy/Share/Save chooser behavior, report text file content, light/dark, 200% font, landscape, IME, nested Back, Activity recreation, privacy copy, and erase confirmation/cancellation. Restore emulator theme/font/orientation after inspection.

## Machine-checkable acceptance criteria

- [x] SPEC: this complete spec and D-1 through D-12 are frozen in a documentation-only commit before application-code edits.
- [x] NAV: Profile/Report overlays and nested Back/restoration match the frozen stack; root navigation regressions pass.
- [x] PROFILE: optional name validation/privacy/targeted save and derived initials work with draft/conflict restoration.
- [x] SCORES: only validated, dated, explicitly external PHQ-8/GAD-7 totals persist; add/edit/delete/duplicate/stale paths are covered and no administration/calculation/interpretation UI exists.
- [x] REPORT: one immutable transaction snapshot produces exact bounded sections/caps/empty states and contains every required caveat and no banned grammar.
- [x] PRIVACY: data remains local until an explicit Copy/Share/Save; payload identity, cancellation, provider failure, and retry are covered.
- [x] MIGRATION: schema 5 and all 1→5 paths preserve prior data/settings and seed valid Profile state with no destructive fallback.
- [x] EXPORT: deterministic JSON v5 includes Profile/scores; existing records CSV is byte-compatible; report file is deterministic captured UTF-8 text.
- [x] ERASE: export-first erase/reset atomically removes Profile/scores with all existing records/settings and aborts on invariant failure.
- [x] RESTORATION: range, overlay stack, drafts, ids, confirmation, pending bytes, and concurrent/stale behavior satisfy the frozen contract.
- [x] ACCESSIBILITY: semantics, live regions, error association, 48 dp actions, large text, landscape, light/dark, and TalkBack reading order are verified.
- [x] ORACLES: JVM, lint, assemble, device discovery, full connected suite, and diff check pass from the verified implementation head.
- [x] REVIEW: one critical-path review covers medical grammar, provenance, schema/migration, exports/privacy, erase/reset, accessibility, restoration/concurrency, and rollback; every blocking finding is resolved.

## Verification evidence

- Frozen documentation commit: `2019897655f9db5f143ef91c0707f16ba7c13cb8`.
- Verified implementation commit: `cb469100b3b34fd3a8716428ff1448668af1cdcb`.
- `.\gradlew.bat test`: 180/180 tests passed across 15 suites; 0 failures, errors, or skips.
- `.\gradlew.bat lint`: passed with 0 errors and the unchanged 22-warning baseline.
- `.\gradlew.bat assembleDebug`: passed.
- `adb devices -l`: confirmed `emulator-5554`, `MindScale_API_36`, API 36.
- `.\gradlew.bat connectedDebugAndroidTest`: 111/111 tests passed; 0 failures or skips.
- `git diff --check` and the staged-diff check passed.
- Installed-app inspection covered empty and populated Profile/Report states, external-total provenance and duplicate-field error association, light/dark, 200% font, landscape, IME, nested Back, rotation restoration, Copy/Share/Save, and export disclosure. A locally saved 1,535-byte report had SHA-256 `12068f72f3de6583731e192eb115e881d21b1950e2bfc96fc2ffc8e9de86a96a` and matched the displayed factual header/privacy footer.
- Export-then-erase was exercised end to end after creating an unsaved sensitive name draft and retained report bytes. It returned to Track and reopened Profile with empty name, records, and scores; Room transaction tests cover rollback invariants.
- Critical review initially blocked on retained UI state after erase, silent reuse of older Save bytes, a profile-name compare/write race, detached score-field errors, and missing records-CSV disclosure. The implementation now clears restorable sensitive state and overlay navigation on erase, visibly identifies/discards retained Save bytes, uses an atomic conditional name update, associates date/total errors with their fields, and discloses CSV exclusions plus JSON/report alternatives.

Known coverage gaps: a real share recipient was deliberately not selected, and external document-provider open/write failure was exercised at the ViewModel boundary rather than by forcing DocumentsUI to fail. TalkBack semantics and reading order were inspected through Compose tests; spoken audio output was not manually audited. Saved JSON/report files are export evidence only until the separately scoped import/restore phase.

## Task decomposition

1. Freeze spec/decision/state/backlog documentation — oracle: `git diff --check` and documentation-only diff.
2. Add schema 5, migration, DAOs/entities/converters, snapshot/reset, JSON v5 — oracle: focused JVM/Room tests.
3. Add pure report encoder and Profile/Report ViewModel — oracle: focused JVM tests.
4. Add Profile/Report navigation and Compose UI/actions — oracle: focused connected/UI tests.
5. Run full oracles and installed-app matrix; perform critical review and fixes.
6. Publish verified branch/PR, merge, synchronize main, and record the phase boundary.

## Rollout and safe rollback

The phase is an additive local database migration with no feature flag or server dependency. The migration is mandatory and forward-only; an older binary must fail to open schema 5 and must never destructively downgrade it. Before publication, verify the v5 JSON backup and erase snapshot paths; because import is not yet implemented, document that saved backups are one-way export evidence until the later restore phase.

## Decisions and approval gate

- **D-1 (bounded product):** ship only Profile, externally obtained totals, and a bounded clinician summary; exclude questionnaires, calculation, interpretation, function inference, Safety, breathing, and import.
- **D-2 (navigation ownership):** Profile owns name/scores/Report/Settings; Report and Settings are saveable overlays; Report shares the existing Insights range.
- **D-3 (profile privacy):** store only an optional validated display name; derive initials; show local/export privacy copy.
- **D-4 (external totals):** store one validated instrument total per instrument/date with explicit fixed external/user-entered provenance, entered-at audit time, targeted edit/delete, and no source free text.
- **D-5 (report source):** generate from one immutable transaction snapshot through the existing episode/onset/sleep engine and a captured time/zone/range.
- **D-6 (report grammar):** use only the frozen exact factual sections/caveats, caps, empty states, and banned-language contract; make no comparisons or clinical/inferential claims.
- **D-7 (privacy/actions):** remain local until explicit Copy/Share/Save; capture byte-identical sensitive plain text and handle provider cancellation/failure without data mutation.
- **D-8 (migration/export):** Room and JSON advance additively to v5; records CSV stays compatible and excludes Profile/scores; report text exports the bounded selected data.
- **D-9 (erase/import):** export-first erase atomically deletes scores/resets Profile; import remains absent but its future contract must understand v5.
- **D-10 (accessibility/restoration):** native labeled controls, live errors, 48 dp targets, responsive layouts, primitive draft/id/pending-byte restoration, and stale-id revalidation are required.
- **D-11 (concurrency/failure):** targeted checked mutations, uniqueness, dirty-draft conflict handling, immutable report publication, retryable non-destructive failures, and neutral cancellation are mandatory.
- **D-12 (rollback/compatibility):** no destructive downgrade or 5→4 path; old binaries fail closed and operational rollback is forward-fix only.

Approval gate is satisfied. D-1 through D-12 are frozen; a genuine source-of-truth conflict or material frozen-interface change requires a documented amendment before implementation continues.

## Required durable-document updates

### Before application-code edits

- Commit this `FROZEN — APPROVED` spec, D-011, active Phase 11 state, and backlog removal as documentation only.

### After implementation and verification

- Mark the spec `IMPLEMENTED — VERIFIED LOCALLY`, check acceptance criteria, add exact oracle/manual/review evidence, and update `PROJECT_STATE.md` before publication.
- After merge and main synchronization, add and push a phase-boundary documentation commit on `main` recording the merge/PR/final hash and remaining backlog order.
