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

## 2026-08-03 — `NoDefaultCurrentDirectoryInExePath=1` breaks bare `gradlew.bat` from cmd.exe

- Scope: `.claude/hooks/oracle.sh` on this machine (a headless/background Claude session, not launched via the Android Studio plugin).
- What failed: `cmd.exe //d //s //c "gradlew.bat test"` failed with "'gradlew.bat' is not recognized as an internal or external command, operable program or batch file." even though `cmd.exe`'s working directory was confirmed correct (`cd` and `dir gradlew.bat` both succeeded from the same cmd.exe invocation). Root cause: this machine has the Windows env var `NoDefaultCurrentDirectoryInExePath=1` set, which disables cmd.exe's normal fallback of searching the current directory for a bare command name not found on `PATH`. This blocked a `git commit` via the `gatekeeper.sh`/`oracle.sh` hook chain, even though `test`/`lint`/`assembleDebug` had all just passed moments earlier when invoked directly (with an explicit `.\` prefix) from Git Bash.
- Decision/workaround: `oracle.sh`'s `run_gradle()` now invokes `.\gradlew.bat` (explicit relative path) instead of the bare `gradlew.bat`, which resolves regardless of `NoDefaultCurrentDirectoryInExePath`.
- Status: resolved and regression-tested (`bash .claude/hooks/oracle.sh` run standalone, confirmed `PASS test lint assembleDebug` logged). Do not revert the leading `.\` back to a bare filename.

## 2026-08-04 — Terminated Gradle test run left a truncated binary result

- Scope: local JVM oracle recovery after a timed-out/stopped Gradle invocation.
- What failed: `testDebugUnitTest` immediately reported only `java.io.EOFException` and launched no test executor because the interrupted run left `app/build/test-results/testDebugUnitTest/binary/results-generic.bin` truncated. Repeated source-level debugging could not address it.
- Decision/workaround: confirm with `--info` that failure occurs before `Gradle Test Executor` starts, then run `./gradlew.bat cleanTestDebugUnitTest testDebugUnitTest`. Do not delete source or Gradle caches for this signal.
- Status: resolved; the focused suite and all later full oracles passed.

## 2026-08-04 — Connected-suite dialog IME and Room teardown races

- Scope: full `connectedDebugAndroidTest` regression runs while verifying Phase 8; production behavior was not implicated.
- What failed: after Activity recreation, the first Espresso Back in the restored Edit dialog could be consumed by the still-open soft keyboard, so the full-suite navigation assertion observed the dialog still present even though the test passed alone. Separately, `TrackDialogSavedStateTest` could close its in-memory Room database while a destroyed `TrackViewModel` still had a sleep query in flight, crashing the instrumented process after otherwise passing tests.
- Decision/workaround: explicitly close the soft keyboard before the navigation test's Back assertion. In the saved-state Room harness, retain each ViewModel scope job, destroy owners, join those jobs, drain the query/transaction executors, and only then close the database. Do not mask either signal with retries or production delays.
- Status: resolved and regression-tested; the focused classes and final 91/91 connected suite passed.

## 2026-08-05 — `gh` is installed and authenticated but not on the agent shell's PATH

- Scope: any GitHub CLI work — opening, inspecting, or merging a PR — from an agent shell on this machine. Not a `gh` fault and not an authentication problem.
- What failed: `gh` does not resolve in either the PowerShell or the Bash tool (`Get-Command gh` and `command -v gh` both come back empty), even though `gh --version` reports 2.97.0 and the CLI is already authenticated for `Kiezling/Mindscale`. WinGet installs the shim into `%LOCALAPPDATA%\Microsoft\WinGet\Links`, which is on the interactive user's PATH but not the one these tool shells inherit. The failure looks like a missing or broken install, which is the wrong thing to start debugging.
- Decision/workaround: prepend the shim directory for the invocation instead of reinstalling or re-authenticating: `$env:PATH += ";$env:LOCALAPPDATA\Microsoft\WinGet\Links"` before the first `gh` call in a command. Shell state does not persist between tool calls, so repeat it in each command that uses `gh`. Verify with `gh --version` rather than assuming.
- Status: active environment rule; used for the Phase 13 PR #10 create/ready/view/merge sequence. Do not install a second copy of `gh` or modify the machine PATH to work around it.

## 2026-08-05 — Harness command guard rejects a long PR body passed inline to PowerShell

- Scope: passing a multi-section markdown body inline to a PowerShell command, typically `gh pr create` with a `@'...'@` here-string. Not a repository hook and not a `gh` fault.
- What failed: the command was rejected before execution with `Remove-Item on system path '/' is blocked. This path is protected from removal.` Nothing ran and no output was produced. The message is the agent harness's own command guard, **not** `.claude/hooks/gatekeeper.sh`, which rejects with `Blocked broadly destructive command`. Do not start by debugging the repository hooks; they were never reached.
- Reproduced 2026-08-05 by bisection, with negative controls. One standalone `/` token in the inline text is a necessary condition: a full markdown body containing `` `ACTION_DIAL` / `ACTION_SENDTO` / `ACTION_VIEW` `` was blocked, and the byte-identical body with those two slashes changed to the word `and` passed. It is not sufficient on its own — that same slash line passed when paired with each individual section, with 1,800 characters of benign filler, and with a markdown link. Ruled out as causes: apostrophes and here-string quote state, non-ASCII characters (em dash, emoji), total command length, markdown tables and pipe characters, and the words `erase`/`deleted`. Two bodies that each passed alone were blocked when concatenated, so the guard is a heuristic over the whole command rather than a single-token match. The exact sufficient condition was not isolated after seventeen probes and is not worth further spend; it lives in a closed harness component that can change.
- Decision/workaround: do not pass a long body inline. Write it to a file with the Write tool, then reference it: `gh pr create ... --body-file <path>`. This is how PR #10 was actually created after the inline attempt was blocked. The same applies to any long prose argument: `git commit -F <file>` rather than `-m`. That is not hypothetical — the commit recording this very entry was itself rejected, because its message described the forward-slash finding using a bare slash, and it went through unchanged once moved to a file.
- Status: active. Recognize the message, switch to a file, and move on. Do not attempt to defeat the guard by rewording, escaping, splitting, or encoding the text, and do not weaken or disable the repository hooks in response — they are a different mechanism and were not involved.
