# Simplification verification — 17 September 2026

The requested changes in `docs/specs/SPEC-simplification.md` are implemented and verified locally.
Branch: `codex/release-readiness`; base commit: `937ab49`. Work remains uncommitted, together with
prior Phase 19 repairs. Unrelated local files were preserved.

## Outcomes

- Centered Track branding, a larger centered edit keypad, calendar dates, and labeled hour/minute
  wheels with an AM/PM toggle. Large-text headers and tabs reflow; system icons follow app theme.
- Bold, italic, underline and strike-through notes in Track/Log. Legacy text remains readable;
  versioned formatting round-trips through existing JSON/CSV without a schema change. Rapid
  typing preserves text, style and IME composition when parent state updates arrive late.
- Onset/tag controls and labels are parked without deleting stored tags or backup fields.
- Settings and Insights hide secondary detail behind named expanders. Profile name saves on Done,
  focus exit and navigation. New/default/reset theme is Light; existing explicit choices survive.
- Breathing uses the requested quiet footer. Chart transitions ease over at most 15 minutes,
  with an adjacent explanation; recorded values, source readouts, gaps, calculations and reports
  remain unchanged.

## Verification

Used the project wrapper, Android Studio bundled JDK, existing SDK and Gradle cache. Device tests
were pinned with `ANDROID_SERIAL=emulator-5554` to the isolated `MindScaleReview_API36` AVD.
No instrumented suite touched personal-phone data.

- `./gradlew.bat test lint assembleDebug connectedDebugAndroidTest --console=plain` exercised
  all 280 then-existing device tests. Four remaining failures were Insights test setup clicking
  lazy controls outside the viewport; the other 276 passed. Source logic was not weakened to pass.
- After fixing test scrolling, the same wrapper tasks with
  `-Pandroid.testInstrumentationRunnerArguments.class=com.kieslingdev.mindscale.insights.InsightsScreenTest,com.kieslingdev.mindscale.insights.InsightsVisualTest`
  passed all 35 affected tests.
- Final accessibility/copy corrections passed `test lint assembleDebug connectedDebugAndroidTest`
  with `-Pandroid.testInstrumentationRunnerArguments.class=com.kieslingdev.mindscale.MindScaleChromeTest,com.kieslingdev.mindscale.NavigationTest,com.kieslingdev.mindscale.insights.InsightsScreenTest,com.kieslingdev.mindscale.insights.InsightsVisualTest`:
  **66 device tests passed**, plus **480 JVM tests**, **lint: 0 errors / 26 warnings / 2 hints**,
  and successful debug assembly.
- `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.kieslingdev.mindscale.MindScaleHeaderGeometryTest --console=plain`
  passed both 200% header geometry tests, including the newly added overlay-title check.
- Latest-result union: **281 distinct device cases, zero remaining failures/errors/skips**.
  This combines the full run and focused reruns; no single full 281-test run is claimed.
- `git -c core.safecrlf=false diff --check` passed. No dependency, toolchain, signing or schema change.

Ignored evidence under `build/review/`: `simplification-verified-suite.log`,
`simplification-insights-final.log`, `simplification-final-chrome.log`,
`simplification-header-final.log`, and copied XML in `simplification-full-results/` and
`simplification-chrome-results/`. Current header XML remains in the standard connected output.

## Visual and interaction checks

Inspected normal Light capture, Settings, calendar, editing and formatted notes; populated Dark
Insights; 200% Dark edit keypad/time wheels; final Track and Settings at 200% in both themes.
Repeated real rapid typing of `A calmer day` after the formatting repair: complete text and bold
style remained intact. Native wheels were swiped in tests and confirmation/cancellation checked.
Profile tests assert stored DAO values after Done and actual focus loss, not merely draft text.

Screenshots are under `build/review/simplification-*.png`, particularly `note`, `calendar`,
`time-dark-200`, `track-dark-200`, `settings-dark-200`, `track-light-200`, and `settings-light-200`.
The final header captures confirm readable system icons and no broken navigation words.
The emulator was restored to 100% text size and Light after inspection.

## Artifact and next action

APK: `app/build/outputs/apk/debug/app-debug.apk`.
SHA-256: `C317F0A80CD545802E29A6C393141873381D1ECD8C799FC170C6A7516927AC96`.

Installed/launched successfully on the isolated API 36 emulator and subsequently on the owner's
Pixel 10 after fresh wireless pairing. `adb install -r` returned Success; `am start -W -n
com.kieslingdev.mindscale/.MainActivity` returned Status: ok (cold launch, 643 ms). Track UI and
the running app process were confirmed. Package lastUpdateTime was 2026-09-17 23:00:53 on the
device clock. The in-place update retained app data; no uninstall, clear-data operation or
instrumented tests were run on the phone. Theme selection was not confirmed because navigation
changed during the UI check, so no Light override is claimed. The build is ready for owner testing.

API 26 runtime and spoken TalkBack checks remain unperformed. Durable signing/version policy and
broader distribution gates from the release-readiness review remain separate from this UI pass.
No commit, push, publication, signing-key creation or user-data deletion was performed.
