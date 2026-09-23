# Phase 19 verification evidence — 17 September 2026

This report records the release-readiness evidence available during the final validation pass. It
does not claim a full release or store readiness.

## Completed checks

- Baseline before the Phase 19 repairs: 440 JVM tests passed; lint had 22 warnings and 0 errors.
- Final `.\gradlew.bat test lint assembleDebug connectedDebugAndroidTest --console=plain`:
  passed in 5m 17s; **462 JVM and 273 API 36 device tests**, zero failures/errors/skips.
  Lint had 0 errors and 26 warnings. The four-warning increase is the
  documented Gradle dependency warning group (13 versus 9 at baseline); the remaining warnings are
  unchanged. Toolchain versions were not changed.
- `app-debug.apk` was verified with `apksigner`: v2 signature, existing Android Debug certificate.
  Candidate identity: application ID `com.kieslingdev.mindscale`, version name `1.0`, version code
  `1`, min SDK 26, target SDK 36.
- `aapt` manifest inspection found no `INTERNET` permission and no dangerous permissions; the only
  declared permission is the app-defined `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- The debug candidate is suitable for initial private APK testing. It is not a full release claim.

## Pending checks

- Physical-device full-flow, API 26 runtime, and spoken TalkBack checks remain unverified. The API 36
  emulator checks below do not replace them. See `docs/release/BETA-CHECKLIST.md`.

## Physical-phone launcher follow-up (R-11)

- Pixel 10, API 37: original package advertised the debug gallery before MainActivity.
- Removed the gallery MAIN/LAUNCHER filter; explicit developer activity launch remains available.
- `.\gradlew.bat test lint assembleDebug --console=plain` passed in 1m 15s: 462 JVM tests,
  zero failures/errors/skips, lint 0 errors and 26 warnings. `git diff --check` passed.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` succeeded without uninstall or data clear.
- `cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER com.kieslingdev.mindscale`
  reported exactly one activity, MainActivity. A package-only `am start` intent did not resolve;
  `monkey -p com.kieslingdev.mindscale -c android.intent.category.LAUNCHER 1` then successfully
  selected the sole launcher. Foreground activity and UI hierarchy confirmed Track, rating
  controls 0–10, and Track/Log/Insights tabs on the physical phone.
- New APK SHA-256: `33BF6DDA2FC4308C679970A7E3AB2CD6574079F33343A2D030C929552C4F7CF5`.
  The previously packaged dated beta APK/ZIP are historical and superseded by this build.
- No instrumented suite was run on the user's personal data. The 273 API 36 device-test result
  above predates this manifest-only change. No product Compose UI changed.

## Manual API 36 evidence

- On the installed API 36 app, Breathing idle Close returned to Track. A one-minute session started,
  Stop showed the finished `Done` view, and Close returned to Track. Captures are retained as
  `build/review/breathing-after.png` and its corresponding XML hierarchy.
- At 200% font, the Settings privacy cards were reachable and readable in both light and dark
  themes. Captures are retained as `build/review/privacy-200-light.png` and
  `build/review/privacy-200-dark.png`.
- A light Insights capture inspected the intensity legend for ratings 1 and 10. The shipped colors
  were preserved and 10 visibly has the stronger contrast, as required by R-10.
  Capture: `build/review/intensity-light-after.png`.
- A known cosmetic gap remains: at 200% font the Settings header wraps `SETTINGS` as `SETTING` and
  `S`. This is recorded as a focused deferred backlog item. These checks do not establish physical
  device, API 26 runtime, or spoken TalkBack behavior.

## Intermediate failures and recovery

- An asynchronous Settings test used the wrong IO context; the test setup was corrected.
- A connected test had an invalid `assertExists` import; it was replaced with `assertIsDisplayed`.
- Two R-10 endpoint expectations were corrected to the frozen endpoint contract.
- The Navigation test's uppercase `CLOSE` selector was corrected to its stable test tag while the
  first connected suite was in progress. That full run ended 272 passed / 1 failed in 8m 16s;
  the final full run passed all 273. This was a deterministic selector error, not a hidden retry.
- An earlier connected install failed before tests ran because a read-only saved-AVD clone had only
  267 MiB free. A clean independent AVD with 4.9 GiB free is now used; the saved AVD is preserved.
- Build logs and generated review harness material remain under ignored `build/review/`.

## Deferred performance evidence

The bounded long-history measurement is recorded in
[`docs/reviews/2026-09-17-performance.md`](2026-09-17-performance.md). It found five years of
synthetic daily data under 100 ms on the desktop JVM and an artificial 20,000-sleep/20,000-entry
case around two seconds. This supports a later bounded optimization decision; it does not authorize
a rewrite. Physical Android latency is unmeasured.

No API 26 runtime, physical-device, or spoken TalkBack result is claimed here. No publication,
store approval, safety certification, or production signing claim is made.

## Private beta artifact and review verdict

The bounded R-1 through R-10 implementation passes its local oracles. The candidate is ready for
the owner's physical-phone beta check, with the external and cosmetic gaps above left explicit.
No schema, dependencies, toolchain, signing configuration, or network permissions changed.
Source changes remain uncommitted on `codex/release-readiness`, based on `937ab49`.

Prepared folder: `build/private-beta/2026-09-17/` (ignored build output), containing the APK,
install handout, privacy draft, checksum and build metadata. APK SHA-256:
`2DA0B9A4674C87047C11C7134A4AF539DAF5CBDC8151281D5CFCB2060CF78518`.
Final build log: `build/review/phase19-final-all.log`. Existing unrelated `.idea/misc.xml`,
`.agents/`, and `.codex/` changes are preserved. No commit, push, upload or publication occurred.
