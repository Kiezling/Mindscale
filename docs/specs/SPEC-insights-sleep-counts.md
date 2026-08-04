# MindScale Phase 10 — Insights descriptive sleep counts

Status: IMPLEMENTED — VERIFIED LOCALLY

Owner: Codex

Date: 2026-08-04

Approval: On 2026-08-04, the user granted full Phase 10 ownership and authorized product decisions, specification, implementation, verification, commits, push, PR readiness, merge, and final synchronization without another review gate. D-1 through D-10 below are frozen before application-code edits.

Governing product sources:

- Local Claude Design handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html`, especially the `sleep facts` block
- The same handoff's `SPEC.md`, especially raw-data truth, descriptive grammar, sleep-paused measurement, and non-inferential constraints
- `docs/specs/SPEC-insights-foundation.md`
- `docs/specs/SPEC-insights-entry-chart.md`
- `docs/specs/SPEC-insights-onset-gap-histogram.md`
- `docs/specs/SPEC-insights-onset-time-counts.md`
- `docs/specs/SPEC-track-phase2-completeness.md`

## Purpose

Add the next bounded native Insights slice: direct descriptive counts of completed sleep periods already recorded through Sleep/Wake. The view restates how many completed periods meet the handoff's duration-only `night` and `nap` labels and supplies exact duration summaries without comparing later symptoms or implying that sleep changed them.

## Product-source reconciliation

The handoff prototype:

- normalizes overlapping/touching sleep intervals before using them;
- calls a completed period over three elapsed hours a `night` and one of three hours or less a `nap`;
- includes a completed period when its end/Wake is inside the selected range, even if it began before the range;
- reports night count plus median/shortest/longest duration and nap count plus median duration;
- compares positive ratings within five hours after short and long nights; and
- appends `Direction of effect not established by these counts.`

Native reconciliation:

1. Preserve the handoff's duration boundary and Wake-time attribution, but disclose both explicitly because the labels do not infer time of day or intent.
2. Reuse the Phase 5 normalized sleep union. Overlapping or touching source rows count once, matching every existing Insights sleep/awake calculation.
3. Exclude open and future-ending intervals from completed counts. The prototype clips them to `now` and can accidentally treat them as completed; native output must not invent a Wake.
4. Omit the five-hour post-wake comparison. It mixes symptom state, recording opportunity, episode semantics, and an arbitrary observation window, while the requested slice is descriptive sleep counts. A later separately specified view would need a cohort, coverage denominator, minimum sample, range-boundary, and grammar contract.
5. Do not impose the six-record threshold used by onset charts. Exact direct counts and order statistics make no shape or comparison claim; one completed period is valid evidence of one recorded period. Zero completed periods receive an exact refusal state.
6. Use elapsed duration rather than local wall-clock subtraction, so DST transitions and zone changes cannot alter a period's classification or duration. The current zone affects only the selected local-calendar range boundary and display times.

## Goals

1. Add one `Sleep you recorded` section after the existing onset-time section.
2. Count completed normalized sleep periods whose recorded Wake lies in the selected range.
3. Classify exactly three elapsed hours as a nap and anything longer as a night.
4. Show an exact completed-period denominator, two category count cells, deterministic duration readout, and honest caveats.
5. Disclose incomplete periods that overlap the selected window while excluding them from counts and duration summaries.
6. Restore the selected sleep category independently through primitive `SavedStateHandle` state.
7. Keep derivation in the existing pure, off-main, immutable Insights snapshot and keep all persistence and architecture contracts unchanged.

## Non-goals

- No post-wake observation window, later-rating count, short-versus-long-night comparison, direction-of-effect estimate, association, correlation, causal language, prediction, diagnosis, significance, or recommendation.
- No bedtime/wake-time chart, sleep-duration histogram, weekday grouping, regularity score, sleep quality, sleep efficiency, circadian inference, target duration, streak, goal, badge, or notification.
- No time-of-day classifier for night versus nap. The terms are frozen duration-only labels from the handoff.
- No separate short-night/long-night cohorts at six/seven hours and no treatment of the one-hour gap between them.
- No raw-row count alongside normalized periods; no repair or mutation of overlapping, touching, invalid, open, or future source rows.
- No persisted derived statistics, `SleepInterval` change, Room/schema migration, JSON/CSV change, import, permission, dependency, toolchain, navigation, DI, module, service, analytics, or background work.
- No edit/delete actions on Insights. Track and Full Log remain the raw-record maintenance surfaces.

## Frozen user experience and copy

### Placement and visibility

- The section appears after `Time of day it started` in the existing Insights vertical list.
- It participates in all six existing ranges and the global loading/error/empty behavior.
- When any Entry exists, the section remains visible even when the selected range has no completed sleep. Sleep/Wake capture always records an Entry, so the existing truly-empty screen remains authoritative.

### Zero-completed state

- Heading: `Sleep you recorded`.
- Refusal: `No completed sleep periods woke in this range.`
- If one incomplete normalized period overlaps the range: `1 incomplete sleep period is excluded because its Wake time is missing or later than now.`
- Otherwise pluralize deterministically: `{n} incomplete sleep periods are excluded because their Wake times are missing or later than now.`
- No selectable category cells or duration summary appear when the completed denominator is zero.
- The explanatory caveat remains visible in the zero state.

### Eligible state and denominator

- Eligibility begins with one completed normalized period; there is no higher minimum sample.
- Denominator: `{n} completed sleep period(s) woke in this range.`
- Two fixed-order selectable cells render side by side:
  - `Nights`, visible boundary `>3h`;
  - `Naps`, visible boundary `≤3h`.
- Each cell shows its exact count. Both cells remain selectable when eligible, including a zero-count category.
- Default readout: `Select nights or naps to read the exact durations.`
- Night selection with one night uses `was a night`; otherwise it uses `were nights`: `Of {total} completed sleep period(s), {count} {classification}. Middle duration {median}; shortest {min}; longest {max}.`
- Nap selection with one nap uses `was a nap`; otherwise it uses `were naps`, with the same deterministic duration clause.
- Zero-category selection replaces the duration clause with `There are no durations in this category.`
- `formatDuration` supplies deterministic minute-rounded visible durations. Classification and median/min/max use exact milliseconds before formatting; no already-rounded value participates in arithmetic.

### Caveat

Exact caveat:

`Nights are periods over 3 elapsed hours; naps are 3 hours or less. These labels use duration, not time of day. Periods are grouped by recorded Wake time, so one may have started before this range. Selected-range day boundaries use your device's current time zone. Incomplete periods are excluded. These counts do not show whether sleep changed what you recorded afterward.`

### Accessibility and layout

- Each category is one coherent at-least-48-dp button with role, selected state, exact count/denominator/boundary description, fixed traversal order, and a visible text label.
- Selection uses both a two-dp outline and container change; color is not the only carrier.
- The persistent readout is a polite live region and is identical for touch, keyboard/D-pad, and TalkBack activation.
- The two cells use a wrapping/equal-width layout that remains reachable without horizontal scrolling at 150% font and landscape. If implementation evidence proves they cannot fit at 48 dp, a native horizontally reachable row is permitted, but it must not consume vertical parent drags.
- The section has no custom pointer detector, Canvas, hover behavior, hidden tooltip, or bitmap.

## Frozen source and derivation

### Inputs and normalization

- Source is the existing `SLEEP` rows already included in the single `EpisodeSourceDao.observeSource()` query and the same `BuiltModel` created by `deriveInsights`.
- Raw rows retain their current validation: a persisted non-null `endTs <= startTs` fails the whole derivation into the existing honest Retry/error state and is never repaired.
- Rows starting after `now` are excluded until their scheduled future boundary.
- A row is incomplete at analysis time when `endTs == null` or `endTs > now`.
- Sleep rows are sorted and unioned when they overlap or touch, exactly as Phase 5. The normalized span is incomplete if any contributing row is incomplete at analysis time.
- Normalization remains O(S log S) plus a linear merge. Sleep counts consume the already-built normalized spans and do not rescan raw rows per category or composable.

### Completed cohort and range attribution

For selected half-open range `[rangeStart, now)`:

- A normalized span is completed only when it is not incomplete and its real recorded end is at or before analysis `now`.
- A completed span belongs to the selected cohort when its Wake/end is `>= rangeStart` and `< now`.
- A span ending exactly at `rangeStart` is included; a span whose Wake is before `rangeStart` is excluded even if part overlaps the range.
- The entire elapsed duration `end - start` classifies and summarizes an included span. It is never clipped to `rangeStart`.
- Therefore a cross-boundary period can be included when it wakes in range, and the caveat discloses that it may have started earlier.
- Current-zone changes can move the local-calendar `rangeStart` and therefore cohort membership, but cannot change exact elapsed duration or night/nap classification.

### Incomplete disclosure

- An incomplete normalized span is disclosed when its clipped analysis span has positive overlap with `[rangeStart, now)`.
- It contributes to neither completed denominator, category count, nor duration summaries.
- An open span that began before the range remains disclosed because it overlaps the current selected window.
- A future-starting span is not disclosed before its start. A future-ending row already underway is disclosed as incomplete and is reconsidered at its existing scheduled boundary.

### Classification and summaries

```kotlin
enum class SleepCategory { NIGHT, NAP }

data class SleepCategoryCount(
    val category: SleepCategory,
    val count: Int,
    val durationsMillis: List<Long>,
    val medianDurationMillis: Long?,
    val shortestDurationMillis: Long?,
    val longestDurationMillis: Long?
)

data class SleepCounts(
    val completedCount: Int,
    val incompleteCount: Int,
    val categories: List<SleepCategoryCount>
) {
    val isEligible: Boolean get() = completedCount > 0
}
```

- `NIGHT` means exact elapsed duration `> 10_800_000` ms.
- `NAP` means exact elapsed duration `<= 10_800_000` ms. The existing valid-row floor guarantees positive duration.
- Categories always appear `[NIGHT, NAP]` and partition the completed cohort exactly; their counts sum to `completedCount` and their duration lists contain every completed duration exactly once.
- Median uses the existing overflow-safe exact-millisecond median rule; min/max use exact milliseconds.
- The snapshot gains one immutable `sleepCounts` value. No UI-side filtering, classification, aggregation, grammar arithmetic, or source read is allowed.

## State, concurrency, privacy, and failure behavior

- The ViewModel stores only validated primitive category index `0..1` under `insights.selectedSleepCategory`.
- Category selection clears on range change or when completed eligibility becomes zero; it survives Activity/ViewModel recreation and eligible source/clock/zone refresh, with readout values recomputed from the newest immutable snapshot.
- Invalid restored indices and invalid/ineligible selection events are discarded/no-ops.
- Raster, entry-chart, onset-gap, onset-hour, and sleep-category selections remain independent.
- The existing combined Room/settings Flow, `flatMapLatest`, cancellable off-main derivation, whole-snapshot publication, scheduled invalidation, stale-snapshot-on-error behavior, and Retry are unchanged.
- Sleep counts expose timestamps only through aggregate durations and category counts. Entry values, kinds, chips, notes, and Marker text cannot affect or appear in the section.
- Source/derivation failure leaves the last complete snapshot visible with the existing error and Retry; no partially updated sleep data is published.

## Invariants

1. Every completed normalized in-range span appears in exactly one category.
2. Exactly three elapsed hours is a nap; one millisecond more is a night.
3. Cohort attribution uses recorded Wake/end, lower-inclusive at `rangeStart` and upper-exclusive at `now`.
4. Cross-boundary duration is full elapsed duration, never range-clipped.
5. DST and time-zone changes cannot change exact duration/classification; the current zone may change local-range membership only.
6. Incomplete spans contribute zero completed/category/duration values and are disclosed only when they overlap the selected window.
7. Counts and order statistics are direct record restatements with no minimum above one and no later-rating comparison.
8. Existing normalized sleep behavior, episode hold/AUC, raster, entry chart, and all prior Insights outputs remain unchanged.
9. UI reads only immutable `SleepCounts`; business logic and deterministic grammar stay outside composables.
10. Room/schema/JSON stay version 4; all raw records and export behavior remain unchanged, making source rollback safe.

## Test and verification contract

### Pure engine and copy tests

- Empty, only-open, future-start, future-end, and 1/N completed cohorts.
- Exact `3h`, `3h + 1ms`, minimum valid minute, long/extreme duration, odd/even median, min/max, and overflow-safe arithmetic.
- Wake just before/at/after `rangeStart`, Wake just before/at `now`, start-before-range/full-duration attribution, and all six ranges.
- Spring-forward and fall-back intervals use elapsed duration; current-zone change can alter membership but not duration/classification.
- Overlapping/touching closed rows normalize once; incomplete plus overlapping rows remain conservatively incomplete; invalid closed rows fail.
- Category counts partition the denominator; Entry values/kinds/notes/chips, Markers, hold duration, episodes, and later ratings cannot change sleep counts.
- Exact zero/denominator/incomplete/selected/caveat copy, singular/plural grammar, zero-category selection, and forbidden inference-term scan.

### ViewModel tests

- The same immutable snapshot updates from source/range/time/zone seams and preserves prior outputs.
- Valid selection restores through a new `InsightsViewModel`/`SavedStateHandle`, refreshes while eligible, and remains independent from every prior selection.
- Range change, zero eligibility, invalid restored index, and invalid/ineligible events clear or no-op exactly as frozen.
- Existing failure, Retry, scheduled future-end invalidation, and stale-snapshot behavior remain green.

### Compose/accessibility and Room regression tests

- Zero state shows heading, exact refusal/incomplete disclosure/caveat, and no category cells.
- Eligible state shows denominator, exactly two fixed-order cells, count/boundary labels, zero-count selection, default readout, selected duration readout, and caveat.
- Each category cell has coherent at-least-48-dp role/selected/content semantics and redundant non-color selection; activation updates the same visible polite live region.
- Parent vertical scrolling starting over the section remains functional; light/dark, 150% font, rotation, and supported layouts preserve critical text.
- `EpisodeSourceDaoTest` proves Sleep source changes update the same projection and Entry/Marker mutations do not alter sleep counts; no query/schema migration is introduced.
- All Phase 1–9 JVM and connected tests remain green.

### Full oracles and installed-app walkthrough

Run from the repository root with the configured wrapper/JDK/SDK/cache:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Install and launch the debug APK on `MindScale_API_36`. Inspect no-completed/open, exact-three-hour, over-three-hour, cross-boundary, and mixed eligible fixtures; both selections/readouts; 12/24-hour settings (the sleep aggregate must remain unchanged); light/dark; 150% font; rotation; accessibility semantics; vertical ownership; Activity/process restoration; and practical DST/current-zone behavior. Restore emulator font, theme, rotation, and timezone afterward.

## Machine-checkable acceptance criteria

- [x] SPEC: this complete spec and D-1 through D-10 are frozen in a documentation-only commit before application-code edits.
- [x] SOURCE: `SleepCounts` consumes the existing normalized sleep spans inside the same `deriveInsights` snapshot; no DAO, second Flow, UI classifier, or cache is added.
- [x] COHORT: tests prove lower-inclusive/upper-exclusive Wake attribution, full cross-boundary duration, all six ranges, `now`, and current-zone range changes.
- [x] CLASSIFICATION: tests prove exact elapsed `<=3h` nap and `>3h` night partitioning, DST invariance, normalization, and exact duration summaries.
- [x] INCOMPLETE: open/future-ending/overlapping incomplete spans are excluded and honestly disclosed; scheduled re-derivation makes them eligible only after a real end is in the analysis past.
- [x] SAMPLE/GRAMMAR: zero gets exact refusal; one or more gets direct counts without a higher threshold; denominator/readout/caveat grammar is deterministic and non-inferential; no post-wake comparison exists.
- [x] UI: heading, refusal/denominator, two fixed-order native cells, count/boundary labels, selection, live readout, incomplete disclosure, and caveat match the frozen behavior.
- [x] ACCESSIBILITY: cells are coherent at-least-48-dp buttons with exact semantics and non-color selection; parent vertical scroll, keyboard/D-pad/TalkBack, light/dark, rotation, and 150% font remain usable.
- [x] STATE: primitive category selection restores independently, clears on range/zero eligibility, rejects invalid state, and refreshes from the latest snapshot.
- [x] CONCURRENCY/FAILURE: the existing single Flow, off-main cancellable derivation, atomic snapshot, invalidation, stale snapshot, and Retry contracts remain unchanged and tested.
- [x] PRIVACY/PERSISTENCE: no Entry/Marker private content appears; Room/schema/JSON remain version 4 and CSV/backup/permissions/dependencies/toolchain/navigation/DI remain unchanged.
- [x] REGRESSION: all Phase 1–9 behavior and prior Insights selections remain green.
- [x] ORACLES: JVM tests, lint, assembleDebug, intended-device identity, connected tests, installed-app walkthrough, and `git diff --check` pass.
- [x] REVIEW: one critical-path review covers normalization/cohort boundaries, DST/zone, incomplete data, arithmetic/grammar, accessibility, restoration, concurrency, privacy, persistence compatibility, and rollback; every blocking finding is resolved.
- [x] PUBLICATION: spec/state/backlog/decision evidence is current; branch is intentionally committed/pushed, PR is ready and mergeable at the verified head, merge succeeds, and local/tracking/live `main` synchronize with the final phase-boundary commit.

## Verification evidence

- Frozen documentation commit: `e6afe3280acccb1fd0849b26b5fac5b9e4699980` (`Freeze Phase 10 sleep counts spec`), before application-code edits.
- Verified implementation commit: `945300d0bad66be21de33b9494a19d393cf40aba` (`Implement Phase 10 sleep counts`).
- `test`: 168/168 JVM tests passed; 0 failures, errors, or skips across 12 suites.
- `lint`: passed with 0 errors and the existing 22 warnings.
- `assembleDebug`: passed.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- Focused Room/Insights connected run: 27/27 passed after final copy and zero-category coverage.
- `connectedDebugAndroidTest`: 100/100 passed; 0 failures or skips.
- `git diff --check`: passed with only configured LF-to-CRLF notices.
- Installed-app inspection used disposable local test fixtures and verified the exact no-completed/open refusal, a four-hour night, an exact-three-hour nap, the completed denominator, incomplete disclosure, both selections/readouts, redundant two-dp selection outline, vertical reachability, light/dark, 12/24-hour behavior, 150% font, rotation/configuration recreation, and selection restoration. Native UI hierarchy inspection exposed exact Night/Nap content descriptions, click targets substantially above 48 dp, selected-state coverage, visible live readout copy, and the incomplete disclosure.
- Emulator font, theme, rotation, and time format were restored to `1.0`, `no`, automatic portrait, and `12-hour`. The temporary manual seed source and test package were removed; only disposable target-app fixture data remains on the emulator.
- Critical-path review covered normalized/cohort boundaries, exact-three-hour and Wake boundaries, DST/current-zone behavior, incomplete/future-ending intervals, median/copy grammar, accessibility, restoration/invalidation, single-snapshot concurrency, privacy, unchanged version-4 persistence, and rollback. It corrected one inaccurate phrase for a future recorded Wake (`unavailable` became `missing or later than now`); no blocking finding remains.
- Practical DST and current-zone reprojection were not manually induced; pure tests cover both spring/fall elapsed duration and current-zone cohort movement. Actual TalkBack speech was not listened to; exact Compose semantics/activation/live-region tests and device UI-hierarchy inspection cover the accessibility contract.
- No plausible reusable failed implementation path was introduced, so `FAILED_PATHS.md` was not changed. One unquoted PowerShell Gradle filter and one temporary JUnit return-type inference error failed before product tests ran and were corrected immediately.
- Verification-state commit `e7e1e117df5b75564b533140654af1b0c47f451b` was pushed on `agent/phase10-insights-sleep-counts`. GitHub PR #7 was opened as draft, marked ready, verified mergeable at that exact head, and merged into `main` as `03bad992726bd336e43b0d5f17c064a07bb2bb57` on 2026-08-04. Local `main` fast-forwarded to the merge before this requested phase-boundary documentation commit.

## Rollout and safe rollback

- Rollout is source publication only after every acceptance criterion and applicable oracle passes. No runtime flag or data backfill is needed.
- No migration or new durable application state exists. Pre-Phase 10 and Phase 10 builds read the same version-4 raw records and exports.
- Before or after release, rollback is a source revert/forward-fix of sleep models, derivation, ViewModel/UI state, tests, and docs. Older builds ignore the unknown primitive SavedState key; raw sleep records remain untouched.

## Decisions and approval gate

- **D-1 (bounded scope):** add only direct completed night/nap counts, exact duration summaries, incomplete disclosure, selection/readout, and caveats; omit all post-wake or direction-of-effect comparison.
- **D-2 (single source):** consume the existing normalized sleep union within `deriveInsights`; add no DAO, Flow, classifier path, cache, or persisted aggregate.
- **D-3 (classification):** exact elapsed duration `<=3h` is nap and `>3h` is night; the labels never use clock time and the caveat says so.
- **D-4 (range attribution):** group completed spans by recorded Wake in `[rangeStart, now)`, include a Wake exactly at range start, and summarize the full cross-boundary elapsed duration.
- **D-5 (incomplete data):** open/future-ending normalized spans never count as completed; disclose overlapping incomplete spans and re-evaluate them only through normal invalidation/source refresh.
- **D-6 (sample and summaries):** refuse only at zero completed periods; from one onward show exact counts and exact-millisecond median/min/max because no distribution or comparison claim is made.
- **D-7 (grammar):** use frozen denominator, selected readout, boundary, zone/attribution/incomplete caveat, and explicit no-effect language; expose no later ratings, causes, associations, predictions, diagnoses, or recommendations.
- **D-8 (accessibility):** use two native fixed-order, at-least-48-dp selectable cells with visible count/boundary text, redundant selection, persistent polite readout, and parent vertical-scroll ownership.
- **D-9 (state/concurrency):** persist only a validated primitive category index; retain the single Room/settings Flow, cancellable off-main immutable derivation, scheduled invalidation, whole-snapshot publication, and Retry behavior.
- **D-10 (persistence/architecture):** keep Room/schema/JSON version 4 and CSV, backup, privacy, permissions, dependencies, toolchain, navigation, manual DI, and local-only architecture unchanged.

Approval gate is satisfied. D-1 through D-10 are frozen; a genuine source-of-truth conflict or material interface change requires a documented amendment before implementation continues.

## Required durable-document updates

### Before application-code edits

- Commit this `FROZEN — APPROVED` spec, D-010, active Phase 10 state, and backlog removal as documentation only.

### After implementation and verification

- Mark this spec `IMPLEMENTED — VERIFIED LOCALLY`, check the criteria, and record exact test/manual/review/publication evidence.
- Update `PROJECT_STATE.md`, `docs/specs/BACKLOG.md`, and `docs/DECISIONS.md`; update `FAILED_PATHS.md` only for a plausible reusable dead end.
