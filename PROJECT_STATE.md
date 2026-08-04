# MindScale project state

Last updated: 2026-08-03

## Goal

Build MindScale as a native Android application using Kotlin, Jetpack Compose, Material 3, and Kotlin DSL. MindScale is a local-only, event-contingent symptom tracker for depression/anxiety: a measurement instrument that assembles the user's own data without interpreting it.

The product source is the Claude Design project `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`: `SPEC.md` is the rationale and `MindScale v2.dc.html` is the visual/behavioral reference for Track, Full Log, Insights, Report, Safety card, Profile, and Settings. A local exported handoff is available at `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\`; its `MindScale v2.dc.html` is the primary implementation reference. Repository specs under `docs/specs/` govern native implementation after human approval.

## Current phase: Phase 3 implemented and verified; awaiting commit approval

- Branch: `main`
- Current committed checkpoint: `4cf9f068d42a0c906b76ddac460d4b5e907410d3` (`Implement Phase 2 track completeness`)
- Upstream: `origin/main` at the same commit; remote is `https://github.com/Kiezling/Mindscale.git`
- Phase 1 spec: `docs/specs/SPEC-track-numpad-logging.md` — `IMPLEMENTED`
- Phase 2 spec: `docs/specs/SPEC-track-phase2-completeness.md` — `IMPLEMENTED`
- Phase 3 spec: `docs/specs/SPEC-full-log.md` — `IMPLEMENTED` on 2026-08-03
- Current worktree: verified Phase 3 implementation and documentation are uncommitted; the committed/upstream checkpoint remains Phase 2 until the user explicitly requests a commit

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

## Active blocker

None. Phase 3 is implemented and fully verified. It is intentionally uncommitted until the user explicitly approves commit/push. D-9 option A remains in force: keep the existing native theme for Phase 3 and apply gold/ink tokens consistently in the later global brand phase.

## Known decisions

- Preserve the template-generated Gradle/AGP/Kotlin/Compose compatibility set; do not independently change toolchain versions.
- Use the Gradle wrapper and Android Studio bundled JDK; never install or use system Gradle.
- Keep SDK, AVDs, projects, and Gradle caches on S:; keep Android Studio and normal configuration on C:.
- Keep production signing and publishing out of scope until explicitly requested.
- Use `AGENTS.md` as the provider-neutral contract and `CLAUDE.md` only as the Claude adapter.
- Reserve scarce models for specs, irreversible architecture decisions, adjudication, and critical review; use cheaper tiers for exploration and routine implementation.
- Preserve local-only storage and never add interpretation, accounts, servers, ads, social features, streaks, or gamification.

## Next tasks

1. User reviews the running Phase 3 result or requests adjustments.
2. On explicit approval only, commit the complete Phase 3 diff and push `main` to `origin/main`.
3. Select and approve the next bounded product phase before changing application code.
4. Before later Settings/Profile work, reconcile the template purple/dynamic-color theme with the design's gold/ink brand tokens.
5. Continue backlog items in `docs/specs/BACKLOG.md` only when their governing phase is approved.

## Last verification

Phase 3 final pre-commit verification completed 2026-08-03 from `S:\Android\AndroidProjects\MindScale`:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Results:

- Unit tests: 80/80 passed; 0 failures, 0 errors, 0 skipped.
- Lint: passed with 0 errors and 22 existing template/dependency-version warnings.
- Debug assembly: passed.
- Intended device: `emulator-5554`, `MindScale_API_36`.
- Instrumented tests: 51/51 passed with 0 failures and 0 skipped, including existing Phase 1/2 regressions, real Room DAO bounds/mutations, mixed Full Log Compose coverage, Track/Log navigation, and recreation.
- Diff check: passed; only harmless line-ending notices were emitted.
- Manual installed-app walkthrough used mixed seeded records and confirmed ordering, numeric ratings, Sleep badge/interval duration, marker, chips/note, inline edit persistence, distinct actions, native date picker, rotation retention, clean process relaunch, and Back-to-Track behavior. The temporary instrumentation seeder was removed before final scope inspection.
- Current commit/upstream remain `4cf9f068d42a0c906b76ddac460d4b5e907410d3`; Phase 3 is not committed or pushed.

## Known coverage gaps and backlog

- Phase 1 backdate/edit/note dialog-open state still needs a dedicated true process-recreation correction; see `docs/specs/BACKLOG.md`.
- No Settings UI yet exposes Phase 2's `sleepOn`, `askChips`, paused/check-in, or custom vocabulary controls.
- The approved time-weighted/hold/auto-end episode model remains unimplemented and must reconcile Phase 2's simplified onset rule.
- Brand theming remains the template Material 3 palette until its approved phase.
- Full Log import/export and clinician-report data actions remain deferred; no inert controls are shown.

## Handoff checklist

Before a pause, compaction, or provider switch, update:

- branch and current commit;
- dirty files and what each contains;
- governing spec/task and exact next action;
- last oracle commands/results and coverage gaps;
- blocker or approval gate;
- new decisions and failed paths.
