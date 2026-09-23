# Phase 19 — first-release correctness and readiness

Status: IMPLEMENTED — VERIFIED LOCALLY, 2026-09-17. Frozen decisions retained below.
Final evidence: `docs/reviews/2026-09-17-verification.md`; 462 JVM and 273 API 36 tests pass,
lint/build pass. Physical-device, API 26 runtime and spoken TalkBack checks remain release gates.
Distribution decision: the user selected private APK distribution first on 2026-09-17.
Prepare the existing debug-signed candidate for initial testing. This does not authorize a
production signing change, upload, or contacting testers. Play-specific gates are conditional
on a later Play release; device checks and honest privacy information still apply now.
Authority: the user requested comprehensive review, then explicitly delegated leadership to
finish the existing product and get it ready for people to use. Publication, signing, remote
push, and deletion of user data still require the existing explicit boundaries.

## Outcome and scope

Finish the existing local symptom-tracking product by repairing confirmed correctness,
data-safety, privacy, and navigation defects. Do not add new features or wait for the user's
unremembered personal improvement list. This spec supersedes conflicting narrow behavior
clauses of earlier specs only where a decision below explicitly says so.

Baseline: main at 937ab49; application tree equals the Phase 18 merge 9d6767a.
Fresh JVM run: 440 passed. Wrapper test/lint/assembleDebug passed; lint 22 warnings, 0 errors.
Five review probes reproduced backup round-trip, CSV chip identity, cross-type duplicate,
future-time edit, and Safety retry defects. Connected verification must be rerun.

## Non-goals

No accounts, network, inference, diagnostic claims, notifications, new dependencies,
toolchain upgrade, schema migration, new backup version, new release/signing configuration,
publication, or speculative UI redesign. Do not implement the forgotten personal defect list.
No blanket rewrite and no requirement to eliminate harmless template/dependency lint warnings.

## Frozen decisions and interfaces

### R-11 — unambiguous private-beta launcher (2026-09-17 follow-up)

The user's installed private APK opened the design gallery instead of the product. The debug
package advertises two MAIN/LAUNCHER activities, with the gallery listed first on the phone.
This decision supersedes SPEC-visual-foundation D-21's second launcher requirement: only
MainActivity may advertise MAIN/LAUNCHER. Retain the debug-only gallery activity for explicit
developer launches; do not add it to product navigation or change release/signing configuration.
App ID, signing key, database and user data remain unchanged. An update must install with
`adb install -r`; never uninstall or clear data to resolve a deployment failure.
Acceptance: test/lint/assembleDebug pass, the installed package exposes exactly one launcher
(MainActivity), and normal MAIN/LAUNCHER launch opens Track on the user's Pixel 10.
No new feature, persistence change, or gallery redesign is in scope.

### R-1 — protect export-first erase and preserve recovery

The existing bounded import format remains authoritative (8 MiB, collection limits and field
limits). New Note and Marker saves must obey the import text rules, including 4,000 Unicode
code points and allowed control characters. Invalid text stays editable with a visible error;
never truncate or silently clear it. Existing oversized records are not modified automatically.

Before offering export-first erase, the exact encoded JSON must pass the same bounded reader
and parser as restoration. An invalid/unrestorable snapshot must never reach erase confirmation.
An ordinary JSON export remains available for recovery of legacy data outside import limits,
but clearly warns that the file cannot be restored by this version instead of calling it a
successful restorable backup. Keep raw data intact and messages free of private content.

The snapshot protected by the successful file write must also be the snapshot erased. Add a
transactional DataControlDao erase-if-unchanged operation (expected DataSnapshot, Boolean
result), comparing current data/settings/profile/plan/sessions with the exported snapshot inside
the same transaction. Any intervening change refuses erase and requires a fresh export. Preserve
the existing unconditional DAO primitive for existing tests/internal use; the Settings user
path must use the guarded operation. Concurrent duplicate confirmation cannot erase new data.

R-1 authorizes focused edits to settings export/erase tests and Track/Log note/marker tests
where necessary to assert these exact corrected behaviors; unrelated assertions stay intact.

### R-2 — preserve CSV identities

Natural keys are typed by record kind. Sleep(start,end) and Breathing(start,end) are never
duplicates of one another, either within a file or against stored records.

Keep the eight-column CSV format and existing pipe-separated chip representation. New custom
onset words must reject pipe and disallowed control characters with a visible validation error.
Existing/imported pipe-containing chips remain valid in JSON. Refuse CSV export for such records
with an actionable message to use JSON backup; never silently split or alter a chip. Do not
silently rewrite historical data. R-2 authorizes corresponding SettingsLogic/export tests.

### R-3 — validate every write path and recover capture failures

Every Full Log edit path must enforce parsed timestamp <= now, including rating and chip taps.
An invalid draft remains visible with its validation error and does not write any field.

Normal Track capture, Sleep/Wake capture, onset-chip submit, and Marker writes must catch
non-cancellation database failures, expose a recoverable message, and avoid false success.
Retain marker/chip drafts on failure and close them only after successful persistence. Do not
catch cancellation as an error. If an armed capture has already saved the rating before the
sleep operation fails, explicitly disclose that partial outcome, do not claim nothing changed,
and do not retry the rating automatically. Preserve the existing sleep semantics and DAO
interfaces; atomic capture redesign is outside this bounded repair.

R-3 authorizes focused new failure tests and existing assertion edits only for these paths.

### R-4 — real Safety recovery and Breathing exit

Safety Retry must create a fresh observation after read failure, with at most one active
collector; recovery must show stored plan items and keep reacting to updates. Existing mutation
errors must continue preserving drafts. Test recovery, not merely dismissal of the message.

Breathing Stop ends an active session and stays on the finished view. Close in idle/finished
returns to the previous destination, recording no new session. Add an onClose callback to the
Breathing route/screen (default permitted for source compatibility); wire it to existing back
navigation. System Back behavior and session persistence remain unchanged. R-4 authorizes the
specific Retry test correction and new navigation regression coverage; all existing timing,
isolation, and visual tests remain applicable.

### R-5 — preserve Profile edits during asynchronous refresh

After suspended snapshot/report work, merge against the latest UI/draft state rather than
captured draft fields. Name typing, opening/changing score editors, and pending deletion while
a refresh is suspended must not be overwritten by that refresh. Persisted baseline/conflict
and stale-id logic still applies. Add deterministic suspended-read regression coverage.
No report format, instruments, interpretation, or database schema change.

### R-6 — enforce the local-only promise at the platform boundary

Disable Android automatic cloud backup and explicitly exclude application data from legacy
backup, Android 12+ cloud backup, and device transfer rules. User-initiated JSON/CSV exports
remain available. The no-network manifest is insufficient by itself: platform backup can copy
the database independently. Keep package id and permissions unchanged. Add a source/config
regression test covering manifest and both XML rules. This explicitly supersedes earlier
instructions to leave platform backup configuration unchanged.

### R-7 — evidence and honest product claims

Recheck crisis contact numbers and coverage against operator sources, then update the visible
check date and any date-specific tests. Do not contact crisis services as a test. No claim
about treatment efficacy, diagnosis, or risk assessment is added. A month-old verification date
alone is a maintenance item, not evidence that a number is wrong. Operator evidence checked
2026-09-17 confirms 988 and US TTY 711 then 988. The Canadian operator states texts over a
plan's allowance can incur carrier charges; add that qualification to the resource detail
instead of leaving an unconditional free-texting claim. Date-specific content assertions may
be updated under R-7; no contact action changes.

Review deferred visual/accessibility gaps with the installed API 36 app. Source/semantics tests
do not establish spoken TalkBack quality or real-device readability. Record checks actually
performed. The existing light intensity-ramp direction needs a separate explicit visual
decision if changed; do not silently reverse it during a correctness repair.

### R-8 — scope and completion gates

The Phase 18 acceptance gaps are inputs, not evidence that all missing previews are release
blockers. Prioritize data integrity, failure recovery, privacy and reachable controls. Measure
the flagged long-history derivation risk before a broad algorithm rewrite. Record deferred
cosmetic changes and non-blocking performance work in BACKLOG, not duplicate task trackers.

Prepare an honest beta/release checklist covering physical-device and API 26 compatibility,
accessibility, signing ownership, distribution choice, privacy/store declarations, and any
external assets/contact information still needed. Do not mark the app published or production
ready while those gates remain unverified. No production key or store submission is authorized.

### R-9 — privacy and product information before beta

Add accessible, offline-readable privacy/product information within Settings, using existing
screen components. Explain local storage; no accounts/analytics/app network; automatic backup
disabled; user-controlled exports are unencrypted files and may go to a cloud document provider;
Copy/Share and external safety actions pass selected information to the chosen app; local erase
does not remove exported files or a recipient's copies. State that MindScale is not a medical
device and does not diagnose, treat, cure, or prevent a medical condition, and directs medical
questions to a healthcare professional. No alarmist modal or forced onboarding.

Prepare matching public-policy/store-listing drafts in docs/release, leaving publisher contact
and public hosting as explicit release gates rather than inventing either. This is an explicit
bounded addition to R-8, based on Google Play Health Content and Services requirements verified
2026-09-17. It does not authorize publishing a site or submitting a store listing.

### R-10 — intuitive light-mode intensity direction

The review reconfirmed the documented light-mode inversion: value 1 uses the darker/more
contrasting endpoint and value 10 uses the lighter endpoint. The old direction survived solely
because the visual-only phase could not change an inherited test, not because of a product
requirement. Reverse the light endpoints so intensity increases in visual weight against a
light background. Keep the dark endpoints and every endpoint color unchanged, preserving the
measured minimum contrast, clamping, value-to-text semantics and `(v-1)/9` mapping.

This explicitly supersedes SPEC-insights-visual D-4's light-direction constraint and authorizes
the exact IntensityRampTest monotonicity/endpoint expectations to change to decreasing luminance
for light, increasing for dark. Add assertions of monotonically increasing contrast against
the appropriate background in both themes. Do not edit any unrelated visual assertion or palette.
Inspect the updated light ramp in the installed app as part of the changed-path checks.

## Invariants and failure behavior (all decisions)

- User data never changes because of failed validation/export/import or stale erase confirmation.
- No private record contents in logs/error messages; no extra permission or network capability.
- Existing Room v7 and backup v3–7 compatibility remain intact.
- Failure retains editable drafts; no successful-save message precedes the relevant write.
- Safety and Breathing remain structurally independent of symptom-derived decisions.
- Preserve unrelated .idea/.agents/.codex changes; no commit or remote push without request.

## Machine-checkable acceptance criteria

- R-1: 4,001-code-point new note/marker fails non-destructively; 4,000 succeeds. Legacy invalid,
  oversized and excessive-record exports cannot enable erase; ordinary recovery export warns.
  A changed database between file write and confirmation refuses erase transactionally.
- R-2: matching Sleep/Breathing timestamps import successfully; same-kind duplicates still fail.
  New pipe-containing onset word is refused; legacy pipe-containing CSV export is refused with
  a JSON alternative; JSON retains exact chips.
- R-3: future timestamp + rating/chip tap makes zero writes. Injected capture/marker/chip failure
  leaves usable UI and accurate status; cancellation propagates.
- R-4: Safety failure -> retry -> recovered rows -> further updates works. Idle and finished
  Breathing Close return to origin; active Stop still records once and displays finished.
- R-5: edits interleaved with suspended report generation survive publication.
- R-6: manifest backup flag false and all relevant XML backup/transfer domains excluded.
- R-7: dated source evidence recorded; no emergency service contacted.
- Full wrapper test, lint and assembleDebug pass, plus connectedDebugAndroidTest on API 36.
  Report exact failures/flakes, do not hide them with retries. Existing 440 JVM/268 device tests
  remain the baseline; new regressions supplement them. Focused UI inspection accompanies changes.

## Implementation ownership and test edits

Use independent lower-cost workers for settings/data, Track/Log, and Safety/Breathing/Profile.
Each owns its files and new focused tests; root owns this spec, state and integration evidence.
Each changed pre-existing test must cite the applicable R decision in a nearby comment or report.
Two failed attempts on the same hypothesis require escalation with a compact evidence packet.
No concurrent Gradle runs: root orchestrates verification against the combined tree.
