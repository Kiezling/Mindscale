---
description: Scarce, read-only architectural review of a critical or cross-cutting diff
allowed-tools: Read, Bash, Grep, Glob, Agent
---

Review: $ARGUMENTS

1. Identify the governing spec and the exact diff. If neither is clear, stop.
2. Use a cheap explorer first only when bounded context is missing.
3. Send the compact spec/evidence/diff packet to `architect-reviewer`.
4. Return its verdict without implementing fixes.
5. Route blocking issues to a fresh workhorse implementation context.

Do not use this command for routine style, formatting, naming, boilerplate, or lint findings.
