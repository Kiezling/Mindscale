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
- Phase 16 — Track and Full Log. Active on 2026-08-06, governed by
  `docs/specs/SPEC-track-and-log-visual.md`; `PROJECT_STATE.md` owns it while it is in progress.
- [ ] Phase 17 — Insights and the intensity ramp — apply the foundation to the Insights
      surfaces and move `intensityColor` onto the prototype's single warm interpolation.
      Depends on Phase 15. Needs its own spec, which must decide the 0-versus-1 low-anchor
      question explicitly and re-check `SPEC-track-numpad-logging.md` Invariant 14, because the
      warm low end sits very close to the card surface.
- [ ] Phase 18 — Settings, Profile, Report, Safety, Breathing, and the closing audit — apply the
      foundation to the remaining surfaces and audit every screen against
      `docs/design/reference/`. Depends on Phases 15 through 17.

Every phase inherits the visual-only rule frozen as `SPEC-visual-foundation.md` D-1: the phases
change how the app looks and nothing about how it works. Each needs its own spec frozen before
any application-code edit.
