# MindScale Phase 12 — local import and restore

Status: **FROZEN — APPROVED**

Owner: Claude Code under the user's full Phase 12 delegation

Date: 2026-08-04

Starting commit: `ce5913341461725c3ad59b697a4e994e501fa046`

Approval: On 2026-08-04 the user granted full Phase 12 ownership and authorized product decisions, specification, implementation, verification, commits, pushes, PR operations, merge, and final synchronization without another routine review gate. D-1 through D-12 below are frozen before application-code edits.

## Purpose

Make MindScale's own exports readable again. Phase 4 and Phase 11 produce a JSON backup and a records CSV that are currently one-way; a user who reinstalls, changes devices, or erases has no supported way back. Phase 12 adds exactly two local, explicitly confirmed, atomic import paths and nothing else.

## Product-source reconciliation

`docs/specs/SPEC-settings-data-control.md` (D-5) deferred import until "validation/merge/rollback semantics have their own approved spec". `docs/specs/SPEC-clinician-report-profile.md` (D-9) further required that any import spec "must accept/version-check v5, validate the canonical profile and every external-score invariant, handle duplicate instrument/date rows, and roll back atomically on any failure". `docs/specs/BACKLOG.md` names this as the next ordered item and requires "version validation, conflict/duplicate policy, atomic merge-or-replace behavior, rollback, and hostile-file limits before any file can mutate Room". This spec satisfies each of those constraints and is the governing source for import behavior.

MindScale remains a measurement instrument. Import moves the user's own recorded facts; it never derives, scores, interprets, or repairs them.

## Goals

1. Restore a complete MindScale JSON backup (versions 3, 4, and 5) by replacing everything on the device, atomically.
2. Import a MindScale records CSV additively, atomically, without touching settings, Profile, or external totals.
3. Refuse every malformed, ambiguous, conflicting, duplicate, oversized, unsupported, or future-version file totally, with a factual, actionable, non-destructive message.
4. Show an exact, factual preview of what will change and require explicit confirmation before any mutation.
5. Preserve the external provenance of imported PHQ-8/GAD-7 totals without administering, calculating, comparing, or interpreting them.
6. Preserve deterministic export output so an exported backup can be restored and re-exported identically.

## Non-goals

- No merge, deduplication, reconciliation, or "smart" conflict resolution for JSON backups. JSON restore is replace-only.
- No deletion, replacement, or settings/Profile/external-score mutation from CSV. CSV import is add-only.
- No partial import, no "skip the bad rows", no automatic repair, truncation, rounding, coercion, or re-encoding of any value.
- No import of any format MindScale did not write: no third-party CSV, spreadsheet export, PDF, XML, database file, or clinician summary text.
- No import of the Phase 11 clinician summary `.txt`; it is a rendered artifact, not a record source.
- No file browsing, folder scanning, background/scheduled import, auto-detect-on-launch, "recent files" list, or persisted URI permission grant.
- No account, server, sync, cloud restore, analytics, telemetry, network request, or new permission of any kind.
- No new dependency. The bounded JSON reader and RFC-4180 reader are hand-written, mirroring the existing hand-written encoders.
- No Room schema change, no new entity or column, no migration, no `fallbackToDestructiveMigration`, and no 5→4 downgrade path.
- No Safety card, paced breathing, or UI overhaul work.
- No change to rating, episode, onset, hold, night/nap, clear-day, or sleep-source semantics.

## Frozen scope split (D-1)

Two separately labelled actions in Settings → `Your data`. They never blend.

| | `Restore from backup` | `Import records` |
|---|---|---|
| Source | MindScale JSON backup, `format: "mindscale-backup"` | MindScale records CSV |
| Semantics | **Replace everything** | **Add only** |
| Entries / sleeps / markers | deleted, then replaced by the file's | appended |
| External totals | deleted, then replaced by the file's | untouched |
| Settings row `id=0` | replaced by the file's values | untouched |
| Profile row `id=0` | replaced by the file's display name | untouched |
| Deletes existing data | yes, after explicit confirmation | never |
| Record ids | taken verbatim from the file | assigned by Room |

There is no mode selector. `Restore from backup` never merges; `Import records` never replaces.

## Frozen JSON version policy (D-2)

- Accepted: `format == "mindscale-backup"` and integer `version` in `{3, 4, 5}` — every version MindScale has ever written.
- `version > 5`: rejected with `This backup was made by a newer version of MindScale. Update MindScale, then try again.` Nothing is read further and nothing is imported.
- `version < 3`, non-integer `version`, missing `version`, missing/other `format`: rejected as an unsupported file.
- Fields a given version could not contain take documented current defaults, and every such default is disclosed verbatim in the preview before confirmation. Defaulting is never silent.

Disclosed defaults:

| Absent in | Field | Restored value |
|---|---|---|
| every version (3, 4, 5) | `checkinAt` | `0` |
| every version (3, 4, 5) | `sleepIntroShown` | `false` |
| every version (3, 4, 5) | `anchorPromptDone` | `false` |
| 3 | `holdHours` | `SIXTEEN` (16 waking hours) |
| 3, 4 | `profile.displayName` | `""` |
| 3, 4 | `externalScores` | none restored |

Every field a version *does* contain is mandatory. A version-5 file missing `profile` is rejected, not defaulted.

## Frozen record identity, duplicate, and conflict rules

### JSON restore (ids present)

- Every `id` must be present, an integer, and `>= 1`.
- Ids must be unique within each collection. A duplicate id rejects the file.
- Ids are inserted verbatim. The destination tables are emptied inside the same transaction, so no collision with existing data is possible. This preserves `external_scores` audit identity and makes export-after-restore byte-reproducible.
- `(instrument, assessedEpochDay)` must be unique across `externalScores`; a duplicate rejects the file (it would violate the unique index).

### CSV import (no ids) (D-4)

Natural identity per record type:

- rating: `(ts, value, chips, note, kind)`
- sleep: `(startTs, endTs)`
- marker: `(ts, text)`

Rules, each total:

- **(a) Duplicate within the file** — two rows with the same natural identity: reject the whole file, naming the duplicate count. Never deduplicate.
- **(b) Identical to an existing stored record**: reject the whole file, naming the conflict count. Never skip. This intentionally means re-importing a CSV that was already imported fails entirely; that is the correct outcome under the no-silent-skip boundary and the message says so plainly.
- **(c) Sleep structure**: reject if any imported interval overlaps another imported interval or any stored interval (half-open `[startTs, endTs)`, an open interval spanning `[startTs, ∞)`), or if the file contains more than one open interval, or contains an open interval while the database already holds one. `endTs`, when present, must be `>= startTs`.

The same sleep structural rules apply to a JSON restore, evaluated within the file alone (the database is emptied first).

Conflict and structure checks are computed for the preview against a transactional read, **and re-verified inside the mutating transaction**. If the stored records changed while the preview was open, the transaction rolls back and reports `Your records changed while this preview was open. Nothing was imported.`

## Frozen limits (D-5)

Enforced before unbounded allocation; every violation rejects the whole file.

| Limit | Value |
|---|---|
| Maximum file size | 8 MiB (8,388,608 bytes), counted while streaming; reading stops at the first byte past the limit |
| Maximum total records | 50,000 |
| Maximum records per collection / per record type | 20,000 |
| Maximum JSON nesting depth | 8 |
| Maximum JSON values parsed | 2,000,000 |
| Maximum CSV rows (excluding header) | 50,000 |
| CSV fields per row | exactly 8 |
| `note` | ≤ 4,000 Unicode code points |
| marker `text` | ≤ 4,000 Unicode code points |
| chips per entry | ≤ 20; each chip 1–32 code points, non-blank |
| `anchor2` / `anchor5` / `anchor8` | ≤ 160 code points |
| `onsetChips` | ≤ 20 items; each 1–32 code points; case-insensitive unique under `Locale.ROOT` |
| `displayName` | ≤ 80 code points |
| `intensity` | integer 0–10 |
| external `total` | integer 0–24 (PHQ-8) / 0–21 (GAD-7) |
| `holdHours` | integer in `{8, 12, 16, 24}` |
| Instant fields (`timestamp`, `start`, `end`, `enteredAt`, `exportedAt`) | strict ISO-8601 instant in `[1970-01-01T00:00:00Z, now + 24h]` |
| `assessedDate` | strict ISO-8601 local date in `[1970-01-01, today in the device's current zone]` |

Any integer literal that overflows `Long`, any non-integer where an integer is required, any JSON number with a leading `+`, a leading zero, an exponent, or a fractional part where an integer is required, and `NaN`/`Infinity` reject the file. The `now + 24h` upper bound exists only to tolerate clock skew between the exporting and importing device; it is a bound, not a correction.

## Frozen text, encoding, and structure rules (D-6)

- The byte stream must decode as strict UTF-8. Malformed or unmappable sequences reject the file; `U+FFFD` is never substituted.
- A single leading UTF-8 BOM is accepted and stripped. A BOM anywhere else rejects the file.
- **Control characters.** `note` and marker `text` may contain `TAB (U+0009)`, `LF (U+000A)`, and `CR (U+000D)`, because Track stores multi-line notes and those characters round-trip through both encoders. Every other C0 control (`U+0000`–`U+001F` excluding those three) and `U+007F` rejects the file. Single-line fields — `displayName`, `anchor2/5/8`, each `onsetChips` item, and each entry chip — reject **all** control characters. This is not decoration: entry chips are persisted joined by `U+001F`, so a chip containing it would silently corrupt the stored list.
- JSON: object keys must be exactly the expected set for the declared version — unknown, extra, missing, or duplicate keys reject the file. Unknown enum names reject the file. `null` where the model is non-nullable rejects the file. Trailing content after the top-level value rejects the file. Rejecting unknown keys is safe rather than a forward-compatibility trap because MindScale controls every version it writes and any genuinely new shape bumps `version`, which is already refused above 5.
- CSV: the first line must be exactly `record_type,timestamp,end_timestamp,intensity,kind,chips,note,text`. Every row must have exactly 8 fields with RFC-4180 quoting (`"` doubles inside a quoted field; embedded `,`, `CR`, and `LF` are legal only inside a quoted field). `record_type` must be exactly `rating`, `sleep`, or `marker`. Fields not applicable to a row type must be empty; a non-empty inapplicable field rejects the file. Chips are `|`-joined; an empty chips field means no chips.
- CSV row terminators: both `CRLF` and bare `LF` are accepted. This is the single deliberate leniency, justified because line-ending rewriting by editors and transfer tools cannot change any field value. A final terminator is optional. A bare `CR` terminator is rejected.

## Frozen preview and confirmation copy (D-7)

Nothing mutates until the user confirms the preview. The preview is rendered only from the fully validated in-memory parse.

### `Restore from backup`

```text
Restore from backup

This file is a MindScale backup, version {V}, created {exportedAt}.

It contains {E} ratings, {S} sleep periods, {M} marked events, and {X}
externally obtained totals, plus your settings and Profile name.

Restoring replaces everything currently on this device. MindScale will
permanently delete {e} ratings, {s} sleep periods, {m} marked events, and
{x} externally obtained totals, and will replace your settings and Profile
name.

Check-in time, the sleep introduction flag, and the anchor prompt flag are
not stored in any backup file. They return to their defaults.

Nothing has changed yet.
```

Appended only when applicable:

- version 3: `This backup predates the entry-hold setting. The hold returns to 16 waking hours.`
- version 3 or 4: `This backup predates Profile and externally obtained totals. Your Profile name will be empty and no totals will be restored.`
- `{X} > 0`: `Totals entered by you from results obtained elsewhere. MindScale did not administer or calculate them.`

Actions: `Cancel` and `Replace everything`.

### `Import records`

```text
Import records

This file is a MindScale records CSV.

It will add {E} ratings, {S} sleep periods, and {M} marked events.

Nothing is deleted. Your settings, Profile name, and externally obtained
totals are not changed. A records CSV does not contain them.

Nothing has changed yet.
```

Actions: `Cancel` and `Add these records`.

### Result copy

- Restore: `Restored {E} ratings, {S} sleep periods, {M} marked events, and {X} externally obtained totals.`
- Import: `Added {E} ratings, {S} sleep periods, and {M} marked events.`

No preview or result text may contain severity, threshold, comparison, reassurance, diagnosis, interpretation, or any claim about what the data means. Counts and format facts only.

## Frozen external-score provenance rules (D-8)

- `provenance` must be present and exactly `EXTERNALLY_OBTAINED_USER_ENTERED`. Any other value, or an absent field in a version-5 file, rejects the file.
- MindScale re-asserts the same fixed provenance on every stored row; provenance is never inferred from the file.
- `instrument` must be exactly `PHQ_8` or `GAD_7`; `total` must be within that instrument's frozen range; `assessedDate` must be a real local date not later than today in the device's current zone.
- Import performs no arithmetic, comparison, ranking, trend, threshold, or severity labelling on any total, and attaches no severity language anywhere.
- The records CSV contains no external totals and importing one never creates, changes, or deletes any.

## Frozen restoration and saved-state rules (D-9)

- Raw file bytes, decoded text, parsed records, and preview payloads are **never** written to `SavedStateHandle`, `rememberSaveable`, disk, logs, or any other persisted store. `SavedStateHandle` is a system-persisted Bundle and is the wrong home for untrusted health data.
- Only two primitives are saved: the in-flight import action (`ImportKind` enum name) and a boolean recording that a preview was pending.
- On Activity recreation with a preview pending, the parsed payload is discarded and the user re-picks the file, with the exact message `Choose the file again to see the preview.` Persisting a bare `OpenDocument` URI is rejected: without `takePersistableUriPermission` it is frequently unreadable after process death, trading a clear prompt for an intermittent silent failure.
- The launcher, preview dialog, error text, and result message survive rotation as ordinary UI state within a live process.

## Frozen atomicity, invariants, and rollback (D-10)

The approved mutation is exactly one Room `@Transaction` method per kind. It succeeds completely or changes nothing.

Post-mutation checks inside the transaction, each of which throws and therefore rolls back:

1. Exactly one `track_settings` row with `id = 0` exists, and its reset reported one affected row.
2. Exactly one `user_profile` row with `id = 0` exists, and its reset reported one affected row.
3. Inserted row counts equal the approved counts for every collection, and every returned row id is `> 0`.
4. The count of `sleeps` rows with `endTs IS NULL` is `<= 1`.
5. For CSV import, the conflict/duplicate/overlap re-verification against the current stored records still passes.
6. A `UNIQUE(instrument, assessedEpochDay)` constraint violation is caught as a rollback-and-reject path, never an uncaught crash.

Additional invariants:

- No import path calls `fallbackToDestructiveMigration`, deletes the database file, or adds a downgrade migration. Phase 11 D-12 stands unchanged: an old binary must fail closed on schema 5, and operational rollback is forward-fix only.
- A failed or cancelled import leaves every row, id, timestamp, setting, and Profile field byte-identical to its pre-import value.
- Export-after-restore: restoring a version-5 backup whose every field is in range and then exporting produces a JSON backup byte-identical to the input except its `exportedAt` value. Ordering is preserved because the exporter's `ts DESC, id DESC` ordering and the importer's verbatim ids agree.
- After a successful JSON restore, stale sensitive UI state is cleared: Settings anchor/onset-word drafts are dropped from `SavedStateHandle` and reseeded from the restored settings, and Profile/Report drafts are cleared through the existing post-erase clearing path. Navigation is **not** reset; the user stays in Settings and reads the result message. A CSV import clears nothing, because it changes no settings, Profile, or draft-backed value.
- Import adds no destructive gate of its own beyond the preview. It deliberately does not copy Phase 4's export-first erase gate: erase destroys data and leaves nothing, whereas restore replaces data with the file the user just chose and most commonly runs against an empty install. The exact deletion counts in the preview are the safety mechanism.

## Frozen picker, URI, and cancellation rules (D-11)

- `ActivityResultContracts.OpenDocument`. JSON filter `["application/json"]`; CSV filter `["text/csv", "text/comma-separated-values", "text/plain"]`, because document providers routinely mislabel CSV. A permissive filter is safe because content is fully validated regardless of declared MIME type.
- The URI is opened read-only through `contentResolver.openInputStream` inside a cancellable IO coroutine. No permission grant is persisted, no write mode is requested, and no storage permission is declared.
- Picker cancellation is neutral: no error, no message, no state change.
- Open/read failure reports `Could not read that file. Choose it again to retry.` — no filename, no path, no content.
- Cancelling during read/parse discards everything and leaves the database untouched.

## Frozen interfaces and data contracts

Package: existing `com.kieslingdev.mindscale.settings` (import lives beside `DataExport.kt`; `import` is a Kotlin keyword and cannot be a package segment). Pure logic only — no Android types in the parsers.

```kotlin
enum class ImportKind { BACKUP_RESTORE, RECORDS_MERGE }

data class BackupPayload(
    val version: Int,
    val exportedAt: Instant,
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>,
    val settings: TrackSettings,
    val profile: UserProfile,
    val externalScores: List<ExternalScore>
)

data class RecordsPayload(
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>
)

sealed interface ImportPayload {
    data class Restore(val backup: BackupPayload) : ImportPayload
    data class Merge(val records: RecordsPayload) : ImportPayload
}

data class ImportPreview(
    val kind: ImportKind,
    val title: String,
    val lines: List<String>,
    val confirmLabel: String
)

sealed interface ParseResult<out T> {
    data class Ok<T>(val value: T) : ParseResult<T>
    data class Rejected(val message: String) : ParseResult<Nothing>
}

// Pure, deterministic, Android-free.
fun parseBackup(text: String): ParseResult<BackupPayload>
fun parseRecordsCsv(text: String): ParseResult<RecordsPayload>
fun checkRecordConflicts(payload: RecordsPayload, existing: RecordSnapshot): ParseResult<RecordsPayload>
fun previewOf(payload: ImportPayload, existing: RecordCounts): ImportPreview

// Bounded reader: counts bytes, aborts past MAX_IMPORT_BYTES, decodes strict UTF-8, strips one BOM.
fun readBoundedUtf8(stream: InputStream): ParseResult<String>
```

Added to `DataControlDao`:

```kotlin
data class RecordSnapshot(
    val entries: List<Entry>,
    val sleeps: List<SleepInterval>,
    val markers: List<Marker>
)

data class ImportCounts(
    val entries: Int, val sleeps: Int, val markers: Int, val externalScores: Int
)

@Transaction suspend fun recordSnapshot(): RecordSnapshot
@Transaction suspend fun replaceEverything(payload: BackupPayload): ImportCounts
@Transaction suspend fun addRecords(payload: RecordsPayload): ImportCounts
```

`SettingsViewModel` additions (all state immutable and published whole):

```kotlin
data class PendingImport(val kind: ImportKind, val payload: ImportPayload, val preview: ImportPreview)

// added to SettingsUiState
val importLaunch: ImportKind? = null,
val importing: Boolean = false,
val pendingImport: PendingImport? = null,
val importError: String? = null

fun requestBackupRestore()
fun requestRecordsImport()
fun importPickerCanceled()
fun importFileSelected(kind: ImportKind, open: suspend () -> InputStream)
fun cancelImport()
fun confirmImport()
fun dismissImportError()
```

Composables never parse, validate, read Room, or resolve conflicts. `SettingsRoute` owns only the `OpenDocument` launchers and hands the ViewModel a suspending stream opener.

Room schema, entities, converters, migrations, exported schema JSON, `encodeBackup`, `encodeRecordsCsv`, filenames, permissions, dependencies, toolchain, navigation, and manual DI are unchanged.

## Privacy and logging

- No `android.util.Log`, `println`, stack-trace print, crash-report field, or analytics event in any import path.
- Filenames, URIs, file contents, parsed records, health data, validation payloads, and offending values are never logged and never appear in an error message. Structural JSON errors may name a line and column, which is structure, not content.
- All work is local. No network call, no new permission, no persisted URI grant, no file written outside the user-chosen export path that already exists.

## Failure behavior

Every failure below is total, non-destructive, retryable, and leaves the database unchanged.

| Condition | Message |
|---|---|
| File larger than 8 MiB | `This file is larger than 8 MB. MindScale did not read it.` |
| Not valid UTF-8 | `This file is not valid UTF-8 text.` |
| Not a MindScale backup | `This is not a MindScale backup file.` |
| `version > 5` | `This backup was made by a newer version of MindScale. Update MindScale, then try again.` |
| `version < 3` or unusable | `This backup version is not supported.` |
| Malformed JSON | `This backup is not valid JSON (line {L}, column {C}).` |
| Structural/semantic JSON violation | `This backup does not match the MindScale backup format.` |
| Value outside a frozen bound | `This file contains a value MindScale cannot store.` |
| Too many records | `This file contains more records than MindScale can import.` |
| Not a MindScale records CSV | `This is not a MindScale records CSV.` |
| Malformed CSV row | `This CSV does not match the MindScale records format (line {L}).` |
| Duplicates within the file | `This file contains {N} duplicate records. Nothing was imported.` |
| Conflicts with stored records | `{N} of these records are already in MindScale. Nothing was imported.` |
| Overlapping sleep periods | `This file contains overlapping sleep periods. Nothing was imported.` |
| Would leave two open sleep periods | `This file would leave more than one sleep period open. Nothing was imported.` |
| Stored records changed during preview | `Your records changed while this preview was open. Nothing was imported.` |
| URI open/read failure | `Could not read that file. Choose it again to retry.` |
| Transaction failure | `Could not import this file. Nothing on this device was changed.` |
| Preview lost to recreation | `Choose the file again to see the preview.` |
| Picker cancelled | (silent; neutral) |

## Accessibility, Android compatibility, and lifecycle

- Minimum SDK 26, target 36, compile 36.1, Kotlin, Compose, Material 3, Kotlin DSL, wrapper, bundled JDK: all unchanged. No dependency added.
- Both import actions are ≥ 48 dp, have visible labels and explicit accessible names, and appear only in Settings → `Your data`.
- The preview is an `AlertDialog` whose body scrolls, so it remains fully readable at 200% font scale, in landscape, and with the IME visible; no text is clipped or truncated.
- The confirm action names the outcome (`Replace everything` / `Add these records`), never a bare `OK`.
- Progress, result, and error text are announced through a polite live region and are associated with the action, not conveyed by color alone.
- Light and dark themes both render preview and error text at the existing gold/ink contrast level.
- Reading and parsing run in `viewModelScope` on `Dispatchers.IO`, are cancellable, and never block the main thread. Cancellation is silent and non-destructive.
- Activity recreation during the picker returns the result to the recreated launcher; recreation while a preview is pending discards the payload and prompts a re-pick (D-9).
- Everything works offline, with zero records, and after process death.

## Test and verification contract

### JVM (pure) tests

- Bounded reader: exact-limit and over-limit files, BOM accepted once, misplaced BOM rejected, invalid UTF-8 rejected without `U+FFFD`, empty stream rejected.
- JSON reader: nesting depth at and over 8, duplicate keys, unknown/missing keys, trailing content, `NaN`/`Infinity`, leading zeros, exponents, `Long` overflow, unterminated string/array/object, `\u` escapes and surrogate pairs.
- Backup validation: versions 3, 4, 5 accepted with the exact disclosed defaults; 2 and 6 rejected with their exact distinct messages; non-integer version rejected; every bound in the limits table exercised at accept/reject boundary; every control-character rule including a chip containing `U+001F`; every enum; duplicate ids; duplicate `(instrument, assessedEpochDay)`; provenance mismatch; `total` at 0/24/25 for PHQ-8 and 0/21/22 for GAD-7; future timestamps and future assessment dates.
- CSV reader: exact header required; 7/9-field rows; RFC-4180 quote doubling; embedded comma, `CR`, `LF` inside quotes; `CRLF` and `LF` terminators; bare `CR` rejected; optional final terminator; non-empty inapplicable field; unknown `record_type`; multi-line note round-trip.
- Conflict rules: duplicate within file, conflict with stored record, sleep overlap (touching intervals do not overlap), two open intervals in file, one open in file with one already stored, `endTs < startTs`.
- Preview: exact expected text for empty database, populated database, version 3, version 4, version 5 with and without external totals; a banned-word scan over every preview, result, and error string.
- ViewModel: launch, cancellation, read failure, parse rejection, preview publication, confirm success, confirm failure, retry, discard-on-recreation, and proof that no raw content reaches `SavedStateHandle`.
- Export-after-import: a version-5 fixture restored and re-encoded is byte-identical except `exportedAt`.

### Room and instrumented tests

- `replaceEverything` on an empty and on a populated database: exact resulting rows, verbatim ids, settings and Profile replaced, external totals replaced.
- `addRecords`: appended rows only; settings, Profile, and external totals unchanged; assigned ids positive and non-colliding.
- Rollback: forced failure at each post-mutation check (missing canonical settings row, missing canonical profile row, count mismatch, two open sleeps, unique-constraint violation, mid-transaction conflict) leaves every table byte-identical.
- Existing schema-5, migration, export, erase, navigation, Track, Log, Insights, and Profile/Report suites still pass unchanged.
- Compose: both actions present and reachable, preview dialog content and scrollability, confirm and cancel paths, error and result live regions, 48 dp targets, picker cancellation, and URI read failure.

### Full oracles

Run exactly:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

Then inspect the installed app on the API 36 emulator: valid JSON restore; valid CSV import; empty, malformed, oversized, unsupported, future-version, duplicate, and conflicting files; cancellation before confirmation; successful atomic mutation; forced failure with original data unchanged; Profile/settings/external totals after restore; export after import; erase/reset after import; light and dark; 200% text; landscape; IME and focus; Activity recreation; accessibility and privacy-sensitive content. Restore emulator theme, font scale, and orientation afterwards.

## Machine-checkable acceptance criteria

- [ ] SPEC: this spec and D-1 through D-12 are frozen in a documentation-only commit before any application-code edit.
- [ ] SCOPE: exactly two import actions exist; JSON is replace-only, CSV is add-only, and no code path blends them.
- [ ] VERSION: 3, 4, and 5 are accepted with the exact disclosed defaults; `> 5` and `< 3` are rejected with their distinct frozen messages; nothing is imported on rejection.
- [ ] LIMITS: every value in the limits table is enforced before unbounded allocation, at the exact boundary, and rejects totally.
- [ ] ENCODING: strict UTF-8, single leading BOM, the frozen control-character policy, RFC-4180 quoting, `CRLF`/`LF` terminators, and reject-unknown-keys all behave exactly as frozen.
- [ ] IDENTITY: duplicate-in-file, conflict-with-stored, and sleep-overlap/open-interval rules reject totally, with exact counts and no silent skip, repair, or dedupe.
- [ ] PREVIEW: the exact frozen preview, confirm-label, result, and error strings are produced, contain no banned language, and no mutation occurs before confirmation.
- [ ] PROVENANCE: imported totals require and re-assert `EXTERNALLY_OBTAINED_USER_ENTERED`; no scoring, comparison, threshold, or severity language exists on any import path.
- [ ] ATOMICITY: all six post-mutation checks roll back on violation, and every failure path leaves the database byte-identical.
- [ ] RESTORATION: no raw content reaches `SavedStateHandle`; only the two frozen primitives are saved; recreation discards the payload and prompts a re-pick.
- [ ] COMPATIBILITY: a restored version-5 backup re-exports byte-identically except `exportedAt`; schema, migrations, encoders, permissions, dependencies, and toolchain are unchanged.
- [ ] PRIVACY: no logging of filenames, content, health data, or validation payloads exists on any import path.
- [ ] ACCESSIBILITY: 48 dp targets, accessible names, scrollable preview, live-region status, light/dark, 200% font, landscape, and IME behavior are verified.
- [ ] ORACLES: `test`, `lint`, `assembleDebug`, `adb devices -l`, `connectedDebugAndroidTest`, and `git diff --check` pass from the verified implementation head.
- [ ] REVIEW: one critical-path review covers hostile-file resistance, parser/version correctness, conflict semantics, atomicity/rollback, canonical-row integrity, provenance, privacy, URI lifecycle, restoration/concurrency, accessibility, export compatibility, and rollback; every blocking finding is resolved.

## Task decomposition

1. Freeze this spec, D-012, `PROJECT_STATE.md`, and the backlog removal — oracle: documentation-only `git diff --check`.
2. Bounded UTF-8 reader, pure JSON reader, and limit constants — oracle: focused JVM reader tests.
3. Backup validation for versions 3/4/5 with disclosed defaults — oracle: focused JVM backup tests.
4. RFC-4180 CSV reader and records validation — oracle: focused JVM CSV tests.
5. Identity/duplicate/conflict/sleep-structure rules and preview encoder — oracle: focused JVM conflict and preview tests.
6. `DataControlDao` transactional `recordSnapshot`, `replaceEverything`, `addRecords` with all six checks — oracle: focused Room instrumented tests.
7. `SettingsViewModel` import state machine and saved-state discipline — oracle: focused JVM ViewModel tests.
8. `SettingsRoute`/`SettingsScreen` picker, preview dialog, and status UI — oracle: focused Compose instrumented tests.
9. Full oracles, installed-app matrix, critical review, and fixes — oracle: the completion checklist above.

## Rollout, migration, and rollback

No schema change, no migration, and no feature flag. Import is additive application behavior on the existing schema-5 database. Rollback before publication is a source-control revert; after publication it is forward-fix, exactly as Phase 11 D-12 froze. No destructive downgrade fallback is added, and an older binary must continue to fail closed on schema 5 rather than mutate it. Saved backups stop being one-way export evidence and become restorable inputs as of this phase.

## Decisions and approval gate

- **D-1 (scope split):** JSON backup restores by replacing everything; records CSV imports additively; the two never blend and there is no mode selector.
- **D-2 (version policy):** accept backup versions 3, 4, and 5 with every absent field's default disclosed verbatim before confirmation; reject `> 5` and `< 3` totally with distinct messages.
- **D-3 (id policy):** JSON restore inserts the file's ids verbatim into emptied tables, requiring present, positive, unique ids, so audit identity and byte-reproducible export survive.
- **D-4 (identity/conflict):** natural-tuple identity for CSV; in-file duplicates, conflicts with stored records, sleep overlaps, and multiple open intervals each reject the whole file with an exact count and never skip, dedupe, or repair.
- **D-5 (limits):** the frozen limits table is enforced before unbounded allocation and every violation is total.
- **D-6 (text/encoding):** strict UTF-8, one leading BOM, multi-line `note`/marker text but no other control characters, no control characters at all in single-line fields, exact CSV header and field count, RFC-4180 quoting, `CRLF`/`LF` leniency only, and reject-unknown-keys.
- **D-7 (preview/confirmation):** the exact frozen factual preview and confirm labels are mandatory, mutation happens only after confirmation, and no interpretive or reassuring language is permitted.
- **D-8 (provenance):** imported PHQ-8/GAD-7 totals require and re-assert fixed external provenance; MindScale performs no scoring, comparison, or interpretation on them.
- **D-9 (restoration):** only an action enum and a pending-preview boolean are saved; raw content never enters saved state; recreation discards the payload and prompts a re-pick.
- **D-10 (atomicity):** one checked Room transaction per kind with all six post-mutation checks; any violation rolls back and the database stays byte-identical; no destructive migration fallback or downgrade path is added.
- **D-11 (picker/URI):** read-only single-use `OpenDocument` with permissive MIME plus mandatory content validation, no persisted grant, neutral cancellation, and factual read-failure retry.
- **D-12 (privacy/logging):** no filename, URI, content, health data, or validation payload is ever logged; errors describe rules and structure, never values.

Approval gate is satisfied. D-1 through D-12 are frozen; a genuine source-of-truth conflict or material frozen-interface change requires a documented amendment before implementation continues.

## Required durable-document updates

### Before application-code edits

- Commit this `FROZEN — APPROVED` spec, `docs/DECISIONS.md` D-012, active Phase 12 state in `PROJECT_STATE.md`, and the backlog removal as documentation only.

### After implementation and verification

- Mark this spec `IMPLEMENTED — VERIFIED LOCALLY`, check the acceptance criteria, add exact oracle/manual/review evidence, and update `PROJECT_STATE.md` before publication.
- After merge and main synchronization, add and push a phase-boundary documentation commit on `main` recording the merge/PR/final hash and the remaining backlog order.
