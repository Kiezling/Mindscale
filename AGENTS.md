# MindScale agent contract

Scope: the entire repository. This is the canonical, provider-neutral operating contract for Claude Code, ChatGPT/Codex, and human contributors. Provider-specific files may add mechanics but must not contradict this file.

Full rationale lives in `docs/ORCHESTRATION.md`. Read that document only when designing or changing the workflow; do not load it for routine tasks.

## Session start

For every non-trivial task:

1. Inspect `git status --short --branch` and `git log -1 --oneline`.
2. Read `PROJECT_STATE.md` for the current phase, blockers, and next tasks.
3. Scan the headings in `FAILED_PATHS.md`; read entries relevant to the task before retrying an approach.
4. Read the governing spec under `docs/specs/` when one exists.
5. Check whether the state document's commit and verification evidence still match the repository. Refresh stale state before relying on it.

An explicit user request outranks the backlog. Do not start backlog work when a task is already assigned.

## Source-of-truth order

When sources disagree, stop and surface the conflict instead of silently choosing:

1. Current user instruction and explicit approval boundaries.
2. Frozen feature spec and recorded decisions.
3. Executable code, tests, Gradle configuration, and tool output for current behavior.
4. `PROJECT_STATE.md` for status and sequencing.
5. `FAILED_PATHS.md` for known dead ends and workarounds.

Time-sensitive facts such as current SDK, library, API, policy, or model guidance must be verified from authoritative sources before they drive a change.

## Android baseline

- Application ID: `com.kieslingdev.mindscale`.
- Kotlin, Jetpack Compose, Material 3, and Kotlin DSL are the required stack.
- Minimum SDK is 26; target SDK is 36; the template currently compiles with Android 36.1.
- Treat the generated Android Studio project as the compatibility baseline.
- Use the project Gradle wrapper. Never install or invoke a system Gradle.
- Use Android Studio's bundled JDK through `JAVA_HOME` and the SDK through `ANDROID_HOME`.
- Do not independently change Java, Gradle, AGP, Kotlin, Compose, or SDK versions. Upgrade them as a mutually compatible set only for an approved, documented reason.
- Prefer unidirectional state flow, immutable UI state, lifecycle-aware collection, stable Compose keys, accessible semantics, and previews for reusable composables.
- Keep business logic out of composables when it can be tested independently.
- Avoid introducing a framework, abstraction layer, dependency, or module until a concrete requirement justifies it.

## Oracle commands

Run from the repository root in PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

Additional oracles:

- Environment, cache, build-system, or dependency changes: run `.\gradlew.bat clean` before the commands above.
- Instrumented/UI behavior when tests exist and an emulator is connected: run `.\gradlew.bat connectedDebugAndroidTest`.
- Device work: run `adb devices -l`, confirm the intended device, install/launch the debug build, and exercise the changed path.
- User-visible Compose changes: inspect the running API 36 emulator. Add focused UI tests or screenshot evidence when the spec requires it.

There is no separate typecheck command; Kotlin compilation is covered by the Gradle build. A task is done only when its acceptance criteria and applicable oracles pass. Passing tests are not proof for behavior that has no meaningful coverage; state coverage gaps honestly.

## Task and spec sizing

- Tiny, mechanical, low-risk changes may proceed from explicit acceptance criteria without a full spec.
- Non-trivial features, data-model changes, navigation changes, persistence, permissions, networking, background work, security-sensitive work, or cross-cutting refactors require a spec in `docs/specs/`.
- Specs must define non-goals, frozen interfaces, invariants, failure behavior, and machine-checkable acceptance criteria.
- Implementers may not silently alter frozen interfaces. Record and resolve the conflict first.
- Keep each implementation task small enough to finish and verify in one focused session.

## Capability routing

Choose the cheapest capable tier; use deterministic tools and oracles before model judgment.

| Tier | Use for | Do not use for |
|---|---|---|
| Mechanical / fast | File discovery, bounded summaries, boilerplate, fixtures, documentation upkeep, formatting, commit-message drafts | Architecture or ambiguous product decisions |
| Workhorse | Normal implementation from a clear spec, focused refactors, routine tests and fixes | Open-ended architecture or repeated failed debugging |
| Senior specialist | Difficult debugging, concurrency, performance, security, novel algorithms, design critique | Routine implementation or broad exploration |
| Scarce architect / adjudicator | Feature specs, irreversible architecture decisions, critical-path final review, resolving conflicting evidence | Bulk code, file-by-file exploration, boilerplate, routine fixes |

Provider mapping belongs in the provider adapter, not here. If model routing is unavailable, emulate it by using low reasoning for mechanical work, normal reasoning for implementation, and high reasoning only for the senior/scarce triggers above.

### Premium-model guardrails

- A scarce model writes decisions, invariants, acceptance criteria, or a review verdict—not code volume.
- Reconnaissance expected to exceed roughly 1,000 lines belongs in a cheap read-only subagent that returns at most 500 tokens with file/line references.
- Give the scarce tier a concise decision packet, never a raw transcript or full build log.
- Default to one architecture/spec pass and one critical review pass per feature. Ask before spending another scarce-tier pass.
- Downshift immediately after the decision is recorded; implementation returns to the workhorse tier.
- Do not spawn subagents unless their work is independent, bounded, and likely to save more context or elapsed time than it consumes.

## Two-strike escalation

For the same failing hypothesis in the same context, allow at most two implementation attempts:

1. Reproduce the failure and run the smallest relevant oracle.
2. After the second failed attempt, stop editing and produce an escalation packet of at most 500 tokens:
   - spec or acceptance-criteria reference;
   - trimmed failure signal;
   - files/diff changed;
   - hypotheses tested and evidence;
   - recommended next tier or human decision.
3. Escalate into a fresh context. Do not paste the full failed transcript.
4. After three tier hops, or when evidence shows the spec is defective, stop for human review.

A materially new diagnosis may reset the attempt count only when it is documented in the packet.

## Context and durable memory

- Keep one primary task per session. Finish, hand off, or explicitly park it before starting another.
- Keep the main context focused on requirements, decisions, current diff, and final evidence. Delegate noisy exploration or log analysis only when useful.
- Summarize command output to the failure signal; store durable results in project files instead of relying on chat memory.
- Update `PROJECT_STATE.md` at phase boundaries, before a long pause, and after a verified milestone—not after every tiny edit.
- Record stable architecture/product decisions in `docs/DECISIONS.md`.
- Add a `FAILED_PATHS.md` entry only for a plausible approach future agents might repeat. Include date, scope, evidence, workaround, and whether it remains active.
- Never copy the same task into multiple trackers. `PROJECT_STATE.md` owns active work; `docs/specs/BACKLOG.md` owns unstarted, non-urgent work.
- Before compaction or handoff, record the current branch/commit, dirty files, last oracle results, blocker, and exact next action in `PROJECT_STATE.md`.

## Safety and Git

- Never create or commit production signing keys, credentials, API keys, `local.properties`, IDE-local state, build outputs, APKs, or AABs.
- Do not disable security controls, bypass approval prompts, use unrestricted automatic approval, or expose secrets.
- Do not delete user data, rewrite history, force-push, publish builds, change signing/release configuration, create repositories, or push remotes without explicit approval.
- Preserve unrelated user changes. Inspect the diff before editing and again before handoff.
- Do not commit unless requested. When a commit is requested, run applicable oracles first and keep the commit scoped.

## Completion report

Report only what a reviewer needs:

- outcome and user-visible behavior;
- files changed and important decisions;
- exact verification commands and results;
- known coverage gaps, risks, or manual checks;
- updated state/spec/decision references;
- repository status and commit hash when applicable.
