# SPEC-<slug>: <feature name>

Status: DRAFT | FROZEN | IMPLEMENTED | SUPERSEDED

Owner: <human/agent>

Date: YYYY-MM-DD

Last verified commit: <sha or N/A>

## Purpose

In no more than three sentences: user problem, intended outcome, and why this belongs in MindScale.

## Non-goals

- Explicitly excluded behavior.
- Deferred platforms, integrations, polish, or migrations.

## User experience

- Entry point and primary flow.
- Loading, empty, success, error, offline, and retry behavior.
- Accessibility, localization, state restoration, and configuration-change expectations.

## Frozen interfaces and data contracts

List exact Kotlin signatures, navigation routes, state/events, persistence schema, network contracts, and module boundaries. Implementers may not alter these without returning the spec to DRAFT.

## Invariants

- Must-hold correctness and data-integrity properties.
- Threading/cancellation/lifecycle requirements.
- Privacy, permission, and security boundaries.
- Determinism requirements for calculations or state reducers.

## Android compatibility

- Minimum/target/compile SDK impact.
- API-level behavior or permission differences.
- Compose performance and recomposition constraints.
- Process death, rotation, back navigation, and offline implications.

## Acceptance criteria

- [ ] UNIT: concrete deterministic behavior and expected result.
- [ ] LINT/BUILD: `test`, `lint`, and `assembleDebug` pass.
- [ ] INSTRUMENTED: focused emulator/device scenario when applicable.
- [ ] UI/ACCESSIBILITY: semantics, focus, touch target, contrast, and state coverage when applicable.
- [ ] MANUAL: exact API 36 emulator flow and expected visible outcome.
- [ ] FAILURE: error/offline/cancellation case and recovery behavior.

## Task decomposition

Each task should fit one focused implementation session and name its smallest oracle.

1. <task> — oracle: <command/test>
2. <task> — oracle: <command/test>

## Rollout, migration, and rollback

State `N/A` when none. Otherwise describe compatibility, migration evidence, feature gating, and a safe rollback path.

## Open questions and approval gates

- Question, owner, and what work must wait for the answer.
