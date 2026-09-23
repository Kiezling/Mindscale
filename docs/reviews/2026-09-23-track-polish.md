# Track polish and JSON backup shortcuts — verification

Governing spec: `docs/specs/SPEC-track-polish-backup-2026-09-23.md`. Same checkout and branch
`codex/release-readiness`, starting at `937ab49f169cfccffec94bf640fe99b14246ba51`.
User authorizes committing accumulated project work and updating the connected Pixel 10 in place.

## Changes

- Track help explains Sleep/Wake followed by a rating. Recent Logs is centered and larger.
  Mark an event uses a smaller painted pill while retaining a 48dp touch target.
- Shared notes have a centered formatting toolbar and a themed border around the text section.
  Pending-echo, selection, IME composition and formatting logic are preserved.
- Track keeps one outlined Ended marker; Log keeps one band label. Duplicate metadata and
  repeated accessible description are removed. The safety link is closer to breathing and italic.
- Final muted Import logs/Export logs actions open the existing Settings DATA JSON flow.
  All existing backup record types and settings remain included; restore retains validation,
  preview and explicit replacement confirmation. Export picker launch is consumed once while
  prepared bytes survive activity recreation. No schema, serializer or permission changes.

## Verification

- Project wrapper with the existing Android Studio JDK/SDK; all connected commands explicitly
  target isolated `emulator-5554`. No phone test suites or data clearing.
- First full command: `test lint assembleDebug connectedDebugAndroidTest --console=plain`.
  490 JVM tests pass with no failures/errors/skips; lint has 0 errors, 26 warnings and 2 hints;
  debug assembly passes. Full connected run: 295/312 passed before harness corrections.
- Initial UI failures came from the emulator sleeping (confirmed by power state and black
  screenshot); it was woken and set to stay awake. Two picker tests used an action-only intent
  filter that missed MIME-typed intents; they now intercept and assert the actual JSON contract.
  The production backup shortcut was not changed to accommodate those harness failures.
  The Ended assertion also needed the unmerged semantics tree, matching the existing row tests.
  New Log test imports were corrected before its runtime check.
- Synthetic screenshots are copied during runs into ignored `build/review/track-polish-shots/`.
  Light/Dark normal and Compose 200% Track/help/footer views inspected. A focused run at
  945×2100/420dpi (360dp width), with actual Android font_scale 2.0, passed all four visual
  cases. Both Light and Dark note dialogs were inspected: centered toolbar, visible field
  border, readable content and reachable actions. Footer and help also fit/reflow.

Final focused command ran `test lint assembleDebug connectedDebugAndroidTest --console=plain`
with `-Pandroid.testInstrumentationRunnerArguments.class=` and these comma-separated targets:

- `com.kieslingdev.mindscale.MindScaleChromeTest`
- `com.kieslingdev.mindscale.MindScaleHeaderGeometryTest`
- `com.kieslingdev.mindscale.NavigationTest#trackImportShortcutUsesJsonPickerAndCancelLeavesDataUntouched`
- `com.kieslingdev.mindscale.NavigationTest#trackExportShortcutUsesJsonPickerOnceAcrossRecreation`
- `com.kieslingdev.mindscale.log.LogScreenTest`
- `com.kieslingdev.mindscale.track.TrackScreenTest#zeroRatingHasExactlyOneVisibleEndedMarker`

All 27 cases passed. The four-case narrow/200% run used `connectedDebugAndroidTest` filtered to
`com.kieslingdev.mindscale.OwnerFeedbackVisualReviewTest`. The latest-result union is **313 current
device cases, all passing**, with no skips, across the full run and focused reruns (not one full
313-case run). It includes JSON intent/MIME assertions, cancellation without database changes,
explicit restore preview/confirmation, and existing backup round-trip and rich-note typing tests.

Ignored evidence: `build/review/track-polish-{full,final,large-font}.log`, corresponding
`*-results/` XML, `track-polish-latest-device-results.csv`, `track-polish-shots/` and
`track-polish-large-font-shots/`. Emulator restored to physical size and font_scale 1.0;
final APK installed/launched successfully there (cold launch 2759ms).

Final APK SHA-256: `C93A3A35D595A633B05A6FD859058D7626C97C2235E885BD83BB84BA943EFC8C`.
No production edits followed this verification.

## Commit scope and remaining checks

Include accumulated application, tests, specs/reviews/release drafts and portable project-agent
configuration. Keep `.idea/misc.xml`, `.codex/hooks.json` (absolute machine-specific hook path),
user attachments and ignored generated artifacts outside the commit. No push or publication.
API26 runtime and spoken TalkBack remain broader-release coverage gaps.
