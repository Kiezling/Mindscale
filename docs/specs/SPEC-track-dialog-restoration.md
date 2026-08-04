# MindScale Phase 7 — Track dialog process restoration

Status: IMPLEMENTED — VERIFIED LOCALLY

Owner: Codex

Date: 2026-08-04

Approval: On 2026-08-04, the user approved the complete Phase 7 specification and D-1 through D-10, authorized implementation without further approval gates, and delegated lead responsibility through completion.

Governing sources:

- `docs/specs/SPEC-track-numpad-logging.md`, especially the backdate/edit/note behavior, future-time validation, and original restoration claim
- `docs/specs/SPEC-track-phase2-completeness.md`, especially the corrected marker `SavedStateHandle` precedent and Sleep/Wake capture invariants
- `docs/specs/SPEC-full-log.md`, especially primitive restoration values, affected-row-count stale handling, targeted Entry mutations, and mutation-failure behavior
- `docs/specs/SPEC-settings-data-control.md`, especially the saved primitive draft pattern, local-only architecture, and no-stale-snapshot rule
- `docs/specs/SPEC-insights-foundation.md` and `docs/specs/SPEC-insights-entry-chart.md`, especially schema/backup version 4, one-source derivation, navigation restoration, and no-framework constraints
- Android's official saved-state guidance: [`SavedStateHandle`](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate) and [saving UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving)

Approval gate: satisfied on 2026-08-04. D-1 through D-10 are frozen for implementation.

## Purpose

Correct the known Track restoration gap without expanding product scope. If the system destroys and later reconstructs `MainActivity` and `TrackViewModel`, an open backdate, Entry edit, or note dialog must reopen with the user's exact unsaved draft. The restored dialog must resolve its Entry identity against current Room data, preserve targeted mutations, and fail honestly when the target is stale or persistence fails.

Phase 1's Compose-only `rememberSaveable` buffers are insufficient by themselves: `TrackViewModel` currently reconstructs with `backdateDialog`, `editDialog`, and `noteDialog` all null, so the owning conditional dialog composition is not recreated. Phase 2's marker fix already establishes the repository precedent: the ViewModel must save the owning open state and draft primitives in `SavedStateHandle`.

## Current implementation reconciliation

- `MainActivity` already creates `TrackViewModel` with `extras.createSavedStateHandle()` through manual DI. No Activity or factory architecture change is needed.
- Marker visibility and text use `track.markerOpen` and `track.markerDraft`; a focused ViewModel test reconstructs the marker state. These keys and behavior remain unchanged.
- Backdate and edit date/time strings are held only by `rememberSaveable` inside `TimestampEditDialog`; their ViewModel models hold only the last successfully parsed epoch milliseconds. A partially typed or invalid string is not represented in `TrackUiState`.
- Note text is mirrored to `TrackViewModel`, but neither the owning note dialog nor its text is written to `SavedStateHandle`.
- `EditEntryState` and `NoteEditState` retain a full `Entry` snapshot. Phase 3 later made that unnecessary for persistence by introducing `updateEditableFields(id, ...)` and `updateNote(id, ...)`; Room entities must not be copied into saved state.
- The four nullable fields `backdateDialog`, `editDialog`, `noteDialog`, and `pendingDelete` can represent multiple simultaneous modal dialogs even though normal touch interaction makes that state difficult to reach.
- Track currently closes Edit, Note, and Delete before Room reports success and does not catch their DAO exceptions. That contradicts Phase 3's frozen rule that a meaningful mutation failure retains a retryable surface and never claims success.
- Track's recent list is limited to ten Entries, so restoration and stale validation cannot depend on `recentEntries` containing the referenced id.

## Goals

1. Restore an open Track backdate, Entry edit, or note dialog after Activity saved-state recreation and after construction of a new `TrackViewModel`.
2. Restore every user-editable field exactly, including partially typed invalid date/time text, edit chip order/selection, note whitespace/newlines, target id, and Backdate's armed Sleep/Wake meaning.
3. Make all Track modal dialogs mutually exclusive by construction.
4. Revalidate restored Entry ids from Room without depending on the top-ten recent list or saving Room entities in a Bundle.
5. Preserve the user's draft when the same record changed elsewhere, and prevent silent same-field conflict.
6. Preserve affected-row-count targeted mutations and their unrelated-column concurrency protection.
7. Clear saved draft keys on explicit cancellation/dismissal and only after the relevant persistence outcome is known.
8. Add focused deterministic tests that distinguish ordinary Activity recreation from a rebuilt ViewModel/process-restoration simulation.

## Non-goals

- No restoration of delete confirmation, transient readout, toast, help card, onset-chip prompt, check-in banner, Settings focus, Insights exploration, or marker state beyond its already-implemented behavior.
- No change to Full Log's inline restoration model.
- No new Entry field, revision column, operation id, persisted draft table, temporary file, or Room schema migration.
- No exactly-once guarantee for an abrupt process kill in the instruction-level interval between a new Entry transaction committing and its saved-state keys being cleared. This phase covers a draft saved before submission and normal Room success/failure completion; closing that crash window would require a persisted idempotency token and a separately approved schema/data-integrity design.
- No optimistic-lock/CAS redesign of every Track/Log mutation. Phase 7 detects same-field changes for an open Track draft and warns before overwrite; it does not claim to serialize two independent editors that save the same columns at the same instant.
- No change to onset, episode, hold, sleep-union, chart, export, erase, or backup semantics.
- No note-length/product-copy redesign. Draft restoration is subject to Android's shared saved-state Bundle size; Phase 7 does not truncate existing or newly typed note text.
- No navigation, DI, repository, persistence, or state-management framework.
- No dependency, Gradle, AGP, Kotlin, Compose, Java, SDK, or toolchain upgrade.

## User experience

### Backdate

- Long-pressing a rating opens the existing Backdate dialog with the chosen value and current local date/time.
- The exact date and time strings survive restoration, including a partial or invalid draft. The inline validation message is re-derived; Save remains disabled until the strings resolve to a valid local time not after `nowProvider()`.
- If Sleep or Wake was armed when Backdate opened, that capture kind is part of the saved dialog draft. Restoration preserves the pad's armed meaning. Cancel/dismiss returns to that armed state; a successful save consumes it exactly once under the existing Phase 2 rules.
- A persistence exception before the Entry is recorded keeps the dialog and draft open and exposes `Could not save that entry. Please try again.`
- Once an Entry insert/ordinary-capture transaction succeeds, the Backdate draft is cleared. For an armed Sleep/Wake capture, a later exceptional sleep-bookkeeping failure reports that the rating was saved but the sleep update failed; it does not reopen/retry the whole dialog and risk inserting a duplicate rating. Existing `AlreadyOpen`/`NothingOpen` outcomes remain normal, specified outcomes rather than exceptions.

### Entry edit

- The exact target id, value, local date text, local time text, ordered chip selection, and baseline editable fields survive restoration.
- Save is disabled while the restored target is being resolved from Room or while a mutation is in flight.
- If the target no longer exists, the dialog and all saved keys close and the app says `That record no longer exists`.
- If the current Entry's `ts`, `value`, or `chips` changed from the saved baseline, the user's draft remains intact and the dialog shows: `This rating changed elsewhere. Saving will replace its current value, time, and chips. Cancel and reopen to use the latest record.` Save is an explicit `Save my changes`; Cancel/dismiss discards the draft. A change only to `note` or `kind` is not a conflict because `updateEditableFields` does not write those columns.
- Save continues to call only `EntryDao.updateEditableFields`. Affected-row count 1 clears the dialog/saved keys. Count 0 clears as stale. An exception retains the exact draft, re-enables Save, and exposes `Could not update that rating. Please try again.`

### Note edit

- The exact target id, baseline note text, and draft note text survive restoration, including whitespace and newlines. Blank-save normalization remains the existing Track behavior (`null` for blank content).
- If the target disappears, stale behavior matches Entry edit.
- If the current note differs from the saved baseline, keep the draft and show: `This note changed elsewhere. Saving will replace the current note. Cancel and reopen to use the latest note.` Save becomes `Save my changes`. Changes to value, timestamp, chips, or kind are not conflicts because `updateNote` writes only `note`.
- Count 1 clears; count 0 clears as stale; an exception retains the draft and exposes `Could not save that note. Please try again.`

### Dismissal and navigation

- Cancel, system Back, and outside-tap dismissal are the same explicit cancel event for the active modal. They remove every Phase 7 dialog key and perform no DAO mutation.
- A Track modal prevents normal interaction with underlying Track/navigation controls. If an external/test navigation state change hides Track anyway, navigation alone does not discard the draft; returning to Track presents it again after validation.
- Android saved state is task-stack state, not durable user data. Force-stop, removing the task from Recents, clearing app data, or uninstalling may discard the draft and is outside this guarantee. Persisted Room records remain unaffected.

## Frozen dialog model

Replace independently nullable modal fields with one discriminated state. Equivalent naming is allowed, but the shape and invariants are frozen:

```kotlin
sealed interface TrackModalState {
    data class Backdate(
        val draft: BackdateDraft,
        val isSaving: Boolean = false
    ) : TrackModalState

    data class Edit(
        val draft: EditEntryDraft,
        val validation: RecordValidation = RecordValidation.Checking,
        val isSaving: Boolean = false,
        val message: String? = null
    ) : TrackModalState

    data class Note(
        val draft: NoteEntryDraft,
        val validation: RecordValidation = RecordValidation.Checking,
        val isSaving: Boolean = false,
        val message: String? = null
    ) : TrackModalState

    data class Delete(val entry: Entry) : TrackModalState
}

data class BackdateDraft(
    val value: Int,
    val dateText: String,
    val timeText: String,
    val captureKind: EntryKind?
)

data class EditEntryDraft(
    val entryId: Long,
    val baselineTimestampMillis: Long,
    val baselineValue: Int,
    val baselineChips: List<String>,
    val value: Int,
    val dateText: String,
    val timeText: String,
    val chips: List<String>
)

data class NoteEntryDraft(
    val entryId: Long,
    val baselineText: String,
    val text: String
)

sealed interface RecordValidation {
    data object Checking : RecordValidation
    data object Current : RecordValidation
    data object Conflicting : RecordValidation
}
```

- `TrackUiState` owns `activeModal: TrackModalState?`; the former four nullable modal fields are removed.
- `Delete` remains non-restored and may retain an `Entry` in live UI state; no `Entry` is placed in `SavedStateHandle`.
- `dateText`/`timeText` become ViewModel-owned strings. The Composable renders them directly and emits text-change events; it no longer owns a second authoritative `rememberSaveable` buffer for these dialogs.
- `NoteEntryDraft.text` is likewise the single source for the text field. Cursor selection, IME composition, focus, scroll offset, validation status, conflict status, and `isSaving` are not persisted.
- Chip lists preserve order and contain no duplicates. Toggling an existing chip removes it; selecting a new chip appends it.

## Modal invariants

1. `activeModal` is null or exactly one of Backdate/Edit/Note/Delete. Compose can render at most one `AlertDialog`.
2. While any modal is active, new long-press/edit/note/delete open requests are ignored. The existing draft cannot be replaced without explicit cancel/dismiss/success.
3. Backdate has no record id. Edit and Note always have exactly one positive Entry id and never depend on `recentEntries` membership.
4. Only Backdate/Edit/Note are encoded in `SavedStateHandle`. Delete confirmation restores closed so a destructive action must be requested again.
5. Only draft/source identity is persisted. Errors and status are re-derived from `nowProvider()`, `ZoneId.systemDefault()`, and current Room state.
6. At most one mutation is launched for one Save press. Inputs, Save, Cancel, and dismissal are disabled while that mutation is in flight; accessibility state announces `Saving`.
7. Successful or stale terminal outcomes clear the active modal and all saved keys. Retryable exceptions do not.

## SavedStateHandle contract

All keys are private to `TrackViewModel`. Version 1 uses:

| Key | Type | Required for |
|---|---|---|
| `track.dialog.version` | `Int` (`1`) | every restored draft |
| `track.dialog.kind` | `String` (`BACKDATE`, `EDIT`, `NOTE`) | every restored draft |
| `track.dialog.backdate.value` | `Int` | Backdate |
| `track.dialog.backdate.dateText` | `String` | Backdate |
| `track.dialog.backdate.timeText` | `String` | Backdate |
| `track.dialog.backdate.captureKind` | `String` (`NONE`, `SLEEP`, `WAKE`) | Backdate |
| `track.dialog.edit.entryId` | `Long` | Edit |
| `track.dialog.edit.baselineTimestamp` | `Long` | Edit |
| `track.dialog.edit.baselineValue` | `Int` | Edit |
| `track.dialog.edit.baselineChips` | `ArrayList<String>` | Edit |
| `track.dialog.edit.value` | `Int` | Edit |
| `track.dialog.edit.dateText` | `String` | Edit |
| `track.dialog.edit.timeText` | `String` | Edit |
| `track.dialog.edit.chips` | `ArrayList<String>` | Edit |
| `track.dialog.note.entryId` | `Long` | Note |
| `track.dialog.note.baselineText` | `String` | Note |
| `track.dialog.note.text` | `String` | Note |

Encoding and validation rules:

- Write only Bundle-supported primitives/strings/string array lists. Never write `Entry`, a dialog data class, `Set`, `Instant`, `LocalDate`, `ZoneId`, `Throwable`, or coroutine state.
- `version` and `kind` must be present and recognized. Every required key for that kind must be present with the exact type; partial groups are invalid.
- Entry ids must be greater than zero. Values must be 0 through 10.
- Date/time strings are restored exactly even when not parseable. Their normal UI input bounds are 10 characters for ISO date and 5 for `HH:mm`; a restored value outside those bounds is malformed rather than truncated.
- Chip array lists must contain nonblank strings, each at most 32 Unicode code points, with no duplicate exact strings. Order is significant. Do not impose a count cap at restoration: an Entry can legitimately accumulate historical extras across multiple vocabulary changes even though each current Settings vocabulary is capped at 20.
- Note strings are restored without trimming or truncation. They must fit the platform saved-state Bundle; the app adds no separate durable draft store.
- On any missing, unknown, wrong-type, or invalid required value, remove the entire `track.dialog.*` namespace, initialize with no modal, and expose `The unfinished dialog could not be restored.` The app must not crash or guess defaults that could target the wrong record.
- Opening Backdate/Edit/Note first clears every Phase 7 key, then writes a complete canonical group for the new draft. Each draft edit synchronously updates its corresponding key before publishing the matching `TrackUiState`.
- A shared `clearSavedDialog()` removes every key listed above. It is called for cancel/dismiss, successful save, stale id, malformed restore, and Delete replacement after the restorable modal is already absent.
- Marker keys remain separate and unchanged. Phase 7 dialog cleanup must not clear marker state or any Log/Settings/Insights key.

## Room identity validation and concurrency

Add one schema-neutral DAO observation:

```kotlin
@Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
fun observeById(id: Long): Flow<Entry?>
```

- `TrackViewModel` observes only the active Edit/Note id using cancellation-aware `flatMapLatest` or an equivalent single active job. Moving out of the recent top-ten window has no effect.
- A restored Edit/Note begins in `Checking`; Save is unavailable until the first Room emission.
- Null means deleted/stale: clear and show the frozen missing-record message.
- Edit conflict compares current `(ts, value, chips)` with its saved baseline. Note conflict compares `current.note.orEmpty()` with `baselineText`. Unrelated column changes are intentionally ignored.
- If a later Room emission returns to baseline-equivalent values, conflict clears. The user's draft never changes automatically.
- Conflict detection is a user-visible guard, not a new database lock. The final writes remain `updateEditableFields` and `updateNote`; their targeted columns preserve unrelated concurrent changes, and affected-row count 0 preserves the existing deletion race protection.
- A same-column writer can still commit after the last observation and before this mutation. With no revision column or conditional-update contract, last committed same-column write wins. This limitation is explicit and must not be described as optimistic locking.
- Existing onset `updateChips`, sleep transactions, settings targeted writes, `EpisodeSourceDao` transaction, and all Phase 5/6 derivation invalidation behavior remain unchanged.

## Save, cancel, and cleanup state machine

| Event/outcome | UI result | Saved-state result | Persistence result |
|---|---|---|---|
| Open Backdate/Edit/Note | One modal opens | Complete canonical group written | none |
| Draft field changes | Exact draft updates | Matching key updated | none |
| Invalid/future date-time | Modal stays, inline error, Save disabled | Raw strings retained | none |
| Cancel/Back/outside dismissal | Modal closes | all Phase 7 keys removed | none |
| Restored id missing | Modal closes, missing message | all Phase 7 keys removed | none |
| Same-field conflict | Draft stays with warning | draft/baseline retained | none until explicit Save |
| Edit/Note success (`1`) | Modal closes | all Phase 7 keys removed | targeted mutation committed |
| Edit/Note stale (`0`) | Modal closes, missing message | all Phase 7 keys removed | no row changed |
| Edit/Note exception | Modal/draft stay, retry message | keys retained | no success claimed |
| Backdate insert transaction fails | Modal/draft stay, retry message | keys retained | no success claimed |
| Backdate Entry saved | Modal closes | all Phase 7 keys removed | capture continues under existing onset/sleep rules |
| Delete cancel | Modal closes | no Phase 7 keys exist | none |
| Delete success (`1`) | Modal closes | no Phase 7 keys exist | targeted delete committed |
| Delete stale (`0`) | Modal closes, missing message | no Phase 7 keys exist | no row changed |
| Delete exception | Confirmation stays, retry message | no Phase 7 keys exist | no success claimed |

## Restoration lifecycle

1. `MainActivity` continues to pass `extras.createSavedStateHandle()` to the manually constructed `TrackViewModel`.
2. The ViewModel synchronously decodes the saved dialog before its initial `TrackUiState` is exposed.
3. Backdate can render immediately after local parsing/validation. Edit/Note render with their exact draft in `Checking` and start `observeById`.
4. The first current Entry emission resolves `Current`, `Conflicting`, or stale. No full Entry replaces the user's draft.
5. Activity-only recreation retains the current ViewModel and must also keep the dialog/draft. A fresh ViewModel constructed from the saved Bundle must produce the same user-visible result, which is the missing coverage this phase adds.
6. If Track is not the restored top-level destination, its draft remains in its Activity-scoped ViewModel and appears when Track is selected.

## Failure behavior

- Malformed saved state: clear only Phase 7 dialog keys, show the frozen restoration message, keep the app usable.
- Room validation/read failure: keep the restored draft, keep Save disabled, show `Could not check that record. Please try again.`, and retry observation through a focused action or collector restart. Do not report the record missing.
- Future or invalid local date/time: keep exact text and inline error. Preserve Track's existing `LocalDateTime.atZone(ZoneId.systemDefault())` resolution policy; Phase 7 does not introduce a new DST ambiguity choice.
- Edit/note mutation exception: retain draft and conflict state; catch and rethrow `CancellationException`, but convert other exceptions to the frozen retry message.
- Delete mutation exception: retain the confirmation and expose `Could not delete that record. Please try again.`; count 0 still closes with the missing-record message.
- Stale affected-row count: close safely; never recreate the deleted row.
- Backdate ordinary-capture exception: retain draft. Classification-unavailable/settings-unavailable results that still saved the Entry remain successes and use the already-frozen Track toast behavior.
- Process/task state unavailable after a force-stop/task removal: open no dialog; never infer a draft from Room.

## Accessibility, focus, keyboard, and large font

- The active dialog is the only modal semantics subtree. Its title, purpose, field labels, validation/conflict message, and saving state are available to screen readers.
- Validation, conflict, restoration, and mutation-failure text use visible text plus a polite live region; color is not the only signal.
- Every button/chip remains at least 48 dp. Chip semantics expose selected/unselected state and stable spoken labels.
- Initial open places predictable focus on the first editable control (date for Backdate, value/date group for Edit, note field for Note). Restoration does not persist cursor selection or IME composition; focus returns to the dialog's first logical editable control, never the destructive Delete action.
- Hardware keyboard/D-pad traversal follows title/instructions, fields, chips, Cancel, then Save. Enter in a multiline note inserts a newline and never triggers Save accidentally. Escape/system Back cancels through the same cleanup event.
- The soft keyboard may be reopened by the focused text field, but obscuring it must not make Save/Cancel unreachable. Dialog content scrolls vertically when needed.
- At 200% font scaling, portrait and landscape keep title, current field/error, and both actions reachable without clipped critical copy. The chip area wraps and scrolls; no text is painted into Canvas/bitmap.
- TalkBack announcements must not repeat multiple restored dialogs, which the discriminated modal invariant prevents.

## Room, backup, export, privacy, and architecture implications

- Room stays schema version 4. `observeById` is a query-only DAO addition; `app/schemas/.../4.json` remains byte-for-byte unchanged.
- JSON backup stays format version 4 and CSV stays unchanged. Unsaved dialog drafts are transient UI state and are not exported, imported, erased as records, or written to Room.
- Android's app-private saved-instance-state mechanism may temporarily contain a note draft and Entry id. No account, server, analytics, telemetry, Internet access, storage/media permission, backup-provider configuration, or external file is introduced.
- Native Kotlin, Compose, Material 3, manual DI, `StateFlow`, lifecycle-aware collection, `SavedStateHandle`, direct targeted DAOs, and the process-scoped `AppContainer` remain the architecture.
- No new dependency is needed. Do not add the newer lifecycle ViewModel testing artifact solely for this phase; exercise restoration with the currently pinned lifecycle/activity/test stack.

## Test strategy

### Pure/unit and ViewModel tests

- Round-trip every Backdate/Edit/Note key group through a new `SavedStateHandle`; prove exact raw text, chip order, baseline, id, value, and capture kind restoration.
- Reconstruct a new `TrackViewModel` after each draft mutation and prove one—and only one—modal appears.
- Cover partial/malformed groups, unknown version/kind, wrong types, invalid values/ids/chips, full namespace cleanup, and the restoration message.
- Prove invalid/partial date/time survives exactly while Save remains disabled; valid and future values re-derive correct validation.
- Prove Backdate Sleep/Wake capture kind restores, Cancel preserves arming, and success consumes it.
- Prove restored Edit/Note is Checking until Room emits; target outside recent ten still validates; deletion clears; unrelated edits do not conflict; same-field edits retain the draft and warn.
- Prove Edit/Note/Delete use only targeted DAO calls, clear on 1, clear/message on 0, and retain/retry on non-cancellation exceptions.
- Prove double Save cannot launch two mutations and result callbacks cannot clear a newer dialog generation.
- Preserve every existing Track/marker/onset/sleep/settings test.

### DAO/instrumented tests

- `observeById` emits the selected Entry, reacts to targeted edit/note/delete, returns null for a missing id, and is independent of `observeRecent(10)`.
- A saved-state owner/Bundle harness using the existing lifecycle/activity dependencies performs saved-state save, discards the owner and `ViewModelStore`, creates a new owner/ViewModel/factory, and proves all three drafts survive actual Bundle serialization without storing an `Entry`.
- Existing targeted mutation, onset transaction, sleep concurrency, migration 1→4/2→4/3→4, schema, export, and erase tests remain green.

### Compose/UI tests

- Each modal renders from immutable `TrackUiState` and emits raw text/draft events; no Composable-only authoritative dialog buffer remains.
- Exactly one modal exists even after adversarial open requests.
- Invalid/future text, Checking, conflict, saving, retry, stale, Cancel, Back, and outside-dismiss states expose the specified visible and accessible behavior.
- `ActivityScenario.recreate()` keeps each dialog/draft through Activity recreation; this is retained/configuration coverage, not mislabeled as proof of a rebuilt ViewModel.
- Keyboard traversal, multiline note Enter behavior, TalkBack/live-region semantics, 48 dp actions, scrolling, portrait/landscape, light/dark, and 200% font scaling have focused coverage.

### Device/manual process check

- On `MindScale_API_36`, create a real Entry, open each dialog in turn, type a distinctive unsaved draft, background the task so Android saves state, kill only the background app process with `adb shell am kill com.kieslingdev.mindscale` (not force-stop and not clear data), return through Recents, and verify the rebuilt Activity/ViewModel restores the exact dialog/draft.
- Repeat Edit/Note after changing/deleting the target from Full Log while the Track draft is retained; verify conflict or stale behavior.
- Restore emulator font scale, theme, orientation, and keyboard state after the walkthrough.

### Full oracles

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices -l
.\gradlew.bat connectedDebugAndroidTest
git diff --check
```

## Machine-checkable acceptance criteria

- [x] APPROVAL: the user approved this complete specification and D-1 through D-10 before any application/test-source edit.
- [x] MODEL: `TrackUiState` can represent at most one Backdate/Edit/Note/Delete modal; adversarial event tests never produce two dialogs.
- [x] BACKDATE-RESTORE: a new ViewModel restores exact value/date/time/capture-kind primitives, including invalid partial text; cancel and success follow the frozen arming/cleanup rules.
- [x] EDIT-RESTORE: a new ViewModel restores exact id, baseline, value/date/time/chips and resolves the id independently of recent-ten membership.
- [x] NOTE-RESTORE: a new ViewModel restores exact id/baseline/draft note text including whitespace and newlines.
- [x] ENCODING: Bundle-round-trip tests prove only the versioned primitive/string/string-list contract is saved; malformed groups clear safely and no `Entry`/Room entity is serialized.
- [x] STALE/CONFLICT: deleted ids close with the missing message; unrelated-column changes preserve normal save; same-owned-field changes retain the draft and require an explicit warned overwrite.
- [x] MUTATION: Edit, Note, and Delete call only existing targeted mutations; result 1 clears, result 0 clears stale, exceptions retain the retryable modal/draft, and cancellation is rethrown.
- [x] CAPTURE: Backdate failure before Entry persistence retains the draft; successful Entry persistence clears it without changing hold/onset/Sleep/Wake semantics or allowing an automatic duplicate retry.
- [x] ACTIVITY: Compose/instrumented tests prove all three dialogs survive `ActivityScenario.recreate()` with exact drafts.
- [x] PROCESS-SIMULATION: an instrumented saved-state owner/Bundle test discards the old owner/ViewModelStore and reconstructs all three drafts in a new ViewModel.
- [x] ACCESSIBILITY: focused tests prove one modal semantics tree, labeled/live errors and conflicts, 48 dp Material actions, multiline keyboard behavior, scroll reachability, and critical content at 200% font; the API 36 walkthrough confirmed actions remain reachable with the IME open.
- [x] PERSISTENCE: Room schema JSON 4, JSON backup 4, CSV, permissions, manifest, dependencies, and toolchain are unchanged.
- [x] REGRESSION: all Phase 1–6 JVM and connected suites remain green, including marker restoration, targeted Entry updates, sleep concurrency, onset classification, navigation, Insights, export, and erase.
- [x] ORACLES: wrapper test/lint/assemble, intended-device identity, connected tests, manual background-process restoration, and diff check pass.
- [x] DOCUMENTATION: this spec is marked implemented only after verification; `PROJECT_STATE.md` records exact evidence; `docs/DECISIONS.md` receives the approved stable decision; the active item does not remain duplicated in `BACKLOG.md`.

## Final verification evidence

Verified locally on 2026-08-04 from `agent/phase7-track-dialog-restoration`, based on synchronized `main` commit `372d1d3d91f7308ad35b8824c996bce641b0ce57`:

- `test`: 136/136 JVM tests passed; focused `TrackViewModelTest`: 61/61.
- `lint`: passed with 0 errors and the same 22 existing warnings.
- `assembleDebug`: passed.
- `connectedDebugAndroidTest`: 88/88 tests passed on `emulator-5554`, `MindScale_API_36` (API 36), including all three Activity recreation paths, the real Room `observeById` test, and the discarded-owner/ViewModelStore Bundle reconstruction harness.
- Manual process restoration installed the assembled debug APK, opened each Backdate/Edit/Note dialog with a distinctive unsaved draft, sent the task to background, confirmed the old PID, ran `adb shell am kill com.kieslingdev.mindscale`, confirmed no PID, returned through Recents, confirmed a new PID, and observed the exact restored draft. Backdate restored `2026-0`/`1`, Edit restored `2026-0`/`2`, and Note restored the two-line `Phase7 manual\nrestored` text.
- Visual API 36 inspection confirmed the focused dialog, labeled date/time fields, validation/action layout, scrolling surface, and reachable Cancel/Save actions with the IME open. Emulator font scale remained `1.0`, night mode `no`, and automatic rotation enabled.
- `git diff --check` passed with only Windows line-ending notices. Room schema 4, backup/CSV, manifest/permissions, dependencies, and toolchain files are unchanged.

## Documentation updates

- During this draft: add this spec; move the restoration item out of `BACKLOG.md`; update `PROJECT_STATE.md` with branch `agent/phase7-track-dialog-restoration`, base `372d1d3d91f7308ad35b8824c996bce641b0ce57`, the approval gate, and current dirty documentation.
- On approval: append D-007 to `docs/DECISIONS.md` summarizing the versioned primitive dialog envelope, Room id revalidation, conflict warning, targeted writes, and schema/backup invariance.
- During implementation: update this spec's acceptance checkboxes only with evidence.
- On verified completion: mark the spec implemented, update `PROJECT_STATE.md` with commands/counts/manual process evidence/commit status, and do not add a `FAILED_PATHS.md` entry unless a plausible approach actually fails and merits a durable warning.

## Proposed decisions and approval gate

- **D-1 (bounded scope):** restore only Backdate/Edit/Note drafts; unify Delete into the live single-modal model but do not restore its confirmation.
- **D-2 (single modal):** replace independently nullable dialog fields with one discriminated `activeModal` so multiple `AlertDialog`s are unrepresentable.
- **D-3 (primitive envelope):** save one versioned key group made only of ids, enum names, numbers, raw text, and ordered string lists; save no Room entity or complex UI/runtime object.
- **D-4 (raw timestamp draft):** hoist exact date/time strings into ViewModel state and `SavedStateHandle`; derive parsing/errors so partial invalid input survives.
- **D-5 (Room identity):** add schema-neutral `observeById` and validate restored ids independently of the recent-ten list.
- **D-6 (conflict behavior):** retain the user's draft and visibly warn when the dialog-owned fields changed; allow an explicit warned overwrite, while unrelated targeted columns remain preserved.
- **D-7 (mutation cleanup):** clear only after success or definitive stale/cancel outcomes; retain exact drafts on retryable exceptions; avoid duplicate Backdate retry after primary Entry success.
- **D-8 (navigation/lifecycle):** external navigation does not discard a draft; task-stack saved state covers system restoration but not force-stop/task removal.
- **D-9 (tests):** distinguish Activity recreation from a discarded-owner/ViewModel Bundle reconstruction and a manual real background-process kill; do not overclaim `ActivityScenario.recreate()` alone.
- **D-10 (no persistence/toolchain expansion):** Room/schema/backup/export/permissions/dependencies/frameworks/toolchain remain unchanged.

Approval satisfied: D-1 through D-10 and this complete Phase 7 scope are frozen. Material scope or interface changes still require a documented spec amendment, but no further routine implementation approvals are required.
