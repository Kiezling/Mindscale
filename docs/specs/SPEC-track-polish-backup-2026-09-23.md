# Track polish and backup shortcuts — 2026-09-23

Status: IMPLEMENTED — VERIFIED LOCALLY. Evidence: `docs/reviews/2026-09-23-track-polish.md`. User authorizes implementation, commit of all accumulated project work, and in-place phone update after verification. No push, publication, toolchain/signing change or personal-data deletion is authorized.

## Scope and frozen interfaces

1. Expand Track help with simple accurate copy: tap Sleep, then a rating when going to sleep; tap Wake, then a rating on waking. These mark a sleep interval; tapping the selected button again cancels the selection. Preserve capture behavior.
2. Center Recent Logs and modestly increase its existing text size using an existing typography token. Make Mark an event visually smaller via local padding/type treatment while retaining button semantics and at least 48dp hit area.
3. Shared RichNoteEditor keeps its toolbar above the text, centered as a group. Add a visible themed border/padding around the text section alone. Preserve all input/composition/selection/formatting logic and existing test tags. This consistently covers Track popup and Log note editing.
4. For Track zero ratings with an Ended badge, remove the redundant plain Ended band label; keep the outlined badge, value, timestamp, actions and accessible meaning. In Log, which uses a band label without an Ended badge, remove its redundant ended metadata instead. Other band labels and Sleep/Wake metadata remain.
5. Bring the existing always-available safety link closer to paced breathing by reducing spacing and italicize the text. Preserve its action, accessibility and availability when breathing is disabled or tracking paused.
6. Append muted, readable Import logs and Export logs buttons as the final Track item, including paused/empty states; each remains at least 48dp and wraps at large fonts. Append optional callbacks `onImportLogs: () -> Unit = {}` and `onExportLogs: () -> Unit = {}` to TrackRoute and TrackScreen; preserve existing parameter order.
7. MindScaleApp wires these callbacks to open SettingsFocus.DATA and invoke existing SettingsViewModel.requestBackupRestore()/requestBackup() respectively. Reuse SettingsRoute's JSON document pickers, messages and confirmation UI. JSON is the complete existing versioned backup, including all record types and existing app state. Do not create a new serializer, import path, MIME type, or version. Returning from a canceled picker leaves data untouched.

Integration clarification: consume an export picker launch once while retaining the prepared document for its result callback. Activity recreation must not relaunch the picker; canceled, successful and failed writes clear the launch signal, and an explicit retry rearms it. JSON bytes and restore semantics remain unchanged.

## Invariants, failure behavior and non-goals

- Existing backup/import validation, bounded parsing, exact preview, explicit replace confirmation, conflict detection, atomic transactions, retry and cancellation remain unchanged. A Track tap alone never imports or erases records. Existing CSV controls remain available in Settings.
- No schema, analytical, permission, dependency, navigation-stack or security redesign. Settings is the existing destination hosting the backup operation, not a new screen. No changes to record contents from styling.
- Preserve all earlier dirty app work. Exclude generated outputs, attachments, IDE-local state, secrets and machine-local settings from the requested commit. Review other untracked project configuration before staging.

## Acceptance and verification

- Focused UI assertions: centered/larger heading, smaller painted event button with 48dp touch target, centered toolbar/outlined input, exactly one visible Ended marker, safety placement/italic style, and final footer callbacks in empty/paused states.
- Navigation/backup checks: both footer actions reach the existing JSON flow; import requires confirmation, cancellation is harmless, export payload includes all record types and remains restorable using existing round-trip coverage. Exercise pickers only with synthetic emulator data.
- Run wrapper `test`, `lint`, `assembleDebug`, and `connectedDebugAndroidTest` pinned to the isolated API36 emulator. Inspect Track/help/footer/note at normal/200% fonts in Light/Dark on a narrow viewport. Preserve rapid-note-input regression coverage.
- Review staged files/diff, commit scoped accumulated project changes (no push), then verify intended phone via adb and install the verified APK with `install -r`; launch it and record evidence. Never run instrumented tests on the phone.
