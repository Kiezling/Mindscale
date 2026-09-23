# MindScale Google Play submission

Prepared 23 September 2026. Status: app entry created and signed candidate ready;
Console setup/release validation still in progress; not submitted.
The owner explicitly requests a public production release. This replaces the earlier
private-APK-only distribution choice. Do not treat historical publication restrictions as
a new request for approval of the already-requested submission. Legal agreements, account
eligibility, pricing, signing custody and missing factual declarations still need resolution.

## Confirmed identity and account

- Name: MindScale. Package: `com.kieslingdev.mindscale`.
- Publisher/support: Kiesling Dev / corykiesling@gmail.com, previously owner supplied.
- Console inspection: Kiesling Dev is a personal account with another production app;
  MindScale's free app entry has now been created. Package-name availability check succeeded.
- Owner explicitly approved the creation declarations (policies, Play App Signing terms,
  US export compliance); created with English (United States), App, Free. Default automatic
  protection remained enabled. Policy URL, health features, unrestricted sign-in access,
  no ads, not-government and no-financial-features declarations are saved for review.
- Owner says the account is verified. Creation date remains unknown; another production
  app does not prove this new app has production access.
- Google's account-type guidance says health apps should use an organization account.
  Resolve applicability for this local symptom journal with the actual declaration flow
  or Play support before asserting eligibility. Do not hide health functionality, change
  the account, or contact support without the owner's instruction.

## Candidate and signing

- Checkout: `S:\Android\AndroidProjects\MindScale`, `codex/release-readiness`,
  commit `21a91c8` (application unchanged since `a5a6a45`).
- Current version is `1.0`, code `1`; min API 26, target API 36, compile 36.1.
- Release build has no signing configuration. An unsigned bundle can prove compilation
  but cannot be submitted. Never upload the debug APK or use its debug signing key for Play.
- Dedicated RSA4096 upload key created with owner approval outside the repository; see
  `SIGNING.md` for custody and recovery. Signed candidate and evidence are in
  `docs/reviews/2026-09-23-play-preparation.md`. Google-managed app signing still needs
  final selection/verification in the release workflow. Off-device key backup remains open.
- Once signed, verify the certificate, final version, merged manifest, bundle hash and
  Play-generated APK behavior. Increment versionCode for every subsequent uploaded build.
- The owner's debug-signed phone installation cannot normally be updated by a differently
  signed Play build. Preserve it. Rehearse JSON migration with synthetic data; never uninstall
  the owner's app or delete its records to resolve a signing mismatch.

## Listing and policy materials

- Listing copy: `STORE-LISTING.md`, updated to remove the parked context-word capture feature.
- Public policy: https://kiezling.github.io/Mindscale/ (verified live without sign-in).
  Canonical HTML is `privacy/index.html`; deployed isolated policy branch commit
  `4d20b6806387b0f5a06a28dda6e647bb5fe30b02`. No app branch was pushed.
- Settings now keeps offline text and adds the hosted link plus publisher/support; focused
  API36 UI tests pass. Actual link launch is intercepted in the test to avoid external browsing.
- `assets/` contains the inspected 512×512 icon and 1024×500 opaque feature graphic plus a
  deterministic renderer. Matching adaptive/legacy/monochrome launcher artwork is in the app.
  At least two genuine current phone screenshots with synthetic records still need selection.
- Pricing is Free. Target ages, launch countries and final content-rating answers are unresolved.
  Do not invent these or claim an assigned rating before completing the Console questionnaire.

## Proposed Console answers — review against the final binary

| Area | Proposed answer / evidence |
| --- | --- |
| App access | All functionality available without login; no credentials required. |
| Ads | No ads; current app dependencies contain no advertising SDK. |
| Account creation | None; records and Profile are local, not server accounts. |
| Data safety collection | No off-device developer collection in current implementation. Verify final runtime SDKs and merged manifest. On-device processing is outside Play's collection definition. |
| Data safety sharing | User-initiated export/share may qualify for Play's sharing exception; verify each final flow and disclose exports, clipboard, document providers and external apps in the privacy policy regardless. |
| Health declaration | Has health features. Consider mental/behavioral health, sleep tracking, and stress-management/relaxation categories matching the actual form. Never select “no health features.” |
| Medical claims | Not a medical device; no diagnosis, treatment, cure, prevention or risk-assessment claims. Include the existing healthcare-professional reminder. |
| Store category | Health & Fitness is a proposed category, not an account-eligibility exemption. |

Release notes draft:

> First release of MindScale. Record ratings, sleep, notes and events; review your history
> and charts; create a summary; and manage local JSON backups. Includes a personal safety
> plan and an optional paced-breathing tool. No account or ads.

## Remaining gates in order

1. Resolve the publisher account's eligibility for MindScale. Console accepted the health
   declaration with no extra regional requirements, which does not prove policy approval.
2. Finish age/country/category/content-rating/data-safety choices, store listing and screenshot
   selection. Upload the prepared store graphics and use the existing published privacy URL.
3. Back up upload-key custody; inspect the signed candidate in Play. Rebuild and verify if any
   additional source/resource edit is needed; never silently replace an already-uploaded version.
4. Complete API 26 runtime and spoken TalkBack checks. Existing evidence covers the API 36
   emulator and Pixel 10 debug installation; it does not prove the final Play-signed build.
5. Upload the signed AAB, inspect Play validation/pre-launch results, resolve real findings,
   and submit production if Console permits. If the new-personal-account testing gate applies,
   complete the required closed test before applying for production access.
6. Record submission/review status and only claim public availability after Console confirms it.

## Official sources checked 23 September 2026

- Account type: https://support.google.com/googleplay/android-developer/answer/13634885
- Personal-account testing: https://support.google.com/googleplay/android-developer/answer/14151465
  (for accounts created after 13 November 2023: 12 continuously opted-in testers for 14 days
  before applying for production access; elapsed time alone is not approval).
- Health policy: https://support.google.com/googleplay/android-developer/answer/16679511
- Health categories/declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Signing: https://developer.android.com/studio/publish/app-signing
- Store assets: https://support.google.com/googleplay/android-developer/answer/9866151
