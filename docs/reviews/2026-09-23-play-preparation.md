# Play preparation verification — 23 September 2026

Checkout: `S:\Android\AndroidProjects\MindScale`, branch `codex/release-readiness`,
base HEAD `21a91c8`. Candidate includes uncommitted privacy link/contact, branded launcher
resources and focused privacy UI test. No toolchain, dependency, schema, permission or
measurement behavior change. These checks preceded the owner's subsequent request to commit
and publish the complete current project to GitHub. The application diff was reconfirmed identical
to the verified candidate before that commit; GitHub publication status is in PROJECT_STATE.md.

## Final candidate

- Signed AAB: `build/play-release/mindscale-1.0-1.aab` (ignored).
- SHA256: `4AED2619A8DC916F2AABB36145467B193BA4104A6A0A9F375F957EC3E98A7A66`.
- Package `com.kieslingdev.mindscale`, version `1.0`/`1`, min 26, target 36.
- New dedicated upload certificate, fingerprint in `docs/release/SIGNING.md`.
- Jarsigner reports `jar verified`. Expected Android upload-certificate warnings: self-signed,
  no public CA chain, no timestamp. Certificate expires February 2054. Not uploaded yet.
- All four bundled `libandroidx.graphics.path.so` ABIs have every ELF LOAD segment aligned to
  16384. Evidence: `build/review/play-submission/native-alignment.json`. This checks ELF alignment,
  not Play-generated APK ZIP alignment or runtime on a 16KB-page device; verify those separately.

## Commands and results

```powershell
$env:ANDROID_SERIAL = 'emulator-5554'
.\gradlew.bat test lint assembleDebug bundleRelease connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.kieslingdev.mindscale.settings.SettingsPrivacyTest' --console=plain
git diff --check
```

- PASS: 490 JVM tests; 0 failures/errors/skips.
- PASS: 2 focused API36 SettingsPrivacyTest cases; 0 failures/errors/skips. Exact HTTPS
  ACTION_VIEW is intercepted, no real external browser launch. Offline text and publisher/support
  remain reachable; existing 200% text privacy test passes.
- PASS: lint, 0 errors / 27 warnings / 2 hints. Added warning is `UseKtx` suggesting `String.toUri`
  instead of `Uri.parse`; no correctness issue. Prior warnings retained.
- PASS: debug assembly and release bundle build. Signing performed afterward without changing
  application bytes. Gradle/source files contain no key/password configuration.
- PASS: diff whitespace check; only Windows line-ending notices.
- API36 emulator only; personal Pixel 10 never targeted. Emulator console name query failed
  authentication, but adb SDK property reports 36 and the focused suite passed on emulator-5554.
- Initial integration build caught a missing `LocalContext.current` binding in SettingsScreen;
  fixed before the final passing run. Initial signing attempt failed to parse the newline at the
  end of the protected-password file; adding Trim fixed it without regenerating/exposing secrets.

Ignored evidence: `build/review/play-submission/final-build.log`, `test-summary.json`,
native alignment JSON, key-creation/signing scripts, and existing Gradle reports.
Full prior API36 suite (313 latest passing cases across runs) predates these bounded edits;
do not describe the current two-case run as a new full-suite pass.

## Assets and publication evidence

- `docs/release/assets/`: 512×512 RGB PNG icon, 1024×500 RGB PNG feature graphic; deterministic
  renderer uses bundled Instrument Sans and frozen palette. Branding worker inspected generated
  artifacts and verified all existing legacy density sizes, adaptive safe-area and monochrome.
- Public policy verified live through browser text at https://kiezling.github.io/Mindscale/.
  GitHub Pages branch `codex/privacy-policy`, commit `4d20b6806387b0f5a06a28dda6e647bb5fe30b02`.
- Free Console entry created after explicit owner confirmation of declarations. Privacy, health,
  unrestricted sign-in, no-ads, not-government and no-financial-features answers are saved.

## Open coverage/release gates

No production submission/approval yet. API26 image is not installed (only API36 available),
API26 runtime and spoken TalkBack remain open. No final Play-generated APK/install/pre-launch
check yet. Account is personal and Google's health-account organization guidance needs resolution.
Age/country choices, rating/data safety, listing screenshots and category/contact still pending.
Off-device upload-key backup remains open. No migration of the owner's debug installation.
