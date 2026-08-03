# MindScale spec backlog

This file contains only unstarted, non-urgent work. Active work belongs in `PROJECT_STATE.md`; approved implementation details belong in a `SPEC-*.md` file.

<!-- Add entries in this form:
- [ ] Short outcome — why it matters; dependencies or decision needed.
-->

- [ ] Behavioral-anchor definitions (SPEC.md's "DRIFT FIX": user writes what a 2/5/8 means, shown back at logging time, prompted once around entry ~15) — deliberately excluded from `SPEC-track-phase2-completeness.md` (D non-goal); needs its own Settings storage/UI, likely bundled with Phase 4.
- [ ] Paused-banner "Export or delete" action — deferred from `SPEC-track-phase2-completeness.md` D-6; needs Settings-screen data actions (export/erase), which is Phase 4 work.
- [ ] Settings-screen UI to toggle `sleepOn`/`askChips`/custom onset-chip vocabulary — the persisted settings exist as of Phase 2 (`TrackSettings`) but nothing lets a user change them yet; Phase 4.
- [ ] Time-weighted/hold/auto-end episode model consuming `SleepInterval` rows (SPEC.md's "Statistics" section: AUC, onset-to-onset intervals, the "an entry ends after N awake hours" rule) — Phase 2 only stores raw sleep intervals; this is Insights-phase (later) work, and Phase 2's `isOnset` simplification (D-3) should be reconciled against it once it exists.
- [ ] Correct the Phase 1 note/backdate/edit dialog documentation and implementation for true process recreation. Their current `rememberSaveable` buffers survive Compose state restoration only when the owning dialog is recreated; the dialog-open state itself still lives solely in `TrackViewModel` and resets when the ViewModel is rebuilt. Use `SavedStateHandle` or an equivalent explicit restoration design in a dedicated, tested slice rather than repeating the earlier overclaim.
