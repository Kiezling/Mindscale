---
description: Package a two-strike failure and move one capability tier up in fresh context
allowed-tools: Read, Bash, Grep, Glob, Agent
---

Escalate the current failure. Optional target: $ARGUMENTS

1. Build a packet of at most 500 tokens:
   - spec/acceptance reference;
   - trimmed failure signal;
   - files and unified diff;
   - hypotheses tested with evidence;
   - recommended next tier or human decision.
2. Append date, task, from-tier, and to-tier to `.claude/logs/escalations.log`.
3. Send only the packet to the next tier: Haiku → Sonnet → Opus → Fable → human.
4. Fable reviews/adjudicates but does not implement; return its decision to a fresh Sonnet implementer.
5. Stop for human spec review after three tier hops or a second post-Fable implementation failure.
