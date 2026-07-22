---
description: Create or finalize a frozen Android feature spec with scarce-tier judgment
allowed-tools: Read, Write, Grep, Glob, Bash, Agent
model: fable
---

Author a specification for: $ARGUMENTS

1. Confirm the human goal, constraints, non-goals, and approval boundaries. Stop for material ambiguity.
2. Delegate broad reconnaissance to the cheap read-only `explorer`; receive at most 500 tokens of evidence.
3. Read `docs/specs/TEMPLATE-SPEC.md`, relevant decisions, and relevant failed paths.
4. Write `docs/specs/SPEC-<slug>.md` with frozen interfaces, invariants, Android lifecycle/API implications, failure behavior, and machine-checkable acceptance criteria.
5. Decompose implementation into workhorse-sized tasks with the smallest oracle for each.
6. Do not implement. Output the spec path, unresolved approval gates, and a summary of at most 8 lines.
