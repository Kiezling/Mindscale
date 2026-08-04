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

## D-006 — Phase 6 step-only entry chart

- Date: 2026-08-04
- Decision: add one native step-only entry chart to Insights using the Phase 5 hold/sleep derivation, with normalized sleep gaps/bands, marker event lines, persistent touch/TalkBack readout, and existing note-preview privacy. Extend the unified DAO projection for Entry note and Marker text, but keep Room/backup at version 4 and add no chart mode, ramp, cache, library, or inference. See `docs/specs/SPEC-insights-entry-chart.md`, D-1 through D-8.
- Reason: supplies the next product-defined descriptive view without allowing the handoff's smoothed-line prototype to contradict the measurement model or create a second derivation path.
- Supersedes: none; narrows the deferred step-chart portion of Phase 5 D-1/D-4.

## D-007 — Phase 7 Track dialog restoration

- Date: 2026-08-04
- Decision: restore Track Backdate/Edit/Note as one versioned, primitive `SavedStateHandle` dialog envelope; revalidate Entry ids reactively from Room, retain and warn on same-field conflicts, keep targeted affected-row-count mutations, and clear drafts only on cancel, success, or definitive stale outcomes. Delete joins the live mutually exclusive modal model but is not restored. Room/schema/backup/export/dependencies remain version 4/unchanged. See `docs/specs/SPEC-track-dialog-restoration.md`, D-1 through D-10.
- Reason: Compose-only field buffers cannot restore when a rebuilt `TrackViewModel` forgets the owning dialog, and full Room entities or stale snapshots do not belong in saved instance state.
- Supersedes: Phase 1's overbroad claim that its Compose-only dialog buffers covered low-memory process death; preserves Phase 2's marker restoration precedent and Phase 3's targeted mutation contracts.

## D-008 — Phase 8 onset-gap histogram

- Date: 2026-08-04
- Decision: add one native onset-to-onset elapsed-gap histogram derived from the existing Phase 5 episode model. Require six onsets/five gaps with both onsets inside the selected half-open range; use ten frozen elapsed-day buckets, exact denominators/boundary readouts, and no periodicity or inference claim. Use accessible 48 dp Compose bucket cells and primitive SavedStateHandle selection while keeping the single Room/StateFlow derivation and Room/schema/JSON version 4, CSV, backup rules, permissions, dependencies, and architecture unchanged. See `docs/specs/SPEC-insights-onset-gap-histogram.md`, D-1 through D-10.
- Reason: this is the next product-defined episode-level view and can reuse the reviewed episode engine without adding storage or a competing onset interpretation; sparse refusal and exact language constrain apophenia risk.
- Supersedes: none; narrows the onset-gap portion deferred by Phase 5 D-1 and Phase 6 D-1.

## D-009 — Phase 9 onset-time counts

- Date: 2026-08-04
- Decision: add one native 24-bucket local-clock onset-time count view derived from existing Phase 5 episodes. Require six in-range starts; use the current device zone, explicit recording-displacement/current-zone caveats, exact denominators and half-open hour readouts, and one deterministic wrapping four-hour count sentence with earliest-hour tie-breaking. Use accessible 48 dp Compose cells and primitive `SavedStateHandle` selection while keeping the single Room/StateFlow derivation and Room/schema/JSON version 4, CSV, backup rules, privacy, permissions, dependencies, and architecture unchanged. See `docs/specs/SPEC-insights-onset-time-counts.md`, D-1 through D-10.
- Reason: this is the next product-defined Insights view and can reuse reviewed episode onsets without a new classifier or persistence change; descriptive sleep counts require separate cohort and boundary decisions.
- Supersedes: none; narrows the onset-time portion deferred by Phase 5 D-1, Phase 6 D-1, and Phase 8 D-1.

## D-010 — Phase 10 descriptive sleep counts

- Date: 2026-08-04
- Decision: add one native descriptive sleep section from the existing normalized sleep union. Group completed periods by recorded Wake in the selected half-open range, classify exact elapsed duration `<=3h` as nap and `>3h` as night, show direct counts and exact duration summaries from the first completed period, and exclude/disclose incomplete periods. Omit the prototype's five-hour post-wake comparison and every effect claim. Restore only primitive category selection while keeping Room/schema/JSON version 4 and CSV, backup, privacy, permissions, dependencies, and architecture unchanged. See `docs/specs/SPEC-insights-sleep-counts.md`, D-1 through D-10.
- Reason: this is the smallest handoff-supported sleep slice that preserves raw-data truth and range behavior without turning recording opportunity or sparse later ratings into an implied sleep effect.
- Supersedes: none; narrows the descriptive sleep portion deferred by Phase 5 D-1, Phase 6 D-1, Phase 8 D-1, and Phase 9 D-1.
