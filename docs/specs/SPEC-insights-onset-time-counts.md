# MindScale Phase 9 — Insights onset-time counts

Status: FROZEN — APPROVED

Owner: Codex

Date: 2026-08-04

Approval: On 2026-08-04, the user granted full project ownership and explicitly authorized Phase 9 specification, decisions, implementation, commits, pushes, pull requests, and merge without another review pause. D-1 through D-10 are frozen before application-code edits.

Governing product sources:

- `docs/specs/SPEC-insights-foundation.md`, especially the episode, hold, sleep, range, source-snapshot, local-time, failure, and insight-grammar rules
- `docs/specs/SPEC-insights-entry-chart.md`, especially the single-derivation, native rendering, accessibility, and unchanged-persistence decisions
- `docs/specs/SPEC-insights-onset-gap-histogram.md`, especially onset eligibility, sparse refusal, selection restoration, and one-snapshot architecture
- Local Claude Design handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\SPEC.md`, especially Axioms, Statistics, Insight grammar, and Views
- Local Claude Design handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html`, especially `Time of day it started`, the 24 hour counts, six-start refusal, four-hour count sentence, and displacement caveat

## Purpose

Add the next bounded Insights view: native counts of derived episode starts by local clock hour. The view restates when starts were recorded, refuses sparse samples, discloses the recording-time and current-time-zone limitations, and makes no claim about a diurnal pattern, cause, risk, diagnosis, or future episode.

## Product-source and backlog reconciliation

Already implemented:

- Phase 5 derives deterministic episodes from one reactive Entry/Sleep/Marker Room snapshot under the current 8/12/16/24 waking-hour hold and owns all six local-calendar ranges, future-fact exclusion, DST-safe range starts, scheduled invalidation, and error/Retry behavior.
- Phase 6 proves that another native descriptive projection can share the same immutable `InsightsSnapshot` without a second classifier or persistence change.
- Phase 8 derives selected-range onsets from the Phase 5 episode model, freezes a six-onset refusal rule, restores primitive selection, and preserves the single Room/settings Flow.
- The remaining backlog contains onset-time counts and descriptive sleep counts. The product source orders onset-time counts after the onset-gap histogram and before sleep facts.

Why onset-time counts are the smallest correct Phase 9 slice:

1. Every required fact already exists as a Phase 5 `DerivedEpisode.onsetMillis`; no new source row, DAO query, classifier, or cohort model is needed.
2. The product `SPEC.md` explicitly names time-of-day onset counts as the next view and supplies the approved descriptive grammar example.
3. Descriptive sleep counts require separate decisions for night/nap classification, range attribution of cross-boundary intervals, incomplete/open sleeps, and any post-wake window. Combining that work would make Phase 9 cross-cutting and less reviewable.
4. A 24-bin local-clock projection plus sparse refusal, exact denominators, selection, and caveats is independently useful and safely reversible.

Conflicts and ambiguities resolved by this spec:

1. The HTML says sparse shapes are "probably noise." Native Phase 9 does not characterize data as noise or reliability; it simply refuses the chart below six eligible starts.
2. The HTML groups onset hours with `new Date(onset).getHours()` but does not disclose that historical timestamps lack their original capture zone. Native Phase 9 explicitly states that all starts are projected through the device's current zone and re-bucket when that zone changes.
3. The HTML chooses the highest-count rolling four-hour window but does not freeze ties or the end boundary. Native Phase 9 uses 24 one-hour buckets, 24 wrapping four-hour windows, a lower-inclusive/upper-exclusive end, and earliest local start hour as the deterministic tie-break.
4. The HTML uses fixed-millisecond range arithmetic. Native Phase 9 retains the Phase 5 local-calendar range start and half-open `[rangeStart, now)` onset eligibility.
5. The HTML uses 24 narrow hover bars. Native Phase 9 uses horizontally reachable, individually selectable native cells with visible counts, persistent readout, and keyboard/screen-reader activation.
6. The product example says an onset "began" in a clock window. Native copy says a start "was recorded" in the window so it cannot overstate the observed timestamp as the unobserved symptom beginning.

## Exact bounded scope

1. Add one `Time of day it started` section to Insights immediately after `Days between onsets`.
2. Reuse Phase 5 derived episode onsets that fall inside the current selected range.
3. Count each eligible onset into exactly one of 24 local clock-hour buckets in the current device zone.
4. Refuse bucket rendering until the selected range contains at least six eligible onsets.
5. Show the exact start denominator and the number of local calendar dates covered since measurement began within the selected range.
6. Show one deterministic descriptive sentence for the highest-count wrapping four-hour clock window, with earliest-hour tie-breaking.
7. Let touch, keyboard/D-pad, and screen readers select any hour bucket and update one persistent visible/live readout.
8. Persist only the selected hour index across Activity/process recreation and clear it under the rules below.
9. Reuse the existing single-snapshot StateFlow/ViewModel/Room pipeline and existing loading/error/Retry behavior.

## Explicit non-goals

- No sleep facts, night/nap classification, short/long-night comparison, post-wake window, or direction-of-effect language.
- No dominant/typical onset time, "most common" hour, morning/afternoon/evening phenotype, diurnal pattern, circadian rhythm, periodicity, trend, range-to-range comparison, clustering, likelihood, risk, forecast, early warning, correlation, trigger detection, causal attribution, p-value, confidence interval, significance, diagnosis, advice, or population comparison.
- No persisted original-capture timezone, location, timezone history, or attempt to infer travel.
- No edit, deletion, creation, filtering, or export action from the chart.
- No new range, configurable bucket/window/threshold, setting, chart mode, animation requirement, or summary notification.
- No persisted onset-hour row, derived cache, Episode entity, backfill, or data repair.
- No Room migration, exported schema JSON change, JSON backup-format change, CSV change, Android backup-rule change, permission, or import behavior.
- No Report/Profile/Safety/breathing work.
- No WebView, JavaScript, bitmap prototype, chart library, navigation framework, repository layer, DI framework, state-management framework, time library, background worker, server, account, analytics, or permission.
- No toolchain, SDK, Gradle, AGP, Kotlin, Compose, Java, or dependency upgrade.

## Eligibility, minimum sample, and range attribution

- An eligible onset is an existing Phase 5 `DerivedEpisode.onsetMillis` satisfying `rangeStartMillis <= onsetMillis && onsetMillis < nowMillis`.
- Explicitly ended, assumed-ended, and ongoing episodes are eligible. Episode closure is irrelevant once the onset exists.
- A carried-in episode is not eligible because its onset is before the selected range. An episode whose onset is in range remains eligible even if its ending lies beyond the range.
- The current selected range, hold setting, source snapshot, injected `now`, and current device zone determine eligibility. There is no fallback to a larger range.
- `minimumOnsetCount` is exactly 6. The hour counts are eligible exactly when `eligibleOnsetCount >= 6`.
- Below six onsets, no hour cells, four-hour sentence, or empty axis is rendered. The refusal card states the exact count and exact number of additional starts required.
- The refusal threshold is a display-integrity rule, not a statistical claim. Copy never says six starts prove a pattern or reliability.
- The day denominator is `coveredLocalDayCount`: the number of distinct local calendar dates with positive-duration overlap between `[max(rangeStartMillis, firstEntryMillis), nowMillis)` in the current zone. It is not a count of entry days, clear days, or 24-hour millisecond blocks.
- A partial first date and the current partial date each count once when they have positive-duration overlap. Pre-first-entry dates do not count. DST-short and DST-long dates each count once.

Frozen sparse copy:

- Zero starts: `Needs 6 more recorded starts in this range before this chart is shown. There are 0 starts to count by hour.`
- One start: `Needs 5 more recorded starts in this range before this chart is shown. There is 1 start to count by hour.`
- Two through five starts: `Needs N more recorded starts in this range before this chart is shown. There are C starts to count by hour.`

Pluralization is deterministic: `1 start`; all other counts use `starts`.

## Local-time projection, timezone changes, DST, and midnight

1. Start only from completed Phase 5 `BuiltModel.episodes`; do not reclassify Entries for this view.
2. Filter eligible onsets through the selected half-open instant range before local-time conversion.
3. Convert each eligible onset instant with `Instant.ofEpochMilli(onset).atZone(zoneId)` using the same injected/current `ZoneId` as the rest of the snapshot.
4. Bucket by `ZonedDateTime.hour` in `0..23`. Minute, second, millisecond, and UTC offset do not change the bucket.
5. Bucket `h` means local clock time `[h:00:00.000, (h+1):00:00.000)`, wrapping hour 23 to local midnight for its label/readout only.
6. An onset exactly at local midnight enters hour 0. An onset exactly on any hour boundary enters the hour beginning at that boundary.
7. During a fall-back overlap, both occurrences of the repeated local hour enter the same hour bucket even though their instants/offsets differ. During a spring-forward gap, a nonexistent local hour can honestly have zero starts.
8. Stored records contain epoch timestamps but no original capture zone. Changing the device zone reprojects all eligible historical onsets and may change hour counts, covered local-day count, and the four-hour sentence; raw timestamps remain unchanged.
9. Zone changes also retain Phase 5 local-calendar range semantics, so onsets near a range boundary may enter or leave the selected range.
10. An onset exactly at `rangeStartMillis` is included. An onset exactly at `nowMillis` is excluded until a later recomputation makes it earlier than `now`. Future facts remain excluded and use the existing invalidation path.

## Frozen 24 hour buckets and deterministic labels

- The model always contains exactly 24 buckets in hour order `0..23`, including zero-count buckets and below threshold.
- Each bucket stores `hourOfDay` and `count`; its lower/upper local-clock boundaries are implied by the frozen hour index.
- Visible labels follow the existing time-format setting:
  - 12-hour: `12a`, `1a`, ..., `11a`, `12p`, `1p`, ..., `11p`;
  - 24-hour: `00`, `01`, ..., `23`.
- Spoken/readout boundaries follow the same setting and always state a half-open interval, for example `from 2:00 PM up to but not including 3:00 PM` or `from 14:00 up to but not including 15:00`.
- Every eligible onset belongs to exactly one bucket and `sum(bucket.count) == eligibleOnsetCount`.
- Cell fill height is `count / maximumBucketCount` for the eligible chart. Equal counts have equal heights, zero counts have zero fill, and every positive count has a small visible minimum fill while the exact numeric count remains visible.
- Bucket order is independent of locale, data, zone, range, theme, and font scale. Only the existing 12/24-hour label form changes.

## Four-hour window rule and deterministic sentence

- Form exactly 24 candidate local-clock windows, one beginning at each hour `0..23`.
- Candidate `h` contains buckets `h`, `h+1`, `h+2`, and `h+3`, with indices modulo 24. Its end is the start of bucket `h+4`, lower-inclusive and upper-exclusive.
- Window counts overlap by design; they are used only to choose the one displayed descriptive sentence and are never summed into a denominator.
- Choose the candidate with the largest summed count. If multiple candidates tie, choose the numerically earliest start hour in `0..23`.
- The sentence is `C of N recorded starts in this range were recorded from START up to but not including END.` using the existing 12/24-hour format. A midnight-wrapping window is labeled honestly, for example `from 10:00 PM up to but not including 2:00 AM`.
- The sentence reports an arithmetic maximum only. It never says `most common`, `typical`, `tends to`, `pattern`, `peak time`, `risk`, or an equivalent interpretation.

## Missing, invalid, tied, sparse, future, and extreme data

- Before the first Entry, keep the existing global `Nothing to draw yet` state and do not show an onset-time shell.
- When Entries exist but the selected range has fewer than six eligible onsets, show only the refusal card.
- Zero-value Entries do not create onsets. Sleep/Wake-kind positive Entries follow the Phase 5 episode rules; this view adds no kind-specific behavior.
- Same-timestamp Entries retain Phase 5's highest-id winner. Lower-id ties create no episode, onset, or hour count. Two episode onsets cannot share one timestamp under the completed engine.
- Multiple positive Entries within one continuing episode create no additional onset.
- Assumed hold expiry can allow a later positive Entry to create a new onset. A hold change can therefore re-derive historical hour counts without rewriting records.
- Sleep can change whether positive Entries form one or multiple episodes by pausing the hold clock. Sleep does not shift the local clock time of a retained onset.
- Episode end reason, episode duration, AUC, peak, chips, notes, markers, sleep count, and note-privacy preference do not affect hour membership, counts, threshold, or copy.
- Invalid Entry values, invalid sleep intervals, missing required projected columns, unknown discriminators, corrupt hold settings, bucket-sum mismatch, or invalid hour indices use the existing visible derivation failure/Retry path. No offending fact is skipped or repaired.
- Source edit/delete, hold change, future-fact activation, midnight, lifecycle resume, or zone change re-derives the complete immutable snapshot. No bucket is patched in place.
- If the view becomes ineligible, clear its selected hour. If it remains eligible, retain a valid selected hour and refresh the count/readout.
- Long historical ranges and extreme timestamps remain bounded by the selected range and Java time conversion. Arithmetic overflow or unrepresentable local time is a derivation error, not clamped output.

## Deterministic text and strict no-inference grammar

Always-visible eligible text:

- Denominator: `N recorded starts across D local calendar days in this range.`
- Default readout: `Select an hour to read its exact count.`
- Four-hour sentence: the frozen sentence defined above.
- Caveat: `These are the local times when you recorded a start, not necessarily when it began. Driving, meetings, and sleep can make recording happen later. Historical starts use your device's current time zone.`

Selected-hour readout:

- `C of N recorded starts were recorded B.` where `B` is the frozen spoken half-open hour boundary.
- Example: `2 of 8 recorded starts were recorded from 2:00 PM up to but not including 3:00 PM.`
- Zero-count hours remain selectable and report `0 of N`; they are not omitted.

Forbidden result language includes `typical time`, `usually`, `tends to`, `most common`, `dominant`, `peak time`, `pattern`, `diurnal`, `circadian`, `cycle`, `rhythm`, `likely`, `risk`, `trigger`, `causes`, `associated`, `correlated`, `predicts`, `significant`, `confidence`, `diagnosis`, advice, reassurance, or equivalent claims.

The fixed caveat may discuss recording displacement and current-zone projection only as limitations of the timestamp. Grammar tests check exact approved strings and scan generated result text for forbidden terms.

## User experience and UI sequence

- The section title is `Time of day it started`.
- It appears immediately after `Days between onsets`; descriptive sleep counts remain absent.
- The existing range chips are the only onset-time range control. Changing range clears onset-time selection together with raster, entry-chart, and onset-gap selections.
- When eligible, the card contains a horizontally scrollable native Compose row of exactly 24 fixed-order hour cells. Each cell shows its exact count, geometry-scaled fill, and visible hour label.
- Each hour cell is selectable by touch and keyboard/D-pad. Selection updates selected state and the persistent readout below the row.
- The denominator, four-hour sentence, and caveat remain visible without selecting a cell.
- Horizontal motion scrolls the hour row. Vertical swipes remain owned by the parent Insights list; no cell gesture detector traps vertical scrolling.
- Loading, global empty, filtered-empty, stale/error banner, and Retry behavior remain owned by the existing Insights screen.
- No meaning exists only in hover, tooltip, animation, color, or precise pointer position.

## Frozen interfaces and data contracts

The pure Insights model adds equivalents of:

```kotlin
const val MIN_ONSET_TIME_COUNT = 6

data class OnsetHourBucket(
    val hourOfDay: Int,
    val count: Int
)

data class OnsetTimeCounts(
    val eligibleOnsetCount: Int,
    val coveredLocalDayCount: Int,
    val minimumOnsetCount: Int = MIN_ONSET_TIME_COUNT,
    val buckets: List<OnsetHourBucket>,
    val fourHourWindowStartHour: Int,
    val fourHourWindowCount: Int
) {
    val isEligible: Boolean
        get() = eligibleOnsetCount >= minimumOnsetCount
}
```

- `InsightsSnapshot` adds exactly one immutable `onsetTimeCounts: OnsetTimeCounts` derived inside `deriveInsights` from the same `BuiltModel.episodes`, first effective Entry, range, `now`, and zone as every existing output.
- `buckets` always contains 24 hour-indexed counts, including below threshold. Sparse UI does not expose the distribution.
- The four-hour fields are calculated deterministically even below threshold but are not rendered while ineligible.
- UI/copy helpers accept the existing `HourFormat`; they do not derive counts in composables.
- `InsightsUiState` adds `selectedOnsetHour: Int?`.
- `InsightsViewModel` persists that selection as one primitive `Int` in `SavedStateHandle`, validates it against `0..23`, clears it on every range change, clears it when onset-time counts become ineligible, and otherwise retains it through re-derivation/recreation.
- `InsightsViewModel.selectOnsetHour(hour: Int)` ignores invalid hours or an ineligible/missing snapshot and never mutates derived data.
- No event writes Room data.

## StateFlow, ViewModel, cancellation, retry, invalidation, and concurrency

- Keep the existing `combine(sourceDao.observeSource(), settingsDao.observe())`, range Flow, lifecycle/time invalidation, off-main derivation, immutable `InsightsSnapshot`, and public `StateFlow<InsightsUiState>`.
- Onset-time derivation occurs inside the same cancellable off-main `deriveInsights` call. Add no second Flow, nested collector, repository, cache, mutex, or UI-side count derivation.
- The existing Room UNION projection already supplies every required Entry/Sleep fact in one consistent SQLite read snapshot. No DAO query or interface change is needed.
- The settings Flow supplies hold and time-format values. Hold changes re-derive counts; time-format changes only reformat labels/copy while the count snapshot remains coherent.
- Every published snapshot contains range, onset eligibility, 24 counts, window sentence inputs, and all prior Insight outputs for one source/settings/time/zone derivation. Partial mutation is forbidden.
- Preserve current collection cancellation/ordering: `flatMapLatest` cancels replaced source collection work, `combine` emits ordered inputs, and only whole derivations publish. No independent coroutine may publish stale hour counts after newer input.
- Marker-only invalidation may re-run derivation but must not alter onset-time counts when Entry/Sleep/hold/time/range/zone are unchanged.
- Existing future-fact, hold-expiry, and midnight invalidations remain in-process and lifecycle-bound. Add no alarm, service, worker, permission, or wake lock.
- Retry continues to create a new Room collection after a terminal failure. Phase 9 adds no separate loading/error state.

## Room, schema, backup, export, privacy, permission, and architecture implications

- Room remains version 4. No entity, column, index, migration, trigger, view, exported schema JSON, seed SQL, or DAO SQL changes.
- Deterministic JSON backup remains format version 4 and exports raw records/settings only, not onset-time buckets, window summaries, or UI selection.
- CSV remains unchanged and raw-record-only. Android automatic-backup/data-extraction rules remain unchanged.
- Selected onset hour is transient `SavedStateHandle` UI state, not Room, JSON, CSV, preferences, or process-global state.
- Notes, chips, and marker text are not rendered in this section. The existing `hideNotes` preference is therefore irrelevant and no private text is exposed.
- No storage, Internet, notification, alarm, location, health, accessibility-service, or other permission is added.
- Manual DI, process-scoped `AppContainer`, Room, StateFlow, SavedStateHandle, Kotlin, Compose, and Material 3 remain the complete architecture.
- The implementation is fully local/offline and creates no account, identifier, network request, analytics event, or server state.

## Accessibility, TalkBack, keyboard/D-pad, large-font, rotation, and color

- Heading, denominator, four-hour sentence, readout, and caveat are normal Compose text and scale with user font settings; no text is painted into Canvas or a bitmap.
- The eligible view exposes exactly 24 focusable/selectable hour nodes in logical `0..23` order. Each is at least 48 dp by 48 dp with button role, selected state, visible label, and a coherent description equivalent to `2 PM hour, 2 of 8 recorded starts, from 2:00 PM up to but not including 3:00 PM`.
- Decorative fills are absent from the semantics tree. The section does not duplicate all bucket descriptions as another focus target.
- The selected-hour readout is visible and a polite live region. Touch, keyboard/D-pad activation, and TalkBack activation update the same text.
- The horizontal row exposes normal scroll semantics and focus can reach all 24 cells without precise touch. Parent vertical scrolling works when a gesture begins over a cell.
- Every cell shows numeric count and label; height and gold color are redundant. Selection adds border/shape emphasis, not color alone.
- Positive/zero fills, selected/unselected state, card/background, counts, and labels remain legible in light/dark and distinguishable in grayscale.
- At 150% font scale, labels/counts may wrap or cells may grow; critical text cannot clip or overlap. Horizontal scrolling is preferred to shrinking text.
- Rotation, screen magnification, keyboard/D-pad traversal, edge-to-edge insets, and process recreation remain usable. Row scroll position need not restore; selected hour does.
- Below threshold, the refusal card is normal readable text and has no hidden chart semantics.

## Failure behavior

- Source/settings read failure, corrupt data, or derivation exception follows the existing initial-error or stale-snapshot banner and working Retry path. Raw data remains unchanged.
- A bucket-sum mismatch, invalid hour, invalid four-hour window, overflow, or unrepresentable zone conversion is a derivation/programming error. Do not drop an onset or show partial counts.
- Invalid restored hour indices are discarded. Missing selection uses the default readout and is not an error.
- If a selected hour's count changes, retain its index while eligible and display the new count. If eligibility falls below six, clear selection and show refusal copy.
- If input changes arrive during derivation, existing cancellation/serialized publication rules apply; never patch or flash partial hour counts.
- Focus/click outside a cell changes nothing. Horizontal scroll boundaries clamp normally.

## Test strategy

### Pure unit tests

- Exactly six eligible onsets produce an eligible 24-bucket result; zero through five refuse with exact missing-start/count copy.
- Seven or more onsets keep `sum(counts) == eligibleOnsetCount`; zero-count hours remain present.
- Both range endpoints use `[rangeStart, now)`: carried-in onset excluded, exact-start included, exact-now/future excluded.
- Every exact hour boundary, minute/second within an hour, hour 23, and local midnight enters the correct bucket.
- All six existing range selections drive the same onset-time projection.
- Current-zone conversion re-buckets fixed instants deterministically; zone changes do not mutate raw records.
- Spring-forward nonexistent hour remains empty when appropriate; repeated fall-back hour combines both offsets into one bucket.
- Covered local-day count handles partial first/current days, DST 23/25-hour dates, first Entry before/inside the range, and pre-measurement dates.
- Four-hour windows wrap midnight, use exactly four buckets, choose the maximum, and break ties at the earliest numeric start hour.
- Same-timestamp Entry ties, continuing positives, explicit zero, assumed hold split, ongoing episode, and sleep-paused hold reuse Phase 5 onset semantics.
- Markers, notes, chips, episode ending, peak, AUC, and note privacy never change counts.
- Exact sparse, denominator, selected readout, four-hour sentence, and caveat strings pass in 12/24-hour forms; generated result copy contains no forbidden inference terms.

### ViewModel tests

- Source, hold, range, scheduled time, lifecycle refresh, and zone seam changes publish onset-time data in the same immutable snapshot as prior outputs.
- Selected hour persists through a new `InsightsViewModel`/`SavedStateHandle`; invalid restored indices are discarded.
- Selection clears on range change or ineligibility, remains independent of raster/chart/gap selections, and refreshes its count while eligible.
- Invalid/ineligible selection events are no-ops.
- Time-format changes update UI state without changing bucket membership.
- Terminal source failure Retry and stale-snapshot behavior remain green.

### Compose/UI/accessibility tests

- Global empty state has no onset-time shell. Populated below-threshold state shows exact refusal copy and no hour row/four-hour sentence.
- Eligible state shows title, denominator, 24 buckets in fixed order, exact counts, labels, four-hour sentence, default readout, and caveat.
- Each cell has at least a 48 dp target, coherent role/selected semantics, exact count/boundary description, and deterministic traversal.
- Touch/semantic activation selects an hour and updates the same visible polite-live-region readout.
- Horizontal scrolling reaches hour 23; a vertical swipe beginning over a cell scrolls the parent list.
- Zero-count cells remain selectable and announce zero honestly.
- 12/24-hour modes, light/dark, grayscale/non-color selection, rotation, and 150% font scaling retain critical text without clipping.

### Room/instrumented and regression tests

- Existing `EpisodeSourceDaoTest` remains green with no query/schema change; an explicit marker/note mutation regression may assert identical onset-time counts when useful.
- Instrumented Insights fixtures verify sparse and eligible states, selection/semantics, horizontal reachability through hour 23, parent vertical scrolling, and Activity recreation through the ViewModel path.
- Navigation, range selection, Settings return, prior selection restoration, and all Phase 1–8 connected tests remain green.
- Database remains version 4; migrations, schema, JSON v4, CSV, erase/reset, backup rules, and permission checks remain unchanged.

### Full oracles and manual API 36 verification

Run from the repository root with the bundled JDK, configured S:-based SDK/AVD/cache, and project wrapper:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Install and launch the debug app on `MindScale_API_36`. Manually inspect sparse and eligible data, all six ranges, 12/24-hour labels, hour selection, hour-23 reachability, parent vertical scrolling, the four-hour sentence including a midnight wrap fixture, current-zone/DST behavior where practical, Activity/process restoration, light/dark, rotation, TalkBack/keyboard semantics, and 150% font scaling. Restore emulator font, theme, rotation, and timezone settings afterward.

## Machine-checkable acceptance criteria

- [x] SPEC/APPROVAL: this complete spec and D-1 through D-10 are approved and frozen before the first application-code edit; Git history proves the documentation-only approval commit precedes implementation.
- [ ] BOUNDARY: Phase 9 adds only `Time of day it started`; descriptive sleep, Report, import, Safety, breathing, and inferential analysis remain absent.
- [ ] SOURCE: `InsightsSnapshot.onsetTimeCounts` derives only from existing Phase 5 episodes, first Entry, selected range, `now`, and zone in the same pure derivation; no parallel classifier or UI-side counting exists.
- [ ] RANGE/TIME: tests prove half-open onset eligibility, all six ranges, 24 exact local-hour buckets, midnight, current-zone reprojection, DST overlap/gap, and covered-local-day arithmetic.
- [ ] SAMPLE: tests prove the fixed six-onset threshold, exact 0–5 refusal arithmetic/copy, and no chart/window sentence below threshold.
- [ ] WINDOW: tests prove all 24 wrapping four-hour windows, exact four-bucket membership, maximum selection, earliest-hour tie-break, and honest 12/24-hour boundary copy.
- [ ] GRAMMAR: denominator, selected readout, window sentence, and displacement/current-zone caveat match approved deterministic text and contain no positive inference, causal, predictive, correlation, significance, diagnostic, or dominant-time claim.
- [ ] UI: exactly 24 native Compose hour cells render in fixed order with visible count/label, relative geometry, selection, denominator, sentence, readout, and caveat; no chart dependency/WebView/bitmap exists.
- [ ] ACCESSIBILITY: each cell is a coherent at-least-48-dp selectable node with exact count/boundary semantics; live readout, keyboard/D-pad/TalkBack, horizontal reachability, vertical-scroll ownership, non-color encoding, light/dark, rotation, and 150% font pass focused tests/manual review.
- [ ] STATE: primitive selected hour restores independently through `SavedStateHandle`, clears on range/ineligibility, rejects invalid values, and refreshes safely on source/hold/time/zone changes.
- [ ] CONCURRENCY: the existing single Room/settings Flow, cancellable off-main immutable derivation, invalidation, and Retry remain the only data path; snapshots publish whole and internally consistent.
- [ ] PERSISTENCE/PRIVACY: Room/schema/JSON remain version 4, CSV/backup rules/permissions remain unchanged, no note/marker text is exposed, and no durable derived state or architecture dependency is added.
- [ ] REGRESSION: all Phase 1–8 JVM and connected behavior remains green, including transactional onset classification and prior Insights views/selections.
- [ ] ORACLES: wrapper `test`, `lint`, `assembleDebug`, intended-device identity, `connectedDebugAndroidTest`, installed-app walkthrough, and `git diff --check` pass.
- [ ] DOCUMENTATION: spec status/evidence, `PROJECT_STATE.md`, `docs/specs/BACKLOG.md`, `docs/DECISIONS.md`, and any genuinely reusable `FAILED_PATHS.md` finding are updated without duplicating active work.
- [ ] REVIEW: one critical-path review covers source/range attribution, local-time and zone semantics, DST, hold/sleep interaction, window arithmetic, grammar, accessibility, restoration, concurrency, persistence, privacy, and rollback; every blocking finding is resolved before publication.

## Task decomposition after approval

1. Freeze this spec and record D-009; update active state/backlog ownership and commit the documentation-only approval state.
2. Add onset-time models, pure derivation/copy helpers, and exhaustive JVM tests.
3. Add ViewModel primitive selection/restoration and focused tests.
4. Add the native accessible sparse/eligible section and Compose tests.
5. Keep Room/query/schema unchanged and add only proportionate instrumented regression coverage.
6. Run the full oracle suite and API 36 manual accessibility/visual walkthrough.
7. Run one critical-path review, resolve findings, update durable evidence, commit, push, open/ready/merge the PR, synchronize main, and record the phase boundary.

## Required durable-document updates

### Before application-code edits

- Keep this spec `FROZEN — APPROVED` and D-1 through D-10 frozen.
- Add `D-009 — Phase 9 onset-time counts` to `docs/DECISIONS.md`.
- Update `PROJECT_STATE.md` with branch/base, governing spec, approval, selected boundary, and exact first implementation task.
- Remove onset-time counts from `BACKLOG.md`; leave descriptive sleep counts as unstarted work.

### After implementation and verification

- Mark this spec `IMPLEMENTED — VERIFIED LOCALLY`, check all criteria, and record exact oracle/manual/review evidence, commits, and PR state.
- Update `PROJECT_STATE.md` with branch/head, changed files, verification counts/results, manual checks, risk/coverage gaps, blocker, and next action.
- Keep `BACKLOG.md` limited to unstarted work. Add a `FAILED_PATHS.md` entry only for a plausible approach future agents might repeat.
- Add a superseding decision instead of rewriting D-009 if a frozen contract materially changes.

## Rollout and safe rollback

- Rollout is source publication only after every acceptance criterion passes. No runtime feature flag or data backfill is required.
- No Room/backup migration exists. Pre-Phase 9 and Phase 9 builds read the same version-4 database and raw exports.
- Before release, rollback is a source-control revert of onset-time model/ViewModel/UI/tests/docs. Older builds ignore the unknown primitive SavedState key; raw data is unaffected.
- After release, a source forward-fix or revert remains safe because Phase 9 creates no durable data, permission, external contract, or background work.

## Decisions and approval gate

- **D-1 (bounded scope):** Phase 9 adds only 24 local-clock onset counts, sparse refusal, one deterministic four-hour count sentence, selection, and caveats. Descriptive sleep counts remain deferred.
- **D-2 (single derivation):** derive eligible starts only from the existing Phase 5 episode model inside the same `deriveInsights` snapshot; add no second onset classifier.
- **D-3 (range and day denominator):** use half-open `[rangeStart, now)` onsets and count covered local calendar dates from `max(rangeStart, firstEntry)` through `now`; exclude carried-in onsets and pre-measurement dates.
- **D-4 (sample refusal):** freeze eligibility at six onsets; show exact missing/count copy and no distribution or window sentence below threshold.
- **D-5 (local time and zone):** bucket by current-zone local hour `0..23`, combine repeated DST hours, permit missing gap hours, include midnight in hour 0, and disclose that zone changes reproject history.
- **D-6 (four-hour sentence):** sum exactly four wrapping adjacent hour buckets, choose the largest count, break ties at the earliest numeric hour, and state lower-inclusive/upper-exclusive boundaries without `most common` or equivalent language.
- **D-7 (grammar):** show counts, denominators, exact hour boundaries, and the approved recording-displacement/current-zone caveat only; make no dominant-time, diurnal, causal, predictive, diagnostic, correlation, or significance claim.
- **D-8 (accessibility):** use 24 native, individually selectable, at-least-48-dp Compose cells with visible count/label, persistent live readout, horizontal reachability, parent vertical-scroll ownership, and redundant non-color encoding.
- **D-9 (state/concurrency):** persist only a validated selected hour in `SavedStateHandle`; retain the existing single Room/settings Flow, cancellable off-main immutable derivation, invalidation, and Retry behavior.
- **D-10 (persistence/architecture):** keep Room/schema/JSON at version 4, CSV/backup rules/privacy/permissions unchanged, and add no framework, dependency, cache, module, server, account, analytics, or background work.

Approval gate is satisfied. D-1 through D-10 are frozen; a material scope or interface change requires a documented spec amendment before implementation continues.
