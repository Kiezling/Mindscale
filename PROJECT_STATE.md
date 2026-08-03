# MindScale project state

Last updated: 2026-08-03

## Goal

Build MindScale as a native Android application using Kotlin, Jetpack Compose, Material 3, and Kotlin DSL. Product behavior is now specified for the first vertical slice (see governing spec below); do not invent features or backend requirements beyond an approved spec. Preserve the verified Android toolchain and keep premium-model usage focused on decisions that genuinely require it.

The product itself (MindScale v2): a local-only, event-contingent symptom tracker for depression/anxiety — a measurement instrument, not an intervention. Full product rationale lives in the source design project's `SPEC.md` (Claude Design project id `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`, readable via the `DesignSync`/`claude-design` MCP tools); the same project's `MindScale v2.dc.html` mockup is the visual/behavioral reference for all 7 screens (Track, Full log, Insights, Report, Safety card, Profile, Settings). A full 8-phase native-implementation plan derived from that mockup exists (not yet committed to the repo as a doc — currently only in the authoring session's plan file) covering all 7 screens; this repo has implemented Phase 1 only.

## Current phase: Phase 1 of 8 implemented (Track screen numpad logging + persistence)

- Branch: `master`
- Current verified commit: `95977fd615358ef6f00056b5d7d599468ecaea0c` (working tree is DIRTY on top of this — see below).
- Starter baseline commit: `673d6c76dff4a0ea27a4d036e251a0ac83e8538e`
- Working-tree state at handoff: **dirty, uncommitted** — Phase 1 implementation is complete and oracle-verified but not yet committed (no commit was requested). Run `git status --short --branch` to see the full file list; summary: modified `app/build.gradle.kts`, `MainActivity.kt`, root `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`; new `app/schemas/`, `app/src/{main,test,androidTest}/java/com/kieslingdev/mindscale/{data,track,ui/theme}/`, `docs/specs/SPEC-track-numpad-logging.md`. Also untracked: several `.idea/*` files pre-existing from before this session (unrelated to this work, left alone).
- Project: `S:\Android\AndroidProjects\MindScale`
- SDK: `S:\Android\Sdk`
- AVD: `S:\Android\Avd\MindScale_API_36.avd`
- Gradle user home: `S:\Android\Gradle`
- JDK: Android Studio Quail 2 bundled JDK 21.0.10

## Completed work

- Installed and verified Android Studio Quail 2 (2026.1.2), the API 36 SDK/toolchain, Git, native Claude Code, and the official Claude JetBrains plugin.
- Generated the Android Studio Empty Activity Compose starter with application ID `com.kieslingdev.mindscale`, minimum SDK 26, target SDK 36, and compile SDK 36.1.
- Created and booted the S:-based Pixel 9 API 36 Google Play AVD; confirmed hardware acceleration, adb connectivity, installation, and foreground launch.
- Passed `clean`, `test`, `lint`, and `assembleDebug`; confirmed an empty Android Studio diagnostics result for `MainActivity.kt`.
- Initialized Git and committed the working starter baseline as `673d6c7`.
- Removed the superseded Android Studio, duplicate C:-based SDK/Pixel 9 AVD, and obsolete local caches after verification and approval.
- Adapted the orchestration template into shared `AGENTS.md` guidance plus a Claude-specific adapter, durable state/decision/failure records, Android specs, and tested Claude hooks; committed in the current `HEAD`.
- Authored `docs/specs/SPEC-track-numpad-logging.md` (Status: DRAFT — implemented but not yet promoted to IMPLEMENTED in the doc) for the Phase 1 vertical slice: numpad logging (tap-to-record 0–10, long-press-to-backdate) + recent-entries list (edit/note/delete) + local Room persistence. Explicitly excludes sleep tracking, onset chips, markers, help card, paused/check-in banners, and breathing (later phases).
- Implemented Phase 1 end-to-end: Room (`Entry`/`ChipsConverter`/`EntryDao`/`MindScaleDatabase` in `data/`), `band()`/`intensityColor()` domain helpers, `TrackUiState`/`TrackEvent`/`TrackViewModel` (StateFlow, manual-DI, no repository layer), `TrackScreen`/`TrackRoute` composables, wired into `MainActivity`'s existing `Scaffold` (no navigation-compose yet — single screen).
- A critical-path `architect-reviewer` pass on this slice found and got fixed two real bugs before sign-off: (1) editing/noting an entry that had scrolled out of the observed top-10 "recent" window silently wiped its `note`/`chips` on save — fixed by having `EditEntryState`/`NoteEditState` capture the full original `Entry` at dialog-open time instead of re-looking it up; (2) in-progress note text lived only in ViewModel `StateFlow` and didn't survive true process death — fixed with a `rememberSaveable` buffer matching the pattern already used for the backdate/edit timestamp fields.
- Full oracle pass green on the `MindScale_API_36` emulator: `.\gradlew.bat test` (unit), `.\gradlew.bat lint`, `.\gradlew.bat assembleDebug`, and `.\gradlew.bat connectedDebugAndroidTest` (9/9 instrumented tests passed: `ExampleInstrumentedTest`, 4×`EntryDaoTest`, 4×`TrackScreenTest`). Manual on-device walkthrough (install, launch, tap-to-log, color-coded readout + recent-list row, delete-confirmation dialog open/cancel) confirmed working via screenshots — see this session's transcript for evidence; long-press-backdate, edit, note, and rotation/process-death survival are covered by passing automated tests but were not separately eyeballed on-device this session.

## Active blockers

None currently. Phase 1 is implemented, reviewed, and oracle-verified but **not committed** (no commit was requested this session) and the spec's own `Status:` field still reads `DRAFT` rather than `IMPLEMENTED` — update both before starting Phase 2 if picking this up fresh. Phases 2–8 (sleep/chips/markers/help/checkin, Full log, Settings/Profile, Safety card, Insights, Report/breathing, polish) remain unspecified and unimplemented.

## Known decisions (do not relitigate without new evidence)

- Use the template-generated Gradle/AGP/Kotlin/Compose compatibility set; no independent version changes.
- Use the Gradle wrapper and Android Studio bundled JDK; no system Gradle or separate project JDK.
- Keep SDK, AVDs, projects, and Gradle caches on S:; keep Android Studio and its configuration on C:.
- Keep production signing and release publishing out of scope until explicitly requested.
- Use `AGENTS.md` as the provider-neutral contract and `CLAUDE.md` only as the Claude adapter.
- Reserve scarce models for specs, irreversible design, adjudication, and critical review; use cheaper tiers for exploration and implementation.

## Next tasks

1. Decide whether to commit the Phase 1 work as-is, and update `docs/specs/SPEC-track-numpad-logging.md`'s `Status:` to `IMPLEMENTED` once committed.
2. Write the Phase 2 spec (Track screen completeness: sleep on/off, onset chips, marker input, help card, paused/check-in banners) per the approved 8-phase plan, following the same `/spec` → `/implement` → oracle → `architect-reviewer` (critical paths only) loop used for Phase 1.
3. Before Phase 4 (Settings/Profile) implements the real `theme`/brand palette, reconcile `ui/theme/Color.kt`'s still-unused generic Compose-template purple palette and `Theme.kt`'s `dynamicColor = true` default against the mockup's actual gold/ink brand tokens (deliberately deferred, not forgotten).
4. Consider a small shared `AppContainer` before wiring 3+ more screens' worth of manual DI through `MainActivity` — flagged as a non-blocking review note on Phase 1, not yet acted on.

## Last verification

Environment verification completed 2026-07-22 (see git history for that baseline).

Phase 1 vertical-slice verification completed 2026-08-03:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

All four passed on `MindScale_API_36` (emulator-5554): unit tests (`BandTest`, `IntensityRampTest`, `TrackViewModelTest`, `ExampleUnitTest`), lint clean, debug APK assembled, and all 9 instrumented tests passed on-device. Manual walkthrough via `adb`/screenshots additionally confirmed: app launches without crash on the real theme (still default Material3 purple — brand theming is a later phase), numpad tap logs an entry with a transient "N · band" readout that auto-dismisses, the entry appears in the recent list with a color-coded indicator and Edit/Note/Delete actions, and the Delete action opens a confirmation dialog (not immediate deletion) that Cancel dismisses harmlessly.

Orchestration verification completed 2026-07-22:

- `.claude/hooks/oracle.sh` ran `test`, `lint`, and `assembleDebug` successfully through Git Bash.
- A benign-command probe exited 0.
- A simulated `git reset --hard` probe was blocked with exit code 2.
- The documentation-only commit-gate probe exited 0 without rerunning Gradle.

## Handoff checklist

Before a pause, compaction, or provider switch, update:

- branch and current commit;
- dirty files and what each contains;
- governing spec/task and exact next action;
- last oracle commands/results and coverage gaps;
- blocker or approval gate;
- new decisions and failed paths.
