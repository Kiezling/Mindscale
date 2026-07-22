---
description: Run one bounded spec task through test scaffolding, workhorse implementation, and Android verification
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Agent
---

Implement: $ARGUMENTS

1. Read `AGENTS.md`, current state, and the governing spec. If non-trivial work has no spec, stop and recommend `/spec`.
2. Select one task small enough for a focused session.
3. Use `test-scaffolder` only when test setup is mechanical and independent.
4. Dispatch `implementer` with the exact spec section, files in scope, tests, and oracle.
5. Allow two attempts against the same diagnosis. Then run `/escalate`.
6. On success, use `architect-reviewer` only for critical/cross-cutting paths.
7. Update `PROJECT_STATE.md` at the phase boundary.
8. Report changed files, oracle evidence, review verdict, coverage gaps, and repository status. Do not commit unless requested.
