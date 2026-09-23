# MindScale beta and release checklist

Prepared 17 September 2026 from R-8 and R-9. A checkbox marks a gate that is verified only when
checked; unchecked items remain open and do not claim publication or production readiness.

## Private APK beta first

- [ ] Choose a private APK delivery route and recipient list; do not publish or upload the APK.
- [x] Prepare the debug candidate at `build/private-beta/2026-09-17/app-debug.apk`. It is signed
  by the existing local debug key for private testing; no production signing change is authorized.
- [ ] Record the private feedback route and give testers `docs/release/PRIVATE-BETA.md` with the
  install and data-loss instructions.
- [ ] Verify the final candidate on at least one physical device and record the device model and
  Android version. API 26 compatibility remains an open check.
- [ ] Confirm the owner understands that automatic backup is disabled and that uninstalling the
  app can remove local data; ask testers to export data before uninstalling.

## Later conditional public or Google Play distribution

These gates apply only if the owner chooses a public or Google Play route after the private beta;
they do not block private APK testing.

- [ ] Choose the Google Play testing/production track and confirm the publisher account owner.
- [x] Record the owner-supplied publisher and support details: Kiesling Dev,
  corykiesling@gmail.com. The Play account is personal; its creation date is unknown and must be
  checked before choosing the production path.
- [ ] Confirm whether the personal Google Play account was created after 13 November 2023. If it
  was, complete the required closed test with at least 12 testers opted in for 14 continuous days
  before production access. If it was not, record the applicable Play requirement:
  <https://support.google.com/googleplay/android-developer/answer/14151465>
- [ ] Supply and verify the durable production signing identity, upload-key recovery plan, and
  reproducible version/update decisions. No production key belongs in this repository.
- [ ] Host `PRIVACY.md` at a public HTTPS URL and add the URL to the final public release UI and
  listing. The hosting URL is currently unknown and must be supplied by the owner.
- [ ] Complete the Google Play Health Content and Services declaration and retain its result.
- [ ] Verify the final non-medical-device description and healthcare-professional reminder.

## Product identity and owner details

- [x] Confirm the APK identity is `MindScale` and the application ID is
  `com.kieslingdev.mindscale` in the final candidate.
- [ ] Review the shipped launcher icon and round icon at each density, then supply approved store
  icon, screenshots, feature graphic, and any other required listing assets. The manifest currently
  points to `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`; appearance approval remains open.
- [ ] For a later public release, confirm the supplied publisher support email and any required
  legal address in Play Console, the public privacy policy, and the store listing. Do not publish
  until the same owner-approved contact is present everywhere it is required.
- [ ] For a later public release, record the public HTTPS privacy-policy URL and verify it opens
  without sign-in; add the same URL to the listing and the shipped release UI.

## Tester flow

1. Give each tester the beta install instructions and a private bug-report route.
2. Ask testers to exercise first launch, Track, Full Log, Insights, Profile, Report, Settings,
   Safety, export/import, erase confirmation, process recreation, and offline use.
3. Ask testers to check light/dark themes, large font, TalkBack navigation, back navigation,
   rotation or recreation, and the Safety resource actions without contacting a crisis service.
4. Ask testers to report reproducible steps, device model, Android version, app version, and
   whether the problem survives a fresh launch. Do not request health details or safety-plan text.
5. Triage reports privately; redact record contents and never paste personal health information into
   public issue trackers.

## Device and accessibility gates

- [ ] Run the private APK candidate on a physical device; a durable release-signed candidate is a
  later distribution gate.
- [ ] Verify API 26 compatibility and record device/model evidence.
- [ ] Inspect TalkBack spoken output and traversal on a physical device.
- [ ] Check 100%, 150%, and 200% font sizes, portrait and landscape where supported.
- [ ] Confirm every important control remains reachable and at least the intended touch target.
- [ ] Confirm offline launch and all local data actions after process death.

## Technical and publication gates

- [x] Run `test`, `lint`, `assembleDebug`, and the API 36 connected suite against the combined tree:
  462 JVM and 273 device tests passed; lint 0 errors/26 warnings. See the dated verification report.
- [x] Review the final diff for permissions, network capability, backup rules, and signing changes.
- [ ] For a later public release, verify privacy-policy URL, publisher contact, store declarations,
  screenshots, icon, and other external assets.
- [ ] For a later public release, confirm release signing key ownership, upload-key recovery, Play
  Console ownership, and the selected testing/production track with the publisher.
- [ ] Obtain explicit publication approval before any public upload or release.

## Evidence recorded for the safety copy

- United States 988 free, confidential, 24/7 support: <https://988lifeline.org/>
- United States 711 then 988 relay path: <https://988lifeline.org/deaf-hard-of-hearing-hearing-loss/>
- Canada English/French 24/7 service and carrier-charge qualification for texts over a plan's
  allowance: <https://988.ca/get-help/what-to-expect>
- Canada 9-8-8 service and local emergency direction: <https://988.ca/>
- United States emergency calling reference: <https://www.911.gov/calling-911/>
- International directory description: <https://findahelpline.com/about>

The safety resource check date in the app is 17 September 2026. No crisis service was contacted as
part of verification.

## Current unverified gates

Physical-device and API 26 runs, spoken TalkBack review, public policy URL, and durable production
signing remain open. Publisher/support were supplied by the owner. No store approval, safety
certification, or publication claim is made here.
