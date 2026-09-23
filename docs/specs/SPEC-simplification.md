# MindScale — everyday-use simplification

Status: IMPLEMENTED — VERIFIED LOCALLY, 2026-09-17. Installed and launched on the owner's Pixel 10.
Evidence: `docs/reviews/2026-09-17-simplification.md`.
Authority: user's physical-phone walkthrough and request to implement all listed changes;
resumed after an explicit pause. Base: codex/release-readiness, 937ab49 plus verified,
uncommitted Phase 19 repairs. Preserve those repairs and unrelated local changes.

## Outcome and non-goals

Make common actions clear and fast; place secondary explanation and controls behind named,
accessible expanders. No account, network, dependency/toolchain/signing change, deletion of
existing records, schema migration, report-metric change, or wholesale visual redesign.
This spec supersedes older visual-only/test-immutability constraints for the listed changes;
old step-only chart presentation, exposed onset controls, plain-only note UI, typed timestamp
UI and explicit name-save requirements are superseded only as stated below.

## S-1 — Track and entry editing

Center MindScale's wordmark on Track; put the Track label at the side and retain Profile access.
At large font sizes give long header titles their own full-width row and reduce decorative
navigation letter spacing to avoid breaking words. System-bar icon contrast follows the app's
resolved Light/Dark choice, including when it differs from the device setting.
Keep other screen navigation intact. Use a centered, generously sized 0–10 keypad for Track
entry edits. Date buttons open a Material calendar with month/year selection; time buttons open
hour/minute wheels and AM/PM for 12-hour mode (24-hour respects saved preference). No mandatory
typing or format-instruction prose. Apply shared controls to Track backdate/edit and Full Log
entry edit; existing Log range date pickers remain. Preserve ViewModel-owned timestamp strings,
future-time validation, local-zone/DST semantics, cancellation, saved drafts and targeted writes.
Picker confirmation changes the draft through existing events; cancellation performs no write.

Shared picker API (new ui/components/MsDateTimeFields.kt):
`MsDateTimeFields(dateText: String, timeText: String, onDateChanged: (String)->Unit,
onTimeChanged: (String)->Unit, hourFormat: HourFormat, modifier: Modifier = Modifier,
enabled: Boolean = true, tagPrefix: String = "timestamp")`. Date/time button tags are
`${tagPrefix}_date` and `${tagPrefix}_time`; confirmed callbacks emit ISO yyyy-MM-dd and HH:mm
strings, preserving existing draft parsers. Calendar selection uses UTC civil dates only;
final local-zone instant conversion stays in the existing ViewModel. Wheel selection is local
dialog state, restored on recreation, and sends no callback until confirmation. A malformed
legacy draft stays intact until a user confirms a replacement. Use platform NumberPicker
through AndroidView with app light/dark styling and accessible hour/minute labels; respect font
scale and give AM/PM its own row when needed. No keyboard is required. Calendar/time dialogs
must fit a small phone with scrollable content at 200% font.

## S-2 — formatted notes

Add a shared styled editor and renderer for entry notes in Track and Full Log. Toolbar actions
are Bold, Italic, Underline and Strikethrough, with >=48dp accessible targets. Selected text can
be toggled; with a collapsed selection the chosen style applies to subsequently typed text.
Editor shows styled text without markup. Preserve plain legacy strings exactly until edited.

Frozen transport: existing nullable note string carries either plain text or a versioned span
envelope: `[[MindScale note v1]]\n` + semicolon-separated `b|i|u|s,start,end` triples + `\n` +
literal text (for example `b,0,4;i,2,6`). Offsets are UTF-16 half-open ranges, bounds checked and
not inside surrogate pairs. Unknown/malformed envelopes render as literal original text, never
throw or discard it. Normalize/merge same-style ranges, cap at 128 spans; no-span notes use plain
text. All existing 4,000-code-point/allowed-character validation applies to the entire persisted
string; do not truncate to fit. JSON/CSV retain the exact string for lossless round trips without
schema/backup-version changes. Existing backup/import keys and conflict comparisons stay intact.
Renderer, accessibility descriptions and note previews show decoded text/styles, not metadata.
Codec/edit-range transformations are pure Kotlin; no HTML, WebView or external parser.
Frozen shared APIs: `RichNoteCodec.decode(String): RichNote`, `encode(RichNote): String`,
`plainText(String): String`; `RichNote(text: String, spans: List<NoteSpan>)`;
`RichNoteEditor(value: String, onValueChange: (String)->Unit, modifier: Modifier = Modifier,
enabled: Boolean = true, isError: Boolean = false, supportingText: String? = null)`;
`richNoteAnnotatedString(value: String): AnnotatedString`.

## S-3 — set onset words aside

Remove onset prompts, tag editing/display and onset settings/prompt toggle from active Track,
Log, Insights and Settings UI. Suppress prompting even for old askChips=true settings. Preserve stored
chips/settings, backup/import fields, DAO methods and episode derivation; editing value/time
must retain an existing record's chips. Keep this feature parked, not deleted from user data.

## S-4 — quieter Settings, Profile and Breathing

Settings initially shows Appearance and clearly named closed sections: Tracking preferences,
Data & backups, Privacy & product information. Put secondary anchor/hold/toggle explanations
inside Tracking; exports/import/erase inside Data; all current privacy cards inside Privacy.
Expanders expose state and >=48dp targets and survive recreation. Focused navigation opens the
appropriate section; no hard-coded obsolete item index. Preserve destructive confirmations.
Light is the fresh/default/reset theme (entity and fresh seed), without rewriting existing
explicit saved choices or historical migration/import semantics. Set this user's phone to
Light through the normal setting after deployment if needed.

Profile name saves on IME Done and real focus exit, with a short Saved/Saving status. Remove the
ordinary tiny Save name button. Serialize writes; never let an older completion overwrite a
newer draft. Save latest pending edits; retain draft and visible retry on failure. Existing
conflict requires explicit replace, never automatic overwrite. Navigation away also commits
the latest valid name. No other profile/score behavior changes.

Breathing retains practical instructions, timing and session recording disclosure. Remove
`It is a circle that keeps pace.` Use `MindScale makes no claim about what this does.` as a
quiet footer after the usable controls/instructions. Do not add medical-benefit claims.

## S-5 — calmer Insights and visual transitions

Default visible content: range, compact primary summary, What you recorded chart, report link.
Put raster/legend explanation behind `Day-by-day view`, and episode/gap/onset/sleep breakdowns
behind `More details`. Keep errors/empty states and chart accessibility reachable.

Only the chart's visual stroke and matching area fill are smoothed. At a contiguous actual source change, transition from the
previous value into the new recorded value during the last min(15 minutes, available interval)
before the new source timestamp. Use monotonic smoothstep/cubic easing without overshoot;
short intervals interpolate between their two actual recordings. Retain exact recording points,
source selection/readouts, episode/raster/summary/report/export data and held-state calculations.
Never interpolate through sleep, unknown, hold expiry or future time, nor into an artificial
range/sleep boundary. Adjacent label: `Transitions are smoothed for display; only entries are
recorded.` Pure projection helper with tests for increasing/decreasing/equal/short-interval,
range-clipped, sleep/gap and expired-hold cases; calculation model unchanged.

## Failure behavior and verification

Implementation references checked 2026-09-17: Android's
[Compose date picker guidance](https://developer.android.com/develop/ui/compose/components/datepickers),
[NumberPicker API](https://developer.android.com/reference/android/widget/NumberPicker), and
[styled text guidance](https://developer.android.com/develop/ui/compose/text/style-text).
Use the project's installed versions; these references authorize no dependency upgrade.

No silent note truncation, chip loss, date normalization, failed-save dismissal, or unannounced
metric change. Existing error/retry/conflict paths remain. Preserve >=48dp targets, readable
light/dark and 200% font, selection semantics and draft restoration. Update tests for explicitly
replaced UI contracts, not to weaken persistence/validation checks.

- Pure tests: note codec and span edits/round trips; time conversions; smoothing/gap boundaries;
  profile autosave races/failure; no onset prompt despite stored preference; light fresh seed.
- UI tests: formatted note edit/reopen; calendar/wheels confirm/cancel; keypad selection;
  closed/open Settings and Insights; Profile Done/focus-exit; centered Track wordmark.
- Run wrapper test, lint, assembleDebug and connectedDebugAndroidTest on isolated API 36 emulator.
- Inspect changed screens at normal and 200% font in light/dark. Never run destructive test suites
  against the user's phone; after oracles pass, update in place with adb install -r and launch.
- Record exact evidence, remaining gaps and current next action before handoff. No commit/push.
