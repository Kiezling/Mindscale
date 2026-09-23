# MindScale spec backlog

This file contains only unstarted, non-urgent work. Active work belongs in `PROJECT_STATE.md`; approved implementation details belong in a `SPEC-*.md` file.

<!-- Add entries in this form:
- [ ] Short outcome — why it matters; dependencies or decision needed.
-->

The visual overhaul is no longer unscoped. It was authorized by `SPEC-full-log.md` D-9, which
deferred "the shared gold/ink light/dark token foundation until the later global brand phase",
and it now has an agreed outcome, an agreed dependency order, and frozen decisions. It runs as
four phases, 15 through 18:

- Phase 15 — tokens and chrome. Merged 2026-08-06, governed by
  `docs/specs/SPEC-visual-foundation.md`.
- Phase 16 — Track and Full Log. Merged 2026-08-06, governed by
  `docs/specs/SPEC-track-and-log-visual.md`.
- Phase 17 — Insights and the intensity ramp. Merged 2026-08-06, governed by
  `docs/specs/SPEC-insights-visual.md`, which is frozen. Its D-4 resolves the ramp: the
  prototype's warm interpolation and its `(v-1)/9` mapping are adopted, both low anchors are
  raised because the design's own measure 1.26:1 and 1.38:1 against `card`, and the light ramp
  keeps an ascending luminance direction because `IntensityRampTest` is a pre-existing JVM test
  that pins it. D-5 resolves the 0-versus-1 question and D-6 re-checks
  `SPEC-track-numpad-logging.md` Invariant 14 at all three fill sites.
- Phase 18 — Settings, Profile, Report, Safety, Breathing, and the closing audit. Merged
  2026-08-06, governed by `docs/specs/SPEC-remaining-screens-visual.md`, which is frozen. Its D-3
  corrects `SPEC-visual-foundation.md` D-22's L-5 row, which names Settings but describes a row
  that lives on Profile in MindScale; D-4 and D-5 verify L-6 at the component layer and correct a
  fifth Phase-15 component defect; D-14 turns the foundation's dimension-literal promise into a
  runnable source-scan test; and D-15 closes the type question `SPEC-insights-visual.md` D-19 left
  open, with the evidence that zero of these five screens need the data-label idiom. Active work
  is tracked in `PROJECT_STATE.md`.

Phases 15 through 18 each inherited the visual-only rule frozen as `SPEC-visual-foundation.md` D-1:
the phases change how the app looks and nothing about how it works. All four are merged, so that
rule has done its job and the user retired it on 2026-08-06. It does **not** extend to Phase 19.

Phase 19 readiness work is governed by the frozen `docs/specs/SPEC-release-readiness.md` and is
tracked in `PROJECT_STATE.md`; this backlog does not duplicate its active repairs or release gates.
The following work is explicitly deferred until after that readiness pass and a separately scoped
decision:

- [ ] Use the completed measurement in
  `docs/reviews/2026-09-17-performance.md` to decide whether long-history derivation cost warrants
  a separately specified optimization. Keep the existing bounded model and descriptive semantics
  unless a new spec freezes a change.
- [ ] Decide whether to correct the Settings `SETTINGS` header wrapping into `SETTING` and `S` at
  200% font. This is a cosmetic accessibility follow-up; preserve the current reachable privacy
  content and wait for a separately scoped visual decision.
- [ ] Revisit additional personal improvements beyond the 2026-09-17 walkthrough only when the
  user supplies them. The supplied simplification work is now active in `PROJECT_STATE.md`.
- [ ] Consider reintroducing onset tags only after a separate product decision about discoverable
  customization and entry capture. The simplification pass parks their UI and preserves stored
  chips/settings plus backup/import support; do not silently delete or reactivate them.
