# MindScale first Google Play production submission

Status: FROZEN for bounded release preparation, 23 September 2026.
User authority: public production release, free pricing, GitHub-hosted privacy page,
explicit acceptance of app-creation declarations, and a new dedicated upload key.
The user's most recent direction supersedes Phase 19's private-APK-only restriction.
Progress and Console evidence belong in `docs/release/PLAY-SUBMISSION.md` and PROJECT_STATE.

## Scope and non-goals

Package the existing product for Play. No new features, data migrations, network permission,
toolchain/dependency upgrades, analytics, billing, or changes to measurement semantics.
No personal-phone uninstall, wipe, destructive test, or assumption of signing compatibility.
Use APIs/CLI/browser text only for Console/GitHub; user prohibits whole-screen viewing.

## Frozen decisions and interfaces

- Retain application ID `com.kieslingdev.mindscale`, minSdk 26, targetSdk 36,
  versionName `1.0`, versionCode `1` for the first upload. Never reuse an uploaded code.
- Privacy URL: `https://kiezling.github.io/Mindscale/`. Retain offline privacy text and add
  an accessible external-browser link with non-crashing missing-handler behavior. Display
  publisher Kiesling Dev and support corykiesling@gmail.com. No INTERNET permission.
- GitHub Pages uses an isolated `codex/privacy-policy` branch, containing only the public
  privacy HTML and `.nojekyll`; application branches remain unchanged by policy deployment.
- Replace the Android-template launcher artwork with a simple MindScale M monogram:
  background LightInk `#17130C`, mark LightOnInk `#E3CE9F`. Use a geometric M path, centered
  inside the adaptive-icon safe area. Supply matching legacy icons at every existing density,
  adaptive foreground/background and monochrome treatment. Retain resource names so the
  manifest interface is unchanged. Do not add dependencies or change the in-app wordmark.
- Provide a matching 512×512 store icon and 1024×500 feature graphic using the existing
  Instrument Sans font and palette. Copy: “MindScale” and “Your symptoms. Your record.”
  No therapeutic/diagnostic claims, fake badges, screenshots of personal records or fabricated UI.
- Upload key is distinct from the app signing key. Keep it outside the repo, protected for the
  current Windows user. No password in source, command arguments, logs or chat. Supply clear
  recovery/backup guidance; do not claim a local protected copy is a completed off-device backup.
- Signing may be an external artifact step; do not introduce release Gradle configuration
  solely to store credentials. Google-managed Play App Signing remains the proposed final identity.
- Declare actual mental/behavioral health, sleep, relaxation features and local data practices.
  Never change feature declarations to evade account policy. Public eligibility/review remains
  a Play Console decision, not inferred from another app in the account.
- Age selection remains owner decision after explaining the recommendation for adult-first
  release; no claim that law universally requires 18+. Initial countries proposal: US/Canada.

## Invariants and failure behavior

Keep all personal data and unrelated local changes intact. A missing signing secret, browser
handler, or account prerequisite must fail with actionable status; never fall back to the debug
key or silently claim submission. Treat debug-to-Play migration separately and rehearse with
synthetic records before any owner migration.

## Acceptance and verification

- Tests/lint/debug build and release bundle succeed after final source/resource edits.
- Final manifest retains package/version/min/target, disables automatic backup, has no network,
  advertising or health permissions, and includes no developer gallery.
- Store icon and feature graphic dimensions/format verified; adaptive and legacy icon assets
  present at each existing density. Inspect generated assets, not the user's desktop.
- Public privacy page loads without login; Console saves the same URL; in-app link and retained
  text are reachable on API36. Meaningful focused UI test covers button intent/fallback if feasible.
- Validate upload certificate and signed AAB hash, inspect native-library 16KB compatibility,
  then validate through Play before claiming it can ship.
- Existing API36 suite, API26 runtime, spoken TalkBack, final signed-install/pre-launch checks and
  rating/audience/country/declaration gates must be reported honestly; no obsolete pass evidence
  is promoted to proof of the final signed release.
- Completion means submitted to the requested production track with observable Console status;
  review approval and public availability must be distinguished from submission.
