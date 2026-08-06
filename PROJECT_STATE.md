# MindScale project state

Last updated: 2026-08-06

## Goal

Build MindScale as a native Android application using Kotlin, Jetpack Compose, Material 3, and Kotlin DSL. MindScale is a local-only, event-contingent symptom tracker for depression/anxiety: a measurement instrument that assembles the user's own data without interpreting it.

The product source is the Claude Design project `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`: `SPEC.md` is the rationale and `MindScale v2.dc.html` is the visual/behavioral reference for Track, Full Log, Insights, Report, Safety card, Profile, and Settings. A local exported handoff is available at `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\`; its `MindScale v2.dc.html` is the primary implementation reference. Repository specs under `docs/specs/` govern native implementation after human approval.

## Current phase: Phase 16 Track and Full Log — spec frozen, implementation in progress

- Phase 16 branch: `agent/phase16-track-and-log`, created from synchronized `main` at `238b0ba25a9e5f040e3693ca70d4d91ef8b168e0`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `238b0ba25a9e5f040e3693ca70d4d91ef8b168e0` before branching
- Governing spec: `docs/specs/SPEC-track-and-log-visual.md` — `FROZEN — APPROVED`; D-1 through D-19 frozen on 2026-08-06 before any application-code edit
- Selected work: Phase 16 of 18 in the visual overhaul — apply the P15 foundation to Track's and Full Log's bodies, and to nothing else. Insights is Phase 17; Settings, Profile, Report, Safety, Breathing, and the closing audit are Phase 18
- Inherited rule: **the phase changes how the app looks and nothing about how it works.** Baselines to hold are 206/206 connected and 399/399 JVM, with no pre-existing test file modified. Checked mechanically with `git diff --name-status 238b0ba HEAD -- app/src/test app/src/androidTest`, where every line must be an `A`
- The phase's largest finding is arithmetic, not taste: eight of the design's own border values fail D-23's 3:1 non-text floor because almost every control here is drawn as transparent-with-a-hairline, so the hairline *is* the control's only boundary. The numpad key at rest measures 1.24:1 and the armed key ring 1.78:1 on `card`. Every ink boundary moves to the existing `outline` token (3.49:1 / 3.51:1) and every gold boundary to full `gold` (3.15:1). The pad wrapper's armed border already passed at 3.15:1 and is unchanged. Every figure is computed by `MsControlBoundaryContrastTest`, not estimated
- One earlier invariant is reinterpreted and flagged rather than smuggled: `SPEC-track-numpad-logging.md` Invariant 12's "visually distinct group" is satisfied by position rather than by shape and tone, because the mockup it appeals to draws all twelve keys from one `border-radius:50%` rule. Order and grouping are untouched
- Room stays at schema 7, the records CSV header stays byte-identical, and `IntensityRamp.kt` is byte-identical — Track simply stops calling `intensityColor`, which keeps three `InsightsScreen` callers, so Phase 17 still owns the ramp decision D-24 reserved for it
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope
- Exact next action: implement task 2 of the spec's decomposition — `MsControlBoundaryContrastTest` and the three D-15 component corrections — then proceed through tasks 3 to 8
- Explicitly not started: Phases 17 and 18

### Phase 15 merged checkpoint

- Phase 15 branch: `agent/phase15-visual-foundation`, created from synchronized `main` at `009f33455a8f79263f5a17de16d00f57ebec45d7`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `009f33455a8f79263f5a17de16d00f57ebec45d7` before branching
- Governing spec: `docs/specs/SPEC-visual-foundation.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-25 frozen on 2026-08-05 before application-code edits; frozen documentation commit `301b4ee`, verified implementation commits `5b38bf7` and `4d6dff4`
- Selected work: Phase 15 of 18 in the visual overhaul — the token foundation and the app chrome. Authorized by `SPEC-full-log.md` D-9, which deferred "the shared gold/ink light/dark token foundation until the later global brand phase"
- The governing rule, inherited by Phases 16 through 18: **the phases change how the app looks and nothing about how it works.** No feature added, removed, renamed, reordered, or rewired; no navigation change, no new entry point, no copy edit, no data/export/migration change, no dependency/permission/toolchain change. A broken test is evidence that behavior changed and is a defect in the phase, not a test to update
- Delivered: the complete palette from `MindScale v2.dc.html`; a four-level ink emphasis scale replacing the prototype's thirty alphas; all 47 Material 3 colour roles; all 15 text styles in bundled Instrument Sans; the shape scale passed to `MaterialTheme` for the first time; a spacing scale; a presentation-only component layer that owns no state and sets no testTag; and the chrome — flat three-cell header with the wordmark treatment and its gold rule, text-only bottom navigation, full-bleed Breathing overlay, and nine dialogs routed through one gold-bordered container
- The load-bearing technique: case is presentation, never semantics. `MsUppercaseText` renders `uppercase(Locale.ROOT)` and restores the original through `clearAndSetSemantics` — not `semantics`, because the `Text` key merges by appending and would leave both strings on the node. Proved by six connected tests before anything depended on it
- Three defects were found by building rather than by reasoning: twelve Material `…Fixed` colour roles still held Material's purple after the first pass; the Instrument Sans **variable** font would have silently collapsed the 400/500/600 hierarchy on API 26 and 27 because `fontVariationSettings` needs API 28 while `minSdk` is 26, so three static instances are bundled instead; and the `INSIGHTS` tab clipped its last glyph at 200% font, caught by installed-app capture and by no test
- Two frozen decisions were amended by measurement: light gold text is `#7D6539` rather than the first candidate `#886D3E`, which passes on `bg` and `card` and drops to 4.14:1 on the dimmest container; and light `onPrimary` is `bg` after the warm `onInk` measured 3.16:1
- Design reference now lives in the repository at `docs/design/reference/` as fourteen named screenshots, plus `docs/design/InstrumentSans-OFL.txt`. Its absence was a named cause of the drift this phase corrects
- Frozen constraints held: no Room schema, migration, backup, CSV, export, import, dependency, permission, manifest permission, or toolchain change. Room stays at schema 7 and the records CSV header is byte-identical. `IntensityRamp.kt` is untouched
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope
- Verification: 399/399 JVM tests (370 before this phase), lint with 0 errors and the unchanged 22-warning baseline, debug assembly, confirmed API 36 emulator, 206/206 connected tests (187 before this phase), `git diff --check`, `processReleaseManifest` confirming the debug gallery is absent from release, and installed-app capture in light, dark, and at 200% font
- The visual-only rule is proved objectively: `git diff --name-status 009f334 HEAD` over `app/src/test` and `app/src/androidTest` shows four additions and **zero modifications**
- Accepted consequence of the phasing: between this phase and Phase 18 the app is a correct foundation under unfinished rooms. The chrome is the design; the screen bodies still carry their pre-Phase-15 layout
- Honest gaps: spoken TalkBack output was not audited, only the semantics tree; the emphasis scale is proved by computation rather than by a human judging legibility on a real panel; Full Log, Insights, Settings, and Breathing were not individually captured because their chrome is the same chrome Track exercises and their bodies belong to later phases; the missing-font-resource fallback was not exercised because a bundled resource cannot go missing without a corrupt APK; and no critical-tier review pass was spent on this phase
- Verification-state commit: `4a43a56e9a9c47ad74cd1a863480aa7fbd7d370d`
- Phase 15 PR #12 was opened as draft, marked ready, and verified `CLEAN`/`MERGEABLE` at exact head `4a43a56e9a9c47ad74cd1a863480aa7fbd7d370d`; no remote status checks are configured
- PR #12 merged into `main` as `e22ee6b843445614f2aef5f62c727269469ce232` on 2026-08-06 by the user, after the agent's `gh pr merge` was declined by the harness permission classifier. Local `HEAD`, local `main`, `origin/main`, and live GitHub `refs/heads/main` all matched that merge before this phase-boundary documentation commit, and the verified head is an ancestor of it
- Exact next action after the phase boundary: freeze `docs/specs/SPEC-track-and-log-visual.md` for Phase 16 before any application-code edit. Done on 2026-08-06; see the Phase 16 section above
- Explicitly not started at the Phase 15 boundary: Phases 16, 17, and 18

### Phase 14 merged checkpoint

- Phase 14 branch: `agent/phase14-paced-breathing`, created from synchronized `main` at `82ca7041120df0903738645f1a6d46cec44a2fb6`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `82ca7041120df0903738645f1a6d46cec44a2fb6` before branching
- Governing spec: `docs/specs/SPEC-paced-breathing.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-14 frozen on 2026-08-05 before application-code edits; frozen documentation commit `0e71f16`, verified implementation commit `570a2fe`
- Selected work: the final scoped backlog item — an optional paced-breathing object with a session entity/export migration and a no-claims, never-auto-offered native pacer
- Reconciled boundary: one fixed-cadence pacing circle (4500 ms in, 6500 ms out, 11 000 ms cycle, 5.45 br/min) with exactly two phases and no configurability; 1/3/5/10 minute choices with nothing preselected; no claim of any kind anywhere; reachable only by a user tap on a Track link gated on the `breathingOn` setting and the paused state
- Non-inference guarantee, structural not conventional: `BreathingViewModel` takes `BreathingSessionDao` and `BreathingClock` and nothing else, `BreathingSessionDao` exposes only `insert` and `count` with no observing read, and a source scan plus a constructor-reflection test enforce that no recorded value can reach the screen. This mirrors Phase 13's `SafetyPlanDao`-only pattern
- Safety basis for the never-auto-offered rule: Schmidt 2000 found breathing retraining trending toward poorer outcomes in panic and Barlow discourages respiratory control as a safety behavior, so a pacer keyed to a rating is both an inference MindScale does not make and a plausible harm — not a matter of taste
- Evidence honestly reconciled: Fincham's 2023 meta-analysis had mostly-inactive controls, and the same team's two placebo-controlled trials were both null (coherent ~5.5 br/min vs 12 br/min placebo, n=400/4wk; high-ventilation vs 15 br/min placebo, n=200/3wk); STARS found fast equal to slow. The working hypothesis is paced attention interrupting rumination, not respiration. Nothing in the UI cites, quotes, or alludes to any of it
- Prototype reconciliation recorded rather than inherited: cadence, length set, toggle copy, and the instruction line are adopted verbatim; the completion-only session record, the 66-second "1 min" overshoot, the stored `minutes` field, and the always-plural running readout are each rejected with reasons in D-014
- Approval gate satisfied: the user granted full Phase 14 ownership and authorized decisions, implementation, verification, commits, pushes, PR readiness, merge, and final synchronization without another routine pause
- Frozen constraints: additive Room 6→7 and JSON backup v7 only; the records CSV header stays byte-identical and gains only a `breathing` row type accepted by both export and import; no dependency, permission, network, account, analytics, haptic, audio, notification, wake lock, background worker, destructive migration, or toolchain change; no Full Log, Insights, or clinician-summary surface; no UI-overhaul work
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope
- Verification: 370/370 JVM tests, lint with 0 errors and the unchanged 22-warning baseline, debug assembly, confirmed API 36 emulator, 187/187 connected tests, `git diff --check`, the full installed-app breathing matrix, and one critical review that returned APPROVE with no blocking finding
- One production defect was caught by installed-app inspection rather than by tests: the Settings data-section copy still said a records CSV contains "ratings, sleep, and marked events only", which became false once breathing sessions were written into it. Both that sentence and the matching import sentence are corrected and pinned by `SettingsImportScreenTest`
- The critical review returned APPROVE with three non-blocking findings, all closed rather than deferred: the banned-word scan now also covers the breathing disclosure strings outside `BreathingCopy`; the backup array's descending order is now explicit in D-9; and `discardSession()` replaces `leaveScreen()` on the erase path so a session running during an erase is dropped rather than written back into a just-cleared table
- Honest gaps: spoken TalkBack output was not audited, only semantic order, content descriptions, and the live region; a session in progress is lost on process death, disclosed on screen by `BreathingCopy.RECORDING`; backgrounding does not stop a running session, and whether the user was watching is deliberately not inferred; breathing sessions appear in no in-app view and have no per-session delete, so erase and replace-only restore are the removal paths; overlapping sessions are deliberately not rejected on import; the device cadence check confirmed alternating phases on an ~11-second cycle but `uiautomator` dump latency is too coarse to resolve the 4500/6500 ms boundaries, which are pinned by unit tests and by the exact stored intervals instead
- Verification-state commit: `f83da2dcdd0b371219accf56d4d3f0abe653380e`
- Phase 14 PR #11 was opened as draft, marked ready, and verified `CLEAN`/`MERGEABLE` at exact head `f83da2dcdd0b371219accf56d4d3f0abe653380e`; no remote status checks are configured
- PR #11 merged into `main` as `dbc3bc0a0575be1a625807e71be79510b5114be8` on 2026-08-05 (`2026-08-05T20:17:22Z`) using expected-head protection; local `HEAD`, local `main`, `origin/main`, and live GitHub `refs/heads/main` all matched that merge before this phase-boundary documentation commit, and the verified head is an ancestor of it
- `docs/specs/BACKLOG.md` now holds no unstarted scoped work. Every item the product source defined has been specified, implemented, verified, and merged
- Exact next action: leave the UI-overhaul work unstarted and unlisted until it is separately scoped, specified, and approved. It has no agreed outcome, dependency, or decision yet, so a backlog entry for it would be a placeholder rather than a task
- Explicitly not started: the later UI-overhaul phase

### Phase 13 merged checkpoint

- Phase 13 branch: `agent/phase13-safety-card`, created from synchronized `main` at `5eb10e497e328fb09dcf8254e7d5271c09cca1eb`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `5eb10e497e328fb09dcf8254e7d5271c09cca1eb` before branching
- Governing spec: `docs/specs/SPEC-safety-card.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-11 frozen on 2026-08-05 before application-code edits; frozen documentation commit `040403f`, verified implementation commit `bad6c8d`
- Selected work: the Safety half of the next ordered backlog item — a reachable Safety card with verified crisis resources and the user's own locally stored Stanley-Brown safety plan
- Reconciled boundary: the card is calm reference content plus the user's own writing. It is never triggered, ordered, filtered, or surfaced by any rating, episode, count, streak, or inference; `SafetyViewModel` depends on `SafetyPlanDao` alone so it structurally cannot read recorded data
- Crisis resources verified 2026-08-05 from operator and funding-agency sources, not from model memory: 988 Suicide & Crisis Lifeline (US) and 9-8-8 Suicide Crisis Helpline (Canada) by call and text, plus Find A Helpline for elsewhere. No country count is frozen into the copy because the directory's own pages disagree on it
- Region approach: MindScale has no network, no location permission, and no reliable country signal, so it shows one clearly labeled resource set with plain coverage wording and a static international directory pointer, and never infers a region
- Phone actions are `ACTION_DIAL`/`ACTION_SENDTO`/`ACTION_VIEW` hand-offs built with `Uri.fromParts`; `ACTION_CALL` is forbidden, no `CALL_PHONE` or other permission is added, and there is deliberately no emergency-number button
- Off-ramp finding: the "low-frequency off-ramp follow-on" clause is already delivered by Phase 2's check-in/paused banners and Phase 4's `SettingsFocus.DATA` one-screen pause/export/erase. Phase 13 adds nothing to it, and adding a Safety link to those data-conditioned banners was considered and rejected
- Approval gate satisfied: the user granted full Phase 13 ownership and authorized decisions, implementation, verification, commits, pushes, PR readiness, merge, and final synchronization without another routine pause
- Frozen constraints: additive Room 5→6 and JSON backup v6 only; records CSV, clinician summary, and every share/copy action stay free of plan content; no dependency, permission, network, account, analytics, destructive migration, or toolchain change; no paced-breathing or UI-overhaul work
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope
- Verification: 306/306 JVM tests, lint with 0 errors and the unchanged 22-warning baseline, debug assembly, confirmed API 36 emulator, 157/157 connected tests, `git diff --check`, the full installed-app Safety matrix, and one critical review that returned APPROVE with no blocking finding
- One defect was caught by installed-app inspection rather than by tests: the restore preview's "It contains …" line claimed a pre-version-6 backup held a safety plan. The clause is now version-conditional and pinned by `ImportPreflightTest`
- Three frozen-interface amendments are recorded under D-013: the corrected DAO ordering (SQL cannot express canonical step order), the `PlanFieldResult` return type, and the preview copy correction
- Honest gaps: spoken TalkBack output was not audited, only semantic order and live regions; no fuzzing campaign was run against version-6 plan payloads; the emergency-number sentence is copy only and was not exercised against a real emergency dialer by design; crisis-resource currency is a point-in-time fact checked on 2026-08-05 and surfaced in-app by `SafetyCopy.VERIFIED_ON`, so it needs periodic re-verification rather than being permanently correct
- Verification-state commit: `de950e47f03266671e9b2e191e96b6f1e4b6928d`
- Phase 13 PR #10 was opened as draft, marked ready, and verified `CLEAN`/`MERGEABLE` at exact head `de950e47f03266671e9b2e191e96b6f1e4b6928d`; no remote status checks are configured
- PR #10 merged into `main` as `c6f0989e5344f094eb1fe9d3e365c2df0cee3e4a` on 2026-08-05 (`2026-08-05T14:13:49Z`) using expected-head protection; local `HEAD`, local `main`, `origin/main`, and live GitHub `refs/heads/main` all matched that merge before this phase-boundary documentation commit, and the verified head is an ancestor of it
- Exact next action after the phase boundary: leave paced breathing and the UI-overhaul work unstarted until each is separately scoped, specified, and approved
- Explicitly not started: paced breathing and the later UI-overhaul phase

### Phase 12 merged checkpoint

- Phase 12 branch: `agent/phase12-import-restore`, created from synchronized `main` at `ce5913341461725c3ad59b697a4e994e501fa046`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `ce5913341461725c3ad59b697a4e994e501fa046` before branching
- Governing spec: `docs/specs/SPEC-import-restore.md` — `FROZEN — APPROVED`; D-1 through D-12 frozen on 2026-08-04 before application-code edits
- Selected work: the next ordered backlog item, safe local import and restore for MindScale's own JSON backup and records CSV formats
- Reconciled boundary: two never-blended actions — replace-only JSON restore for backup versions 3/4/5 with verbatim ids and disclosed defaults, and add-only records CSV import; total rejection of every malformed, ambiguous, duplicate, conflicting, oversized, unsupported, or future-version file; mandatory size limit, strict UTF-8, bounded hand-written parsers, preview, and explicit confirmation before one checked atomic Room transaction
- Approval gate satisfied: the user granted full Phase 12 ownership and authorized decisions, implementation, verification, commits, pushes, PR readiness, merge, and final synchronization without another routine pause
- Frozen constraints: no schema change, migration, downgrade path, destructive fallback, dependency, permission, network, account, analytics, or toolchain change; imported PHQ-8/GAD-7 totals retain fixed external provenance and are never scored or interpreted; raw untrusted file content never enters `SavedStateHandle`
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope
- Governing spec status: `IMPLEMENTED — VERIFIED LOCALLY`; frozen documentation commit `c6bb4d4`, verified implementation commit `9a670d9`
- Verification: 255/255 JVM tests, lint with 0 errors and the unchanged 22-warning baseline, debug assembly, confirmed API 36 emulator, 127/127 connected tests, `git diff --check`, the full installed-app import/restore matrix, and one critical review with its single blocking finding resolved
- Two defects were caught by installed-app inspection rather than by tests: microsecond `exportedAt` precision made MindScale reject its own export, and the preview dialog's fixed height cap hid the permanent-deletion sentence at 200% font. Both are fixed and pinned by `RealDeviceBackupTest` and the uncapped scrollable dialog
- Critical review blocked on a cancel-after-confirm race that could report a completed mutation as cancelled; an import is now non-cancellable once its transaction starts
- Honest gaps: the two canonical-row rollback checks are defensive and unexercisable from a valid database; at 200% font combined with landscape the preview's deletion counts require scrolling; a chip containing `|` remains ambiguous in the pre-existing records CSV format; no fuzzing campaign was run; spoken TalkBack output was not audited
- Verification-state commit: `2a6afddd06b09d7e367fa46ade1c5aa58ccd9bc1`
- Phase 12 PR #9 was opened as draft, marked ready, and verified `CLEAN`/`MERGEABLE` at exact head `2a6afddd06b09d7e367fa46ade1c5aa58ccd9bc1`; no remote status checks are configured
- PR #9 merged into `main` as `cbd29d0b3db2259f477a29fdf102c91ecf607d79` on 2026-08-05 (`2026-08-05T05:00:38Z`) using expected-head protection; local `HEAD`, local `main`, `origin/main`, and live GitHub `refs/heads/main` all matched that merge before this phase-boundary documentation commit
- Exact next action after the phase boundary: leave Safety, paced breathing, and the UI-overhaul work unstarted until each is separately scoped, specified, and approved
- Explicitly not started: Safety, paced breathing, and the later UI-overhaul phase

### Phase 11 merged checkpoint

- Phase 11 branch: `agent/phase11-clinician-report-profile`, created from synchronized `main` at `9d8cf4bd2dbd3834e2094c81f63e838d6348bdb5`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `9d8cf4bd2dbd3834e2094c81f63e838d6348bdb5` before branching
- Governing spec: `docs/specs/SPEC-clinician-report-profile.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-12 frozen on 2026-08-04 before application-code edits
- Reconciled boundary: local Profile/Report overlays; optional name; dated, explicitly external/user-entered PHQ-8 or GAD-7 totals; and a capped factual summary using existing Insights derivations without questionnaire administration, scoring, interpretation, comparison, diagnosis, or severity language
- Approval gate satisfied: the user granted full Phase 11 ownership and authorized decisions, implementation, verification, commits, pushes, PR readiness, merge, and final synchronization without another routine pause
- Persistence delivered: additive Room 4→5 plus JSON backup v5; records CSV compatibility; transactional snapshot/erase/reset; no import or destructive downgrade
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope
- Frozen documentation commit: `2019897655f9db5f143ef91c0707f16ba7c13cb8`; verified implementation commit: `cb469100b3b34fd3a8716428ff1448668af1cdcb`
- Verification: 180/180 JVM tests, lint with 0 errors/22 unchanged warnings, debug assembly, confirmed API 36 emulator, 111/111 connected tests, `git diff --check`, installed-app/manual export/reset matrix, and one critical review with all five blocking findings resolved
- Honest gaps: no actual share recipient was selected; provider-open/write failure was tested at the ViewModel boundary; TalkBack semantic order was tested but spoken audio was not manually audited
- Verification-state commit: `e020e3a18a8a4b2e552ede554986590499c52d7d`
- Phase 11 PR #8 was opened as draft, marked ready, and verified mergeable at exact head `e020e3a18a8a4b2e552ede554986590499c52d7d`; no remote status checks were configured
- PR #8 merged into `main` as `3dd139278bc3d190f720edd61112666f6129b564` on 2026-08-04; local `main`, `origin/main`, and live GitHub `refs/heads/main` matched that merge before this phase-boundary documentation commit
- Exact next action after the phase boundary: leave JSON/CSV import/restore, Safety, and paced breathing unstarted until separately scoped and approved

### Phase 10 merged checkpoint

- Phase 10 branch: `agent/phase10-insights-sleep-counts`, created from synchronized `main` at `652096b9cb61cdb5ff004ed352ce0f5a54d62e95`
- Starting synchronization: `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `652096b9cb61cdb5ff004ed352ce0f5a54d62e95` before branching
- Governing spec: `docs/specs/SPEC-insights-sleep-counts.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 frozen on 2026-08-04 before application-code edits
- Reconciled boundary: direct counts of completed normalized sleep periods grouped by recorded Wake, duration-only `night` (`>3h`) versus `nap` (`<=3h`), exact duration summaries, incomplete disclosure, and accessible/restorable category selection
- The prototype's five-hour post-wake comparison is excluded because it needs a separate coverage/cohort/minimum-sample contract and would exceed this non-inferential descriptive slice
- Approval gate satisfied: the user granted full Phase 10 ownership and authorized decisions, implementation, commits, pushes, PR readiness, merge, and synchronization without another review pause
- Room/schema/JSON version 4, CSV, backup, permissions, dependencies, toolchain, navigation, DI, privacy, and local-only architecture remain frozen
- Frozen documentation commit: `e6afe3280acccb1fd0849b26b5fac5b9e4699980`
- Verified implementation commit: `945300d0bad66be21de33b9494a19d393cf40aba`
- Verification-state commit: `e7e1e117df5b75564b533140654af1b0c47f451b`
- Phase 10 PR #7 was opened as draft, marked ready, verified mergeable at the exact verification-state head, and merged into `main` as `03bad992726bd336e43b0d5f17c064a07bb2bb57` on 2026-08-04
- Local `main` and `origin/main` fast-forwarded to the merge commit before this requested phase-boundary documentation commit
- Local verification: 168/168 JVM tests, lint with 0 errors/22 existing warnings, debug assembly, focused 27/27 connected tests, full 100/100 connected tests, installed-app manual inspection, critical review, and `git diff --check` passed
- Exact next action after the phase boundary: leave the remaining Report/Profile, import/restore, Safety, and breathing backlog unstarted until a separately scoped task is assigned

### Phase 9 publication checkpoint

- Phase 9 branch: `agent/phase9-onset-time-counts`, created from synchronized `main` at `76880a978e68e79348941de891a0bc6251986188`
- Before branch creation, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `76880a978e68e79348941de891a0bc6251986188`; the GitHub connector confirmed `Kiezling/Mindscale` is accessible with push/admin permission and default branch `main`
- Phase 9 spec: `docs/specs/SPEC-insights-onset-time-counts.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 were frozen on 2026-08-04 before application-code edits
- Reconciled boundary: one six-onset-minimum, 24 local-clock-hour count view derived from the existing Phase 5 episode model, with a deterministic four-hour count sentence, exact denominators, recording-displacement/current-zone caveats, and accessible primitive selection
- Selection rationale: onset-time counts are the next product-ordered Insights view and need no new source/cohort rules; descriptive sleep counts remain deferred because their night/nap, boundary, incomplete-sleep, and post-wake semantics require a separate spec
- Approval gate satisfied: the user granted full ownership and authorized decisions, implementation, commits, pushes, PR readiness, merge, and synchronization without another review pause
- Frozen documentation commit: `1846598`; verified implementation commit: `338a4fe`
- Verification-state commit: `ecb98d6c30d8a66efde80bfde97837400ef2f33f`
- Phase 9 PR #6 was opened as draft, marked ready, and merged into `main` as `6e000bd3126b79aa930357c14b7987917e7b9268` on 2026-08-04 (`2026-08-04T20:10:15Z`)
- Local `main` and `origin/main` were synchronized at the merge commit before this requested phase-boundary documentation commit
- Exact next action after the phase boundary: freeze a separate bounded spec before any descriptive sleep-count implementation
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope

### Phase 8 publication checkpoint

- Phase 8 branch: `agent/phase8-onset-gap-histogram`, created from synchronized `main` at `86bd313e47515bd81eded5e03796195a3b389bff`
- Phase 8 PR #5 merged into `main` as `042376265cb801a4d8e80375b3eb7fff54c1d0aa` on 2026-08-04 (`2026-08-04T18:48:52Z`)
- Local `main`, local `origin/main`, and live GitHub `refs/heads/main` are synchronized at `042376265cb801a4d8e80375b3eb7fff54c1d0aa`
- Phase 8 spec: `docs/specs/SPEC-insights-onset-gap-histogram.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 were frozen on 2026-08-04 before application-code edits
- Phase 8 reconciled boundary: one six-onset-minimum, ten-bucket, elapsed onset-to-onset gap histogram derived from the existing Phase 5 episode model; onset-time and sleep views remain deferred
- Approval gate satisfied: the user reiterated full project ownership and authorized all decisions, implementation, commits, pushes, and PRs without further approval
- Phase 8 implementation commit: `d2d546686d6ced87e98eea5c2e73ed66ee41138e`; complete local JVM, lint, build, connected, and installed-app verification passed
- Expected untracked paths remain `.agents/` and `.codex/`; they are excluded from product/documentation scope

### Prior phase publication

- Branch: `agent/phase7-track-dialog-restoration`, created from synchronized `main` at `372d1d3d91f7308ad35b8824c996bce641b0ce57`
- Local `main`, its tracking ref, and live `origin/main` were verified at `372d1d3d91f7308ad35b8824c996bce641b0ce57` before the branch was created
- Phase 7 spec: `docs/specs/SPEC-track-dialog-restoration.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 frozen on 2026-08-04
- Phase 7 implementation commit: `8d8d1a401d1d4a193985f8377c3ff9db09a3ec3c`
- Phase 7 PR #4: merged into `main` as `afeec04d3ca48a9574446ccda24484ed8bf5ed56` on 2026-08-04 (`2026-08-04T17:29:20Z`)
- Phase 4 final branch head: `93fac65711e8e3eeb72cf6f1406d386f0386e9da`
- Phase 4 PR #1: merged into `main` as `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5` on 2026-08-03 local time (`2026-08-04T03:37:50Z`)
- Phase 5 spec: `docs/specs/SPEC-insights-foundation.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-10 frozen
- Phase 5 implementation commit: `cd3a1cf51adb468f968a78a2a04d85ce317d76ed`, pushed to `origin/agent/phase5-insights-foundation` on 2026-08-04
- Phase 5/6 integration PR #3: merged into `main` as `fee0caf6dd1da2a3ecb9e04da1bab454ca009836` on 2026-08-04 (`2026-08-04T14:10:15Z`)
- Phase 6 branch: `agent/phase6-entry-chart`, merged into `agent/phase5-insights-foundation` by PR #2 as `3d92a2fd5805b102c2a6b0ef01f5d47931361c56`
- Phase 6 spec: `docs/specs/SPEC-insights-entry-chart.md` — `IMPLEMENTED — VERIFIED LOCALLY`; D-1 through D-8 frozen
- Phase 6 implementation commit: `53f9d54ea79f56b2d069297f4d6a4b8923aeae23`, pushed to `origin/agent/phase6-entry-chart` on 2026-08-04
- Phase 1 spec: `docs/specs/SPEC-track-numpad-logging.md` — `IMPLEMENTED`
- Phase 2 spec: `docs/specs/SPEC-track-phase2-completeness.md` — `IMPLEMENTED`
- Phase 3 spec: `docs/specs/SPEC-full-log.md` — `IMPLEMENTED` on 2026-08-03
- Phase 4 spec: `docs/specs/SPEC-settings-data-control.md` — `IMPLEMENTED` on 2026-08-03/04; D-1 through D-9 remain frozen
- Phase 7 publication scope: the implementation, focused unit/instrumented/UI tests, and governing documentation on `agent/phase7-track-dialog-restoration`. The pre-existing untracked `.agents/` and `.codex/` tooling directories remain excluded.

## Environment

- Project: `S:\Android\AndroidProjects\MindScale`
- SDK: `S:\Android\Sdk`
- AVD: `S:\Android\Avd\MindScale_API_36.avd`
- Gradle user home: `S:\Android\Gradle`
- JDK: Android Studio Quail 2 bundled JDK 21.0.10
- Android Studio: Quail 2 (2026.1.2)
- Application ID: `com.kieslingdev.mindscale`
- Minimum SDK 26; target SDK 36; compile SDK 36.1

## Completed work

### Environment and baseline

- Installed and verified stable Android Studio, API 36 SDK/toolchain, Git, native Claude Code, and the official Claude JetBrains plugin.
- Created and booted the S:-based Pixel 9 API 36 Google Play AVD; verified hardware acceleration, adb, installation, and foreground launch.
- Generated the Empty Activity Compose starter and committed the verified baseline as `673d6c7`.
- Removed superseded C:-based Android Studio/SDK/AVD/caches only after replacement verification and approval.
- Added provider-neutral `AGENTS.md`, provider adapters, durable project state/decision/failure records, Android specs, and tested Claude hooks.

### Phase 1 — Track numpad logging

- Implemented Room `Entry` storage, 0–10 tap logging, long-press backdating, band/intensity helpers, recent list, and edit/note/delete in native Compose.
- Used unidirectional `StateFlow`, manual DI, stable Compose identity, and no repository/DI framework.
- Critical review found and fixed stale recent-row overwrites and incomplete note-buffer restoration claims.
- Committed as `7258999` (`Implement Phase 1 track numpad logging`).

### Phase 2 — Track completeness

- Added additive Room migration v1→v2: `entries.kind`, sleep intervals, markers, and canonical track settings.
- Added transactional Sleep/Wake capture with real concurrency coverage, timestamp-relative onset-chip logic, marker capture, help card, paused/check-in banners, badges, chips, and settings persistence.
- Critical review fixed a stale onset-chip full-row overwrite and marker restoration. Independent Codex review then completed true marker process restoration with `SavedStateHandle` and a focused recreation test.
- Re-ran all Android oracles independently on the API 36 emulator and reviewed the exact 37-file scope.
- Committed and pushed as `4cf9f068d42a0c906b76ddac460d4b5e907410d3` (`Implement Phase 2 track completeness`).

### Phase 3 — Full Log

- Added a native Track/Log bottom navigation shell without adding a navigation framework; Track remains the launch destination and Back from Log returns to Track.
- Added a process-scoped `AppContainer`/`MindScaleApplication` so Track and Log use one Room database without adding a DI framework or repository layer.
- Added a reactive, mixed Full Log of all ratings, sleep intervals, and markers, grouped by local day and ordered deterministically newest-first.
- Added inclusive local-date From/To filtering with DST-safe half-open epoch bounds, All reset, calm empty/error states, and a working Retry path.
- Added inline targeted rating edit/note behavior plus isolated, confirmed deletion for all three record types. Mutation failures retain retryable UI; stale row ids close safely with a concise message.
- Refactored Track's edit/note/delete path onto the same targeted DAO mutations, eliminating stale full-row overwrites without changing Track behavior.
- Critical review corrected a terminal read-error collector, action semantics swallowed by a merged row, filtered-empty wording, and mutation UI closing before Room confirmed success.
- Kept Room at schema version 2; no migration, new toolchain dependency, paging library, navigation library, or DI framework was introduced.
- Committed and pushed as `87eb817713a459fd231ce77169dfa522b249f641` (`Implement Phase 3 full log`).

### Phase 4 — Settings, anchors, and data control

- Added additive Room migration 2→3, exported schema 3, appearance/time enums, anchors, custom onset vocabulary, note-preview privacy, and one-shot anchor-prompt state.
- Replaced production full-row settings writes with targeted mutations and added a transactional guard that refuses to hide Sleep/Wake while an interval is open.
- Added a reachable Settings overlay that returns to Track or Log, deep links from the paused/anchor cards, and state restoration for destination, focus, and drafts.
- Applied the gold/ink Light/Dark/System theme globally and removed dynamic wallpaper colors; 12/24-hour mode now reaches Track feedback/history and Full Log.
- Added deterministic version-3 JSON backup and RFC-4180 CSV export through `CreateDocument`, with no storage permission, plus export-first atomic erase/reset.
- Critical review fixed an untappable status-bar-overlapped Settings action, retained encoded exports for write retry, and made word de-duplication locale-independent.

### Phase 5 — Insights foundation

- Added a third native Insights destination with six local-calendar ranges, restoration, Back-to-Track behavior, and Settings return-to-Insights behavior without a navigation framework.
- Added a pure deterministic episode engine over Entry/Sleep facts: same-timestamp collapse, explicit/assumed/ongoing endings, awake-time holds, sleep union, clipped range duration/AUC, clear-day eligibility, local/DST-safe raster projection, and scheduled time invalidation.
- Replaced production ordinary-capture onset detection with a hold/sleep-aware Room transaction that snapshots settings, classifies at the capture timestamp, inserts the rating atomically, and suppresses prompting safely if classification/settings are unavailable.
- Added additive Room 3→4 hold persistence with 8/12/16/24 waking-hour choices, schema 4, targeted writes, fresh/default erase reset, and deterministic JSON backup version 4 `holdHours`; CSV remains unchanged.
- Added native Material 3 summary/fact/episode UI and a Canvas day/hour raster with text legend, future hatching, touch/horizontal-drag exploration, visible live readout, TalkBack custom actions, stable date keys, and explicit 48 dp range/choice targets.
- Critical review and manual API 36 inspection fixed carried-duration leakage, future sleep-end invalidation, raster vertical-scroll interception, low-contrast future state, duplicate ongoing copy, and undersized range targets.

### Phase 6 — Insights entry chart

- Added a native step-only `What you recorded` chart driven by the Phase 5 effective-state engine, with 0/5/10 axes, shared ranges, sleep gaps/bands, event lines, and no interpolation or chart dependency.
- Expanded the single reactive Room UNION projection with Entry note and Marker text while keeping Room/schema/backup at version 4 and preserving transactional onset classification.
- Added independent chart selection restoration, touch and horizontal-drag exploration, event snapping, visible/live source-rating readout, note-preview privacy, and six TalkBack navigation actions.
- Added pure engine, ViewModel, Room, Compose, gesture-ownership, semantics, and privacy coverage; final manual inspection covered touch readout, event discovery, light/dark rendering, and 150% font scaling.

### Phase 8 — Insights onset-gap histogram

- Added one native `Days between onsets` section derived from the existing Phase 5 episode snapshot, with a fixed six-onset threshold and exact 0–5 sparse refusal copy.
- Added ten fixed lower-inclusive/upper-exclusive elapsed-day buckets from `<1d` through `14+d`, exact denominators and selected-boundary readouts, and strict cycle/cause/prediction disclaimers.
- Added horizontally reachable, individually selectable native Compose cells with visible count/label geometry, redundant border selection, 48 dp-plus semantics, a polite live readout, and parent vertical-scroll ownership.
- Added primitive validated `SavedStateHandle` selection, range/ineligibility clearing, and retention across eligible re-derivation without changing the existing single Room/settings Flow or immutable snapshot publication.
- Kept Room/schema/JSON at version 4 and left CSV, backup, permissions, dependencies, toolchain, manual DI, navigation shell, and local-only architecture unchanged.
- Expanded pure engine, ViewModel, and Compose coverage for every range, 0–5 refusal arithmetic, all ten exact boundaries, fractional/DST/extreme gaps, selection restoration/refresh, semantics, horizontal reachability, and vertical gesture ownership.

### Phase 9 — Insights onset-time counts

- Added one native `Time of day it started` section derived from the existing Phase 5 episode onsets, with a fixed six-start threshold, exact 0–5 refusal copy, and a covered-local-calendar-day denominator.
- Added 24 current-zone local-clock buckets, deterministic DST overlap/gap handling, and a wrapping four-hour maximum-count sentence with earliest numeric-hour tie-breaking and exact half-open boundaries.
- Added horizontally reachable, individually selectable native Compose cells with visible count/hour geometry, redundant border selection, at-least-48-dp targets, exact spoken semantics, a polite live readout, and parent vertical-scroll ownership.
- Added validated primitive `SavedStateHandle` hour restoration, range/ineligibility clearing, and eligible refresh retention without changing the existing single Room/settings Flow or immutable snapshot publication.
- Kept Room/schema/JSON at version 4 and left CSV, backup, permissions, dependencies, toolchain, navigation, DI, and local-only architecture unchanged; descriptive sleep counts remain deferred.
- Added pure engine, copy, ViewModel, Room regression, and Compose coverage for all six ranges, exact hour/midnight/range boundaries, current-zone reprojection, DST overlap/gap, covered-day arithmetic, window wrap/ties, hold/sleep/marker invariance, selection lifecycle, accessibility, hour-23 reachability, and vertical gesture ownership.

## Active blocker

No active blocker. Phase 16's spec is frozen and implementation is in progress. Phases 17 and 18
are scoped in `docs/specs/BACKLOG.md` and each needs its own spec frozen before any
application-code edit.

One environment constraint, not a repository problem: the harness permission classifier declined
`gh pr merge` and `gh pr view` during Phase 15. Branch, push, and PR creation all worked. Expect
to hand the merge to the user, or to have the permission granted, at each future phase boundary.

## Known decisions

- Preserve the template-generated Gradle/AGP/Kotlin/Compose compatibility set; do not independently change toolchain versions.
- Use the Gradle wrapper and Android Studio bundled JDK; never install or use system Gradle.
- Keep SDK, AVDs, projects, and Gradle caches on S:; keep Android Studio and normal configuration on C:.
- Keep production signing and publishing out of scope until explicitly requested.
- Use `AGENTS.md` as the provider-neutral contract and `CLAUDE.md` only as the Claude adapter.
- Reserve scarce models for specs, irreversible architecture decisions, adjudication, and critical review; use cheaper tiers for exploration and routine implementation.
- Preserve local-only storage and never add interpretation, accounts, servers, ads, social features, streaks, or gamification.

## Next tasks

1. Implement `docs/specs/SPEC-track-and-log-visual.md` through its task decomposition, then verify with all four Gradle oracles and installed-app capture. It inherits the visual-only rule and must hold 206/206 connected and 399/399 JVM with no pre-existing test file modified.
2. Re-verify the crisis resources in `SafetyCopy` against their operator sources before any future release, and update `SafetyCopy.VERIFIED_ON` in the same edit. Hotline numbers and coverage change; a stale number in a safety feature is a real harm, not a cosmetic bug.
3. Continue excluding `.agents/` and `.codex/` from product/documentation commits.

## Last verification

Phase 16 starting-state verification completed 2026-08-06 before application-code edits:

- `git status --short --branch` showed synchronized `main` with only the expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `238b0ba25a9e5f040e3693ca70d4d91ef8b168e0`.
- Branch `agent/phase16-track-and-log` was created from that exact head.
- `AGENTS.md`, `PROJECT_STATE.md`, all `FAILED_PATHS.md` headings, `docs/DECISIONS.md` D-001 through D-015, `docs/specs/BACKLOG.md`, and `docs/specs/SPEC-visual-foundation.md` in full were reconciled before drafting, along with `SPEC-track-numpad-logging.md`'s invariant list — which is where the Invariant 12 reconciliation recorded in D-016 came from.
- The design authority was read at its exact anchors rather than impressionistically: `keyBase` (1204), `order` (1205), `padWrapStyle` (1218), `toggleBase` (1222), `chipBtn` (1223), `dotStyle` (1240), `numS` (1276), `sleepStyle`/`wakeStyle` (1719-1720), the Track markup (60-250), and the Log markup (255-292).
- The current screens were measured so the acceptance criteria are checkable: `TrackScreen.kt` 1089 lines, `LogScreen.kt` 453 lines, 51 `testTag` call sites across the two, 60 `.dp` literals in Track and 26 in Log, the toast still a full-width `inverseSurface` `Surface`, the numpad still rounded squares and pills with no armed state rendered, and the entry-row dot still filled with `intensityColor`.
- Every connected test that touches either screen was read in full before any restructuring: `TrackScreenTest` (24 tests), `LogScreenTest` (7), `TrackDialogSavedStateTest`, `NavigationTest`, `MindScaleChromeTest`, and `MsUppercaseTextTest`. Four fragile shapes were identified and are frozen as D-16 of the new spec.
- The eight failing control-boundary contrast values in D-4 were computed by `MsControlBoundaryContrastTest` rather than estimated, and the two the user flagged were reproduced to the decimal — `rgba(ink,.10)` at 1.24:1 and `rgba(gold,.55)` at 1.78:1 on `card`. Hand arithmetic had put the first at 1.23:1; the test's 8-bit float channels are the authority and the spec's tables were corrected to them.
- No Gradle oracle was rerun for this documentation-only freeze; the merged Phase 15 application tree and its verification evidence below remain unchanged.

Phase 15 publication checkpoint completed 2026-08-06:

- Pushed `agent/phase15-visual-foundation` through verification-state commit `4a43a56e9a9c47ad74cd1a863480aa7fbd7d370d`.
- GitHub PR #12 was opened as draft, marked ready, and confirmed `CLEAN`/`MERGEABLE` at that exact head with no status checks configured.
- The agent's `gh pr merge --match-head-commit` was declined by the harness permission classifier, so the merge was performed by the user. This is an environment constraint, not a repository or authentication problem; `gh` itself worked for create, ready, and view.
- PR #12 merged into `main` as `e22ee6b843445614f2aef5f62c727269469ce232`.
- Local `main` fast-forwarded from `009f33455a8f79263f5a17de16d00f57ebec45d7` to `e22ee6b843445614f2aef5f62c727269469ce232`; `HEAD`, local `main`, `origin/main`, and live `git ls-remote origin refs/heads/main` all matched before this phase-boundary documentation commit, and `git merge-base --is-ancestor` confirmed the verified head is an ancestor of the merge.
- No Android oracle was rerun for the GitHub merge or this documentation-only checkpoint because the application/test tree is the exact verified PR head plus GitHub's merge commit.
- Expected untracked `.agents/` and `.codex/` remain excluded; no other local change was present before this documentation update.

Phase 15 final local verification completed 2026-08-05 for implementation commits `5b38bf7` and `4d6dff4` on `agent/phase15-visual-foundation`:

- `test`: 399/399 JVM tests passed; 0 failures, errors, or skips (370 before this phase; the 29 new ones are the colour-scheme, contrast, and typography token tests).
- `lint`: passed with 0 errors and the same 22 existing warnings. Three new `UnusedAttribute` warnings appeared and were removed at the source rather than suppressed, by replacing the variable font with static instances.
- `assembleDebug`: passed. `processReleaseManifest` confirmed `DesignGalleryActivity` and `design_gallery_name` are absent from the release manifest while `MainActivity` is present.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- `connectedDebugAndroidTest`: 206/206 passed; 0 failures or skips (187 before this phase; the 19 new ones are `MsUppercaseTextTest` and `MindScaleChromeTest`).
- `git diff --check`: passed with only the configured LF-to-CRLF notices.
- The visual-only rule was checked mechanically, not by inspection: `git diff --name-status 009f334 HEAD -- app/src/androidTest app/src/test` returned four `A` lines and zero `M` lines.
- Installed-app inspection covered: Track in light and dark at 100% font, compared against `docs/design/reference/light-track-top.png` and `dark-track-top.png`; the header rendering `MINDSCALE` / `TRACK` with its gold rule / `PROFILE` in tracked uppercase Instrument Sans, flat on the bone page; the text-only bottom navigation with `TRACK` in gold at weight 600 above a hairline; both launcher icons present on the debug install and only `MainActivity` in the release manifest; the design gallery rendering every token, the emphasis scale, all fifteen text styles, the shape scale, and every component in both themes; and Track at 200% font. Emulator font scale, night mode, and auto-rotate were restored to `1.0`, `no`, and enabled, and the app's data was cleared.
- One production defect was found by installed-app inspection rather than by tests: the `INSIGHTS` bottom-navigation tab clipped its final glyph at the screen edge at 200% font, because a single-line label is wider than a third of the screen at that scale. The label now wraps, which is not pretty at 200% but loses nothing.
- Two calibration results amended frozen decisions before implementation depended on them: light gold text moved from `#886D3E` to `#7D6539` because the first value drops to 4.14:1 on `surfaceContainerHighest`, and light `onPrimary` became `bg` because the warm `onInk` measured 3.16:1 on gold. Both rejections are pinned by tests.
- No critical-tier review pass was spent on this phase. The three token tests, the two chrome test classes, and the unchanged 206/399 baselines were treated as the objective evidence instead; this is recorded as a gap rather than a claim of equivalence.
- No manifest permission, dependency, Room schema, migration, exported schema JSON, backup, CSV, or toolchain file changed.

Phase 15 starting-state verification completed 2026-08-05 before application-code edits:

- `git status --short --branch` showed synchronized `main` with the expected untracked `.agents/` and `.codex/` directories plus the user-supplied `Screenshots from Claude Design/` directory, which was moved into `docs/design/reference/` as the phase's first task.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `009f33455a8f79263f5a17de16d00f57ebec45d7`.
- Branch `agent/phase15-visual-foundation` was created from that exact head.
- `AGENTS.md`, `PROJECT_STATE.md`, all `FAILED_PATHS.md` headings, `docs/DECISIONS.md` D-001 through D-014, `docs/specs/BACKLOG.md`, and `docs/specs/SPEC-full-log.md` D-9 — the decision that authorized this phase — were reconciled before drafting.
- The design authority `MindScale v2.dc.html` was inventoried programmatically rather than read impressionistically: 23 distinct font sizes, 13 letter-spacings, 9 radii, 3 weights, 30 ink alphas, 18 literal hexes, 12 gold rgba values, and 5 shadows, each traced to its source line.
- The current app was measured so the acceptance criteria were checkable: 13 of the colour roles set, 1 of 15 text styles overridden and naming `FontFamily.Default`, `shapes` never passed to `MaterialTheme`, 246 hardcoded dimension literals, 158 testTag call sites, 111 `onNodeWithText` assertions across exactly 8 of 23 connected test files, 286 `onNodeWithTag`, 4 `onNodeWithContentDescription`, 9 `AlertDialog` call sites, one `ToastBanner`, no `Snackbar`, and `ui-tooling` already on the classpath and unused.
- No Gradle oracle was rerun for the documentation-only freeze; the merged Phase 14 application tree and its verification evidence below remain unchanged.

Phase 14 publication checkpoint completed 2026-08-05:

- Pushed `agent/phase14-paced-breathing` through verification-state commit `f83da2dcdd0b371219accf56d4d3f0abe653380e`.
- GitHub PR #11 was opened as draft, marked ready, and confirmed `CLEAN`/`MERGEABLE` at that exact head.
- PR #11 merged into `main` as `dbc3bc0a0575be1a625807e71be79510b5114be8` with expected-head protection.
- Local `main` fast-forwarded from `82ca7041120df0903738645f1a6d46cec44a2fb6` to `dbc3bc0a0575be1a625807e71be79510b5114be8`; `HEAD`, local `main`, `origin/main`, and live `git ls-remote origin refs/heads/main` all matched before this phase-boundary documentation commit, and the verified head is an ancestor of the merge.
- No Android oracle was rerun for the GitHub merge or this documentation-only checkpoint because the application/test tree is the exact verified PR head plus GitHub's merge commit.
- Expected untracked `.agents/` and `.codex/` remain excluded; no other local change was present before this documentation update.

Phase 14 final local verification completed 2026-08-05 for implementation commit `570a2fe` plus the critical-review follow-up on `agent/phase14-paced-breathing`:

- `test`: 370/370 JVM tests passed across 35 suites; 0 failures, errors, or skips (306 before this phase).
- `lint`: passed with 0 errors and the same 22 existing warnings. No new warning was introduced or suppressed.
- `assembleDebug`: passed.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- `connectedDebugAndroidTest`: 187/187 passed; 0 failures or skips (157 before this phase).
- `git diff --check`: passed with only the configured LF-to-CRLF notices.
- Installed-app inspection covered: the Track link rendering above the Safety link with the frozen copy and content description; every on-screen string on the pacer compared against D-12; a full session at all four offered lengths, whose exported CSV intervals were exactly 60.000 s, 180.000 s, 300.000 s, and 600.000 s; the cue alternating In/Out on an approximately 11-second cycle; the static chosen-length readout rather than a countdown; logging a 10 and then a 9 and confirming no pacer, prompt, dialog, or breathing node appeared anywhere; the Settings toggle hiding the link when off and the paused state hiding it while the unconditional Safety link stayed; dark mode at 200% font and landscape at 200% font both reflowing with nothing clipped; the semantic reading order with the circle contributing no node at all; a version-7 backup containing `breathingOn` and a newest-first `breathingSessions` array; the erase dialog disclosing "4 breathing sessions"; a version-5 restore disclosing "This backup predates paced breathing" with the exact count; a version-7 restore returning every session with its id verbatim and re-exporting identically; a records CSV whose four breathing rows were rejected in full as already present; a non-canonical instant rejected by the strict CSV validator; and an additive import adding 4 breathing sessions after an erase. Emulator font scale, night mode, and rotation were restored to `1.0`, `no`, and automatic, the six export files created during inspection were deleted, and the app's data was cleared.
- One production defect was found by installed-app inspection rather than by tests: the Settings data-section sentence still read "Records CSV contains ratings, sleep, and marked events only", which became false the moment breathing sessions were written into that file, and the matching import sentence had the same omission. Both are corrected and pinned by `SettingsImportScreenTest`.
- Critical review returned APPROVE with no blocking finding. All three non-blocking findings were closed rather than deferred: the banned-word scan was extended to the breathing disclosure strings that live outside `BreathingCopy`; D-9's `breathingSessions` ordering is now explicitly descending; and `discardSession()` was added so a session running when the user erases everything is dropped instead of being written back into a table the erase transaction had already cleared.
- No manifest, permission, dependency, or toolchain file changed. Room advanced additively 6→7 with exported schema 7, and the records CSV header is byte-identical.

Phase 14 starting-state verification completed 2026-08-05 before application-code edits:

- `git status --short --branch` showed synchronized `main` with only the expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `82ca7041120df0903738645f1a6d46cec44a2fb6`.
- Branch `agent/phase14-paced-breathing` was created from that exact head.
- `AGENTS.md`, `CLAUDE.md`, `PROJECT_STATE.md`, all `FAILED_PATHS.md` headings including the two 2026-08-05 environment entries, `docs/DECISIONS.md` D-001 through D-013, `docs/specs/BACKLOG.md`, and `docs/specs/SPEC-safety-card.md` were reconciled as the closest precedent for a non-inferential, never-auto-triggered object with its own persistence and export contract.
- The product handoff's `SPEC.md` "Breathing" section was reread in full, and the prototype's complete breathing implementation was read at `MindScale v2.dc.html` lines 167–171 (Track entry), 714–735 (overlay markup), 761–762 (`BREATH`/`LENS`), 1052–1067 (`startBreathe`), 1075 and 1084 (JSON/CSV export rows), 1640 (settings toggle), and 1665 (erase reset), then reconciled against MindScale's measurement-only, non-inferential identity rather than carried forward.
- The live `MindScaleApp` destination stack, `TrackScreen` footer and paused gating, `TrackUiState`, `TrackSettings`/`TrackSettingsDao`, `MindScaleDatabase` schema 6, `Migrations`, `DataControlDao`, `ImportPayloads`, `ImportModels`, `DataExport`, `BackupImport`, `ImportPreflight`, `RecordsCsvImport`, `SafetyViewModel`, `NoAutoDialSourceTest`, and `MainActivity` were read before freezing D-014.
- No Gradle oracle was rerun for this documentation-only freeze; the merged Phase 13 application tree and its verification evidence below remain unchanged.

Phase 13 publication checkpoint completed 2026-08-05:

- Pushed `agent/phase13-safety-card` through verification-state commit `de950e47f03266671e9b2e191e96b6f1e4b6928d`.
- GitHub PR #10 was opened as draft, marked ready, and confirmed `CLEAN`/`MERGEABLE` at that exact head.
- PR #10 merged into `main` as `c6f0989e5344f094eb1fe9d3e365c2df0cee3e4a` with expected-head protection.
- Local `main` fast-forwarded from `5eb10e497e328fb09dcf8254e7d5271c09cca1eb` to `c6f0989e5344f094eb1fe9d3e365c2df0cee3e4a`; `HEAD`, local `main`, `origin/main`, and live `git ls-remote origin refs/heads/main` all matched before this phase-boundary documentation commit, and the verified head is an ancestor of the merge.
- No Android oracle was rerun for the GitHub merge or this documentation-only checkpoint because the application/test tree is the exact verified PR head plus GitHub's merge commit.
- Expected untracked `.agents/` and `.codex/` remain excluded; no other local change was present before this documentation update.

Phase 13 final local verification completed 2026-08-05 for implementation commit `bad6c8d` on `agent/phase13-safety-card`:

- `test`: 306/306 JVM tests passed across 25 suites; 0 failures, errors, or skips.
- `lint`: passed with 0 errors and the same 22 existing warnings. One new `UseKtx` warning was removed at the source by using `String.toUri()` instead of `Uri.parse` for the directory link, rather than suppressed.
- `assembleDebug`: passed.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- `connectedDebugAndroidTest`: 157/157 passed; 0 failures or skips.
- `git diff --check`: passed.
- Installed-app inspection covered: reaching Safety from the Track footer link and from the Profile row and returning with Back; every on-screen string compared against the frozen spec; `Call 988` opening the dialer pre-filled at 988 with `dumpsys telecom` confirming no call was placed; `Text 988` opening Messages with no body; the directory button handing off to Chrome; adding a contact with a number and its `Call Sam` pre-filling 555-0100; dark mode at 200% font and landscape at 200% font both reflowing with nothing clipped; a version-6 backup exported containing the plan verbatim; the erase dialog disclosing "your safety plan (1 line)"; and a version-5 restore disclosing then producing an empty plan. Emulator font scale, night mode, and rotation were restored to `1.0`, `no`, and automatic, and the two backup files created during inspection were deleted.
- Two production defects were found by installed-app inspection rather than by tests: the restore preview claimed a pre-version-6 backup contained a safety plan, and the deletion sentence read redundantly as "N safety plan lines" where "safety plan" was already the subject. Both are fixed and pinned by `ImportPreflightTest`.
- Critical review returned APPROVE with no blocking finding. Its one non-blocking note — that 48 dp width relied on a Material3 default rather than an assertion — was closed by adding explicit `assertWidthIsAtLeast` coverage for the narrow Add/Edit/Delete controls.
- No manifest, permission, dependency, or toolchain file changed. Room advanced additively 5→6 with exported schema 6; the records CSV format is byte-identical.

Phase 13 starting-state verification completed 2026-08-05 before application-code edits:

- `git status --short --branch` showed synchronized `main` with only the expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live `git fetch`-refreshed `origin/main` all resolved to `5eb10e497e328fb09dcf8254e7d5271c09cca1eb`.
- Branch `agent/phase13-safety-card` was created from that exact head.
- `AGENTS.md`, `CLAUDE.md`, `PROJECT_STATE.md`, all `FAILED_PATHS.md` headings, `docs/DECISIONS.md` D-001 through D-012, `docs/specs/BACKLOG.md`, and the product handoff's `SPEC.md` and `MindScale v2.dc.html` Safety/check-in/paused/Profile markup were reconciled, along with the live `MindScaleApp` destination stack, `TrackScreen` paused and check-in banners, `TrackSettings`/`TrackSettingsDao`, `ProfileScreen`, `MindScaleDatabase` schema 5, `DataControlDao`, `ImportPayloads`, `DataExport`, `BackupImport`, `ImportModels`, and `ImportPreflight`, before freezing D-013.
- Every crisis number, service name, and coverage claim was verified on 2026-08-05 from `988lifeline.org`, `988lifeline.org/help-yourself/deaf-hard-of-hearing/`, `samhsa.gov/find-help/988`, `988.ca`, `camh.ca`, `crtc.gc.ca`, and `findahelpline.com` rather than carried forward from model memory.
- No Gradle oracle was rerun for this documentation-only freeze; the merged Phase 12 application tree and its verification evidence below remain unchanged.

Phase 12 publication checkpoint completed 2026-08-05:

- Pushed `agent/phase12-import-restore` through verification-state commit `2a6afddd06b09d7e367fa46ade1c5aa58ccd9bc1`.
- GitHub PR #9 was opened as draft, marked ready, and confirmed `CLEAN`/`MERGEABLE` at that exact head.
- PR #9 merged into `main` as `cbd29d0b3db2259f477a29fdf102c91ecf607d79` with expected-head protection.
- Local `main` fast-forwarded from `ce5913341461725c3ad59b697a4e994e501fa046` to `cbd29d0b3db2259f477a29fdf102c91ecf607d79`; `HEAD`, local `main`, `origin/main`, and live `git ls-remote origin refs/heads/main` all matched before this phase-boundary documentation commit, and the verified head is an ancestor of the merge.
- No Android oracle was rerun for the GitHub merge or this documentation-only checkpoint because the application/test tree is the exact verified PR head plus GitHub's merge commit.
- Expected untracked `.agents/` and `.codex/` remain excluded; no other local change was present before this documentation update.

Phase 12 final local verification completed 2026-08-04 for implementation commit `9a670d9` on `agent/phase12-import-restore`:

- `test`: 255/255 JVM tests passed across 21 suites; 0 failures, errors, or skips.
- `lint`: passed with 0 errors and the same 22 existing warnings. The two new `Recycle` warnings were removed by a narrow documented suppression at the launcher callbacks, where the stream is opened lazily and always closed on the ViewModel's IO context.
- `assembleDebug`: passed.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- `connectedDebugAndroidTest`: 127/127 passed; 0 failures or skips.
- `git diff --check`: passed.
- Installed-app inspection covered export, export-then-erase, restore into an empty install with the exact frozen preview and confirmed settings/Profile/external-total restoration, CSV add, duplicate and conflict rejection with exact counts, oversized/malformed/empty/version-2/version-6 rejection, cancel before confirmation, export-after-import with verbatim restored ids, light and dark, 200% font, landscape, true process death, and erase after import. Emulator font scale, rotation, orientation, and night mode were restored and the pushed fixtures were deleted.
- Critical review found one blocking issue — cancel after confirm could misreport a completed mutation — which is fixed and covered; the review's two documentation amendments are recorded under D-012.
- No Room schema, migration, exported schema JSON, manifest/permission, dependency, or toolchain file changed.

Phase 12 starting-state verification completed 2026-08-04 before application-code edits:

- `git status --short --branch` showed synchronized `main` with only the expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `ce5913341461725c3ad59b697a4e994e501fa046`.
- Branch `agent/phase12-import-restore` was created from that exact head.
- `AGENTS.md`, `CLAUDE.md`, `PROJECT_STATE.md`, all `FAILED_PATHS.md` headings, `docs/DECISIONS.md`, `docs/specs/BACKLOG.md`, and the Phase 4 and Phase 11 data/export specs were reconciled, along with the live Room schema-5 entities, converters, migrations, `DataControlDao`, `DataExport.kt` encoders, `SettingsViewModel`/`SettingsScreen` Activity Result handling, `MainActivity` wiring, and the exported schema JSONs, before freezing D-012.
- No Gradle oracle was rerun for this documentation-only freeze; the merged Phase 11 application tree and its verification evidence below remain unchanged.

Phase 10 publication checkpoint completed 2026-08-04:

- Pushed `agent/phase10-insights-sleep-counts` through verification-state commit `e7e1e117df5b75564b533140654af1b0c47f451b`.
- GitHub PR #7 was opened as draft, marked ready, and confirmed mergeable at that exact head.
- PR #7 merged into `main` as `03bad992726bd336e43b0d5f17c064a07bb2bb57`.
- Local `main` fast-forwarded from `652096b9cb61cdb5ff004ed352ce0f5a54d62e95` to `03bad992726bd336e43b0d5f17c064a07bb2bb57`; local `origin/main` matched before this phase-boundary documentation commit.
- No Android oracle was rerun for the GitHub merge or documentation-only checkpoint because the application/test tree is the exact verified PR head plus GitHub's merge commit.
- Expected untracked `.agents/` and `.codex/` remain excluded; no other local change was present before this documentation update.

Phase 10 final local verification completed 2026-08-04 for implementation commit `945300d0bad66be21de33b9494a19d393cf40aba` on `agent/phase10-insights-sleep-counts`:

- `test`: 168/168 JVM tests passed; 0 failures, errors, or skips across 12 suites.
- `lint`: passed with 0 errors and the same 22 existing warnings.
- `assembleDebug`: passed.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- Focused Room/Insights connected run passed 27/27 after final copy and zero-category coverage.
- `connectedDebugAndroidTest`: 100/100 passed with 0 failures or skips.
- Installed-app inspection verified no-completed/open and mixed eligible states, exact-three-hour nap/four-hour night, selection/readout, incomplete disclosure, vertical reachability, light/dark, 12/24-hour behavior, 150% font, rotation/configuration restoration, and native content descriptions/targets. Font, theme, rotation, and time format were restored; the temporary seed source/test package were removed.
- Critical review corrected future-Wake disclosure grammar and found no remaining blocking cohort/range, DST/zone, incomplete-data, arithmetic, accessibility, restoration, concurrency, privacy, persistence, or rollback issue.
- Practical manual DST/zone reprojection and listened-to TalkBack speech remain coverage gaps; exhaustive pure tests plus Compose semantics/activation/live-region tests and device UI hierarchy cover those contracts.
- `git diff --check` passed with only configured LF-to-CRLF notices. No schema, backup/CSV, manifest/permission, dependency/toolchain, navigation/DI, WebView, server/account/analytics, or background-work file changed.

Phase 10 starting-state verification completed 2026-08-04 before application-code edits:

- `git status --short --branch` showed synchronized `main` with only expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live GitHub `refs/heads/main` all resolved to `652096b9cb61cdb5ff004ed352ce0f5a54d62e95`.
- Branch `agent/phase10-insights-sleep-counts` was created from that exact head.
- Product handoff `SPEC.md` and `MindScale v2.dc.html`, all existing Insights specs, backlog, decisions, state, and relevant failed paths were reconciled before freezing D-010.
- No Gradle oracle was rerun for the documentation-only freeze; the merged Phase 9 application tree and its verification evidence below remain unchanged.

Phase 9 publication checkpoint completed 2026-08-04:

- Pushed `agent/phase9-onset-time-counts` through verification-state commit `ecb98d6c30d8a66efde80bfde97837400ef2f33f`.
- GitHub PR #6 was opened as draft, marked ready, verified `CLEAN`/`MERGEABLE` at that exact head, and merged with the guarded expected-head SHA.
- PR #6 merged into `main` as `6e000bd3126b79aa930357c14b7987917e7b9268` at `2026-08-04T20:10:15Z`.
- Local `main` fast-forwarded from `76880a9` to `6e000bd`; no Android oracle was rerun for the merge or this documentation-only checkpoint because the application tree is the exact verified PR head plus GitHub's merge commit.
- Expected untracked `.agents/` and `.codex/` remain excluded; no other local change was present.

Phase 9 final local verification completed 2026-08-04 for implementation commit `338a4fe` on `agent/phase9-onset-time-counts`:

- `test`: 158/158 JVM tests passed; 0 failures, 0 errors, 0 skipped across 12 suites.
- `lint`: passed with 0 errors and the existing 22 warnings.
- `assembleDebug`: passed.
- `adb devices -l`: intended `MindScale_API_36` API 36 emulator connected as `emulator-5554`.
- `connectedDebugAndroidTest`: 95/95 passed; 0 failures, errors, or skips. The focused Insights screen/DAO run passed 22/22 first.
- Installed-app inspection covered the exact one-start refusal, the exactly-six-start eligible state, denominator, four-hour sentence, selection/live readout, all 24 semantics, hour-23 reachability and selection, parent vertical scrolling, light/dark rendering, 150% font scaling, and landscape scrolling. Device font, theme, and rotation were restored to `1.0`, `no`, and automatic portrait defaults.
- Critical review covered source/range attribution, zone/DST semantics, hold/sleep reuse, four-hour wrap/tie arithmetic, grammar, accessibility, restoration, single-snapshot concurrency, privacy, unchanged version-4 persistence, and rollback. No blocking finding remained.
- `git diff --check` passed. No Room/schema, JSON/CSV, backup, manifest/permission, dependency/toolchain, WebView/JavaScript, sample-data, account/server/analytics, or background-work file changed.
- No reusable failed implementation approach was introduced; `FAILED_PATHS.md` remains unchanged. Expected untracked `.agents/` and `.codex/` remain excluded.

Phase 9 starting-state verification completed 2026-08-04 before application-code edits:

- `git status --short --branch` showed synchronized `main` with only expected untracked `.agents/` and `.codex/`.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `76880a978e68e79348941de891a0bc6251986188`.
- GitHub repository metadata confirmed public repository `Kiezling/Mindscale`, default branch `main`, and connector push/admin access.
- Branch `agent/phase9-onset-time-counts` was created from that exact head.
- Product rationale, the complete primary HTML handoff, the three governing Insights specs, current engine/ViewModel/Compose/DAO code, and focused JVM/instrumented tests were reconciled before drafting.
- No Gradle oracle was rerun for this documentation-only approval state; the merged Phase 8 application evidence below remains current.

Repository synchronization verified 2026-08-04 after Phase 8 publication:

- GitHub PR #5 merged `agent/phase8-onset-gap-histogram` into `main` as `042376265cb801a4d8e80375b3eb7fff54c1d0aa` at `2026-08-04T18:48:52Z`.
- Local `main`, `origin/main`, and live `refs/heads/main` all resolved to `042376265cb801a4d8e80375b3eb7fff54c1d0aa` before this documentation-only merge checkpoint.
- Phase 8 implementation commit `d2d546686d6ced87e98eea5c2e73ed66ee41138e` and verification-state commit `f63ae4d2ab1fbc4d8f8d8c9c79da86019d48de32` are ancestors of the merge.
- No Android oracle was rerun for the merge or this documentation-only checkpoint; the complete Phase 8 verification evidence below applies to the merged application tree.

Phase 8 final local verification completed 2026-08-04 for implementation commit `d2d546686d6ced87e98eea5c2e73ed66ee41138e` on `agent/phase8-onset-gap-histogram`:

- `test`: 147/147 JVM tests passed; 0 failures, 0 errors, 0 skipped across 12 suites.
- `lint`: passed with 0 errors and the existing 22 warnings.
- `assembleDebug`: passed.
- `adb devices -l`: `emulator-5554`, `MindScale_API_36` (API 36).
- `connectedDebugAndroidTest`: 91/91 passed; 0 failures or skips. The focused 14-test Insights screen class also passed after adding horizontal reachability coverage.
- Installed-app API 36 inspection used eight local episode starts and verified the eligible histogram, exact denominator and selected readout, caveat, parent vertical scrolling, horizontal reachability through `14+d`, light/dark rendering, and 150% font scaling. Emulator font scale and night mode were restored to `1.0` and `no`.
- Critical review covered range attribution, episode-model reuse, exact bucket arithmetic, DST/extremes, refusal grammar, accessibility, selection lifecycle, single-snapshot concurrency, and unchanged persistence/architecture. It added explicit 0–5 refusal, all-six-range, hold-limit, and horizontal reachability assertions; the full oracles then passed.
- Two recurring connected-suite harness races were resolved without production changes: close the restored edit-dialog IME before Back, and join destroyed Track ViewModel scopes before closing the in-memory Room database. `FAILED_PATHS.md` records the signals and remedies.
- `git diff --check` passed. No Room/schema, JSON/CSV, backup, manifest/permission, dependency/toolchain, WebView/JavaScript, account/server/analytics, or background-work file changed.

Phase 8 starting-state verification completed 2026-08-04 before specification edits:

- `git status --short --branch` showed synchronized `main` with only the expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `86bd313e47515bd81eded5e03796195a3b389bff`.
- A sandboxed `git fetch origin --prune` could not update `.git/FETCH_HEAD` because the sandbox exposes `.git` read-only, but the independent live `git ls-remote` check succeeded and matched every local ref.
- Branch `agent/phase8-onset-gap-histogram` was created from that exact head.
- No Gradle oracle was rerun because this approval-gated step changes documentation only; the merged Phase 7 application tree and its verification evidence below remain unchanged.

Repository synchronization verified 2026-08-04 after Phase 7 publication:

- GitHub PR #4 merged `agent/phase7-track-dialog-restoration` into `main` as `afeec04d3ca48a9574446ccda24484ed8bf5ed56`.
- Local `main`, `origin/main`, and live `refs/heads/main` were synchronized to `afeec04d3ca48a9574446ccda24484ed8bf5ed56` before this documentation-only merge checkpoint.
- Phase 7 implementation commit `8d8d1a401d1d4a193985f8377c3ff9db09a3ec3c` and publication-state commit `27a47b88f282d2d77944e9a3c3225b1420e85392` are ancestors of the merge.
- No Android oracle was rerun for the merge and documentation-only synchronization; the complete Phase 7 verification evidence below remains current for the merged application tree.

Phase 7 final local verification completed 2026-08-04 from `S:\Android\AndroidProjects\MindScale` on `agent/phase7-track-dialog-restoration`, based on synchronized `main` commit `372d1d3d91f7308ad35b8824c996bce641b0ce57`:

- `test`: 136/136 JVM tests passed; focused Track ViewModel suite: 61/61.
- `lint`: passed with 0 errors and 22 existing warnings.
- `assembleDebug`: passed.
- `adb devices -l`: `emulator-5554`, `MindScale_API_36` (API 36).
- `connectedDebugAndroidTest`: 88/88 passed with 0 failures/errors/skips. Coverage includes Backdate/Edit/Note Activity recreation, primitive Bundle restoration into a new owner/ViewModelStore, Room id observation outside recent ten, stale/conflict/failure paths, large-font/live-region UI, and all Phase 1–6 regressions.
- Manual installed-app process walkthrough proved all three exact drafts survive true background `am kill` reconstruction through Recents with a changed PID: Backdate `2026-0`/`1`, Edit `2026-0`/`2`, and multiline Note `Phase7 manual\nrestored`.
- Visual inspection on the API 36 emulator confirmed dialog labels, validation, focus, scrolling, and reachable actions with the IME open. Font scale remained `1.0`, night mode `no`, and automatic rotation enabled after the walkthrough.
- `git diff --check` passed with only Windows line-ending notices. No schema, backup/CSV, manifest/permission, dependency, or toolchain file changed.
- Two connected-test-only issues were corrected during verification: asynchronous row/dialog sequencing in the compound navigation test and teardown ordering in the saved-state Room harness. Neither involved a production behavior change or merits a `FAILED_PATHS.md` entry.

Phase 7 starting-state verification completed 2026-08-04 before documentation edits:

- `git status --short --branch` showed synchronized `main` with only the expected untracked `.agents/` and `.codex/` directories.
- `HEAD`, local `main`, local `origin/main`, and live `git ls-remote origin refs/heads/main` all resolved to `372d1d3d91f7308ad35b8824c996bce641b0ce57`.
- Merge commit `fee0caf6dd1da2a3ecb9e04da1bab454ca009836` is an ancestor of `372d1d3d91f7308ad35b8824c996bce641b0ce57`.
- Branch `agent/phase7-track-dialog-restoration` was created from that exact head. No Gradle oracle was rerun because this approval-gated step changes documentation only.
- Documentation checks passed: `git diff --check`, a trailing-whitespace scan of the untracked draft spec, and a required-section check. Application/test-source diff remained empty.

Phase 6 final local verification completed 2026-08-04 from `S:\Android\AndroidProjects\MindScale` for implementation commit `53f9d54ea79f56b2d069297f4d6a4b8923aeae23`, based on Phase 5 publication head `c8a7bdb17a12aba58dba59927ffc81506d5b42dc`:

```powershell
.\gradlew.bat test lint assembleDebug --no-daemon --console=plain
adb devices -l
.\gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain
git diff --check
```

Results:

- JVM tests: 122/122 passed; 0 failures, 0 errors, 0 skipped.
- Lint: passed with 0 errors and the same 22 existing template/dependency/toolchain warnings; no Phase 6 warning remains.
- Debug assembly: passed; no dependency, toolchain, manifest, permission, or schema JSON change was made.
- Intended device: `emulator-5554`, `MindScale_API_36` (API 36).
- Instrumented/UI tests: 80/80 passed; 0 failures, 0 errors, 0 skipped. Coverage includes the unified Entry/Sleep/Marker source, transactional onset with markers present, chart touch/horizontal drag, vertical scroll ownership, note privacy, event text, all six semantic actions' ViewModel targets, and all Phase 1–5 regressions.
- Manual installed-app review used local UI-created records and verified the 30-day compressed step geometry, persistent source/event readout, event-line snapping, scroll into episode facts, light mode, dark mode, and 150% font scaling. Emulator `font_scale` and night mode were restored to `1.0` and `no`.
- Final review found no blocking product, persistence, concurrency, accessibility, grammar, or architecture issue. A stale off-screen heading assertion and an over-broad drag-test start were corrected; the focused 11-test Insights class and final 80-test suite then passed.
- `git diff --check` passed with only Windows line-ending notices.

Phase 5 final local verification completed 2026-08-04 from `S:\Android\AndroidProjects\MindScale` at uncommitted branch head/base `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5`:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Results:

- JVM tests: 116/116 passed; 0 failures.
- Lint and debug assembly: passed; no toolchain/dependency change was made.
- Intended device: `emulator-5554`, `MindScale_API_36` (API 36).
- Instrumented/UI tests: 77/77 passed; 0 failures, 0 errors, 0 skipped. Coverage includes Room 1→4/2→4/3→4, settings/default erase, unified source/transactional onset, all Phase 1–4 regressions, navigation/recreation, Insights states/ranges/semantics, 48 dp range targets, vertical raster scroll, and 12/24-hour readout.
- Manual installed-app walkthrough verified Track/Log/Insights navigation, empty and populated Insights, touch readout, vertical scrolling past the raster, fact/episode cards, hold setting, Settings return, light/dark output, 150% font scaling, and TalkBack-enabled focus/navigation. Connected tests invoke every custom raster accessibility action; no WebView, JavaScript/chart dependency, new permission, account, server, analytics, or background worker was added.
- `git diff --check` passed with only Windows line-ending notices. The emulator-only test records/screenshots are disposable build evidence and are not repository source artifacts.
- One interrupted Gradle run left a truncated generated test result and caused a raw `EOFException`; `cleanTestDebugUnitTest` recovered it. The durable workaround is in `FAILED_PATHS.md`.
- Critical review completed and all findings were resolved; see the checked Phase 5 acceptance criteria.

Repository/PR synchronization verified 2026-08-04 after Phases 5-6 publication:

- GitHub PR #2 merged Phase 6 head `7bc414f4aa04a717247e612f8170d610b22bea47` into `agent/phase5-insights-foundation` as `3d92a2fd5805b102c2a6b0ef01f5d47931361c56`.
- GitHub PR #3 merged the combined Phase 5/6 branch into `main` as `fee0caf6dd1da2a3ecb9e04da1bab454ca009836` on 2026-08-04 (`2026-08-04T14:10:15Z`).
- `git switch main` and `git pull --ff-only origin main` succeeded; local `main`, `origin/main`, and `origin/HEAD` are synchronized at `fee0caf6dd1da2a3ecb9e04da1bab454ca009836`.
- Phase 6 publication head `7bc414f4aa04a717247e612f8170d610b22bea47` is an ancestor of updated `main`.
- No Gradle oracle was rerun for this synchronization and documentation-only handoff update; the Phase 6 verification evidence above remains current for the merged application tree.

Repository/PR synchronization verified 2026-08-03 before Phase 5 drafting:

- GitHub PR #1 is closed and merged; head `93fac65711e8e3eeb72cf6f1406d386f0386e9da`, merge commit `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5`.
- `git fetch --prune origin` and `git pull --ff-only origin main` succeeded.
- Local `main` and `origin/main` were identical (`0 0`) at `c177db20ae5cf7c6ef41dd526b1cb70a5d19baa5`, and Phase 4 head was confirmed as an ancestor.
- Phase 5 branch `agent/phase5-insights-foundation` was created from that merge commit before documentation edits.
- No Gradle oracle was rerun for the documentation-only spec draft; the latest application evidence remains the Phase 4 suite below.

Phase 4 final pre-publication verification completed 2026-08-03/04 from `S:\Android\AndroidProjects\MindScale`:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Results:

- Unit tests: 89/89 passed; 0 failures, 0 errors, 0 skipped.
- Lint: passed with 0 errors and 22 existing template/dependency/toolchain warnings; the two Phase 4 modifier-order warnings found during review were fixed.
- Debug assembly: passed.
- Intended device: `emulator-5554`, `MindScale_API_36`.
- Instrumented tests: 59/59 passed with 0 failures and 0 skipped, including Room 1→3/2→3 migration, settings mutations/open-sleep guard, snapshot/erase, Phase 1–3 regressions, Settings navigation, and recreation.
- Diff check: passed; only harmless line-ending notices were emitted.
- Manual installed-app walkthrough confirmed physical Settings touch after status-bar inset correction, all upper/lower sections, real JSON and CSV writes through DocumentsUI, neutral picker cancellation, and the export-first erase confirmation. The destructive confirmation was canceled.
- Emulator Downloads contains three harmless empty-state verification artifacts: two version-3 JSON backups and one CSV records file.
- Phase 4 implementation is `ea30fdeaa0dfe3987fd5cb71201756ccbe72092d` on `agent/phase4-settings-data-control`; draft PR #1 targets Phase 3 checkpoint `87eb817713a459fd231ce77169dfa522b249f641` on `main`.

## Known coverage gaps and backlog

- Phase 1 backdate/edit/note dialog-open restoration is implemented and verified by Phase 7; see `docs/specs/SPEC-track-dialog-restoration.md`.
- JSON/CSV import and restore is implemented and merged by Phase 12; see `docs/specs/SPEC-import-restore.md` for its frozen semantics and honest coverage gaps. Saved backups are now restorable inputs rather than one-way export evidence.
- The time-weighted/hold episode model and Phase 2 onset reconciliation are implemented and verified by `docs/specs/SPEC-insights-foundation.md`.
- Gold/ink theming is implemented; approved typography assets remain a future design decision.
- Full Log import/export and clinician-report data actions remain deferred; no inert controls are shown.
- The optional paced-breathing object is implemented by Phase 14; see `docs/specs/SPEC-paced-breathing.md`. Sessions are exported facts only: they appear in the JSON backup, the records CSV, the restore preview, and the erase dialog, and deliberately in no in-app view, with no per-session delete. A future Full Log row type for them would need its own edit, delete, and filter contract, and any Insights view of them is rejected outright rather than deferred, because a chart placing sessions beside intensity would manufacture the causal reading two placebo-controlled trials failed to support.
- The Safety card and the user's own Stanley-Brown safety plan are implemented and merged by Phase 13; see `docs/specs/SPEC-safety-card.md`. Its crisis resources were verified on 2026-08-05 and carry a visible in-app check date, so currency is a maintenance obligation rather than a settled fact.

## Handoff checklist

Before a pause, compaction, or provider switch, update:

- branch and current commit;
- dirty files and what each contains;
- governing spec/task and exact next action;
- last oracle commands/results and coverage gaps;
- blocker or approval gate;
- new decisions and failed paths.
