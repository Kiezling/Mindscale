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

## D-011 — Phase 11 clinician summary and Profile foundation

- Date: 2026-08-04
- Decision: add local Profile and Report overlays, an optional validated display name, and dated PHQ-8/GAD-7 totals explicitly stored as externally obtained and user-entered. Generate one bounded, factual clinician summary from a transactional snapshot using existing episode/onset/sleep semantics; permit only explicit Copy/Share/Save; advance Room/JSON backup additively to version 5; keep records CSV compatible; and erase/reset the new data atomically. Never administer, calculate, interpret, compare, diagnose from, or attach severity/threshold language to either instrument. See `docs/specs/SPEC-clinician-report-profile.md`, D-1 through D-12.
- Reason: supplies the next product-ordered clinician conversation artifact while preserving local-only privacy, explicit provenance, deterministic data integrity, and MindScale's role as a measurement instrument rather than an assessment.
- Supersedes: none; resolves the Report/Profile backlog item and rejects conflicting inferential prototype copy.

## D-012 — Phase 12 local import and restore

- Date: 2026-08-04
- Decision: add exactly two local import actions with separate, never-blended semantics. `Restore from backup` accepts a MindScale JSON backup of version 3, 4, or 5 and replaces every record, the canonical settings row, the canonical Profile row, and all external totals, inserting the file's ids verbatim into emptied tables. `Import records` accepts a MindScale records CSV and only appends ratings, sleeps, and markers. No file may mutate Room until it has been size-limited, strictly UTF-8 decoded, parsed by a hand-written bounded reader, version-checked, structurally and semantically validated, conflict-checked, previewed with exact factual counts and disclosed defaults, and explicitly confirmed. Every malformed, ambiguous, duplicate, conflicting, oversized, unsupported, or future-version file is rejected totally — never skipped, repaired, reinterpreted, truncated, or partially accepted. The approved mutation runs in one Room transaction with six post-mutation checks that roll back on violation. Imported PHQ-8/GAD-7 totals require and re-assert fixed `EXTERNALLY_OBTAINED_USER_ENTERED` provenance and are never scored, compared, or interpreted. Raw file content never enters `SavedStateHandle`. No schema change, migration, downgrade path, dependency, permission, or toolchain change is added. See `docs/specs/SPEC-import-restore.md`, D-1 through D-12.
- Reason: Phase 4 and Phase 11 exports were deliberately one-way, leaving reinstall, device change, and post-erase recovery unsupported. Replace-only JSON and add-only CSV are the smallest coherent semantics that are explainable to a user, and total rejection is the only policy consistent with MindScale's rule that it arranges the user's data without ever reinterpreting it.
- Supersedes: none; resolves the deferral in D-5 of `SPEC-settings-data-control.md` and satisfies the future-import contract required by D-9 of `SPEC-clinician-report-profile.md`.
- Amendment (2026-08-04, non-semantic): `BackupPayload` and `RecordsPayload` are declared in the `data` package rather than `settings` as the spec's frozen-interface block wrote them. Name, fields, and meaning are unchanged; the move only prevents a `data` → `settings` package cycle, because `DataControlDao` accepts these types while every parser already depends on the `data` entities. Recorded here because `AGENTS.md` requires a frozen-interface change to be documented rather than made silently.
- Amendment (2026-08-04, behavioral clarification): an import stops being cancellable once `confirmImport` has started its Room transaction. The spec froze cancellation for the picker and read phases and required writes to be cancellable "before the transaction starts"; clearing the pending state mid-transaction would have reported a cancellation while the mutation completed. Cancel is now a no-op and is visibly disabled while the mutation runs.

## D-013 — Phase 13 native Safety card and personal safety plan

- Date: 2026-08-05
- Decision: add one `SAFETY` overlay holding verified crisis resources and the user's own Stanley-Brown safety plan, stored locally. Preserve all six canonical Stanley-Brown steps in declaration order — warning signs, internal coping, distraction, people to ask for help, professionals, making the environment safe — with plain-language headings mapped one-to-one in the spec. Freeze every crisis string verbatim, including a single 988 block covering the United States and Canada, a Find A Helpline pointer for everywhere else, an emergency sentence with deliberately no button, and a visible verification date. Reach the card only by a user tap from a persistent Track footer link or a Profile row; it is never a pop-up, never auto-navigated to, and its content depends on nothing recorded — `SafetyViewModel` takes `SafetyPlanDao` and nothing else, so no future edit can make it inferential without breaking a frozen interface. Hand phone actions off with `ACTION_DIAL`/`ACTION_SENDTO`/`ACTION_VIEW` built through `Uri.fromParts`, never `ACTION_CALL`, with no `CALL_PHONE` permission, no `<queries>` element, and a caught `ActivityNotFoundException` that leaves the number on screen. Record nothing about opening the card or tapping a resource. Store the plan through additive Room 5→6 and JSON backup version 6; keep the records CSV, the clinician summary, and every share/copy action free of plan content; restore backup versions 3–6 with total rejection unchanged; and disclose exact plan counts in both the restore preview and the erase dialog. See `docs/specs/SPEC-safety-card.md`, D-1 through D-11.
- Reason: this is the first MindScale surface whose content can matter to someone in acute distress, so accuracy, ordering fidelity, and legibility under distress are the product, not the mechanics. A card the user cannot write into would be an inert control and would abandon the intervention's actual evidence base; a card gated or triggered by recorded data would make a measurement instrument look like a risk assessment it is not and must never be.
- Supersedes: none; resolves the Safety half of the backlog's Safety-and-off-ramp item.
- Recorded finding (2026-08-05): the "low-frequency off-ramp follow-on" half of that backlog item is already delivered and Phase 13 adds nothing to it. Phase 2 shipped the 40-entry/60-day check-in banner with `Still useful`/`Pause tracking` and the paused banner; Phase 4 shipped the `SettingsFocus.DATA` deep link that makes pause, export, and erase one screen. Adding a Safety link to either banner was considered and rejected: those are the only surfaces whose visibility depends on recorded data, so crisis resources inside them would read as an inference about the user.
