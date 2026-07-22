# MindScale Claude Code adapter

`AGENTS.md` is the canonical project contract and is always in effect. Read it before non-trivial work. At session start, read `PROJECT_STATE.md`, scan `FAILED_PATHS.md` headings, and load only the governing spec and relevant failure entries.

Do not routinely load `docs/ORCHESTRATION.md`; it is rationale, not turn-by-turn context.

## Claude model routing

- Haiku: read-only exploration, bounded summaries, boilerplate, fixtures, formatting, documentation maintenance, and other low-judgment mechanical tasks.
- Sonnet: default implementation workhorse for a clear spec or explicit acceptance criteria.
- Opus: difficult debugging after two Sonnet attempts, concurrency, performance, security, novel algorithms, or deep architecture critique.
- Fable: scarce architect/adjudicator for feature specs, irreversible or cross-cutting design decisions, and final review of critical paths.

Fable guardrails:

- Do not use Fable for repository exploration, routine implementation, boilerplate, formatting, test execution, or commit messages.
- Give Fable a compact evidence packet and request decisions, invariants, acceptance criteria, or a verdict.
- Default budget is one spec/architecture pass and one critical review pass per feature. Ask before another Fable pass.
- Keep Fable output under 800 tokens unless the user explicitly requests a full spec.
- After the decision, record it and downshift implementation to Sonnet.

## Claude workflow helpers

- `/spec`: create or finalize a frozen feature spec using Fable after cheap reconnaissance.
- `/implement`: scaffold tests, implement with Sonnet, run Android oracles, and escalate after two failed attempts.
- `/review`: Fable read-only review for critical-path or cross-cutting changes.
- `/escalate`: create a compact packet and move one tier up in a fresh context.
- `/downshift`: periodically identify work that can move to a cheaper tier.

Use the agents in `.claude/agents/` only for bounded roles. Builders must not recursively delegate. Subagent summaries must be short and include exact file/line references.

## Android Studio integration

- Launch Claude Code from the Android Studio plugin or the IDE-integrated terminal so selected-file context and diagnostics are available.
- Use IDE context for navigation and diagnostics, but verify correctness with the Gradle and device oracles in `AGENTS.md`.
- Keep permission mode manual for edits and commands unless the user grants a narrow, task-specific exception. Never use unrestricted automatic approval.

## Hooks

The checked-in hooks are supplemental guardrails:

- `.claude/hooks/gatekeeper.sh` blocks broadly destructive commands and gates non-doc commits behind the Android oracle.
- `.claude/hooks/oracle.sh` records one PASS/FAIL line in `.claude/logs/oracle.log`.
- Hook success never replaces review or task-specific verification.
