# MindScale Phase 8 — Insights onset-gap histogram

Status: FROZEN — APPROVED

Owner: Codex

Date: 2026-08-04

Last verified commit: `86bd313e47515bd81eded5e03796195a3b389bff`

Approval: On 2026-08-04, the user reiterated full project ownership and explicitly authorized all decisions, implementation, commits, pushes, and PRs without further approval. This satisfies the Phase 8 approval gate; D-1 through D-10 are frozen before application-code edits.

Governing product sources:

- `docs/specs/SPEC-insights-foundation.md`, especially the frozen episode, hold, sleep, range, source-snapshot, failure, and insight-grammar rules
- `docs/specs/SPEC-insights-entry-chart.md`, especially the single-derivation, native rendering, accessibility, and unchanged-persistence decisions
- Local Claude Design handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\SPEC.md`, especially Axioms, Statistics, Insight grammar, and Views
- Local Claude Design handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html`, especially `Days between onsets`, the default six-episode threshold, ten prototype buckets, touch disclosure, and caveat

## Purpose

Add the next bounded Insights view: a native histogram that counts elapsed time from one derived episode onset to the next. It gives the user and clinician a compact episode-level record summary while refusing sparse samples and making no claim about periodicity, causes, likelihood, or future episodes.

## Product-source and backlog reconciliation

Already implemented:

- Phase 5 derives deterministic episodes from one reactive Entry/Sleep/Marker Room snapshot under the current 8/12/16/24 waking-hour hold, including explicit, assumed, and ongoing endings.
- Phase 5 owns all six local-calendar ranges, same-timestamp Entry collapse, future-fact exclusion, DST-safe range starts, scheduled invalidation, error/Retry behavior, and strict descriptive grammar.
- Phase 6 adds the step chart without a second derivation path and proves that one `InsightsSnapshot` can own another native descriptive projection without a schema, backup, export, permission, or dependency change.
- The remaining Insights backlog lists onset-gap histogram, onset-time counts, and descriptive sleep counts. The product source orders the onset-gap histogram immediately after the raster and step chart.

Conflicts and ambiguities resolved by this spec:

1. The product rationale calls the histogram “periodicity with no modeling,” but its grammar forbids the app from interpreting a pattern. Phase 8 displays counts only and never labels a cycle, rhythm, recurrence tendency, or periodicity.
2. The HTML makes `minEpisodes` configurable and defaults it to six. Native Phase 8 freezes the refusal threshold at six eligible onsets, producing five gaps; it adds no setting.
3. The HTML’s sparse copy derives the number of missing starts from the gap count and can undercount when there are no starts. Native copy derives it directly as `6 - eligibleOnsetCount`.
4. The HTML labels buckets with rounded-looking day values. Native Phase 8 freezes exact lower-inclusive, upper-exclusive elapsed-time boundaries and uses readout text that states those boundaries without rounding a gap into a bucket.
5. The HTML caveat says a gap means “nothing” was recorded between starts, which is too broad because ratings may occur within an episode. Native copy says only that no other derived episode start occurred between the two starts.
6. The HTML uses hover-shaped bars. Native Phase 8 uses ten selectable Compose bar cells, visible counts, a persistent live readout, and keyboard/screen-reader access; no information is hover-only or color-only.
7. The existing ranges end at injected `now`. Histogram eligibility uses only onsets inside the selected half-open window; it does not pull a pre-range onset into the first gap or silently widen the range.

## Exact bounded scope

1. Add one `Days between onsets` section to the existing Insights list after the current `Each episode` rows (or after the episode facts when there are no rows).
2. Derive eligible onsets from the existing Phase 5 `DerivedEpisode` model and current selected range.
3. Calculate consecutive onset-to-onset elapsed intervals and count them into ten frozen buckets.
4. Refuse to render bars until the selected range contains at least six eligible onsets and therefore five gaps.
5. Render the eligible histogram using native Compose layout primitives with visible counts and labels.
6. Let touch, keyboard/D-pad, and screen readers select any bucket and update one persistent visible/live readout.
7. Persist only the selected bucket index across Activity/process recreation and clear it when range eligibility changes as defined below.
8. Reuse the existing one-snapshot StateFlow/ViewModel/Room pipeline and existing failure behavior.

## Non-goals

- No time-of-day onset counts, four-hour window sentence, displacement analysis, or diurnal claim.
- No sleep comparison, short/long-night grouping, nap comparison, or direction-of-effect language.
- No median/mean/typical gap fact, mode callout, “most common” bucket, trend comparison, range-to-range comparison, periodicity score, cycle label, recurrence rate, forecast, early warning, correlation, trigger detection, causal attribution, p-value, confidence interval, or significance language.
- No gap measured from episode end to the next onset; no awake-time subtraction from onset-to-onset gaps.
- No editing, deletion, filtering, export, or creation of records from the histogram.
- No new range, user-configurable bucket, user-configurable sample threshold, chart mode, or setting.
- No persisted gap, histogram, Episode entity, derived cache, backfill, or database repair.
- No Room migration, schema JSON change, JSON backup-format change, CSV change, Android backup-rule change, or import behavior.
- No Report/Profile/Safety/breathing work.
- No WebView, JavaScript, bitmap prototype, chart library, navigation framework, repository layer, DI framework, state-management framework, background worker, server, account, analytics, or permission.
- No toolchain, SDK, Gradle, AGP, Kotlin, Compose, Java, or dependency upgrade.

## Eligibility and minimum-sample rules

- An eligible onset is the `onsetMillis` of an existing Phase 5 `DerivedEpisode` satisfying `rangeStartMillis <= onsetMillis && onsetMillis < nowMillis`.
- Explicitly ended, assumed-ended, and ongoing episodes are all eligible. Closure is not required because an onset-to-onset interval is known once the later onset exists.
- An episode carried into the range is not an eligible onset. It remains disclosed by the existing episode facts, but Phase 8 does not use it to form a cross-boundary first gap.
- An eligible episode that ends outside the selected range remains eligible because its onset is inside the range.
- Eligibility is recalculated under the currently selected range, current hold setting, current source snapshot, injected `now`, and current device zone. There is no fallback to a larger range.
- `minimumOnsetCount` is exactly `6`. The chart is eligible exactly when `eligibleOnsetCount >= 6`.
- For `N` eligible onsets, `gapCount` is exactly `max(0, N - 1)`. An eligible chart therefore has at least five gaps.
- Below six onsets, no bars or empty axes are rendered. The refusal card states the exact eligible onset count, exact gap count, and `max(0, 6 - eligibleOnsetCount)` additional starts needed.
- The refusal threshold is a display integrity rule, not a statistical claim. The copy must not say that six starts prove a pattern or make the result reliable.

Frozen sparse copy:

- Zero starts: `Needs 6 more recorded starts in this range before this chart is shown. There are 0 onset-to-onset gaps to count.`
- One start: `Needs 5 more recorded starts in this range before this chart is shown. There are 0 onset-to-onset gaps to count.`
- Two through five starts: `Needs N more recorded starts in this range before this chart is shown. These starts make G onset-to-onset gaps.`

Pluralization is deterministic: `1 recorded start`, all other counts `recorded starts`; `1 onset-to-onset gap`, all other counts `onset-to-onset gaps`.

## Onset-gap calculation and timestamp boundaries

1. Start from the completed Phase 5 `BuiltModel.episodes`; do not reclassify Entries in a histogram-specific engine.
2. Select eligible onsets using the half-open range `[rangeStartMillis, nowMillis)`.
3. Sort eligible onsets ascending by epoch millisecond. Phase 5 episode derivation is already deterministic; Phase 8 nevertheless requires strict ascending order before differencing.
4. For each adjacent pair, calculate `laterOnsetMillis - earlierOnsetMillis` as an exact elapsed duration in milliseconds.
5. Every calculated gap must be strictly positive. A zero, negative, or overflowing difference is an internal derivation error and uses the existing visible derivation error/Retry path; it is never clamped, dropped, made absolute, or assigned to a bucket.
6. A “day” in the histogram is exactly `86_400_000` elapsed milliseconds. Gaps do not count local date boundaries and do not subtract sleep or episode duration.
7. DST transitions therefore preserve real elapsed time: two starts at the same local clock time across spring-forward are 23 elapsed hours; across fall-back they are 25 elapsed hours. The selected range boundary remains local-calendar/DST-safe under Phase 5 rules.
8. Device-zone changes may change which onsets fall inside a local-calendar range, so the snapshot is re-derived. They do not alter the elapsed difference between two retained epoch timestamps.
9. An onset exactly at `rangeStartMillis` is included. An onset exactly at `nowMillis` is excluded until a later recomputation makes it earlier than `now`.
10. Future Entries and future episode onsets remain excluded without mutation and use the existing next-invalidation boundary.

## Frozen histogram buckets and labels

Bucket membership is lower-inclusive and upper-exclusive. `DAY_MILLIS` is exactly `86_400_000L`.

| Index | Millisecond interval | Elapsed-day interval | Visible label | Spoken/readout boundary |
|---:|---|---|---|---|
| 0 | `[0, 1 * DAY_MILLIS)` | `[0, 1)` | `<1d` | `under 1 elapsed day` |
| 1 | `[1 * DAY_MILLIS, 2 * DAY_MILLIS)` | `[1, 2)` | `1d` | `at least 1 and under 2 elapsed days` |
| 2 | `[2 * DAY_MILLIS, 3 * DAY_MILLIS)` | `[2, 3)` | `2d` | `at least 2 and under 3 elapsed days` |
| 3 | `[3 * DAY_MILLIS, 4 * DAY_MILLIS)` | `[3, 4)` | `3d` | `at least 3 and under 4 elapsed days` |
| 4 | `[4 * DAY_MILLIS, 5 * DAY_MILLIS)` | `[4, 5)` | `4d` | `at least 4 and under 5 elapsed days` |
| 5 | `[5 * DAY_MILLIS, 6 * DAY_MILLIS)` | `[5, 6)` | `5d` | `at least 5 and under 6 elapsed days` |
| 6 | `[6 * DAY_MILLIS, 7 * DAY_MILLIS)` | `[6, 7)` | `6d` | `at least 6 and under 7 elapsed days` |
| 7 | `[7 * DAY_MILLIS, 10 * DAY_MILLIS)` | `[7, 10)` | `7–9d` | `at least 7 and under 10 elapsed days` |
| 8 | `[10 * DAY_MILLIS, 14 * DAY_MILLIS)` | `[10, 14)` | `10–13d` | `at least 10 and under 14 elapsed days` |
| 9 | `[14 * DAY_MILLIS, +∞)` | `[14, +∞)` | `14+d` | `at least 14 elapsed days` |

- Exact boundary values enter the bucket beginning at that boundary: exactly 1 day is `1d`, exactly 7 days is `7–9d`, exactly 10 days is `10–13d`, and exactly 14 days is `14+d`.
- Fractional elapsed days are never rounded before bucketing.
- Every valid gap belongs to exactly one bucket; the sum of all ten counts must equal `gapCount`.
- Extreme valid gaps are retained in `14+d`; no maximum cutoff, truncation, winsorization, or overflow bucket is added.
- Bar height is `count / maximumBucketCount` for the current eligible histogram. Equal counts have equal heights, zero counts have zero fill, and every positive count has a small visible minimum fill while its exact numeric count remains visible.
- Bucket order and labels are constants, independent of locale, range, theme, font scale, or data.

## Missing, invalid, tied, sparse, and changing data

- Before the first Entry, keep the existing global `Nothing to draw yet` state and do not show a histogram shell.
- When Entries exist but the range has fewer than six eligible onsets, show only the refusal card described above.
- Zero-value Entries do not create onsets. Sleep/Wake-kind positive Entries follow the existing Phase 5 rule and may create or continue an episode; the histogram never adds a kind-specific rule.
- Same-timestamp Entries retain Phase 5’s highest-id winner. Lower-id ties create no episode, onset, gap, count, or spoken item. A highest-id zero can eliminate a would-be onset at that timestamp.
- Multiple positive Entries inside one continuing episode create no additional onset or gap.
- An assumed hold expiry can allow a later positive Entry to create a new onset. Changing the waking-hour limit can therefore deterministically change historical onset and gap counts without changing raw records.
- Sleep pauses the hold clock and can therefore affect whether two positive Entries belong to one episode, but sleep time is not subtracted from a valid onset-to-onset gap.
- Invalid Entry values, invalid sleep intervals, missing required projected columns, unknown source discriminators, or corrupt hold settings keep the existing honest derivation failure behavior. Phase 8 does not skip the offending fact or render a partial histogram.
- Marker rows never affect onset eligibility, gaps, bucket counts, sample threshold, or text.
- Source deletion/edit, hold changes, future-fact activation, midnight, lifecycle resume, or zone changes re-derive the complete immutable snapshot. No stale count is patched in place.
- If the histogram becomes ineligible, clear its selected bucket. If it remains eligible, preserve a valid selected bucket index and update its readout from the new counts.

## Deterministic descriptive text and strict no-inference language

Always-visible eligible text:

- Denominator: `G onset-to-onset gaps from N recorded starts in this range.`
- Default readout: `Select a bar to read its exact count.`
- Caveat: `Starts are assembled from your ratings using the current waking-hour limit. Each gap is elapsed time from one start in this range to the next. The bars do not identify a cycle, cause, or prediction.`

Selected-bucket readout:

- `C of G onset-to-onset gaps were B.` where `B` is the frozen spoken/readout boundary from the bucket table.
- Example: `2 of 5 onset-to-onset gaps were at least 1 and under 2 elapsed days.`
- Zero-count buckets remain selectable and say `0 of G ...`; they are not omitted.

Forbidden displayed terms and constructions include:

- `cycle`, `cyclical`, `periodic`, `pattern`, `rhythm`, `recurs every`, `usually`, `tends to`, `most common`, `likely`, `risk`, `trigger`, `causes`, `associated`, `correlated`, `predicts`, `significant`, `confidence`, `diagnosis`, or equivalent claims;
- a sentence selecting one bucket as the user’s dominant/typical interval;
- advice, reassurance, treatment interpretation, or comparison with population data.

The fixed caveat may use `do not identify a cycle` only as an explicit negation. Grammar tests check exact approved strings and scan non-caveat output for banned inferential terms.

## User experience and layout

- The section title is `Days between onsets`.
- It appears after the current `Each episode` rows. If the range has no episode rows but the page is not globally empty, it appears after the episode facts.
- The existing range chips are the only histogram range control. Changing range clears the selected histogram bucket along with the existing raster/chart selections.
- When eligible, the card contains a horizontally scrollable native Compose row of exactly ten fixed-order bar cells. Each cell shows its exact numeric count, a geometry-scaled bar, and its visible label.
- Each bar cell is selectable by touch and keyboard/D-pad. Selecting it updates selected state and the persistent readout below the bars.
- Horizontal motion scrolls the bucket row. Vertical swipes remain owned by the parent Insights list; no bar gesture detector may trap vertical scrolling.
- The denominator and caveat remain visible without selecting a bar. No meaning exists only in an animation, tooltip, hover state, or color.
- Changing range, source data, hold duration, time, or zone updates the card from the new immutable snapshot without animation being required for correctness.
- Existing global loading, filtered-empty, stale/error banner, and Retry states remain authoritative. A last successful snapshot may remain visible under the existing stale banner.

## Frozen interfaces and data contracts

The pure Insights model adds equivalents of:

```kotlin
const val MIN_ONSET_COUNT = 6

data class OnsetGapBucket(
    val index: Int,
    val lowerBoundMillisInclusive: Long,
    val upperBoundMillisExclusive: Long?,
    val visibleLabel: String,
    val spokenBoundary: String,
    val count: Int
)

data class OnsetGapHistogram(
    val eligibleOnsetCount: Int,
    val gapCount: Int,
    val minimumOnsetCount: Int,
    val buckets: List<OnsetGapBucket>
) {
    val isEligible: Boolean
        get() = eligibleOnsetCount >= minimumOnsetCount
}
```

- `InsightsSnapshot` adds exactly one immutable `onsetGapHistogram: OnsetGapHistogram` derived inside `deriveInsights` from the same `BuiltModel` as summaries, raster, entry chart, and recent episodes.
- `buckets` always contains the ten frozen bucket definitions and deterministic counts in index order, including below threshold. When ineligible, UI renders only the refusal card and does not expose the sparse bucket distribution.
- The engine may keep the individual gap durations private; UI and saved state need only counts and bucket definitions.
- `InsightsUiState` adds `selectedOnsetGapBucketIndex: Int?`.
- `InsightsViewModel` persists that selection as one primitive `Int` in `SavedStateHandle`, validates it against `0..9`, clears it on every range change, clears it when the histogram becomes ineligible, and otherwise retains it across re-derivation/recreation.
- `InsightsViewModel.selectOnsetGapBucket(index: Int)` ignores invalid indices or an ineligible/missing snapshot and never mutates the derived snapshot.
- No new user event changes Room data.

## StateFlow, ViewModel, DAO, and concurrency behavior

- Keep the existing `combine(sourceDao.observeSource(), settingsDao.observe())`, range flow, time invalidation, off-main derivation, immutable `InsightsSnapshot`, and public `StateFlow<InsightsUiState>`.
- Histogram derivation occurs in the same off-main pure `deriveInsights` call. Do not add a second Flow, nested collector, repository, cache, mutex, or UI-side derivation.
- The existing `EpisodeSourceDao` projection already supplies every required Entry/Sleep fact in one consistent SQLite read snapshot. No query or DAO interface change is needed.
- The existing settings Flow supplies `holdDuration` in the same combined read. A hold change creates one newly derived snapshot; the UI never combines histogram counts from one hold with summaries from another.
- Concurrent Entry/Sleep insert, edit, or delete is serialized by Room and observed through normal invalidation. Every Flow emission is derived atomically into one immutable UI snapshot; partial bucket mutation is forbidden.
- Preserve the current serialized collection and dispatcher semantics: every published snapshot is internally consistent for one source/settings/range/time input, and later emissions replace earlier snapshots in collection order. Do not launch independent derivations that can complete out of order.
- Marker changes may invalidate the shared UNION query but must yield identical histogram counts when Entries/Sleeps/settings/time/range are unchanged.
- Scheduled future-fact, hold-expiry, and midnight invalidations remain in-process and lifecycle-bound. Add no alarm, service, worker, permission, or wake lock.
- Retry continues to create a new Room collection after a terminal Flow failure. Phase 8 adds no separate retry state.

## Room, schema, backup, export, permission, and architecture implications

- Room remains version 4. No entity, column, index, migration, trigger, view, exported schema JSON, or seed SQL changes.
- The unified `EpisodeSourceDao` SQL and Kotlin projection remain unchanged.
- Deterministic JSON backup remains format version 4 and byte-schema-compatible. It exports raw records/settings, not derived onsets, gaps, buckets, eligibility, or selection.
- CSV remains unchanged and raw-record-only.
- Android automatic-backup/data-extraction rules remain unchanged.
- Histogram selection is transient `SavedStateHandle` UI state and is not part of Room, JSON, CSV, or durable preferences.
- No storage, Internet, notification, alarm, location, health, accessibility-service, or other permission is added.
- Manual DI, the process-scoped `AppContainer`, StateFlow, SavedStateHandle, Room, Kotlin, Compose, and Material 3 remain the complete architecture. No new framework or module is justified.
- The implementation is fully local and offline; it creates no account, identifier, network request, analytics event, or server-owned state.

## Accessibility, screen-reader, large-font, color, and empty-state behavior

- The section heading, denominator, persistent readout, and caveat are normal Compose text and participate in font scaling; no text is painted into Canvas or embedded in a bitmap.
- The eligible chart exposes exactly ten focusable/selectable bucket nodes in logical left-to-right order. Each node is at least 48 dp by 48 dp and has button role, selected state, visible label, and a content/state description equivalent to `1d bucket, 2 of 5 onset-to-onset gaps, at least 1 and under 2 elapsed days`.
- Decorative bar fills are removed from the semantics tree. The section itself does not duplicate every bucket description as a second focus node.
- The selected-bucket readout is visible and a polite live region. Touch, keyboard/D-pad activation, and TalkBack activation update the same text.
- The horizontal row exposes normal scroll semantics. Focus traversal can reach every bucket without precise touch. Parent vertical scrolling remains functional when a gesture begins over a bar.
- Every bar shows its numeric count and label, so height and gold color are redundant encodings. Selected state adds border/shape/emphasis, not color alone.
- Positive and zero bars, selected and unselected states, card/background, counts, and labels meet applicable Material contrast in light and dark themes. Grayscale inspection must still distinguish selection and count.
- At 150% font scale, bucket labels/counts may wrap or increase cell height; they must not be clipped, painted smaller to fit, or overlap. Horizontal scrolling is preferred to unreadably narrow cells.
- Screen magnification, keyboard/D-pad traversal, rotation, edge-to-edge insets, and process recreation remain usable. Horizontal scroll position need not survive process death; selected bucket does.
- Global no-entry state remains the existing `Nothing to draw yet` copy. The below-threshold refusal card is readable without chart semantics and contains the exact missing-sample arithmetic.
- No eligible chart emits a blank graph: at least five gaps guarantee at least one positive bucket count.

## Failure behavior

- Source/settings read failure, corrupt data, or derivation exception follows the existing Insights initial-error or stale-snapshot banner and working Retry path. Raw data remains unchanged.
- Bucket-sum mismatch, non-positive gap, out-of-order onset, arithmetic overflow, unknown bucket index, or a gap that matches no bucket is a derivation/programming error. Do not drop data or show a partial chart.
- Invalid restored bucket indices are discarded. A missing/cleared selection uses the default readout and is not an error.
- If a selected bucket’s count changes, keep the selected index while eligible and read the new count. If eligibility falls below six onsets, clear selection and show the refusal card.
- If source/settings/time changes arrive while a derivation is running, preserve the existing serialized ViewModel flow: publish only whole snapshots, then process the later emission in collection order. Never patch or flash partial bucket counts.
- Pointer or focus activation outside a bucket changes nothing. Horizontal-scroll boundaries clamp normally.

## Test strategy

### Pure unit tests

- Exactly six eligible onsets produce exactly five gaps and an eligible histogram; zero through five onsets refuse with exact missing-start and gap counts.
- Seven or more onsets produce `N - 1` gaps; all bucket counts sum to that denominator.
- Both endpoints must be inside `[rangeStart, now)`: carried-in onset is excluded from the first gap, range-start onset is included, and exact-`now` onset is excluded.
- Exact boundaries at 1, 2, 3, 4, 5, 6, 7, 10, and 14 elapsed days enter the lower-inclusive bucket; values one millisecond below each boundary stay in the previous bucket.
- Fractional-day and extreme `14+d` gaps are not rounded or dropped.
- Spring-forward same-local-time starts make a 23-hour `<1d` gap; fall-back same-local-time starts make a 25-hour `1d` gap.
- Same-timestamp Entry ties use the highest id and create at most one onset. Highest-id zero, continuing positives, explicit zero, assumed hold expiry, ongoing final episode, and sleep-paused hold all reuse Phase 5 onset semantics.
- Hold changes may merge/split eligible episode starts deterministically while leaving raw rows untouched.
- Markers never change the histogram.
- Invalid source facts continue to fail the full derivation. Determinism tests compare equality-equivalent snapshots for identical rows/hold/now/zone/range.
- Exact sparse, denominator, bucket readout, and caveat strings pass; non-caveat generated strings contain no banned inferential terms.

### ViewModel tests

- Source, hold, range, scheduled-time, lifecycle refresh, and zone seam changes publish histogram data in the same immutable snapshot as existing Insights outputs.
- A selected bucket persists through a new `InsightsViewModel`/`SavedStateHandle`, invalid restored indices are discarded, and selection clears on range change.
- Re-derivation retains a valid selection and refreshes its count; falling below six onsets clears it.
- Invalid/ineligible selection events are no-ops.
- Terminal source failure Retry and stale-snapshot behavior remain green with histogram state present.
- Existing raster/chart selections remain independent; selecting a histogram bucket does not rewrite either epoch selection.

### Compose/UI tests

- Global empty state shows no histogram shell. Populated below-threshold state shows exact refusal copy and no bar row.
- Eligible state shows title, denominator, ten buckets in frozen order, exact counts, labels, default readout, and caveat.
- Each bucket has at least a 48 dp target, coherent role/selected semantics, exact count/boundary description, and deterministic traversal order.
- Touch, keyboard/D-pad or semantic activation selects a bucket and updates the same visible polite-live-region readout.
- Horizontal scrolling reaches all ten buckets; a vertical swipe beginning over a bucket scrolls the parent Insights list.
- Zero-count buckets remain selectable and announce zero honestly.
- Light/dark themes, grayscale/non-color selection, rotation, and 150% font scaling retain counts, labels, refusal text, and caveat without clipping critical content.

### Room/instrumented and regression tests

- Existing `EpisodeSourceDaoTest` remains green byte-for-behavior: no DAO/query/schema change is expected.
- An instrumented Insights screen fixture with at least six derived onsets verifies the eligible chart, touch selection, semantics, horizontal reachability, and parent vertical scrolling.
- An instrumented sparse fixture verifies refusal arithmetic and absence of bars.
- Navigation, range selection, Settings return, Activity recreation, process-restorable primitives, and all Phase 1–7 connected tests remain green.
- Database version remains 4; migration, schema, JSON v4, CSV, erase/reset, and permission regression checks remain unchanged and green.

### Full oracles and manual API 36 verification

Run from repository root with the Android Studio bundled JDK, configured SDK, Gradle user home, and project wrapper:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Manual verification on the intended API 36 emulator covers global empty, zero-to-five-onset refusal, exactly-six eligibility, every bucket including `14+d`, all six ranges, hold-change re-derivation, touch and TalkBack/keyboard selection, horizontal chart scroll, parent vertical scroll, Activity/process recreation, light/dark, grayscale judgment, and 150% font scaling. Confirm no schema JSON, backup/CSV, manifest/permission, dependency, toolchain, WebView, or JavaScript change.

## Machine-checkable acceptance criteria

- [x] SPEC/APPROVAL: this complete spec and D-1 through D-10 are approved and frozen before the first application-code edit; Git history/diff proves the approval-gated ordering. — APPROVED 2026-08-04
- [ ] BOUNDARY: Phase 8 adds only `Days between onsets`; onset-time, sleep comparison, report, import, safety, breathing, and all inferential analysis remain absent.
- [ ] SOURCE: `InsightsSnapshot.onsetGapHistogram` is derived from the existing Phase 5 `BuiltModel.episodes` in the same pure derivation; no parallel onset classifier or UI-side derivation exists.
- [ ] RANGE: tests prove both onset endpoints are inside `[rangeStart, now)`, carried-in onsets are excluded, exact-start is included, exact-now/future is excluded, and all six existing ranges drive the result.
- [ ] SAMPLE: tests prove the fixed six-onset/five-gap threshold, exact refusal arithmetic for 0–5 onsets, and no bars below threshold.
- [ ] BUCKETS: tests prove all ten frozen lower-inclusive/upper-exclusive boundaries, fractional values, DST elapsed-time behavior, `14+d` extremes, and `sum(counts) == gapCount`.
- [ ] GRAMMAR: eligible denominator, selected readout, caveat, and sparse copy match approved deterministic strings; no positive inference, causal, predictive, correlation, significance, or periodicity claim is rendered.
- [ ] UI: exactly ten native Compose bucket cells render in fixed order with visible count/label, relative geometry, selection, denominator, readout, and caveat; no chart dependency/WebView/bitmap exists.
- [ ] ACCESSIBILITY: each bucket is a coherent at-least-48-dp selectable node with exact count/boundary semantics; live readout, keyboard/D-pad/TalkBack, horizontal reachability, vertical-scroll ownership, non-color encoding, light/dark, rotation, and 150% font pass focused tests/manual review.
- [ ] STATE: primitive bucket selection restores independently through `SavedStateHandle`, clears on range/ineligibility, and safely refreshes on source/hold/time/zone changes.
- [ ] CONCURRENCY: the existing single Room source/settings combine and serialized off-main immutable derivation remain the only data path; snapshots publish as whole internally consistent values in collection order and Retry still restarts a terminal collection.
- [ ] PERSISTENCE: Room/schema stay version 4, JSON stays format 4, CSV/backup rules stay unchanged, and no permission, durable histogram state, migration, or destructive behavior is introduced.
- [ ] REGRESSION: all Phase 1–7 JVM and connected behavior remains green, including transactional onset classification and current Insights raster/chart behavior.
- [ ] ORACLES: wrapper `test`, `lint`, `assembleDebug`, intended-device identity, `connectedDebugAndroidTest`, installed-app launch/walkthrough, and `git diff --check` pass.
- [ ] DOCUMENTATION: spec status/evidence, `PROJECT_STATE.md`, `docs/specs/BACKLOG.md`, and `docs/DECISIONS.md` are updated exactly as required below without duplicating active work.
- [ ] REVIEW: one critical-path review covers range attribution, onset identity, hold/sleep interaction, boundary arithmetic, sparse refusal, grammar, accessibility, state restoration, concurrency, and unchanged persistence; every blocking finding is resolved before publication.

## Task decomposition after approval

1. Freeze this spec and record D-008 after approval; update active state/backlog ownership — oracle: documentation diff and required-section check.
2. Add pure histogram models/derivation/copy helpers and exhaustive JVM tests — oracle: focused `EpisodeEngineTest`.
3. Add ViewModel primitive selection/restoration and focused tests — oracle: focused `InsightsViewModelTest`.
4. Add native accessible histogram/refusal UI and Compose tests — oracle: focused `InsightsScreenTest`.
5. Add/adjust instrumented navigation/recreation coverage without changing Room — oracle: focused connected Insights tests.
6. Run full JVM/lint/assemble/connected oracles and manual API 36 accessibility/visual walkthrough — oracle: commands above.
7. Run one critical-path review, resolve findings, update durable evidence, commit, push, and open the Phase 8 PR — oracle: exact diff/status/commit/remote/PR checks.

## Required durable-document updates

### On approval, before application-code edits

- Change this spec to `FROZEN — APPROVED`, record the approval date, and state that D-1 through D-10 are frozen.
- Add `D-008 — Phase 8 onset-gap histogram` to `docs/DECISIONS.md`, summarizing the six-onset threshold, both-endpoints-in-range elapsed-time gaps, frozen ten buckets, no-inference/native accessibility contract, existing one-snapshot architecture, and unchanged persistence.
- Update `PROJECT_STATE.md` to name Phase 8 as approved/active, the branch/base commit, the governing spec, the approval gate result, and the exact first implementation task.
- Keep the onset-gap work out of `BACKLOG.md`; leave onset-time counts and descriptive sleep counts as unstarted work.

### After implementation and verification

- Mark this spec `IMPLEMENTED — VERIFIED LOCALLY`, check acceptance criteria, record exact oracle/manual/review evidence, implementation/publication commits, and PR state.
- Update `PROJECT_STATE.md` with branch/head, dirty files, verification counts/results, manual checks, risks/coverage gaps, blocker, and exact next action.
- Keep `BACKLOG.md` limited to truly unstarted work; do not copy Phase 8 implementation tasks into it.
- Amend `docs/DECISIONS.md` only if an approved frozen decision changed; add a superseding entry instead of rewriting history.

## Rollout, migration, and rollback

- Rollout is source publication only after every acceptance criterion passes. No runtime feature flag or data backfill is needed.
- No Room or backup migration exists. Pre-Phase 8 and Phase 8 builds read the same version-4 database and raw exports.
- Before release, rollback is a source-control revert of histogram model/ViewModel/UI/tests/docs. SavedState may contain an unknown primitive key that older code ignores; raw user data is unaffected.
- After release, a source forward-fix or revert remains safe because Phase 8 creates no durable data and changes no permission or external contract.

## Decisions and approval gate

- **D-1 (bounded scope, recommended):** Phase 8 adds only the onset-to-onset elapsed-gap histogram and sparse refusal. Defer onset-time and sleep views.
- **D-2 (single derivation, required):** derive eligible onsets only from the existing Phase 5 episode model inside the same `deriveInsights` snapshot; add no second onset classifier.
- **D-3 (range attribution, recommended):** both onsets must fall in `[rangeStart, now)`; do not use a carried-in onset or silently widen the range.
- **D-4 (sample refusal, recommended):** freeze eligibility at six onsets/five gaps and derive missing-start copy from onset count; add no threshold setting.
- **D-5 (elapsed-time semantics, required):** calculate exact epoch-millisecond onset differences; one histogram day is 86,400,000 ms, sleep is not subtracted, and DST affects elapsed hours honestly.
- **D-6 (buckets, recommended):** freeze the ten prototype-inspired lower-inclusive/upper-exclusive buckets `<1d`, `1d` through `6d`, `7–9d`, `10–13d`, and `14+d`; never round before bucketing.
- **D-7 (grammar, required):** show counts, denominators, exact boundary readouts, and the approved caveat only; make no dominant-bucket, periodicity, causal, predictive, or inferential claim.
- **D-8 (accessibility, recommended):** use ten native, individually selectable, at-least-48-dp Compose cells with visible count/label, persistent live readout, horizontal reachability, vertical-scroll ownership, and redundant non-color encoding.
- **D-9 (state/concurrency, recommended):** persist only a validated bucket index in `SavedStateHandle`; retain the existing single Room/settings Flow, off-main immutable derivation, cancellation, invalidation, and Retry behavior.
- **D-10 (persistence/architecture, required):** keep Room/schema/JSON at version 4, CSV/backup rules/permissions unchanged, and add no framework, dependency, cache, module, server, account, analytics, or background work.

Approval gate is satisfied. D-1 through D-10 are frozen; a material change requires a documented spec amendment before implementation continues.
