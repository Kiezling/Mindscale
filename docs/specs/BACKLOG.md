# MindScale spec backlog

This file contains only unstarted, non-urgent work. Active work belongs in `PROJECT_STATE.md`; approved implementation details belong in a `SPEC-*.md` file.

<!-- Add entries in this form:
- [ ] Short outcome — why it matters; dependencies or decision needed.
-->

- [ ] Correct the Phase 1 note/backdate/edit dialog documentation and implementation for true process recreation. Their current `rememberSaveable` buffers survive Compose state restoration only when the owning dialog is recreated; the dialog-open state itself still lives solely in `TrackViewModel` and resets when the ViewModel is rebuilt. Use `SavedStateHandle` or an equivalent explicit restoration design in a dedicated, tested slice rather than repeating the earlier overclaim.
- [ ] Remaining Insights views after `SPEC-insights-entry-chart.md` — consider an onset-gap histogram, onset-time counts with sample refusal/caveat, and descriptive sleep counts under a separately approved bounded spec.
- [ ] Clinician one-pager and optional externally obtained PHQ-8/GAD-7 values — requires a separate Report/Profile schema, privacy, copy/file export, accessibility, and insight-grammar spec; the app must not administer or score the instruments.
- [ ] JSON/CSV import and restore — requires version validation, conflict/duplicate policy, atomic merge-or-replace behavior, rollback, and hostile-file limits before any file can mutate Room.
- [ ] Native Safety card and low-frequency off-ramp follow-on — preserve the Stanley-Brown ordering and local-only storage; requires explicit crisis-copy, phone-action, privacy, and accessibility decisions.
- [ ] Optional paced-breathing object — requires a session entity/export migration and a no-claims, never-auto-offered native pacer spec; no breath retention or fast-breathing mode.
