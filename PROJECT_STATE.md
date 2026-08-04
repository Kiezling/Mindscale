# MindScale project state

Last updated: 2026-08-04

## Goal

Build MindScale as a native Android application using Kotlin, Jetpack Compose, Material 3, and Kotlin DSL. MindScale is a local-only, event-contingent symptom tracker for depression/anxiety: a measurement instrument that assembles the user's own data without interpreting it.

The product source is the Claude Design project `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`: `SPEC.md` is the rationale and `MindScale v2.dc.html` is the visual/behavioral reference for Track, Full Log, Insights, Report, Safety card, Profile, and Settings. A local exported handoff is available at `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\`; its `MindScale v2.dc.html` is the primary implementation reference. Repository specs under `docs/specs/` govern native implementation after human approval.

## Current phase: Phase 5 Insights foundation implemented and verified locally

- Branch: `agent/phase5-insights-foundation`, created from updated `main` at `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5`
- Phase 4 final branch head: `93fac65711e8e3eeb72cf6f1406d386f0386e9da`
- Phase 4 PR #1: merged into `main` as `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5` on 2026-08-03 local time (`2026-08-04T03:37:50Z`)
- Phase 5 spec: `docs/specs/SPEC-insights-foundation.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 frozen
- Phase 1 spec: `docs/specs/SPEC-track-numpad-logging.md` — `IMPLEMENTED`
- Phase 2 spec: `docs/specs/SPEC-track-phase2-completeness.md` — `IMPLEMENTED`
- Phase 3 spec: `docs/specs/SPEC-full-log.md` — `IMPLEMENTED` on 2026-08-03
- Phase 4 spec: `docs/specs/SPEC-settings-data-control.md` — `IMPLEMENTED` on 2026-08-03/04; D-1 through D-9 remain frozen
- Current worktree: verified Phase 5 application, schema, test, spec, decision, failure-path, backlog, and state changes on `agent/phase5-insights-foundation`; commit and push were authorized on 2026-08-04 and are pending publication
- Pre-existing untracked `.agents/` and `.codex/` tooling directories remain outside the Phase 5 change

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

## Active blocker

No active blocker. Phase 5 is locally complete and authorized for commit and push.

## Known decisions

- Preserve the template-generated Gradle/AGP/Kotlin/Compose compatibility set; do not independently change toolchain versions.
- Use the Gradle wrapper and Android Studio bundled JDK; never install or use system Gradle.
- Keep SDK, AVDs, projects, and Gradle caches on S:; keep Android Studio and normal configuration on C:.
- Keep production signing and publishing out of scope until explicitly requested.
- Use `AGENTS.md` as the provider-neutral contract and `CLAUDE.md` only as the Claude adapter.
- Reserve scarce models for specs, irreversible architecture decisions, adjudication, and critical review; use cheaper tiers for exploration and routine implementation.
- Preserve local-only storage and never add interpretation, accounts, servers, ads, social features, streaks, or gamification.

## Next tasks

1. Commit and push the verified Phase 5 scope; open a pull request only if separately requested.
2. For the next bounded phase, draft a separate spec around the step-only entry chart with sleep/event overlays, using the verified Phase 5 engine; do not bundle histogram/sleep-comparison/report work without a new decision gate.
3. Preserve the current Room 4/export 4 interfaces and Phase 5 engine invariants unless a later approved spec explicitly supersedes them.

## Last verification

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

- Phase 1 backdate/edit/note dialog-open state still needs a dedicated true process-recreation correction; see `docs/specs/BACKLOG.md`.
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
