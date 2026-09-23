# Owner feedback verification — 23 September 2026

Implemented and verified the 17 acceptance criteria in `docs/specs/SPEC-owner-feedback-2026-09-20.md`.
Same checkout: `S:\Android\AndroidProjects\MindScale`, branch `codex/release-readiness`, base
`937ab49f169cfccffec94bf640fe99b14246ba51`. Prior dirty release/simplification work is preserved.
No commit, push, publication, signing/toolchain/schema change, or personal-phone operation.

## Outcomes and source areas

- Track/header/shared note editor: verified the earlier tighter capture layout, 12% larger centered
  root wordmark, help below Sleep/Wake, event pill, Recent Logs heading and toolbar above text.
  Delete note clears the draft; Save persists null while retaining the entry and its other fields.
- Log/MarkerDao: rating rows match Track; events support restorable text/time editing with targeted
  updates, validation, busy guards, Cancel, retry and missing-record handling. No migration required.
- Insights: screen-space rounding preserves exact source readouts, gaps and analytical values;
  nearest rendered rating/event selection replaces event priority. Six summary metrics include
  honestly labeled Median peak and burden in intensity-hours. Day-by-day follows the chart;
  Episodes in this Time Range is independently collapsed. Gap buckets fit two rows of five;
  all 24 hourly intervals share one integer count axis, including zero/single-start cases.
- Report: compact factual metrics, discrete exact-rating bars, concise details and consistent text
  output. Profile/external scores, omitted counts, privacy actions and uncertainty context survive.
  Dark card text has explicit palette colors; zero-rating bars have no misleading terminal dot.

## Oracles

Used the project wrapper, existing Android Studio Quail 2 JDK, SDK and Gradle cache. ADB confirmed
`emulator-5554` is the independent `MindScaleReview_API36`, API 36. Every connected command used
`$env:ANDROID_SERIAL='emulator-5554'`. Required base tasks:

```powershell
.\gradlew.bat test lint assembleDebug connectedDebugAndroidTest --console=plain
git -c core.safecrlf=false diff --check
```

- Final JVM results: **489 tests; zero failures, errors or skips**.
- Final lint: **0 errors, 26 warnings, 2 hints**. Debug assembly passed.
- Full connected run: 300/301 passed; the fifth gap cell width assertion found a real layout
  rounding defect, repaired with weighted cells. Source/test compile and dimension-audit failures
  preceding this checkpoint were fixed without relaxing the audit.
- Narrow rerun at 945×2100 / 420dpi (360dp width): **72/72 passed**. Same base tasks with
  `-Pandroid.testInstrumentationRunnerArguments.class=` followed by these comma-separated classes:
  `com.kieslingdev.mindscale.OwnerFeedbackVisualReviewTest`,
  `com.kieslingdev.mindscale.insights.InsightsScreenTest`,
  `com.kieslingdev.mindscale.insights.InsightsVisualTest`,
  `com.kieslingdev.mindscale.log.LogScreenTest`,
  `com.kieslingdev.mindscale.log.OwnerLogVisualReviewTest`,
  `com.kieslingdev.mindscale.report.ProfileReportVisualTest`.
- Actual Android font_scale 2.0, narrow width: same base tasks filtered to
  `com.kieslingdev.mindscale.OwnerFeedbackVisualReviewTest,com.kieslingdev.mindscale.insights.InsightsVisualTest`.
  24/25 passed; the remaining raster test needed to scroll before touching its lazy node.
  `connectedDebugAndroidTest` filtered to
  `com.kieslingdev.mindscale.insights.InsightsVisualTest#aCentreClickOnTheRasterPanelStillReachesADayRow`
  then passed 1/1 with real 200% font unchanged.
- Final report colors: base tasks filtered to
  `com.kieslingdev.mindscale.report.ProfileReportScreenTest,com.kieslingdev.mindscale.report.ProfileReportVisualTest`.
  14/15 passed; a range-selection assertion needed to scroll back to its lazy range control.
  `connectedDebugAndroidTest` filtered to
  `com.kieslingdev.mindscale.report.ProfileReportScreenTest#clinicianSummaryShowsPrivacyConciseSectionsRangeAndAccessibleActions`
  then passed 1/1. No production edits followed this report run.
- Latest-result union: **305 current device cases, all passing**, including four added wide-course
  captures and excluding three superseded test names. This is not a claim of one full 305-case run.
  The deterministic case/source inventory is `build/review/owner-latest-current-device-results.csv`.

## Visual evidence and artifact

Inspected synthetic Light/Dark layouts at 100% and 200%, at normal and 360dp widths: root branding,
Track controls/history, both note editors, event editing, six metrics, chart rounding at 30 days
with a nearby event, both complete distributions, and report overview/course. Actual Android
200% was used for the platform note dialog. Reviewed the exact `report-sample.txt` output.
The compact gap labels retain full boundaries in semantics/readouts. Report dates/values remain
readable in Dark mode, and chart bars show no false mark for zero.

Ignored evidence is under `build/review/`: `owner-*-suite.log`, `owner-report-final.log`,
`owner-raster-final.log`, `owner-report-range-final.log`, copied `owner-*-results/` XML,
`owner-narrow-screenshots/`, `owner-final-screenshots/`, and `owner-report-screenshots/`.
Earlier `owner-screenshots/` captures predate visual fixes. Prefer the final directories.

APK: `app/build/outputs/apk/debug/app-debug.apk`.
SHA-256: `3E185991BDE64371CD6F2765F82D4E008506270961CCD869DFCFD42974118F4C`.
Installed with `adb -s emulator-5554 install -r`; launched MainActivity with `am start -W`:
Success / Status ok. Final running app inspected in `build/review/owner-launch.png`.
Emulator restored to physical 1080×2424 and font_scale 1.0, with Light as the fresh app default.

API 26 runtime and spoken TalkBack checks remain unperformed. The phone has not received this
build. Broader distribution/signing gates remain separate. Next action: owner reviews the verified
debug build; no additional implementation or continuation task is needed for this scope.

## Authorized phone follow-up

After the verification report above, the user explicitly requested installation on their phone.
Paired through Wireless debugging, confirmed the device model as Pixel 10, and checked the APK
still matched the SHA-256 above. Targeted `adb install -r` returned Success; launching MainActivity
with `am start -W` returned Status ok, cold launch 614ms. Package lastUpdateTime on the phone:
`2026-09-23 14:08:34`. This was an in-place update preserving app data. No uninstall, clear-data,
instrumented tests or record mutations were performed on the phone. Ready for owner testing.
