# MindScale decision log

Record stable decisions that should survive chat resets and model/provider changes. Keep entries short; link to a spec when one exists. If a decision changes, add a superseding entry instead of rewriting history.

## D-001 — Native Android baseline

- Date: 2026-07-21
- Decision: Kotlin, Jetpack Compose, Material 3, Kotlin DSL, minimum SDK 26, target SDK 36, and template-selected compile SDK/tool versions.
- Reason: current stable Android Studio generated and verified this mutually compatible baseline.
- Supersedes: none.

## D-002 — Toolchain and storage ownership

- Date: 2026-07-21
- Decision: use the project Gradle wrapper, Android Studio bundled JDK, `S:\Android\Sdk`, `S:\Android\Avd`, and `S:\Android\Gradle`.
- Reason: avoids version drift and protects limited C: storage.
- Supersedes: old C:-based SDK/AVD/cache paths.

## D-003 — Provider-neutral agent contract

- Date: 2026-07-22
- Decision: `AGENTS.md` is the shared operating contract. `CLAUDE.md` and `.claude/` contain only Claude-specific routing and enforcement.
- Reason: Claude Code and ChatGPT/Codex can share project rules without duplicating large instruction blocks or allowing provider-specific mechanics to drift.
- Supersedes: the original single-file `CLAUDE.md` rules.

## D-004 — Capability-first model routing

- Date: 2026-07-22
- Decision: route by task risk and judgment requirements, not by provider brand. Reserve the strongest/scarcest tier for specs, irreversible design, adjudication, and critical review; downshift implementation and mechanical work.
- Reason: protects scarce credits, reduces context cost, and keeps high-tier attention on decisions with lasting leverage.
- Supersedes: defaulting an entire feature session to the strongest model.

## D-005 — Phase 5 Insights foundation

- Date: 2026-08-03
- Decision: derive episodes from raw Entries/SleepIntervals with a mandatory configurable awake-time hold; add the native range-based episode summary/raster without persisted episodes, interpolation, sample data, or inferential claims. Use additive Room 3→4 hold persistence, JSON backup v4, and a transactional hold-aware onset classifier. See `docs/specs/SPEC-insights-foundation.md`, D-1 through D-10.
- Reason: establishes the product's event-contingent, awake-time-weighted measurement model while keeping the first Insights slice bounded, local, deterministic, and accessible.
- Supersedes: Phase 2's explicitly temporary prior-row-only onset classification.
