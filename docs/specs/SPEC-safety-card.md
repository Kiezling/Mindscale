# SPEC-safety-card: Native Safety card and personal safety plan

Status: FROZEN — APPROVED

Owner: Claude Code agent under full Phase 13 ownership granted by the user on 2026-08-05

Date: 2026-08-05

Last verified commit: `5eb10e497e328fb09dcf8254e7d5271c09cca1eb` (synchronized `main` at branch creation)

## Purpose

MindScale is a measurement instrument that never interprets, but the product source
(`SPEC.md`, "Safety") requires one thing the instrument itself cannot supply: a calm,
always-reachable card holding crisis resources and the user's own Stanley-Brown safety
plan, written in advance and stored only on this device. Phase 13 adds that card as a
reachable overlay with verified crisis-resource copy, six ordered Stanley-Brown steps the
user fills in themselves, and a hand-off-only phone action. It is the first MindScale
surface whose content can matter to someone in acute distress, so its copy, ordering, and
accessibility are the load-bearing part of this spec — not its mechanics.

## Sources reconciled

- `docs/specs/BACKLOG.md`, first ordered item: "Native Safety card and low-frequency
  off-ramp follow-on — preserve the Stanley-Brown ordering and local-only storage;
  requires explicit crisis-copy, phone-action, privacy, and accessibility decisions."
- Product source `SPEC.md` from Claude Design project `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`
  (local handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\`), sections
  "Keep / build", "Safety", "Insight grammar", "Regulatory (US)", "Axioms".
- Prototype `MindScale v2.dc.html`: the `isSafety` screen (lines 559–600), `safetySections`
  (lines 1599–1606), the Track footer entry point (line 249), the Profile action row
  (line 1619), and the check-in/paused banners (lines 60–62, 156–165).
- Stanley, B., & Brown, G. K. (2012), *Safety Planning Intervention: A Brief Intervention
  to Mitigate Suicide Risk*, for the canonical six-step ordering.
- Repository: `docs/DECISIONS.md` D-004 through D-012, `PROJECT_STATE.md`,
  `FAILED_PATHS.md`, `SPEC-settings-data-control.md` (D-5 export/erase),
  `SPEC-clinician-report-profile.md` (D-9 future-import contract),
  `SPEC-import-restore.md` (D-1 through D-12), `SPEC-track-phase2-completeness.md`
  (paused/check-in banners), `SPEC-track-dialog-restoration.md` (D-7 saved-state envelope).

### Crisis-resource verification

Every number, service name, and coverage claim below was verified on **2026-08-05** from
the operator's own site or the funding agency, not from model memory. No number is carried
forward from training data.

| Resource | Verified fact | Source | Verified |
|---|---|---|---|
| 988 Suicide & Crisis Lifeline (United States) | Official name "988 Suicide & Crisis Lifeline"; reachable by call **and** text at 988; "available 24/7/365"; "free and confidential"; Spanish text and chat available | `https://988lifeline.org/` | 2026-08-05 |
| 988 (United States) TTY access | "For TTY Users: Use your preferred relay service or dial 711 then 988." | `https://988lifeline.org/help-yourself/deaf-hard-of-hearing/` | 2026-08-05 |
| 988 (United States) federal confirmation | SAMHSA lists the service as "988 Suicide & Crisis Lifeline", reached by "Call or Text 988" | `https://www.samhsa.gov/find-help/988` | 2026-08-05 |
| 9-8-8 Suicide Crisis Helpline (Canada) | Official name "9-8-8: Suicide Crisis Helpline"; call **or** text 9-8-8; free; 24/7/365; English and French; funded by the Government of Canada, delivered by CAMH | `https://988.ca/`, `https://www.camh.ca/en/driving-change/988`, `https://crtc.gc.ca/eng/phone/988.htm` | 2026-08-05 |
| Find A Helpline | Free international helpline directory, powered by ThroughLine, covering many countries; helpline data verified with operators directly | `https://findahelpline.com/`, `https://findahelpline.com/about` | 2026-08-05 |

Deliberately **not** frozen into the copy: any count of countries covered by Find A
Helpline. The site's own tagline ("130+ countries") and its About page ("over 175
countries") disagree, and a count is exactly the kind of fact that goes stale. The card
describes the directory without asserting a number.

## Scope

In scope:

1. A reachable `SAFETY` overlay destination holding, in this order: an honesty statement,
   the always-open crisis resources, and the six-step personal safety plan.
2. Locally stored, user-written safety-plan items across the six Stanley-Brown steps, with
   an optional phone number on the two contact steps.
3. `ACTION_DIAL` / `ACTION_SENDTO` / `ACTION_VIEW` hand-offs — never `ACTION_CALL`.
4. Additive Room 5→6 `safety_plan_items`, JSON backup version 6, restore acceptance of
   backup versions 3–6, and erase/reset coverage.
5. Two entry points: a persistent Track footer link and a Profile row.

## Non-goals

- **No risk detection of any kind.** Nothing on this card is triggered, surfaced, ordered,
  emphasized, or hidden because of a rating, episode, streak, gap, count, date, or any
  other recorded value. MindScale does not assess risk and must not appear to.
- No automatic display: the card is never a pop-up, interstitial, snackbar, notification,
  or auto-navigation target. The only way to reach it is a user tap.
- No paced-breathing work and no UI overhaul (both remain backlog items).
- No change to the low-frequency off-ramp (see D-2 — it is already delivered).
- No safety-plan content in the records CSV, the clinician summary, or any share/copy
  action.
- No sharing, printing, PDF, or export of the plan other than the standard JSON backup.
- No reminders, nudges, review prompts, completeness meters, or "your plan is empty"
  pressure.
- No region, locale, SIM, or network detection; no location permission; no `CALL_PHONE`
  permission; no new dependency; no toolchain change.
- No network call made by MindScale itself. The Find A Helpline button hands a URL to the
  user's browser; MindScale never opens a socket.
- No destructive migration and no backup downgrade path.

## Decisions

### D-1 — Ship the card with stored plan, not read-only reference content

**Decided: the personal safety plan is stored locally.**

The backlog item names "local-only storage" as a constraint to preserve, and the Stanley-
Brown intervention is the person's own written plan — its structure exists to hold their
warning signs, their coping strategies, their people. A card showing six headings the user
cannot fill in would also violate the standing project rule that no inert control is shown
(`PROJECT_STATE.md`, "Known coverage gaps and backlog"). The alternative — crisis numbers
alone — is honest but is not the backlog item.

Cost accepted: Room 5→6, backup version 6, restore acceptance of version 6, and erase
coverage. All of it is additive and follows patterns Phases 11 and 12 already reviewed.

### D-2 — The low-frequency off-ramp is already delivered; this phase adds nothing to it

**Decided: close the off-ramp clause of the backlog item as already implemented, and add
no off-ramp behavior in Phase 13.**

The product source asks for three things (`SPEC.md`, "Safety"): a low-frequency "is this
still helping?", a real off-ramp, and a dignified one-screen pause / export-and-delete.
All three already exist:

- The check-in banner (`TrackScreen.kt:455`, copy frozen in Phase 2) shows at most once per
  60 days after 40 entries and offers "Still useful" / "Pause tracking"
  (`TrackViewModel.kt:190`).
- Pause is stored (`TrackSettings.paused`) and hides the entire capture surface
  (`TrackScreen.kt:121`).
- The paused banner (`TrackScreen.kt:231`) deep-links to `SettingsFocus.DATA`, the single
  screen holding export JSON, export CSV, and export-then-erase (Phase 4, D-5).

Nothing remains. Explicitly rejected: adding a Safety link to the check-in or paused
banner. Those banners are the only MindScale surfaces whose visibility depends on recorded
data (entry count and elapsed time), and placing crisis resources inside a data-conditioned
banner would make the app appear to have inferred something about the user. The Track
footer link (D-6) is unconditional and serves the same reachability need honestly.

### D-3 — Crisis-resource copy is frozen verbatim, with coverage stated and never inferred

**Decided: one 988 block covering the United States and Canada, one international
directory pointer, one emergency sentence without a button, and an explicit statement of
what MindScale cannot do.**

Reasoning for the region approach: MindScale has no network access, no location permission,
and no way to know the user's country. Device locale is a language and formatting
preference, not a country of residence, and reading it to pick a hotline would silently
assume a region and could show the wrong country's number to someone in crisis. So the card
shows one clearly labeled resource set, states its coverage in plain words, and points
everywhere else at a maintained directory. Resources are never reordered, filtered, or
hidden by any inferred signal.

Reasoning for a single 988 block rather than separate US and Canada blocks: both countries
use the same three digits, so two identical button pairs would double the reading and
tapping cost in the exact moment cognitive load must be lowest. One block, one pair of
buttons, coverage stated in the detail line.

Reasoning for no emergency-services button: an `ACTION_DIAL` button pre-filled with 911 sits
one accidental tap away from an emergency dispatcher, and a mis-tap on a screen someone
opened while distressed is a foreseeable harm. The sentence is present; the button is not.

Reasoning for buttons above the detail paragraph: the actions must be reachable, visually
and in TalkBack reading order, without first reading a paragraph.

Frozen copy (`SafetyCopy`), rendered exactly, with no substitution, truncation, or
reflowing of wording:

| Key | Exact text |
|---|---|
| `SCREEN_INTRO` | `This card is here whenever you want it. Nothing you record opens it, and nothing you record changes what it says.` |
| `HONESTY` | `MindScale keeps this card on your device and does nothing else with it. It cannot tell how you are, alert anyone, or get help for you. Only the buttons you tap on this screen do anything.` |
| `RESOURCES_HEADING` | `Always open` |
| `LIFELINE_NAME` | `988 — United States and Canada` |
| `LIFELINE_CALL` | `Call 988` |
| `LIFELINE_TEXT` | `Text 988` |
| `LIFELINE_DETAIL` | `Calling or texting 988 reaches the 988 Suicide & Crisis Lifeline in the United States and the 9-8-8 Suicide Crisis Helpline in Canada. Free and confidential, 24 hours a day, every day. In Canada it is available in English and French. For TTY in the United States, use your preferred relay service or dial 711 then 988.` |
| `ELSEWHERE_NAME` | `Anywhere else` |
| `ELSEWHERE_ACTION` | `Open findahelpline.com` |
| `ELSEWHERE_DETAIL` | `988 only connects in the United States and Canada. Find A Helpline lists free crisis lines in many other countries. This button opens your browser; MindScale itself never connects to the internet.` |
| `EMERGENCY` | `If someone is in immediate physical danger, a local emergency number is the fastest route. In the United States and Canada that is 911. There is no button for it here, so it cannot be dialled by accident.` |
| `VERIFIED_ON` | `These numbers were checked on 5 August 2026.` |

`VERIFIED_ON` is honest about staleness rather than implying perpetual currency, and it
gives a future maintainer a visible reason to re-verify.

### D-4 — Stanley-Brown ordering is preserved exactly; headings are plain language

**Decided: all six canonical steps, in canonical order, no merge and no omission.**

`SafetyPlanStep` declaration order is the contract, and the screen renders the steps in
`SafetyPlanStep.entries` order:

| # | Canonical Stanley-Brown step | Frozen heading | Frozen hint |
|---|---|---|---|
| 1 | Warning signs | `What I notice first` | `The earliest signs that things are turning, written now while it is easy to think.` |
| 2 | Internal coping strategies | `What I can do on my own` | `Things that have helped without needing anyone else. Easiest first.` |
| 3 | People and social settings that provide distraction | `Where I can go` | `Places and people that take up attention without needing a conversation.` |
| 4 | People whom I can ask for help | `Who I can ask for help` | `You do not have to explain anything. One word is enough.` |
| 5 | Professionals or agencies I can contact | `Professionals I can contact` | `Doctor, therapist, clinic, or crisis line.` |
| 6 | Making the environment safe | `Making my space safer` | `What goes somewhere else for now, and who holds it.` |

Plain-language headings are used rather than the clinical labels because the card is read
under distress, and the mapping above is the record required by the phase boundary. No step
is reordered, merged, or omitted, and the mapping is one-to-one.

Frozen plan copy:

| Key | Exact text |
|---|---|
| `PLAN_HEADING` | `Your plan` |
| `PLAN_INTRO` | `Written by you, in advance, for a moment when thinking is hard. The steps are in a set order; within each one, put the easiest thing first.` |
| `PLAN_EMPTY` | `Nothing written here yet. You can add to any step at any time, or leave it empty.` |
| `stepEmpty(step)` | `Nothing written for this step.` |

`PLAN_EMPTY` deliberately ends with "or leave it empty" — an unfilled plan must not read as
a failure state.

Rejected from the prototype: the means-restriction hint "The single most protective line on
this card." It is an efficacy claim about the world, and MindScale makes none
(`SPEC.md`, "Insight grammar"; `SPEC.md`, "Positioning").

### D-5 — Safety-plan data contract

Storage is a table, not a canonical row, because the plan is a list per step. Contacts and
non-contacts share one entity; the phone column is constrained by step rather than by a
second table.

Validation rules (all enforced by the same pure functions used by import, D-9):

- `text`: trimmed, non-blank, at most `MAX_PLAN_TEXT_CODE_POINTS` (200) code points, no
  line separator, no control character except none — same single-line rule Phase 12 applies
  to display names.
- `phone`: `null`, or a trimmed non-blank string of at most `MAX_PLAN_PHONE_CODE_POINTS`
  (40) code points containing only `0-9 + - ( ) . space` and at least one digit.
- `phone` is non-null only for `PEOPLE_FOR_HELP` and `PROFESSIONALS`. Any other step with a
  non-null phone is invalid.
- `position` is 0-based and contiguous within a step: the positions of a step's items are
  exactly `0 until size`.
- At most `MAX_PLAN_ITEMS_PER_STEP` (10) items per step and
  `MAX_PLAN_ITEMS_TOTAL` (60) overall.

Export/backup: included in JSON backup version 6, ordered by step declaration order then
`position` then `id`, so re-export is byte-reproducible (Phase 12 precedent).

Records CSV: **unchanged**. A safety plan is not a symptom record, and Phase 12 froze CSV
compatibility. Recorded rather than assumed.

Clinician summary and Report: **excluded**. The Report is a boundary object a user hands to
a clinician; the plan holds contacts' phone numbers and means-restriction details. It never
enters the summary text, the copy action, the share action, or the saved file.

Erase: `eraseEverythingAndResetSettings()` deletes every plan item, and the erase
confirmation dialog discloses the exact count before the user confirms.

### D-6 — Entry points

**Decided: an overlay reached from a persistent Track footer link and a Profile row. Not a
bottom-navigation destination.**

Track, Log, and Insights are the three things a user does with the instrument. Making
Safety a fourth tab would present it as a place one is supposed to spend time and would put
crisis content permanently in the chrome of a measurement app. As an overlay it matches
Profile, Report, and Settings, and Back returns to whatever destination opened it — the
existing `destinationStack` already gives this for free.

The Track footer link renders **unconditionally**, including while tracking is paused. Its
visibility depends on nothing recorded. It is the last item in Track's scrolling content,
below the capture surface, so it never competes with logging (axiom 2: costs nothing on a
good day).

Frozen entry copy:

| Key | Exact text |
|---|---|
| `TRACK_LINK` | `If this is a hard moment` |
| `TRACK_LINK_DESCRIPTION` | `Open the Safety card` |
| `PROFILE_ROW` | `Safety card` |
| `TOP_BAR_TITLE` | `Safety` |

### D-7 — Phone-action mechanics

**Decided: `ACTION_DIAL`, `ACTION_SENDTO`, and `ACTION_VIEW` hand-offs only. No call is ever
placed by MindScale.**

- Dial: `Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", dialString, null))`. `ACTION_CALL`
  is forbidden, and no `CALL_PHONE` permission is added. `Uri.fromParts` is required rather
  than `Uri.parse("tel:$raw")` so that `#` and other reserved characters are encoded rather
  than silently truncating the number.
- Text: `Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))` with no
  message body. No `SEND_SMS` permission; the user's messaging app composes and the user
  sends.
- Web: `Intent(Intent.ACTION_VIEW, Uri.parse(url))`.
- The dialer opens pre-filled and **does not dial**. Cancellation is the platform's: the
  user backs out of the dialer and returns to the Safety card with nothing changed and
  nothing recorded.
- No `resolveActivity` and no `<queries>` manifest element. Missing-handler behavior is a
  caught `ActivityNotFoundException` that shows the frozen message with the number still
  visible on screen, so it can be dialled by hand.
- `dialString` strips every character except `0-9 + * # , ;` and returns `null` when no
  digit remains. A stored contact whose number yields `null` shows no Call button at all
  rather than an action that cannot work.

Frozen failure copy:

| Key | Exact text |
|---|---|
| `dialUnavailable(number)` | `No app on this device can open the dialer. The number is <number>.` |
| `textUnavailable(number)` | `No app on this device can send a text message. The number is <number>.` |
| `pageUnavailable(url)` | `No app on this device can open a web page. The address is <url>.` |

### D-8 — Privacy and logging

- MindScale records **nothing** about the Safety card: not that it was opened, not when,
  not how often, not which resource was tapped, not whether a call was placed. No column,
  no counter, no timestamp, no flag. The card writes to Room only when the user saves,
  edits, or deletes a plan item.
- No plan text, phone number, or resource tap is written to Logcat at any level.
- Plan content never enters the records CSV, the clinician summary, or any share/copy
  action (D-5).
- `SavedStateHandle` holds only the open editor's primitive envelope — step name, edited
  item id or `-1`, and the two in-progress drafts — following the Phase 7 pattern, so text
  typed in a hard moment is not lost to a configuration change or process death. The full
  plan is never placed in saved instance state.
- Backups already carry the plan (D-5); the Settings export copy is where that is
  disclosed, and the erase dialog discloses deletion.

### D-9 — Room 5→6, backup version 6, and restore

Additive migration only; no existing table, column, row, or index is altered.

```sql
CREATE TABLE IF NOT EXISTS `safety_plan_items` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `step` TEXT NOT NULL,
  `position` INTEGER NOT NULL,
  `text` TEXT NOT NULL,
  `phone` TEXT
)
```

- `encodeBackup` writes `"version": 6` and a `"safetyPlan"` array after `externalScores`.
- `MAX_BACKUP_VERSION` becomes `6`; `MIN_BACKUP_VERSION` stays `3`. Versions 3, 4, and 5
  remain fully restorable and are treated as carrying an empty plan, disclosed verbatim in
  the preview before anything is written.
- Version-6 files must contain the `safetyPlan` key; exact-key equality is already the
  Phase 12 rule and is extended, not weakened.
- Imported items are validated by the same rules as D-5, are counted against
  `MAX_PLAN_ITEMS_PER_STEP` / `MAX_PLAN_ITEMS_TOTAL`, and a violation rejects the entire
  file — Phase 12's total-rejection policy is unchanged.
- `replaceEverything` deletes every plan item and inserts the file's ids verbatim inside
  the same transaction, with a post-mutation count check that rolls back on mismatch.
- `addRecords` (records CSV) never touches the plan.

Frozen preview additions:

| Condition | Exact line |
|---|---|
| always, appended to the "It contains" line | `, plus your settings, Profile name, and safety plan` replaces `, plus your settings and Profile name` |
| always, appended to the deletion line | `, and will replace your settings, Profile name, and safety plan` replaces `, and will replace your settings and Profile name` |
| version 6 | `This backup contains <n> safety plan <line/lines>. Your current safety plan of <m> <line/lines> is permanently deleted.` |
| version < 6 | `This backup predates the safety plan. Your safety plan of <m> <line/lines> is permanently deleted and nothing replaces it.` |

Frozen erase-dialog copy (replacing the Phase 11 sentence):

`Your Profile name, all externally obtained totals, and your safety plan (<n> <line/lines>) are also deleted.`

### D-10 — Accessibility

This content must be legible and navigable under real distress, so the following are
requirements, not polish:

- Every actionable element is at least 48 dp on both axes, including the Track footer link
  and every Call/Text/Open/Add/Edit/Delete control.
- The whole screen is one vertically scrolling `LazyColumn` with no fixed-height or
  height-capped container, so nothing is clipped at 200% font scale or in landscape. This
  is the defect Phase 12 found by installed-app inspection and must not recur.
- Colour comes only from `MaterialTheme.colorScheme` roles (`onSurface`, `onSurfaceVariant`,
  `primary`, `error`). The prototype's 0.35–0.45 alpha ink is explicitly rejected: it fails
  contrast in both themes and this is the wrong screen on which to be subtle.
- Section headings carry `heading()` semantics; reading order is intro → honesty → crisis
  resources (name → actions → detail) → emergency → plan intro → steps 1–6 in canonical
  order.
- No auto-advancing content, no timed content, no auto-playing animation, no automatic
  focus capture, no marquee, no content that changes without a user action.
- Transient action results (save confirmation, action-unavailable message) are the only
  live regions, and are `LiveRegionMode.Polite`.
- Crisis actions have explicit content descriptions (`Call 988`, `Text 988`), and a contact
  row's Call button describes its target (`Call <name>`).

### D-11 — Non-inference invariant, enforced structurally

`SafetyViewModel`'s constructor accepts `SafetyPlanDao` and nothing else. It has no access
to `EntryDao`, `SleepDao`, `MarkerDao`, `EpisodeSourceDao`, `TrackSettingsDao`, or the
episode engine, so no future edit can make the card's content depend on recorded data
without changing a frozen interface. Screen content is a pure function of the stored plan
alone.

## User experience

Entry: Track footer link ("If this is a hard moment") or Profile row ("Safety card").
Back returns to the opening destination; the destination stack already restores across
rotation and process death.

The screen, top to bottom: intro, honesty statement, `Always open` crisis block (988 name →
Call/Text buttons → detail; Anywhere else name → Open button → detail; emergency sentence;
verification date), then `Your plan` with its intro and the six steps in canonical order.
Each step shows its heading, hint, its items in position order, and an `Add` button.

Item rows show the text, the phone number when present, and — for the two contact steps
with a dialable number — a `Call` button. Every row has `Edit` and `Delete`. `Delete` asks
for confirmation, matching the project-wide rule that permanent deletion is confirmed
(`SPEC-track-numpad-logging.md`, D-1).

Adding or editing opens a modal with a text field and, on the two contact steps, a phone
field. Validation errors are shown inline and block saving. Cancel discards.

Empty state: `PLAN_EMPTY` above the steps when no item exists in any step, and
`stepEmpty` under each empty step. No nudge, no progress indicator, no red state.

Error state: a Room read failure shows the existing message-plus-Retry pattern. A write
failure leaves the modal open and retryable, matching Phase 3 and Phase 11.

## Frozen interfaces and data contracts

```kotlin
// data/SafetyPlanItem.kt — declaration order IS the Stanley-Brown order (D-4)
enum class SafetyPlanStep { WARNING_SIGNS, INTERNAL_COPING, DISTRACTION, PEOPLE_FOR_HELP, PROFESSIONALS, ENVIRONMENT_SAFETY }

@Entity(tableName = "safety_plan_items")
data class SafetyPlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val step: SafetyPlanStep,
    val position: Int,
    val text: String,
    val phone: String? = null
)

val SafetyPlanStep.allowsPhone: Boolean

// data/SafetyPlanDao.kt
@Dao
interface SafetyPlanDao {
    @Query("SELECT * FROM safety_plan_items ORDER BY step ASC, position ASC, id ASC")
    fun observeAll(): Flow<List<SafetyPlanItem>>

    @Query("SELECT * FROM safety_plan_items WHERE step = :step ORDER BY position ASC, id ASC")
    suspend fun itemsIn(step: SafetyPlanStep): List<SafetyPlanItem>

    @Query("SELECT COUNT(*) FROM safety_plan_items")
    suspend fun count(): Int

    @Query("UPDATE safety_plan_items SET text = :text, phone = :phone WHERE id = :id")
    suspend fun updateContent(id: Long, text: String, phone: String?): Int

    @Query("DELETE FROM safety_plan_items WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Transaction suspend fun addItem(step: SafetyPlanStep, text: String, phone: String?): Long
    @Transaction suspend fun removeItem(id: Long): Boolean
}

// safety/SafetyContent.kt — pure, Android-free, no dependency on any recorded value (D-11)
object SafetyCopy { /* every frozen string in D-3, D-4, D-6, D-7 */ }
data class SafetyStepContent(val step: SafetyPlanStep, val heading: String, val hint: String)
val SAFETY_STEPS: List<SafetyStepContent>              // exactly six, canonical order
val SAFETY_RESOURCES: List<SafetyResource>             // exactly two, frozen order

sealed interface SafetyAction {
    data class Dial(val number: String) : SafetyAction
    data class Text(val number: String) : SafetyAction
    data class OpenPage(val url: String) : SafetyAction
}

object SafetyActions {
    fun dialString(raw: String): String?                       // pure; null when no digit
    fun unavailableMessage(action: SafetyAction): String       // pure
}

// safety/SafetyIntents.kt — the only Android-typed edge
fun intentFor(action: SafetyAction): Intent                    // DIAL / SENDTO / VIEW only

// safety/SafetyValidation.kt — shared by the ViewModel and the importer (D-5, D-9)
sealed interface PlanFieldError { object Empty; object TooLong; object MultiLine; object BadPhone; object PhoneNotAllowed; object StepFull; object PlanFull }
fun validatePlanText(raw: String): Result<String>
fun validatePlanPhone(raw: String, step: SafetyPlanStep): Result<String?>

const val MAX_PLAN_TEXT_CODE_POINTS = 200
const val MAX_PLAN_PHONE_CODE_POINTS = 40
const val MAX_PLAN_ITEMS_PER_STEP = 10
const val MAX_PLAN_ITEMS_TOTAL = 60

// MindScaleApp.kt
enum class AppDestination { TRACK, LOG, INSIGHTS, PROFILE, REPORT, SETTINGS, SAFETY }
```

Amended existing contracts (additive only):

- `MindScaleDatabase`: `version = 6`, `SafetyPlanItem::class` added, `MIGRATION_5_6`
  registered, `safetyPlanDao()` added.
- `DataSnapshot`: `+ val safetyPlan: List<SafetyPlanItem> = emptyList()`.
- `EraseCounts`: `+ val safetyPlanItems: Int = 0`.
- `BackupPayload`: `+ val safetyPlan: List<SafetyPlanItem>`.
- `RecordCounts`: `+ val safetyPlanItems: Int = 0`.
- `DataControlDao`: `allSafetyPlanItems()`, `deleteSafetyPlanItems()`,
  `insertSafetyPlanItems()`, `safetyPlanItemCount()`; `snapshot()`, `replaceEverything()`,
  and `eraseEverythingAndResetSettings()` extended.
- `MAX_BACKUP_VERSION = 6`.
- `RecordsPayload`, `RECORDS_CSV_HEADER`, and `encodeRecordsCsv` are **unchanged**.

## Invariants

1. No Safety-screen value is derived from an entry, sleep, marker, episode, score, or any
   setting other than theme and font scale (D-11).
2. `SAFETY_STEPS` has exactly six elements whose `step` values equal `SafetyPlanStep.entries`
   in declaration order.
3. The card is never shown, focused, or navigated to without a user tap.
4. No intent constructed anywhere in the app uses `ACTION_CALL`; the manifest declares no
   `CALL_PHONE` and no new permission.
5. Every stored `SafetyPlanItem` satisfies D-5's validation rules; positions within a step
   are exactly `0 until size` after any add, edit, or delete.
6. Deleting an item renumbers the remaining items of that step inside one transaction.
7. Nothing is written to Room, preferences, or a file when the card is opened or a resource
   is tapped.
8. A restore either applies the whole backup, plan included, or leaves the database
   untouched.
9. Backup versions 3–6 restore; version 7+ is rejected with `NEWER_VERSION`.
10. Plan content never appears in the records CSV, the clinician summary, a share, or a
    copy action.
11. All Room access is off the main thread and collection is lifecycle-aware.
12. Frozen copy is compared by exact string equality in tests; no test asserts a substring
    where the whole string is frozen.

## Android compatibility

- Minimum SDK 26, target 36, compile 36.1 — unchanged. No new API-level branch.
- No manifest change: `ACTION_DIAL`, `ACTION_SENDTO`, and `ACTION_VIEW` are started, not
  queried, so Android 11 package-visibility filtering does not require a `<queries>` entry.
  `ActivityNotFoundException` is the documented, tested failure path.
- Room 5→6 is additive; `exportSchema = true` means `app/schemas/…/6.json` is generated and
  committed. Migration is tested 5→6 and 1→6 by the existing `MigrationTest` pattern.
- Process death and rotation: the destination stack and the editor envelope are primitive
  `rememberSaveable` / `SavedStateHandle` state (Phase 7 pattern).
- Compose: one `LazyColumn` with stable keys (`"plan:<id>"`, `"step:<name>"`); no nested
  vertical scroll; no derived state from any repository other than `SafetyPlanDao`.

## Acceptance criteria

- [ ] UNIT: `SAFETY_STEPS` equals the six canonical steps in `SafetyPlanStep.entries` order,
      asserted element by element with exact heading and hint strings.
- [ ] UNIT: every `SafetyCopy` constant equals its frozen string exactly (literal-text
      assertions, the Phase 11 clinician-grammar pattern).
- [ ] UNIT: `dialString` strips formatting, preserves `+ * # , ;`, and returns `null` for a
      number with no digit.
- [ ] UNIT: `unavailableMessage` returns the frozen dial/text/page strings.
- [ ] UNIT: `validatePlanText` rejects blank, whitespace-only, over-200-code-point, and
      multi-line input and trims accepted input.
- [ ] UNIT: `validatePlanPhone` accepts a formatted number on the two contact steps, rejects
      any phone on the other four, rejects letters, and rejects over 40 code points.
- [ ] UNIT: `encodeBackup` emits `"version": 6` and a `safetyPlan` array ordered by step,
      position, then id; re-encoding a parsed backup reproduces the input byte for byte.
- [ ] UNIT: `parseBackup` accepts a version-6 file, restores plan items verbatim, rejects a
      version-7 file with `NEWER_VERSION`, and restores versions 3/4/5 with an empty plan.
- [ ] UNIT: `parseBackup` totally rejects a version-6 file with a bad step name, a
      non-contiguous position, a phone on a non-contact step, an over-length text, or more
      than the per-step/total limits.
- [ ] UNIT: the restore preview contains the frozen version-6 and pre-version-6 safety-plan
      lines with exact counts and correct singular/plural.
- [ ] UNIT: `encodeRecordsCsv` output is unchanged by the presence of plan items.
- [ ] UNIT: the clinician summary text contains no plan text or phone number when the plan
      is populated.
- [ ] UNIT: `SafetyViewModel` add/edit/delete produce the expected state, surface validation
      errors, and renumber positions after a delete; a DAO failure leaves the editor open
      with a retryable error.
- [ ] INSTRUMENTED: `intentFor` produces `ACTION_DIAL` with `tel:988`, `ACTION_SENDTO` with
      `smsto:988`, and `ACTION_VIEW` with the Find A Helpline URL; a repository-wide check
      asserts `ACTION_CALL` appears in no source file.
- [ ] INSTRUMENTED: `SafetyPlanDao` add/edit/delete/renumber round-trips, and Room migration
      5→6 and 1→6 succeed with `safety_plan_items` present and prior data intact.
- [ ] INSTRUMENTED: `replaceEverything` with a version-6 payload inserts plan ids verbatim
      and rolls back entirely on a post-mutation count mismatch; `addRecords` leaves the
      plan untouched; `eraseEverythingAndResetSettings` deletes every item and reports the
      count.
- [ ] UI: the Track footer link and the Profile row open Safety, and Back returns to the
      opening destination after rotation.
- [ ] UI/ACCESSIBILITY: all six headings carry `heading()` semantics in canonical order;
      every action has a ≥48 dp target; the Call button is absent for a contact without a
      dialable number; the action-unavailable message is a polite live region.
- [ ] UI: a missing dialer shows the frozen `dialUnavailable` message with the number still
      visible, and the app does not crash.
- [ ] LINT/BUILD: `.\gradlew.bat test`, `.\gradlew.bat lint` (0 errors, warnings at or below
      the 22-warning baseline), and `.\gradlew.bat assembleDebug` pass.
- [ ] MANUAL: on the API 36 emulator — reach Safety from both entry points and back; tap
      every resource action and cancel in the dialer/messaging app without dialling; compare
      every on-screen string against this spec; add, edit, and delete plan items in all six
      steps; verify light and dark, 200% font, and landscape with nothing clipped; walk
      TalkBack reading order; export a backup and confirm version 6 with the plan; restore a
      version-5 backup and confirm the disclosed empty plan; erase and confirm the plan
      count in the dialog.
- [ ] FAILURE: an import whose plan violates any rule is rejected in full with nothing
      written; a Room write failure keeps the editor open and retryable.
- [ ] BOUNDARY: no `ACTION_CALL`, no new permission, no new dependency, no toolchain change,
      no records-CSV change, no paced-breathing or UI-overhaul work, and no code path that
      reads recorded data to decide anything on the Safety screen.

## Task decomposition

1. Room 5→6: entity, enum, DAO, migration, exported schema 6 — oracle:
   `connectedDebugAndroidTest --tests "*MigrationTest*" --tests "*SafetyPlanDaoTest*"`.
2. `SafetyContent` / `SafetyValidation` / `SafetyActions` pure layer — oracle:
   `test --tests "*SafetyContentTest*" --tests "*SafetyValidationTest*"`.
3. `SafetyViewModel` and state — oracle: `test --tests "*SafetyViewModelTest*"`.
4. `SafetyScreen`, `SAFETY` destination, Track footer link, Profile row — oracle:
   `connectedDebugAndroidTest --tests "*SafetyScreenTest*" --tests "*NavigationTest*"`.
5. Backup version 6, import acceptance, preview copy, erase counts — oracle:
   `test --tests "*BackupImportTest*" --tests "*DataExportTest*" --tests "*ImportPreflightTest*"`.
6. `intentFor` instrumented coverage and the `ACTION_CALL` source scan — oracle:
   `connectedDebugAndroidTest --tests "*SafetyIntentTest*"`.
7. Full oracle sweep and installed-app inspection — oracle: `test`, `lint`,
   `assembleDebug`, `connectedDebugAndroidTest`, `git diff --check`.

## Rollout, migration, and rollback

Additive Room 5→6 with a generated, committed schema 6. No data is transformed and no
column is dropped, so a 5→6 upgrade cannot lose anything. Backup version 6 is a superset of
version 5; older backups remain restorable. There is no downgrade path from version 6 to a
version-5 build — the same one-way rule Phases 10 and 11 already established, and the
version check rejects a newer file with a message telling the user to update rather than
corrupting anything. Rollback of the feature itself is a revert of the branch before merge.

## Open questions and approval gates

None. The user granted full Phase 13 ownership on 2026-08-05 and authorized all in-scope
product decisions, edits, tests, commits, pushes, PR operations, and merge operations. The
crisis-resource and copy decisions that the grant flagged as load-bearing are frozen in D-3
and D-4 with verification sources and dates.
