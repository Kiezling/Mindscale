# MindScale layout progress — 2026-09-20

Implemented in the existing dirty checkout on `codex/release-readiness`, HEAD `937ab49f169cfccffec94bf640fe99b14246ba51`:

- Removed the empty readout/help row and padded divider above Track's numpad. Transient readout remains when present.
- Moved help toggle below numpad and enabled Sleep/Wake controls, aligned right; expanded help follows it.
- Changed Mark an event into an outlined pill button, selected while its input is open.
- Added Recent Logs heading to Track history.
- Moved shared note formatting toolbar above its text field, preserving editor state logic.
- Centered MindScale branding on Track, Log and Insights, retaining destination labels and Profile. Increased root wordmark font size 12%; overlays retain previous size.

Changed source files (relative to `app/src/main/java/com/kieslingdev/mindscale/`): `track/TrackScreen.kt`, `MindScaleApp.kt`, `ui/components/MsText.kt`, `ui/components/RichNoteEditor.kt`. Original working copies are saved in this preparation task's `work/before-layout/`; they include prior uncommitted changes. Do not rerun `work/layout-fixes.ps1` against already-modified files.

Verification: `./gradlew.bat test lint assembleDebug --console=plain` succeeded in 1m30s. All 480 JVM tests passed, no failures/errors/skips. Build log: task `work/layout-build.log`. Source diffs inspected against pre-edit working copies. `adb devices -l` returned no connected devices; no visual emulator check or connected tests performed. UI work is implemented but NOT visually signed off. No phone installation, commit, push, publication or data changes.

Remaining: verify these layouts at normal and 200% fonts in Light/Dark on isolated API36 emulator; note deletion; Log row parity/event editing; all Insights/chart/distribution/report changes from the main handoff. The broad handoff's original “implementation NOT started” references are historical and superseded by this progress note. Next implementation should begin note deletion and Log editing; do not repeat the completed layout patch. A successor must use the same saved MindScale checkout directly after user resets/resumes.
