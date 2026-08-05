# SPEC-paced-breathing: Optional paced-breathing object

Status: FROZEN — APPROVED

Owner: Claude Code agent under full Phase 14 ownership granted by the user on 2026-08-05

Date: 2026-08-05

Last verified commit: N/A (frozen from synchronized `main` at `82ca7041120df0903738645f1a6d46cec44a2fb6`)

## Purpose

The product source keeps a paced-breathing pacer, but keeps it as a UI object with zero
claims: the evidence it would normally rest on does not survive placebo control. Phase 14
adds that object — a circle that keeps a fixed pace for a length the user picks, reachable
only by a user tap, saying nothing about what it does — and records every session as a
dated, exported fact so the app cannot silently perturb the very thing it measures.

The honesty of the copy and the never-auto-offered rule are the product here. The animation
is the easy part.

## Sources reconciled

- `docs/specs/BACKLOG.md`, sole remaining item: "Optional paced-breathing object — requires
  a session entity/export migration and a no-claims, never-auto-offered native pacer spec;
  no breath retention or fast-breathing mode."
- Product source `SPEC.md` from Claude Design project `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`
  (local handoff `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\`), sections
  "Breathing", "Axioms", "Insight grammar", "Regulatory (US)", "Export", "Positioning".
- Prototype `MindScale v2.dc.html`: `BREATH` and `LENS` (lines 761–762), `startBreathe`
  (1052–1067), the overlay markup (714–735), the Track entry pill (167–171), the settings
  toggle (1640), the JSON/CSV export rows (1075, 1084), and the erase reset (1665).
- Repository: `docs/DECISIONS.md` D-004 through D-013, `PROJECT_STATE.md`,
  `FAILED_PATHS.md`, `SPEC-safety-card.md` (D-8, D-10, D-11 — the closest precedent),
  `SPEC-import-restore.md` (D-1 through D-12), `SPEC-settings-data-control.md` (D-5),
  `SPEC-track-phase2-completeness.md` (Invariant 21, paused hides the capture surface).

### Evidence reconciled before designing anything

The product source's own summary, restated because it is the reason this feature is shaped
the way it is and not the shape a wellness app would choose:

| Finding | What it constrains here |
|---|---|
| Fincham 2023 meta-analysis promising (stress g=−0.35, anxiety −0.32, depression −0.40) but controls mostly inactive | Not quotable. An effect size next to a breathing circle is a claim. |
| Same team, two placebo-controlled trials, both **null**: coherent ~5.5 br/min vs 12 br/min placebo (n=400, 4wk); high-ventilation vs 15 br/min placebo (n=200, 3wk) — both arms improved equally | No efficacy claim may appear anywhere. The pacer is a mechanic, not a treatment. |
| STARS: fast = slow in depressed women | Rate is not the active ingredient, so there is no reason to offer a faster one — and reasons not to. |
| Working hypothesis: intentional paced attention interrupting rumination, not respiration | The copy describes attention and mechanics, never physiology or outcome. |
| Schmidt 2000: breathing retraining trended toward **poorer** outcomes in panic; Barlow discourages respiratory control as a safety behavior | The never-auto-offered rule is a safety rule, not a taste rule. It is enforced structurally (D-10). |

## Scope

In scope:

1. A `BREATHING` overlay destination holding one pacing circle, four length choices, and
   the frozen no-claims copy.
2. One fixed cadence — 4500 ms in, 6500 ms out — with exactly two phases and no
   configurability.
3. A session record written whenever the pacer actually ran, holding the real interval.
4. Additive Room 6→7 (`breathing_sessions` plus `track_settings.breathingOn`), JSON backup
   version 7, a `breathing` records-CSV row on the unchanged header, restore acceptance of
   versions 3–7, and erase coverage.
5. One entry point: a Track link, shown only when the setting is on and tracking is not
   paused.

## Non-goals

- **No claim of any kind.** Not efficacy, not "science-backed", not "calming", "relaxing",
  "helps", "reduces", "regulates", or "resets". No protocol name (no "coherent breathing",
  "resonance", "box breathing", "4-7-8", "Wim Hof", "physiological sigh"). No citation, no
  effect size, no study reference in the user interface.
- **No automatic offering, ever.** Not after a high rating, a long episode, a streak of
  entries, a gap, a time of day, a marker, or any other recorded or derived value. Not as a
  pop-up, interstitial, snackbar, notification, banner, or auto-navigation target. The only
  way in is a user tap on a control whose visibility depends on nothing recorded (D-10).
- **No fast breathing and no breath retention.** No mode, preset, hidden setting, debug
  flag, or configuration may produce a rate above the single frozen cadence or introduce a
  hold phase. There are exactly two phases (D-1).
- No default length, no preselected choice, no recommended dose, no "most people choose".
- No streak, count, total, history list, "sessions this week", completion rate, badge, or
  any other surface that would make using it feel owed. Nothing in the app displays past
  breathing sessions; they exist in the export and in the erase/restore disclosures only.
- No breathing session in the Full Log, in Insights, or in the clinician summary (D-11).
- No per-session delete (D-11). Erase and restore are the removal paths.
- No haptic or audio pacing cue, in any form, default or optional. A vibration or tone
  pattern is a portable technique that outlives the screen and is exactly the kind of
  respiratory-control safety behaviour Barlow warns about; the cue is visual and, for
  TalkBack, spoken by the platform from a live region (D-13).
- No background execution, foreground service, `WAKE_LOCK`, alarm, notification, or
  scheduled work. A session exists only while MindScale is open.
- No new dependency, no animation library, no permission, no network, no account, no
  analytics, no toolchain change, no destructive migration, no backup downgrade path.
- No UI-overhaul work; it remains an unscoped backlog item.

## Decisions

### D-1 — Cadence: adopt the prototype's 4500 ms / 6500 ms verbatim, as one frozen constant

**Decided: 4500 ms inhale, 6500 ms exhale, an 11 000 ms cycle, 5.45 breaths per minute.**

The product source requires ~5–6 br/min with an equal-or-longer exhale. 60 000 / 11 000 =
5.4545 br/min and 6500 > 4500, so the prototype already satisfies both constraints exactly.
Changing the numbers would be a change without a reason, and there is no evidence base that
could justify preferring a different figure inside the permitted band — that is the whole
point of the null trials. Adopted unchanged, and recorded as adopted rather than inherited.

`BreathPhase` has exactly two entries. There is no `HOLD`, no pause between phases, no
`Settings`-reachable rate, and no per-length cadence. The durations are `const val`s on
`BreathingPacer` with no setter and no override, so "never fast breathing or breath
retention" is a property of the type, not a promise in a comment. Two tests pin it: the
enum has exactly two entries with the two frozen durations, and a source scan over the
`breathing` package rejects retention/fast-breathing tokens.

### D-2 — Length set: adopt 1 / 3 / 5 / 10 minutes, with nothing preselected

**Decided: exactly the prototype's four choices, in ascending order, none highlighted, and
the pacer does not run until the user picks one.**

"User-chosen duration, no default dose" is a frozen constraint. A preselected chip is a
default dose with extra steps. The idle screen shows the four choices and the cue text
`Choose a length`; there is no Start button to press past a preselection, because there is
no preselection.

One minute is kept as the shortest choice deliberately: the smallest offered commitment
should be small enough that opening the screen is not itself a decision.

### D-3 — A session is recorded whenever it ran, for its real elapsed time

**Decided: reject the prototype's behaviour. Every session that started is recorded, on
completion, on Stop, and on Back, with the actual elapsed interval and no minimum.**

The prototype commits a record only on natural completion, so stopping a five-minute
session after four minutes stores nothing. That is precisely the failure the product source
names: "Timestamp every session into the export — otherwise the app silently perturbs what
it measures." Four minutes of paced breathing perturbs the data whether or not the timer
ran out, and a record the app declines to write is the app editing the user's history.

No minimum threshold is applied. A two-second session is a two-second row. Any floor would
be MindScale deciding which of the user's own actions count, which is the same class of
judgment as inference, and there is no honest number to pick.

The prototype also overshoots: it ends at the first phase boundary at or after the chosen
total, so its "1 min" button runs 66 seconds. **Rejected.** A session ends exactly when the
chosen duration elapses, mid-phase if that is where the clock lands, so the button label is
true. Because the stored interval is the real one either way, this is a copy-honesty
decision rather than a data one.

### D-4 — Session entity: an interval, with no stored "chosen length"

**Decided: `BreathingSession(id, startedAt, endedAt)`. The chosen length is not stored.**

This mirrors `SleepInterval(id, startTs, endTs)`, the shape MindScale already uses for a
period of time, and it maps onto the frozen records-CSV columns `timestamp` and
`end_timestamp` with no header change (D-9).

The prototype's `minutes` field is dropped. Actual elapsed time is the fact; the chosen
length is an intention. Storing both would make the export legible as compliance with a
dose — "chose 10, managed 2" — which is an interpretation, and axiom 4 gives interpretation
to the human. The elapsed duration already carries everything a person needs to see how
much paced breathing happened and when. Recorded here because it is a deliberate departure
from the reference implementation, not an omission.

### D-5 — Clock: wall-clock start, monotonic duration

**Decided: `startedAt` is `System.currentTimeMillis()` at the moment the user picks a
length. `endedAt` is `startedAt + elapsed`, where `elapsed` is the difference of two
`SystemClock.elapsedRealtime()` readings, clamped to the chosen total.**

A wall-clock subtraction can be corrupted by an NTP correction, a manual time change, or a
DST-unrelated jump mid-session, which would produce a negative or absurd interval. A
monotonic delta cannot. Clamping to the chosen total means a scheduling overshoot of a few
milliseconds cannot produce a session longer than the length the user selected, so
`0 <= endedAt - startedAt <= MAX_BREATHING_SESSION_MILLIS` holds by construction and is
re-asserted on import.

Both readings come through a `BreathingClock` interface so the pacer and the ViewModel are
testable on the JVM with virtual time.

### D-6 — Lifecycle: the session lives in the ViewModel and ends only on a real end

**Decided: a session ends when the chosen duration elapses, when the user taps Stop, or
when the user navigates Back out of the breathing destination. It is not tied to
composition or to `Lifecycle.ON_STOP`.**

Both `DisposableEffect.onDispose` and `ON_STOP` fire on a rotation, so either would end and
record a session because the user turned the phone. The ViewModel is Activity-scoped and
survives configuration changes, and the pacer is a pure function of elapsed time, so a
rotation re-derives the current phase and nothing is recorded.

Consequences, accepted and disclosed rather than hidden:

- Backgrounding the app does not stop a running session. The recorded interval is the
  interval the pacer ran, which is the only thing MindScale can honestly observe; whether
  the user was watching is an inference it will not make.
- Process death during a session records nothing. `onCleared` cannot complete a write after
  its scope is cancelled, and the alternative — writing an open row with `endedAt = null` at
  session start, the way an open `SleepInterval` works — reintroduces exactly the ambiguity
  D-4 just removed and adds an open-interval invariant to a foreground-only pacer.
  **This gap is stated in the on-screen copy** (`RECORDING`, D-12), so a user can audit it
  rather than discover a missing row later. The screen is held awake while a session runs
  (D-13), which makes foreground death rare.

### D-7 — The entry point is hidden while tracking is paused

**Decided: the Track link renders only when `breathingOn` is true and `paused` is false.**

This deliberately differs from Phase 13, where the Safety link renders unconditionally
including while paused. The two objects are structurally different: the Safety card reads
and writes nothing recorded and is a crisis resource, whereas **a breathing session writes a
dated record**. Pause means the capture surface is hidden (`SPEC-track-phase2-completeness.md`,
Invariant 21); offering a control that creates records during a pause would contradict what
pause means. Matching the prototype here is a consequence of that reasoning, not the reason.

Both conditions are settings the user set. Neither is a rating, an episode, a count, a
streak, or anything derived from recorded data, so this gating does not weaken D-10.

### D-8 — Settings toggle, default on

**Decided: an additive `TrackSettings.breathingOn` boolean, defaulting to `true`, with the
prototype's toggle copy adopted verbatim.**

Default-on matches the prototype and makes the object discoverable without a tour. It does
not violate axiom 2 ("costs nothing on a good day") because a link demands nothing: it is
one line of text at the bottom of a screen the user already scrolls, below the capture
surface, and above the Safety link, which stays last. Someone who does not want it turns it
off once and it never reappears.

The toggle description is the honest description of the whole feature, so it is frozen
verbatim from the prototype: `A breathing circle you can open. Never offered to you
unprompted.`

### D-9 — Data contract

Additive only. No existing table, column, row, or index is altered.

```sql
CREATE TABLE IF NOT EXISTS `breathing_sessions` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `startedAt` INTEGER NOT NULL,
  `endedAt` INTEGER NOT NULL
);
ALTER TABLE track_settings ADD COLUMN breathingOn INTEGER NOT NULL DEFAULT 1;
```

**JSON backup version 7.** `MAX_BACKUP_VERSION` becomes `7`; `MIN_BACKUP_VERSION` stays `3`.
`encodeBackup` writes `"version": 7`, a `"breathingSessions"` array after `safetyPlan`
ordered by `startedAt` then `id`, and a `"breathingOn"` key inside `settings`. Exact-key
equality per version is extended, never weakened: a version-7 file must contain both keys,
and a version 3–6 file must not. Versions 3–6 restore with an empty session list and
`breathingOn` at its model default, both disclosed verbatim in the preview before anything
is written.

**Records CSV: the header is unchanged, byte for byte.**
`record_type,timestamp,end_timestamp,intensity,kind,chips,note,text` is frozen by Phase 12
and adding a column would make every previously exported CSV unimportable. A breathing
session is written as `breathing,<start ISO>,<end ISO>,,,,,` — the same two columns
`sleep` already uses for an interval, with every inapplicable column empty.

`parseRecordsCsv` accepts the `breathing` row type and `addRecords` appends the sessions.
Export-only was considered and rejected: a user who exports a CSV and imports it back would
have their own file rejected at the first breathing row, which is not a defensible
round-trip.

**Import validation**, held to the same total-rejection policy as everything else:
`startedAt` and `endedAt` are valid instants within the existing future tolerance,
`endedAt >= startedAt`, and `endedAt - startedAt <= MAX_BREATHING_SESSION_MILLIS`
(600 000 ms — the longest session the app can produce). A violation rejects the whole file.
Sessions count against `MAX_RECORDS_PER_COLLECTION` and `MAX_TOTAL_RECORDS`. Duplicate
detection uses `(startedAt, endedAt)` as the natural key, matching how the CSV treats every
other id-less record. Overlap between sessions is deliberately **not** checked: unlike
sleep, breathing has no open-interval invariant to protect, and inventing one would reject
files MindScale itself could legitimately have produced across a restore boundary.

**Erase.** `eraseEverythingAndResetSettings()` deletes every session, `EraseCounts` carries
the count, and the erase dialog discloses it before the user confirms.

**Excluded, verified by test:** the clinician summary, the Report, every copy and share
action, and the Full Log contain no breathing session and no derived value from one.

### D-10 — The never-auto-offered invariant, enforced structurally

`BreathingViewModel`'s constructor accepts `BreathingSessionDao` and a `BreathingClock`,
and nothing else. There is no `EntryDao`, `SleepDao`, `MarkerDao`, `EpisodeSourceDao`,
`TrackSettingsDao`, `ProfileDao`, `DataControlDao`, or episode engine anywhere in the
`breathing` package. Nothing the screen shows can be made to depend on a rating, an
episode, a count, a streak, or a date without changing a frozen constructor and failing
review — the same guarantee Phase 13's `SafetyViewModel` gives the Safety card.

`BreathingSessionDao` exposes `insert` and `count` only. It has no observing query and no
read used by any screen, so there is no code path by which a stored session can influence
anything MindScale displays.

Two tests enforce this rather than describing it:

1. A source scan over `app/src/main/java/com/kieslingdev/mindscale/breathing/` asserting
   that none of the recorded-data DAO or engine type names appear in code (comments are
   stripped, following `NoAutoDialSourceTest`).
2. A reflection assertion that `BreathingViewModel` has exactly one constructor and that its
   parameter types are exactly `BreathingSessionDao` and `BreathingClock`.

Gating on `breathingOn` and `paused` (D-7) happens in `TrackScreen` from the existing
`TrackUiState`, outside the breathing feature entirely.

### D-11 — Sessions are exported facts, not a view

**Decided: breathing sessions appear in the JSON backup, the records CSV, the restore
preview, and the erase dialog. They appear in no other surface, and there is no per-session
delete.**

The backlog item asks for a session entity, an export migration, and a pacer. It does not
ask for a fourth Full Log record type, which would need its own grouping, filtering, edit,
and delete contract and would roughly double this phase.

An Insights view of breathing is rejected on stronger grounds than scope. Any chart placing
sessions next to intensity invites the reader to draw the causal conclusion that two
placebo-controlled trials failed to support — the app would be manufacturing exactly the
inference its own source document says the evidence does not license.

Honest consequence, accepted: a user reading their CSV will find `breathing` rows for
sessions they cannot see or individually delete inside the app. `RECORDING` (D-12) tells
them the rows exist before they create any, the erase dialog counts them, and the restore
preview counts them. Full erase and replace-only restore are the removal paths.

### D-12 — Frozen copy

Rendered exactly, with no substitution, truncation, or reflowing. Adopted from the
prototype where the prototype is already honest; extended where it is silent.

| Key | Exact text | Origin |
|---|---|---|
| `TOP_BAR_TITLE` | `Paced breathing` | adopted |
| `TRACK_LINK` | `Paced breathing` | adopted |
| `TRACK_LINK_DESCRIPTION` | `Open the paced breathing circle` | new (TalkBack) |
| `SETTING_TITLE` | `Paced breathing` | adopted |
| `SETTING_DESCRIPTION` | `A breathing circle you can open. Never offered to you unprompted.` | adopted verbatim |
| `INSTRUCTIONS` | `Through the nose, following the circle. Out for a little longer than in. Stop whenever you like.` | adopted verbatim |
| `NO_CLAIM` | `MindScale makes no claim about what this does. It is a circle that keeps a pace.` | new |
| `RECORDING` | `Each session is saved with its start and end time and appears in your exports. A session is only saved if MindScale stays open until it ends.` | new |
| `CHOOSE` | `Choose a length` | adopted |
| `CUE_IN` | `In` | adopted |
| `CUE_OUT` | `Out` | adopted |
| `CUE_IN_DESCRIPTION` | `Breathe in` | new (TalkBack) |
| `CUE_OUT_DESCRIPTION` | `Breathe out` | new (TalkBack) |
| `DONE` | `Done` | adopted |
| `CLOSE` | `Close` | adopted |
| `STOP` | `Stop` | adopted |
| `SAVE_FAILED` | `That session could not be saved.` | new |
| `lengthLabel(n)` | `1 min`, `3 min`, `5 min`, `10 min` | adopted |
| `lengthDescription(n)` | `1 minute`, `3 minutes`, `5 minutes`, `10 minutes` | new (TalkBack) |
| `runningLength(n)` | `1 minute`, `3 minutes`, `5 minutes`, `10 minutes` | adopted, singular fixed |

Confirmation, checked word by word against the non-goals: no string above claims, implies,
or hints that paced breathing does anything. `INSTRUCTIONS` describes a mechanic (route,
ratio, permission to stop). `NO_CLAIM` is the explicit refusal to claim, mirroring the
Safety card's `HONESTY` block. `RECORDING` is a disclosure of what MindScale writes,
including the D-6 gap. No protocol name, no rate, no citation, no effect size, and no
therapeutic verb appears anywhere in the feature.

Two prototype details are corrected: `runningLength` was always plural, so its one-minute
case read "1 minutes"; and the length buttons get spoken descriptions because "1 min" is
read poorly by a screen reader.

`Stop whenever you like.` is load-bearing and is not softened. It is the sentence that
keeps this from being a prescription.

### D-13 — Accessibility

- The circle is **decorative**: `clearAndSetSemantics {}`. Nothing a TalkBack user needs is
  carried by the animation, its size, or its colour.
- The cue text is the pacing signal and carries `LiveRegionMode.Polite` with
  `contentDescription` `Breathe in` / `Breathe out`. About eleven announcements a minute is
  not chatter here — it *is* the pace, and it is the only non-visual way to follow it.
- The chosen length is a normal, non-live readable node, so it is available on demand and
  never announces itself. No elapsed counter, countdown, or progress bar: a running clock
  would turn the screen into a compliance monitor.
- **Reduced motion:** when `Settings.Global.ANIMATOR_DURATION_SCALE` is `0`, the circle
  snaps between its two phase sizes instead of tweening. The decision is a pure function,
  `animationMillisFor(phaseMillis, animatorScale)`, so it is unit-tested rather than
  eyeballed. The cue text and the live region are unaffected, so the pacer remains fully
  usable with animations disabled.
- Every actionable element is at least 48 dp on both axes: the four length buttons, the
  Stop/Close button, the Track link, and the Settings toggle.
- The screen is one vertically scrolling column with no fixed-height or height-capped
  container, so nothing is clipped at 200% font scale or in landscape.
- Colour comes only from `MaterialTheme.colorScheme` roles. The circle is distinguishable
  from its track by more than hue, and the prototype's low-alpha ink is rejected as it was
  in Phase 13.
- The screen is held awake while a session runs (`View.keepScreenOn`), which needs no
  permission and prevents a display timeout from silently ending a ten-minute session.
- No auto-focus capture, no marquee, and no content change other than the cue and the
  circle, both of which are the feature.

### D-14 — Privacy and logging

- MindScale records **nothing** about opening the breathing screen, choosing a length,
  abandoning a choice, or how often any of it happens. The only write is one row when a
  session ends.
- Nothing about a session is written to Logcat at any level.
- No breathing value enters the clinician summary, the Report, a share, or a copy action.
- `SavedStateHandle` is not used by this feature at all. There is no draft to protect and
  nothing about a session belongs in saved instance state.
- The session row contains two timestamps and nothing else. There is no breath-by-breath
  data, no phase log, no completion flag, and no note field.

## User experience

Entry: a `Paced breathing` link at the bottom of Track, above the Safety link, shown only
when the setting is on and tracking is not paused. Back returns to Track; the existing
destination stack already restores across rotation and process death.

Idle: the circle rests at its small size, the cue reads `Choose a length`, and the four
length buttons are shown with none selected. Below them: `INSTRUCTIONS`, `NO_CLAIM`, and
`RECORDING`. The button reads `Close`.

Running: the pacer starts immediately on the tap. The circle expands over 4500 ms while the
cue reads `In`, then contracts over 6500 ms while it reads `Out`, repeating until the chosen
duration elapses. The chosen length is shown as static text. The button reads `Stop`.

Finished: the cue reads `Done`, the circle returns to rest, the length buttons return, and
the button reads `Close`. Nothing congratulates the user and nothing is offered next.

Every exit records: reaching the end, tapping `Stop`, and pressing Back all write the real
interval. Tapping `Close` from idle or finished writes nothing, because nothing ran.

Error: a failed insert shows `SAVE_FAILED` as a polite live region and leaves the screen
usable. The session is not retried and not silently dropped from the user's view — they are
told. A read error state does not exist because nothing is read.

## Frozen interfaces and data contracts

```kotlin
// data/BreathingSession.kt
@Entity(tableName = "breathing_sessions")
data class BreathingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long
)

// data/BreathingSessionDao.kt — insert and count only; no observing read exists (D-10)
@Dao
interface BreathingSessionDao {
    @Insert suspend fun insert(session: BreathingSession): Long
    @Query("SELECT COUNT(*) FROM breathing_sessions") suspend fun count(): Int
}

// breathing/BreathingPacer.kt — pure, Android-free, no dependency on any recorded value
enum class BreathPhase { INHALE, EXHALE }   // exactly two; no HOLD, ever (D-1)

data class PacerFrame(
    val phase: BreathPhase,
    val phaseElapsedMillis: Long,
    val phaseTotalMillis: Long,
    val completedCycles: Int
)

object BreathingPacer {
    const val INHALE_MILLIS = 4_500L
    const val EXHALE_MILLIS = 6_500L
    const val CYCLE_MILLIS = 11_000L            // == INHALE + EXHALE
    const val REST_SCALE = 0.62f
    const val FULL_SCALE = 1.0f
    fun frameAt(elapsedMillis: Long): PacerFrame
    fun scaleFor(phase: BreathPhase): Float
    fun animationMillisFor(phaseMillis: Long, animatorScale: Float): Int
}

val BREATHING_LENGTHS_MINUTES: List<Int>       // exactly listOf(1, 3, 5, 10), ascending
const val MAX_BREATHING_SESSION_MILLIS = 600_000L

// breathing/BreathingClock.kt
interface BreathingClock {
    fun wallMillis(): Long
    fun monotonicMillis(): Long
}
class SystemBreathingClock : BreathingClock       // the only Android-typed edge

// breathing/BreathingContent.kt
object BreathingCopy { /* every frozen string in D-12 */ }

// breathing/BreathingViewModel.kt — exactly these two parameters (D-10)
sealed interface BreathingStage {
    data object Idle : BreathingStage
    data class Running(
        val minutes: Int,
        val phase: BreathPhase,
        val phaseMillis: Long
    ) : BreathingStage
    data object Finished : BreathingStage
}

data class BreathingUiState(
    val stage: BreathingStage = BreathingStage.Idle,
    val message: String? = null
)

class BreathingViewModel(
    private val sessionDao: BreathingSessionDao,
    private val clock: BreathingClock
) : ViewModel() {
    val uiState: StateFlow<BreathingUiState>
    fun start(minutes: Int)
    fun stop()
    fun leaveScreen()
    fun dismissMessage()
}

// MindScaleApp.kt
enum class AppDestination { TRACK, LOG, INSIGHTS, PROFILE, REPORT, SETTINGS, SAFETY, BREATHING }
```

Amended existing contracts (additive only):

- `MindScaleDatabase`: `version = 7`, `BreathingSession::class` added, `MIGRATION_6_7`
  registered, `breathingSessionDao()` added.
- `TrackSettings`: `+ val breathingOn: Boolean = true`.
- `TrackSettingsDao`: `+ suspend fun setBreathingOn(enabled: Boolean): Int`.
- `DataSnapshot`: `+ val breathingSessions: List<BreathingSession> = emptyList()`.
- `EraseCounts`: `+ val breathingSessions: Int = 0`.
- `BackupPayload`: `+ val breathingSessions: List<BreathingSession> = emptyList()`.
- `RecordsPayload`, `RecordSnapshot`, `ImportCounts`, `RecordCounts`: `+ breathingSessions`.
- `DataControlDao`: `allBreathingSessions()`, `deleteBreathingSessions()`,
  `insertBreathingSessions()`, `breathingSessionCount()`; `snapshot()`, `recordSnapshot()`,
  `replaceEverything()`, `addRecords()`, and `eraseEverythingAndResetSettings()` extended.
- `MAX_BACKUP_VERSION = 7`; `MIN_BACKUP_VERSION` unchanged at `3`.
- `RECORDS_CSV_HEADER` is **unchanged**, byte for byte.

## Invariants

1. `BreathPhase.entries.size == 2`, and the two durations equal 4500 ms and 6500 ms. No
   code path produces a hold phase or a cycle shorter than 11 000 ms.
2. No value shown by the breathing feature derives from an entry, sleep, marker, episode,
   score, safety-plan item, or any setting other than theme and font scale (D-10).
3. The breathing screen is never shown, focused, or navigated to without a user tap.
4. The Track entry point is visible if and only if `breathingOn && !paused`.
5. A session row is written exactly once per started session, on completion, Stop, or Back,
   and never when the pacer did not run.
6. Every stored session satisfies `0 <= endedAt - startedAt <= MAX_BREATHING_SESSION_MILLIS`.
7. Nothing is written to Room, preferences, or a file when the screen is opened, a length
   is considered, or the screen is closed without a session having run.
8. A restore either applies the whole backup, breathing sessions included, or leaves the
   database untouched.
9. Backup versions 3–7 restore; version 8+ is rejected with `NEWER_VERSION`.
10. `RECORDS_CSV_HEADER` and the eight column meanings are unchanged, and a records CSV
    exported by any prior MindScale version still imports.
11. Breathing data never appears in the Full Log, Insights, the clinician summary, a share,
    or a copy action.
12. All Room access is off the main thread; the pacer loop is cancelled with the ViewModel.
13. Frozen copy is compared by exact string equality in tests; no test asserts a substring
    where the whole string is frozen.
14. No haptic, audio, notification, alarm, wake lock, or background worker exists anywhere
    in the feature.

## Android compatibility

- Minimum SDK 26, target 36, compile 36.1 — unchanged. No new API-level branch and no new
  permission. `View.keepScreenOn` and `Settings.Global.ANIMATOR_DURATION_SCALE` are both
  available from API 26 and neither requires a permission.
- Room 6→7 is additive; `exportSchema = true` generates and commits
  `app/schemas/…/7.json`. Migration is tested 6→7 and 1→7 by the existing `MigrationTest`
  pattern.
- Rotation: the ViewModel is Activity-scoped and the pacer is a pure function of elapsed
  monotonic time, so a rotation re-derives the current phase and records nothing. The
  circle's tween restarts from its current value and re-synchronises at the next phase
  boundary; the cue text is correct immediately.
- Process death: a running session is lost, by design and disclosed on screen (D-6). The
  destination stack itself restores through the existing saveable in `MindScaleApp`.
- Back navigation: Back from `BREATHING` ends any running session and pops the overlay.
- Compose: one recomposition per phase boundary, not per frame — the circle is driven by
  `animateFloatAsState` with the phase duration, exactly as the prototype uses a CSS
  transition. The ViewModel's loop sleeps until the next boundary rather than polling.

## Acceptance criteria

- [ ] UNIT: `BreathPhase.entries` has exactly two elements, `INHALE` then `EXHALE`;
      `INHALE_MILLIS == 4500L`, `EXHALE_MILLIS == 6500L`, `CYCLE_MILLIS == 11000L`, and
      `60_000.0 / CYCLE_MILLIS` is within 5.0..6.0 breaths per minute with
      `EXHALE_MILLIS >= INHALE_MILLIS`.
- [ ] UNIT: `BreathingPacer.frameAt` returns the correct phase, phase-elapsed, and cycle
      count at 0, 4499, 4500, 10999, 11000, and 599 999 ms, and rejects a negative elapsed.
- [ ] UNIT: `animationMillisFor` returns 0 when the animator scale is 0 and the phase
      duration otherwise.
- [ ] UNIT: `BREATHING_LENGTHS_MINUTES == listOf(1, 3, 5, 10)`.
- [ ] UNIT: every `BreathingCopy` constant and function result equals its frozen string
      exactly (literal-text assertions), including the singular/plural length forms.
- [ ] UNIT: no `BreathingCopy` string contains any of a frozen banned-word list covering
      efficacy verbs, science-backing language, and protocol names.
- [ ] UNIT: `BreathingViewModel` with a fake clock and fake DAO records a session on natural
      completion with `endedAt - startedAt` exactly the chosen duration; on `stop()`
      mid-session with the real partial elapsed; and on `leaveScreen()` mid-session. It
      records nothing when the screen is opened and closed without starting, nothing on a
      second `stop()`, and reports `SAVE_FAILED` when the DAO throws.
- [ ] UNIT: a wall-clock jump between start and end does not change the recorded interval,
      and an overshooting monotonic reading is clamped to the chosen total.
- [ ] UNIT: `BreathingViewModel` has exactly one constructor whose parameter types are
      exactly `BreathingSessionDao` and `BreathingClock` (reflection).
- [ ] UNIT: a source scan over the `breathing` package finds no recorded-data DAO or engine
      type name in code, and no retention/fast-breathing token; the comment stripper is
      itself tested.
- [ ] UNIT: `encodeBackup` emits `"version": 7`, a `breathingSessions` array ordered by
      `startedAt` then `id`, and `breathingOn` inside `settings`; re-encoding a parsed
      backup reproduces the input byte for byte.
- [ ] UNIT: `parseBackup` accepts a version-7 file, rejects version 8 with `NEWER_VERSION`,
      restores versions 3–6 with an empty session list and the default `breathingOn`, and
      totally rejects a version-7 file with a negative interval, an over-length interval, a
      duplicate id, or a missing/extra key.
- [ ] UNIT: `encodeRecordsCsv` writes `breathing,<start>,<end>,,,,,` rows, the header is
      byte-identical to the frozen constant, and a records CSV without breathing rows still
      parses.
- [ ] UNIT: `parseRecordsCsv` accepts `breathing` rows, rejects one with a non-empty
      intensity/kind/chips/note/text column, rejects a missing `end_timestamp`, and rejects
      an interval outside the permitted bounds.
- [ ] UNIT: the restore preview contains the frozen version-7 and pre-version-7 breathing
      lines with exact counts and correct singular/plural.
- [ ] UNIT: the clinician summary text is byte-identical with and without breathing sessions
      present.
- [ ] INSTRUMENTED: `BreathingSessionDao` insert/count round-trips, and Room migration 6→7
      and 1→7 succeed with `breathing_sessions` and `track_settings.breathingOn` present and
      prior data intact.
- [ ] INSTRUMENTED: `replaceEverything` with a version-7 payload inserts session ids
      verbatim and rolls back entirely on a post-mutation count mismatch; `addRecords`
      appends sessions; `eraseEverythingAndResetSettings` deletes them and reports the count.
- [ ] UI: the Track link opens the breathing screen and Back returns to Track after
      rotation; the link is absent when the setting is off and absent while paused.
- [ ] UI: picking a length starts the pacer, the cue advances `In` → `Out`, `Stop` returns
      to the finished state, and a completed session shows `Done`.
- [ ] UI/ACCESSIBILITY: the cue is a polite live region with the frozen spoken
      descriptions; the circle carries no semantics; every control is ≥48 dp; the frozen
      screen copy is present verbatim; the layout scrolls with nothing clipped at 200% font
      and in landscape.
- [ ] LINT/BUILD: `.\gradlew.bat test`, `.\gradlew.bat lint` (0 errors, warnings at or
      below the 22-warning baseline), and `.\gradlew.bat assembleDebug` pass.
- [ ] MANUAL: on the API 36 emulator — reach the pacer from Track and back; run a full
      session at each of the four lengths and time the cycle against a stopwatch; compare
      every on-screen string against D-12; confirm no efficacy claim anywhere; log a 10 and
      confirm nothing is offered, in every app state; verify light/dark, 200% font,
      landscape, and TalkBack reading order; export a backup and a CSV and confirm the
      session rows; restore a version-6 backup and confirm the disclosed empty session list;
      erase and confirm the session count in the dialog.
- [ ] FAILURE: an import whose breathing data violates any rule is rejected in full with
      nothing written; a failed session insert shows `SAVE_FAILED` and leaves the screen
      usable.
- [ ] BOUNDARY: no new permission, dependency, or toolchain change; no records-CSV header
      change; no haptic, audio, notification, alarm, wake lock, or background worker; no
      breathing value in the Full Log, Insights, or the clinician summary; no code path that
      reads recorded data to decide anything in the breathing feature; no UI-overhaul work.

## Task decomposition

1. Room 6→7: entity, DAO, migration, `breathingOn` column, exported schema 7 — oracle:
   `connectedDebugAndroidTest --tests "*MigrationTest*" --tests "*BreathingSessionDaoTest*"`.
2. `BreathingPacer` / `BreathingCopy` / `BreathingClock` pure layer — oracle:
   `test --tests "*BreathingPacerTest*" --tests "*BreathingContentTest*"`.
3. `BreathingViewModel` and the structural guards — oracle:
   `test --tests "*BreathingViewModelTest*" --tests "*BreathingIsolationTest*"`.
4. `BreathingScreen`, the `BREATHING` destination, the Track link, the Settings toggle —
   oracle: `connectedDebugAndroidTest --tests "*BreathingScreenTest*" --tests "*NavigationTest*"`.
5. Backup version 7, CSV row, import acceptance, preview copy, erase counts — oracle:
   `test --tests "*BackupImportTest*" --tests "*DataExportTest*" --tests "*ImportPreflightTest*" --tests "*RecordsCsvImportTest*"`.
6. Full oracle sweep and installed-app inspection — oracle: `test`, `lint`,
   `assembleDebug`, `connectedDebugAndroidTest`, `git diff --check`.

## Rollout, migration, and rollback

Additive Room 6→7 with a generated, committed schema 7. No data is transformed and no column
is dropped, so a 6→7 upgrade cannot lose anything; `breathingOn` defaults to `1` for every
existing row, which matches the new-install default. Backup version 7 is a superset of
version 6 and older backups remain restorable. The records CSV header is unchanged, so every
CSV MindScale has ever written still imports. There is no downgrade path from version 7 to a
version-6 build — the same one-way rule Phases 10 through 13 established, with the version
check rejecting a newer file rather than corrupting anything. Rollback of the feature itself
is a revert of the branch before merge.

## Open questions and approval gates

None. The user granted full Phase 14 ownership on 2026-08-05 and authorized all in-scope
product decisions, edits, tests, commits, pushes, PR operations, and merge operations. The
three decisions the grant flagged as load-bearing — cadence, length set, and copy — are
frozen in D-1, D-2, and D-12 with the reasoning for adopting or rejecting each prototype
detail, and the never-auto-offered rule is frozen as a structural guarantee in D-10 rather
than as a convention.
