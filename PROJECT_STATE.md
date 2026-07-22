# MindScale project state

Last updated: 2026-07-22

## Goal

Build MindScale as a native Android application using Kotlin, Jetpack Compose, Material 3, and Kotlin DSL. Product behavior has not yet been specified; do not invent features or backend requirements. Preserve the verified Android toolchain and keep premium-model usage focused on decisions that genuinely require it.

## Current phase: development foundation ready

- Branch: `master`
- Current verified commit: this document is part of `HEAD` (`Add cross-provider AI orchestration workflow`); resolve the immutable hash with `git rev-parse HEAD`.
- Starter baseline commit: `673d6c76dff4a0ea27a4d036e251a0ac83e8538e`
- Working-tree state at handoff: clean; always confirm with `git status --short --branch`.
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

## Active blockers

None. Product scope and the first user-facing feature still require a human-approved specification.

## Known decisions (do not relitigate without new evidence)

- Use the template-generated Gradle/AGP/Kotlin/Compose compatibility set; no independent version changes.
- Use the Gradle wrapper and Android Studio bundled JDK; no system Gradle or separate project JDK.
- Keep SDK, AVDs, projects, and Gradle caches on S:; keep Android Studio and its configuration on C:.
- Keep production signing and release publishing out of scope until explicitly requested.
- Use `AGENTS.md` as the provider-neutral contract and `CLAUDE.md` only as the Claude adapter.
- Reserve scarce models for specs, irreversible design, adjudication, and critical review; use cheaper tiers for exploration and implementation.

## Next tasks

1. Define MindScale's product goal, target user, core workflow, offline/network expectations, data sensitivity, and non-goals.
2. Create and approve the first vertical-slice spec under `docs/specs/`.
3. Add tests for the vertical slice before or alongside implementation.
4. Implement with the workhorse tier and verify on the API 36 emulator.
5. Review architecture/data/privacy risks before adding persistence, accounts, networking, analytics, or AI services.

## Last verification

Environment verification completed 2026-07-22:

```powershell
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
```

All commands passed; `emulator-5554` was online and `com.kieslingdev.mindscale/.MainActivity` was the resumed activity.

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
