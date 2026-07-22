# MindScale failed paths and active workarounds

Read the headings at session start and the full entry only when it overlaps the current task. Keep resolved history concise; mark entries superseded instead of silently deleting useful evidence.

## 2026-07-21 — Incorrect C:-based MindScale project root

- Scope: project discovery and Gradle sync.
- What failed: `C:\Users\mckie\AndroidStudioProject\MindScale` contained IDE metadata but no Gradle settings/build files, so Android Studio reported that it was not a Gradle build.
- Decision/workaround: the only active project root is `S:\Android\AndroidProjects\MindScale`.
- Status: resolved; the abandoned C: folder was removed after approval. Do not recreate or reopen it.

## 2026-07-21 — Superseded C:-based Android toolchain

- Scope: SDK, AVD, Gradle cache, and Android Studio resolution.
- What failed: old C:-based paths conflicted with the requested storage layout and consumed limited C: space.
- Decision/workaround: use `ANDROID_HOME=S:\Android\Sdk`, `ANDROID_AVD_HOME=S:\Android\Avd`, `GRADLE_USER_HOME=S:\Android\Gradle`, and the Quail 2 bundled JDK.
- Status: resolved; the duplicate SDK, old Pixel 9 AVD, old Gradle cache, and Android Studio 2026.1.1 binaries were removed. Do not restore C:-based SDK PATH entries.

## 2026-07-22 — Headless Claude sessions did not expose JetBrains context

- Scope: Claude Code IDE diagnostics and selection verification.
- What failed: a separate `claude -p --ide` process could read/edit the repository and run Gradle/adb, but it did not expose the JetBrains diagnostics tool or selection context.
- Decision/workaround: launch Claude from the official Android Studio plugin or its integrated terminal. The plugin-launched session correctly received the selected `setContent {` line and returned an empty diagnostics array for `MainActivity.kt`.
- Status: active integration rule.

## 2026-07-22 — Standalone dependency/toolchain installation is prohibited

- Scope: Gradle, Java, AGP, Kotlin, and Compose upgrades.
- What failed: not an observed build failure; this entry prevents a recurring high-risk shortcut.
- Decision/workaround: use the wrapper, bundled JDK, and Android Studio template compatibility set. Upgrade the set together under an approved spec with a clean full oracle.
- Status: active constraint.

## 2026-07-22 — Git Bash rewrites single-slash `cmd.exe /c`

- Scope: `.claude/hooks/oracle.sh` on Windows.
- What failed: `cmd.exe /d /s /c "gradlew.bat <task>"` was rewritten by MSYS, opened a command prompt, and returned success without running Gradle. This produced a false-positive oracle log entry during validation.
- Decision/workaround: invoke Windows switches with double slashes from Git Bash: `cmd.exe //d //s //c "gradlew.bat <task>"`. Remove any false log entry before rerunning.
- Status: resolved and regression-tested; do not normalize the double slashes back to single slashes.
