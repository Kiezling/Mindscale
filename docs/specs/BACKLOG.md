# MindScale spec backlog

This file contains only unstarted, non-urgent work. Active work belongs in `PROJECT_STATE.md`; approved implementation details belong in a `SPEC-*.md` file.

<!-- Add entries in this form:
- [ ] Short outcome — why it matters; dependencies or decision needed.
-->

- [ ] JSON/CSV import and restore — requires version validation, conflict/duplicate policy, atomic merge-or-replace behavior, rollback, and hostile-file limits before any file can mutate Room.
- [ ] Native Safety card and low-frequency off-ramp follow-on — preserve the Stanley-Brown ordering and local-only storage; requires explicit crisis-copy, phone-action, privacy, and accessibility decisions.
- [ ] Optional paced-breathing object — requires a session entity/export migration and a no-claims, never-auto-offered native pacer spec; no breath retention or fast-breathing mode.
