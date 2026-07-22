# MindScale model-orchestration SOP

Goal: maximize verified product quality per unit of scarce model attention while keeping project memory durable across sessions, compaction, and provider changes.

This is rationale and maintenance guidance. Routine agents should follow `AGENTS.md` without loading this entire file.

## 1. Separate durable truth from chat context

Chats are working memory, not the project record. The durable chain is:

- `AGENTS.md`: short, provider-neutral operating contract.
- `CLAUDE.md`: Claude-specific adapter only.
- `PROJECT_STATE.md`: current phase, evidence, blockers, and exact next work.
- `docs/DECISIONS.md`: stable decisions and supersession history.
- `FAILED_PATHS.md`: plausible dead ends and active workarounds.
- `docs/specs/`: desired behavior, frozen interfaces, and acceptance criteria.
- Git history and oracle output: evidence of what actually changed and passed.

Before a provider switch, supply only the shared contract, current state, governing spec, relevant decisions/failures, and current diff. Do not export an entire transcript.

## 2. Route by capability, not brand

Use four functional tiers:

1. Mechanical/fast: exploration, summaries, fixtures, repetitive edits, and documentation upkeep.
2. Workhorse: most implementation against a clear spec.
3. Senior specialist: hard debugging, performance, concurrency, security, and deep design critique.
4. Scarce architect/adjudicator: specs, irreversible design, cross-cutting tradeoffs, and critical final review.

Model names and entitlements change. Provider adapters map available models to these roles; the contract stays stable.

### Scarce-tier credit policy

- Spend scarce-tier tokens upstream on decisions that prevent many downstream mistakes.
- Ask for interfaces, invariants, acceptance criteria, risk ranking, or a verdict—not broad code generation.
- Reconnaissance happens first in the cheap tier and returns a compact evidence packet.
- Default to one spec pass and one critical review per feature.
- Downshift immediately after the decision is recorded.
- If the task cannot justify a machine-checkable outcome or durable decision, it probably does not justify the scarce tier.

## 3. Oracle-first work

Every task needs a pass condition before implementation. For MindScale, the default deterministic oracles are Gradle tests, lint, assembly, and—when relevant—instrumented tests and emulator behavior.

Cheap models are safe only when the oracle covers the behavior. A green build does not validate untested product logic. When coverage is missing, either add a test, specify a manual check, or explicitly report the gap.

## 4. Feature lifecycle

1. Human/product clarification: goal, constraints, non-goals, and approval boundaries.
2. Cheap reconnaissance: relevant files/interfaces only, summarized with locations.
3. Scarce or senior design: spec and risk decisions for non-trivial work.
4. Cheap test scaffolding when acceptance criteria are mechanical.
5. Workhorse implementation in bounded tasks.
6. Deterministic oracle after each task.
7. Senior/scarce review only for critical or cross-cutting paths.
8. Update state, decisions, failed paths, and spec status once evidence is final.

Avoid heavyweight ceremony for a tiny, reversible edit with explicit acceptance criteria.

## 5. Attempt budgets and escalation

Two failed attempts against the same diagnosis are evidence that the context, tier, or spec is wrong. Stop and create a compact packet: criteria, failure signal, diff, hypotheses/evidence, and recommended next step. Escalate in a fresh context.

Do not pay a stronger model to read a polluted transcript. Do not let a stronger model make a third speculative edit before it reproduces and diagnoses the failure. After three tier hops, require human review of the spec.

## 6. Context hygiene

- One primary task per session.
- Keep raw searches, logs, and broad reading out of the main decision context.
- Delegate only independent, bounded work; subagents duplicate context and consume extra tokens.
- Require summaries with file/line references and strict output caps.
- Re-read exact files on demand rather than relying on model memory.
- At phase boundaries, record the current commit, dirty files, oracle evidence, blocker, and next action.
- Treat stale task lists as bugs: reconcile state against Git and tool output before continuing.

## 7. Provider adapters

### Claude Code

Claude automatically reads `CLAUDE.md`. That file points to the shared contract and maps Haiku/Sonnet/Opus/Fable to the four tiers. `.claude/agents`, commands, and hooks provide Claude-native enforcement. Use the Android Studio-launched session for selection and diagnostics context.

### ChatGPT and Codex

Codex automatically consumes repository `AGENTS.md` guidance and can use bounded subagents when explicitly requested or allowed by project instructions. Keep the main `AGENTS.md` concise and move rationale/specs into referenced documents. Subagents are for independent exploration, tests, or log analysis—not a default response to every task.

For a plain ChatGPT conversation without repository instruction discovery, attach or provide:

1. `AGENTS.md`;
2. `PROJECT_STATE.md`;
3. the governing spec;
4. only relevant `FAILED_PATHS.md` and decision entries;
5. the current diff or exact files in scope.

Ask with four fields: goal, context, constraints, and done-when evidence. Select a lower-cost/reasoning mode for mechanical work, a balanced coding mode for implementation, and the highest reasoning mode only for the scarce-tier triggers. Current Codex guidance recommends `AGENTS.md` for durable repository rules and notes that subagents can reduce context pollution but consume additional tokens.

Official reference: https://developers.openai.com/codex/

## 8. Maintenance loop

- When the same mistake happens twice, add the smallest practical rule to `AGENTS.md` or a precise entry to `FAILED_PATHS.md`.
- When model capabilities change, update provider mappings—not the shared tier definitions.
- Periodically sample workhorse tasks that may now fit the mechanical tier.
- If escalation is frequent, improve specs and oracles before buying more high-tier rescue work.
- Remove stale instructions after confirming they are obsolete; mark decisions/failures superseded so history remains understandable.

## 9. Anti-patterns

- Defaulting to the strongest model “to be safe.”
- Allowing premium models to explore the repository file by file.
- Carrying multiple features, long logs, and failed hypotheses in one main session.
- Maintaining the same task in several trackers.
- Treating a green build as proof of untested behavior.
- Pasting full transcripts into an escalation or provider handoff.
- Letting provider-specific commands become the only copy of project policy.
