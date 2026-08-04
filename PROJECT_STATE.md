# MindScale project state

Last updated: 2026-08-04

## Goal

Build MindScale as a native Android application using Kotlin, Jetpack Compose, Material 3, and Kotlin DSL. MindScale is a local-only, event-contingent symptom tracker for depression/anxiety: a measurement instrument that assembles the user's own data without interpreting it.

The product source is the Claude Design project `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`: `SPEC.md` is the rationale and `MindScale v2.dc.html` is the visual/behavioral reference for Track, Full Log, Insights, Report, Safety card, Profile, and Settings. A local exported handoff is available at `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\`; its `MindScale v2.dc.html` is the primary implementation reference. Repository specs under `docs/specs/` govern native implementation after human approval.

## Current phase: Phase 11 clinician summary and Profile foundation verified; publication in progress

- Phase 11 branch: `agent/phase11-clinician-report-profile`, created from synchronized `main` at `9d8cf4bd2dbd3834e2094c81f63e838d6348bdb5`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `9d8cf4bd2dbd3834e2094c81f63e838d6348bdb5` before branching
- Governing spec: `docs/specs/SPEC-clinician-report-profile.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-12 frozen on 2026-08-04 before application-code edits
- Reconciled boundary: local Profile/Report overlays; optional name; dated, explicitly external/user-entered PHQ-8 or GAD-7 totals; and a capped factual summary using existing Insights derivations without questionnaire administration, scoring, interpretation, comparison, diagnosis, or severity language
- Approval gate satisfied: the user granted full Phase 11 ownership and authorized decisions, implementation, verification, commits, pushes, PR readiness, merge, and final synchronization without another routine pause
- Persistence delivered: additive Room 4→5 plus JSON backup v5; records CSV compatibility; transactional snapshot/erase/reset; no import or destructive downgrade
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope
- Frozen documentation commit: `2019897655f9db5f143ef91c0707f16ba7c13cb8`; verified implementation commit: `cb469100b3b34fd3a8716428ff1448668af1cdcb`
- Verification: 180/180 JVM tests, lint with 0 errors/22 unchanged warnings, debug assembly, confirmed API 36 emulator, 111/111 connected tests, `git diff --check`, installed-app/manual export/reset matrix, and one critical review with all five blocking findings resolved
- Honest gaps: no actual share recipient was selected; provider-open/write failure was tested at the ViewModel boundary; TalkBack semantic order was tested but spoken audio was not manually audited
- Exact next action: commit this verification state, push the branch, open/ready/verify/merge the PR at the exact head, fast-forward local `main`, and record the final phase boundary

### Phase 10 merged checkpoint

- Phase 10 branch: `agent/phase10-insights-sleep-counts`, created from synchronized `main` at `652096b9cb61cdb5ff004ed352ce0f5a54d62e95`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `652096b9cb61cdb5ff004ed352ce0f5a54d62e95` before branching
- Governing spec: `docs/specs/SPEC-insights-sleep-counts.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 frozen on 2026-08-04 before application-code edits
- Reconciled boundary: direct counts of completed normalized sleep periods grouped by recorded Wake, duration-only `night` (`>3h`) versus `nap` (`<=3h`), exact duration summaries, incomplete disclosure, and accessible/restorable category selection
- The prototype's five-hour post-wake comparison is excluded because it needs a separate coverage/cohort/minimum-sample contract and would exceed this non-inferential descriptive slice
- Approval gate satisfied: the user granted full Phase 10 ownership and authorized decisions, implementation, commits, pushes, PR readiness, merge, and synchronization without another review pause
- Room/schema/JSON version 4, CSV, backup, permissions, dependencies, toolchain, navigation, DI, privacy, and local-only architecture remain frozen
- Frozen documentation commit: `e6afe3280acccb1fd0849b26b5fac5b9e4699980`
- Verified implementation commit: `945300d0bad66be21de33b9494a19d393cf40aba`
- Verification-state commit: `e7e1e117df5b75564b533140654af1b0c47f451b`
- Phase 10 PR #7 was opened as draft, marked ready, verified mergeable at the exact verification-state head, and merged into `main` as `03bad992726bd336e43b0d5f17c064a07bb2bb57` on 2026-08-04
- Local `main` and `origin/main` fast-forwarded to the merge commit before this requested phase-boundary documentation commit
- Local verification: 168/168 JVM tests, lint with 0 errors/22 existing warnings, debug assembly, focused 27/27 connected tests, full 100/100 connected tests, installed-app manual inspection, critical review, and `git diff --check` passed
- Exact next action after the phase boundary: leave the remaining Report/Profile, import/restore, Safety, and breathing backlog unstarted until a separately scoped task is assigned

### Phase 9 publication checkpoint

- Phase 9 branch: `agent/phase9-onset-time-counts`, created from synchronized `main` at `76880a978e68e79348941de891a0bc6251986188`
- Before branch creation, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `76880a978e68e79348941de891a0bc6251986188`; the GitHub connector confirmed `Kiezling/Mindscale` is accessible with push/admin permission and default branch `main`
- Phase 9 spec: `docs/specs/SPEC-insights-onset-time-counts.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 were frozen on 2026-08-04 before application-code edits
- Reconciled boundary: one six-onset-minimum, 24 local-clock-hour count view derived from the existing Phase 5 episode model, with a deterministic four-hour count sentence, exact denominators, recording-displacement/current-zone caveats, and accessible primitive selection
- Selection rationale: onset-time counts are the next product-ordered Insights view and need no new source/cohort rules; descriptive sleep counts remain deferred because their night/nap, boundary, incomplete-sleep, and post-wake semantics require a separate spec
- Approval gate satisfied: the user granted full ownership and authorized decisions, implementation, commits, pushes, PR readiness, merge, and synchronization without another review pause
- Frozen documentation commit: `1846598`; verified implementation commit: `338a4fe`
- Verification-state commit: `ecb98d6c30d8a66efde80bfde97837400ef2f33f`
- Phase 9 PR #6 was opened as draft, marked ready, and merged into `main` as `6e000bd3126b79aa930357c14b7987917e7b9268` on 2026-08-04 (`2026-08-04T20:10:15Z`)
- Local `main` and `origin/main` were synchronized at the merge commit before this requested phase-boundary documentation commit
- Exact next action after the phase boundary: freeze a separate bounded spec before any descriptive sleep-count implementation
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope

### Phase 8 publication checkpoint

- Phase 8 branch: `agent/phase8-onset-gap-histogram`, created from synchronized `main` at `86bd313e47515bd81eded5e03796195a3b389bff`
- Phase 8 PR #5 merged into `main` as `042376265cb801a4d8e80375b3eb7fff54c1d0aa` on 2026-08-04 (`2026-08-04T18:48:52Z`)
- Local `main`, local `origin/main`, and live GitHub `refs/heads/main` are synchronized at `042376265cb801a4d8e80375b3eb7fff54c1d0aa`
- Phase 8 spec: `docs/specs/SPEC-insights-onset-gap-histogram.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 were frozen on 2026-08-04 before application-code edits
- Phase 8 reconciled boundary: one six-onset-minimum, ten-bucket, elapsed onset-to-onset gap histogram derived from the existing Phase 5 episode model; onset-time and sleep views remain deferred
- Approval gate satisfied: the user reiterated full project ownership and authorized all decisions, implementation, commits, pushes, and PRs without further approval
- Phase 8 implementation commit: `d2d546686d6ced87e98eea5c2e73ed66ee41138e`; complete local JVM, lint, build, connected, and installed-app verification passed
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope

### Prior phase publication

- Branch: `agent/phase7-track-dialog-restoration`, created from synchronized `main` at `372d1d3d91f7308ad35b8824c996bce641b0ce57`
- Local `main`, its tracking ref, and live `origin/main` were verified at `372d1d3d91f7308ad35b8824c996bce641b0ce57` before the branch was created
- Phase 7 spec: `docs/specs/SPEC-track-dialog-restoration.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 frozen on 2026-08-04
- Phase 7 implementation commit: `8d8d1a401d1d4a193985f8377c3ff9db09a3ec3c`
- Phase 7 PR #4: merged into `main` as `afeec04d3ca48a9574446ccda24484ed8bf5ed56` on 2026-08-04 (`2026-08-04T17:29:20Z`)
- Phase 4 final branch head: `93fac65711e8e3eeb72cf6f1406d386f0386e9da`
- Phase 4 PR #1: merged into `main` as `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5` on 2026-08-03 local time (`2026-08-04T03:37:50Z`)
- Phase 5 spec: `docs/specs/SPEC-insights-foundation.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 frozen
- Phase 5 implementation commit: `cd3a1cf51adb468f968a78a2a04d85ce317d76ed`, pushed to `origin/agent/phase5-insights-foundation` on 2026-08-04
- Phase 5/6 integration PR #3: merged into `main` as `fee0caf6dd1da2a3ecb9e04da1bab454ca009836` on 2026-08-04 (`2026-08-04T14:10:15Z`)
- Phase 6 branch: `agent/phase6-entry-chart`, merged into `agent/phase5-insights-foundation` by PR #2 as `3d92a2fd5805b102c2a6b0ef01f5d47931361c56`
- Phase 6 spec: `docs/specs/SPEC-insights-entry-chart.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-8 frozen
- Phase 6 implementation commit: `53f9d54ea79f56b2d069297f4d6a4b8923aeae23`, pushed to `origin/agent/phase6-entry-chart` on 2026-08-04
- Phase 1 spec: `docs/specs/SPEC-track-numpad-logging.md` — `IMPLEMENTED`
- Phase 2 spec: `docs/specs/SPEC-track-phase2-completeness.md` — `IMPLEMENTED`
- Phase 3 spec: `docs/specs/SPEC-full-log.md` — `IMPLEMENTED` on 2026-08-03
- Phase 4 spec: `docs/specs/SPEC-settings-data-control.md` — `IMPLEMENTED` on 2026-08-03/04; D-1 through D-9 remain frozen
- Phase 7 publication scope: the implementation, focused unit/instrumented/UI tests, and governing documentation on `agent/phase7-track-dialog-restoration`. The pre-existing untracked `.agents/` and `.codex/` tooling directories remain excluded.

## Environment

- Project: `S:\Android\AndroidProjects\MindScale`
- SDK: `S:\Android\Sdk`
- AVD: `S:\Android\Avd\MindScale_API_36.avd`
- Gradle user home: `S:\Android\Gradle`
- JDK: Android Studio Quail 2 bundled JDK 21.0.10
- Android Studio: Quail 2 (2026.1.2)
- Application ID: `com.kieslingdev.mindscale`
- Minimum SDK 26; target SDK 36; compile SDK 36.1

## Completed work

### Environment and baseline

- Installed and verified stable Android Studio, API 36 SDK/toolchain, Git, native Claude Code, and the official Claude JetBrains plugin.
- Created and booted the S:-based Pixel 9 API 36 Google Play AVD; verified hardware acceleration, adb, installation, and foreground launch.
- Generated the Empty Activity Compose starter and committed the verified baseline as `673d6c7`.
- Removed superseded C:-based Android Studio/SDK/AVD/caches only after replacement verification and approval.
- Added provider-neutral `AGENTS.md`, provider adapters, durable project state/decision/failure records, Android specs, and tested Claude hooks.

### Phase 1 — Track numpad logging

- Implemented Room `Entry` storage, 0–10 tap logging, long-press backdating, band/intensity helpers, recent list, and edit/note/delete in native Compose.
- Used unidirectional `StateFlow`, manual DI, stable Compose identity, and no repository/DI framework.
- Critical review found and fixed stale recent-row overwrites and incomplete note-buffer restoration claims.
- Committed as `7258999` (`Implement Phase 1 track numpad logging`).

### Phase 2 — Track completeness

- Added additive Room migration v1→v2: `entries.kind`, sleep intervals, markers, and canonical track settings.
- Added transactional Sleep/Wake capture with real concurrency coverage, timestamp-relative onset-chip logic, marker capture, help card, paused/check-in banners, badges, chips, and settings persistence.
- Critical review fixed a stale onset-chip full-row overwrite and marker restoration. Independent Codex review then completed true marker process restoration with `SavedStateHandle` and a focused recreation test.
- Re-ran all Android oracles independently on the API 36 emulator and reviewed the exact 37-file scope.
- Committed and pushed as `4cf9f068d42a0c906b76ddac460d4b5e907410d3` (`Implement Phase 2 track completeness`).

### Phase 3 — Full Log

- Added a native Track/Log bottom navigation shell without adding a navigation framework; Track remains the launch destination and Back from Log returns to Track.
- Added a process-scoped `AppContainer`/`MindScaleApplication` so Track and Log use one Room database without adding a DI framework or repository layer.
- Added a reactive, mixed Full Log of all ratings, sleep intervals, and markers, grouped by local day and ordered deterministically newest-first.
- Added inclusive local-date From/To filtering with DST-safe half-open epoch bounds, All reset, calm empty/error states, and a working Retry path.
- Added inline targeted rating edit/note behavior plus isolated, confirmed deletion for all three record types. Mutation failures retain retryable UI; stale row ids close safely with a concise message.
- Refactored Track's edit/note/delete path onto the same targeted DAO mutations, eliminating stale full-row overwrites without changing Track behavior.
- Critical review corrected a terminal read-error collector, action semantics swallowed by a merged row, filtered-empty wording, and mutation UI closing before Room confirmed success.
- Kept Room at schema version 2; no migration, new toolchain dependency, paging library, navigation library, or DI framework was introduced.
- Committed and pushed as `87eb817713a459fd231ce77169dfa522b249f641` (`Implement Phase 3 full log`).

### Phase 4 — Settings, anchors, and data control

- Added additive Room migration 2→3, exported schema 3, appearance/time enums, anchors, custom onset vocabulary, note-preview privacy, and one-shot anchor-prompt state.
- Replaced production full-row settings writes with targeted mutations and added a transactional guard that refuses to hide Sleep/Wake while an interval is open.
- Added a reachable Settings overlay that returns to Track or Log, deep links from the paused/anchor cards, and state restoration for destination, focus, and drafts.
- Applied the gold/ink Light/Dark/System theme globally and removed dynamic wallpaper colors; 12/24-hour mode now reaches Track feedback/history and Full Log.
- Added deterministic version-3 JSON backup and RFC-4180 CSV export through `CreateDocument`, with no storage permission, plus export-first atomic erase/reset.
- Critical review fixed an untappable status-bar-overlapped Settings action, retained encoded exports for write retry, and made word de-duplication locale-independent.

### Phase 5 — Insights foundation

- Added a third native Insights destination with six local-calendar ranges, restoration, Back-to-Track behavior, and Settings return-to-Insights behavior without a navigation framework.
- Added a pure deterministic episode engine over Entry/Sleep facts: same-timestamp collapse, explicit/assumed/ongoing endings, awake-time holds, sleep union, clipped range duration/AUC, clear-day eligibility, local/DST-safe raster projection, and scheduled time invalidation.
- Replaced production ordinary-capture onset detection with a hold/sleep-aware Room transaction that snapshots settings, classifies at the capture timestamp, inserts the rating atomically, and suppresses prompting safely if classification/settings are unavailable.
- Added additive Room 3→4 hold persistence with 8/12/16/24 waking-hour choices, schema 4, targeted writes, fresh/default erase reset, and deterministic JSON backup version 4 `holdHours`; CSV remains unchanged.
- Added native Material 3 summary/fact/episode UI and a Canvas day/hour raster with text legend, future hatching, touch/horizontal-drag exploration, visible live readout, TalkBack custom actions, stable date keys, and explicit 48 dp range/choice targets.
- Critical review and manual API 36 inspection fixed carried-duration leakage, future sleep-end invalidation, raster vertical-scroll interception, low-contrast future state, duplicate ongoing copy, and undersized range targets.

### Phase 6 — Insights entry chart

- Added a native step-only `What you recorded` chart driven by the Phase 5 effective-state engine, with 0/5/10 axes, shared ranges, sleep gaps/bands, event lines, and no interpolation or chart dependency.
- Expanded the single reactive Room UNION projection with Entry note and Marker text while keeping Room/schema/backup at version 4 and preserving transactional onset classification.
- Added independent chart selection restoration, touch and horizontal-drag exploration, event snapping, visible/live source-rating readout, note-preview privacy, and six TalkBack navigation actions.
- Added pure engine, ViewModel, Room, Compose, gesture-ownership, semantics, and privacy coverage; final manual inspection covered touch readout, event discovery, light/dark rendering, and 150% font scaling.

### Phase 8 — Insights onset-gap histogram

- Added one native `Days between onsets` section derived from the existing Phase 5 episode snapshot, with a fixed six-onset threshold and exact 0–5 sparse refusal copy.
- Added ten fixed lower-inclusive/upper-exclusive elapsed-day buckets from `<1d` through `14+d`, exact denominators and selected-boundary readouts, and strict cycle/cause/prediction disclaimers.
- Added horizontally reachable, individually selectable native Compose cells with visible count/label geometry, redundant border selection, 48 dp-plus semantics, a polite live readout, and parent vertical-scroll ownership.
- Added primitive validated `SavedStateHandle` selection, range/ineligibility clearing, and retention across eligible re-derivation without changing the existing single Room/settings Flow or immutable snapshot publication.
- Kept Room/schema/JSON at version 4 and left CSV, backup, permissions, dependencies, toolchain, manual DI, navigation shell, and local-only architecture unchanged.
- Expanded pure engine, ViewModel, and Compose coverage for every range, 0–5 refusal arithmetic, all ten exact boundaries, fractional/DST/extreme gaps, selection restoration/refresh, semantics, horizontal reachability, and vertical gesture ownership.

### Phase 9 — Insights onset-time counts

- Added one native `Time of day it started` section derived from the existing Phase 5 episode onsets, with a fixed six-start threshold, exact 0–5 refusal copy, and a covered-local-calendar-day denominator.
- Added 24 current-zone local-clock buckets, deterministic DST overlap/gap handling, and a wrapping four-hour maximum-count sentence with earliest numeric-hour tie-breaking and exact half-open boundaries.
- Added horizontally reachable, individually selectable native Compose cells with visible count/hour geometry, redundant border selection, at-least-48-dp targets, exact spoken semantics, a polite live readout, and parent vertical-scroll ownership.
- Added validated primitive `SavedStateHandle` hour restoration, range/ineligibility clearing, and eligible refresh retention without changing the existing single Room/settings Flow or immutable snapshot publication.
- Kept Room/schema/JSON at version 4 and left CSV, backup, permissions, dependencies, toolchain, navigation, DI, and local-only architecture unchanged; descriptive sleep counts remain deferred.
- Added pure engine, copy, ViewModel, Room regression, and Compose coverage for all six ranges, exact hour/midnight/range boundaries, current-zone reprojection, DST overlap/gap, covered-day arithmetic, window wrap/ties, hold/sleep/marker invariance, selection lifecycle, accessibility, hour-23 reachability, and vertical gesture ownership.

## Active blocker

No active blocker. Phase 10 is merged and complete.

## Known decisions

- Preserve the template-generated Gradle/AGP/Kotlin/Compose compatibility set; do not independently change toolchain versions.
- Use the Gradle wrapper and Android Studio bundled JDK; never install or use system Gradle.
- Keep SDK, AVDs, projects, and Gradle caches on S:; keep Android Studio and normal configuration on C:.
- Keep production signing and publishing out of scope until explicitly requested.
- Use `AGENTS.md` as the provider-neutral contract and `CLAUDE.md` only as the Claude adapter.
- Reserve scarce models for specs, irreversible architecture decisions, adjudication, and critical review; use cheaper tiers for exploration and routine implementation.
- Preserve local-only storage and never add interpretation, accounts, servers, ads, social features, streaks, or gamification.

## Next tasks

1. Keep clinician Report/Profile, import/restore, Safety, and breathing work unstarted in `docs/specs/BACKLOG.md` until separately assigned and specified.
2. Continue excluding `.agents/` and `.codex/` from product/documentation commits.

## Last verification

Phase 10 publication checkpoint completed 2026-08-04:

- Pushed `agent/phase10-insights-sleep-counts` through verification-state commit `e7e1e117df5b75564b533140654af1b0c47f451b`.
- GitHub PR #7 was opened as draft, marked ready, and confirmed mergeable at that exact head.
- PR #7 merged into `main` as `03bad992726bd336e43b0d5f17c064a07bb2bb57`.
- Local `main` fast-forwarded from `652096b9cb61cdb5ff004ed352ce0f5a54d62e95` to `03bad992726bd336e43b0d5f17c064a07bb2bb57`; local `origin/main` matched before this phase-boundary documentation commit.
- No Android oracle was rerun for the GitHub merge or documentation-only checkpoint because the application/test tree is the exact verified PR head plus GitHub's merge commit.
- Expected untracked `.agents/` and `.codex/` remain excluded; no other local change was present before this documentation update.

Phase 10 final local verification completed 2026-08-04 for implementation commit `945300d0bad66be21de33b9494a19d393cf40aba` on `agent/phase10-insights-sleep-counts`:

- `test`: 168/168 JVM tests passed; 0 failures, errors, or skips across 12 suites.
- `lint`: passed with 0 errors and the same 22 existing warnings.
- `assembleDebug`: passed.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- Focused Room/Insights connected run passed 27/27 after final copy and zero-category coverage.
- `connectedDebugAndroidTest`: 100/100 passed with 0 failures or skips.
- Installed-app inspection verified no-completed/open and mixed eligible states, exact-three-hour nap/four-hour night, selection/readout, incomplete disclosure, vertical reachability, light/dark, 12/24-hour behavior, 150% font, rotation/configuration restoration, and native content descriptions/targets. Font, theme, rotation, and time format were restored; the temporary seed source/test package were removed.
- Critical review corrected future-Wake disclosure grammar and found no remaining blocking cohort/range, DST/zone, incomplete-data, arithmetic, accessibility, restoration, concurrency, privacy, persistence, or rollback issue.
- Practical manual DST/zone reprojection and listened-to TalkBack speech remain coverage gaps; exhaustive pure tests plus Compose semantics/activation/live-region tests and device UI hierarchy cover those contracts.
- `git diff --check` passed with only configured LF-to-CRLF notices. No schema, backup/CSV, manifest/permission, dependency/toolchain, navigation/DI, WebView, server/account/analytics, or background-work file changed.

Phase 10 starting-state verification completed 2026-08-04 before application-code edits:

- `git status --short --branch` showed synchronized `main` with only expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `652096b9cb61cdb5ff004ed352ce0f5a54d62e95`.
- Branch `agent/phase10-insights-sleep-counts` was created from that exact head.
- Product handoff `SPEC.md` and `MindScale v2.dc.html`, all existing Insights specs, backlog, decisions, state, and relevant failed paths were reconciled before freezing D-010.
- No Gradle oracle was rerun for the documentation-only freeze; the merged Phase 9 application tree and its verification evidence below remain unchanged.

Phase 9 publication checkpoint completed 2026-08-04:

- Pushed `agent/phase9-onset-time-counts` through verification-state commit `ecb98d6c30d8a66efde80bfde97837400ef2f33f`.
- GitHub PR #6 was opened as draft, marked ready, verified `CLEAN`/`MERGEABLE` at that exact head, and merged with the guarded expected-head SHA.
- PR #6 merged into `main` as `6e000bd3126b79aa930357c14b7987917e7b9268` at `2026-08-04T20:10:15Z`.
- Local `main` fast-forwarded from `76880a9` to `6e000bd`; no Android oracle was rerun for the merge or this documentation-only checkpoint because the application tree is the exact verified PR head plus GitHub's merge commit.
- Expected untracked `.agents/` and `.codex/` remain excluded; no other local change was present.

Phase 9 final local verification completed 2026-08-04 for implementation commit `338a4fe` on `agent/phase9-onset-time-counts`:

- `test`: 158/158 JVM tests passed; 0 failures, 0 errors, 0 skipped across 12 suites.
- `lint`: passed with 0 errors and the existing 22 warnings.
- `assembleDebug`: passed.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- `connectedDebugAndroidTest`: 95/95 passed; 0 failures, errors, or skips. The focused Insights screen/DAO run passed 22/22 first.
- Installed-app inspection covered the exact one-start refusal, the exactly-six-start eligible state, denominator, four-hour sentence, selection/live readout, all 24 semantics, hour-23 reachability and selection, parent vertical scrolling, light/dark rendering, 150% font scaling, and landscape scrolling. Device font, theme, and rotation were restored to `1.0`, `no`, and automatic portrait defaults.
- Critical review covered source/range attribution, zone/DST semantics, hold/sleep reuse, four-hour wrap/tie arithmetic, grammar, accessibility, restoration, single-snapshot concurrency, privacy, unchanged version-4 persistence, and rollback. No blocking finding remained.
- `git diff --check` passed. No Room/schema, JSON/CSV, backup, manifest/permission, dependency/toolchain, WebView/JavaScript, sample-data, account/server/analytics, or background-work file changed.
- No reusable failed implementation approach was introduced; `FAILED_PATHS.md` remains unchanged. Expected untracked `.agents/` and `.codex/` remain excluded.

Phase 9 starting-state verification completed 2026-08-04 before application-code edits:

- `git status --short --branch` showed synchronized `main` with only expected untracked `.agents/` and `.codex/`.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `76880a978e68e79348941de891a0bc6251986188`.
- GitHub repository metadata confirmed public repository `Kiezling/Mindscale`, default branch `main`, and connector push/admin access.
- Branch `agent/phase9-onset-time-counts` was created from that exact head.
- Product rationale, the complete primary HTML handoff, the three governing Insights specs, current engine/ViewModel/Compose/DAO code, and focused JVM/instrumented tests were reconciled before drafting.
- No Gradle oracle was rerun for this documentation-only approval state; the merged Phase 8 application evidence below remains current.

Repository synchronization verified 2026-08-04 after Phase 8 publication:

- GitHub PR #5 merged `agent/phase8-onset-gap-histogram` into `main` as `042376265cb801a4d8e80375b3eb7fff54c1d0aa` at `2026-08-04T18:48:52Z`.
- Local `main`, `origin/main`, and live `refs/heads/main` all resolved to `042376265cb801a4d8e80375b3eb7fff54c1d0aa` before this documentation-only merge checkpoint.
- Phase 8 implementation commit `d2d546686d6ced87e98eea5c2e73ed66ee41138e` and verification-state commit `f63ae4d2ab1fbc4d8f8d8c9c79da86019d48de32` are ancestors of the merge.
- No Android oracle was rerun for the merge or this documentation-only checkpoint; the complete Phase 8 verification evidence below applies to the merged application tree.

Phase 8 final local verification completed 2026-08-04 for implementation commit `d2d546686d6ced87e98eea5c2e73ed66ee41138e` on `agent/phase8-onset-gap-histogram`:

- `test`: 147/147 JVM tests passed; 0 failures, 0 errors, 0 skipped across 12 suites.
- `lint`: passed with 0 errors and the existing 22 warnings.
- `assembleDebug`: passed.
- `adb devices -l`: `emulator-5554`, `MindScale_API_36` (API 36).
- `connectedDebugAndroidTest`: 91/91 passed; 0 failures or skips. The focused 14-test Insights screen class also passed after adding horizontal reachability coverage.
- Installed-app API 36 inspection used eight local episode starts and verified the eligible histogram, exact denominator and selected readout, caveat, parent vertical scrolling, horizontal reachability through `14+d`, light/dark rendering, and 150% font scaling. Emulator font scale and night mode were restored to `1.0` and `no`.
- Critical review covered range attribution, episode-model reuse, exact bucket arithmetic, DST/extremes, refusal grammar, accessibility, selection lifecycle, single-snapshot concurrency, and unchanged persistence/architecture. It added explicit 0–5 refusal, all-six-range, hold-limit, and horizontal reachability assertions; the full oracles then passed.
- Two recurring connected-suite harness races were resolved without production changes: close the restored edit-dialog IME before Back, and join destroyed Track ViewModel scopes before closing the in-memory Room database. `FAILED_PATHS.md` records the signals and remedies.
- `git diff --check` passed. No Room/schema, JSON/CSV, backup, manifest/permission, dependency/toolchain, WebView/JavaScript, account/server/analytics, or background-work file changed.

Phase 8 starting-state verification completed 2026-08-04 before specification edits:

- `git status --short --branch` showed synchronized `main` with only the expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `86bd313e47515bd81eded5e03796195a3b389bff`.
- A sandboxed `git fetch origin --prune` could not update `.git/FETCH_HEAD` because the sandbox exposes `.git` read-only, but the independent live `git ls-remote` check succeeded and matched every local ref.
- Branch `agent/phase8-onset-gap-histogram` was created from that exact head.
- No Gradle oracle was rerun because this approval-gated step changes documentation only; the merged Phase 7 application tree and its verification evidence below remain unchanged.

Repository synchronization verified 2026-08-04 after Phase 7 publication:

- GitHub PR #4 merged `agent/phase7-track-dialog-restoration` into `main` as `afeec04d3ca48a9574446ccda24484ed8bf5ed56`.
- Local `main`, `origin/main`, and live `refs/heads/main` were synchronized to `afeec04d3ca48a9574446ccda24484ed8bf5ed56` before this documentation-only merge checkpoint.
- Phase 7 implementation commit `8d8d1a401d1d4a193985f8377c3ff9db09a3ec3c` and publication-state commit `27a47b88f282d2d77944e9a3c3225b1420e85392` are ancestors of the merge.
- No Android oracle was rerun for the merge and documentation-only synchronization; the complete Phase 7 verification evidence below remains current for the merged application tree.

Phase 7 final local verification completed 2026-08-04 from `S:\Android\AndroidProjects\MindScale` on `agent/phase7-track-dialog-restoration`, based on synchronized `main` commit `372d1d3d91f7308ad35b8824c996bce641b0ce57`:

- `test`: 136/136 JVM tests passed; focused Track ViewModel suite: 61/61.
- `lint`: passed with 0 errors and 22 existing warnings.
- `assembleDebug`: passed.
- `adb devices -l`: `emulator-5554`, `MindScale_API_36` (API 36).
- `connectedDebugAndroidTest`: 88/88 passed with 0 failures/errors/skips. Coverage includes Backdate/Edit/Note Activity recreation, primitive Bundle restoration into a new owner/ViewModelStore, Room id observation outside recent ten, stale/conflict/failure paths, large-font/live-region UI, and all Phase 1–6 regressions.
- Manual installed-app process walkthrough proved all three exact drafts survive true background `am kill` reconstruction through Recents with a changed PID: Backdate `2026-0`/`1`, Edit `2026-0`/`2`, and multiline Note `Phase7 manual\nrestored`.
- Visual inspection on the API 36 emulator confirmed dialog labels, validation, focus, scrolling, and reachable actions with the IME open. Font scale remained `1.0`, night mode `no`, and automatic rotation enabled after the walkthrough.
- `git diff --check` passed with only Windows line-ending notices. No schema, backup/CSV, manifest/permission, dependency, or toolchain file changed.
- Two connected-test-only issues were corrected during verification: asynchronous row/dialog sequencing in the compound navigation test and teardown ordering in the saved-state Room harness. Neither involved a production behavior change or merits a `FAILED_PATHS.md` entry.

Phase 7 starting-state verification completed 2026-08-04 before documentation edits:

- `git status --short --branch` showed synchronized `main` with only the expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `372d1d3d91f7308ad35b8824c996bce641b0ce57`.
- Merge commit `fee0caf6dd1da2a3ecb9e04da1bab454ca009836` is an ancestor of `372d1d3d91f7308ad35b8824c996bce641b0ce57`.
- Branch `agent/phase7-track-dialog-restoration` was created from that exact head. No Gradle oracle was rerun because this approval-gated step changes documentation only.
- Documentation checks passed: `git diff --check`, a trailing-whitespace scan of the untracked draft spec, and a required-section check. Application/test-source diff remained empty.

Phase 6 final local verification completed 2026-08-04 from `S:\Android\AndroidProjects\MindScale` for implementation commit `53f9d54ea79f56b2d069297f4d6a4b8923aeae23`, based on Phase 5 publication head `c8a7bdb17a12aba58dba59927ffc81506d5b42dc`:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
adb devices -l
.\gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain
git diff --check
```

Results:

- JVM tests: 122/122 passed; 0 failures, 0 errors, 0 skipped.
- Lint: passed with 0 errors and the same 22 existing template/dependency/toolchain warnings; no Phase 6 warning remains.
- Debug assembly: passed; no dependency, toolchain, manifest, permission, or schema JSON change was made.
- Intended device: `emulator-5554`, `MindScale_API_36` (API 36).
- Instrumented/UI tests: 80/80 passed; 0 failures, 0 errors, 0 skipped. Coverage includes the unified Entry/Sleep/Marker source, transactional onset with markers present, chart touch/horizontal drag, vertical scroll ownership, note privacy, event text, all six semantic actions' ViewModel targets, and all Phase 1–5 regressions.
- Manual installed-app review used local UI-created records and verified the 30-day compressed step geometry, persistent source/event readout, event-line snapping, scroll into episode facts, light mode, dark mode, and 150% font scaling. Emulator `font_scale` and night mode were restored to `1.0` and `no`.
- Final review found no blocking product, persistence, concurrency, accessibility, grammar, or architecture issue. A stale off-screen heading assertion and an over-broad drag-test start were corrected; the focused 11-test Insights class and final 80-test suite then passed.
- `git diff --check` passed with only Windows line-ending notices.

Phase 5 final local verification completed 2026-08-04 from `S:\Android\AndroidProjects\MindScale` at uncommitted branch head/base `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5`:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Results:

- JVM tests: 116/116 passed; 0 failures.
- Lint and debug assembly: passed; no toolchain/dependency change was made.
- Intended device: `emulator-5554`, `MindScale_API_36` (API 36).
- Instrumented/UI tests: 77/77 passed; 0 failures, 0 errors, 0 skipped. Coverage includes Room 1→4/2→4/3→4, settings/default erase, unified source/transactional onset, all Phase 1–4 regressions, navigation/recreation, Insights states/ranges/semantics, 48 dp range targets, vertical raster scroll, and 12/24-hour readout.
- Manual installed-app walkthrough verified Track/Log/Insights navigation, empty and populated Insights, touch readout, vertical scrolling past the raster, fact/episode cards, hold setting, Settings return, light/dark output, 150% font scaling, and TalkBack-enabled focus/navigation. Connected tests invoke every custom raster accessibility action; no WebView, JavaScript/chart dependency, new permission, account, server, analytics, or background worker was added.
- `git diff --check` passed with only Windows line-ending notices. The emulator-only test records/screenshots are disposable build evidence and are not repository source artifacts.
- One interrupted Gradle run left a truncated generated test result and caused a raw `EOFException`; `cleanTestDebugUnitTest` recovered it. The durable workaround is in `FAILED_PATHS.md`.
- Critical review completed and all findings were resolved; see the checked Phase 5 acceptance criteria.

Repository/PR synchronization verified 2026-08-04 after Phases 5-6 publication:

- GitHub PR #2 merged Phase 6 head `7bc414f4aa04a717247e612f8170d610b22bea47` into `agent/phase5-insights-foundation` as `3d92a2fd5805b102c2a6b0ef01f5d47931361c56`.
- GitHub PR #3 merged the combined Phase 5/6 branch into `main` as `fee0caf6dd1da2a3ecb9e04da1bab454ca009836` on 2026-08-04 (`2026-08-04T14:10:15Z`).
- `git switch main` and `git pull --ff-only origin main` succeeded; local `main`, `origin/main`, and `origin/HEAD` are synchronized at `fee0caf6dd1da2a3ecb9e04da1bab454ca009836`.
- Phase 6 publication head `7bc414f4aa04a717247e612f8170d610b22bea47` is an ancestor of updated `main`.
- No Gradle oracle was rerun for this synchronization and documentation-only handoff update; the Phase 6 verification evidence above remains current for the merged application tree.

Repository/PR synchronization verified 2026-08-03 before Phase 5 drafting:

- GitHub PR #1 is closed and merged; head `93fac65711e8e3eeb72cf6f1406d386f0386e9da`, merge commit `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5`.
- `git fetch --prune origin` and `git pull --ff-only origin main` succeeded.
- Local `main` and `origin/main` were identical (`0 0`) at `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5`, and Phase 4 head was confirmed as an ancestor.
- Phase 5 branch `agent/phase5-insights-foundation` was created from that merge commit before documentation edits.
- No Gradle oracle was rerun for the documentation-only spec draft; the latest application evidence remains the Phase 4 suite below.

Phase 4 final pre-publication verification completed 2026-08-03/04 from `S:\Android\AndroidProjects\MindScale`:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Results:

- Unit tests: 89/89 passed; 0 failures, 0 errors, 0 skipped.
- Lint: passed with 0 errors and 22 existing template/dependency/toolchain warnings; the two Phase 4 modifier-order warnings found during review were fixed.
- Debug assembly: passed.
- Intended device: `emulator-5554`, `MindScale_API_36`.
- Instrumented tests: 59/59 passed with 0 failures and 0 skipped, including Room 1→3/2→3 migration, settings mutations/open-sleep guard, snapshot/erase, Phase 1–3 regressions, Settings navigation, and recreation.
- Diff check: passed; only harmless line-ending notices were emitted.
- Manual installed-app walkthrough confirmed physical Settings touch after status-bar inset correction, all upper/lower sections, real JSON and CSV writes through DocumentsUI, neutral picker cancellation, and the export-first erase confirmation. The destructive confirmation was canceled.
- Emulator Downloads contains three harmless empty-state verification artifacts: two version-3 JSON backups and one CSV records file.
- Phase 4 implementation is `ea30fdeaa0dfe3987fd5cb71201756ccbe72092d` on `agent/phase4-settings-data-control`; draft PR #1 targets Phase 3 checkpoint `87eb817713a459fd231ce77169dfa522b249f641` on `main`.

## Known coverage gaps and backlog

- Phase 1 backdate/edit/note dialog-open restoration is implemented and verified by Phase 7; see `docs/specs/SPEC-track-dialog-restoration.md`.
- JSON/CSV import remains deferred; Phase 4 exports are one-way until a separately approved validation/merge/rollback spec exists.
- The time-weighted/hold episode model and Phase 2 onset reconciliation are implemented and verified by `docs/specs/SPEC-insights-foundation.md`.
- Gold/ink theming is implemented; approved typography assets remain a future design decision.
- Full Log import/export and clinician-report data actions remain deferred; no inert controls are shown.

## Handoff checklist

Before a pause, compaction, or provider switch, update:

- branch and current commit;
- dirty files and what each contains;
- governing spec/task and exact next action;
- last oracle commands/results and coverage gaps;
- blocker or approval gate;
- new decisions and failed paths.
