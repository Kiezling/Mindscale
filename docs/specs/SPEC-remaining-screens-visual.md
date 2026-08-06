# SPEC-remaining-screens-visual: Settings, Profile, Report, Safety, Breathing, and the closing audit

Status: FROZEN — D-1 through D-17 frozen 2026-08-06 before any application-code edit

Owner: Claude Code (Phase 18 of 18), on the user's instruction of 2026-08-06

Date: 2026-08-06

Branch: `agent/phase18-remaining-screens`, created from synchronized `main` at
`af273f915cb83be6506b0aa5e5859c6743be0676`

Governed by, and subordinate to: `docs/specs/SPEC-visual-foundation.md` (D-1 through D-25),
`docs/specs/SPEC-track-and-log-visual.md` (D-4's measured contrast tables, D-15's component
corrections, D-19's not-copied list), and `docs/specs/SPEC-insights-visual.md` (D-7's measured
contrast tables, D-19's open type question). All three bind this phase. Where a measurement
already exists in one of them, this phase **reuses** it rather than re-deriving it.

## Purpose

Phase 15 installed the token and component foundation and dressed the app chrome. Phase 16 dressed
Track and Full Log. Phase 17 dressed Insights and settled the intensity ramp. Five screens still
carry their pre-Phase-15 layout under a correct foundation: Settings, Profile, Report, Safety, and
the Breathing body.

This phase dresses those five and then closes the overhaul with the audit D-14 of the foundation
deferred to it: no undocumented dimension literal outside `ui/theme`, every remaining dialog action
label uppercased, the `labelSmall` question `SPEC-insights-visual.md` D-19 left open resolved with
evidence, and every screen compared against `docs/design/reference/` with the result — match and
mismatch — written down.

This phase changes how the app looks. It changes nothing about how it works.

## Sources reconciled

Authority order, highest first, unchanged from Phase 15:

1. `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html` — the
   `:root` block, the `.ms-dark` override, and the inline `style=""` strings.
2. `docs/design/reference/` — the fourteen rendered screenshots.
3. The prototype's own layout, which is advisory only (D-22 of the foundation).

### Screenshot coverage for this phase, stated before the work starts

| Screen | Dark | Light |
|---|---|---|
| Settings | `dark-settings-top.png`, `dark-settings-middle.png`, `dark-settings-bottom.png` | `light-settings-top.png`, `light-settings-bottom.png` |
| Breathing | **none** | `light-breathing.png` |
| Profile | **none** | **none** |
| Report | **none** | **none** |
| Safety | **none** | **none** |

Three of the five screens this phase owns have **no reference screenshot in either theme**, and
Breathing has no dark capture. Profile, Report, and Safety are therefore derived from the HTML and
from the idioms the eight captured screens establish, and any claim that they "match the design"
would be a claim about a picture that does not exist. The completion report says so in those words.
This is the same discipline Phases 16 and 17 held for light Full Log and for light Insights top.

One further honesty: the design's Settings screenshots show four controls MindScale deliberately
does not have — `CHANGE IS DRAWN OVER`, `Require Face ID`, `Replace with sample data`, and the
version/tagline footer — and one MindScale control the design does not show, `Bring data back`.
None is added and none is removed. D-25 of the foundation and D-16 below carry the reasons.

### Measured starting state

Taken from the tree at `af273f915cb83be6506b0aa5e5859c6743be0676`:

| Screen | Lines | testTags | `.dp`/`.sp` literals |
|---|---|---|---|
| `settings/SettingsScreen.kt` | 489 | 16 | 12 |
| `report/ProfileScreen.kt` | 284 | 20 | 22 |
| `report/ReportScreen.kt` | 247 | 10 | 14 |
| `safety/SafetyScreen.kt` | 362 | 23 | 17 |
| `breathing/BreathingScreen.kt` | 260 | 10 | 8 |
| **This phase** | **1,642** | **79** | **73** |

Dimension literals in `app/src/main/java`, whole tree: **138**, of which 26 are the scale
definitions in `ui/theme/` itself. Of the remaining 112, **39** are already named private constants
with comments — Insights 31, Track 6, Log 2 — and **73** are the undocumented literals this phase
converts. The audit in D-14 therefore counts *documented versus undocumented*, not raw occurrences.

Baselines to hold: **428/428** JVM tests, **239/239** connected tests, lint 0 errors at the
unchanged 22-warning baseline, `assembleDebug`.

## Scope

The bodies of `SettingsScreen.kt`, `ProfileScreen.kt`, `ReportScreen.kt`, `SafetyScreen.kt`, and
`BreathingScreen.kt`; layout fixes L-5 and L-6; the closing audit; and nothing else.

## Non-goals

- Any behavior change whatsoever. D-1 of the foundation is the governing rule.
- Any copy edit. Every string, including every `SafetyCopy` and `BreathingCopy` constant, is
  byte-identical afterwards.
- **Re-verifying `SafetyCopy`'s crisis numbers and `VERIFIED_ON`.** They need periodic
  re-verification against operator sources (`PROJECT_STATE.md` "Next tasks" item 2) and that is
  overdue, but it is a *content* change to a safety feature and it does not belong in a visual
  phase. It is flagged in D-12 and in the completion report, not done here.
- Any change to Track, Full Log, Insights, the chrome, or `IntensityRamp.kt` (settled by D-017).
- Any dependency, permission, toolchain, Room schema, backup, CSV, export, import, or migration
  change.
- Retiring the 39 documented literals in Insights, Track, and Log. They are already what D-14 asks
  for: a named constant with a comment saying why it is not a token.

## Decisions

### D-1 — The visual-only rule, restated with this phase's numbers

No feature is added, removed, renamed, reordered, or rewired. No navigation change, no new entry
point, no control that was not there, no control taken away. No copy edits. No data, storage,
export, or migration change. No dependency, permission, or toolchain change.

The proof is objective and mechanical:

```
git diff --name-status af273f9 HEAD -- app/src/test app/src/androidTest
```

Every line must be `A`. A single `M` is a defect in this phase, not a test to update. **239/239**
connected and **428/428** JVM must pass with no pre-existing test file edited. New tests may be
added; existing assertions may not be relaxed, retargeted, or deleted.

Two further mechanical checks, as in Phases 16 and 17: a sorted diff of every `testTag` call site
across the five screens must be empty at 79 tags, and a sorted diff of every double-quoted literal
must show no removal and no addition outside KDoc and `LazyColumn` item keys.

### D-2 — Fidelity: careful, not pixel-perfect

Copy the elegance, colour, and theme with care. Do not copy a layout flaw and do not copy a value
that fails an accessibility floor. Every divergence in this phase is numbered: D-5, D-6, D-7, D-9,
D-10, D-13, and D-16.

### D-3 — L-5 is on Profile, not Settings, and the record is corrected rather than obeyed literally

`SPEC-visual-foundation.md` D-22 records L-5 as "**Settings'** DATE / PHQ-8 / GAD-7 / ADD row is
crowded at the right". The diagnosis is right about the row and wrong about the screen.

The prototype puts its external-totals row on the Settings page (`dark-settings-middle.png`,
`SCORES FROM ELSEWHERE`). MindScale does not: `SPEC-clinician-report-profile.md` put external
totals on **Profile**, and `ProfileScreen.kt` owns the whole of that row today — the instrument
selector, `score_date`, `score_total`, and the `Add total` action. `SettingsScreen.kt` contains no
date field, no PHQ-8 or GAD-7 control, and no `ADD`. Moving that row to Settings to make the
screenshot literally true would be a navigation change, which D-1 forbids outright.

**L-5 is therefore implemented on `ProfileScreen.kt`'s `score_form` section**, and D-22's table is
amended here rather than silently reinterpreted. This is the second time a recorded layout flaw has
needed diagnosis before correction: Phase 17 found L-3's flaw was the prototype's flex layout and
that Compose's `weight(1f)` already satisfied it. Diagnose, then correct — and correct the record
when the diagnosis moves.

The correction itself is unchanged from what D-22 froze: even the row's gaps, and give the trailing
action a real gutter rather than letting it sit flush against the row edge. Implemented exactly as
Phase 16 implemented L-4 — the gutter sits on a wrapper `Box`, not on the action's own modifier, so
the control's reported bounds stay its touch area and the gutter is measurable from outside.

### D-4 — L-6 is already satisfied by the component, and this phase is the first to prove it on a screen

`MsSegmentedControl` has carried `Modifier.weight(1f)` on every segment since Phase 15, which is
exactly L-6's correction. But no screen has ever called it: the only call sites in the tree are in
`src/debug/…/DesignGallery.kt`. Settings' four choice rows are `FilterChip`s in a
`horizontalScroll`, which is neither the design's control nor equal-width.

So L-6 is *verified, not reinvented* — and verification means putting the component on the real
screen and pinning the result, the way Phase 17 pinned L-3. `SettingsVisualTest` asserts that every
segment of every choice row shares one width to within 1 px, at 100% **and** at 200% font.

Two consequences of the conversion are recorded rather than discovered later:

1. **The horizontal scroller goes away.** Equal-weight segments are all visible at once, so every
   choice stays reachable without a scroll — reachability improves rather than degrades. This is a
   rendering change of the same bounded kind as D-17 of the foundation, and what must survive it is
   listed in D-6 below.
2. **`SYSTEM` is the widest label in the narrowest row** — three segments, six characters, at
   `labelLarge`'s 0.229 em tracking. See D-5.

### D-5 — A fifth carry-over correction to the component layer: `MsSegmentedControl` must not clip

`MsSegmentedControl` passes `maxLines = 1` with no `overflow`, which resolves to
`TextOverflow.Clip`. At 200% font a three-segment row gives `SYSTEM` roughly a third of the screen
width, and six tracked characters at 21 sp is the same arithmetic that clipped the `INSIGHTS`
navigation tab in Phase 15 and that made Phase 16 remove `maxLines` from Log's filter field.

`maxLines = 1` is removed, so a long segment label wraps rather than losing characters. This is the
fifth Phase-15 component defect found by being the first phase to put that component on a real
screen, after `MsPillButton`'s border, `MsChip`'s border, `MsChip`'s touch target, and
`MsCircularHeaderButton`'s ring (D-15 of `SPEC-track-and-log-visual.md`). It is corrected here for
the same reason those four were: leaving it would ship a component that clips while this spec
claims D-23 holds.

`MsSegmentedControl` also gains one defaulted parameter:

```kotlin
optionModifier: (Int) -> Modifier = { Modifier }
```

so a caller can attach per-segment semantics without the component inventing any. This is required,
not decorative: `NavigationTest.holdSettingIsReachableAndPersistsAcrossRecreation` asserts
`onNodeWithContentDescription("24h, selected")`, and that description belongs to the caller under
D-15's rule that a component never owns a tag or a description. The parameter is defaulted, so the
gallery's three existing call sites are unchanged.

No component gains state, a side effect, navigation, or a `testTag`. Invariant 7 of the foundation
still holds.

### D-6 — What must survive the `FilterChip` → `MsSegmentedControl` conversion

Bounded exactly, because this is the largest structural change in the phase:

- the visible strings `Light`, `Dark`, `System`, `12-hour`, `24-hour`, `8h`, `12h`, `16h`, `24h`,
  restored in original case by D-11 of the foundation, so `onNodeWithText("24h").performClick()`
  still resolves to a clickable node;
- the content description `"<label>, selected"` / `"<label>, not selected"` on the node that
  carries the click, asserted twice by `NavigationTest`;
- the `selected` semantics state;
- the `onSelected` callbacks — `setTheme`, `setHourFormat`, `setHoldDuration`;
- a minimum 48 dp target height per segment;
- the order of the values, which comes from the enum and is never sorted here.

The same conversion is applied to Profile's two instrument chips (`score_instrument_PHQ_8`,
`score_instrument_GAD_7`) and to Report's six range chips (`report_range_*`), which are the same
`FilterChip` idiom. Both are asserted with `assertIsSelected()` by
`ProfileReportScreenTest`, so `selectable(selected = …)` semantics is load-bearing on both.

**Report's six ranges stay a horizontally scrolling row of `MsChip`s rather than becoming a
segmented control**, because six equal segments at 200% font would give each about 55 dp for a
label like `90 days`. The design's own range control on Insights is a chip row, Phase 17 built it
that way, and Report's is the same control on a different screen.

### D-7 — Control-boundary contrast, reusing the measurements Phases 16 and 17 already made

`SPEC-track-and-log-visual.md` D-4 and `SPEC-insights-visual.md` D-7 measured the design's
`rgba(ink,.07–.16)` and `rgba(gold,.4–.55)` border idioms and found every one of them below D-23's
3:1 non-text floor. Those idioms recur throughout Settings, Profile, Report, Safety, and Breathing.
**This phase re-uses those figures rather than re-deriving them**, and `MsRemainingScreensContrastTest`
recomputes them with the same channel, compositing, and rounding code so the tables cannot drift.

Already measured and settled, applied here unchanged:

| Element in this phase | Design value | Light bg / card | Dark bg / card | Adopted |
|---|---|---|---|---|
| Segmented-control container | `rgba(ink,.09)` | 1.21 / 1.20 | 1.22 / 1.26 | **kept** — it is a decorative container around segments that are themselves marked by fill and weight; see below |
| Underlined field rule (`DATE`, anchors, name, totals) | `rgba(ink,.16)` | 1.41 / 1.41 | 1.51 / 1.57 | `ms.outline` — 3.47 / 3.49 light, 3.47 / 3.51 dark |
| Settings row separators | `rgba(ink,.07)` | ~1.16 | ~1.18 | **kept** — a separator, exempt by D-6 of the foundation |
| Chip / instrument border, unselected | `rgba(ink,.14–.16)` | 1.35 / 1.41 | 1.42 / 1.57 | `ms.outline`, already `MsChip`'s border since Phase 16 |
| Breathing outer ring | `rgba(gold,.45)` | 1.57 / 1.60 | 2.62 / 2.61 | `ms.gold` at full opacity — 3.05 / 3.15 light, 8.60 / 8.04 dark |
| Breathing close pill border | `rgba(gold,.45)` | 1.57 / 1.60 | 2.62 / 2.61 | `ms.gold`, already `MsPillButton`'s border since Phase 16 |

The segmented-control container is the one place this phase keeps a faint ink hairline on something
adjacent to controls, and the reason is specific rather than convenient: **the segments inside it
are not bounded by that container.** A selected segment carries an ink fill and `onInk` lettering;
an unselected one carries the page. The container is the design's quiet frame around a group, not
any control's only boundary, and D-6 of the foundation exempts exactly that. It is asserted as
*below* 3:1 on purpose, the way Phases 16 and 17 asserted their exemptions, so a later phase that
"fixes" it into a heavy ring breaks a test on the way.

D-4's constraint 1 is re-checked and holds: **full light gold is a legal control boundary on `bg`
and `card` only** — 2.67:1 on `surfaceContainerHighest`. Every gold boundary this phase paints —
the Breathing ring, the Breathing close pill, `MsPillButton`, `MsCard(emphasized = true)` — sits on
`bg` or on `card`. Nothing paints gold on a container step.

Three Material surfaces currently painted on these screens are replaced rather than measured,
because they are Material's tonal ladder rather than the design's:
`Surface(tonalElevation = 1.dp/2.dp)` on Profile's score rows, Safety's resource cards and plan
rows, and Report's `secondaryContainer` / `errorContainer` / `tertiaryContainer` banners. All become
`MsCard` on the design's `card` with a hairline, or `MsCard(emphasized = true)` where the design
raises a card's status. D-13 of the foundation already forbids tonal elevation above 0 dp outside
the dialog and the armed pad; this phase is where the last four call sites go.

### D-8 — Settings: the design's screen

`dark-settings-top.png`, `dark-settings-middle.png`, `dark-settings-bottom.png`,
`light-settings-top.png`, `light-settings-bottom.png`.

- **Section titles become eyebrows.** `APPEARANCE`, `TIME FORMAT`, `AN ENTRY ENDS AFTER`,
  `WHAT THE NUMBERS MEAN TO YOU`, `WHAT WAS HAPPENING`, `YOUR DATA`, `BRING DATA BACK` — uppercase,
  tracked, at `MsEyebrow`'s compliant emphasis rather than the prototype's `rgba(ink,.38)` at
  2.43:1. The strings are untouched; only the case and the tracking are presentation (D-11 of the
  foundation).
- **Choice rows become segmented controls** (D-4, D-5, D-6).
- **The three anchor fields become one card of hairline-separated rows**, each a gold numeral in a
  fixed leading column beside the field, exactly as the design draws them. The numerals `2`, `5`,
  and `8` are already inside the existing labels (`"2 — noticeable"`), so no string is added: the
  label is rendered as it is, and the card is the container.
- **The five switches become one card of hairline-separated rows**: title at `titleSmall`,
  description beneath at `bodySmall` at the `quaternary` level, `Switch` trailing. The design's own
  row.
- **The data and import actions become label-plus-trailing-action rows** on a card, which is the
  design's `Export everything … JSON` pattern. MindScale's labels are its own and are unchanged;
  no `JSON`/`CSV` trailing string is invented, because that would be a copy addition (D-16).
- **`Export, then erase everything` is painted in `danger`**, as the design paints it. It keeps its
  tag, its callback, and its confirmation dialog.
- Every one of the 16 testTags survives, and the `LazyColumn` keeps one item per current item so
  its keys are stable.

**`SettingsFocus`'s two scroll indices are recomputed, not preserved.** `SettingsFocus.ANCHORS` and
`SettingsFocus.DATA` currently scroll to items 4 and 12. If grouping changes which index holds the
anchors and the data sections, the *index* must move so the *behavior* does not: the deep link's
contract is "focus the anchors section" and "focus the data section", not "scroll to item 4".
Because that is a behavioral contract with no existing assertion, this phase adds one:
`SettingsVisualTest` asserts that rendering at `SettingsFocus.DATA` brings `export_backup` into
view and that rendering at `SettingsFocus.ANCHORS` brings `anchor_2` into view. If grouping leaves
the indices unchanged, the constants are unchanged and the assertion still earns its place.

### D-9 — Profile: the design's screen, and L-5

`ProfileScreen.kt` has no reference screenshot. It is built from the idioms the Settings captures
establish, and the completion report says so.

- Section titles become eyebrows, as in D-8.
- The name field becomes the design's underlined field: no fill, no ring, a 1 dp bottom rule at
  `outline` (D-7). Its supporting text stays exactly the string it is.
- `Clinician summary`, `Settings`, and the Safety row become full-width label-plus-chevron-free
  rows on a card, the same treatment as Settings' data rows. **They keep their order, their tags,
  and their position in the item list**, because `NavigationTest` and `ProfileReportScreenTest`
  both click `profile_open_settings` and `profile_open_report` *without scrolling first*. That is a
  hard geometric constraint on this screen: the identity, stats, and actions sections must still
  fit one viewport at 100% font. `ProfileVisualTest` pins it.
- Stored external totals become hairline-separated rows on one card, replacing
  `Surface(tonalElevation = 1.dp)`.
- **L-5**: the `score_form` row — instrument selector, `score_date`, `score_total`, and the
  `Add total` action — gets even gaps and a real trailing gutter on the action (D-3).

### D-10 — Report: the design's centred ink pill, and a monospace exception

`ReportScreen.kt` has no reference screenshot.

- The range row becomes `MsChip`s (D-6).
- The privacy banner, the error banner, and the retained-document banner become `MsCard`s — the
  privacy one `emphasized`, because the design's gold-bordered card is exactly "this one matters"
  and the sentence is about sensitive health information leaving the app. The `errorContainer`
  banner's text moves to `ms.danger` on `card`, which measures 6.60:1 light and 5.87:1 dark (D-8 of
  the foundation).
- `Copy`, `Share`, and `Save as text` become `MsTextAction`s on one baseline with even gaps — the
  L-2 treatment Phase 16 froze for Track's entry rows, applied to the same shape of problem.
- **`report_text` keeps `FontFamily.Monospace`.** This is the phase's one deliberate refusal of
  Instrument Sans, and it is not a style preference: the clinician summary is a fixed-width text
  document whose alignment carries meaning, it is selectable, and it is the exact byte sequence the
  Copy, Share, and Save actions hand out. Rendering it in a proportional face would misrepresent
  what the user is about to send. Recorded here so a later reader does not "finish" the restyle.

### D-11 — Safety: a safety feature that is being repainted, and nothing else

`SafetyScreen.kt` has no reference screenshot. Three constraints outrank fidelity on this screen.

1. **Nothing about how a control reaches a dialer changes.** `SafetyIntents.kt` is untouched.
   `ACTION_DIAL` / `ACTION_SENDTO` / `ACTION_VIEW` built with `Uri.fromParts` stay exactly as
   `SafetyIntentTest` pins them, `ACTION_CALL` stays forbidden, and no permission is added. This
   phase touches `SafetyScreen.kt` and no other file in `safety/`.
2. **Reaching help must not require reading a paragraph first**, visually or in TalkBack order
   (`SPEC-safety-card.md` D-3). The card order stays name → actions → detail. A restyle that moved
   the detail above the actions to look calmer would be a real harm.
3. **The crisis actions stay filled buttons, not bare text actions.** The design's idiom is
   `background:none;border:none` for every action, and this is the one place MindScale should not
   follow it: `resource_action_*` are the controls a person in crisis has to find, and a bare gold
   label is a weaker affordance than a filled one. They are restyled onto the design's palette and
   shape — ink fill, `onInk` lettering, 999 dp pill, tracked uppercase label — which is the
   design's *selected* treatment used as emphasis, measured at 11.98:1 light and 13.93:1 dark by
   D-20 of the foundation. Prominence is the point; the divergence is deliberate and recorded.

Everything else follows the design: resource cards and plan rows become `MsCard`s on hairlines
rather than tonally elevated Material surfaces; section headings become eyebrows while keeping
their `heading()` semantics and their tags; `Add`, `Edit`, and `Delete` become `MsTextAction`s,
with `Delete` in the `Danger` tone; and the plan editor's two fields become underlined fields.

`SafetyCopy` is not touched — not a character, and not `VERIFIED_ON`. Its re-verification is
flagged in the completion report as overdue and out of scope (Non-goals).

### D-12 — Breathing: the body only, and the non-inference guarantee is structural

`light-breathing.png` is the only capture; there is no dark one.

Phase 15's D-18 already made this screen full-bleed. This phase owns its body:

- The circle becomes the design's pair — an outer ring at full `ms.gold` (D-7) around an inner fill
  that scales, on the design's warm `sleepBand`-adjacent fill rather than Material's
  `secondaryContainer`. Both boxes keep `clearAndSetSemantics { }` and the inner one keeps the
  `breathing_circle` tag, so `theCircleItselfIsDecorativeAndCarriesNoSemantics` still passes.
- **The cue moves inside the circle**, as the design draws it, at the design's tracked uppercase
  treatment through `MsUppercaseText`. `breathing_cue` keeps its tag, its `contentDescription`, and
  its `Polite` live region, and both are asserted by `BreathingScreenTest`. This is a stacking
  change, not a structural one: the cue is still one `Text` node with the same semantics, drawn
  over the circle instead of below it.
- The running-length line becomes an eyebrow; the close control becomes an `MsPillButton`; the
  instruction, no-claim, and recording lines become centred paragraphs at the compliant emphasis
  levels.

**The non-inference guarantee is not touched, and cannot be by this phase's construction.**
`BreathingViewModel`'s constructor takes `BreathingSessionDao` and `BreathingClock` and nothing
else; no parameter is added, no recorded value is routed to the screen, and the source scan and the
constructor-reflection test that enforce it are pre-existing test files this phase may not edit.
The only file in `breathing/` this phase changes is `BreathingScreen.kt`.

### D-13 — Dialog action labels: the last three sites

D-19 of the foundation deferred uppercasing the 9 `AlertDialog` action labels to the phase that
owns each screen. Phase 16 did Track's three and Log's two. This phase does the remaining four
call sites on the three screens it owns:

| File | Actions |
|---|---|
| `SettingsScreen.kt` | `confirm_import` / `cancel_import`, `confirm_erase` / Cancel |
| `ProfileScreen.kt` | `score_delete_confirm` (`"Delete"`, line 252) / `score_delete_cancel` |
| `SafetyScreen.kt` | `plan_delete_confirm` (`SafetyCopy.DELETE_CONFIRM`) / `plan_delete_cancel`, `plan_save` / `plan_cancel` |

The technique is Phase 16's D-3, unchanged: **wrap the label in `MsUppercaseText` inside the
existing `TextButton`.** The button is not replaced, because `assertIsNotEnabled` and every other
assertion target lives on that node — `confirm_import` and `cancel_import` are disabled while a
transaction runs, and `plan_save` is disabled while the editor saves. The colour is left
unspecified so it inherits `TextButton`'s content colour, which D-9 of the foundation resolves to
`goldText`.

`MsDialog` stays `AlertDialog` and the import preview's scroll behaviour is untouched; that scroll
is what fixed the Phase 12 clipping defect at 200% font (D-19 of the foundation).

### D-14 — The closing audit: documented versus undocumented, not counted

D-14 of the foundation promised that Phase 18 "asserts that no `.dp` or `.sp` literal outside
`ui/theme` remains that is not either a token reference or a documented one-off."

The assertion is written as a JVM test over the source tree, `MsDimensionAuditTest`, because a
promise nobody can run is not an acceptance criterion. It reads every `.kt` file under
`app/src/main/java` except `ui/theme/`, extracts every `<number>.dp` and `<number>.sp` literal, and
fails on any that is not either:

1. the initialiser of a `private val` whose declaration carries a KDoc or `//` comment — the
   documented one-off; or
2. inside a KDoc or comment itself.

Expected result after this phase: 39 documented literals — `InsightsScreen.kt` 31,
`TrackScreen.kt` 6, `LogScreen.kt` 2 — and **zero** undocumented ones. The 73 literals across this
phase's five screens become `MsSpacing` references or named documented constants as each screen is
restyled, which is D-14's own rule: literals convert in the phase that already restyles that
screen.

The test is deliberately a source scan rather than a lint rule: a custom lint check would be a
toolchain change, which D-1 forbids.

### D-15 — The open type question is resolved by evidence: `chartLabelStyle` stays local

`SPEC-insights-visual.md` D-19 found a second small-label idiom in the design — data labels tracked
0.5–0.6 px, which at 9 sp is **0.067 em**, against `labelSmall`'s eyebrow tracking of **0.244 em**
— carried it in a private `chartLabelStyle()` in `InsightsScreen.kt`, and left the decision about a
sixteenth shared style to this phase's audit.

The evidence from the five screens this phase restyles:

| Screen | Small labels needing 0.067 em data tracking |
|---|---|
| Settings | 0 — every small label here is a section eyebrow or a switch description |
| Profile | 0 — the score rows' date and total are `titleSmall` and `bodyMedium` |
| Report | 0 — the range labels are `labelLarge`; the summary itself is monospace `bodyMedium` |
| Safety | 0 — headings, hints, and detail are title and body styles |
| Breathing | 0 — the running-length line is the design's *eyebrow*, tracked wide, not a data label |

**Zero of five.** Insights remains the only screen in the app with enough small data labels to need
the idiom, which is exactly the argument D-19 made for keeping it local: widening a shared type
scale on one screen's evidence is how a scale acquires values nobody can justify later. It stays a
private style in `InsightsScreen.kt`, `Type.kt` keeps fifteen Material slots plus the three
`MindScaleTextStyles`, and this decision closes the question rather than deferring it again.

Recorded so a later reader does not reopen it on taste: if a second screen ever needs it, promoting
`chartLabelStyle` to `MindScaleTextStyles.dataLabel` is a two-line change and the evidence for it
will exist then.

### D-16 — What is deliberately not copied

| Not copied | Why |
|---|---|
| `CHANGE IS DRAWN OVER` and its four segments | A setting MindScale does not have; adding it is a feature |
| `Require Face ID` | No biometric dependency, no permission, never specified; D-25 of the foundation |
| `Replace with sample data` | Would write fabricated user data; D-25 of the foundation |
| `End entries on their own` as a switch | MindScale expresses the same thing as `An entry ends after`, which is a choice row, not a toggle; converting it is a control change |
| `MINDSCALE · VERSION 2.0` and the tagline footer | Two visible strings MindScale does not have |
| `JSON`, `CSV`, `Keeps your data` as trailing row labels | Visible strings MindScale does not have; a copy addition |
| The design's `SCORES FROM ELSEWHERE` placement on Settings | MindScale puts external totals on Profile; moving them is a navigation change; D-3 |
| Bare text actions for the crisis resources | Prominence is the affordance on this screen; D-11 |
| Instrument Sans for `report_text` | Fixed-width alignment carries meaning in the exported document; D-10 |
| `rgba(ink,.14–.16)` as a control boundary | Fails the 3:1 non-text floor; D-7 |
| `rgba(gold,.45)` as a control boundary | Fails the 3:1 non-text floor; D-7 |
| `rgba(ink,.35–.45)` as text | Fails AA; D-6 of the foundation |
| Material's tonal elevation on cards | Near-flat is the identity; D-13 of the foundation |

### D-17 — Nothing on these screens becomes conditional on recorded data

Restated because two of the five screens are the ones where it matters most. Safety is never
triggered, ordered, filtered, or surfaced by any rating, episode, count, streak, or inference
(`SPEC-safety-card.md` D-1). Breathing is never auto-offered and records nothing it can read back
(`SPEC-paced-breathing.md`). A visual phase cannot introduce a condition, and this phase introduces
none: every `if` this phase writes tests a UI state the screen already had.

## User experience

Nothing about the flow changes. Every screen is reached the same way, every control does the same
thing, and every string is the same string. What changes is that Settings becomes the design's
screen — eyebrow sections over equal-width segmented controls, one card of hairline-separated
anchor rows, one card of switch rows, and data actions as label-plus-action rows with the erase
path in danger — and that Profile, Report, Safety, and Breathing adopt the same card, hairline,
eyebrow, pill, and underlined-field idioms the other four screens already carry.

Configuration changes, process death, back navigation, and state restoration are untouched: this
phase adds no state and removes none.

## Frozen interfaces and data contracts

Changed:

| File | Change |
|---|---|
| `settings/SettingsScreen.kt` | Body restyle, D-8, L-6, `MsSpacing`, dialog labels |
| `report/ProfileScreen.kt` | Body restyle, D-9, L-5, `MsSpacing`, dialog labels |
| `report/ReportScreen.kt` | Body restyle, D-10, `MsSpacing` |
| `safety/SafetyScreen.kt` | Body restyle, D-11, `MsSpacing`, dialog labels |
| `breathing/BreathingScreen.kt` | Body restyle, D-12, `MsSpacing` |
| `ui/components/MsControls.kt` | The D-5 carry-over correction |

Added:

| File | Contents |
|---|---|
| `app/src/test/…/ui/theme/MsRemainingScreensContrastTest.kt` | D-7's tables, computed |
| `app/src/test/…/ui/theme/MsDimensionAuditTest.kt` | D-14's source-tree assertion |
| `app/src/androidTest/…/settings/SettingsVisualTest.kt` | L-6, focus behaviour, targets, 200% |
| `app/src/androidTest/…/report/ProfileReportVisualTest.kt` | L-5, the one-viewport constraint, targets, 200% |
| `app/src/androidTest/…/safety/SafetyVisualTest.kt` | Targets, order, 200% |
| `app/src/androidTest/…/breathing/BreathingVisualTest.kt` | Circle geometry, targets, 200% |
| `app/src/debug/…/designgallery/RemainingScreenPreviews.kt` | `@Preview`, light/dark at 100%/200% |

Unchanged, and any diff to them is a defect in this phase: every `ViewModel`, every DAO, every
entity, `SafetyIntents.kt`, `SafetyActions.kt`, `SafetyContent.kt`, `SafetyValidation.kt`,
`BreathingContent.kt`, `BreathingPacer.kt`, `BreathingClock.kt`, `ClinicianReport.kt`,
`SettingsLogic.kt`, `DataExport.kt`, `BackupImport.kt`, `ImportPreflight.kt`,
`RecordsCsvImport.kt`, `Migrations.kt`, every exported schema JSON, `IntensityRamp.kt`,
`TrackScreen.kt`, `LogScreen.kt`, `InsightsScreen.kt`, `MindScaleApp.kt`, every other file under
`ui/components/`, every file under `ui/theme/`, the main `AndroidManifest.xml`,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, and every file under `src/test/` and
`src/androidTest/` that existed before this phase.

## Invariants

1. Every one of the 79 pre-existing `testTag` values across the five screens still resolves to a
   node with the same role and the same callback.
2. Every content description asserted by the connected suite is unchanged, and each still sits on
   the node that carries the matching click action, live region, or heading.
3. Every visible string is byte-identical. Case and size differences on screen are presentation
   only and are absent from semantics.
4. No text is painted below 4.5:1 against the surface behind it.
5. No border, ring, or mark that is the sole boundary of an interactive control is below 3:1
   against either adjacent surface. Deliberate exemptions are asserted as *below* it.
6. No interactive element has a touch target below 48 dp on either axis.
7. All five screens reflow at 200% font with nothing clipped, and every segmented control's
   segments stay equal-width at both scales.
8. `SafetyIntents.kt` is byte-identical; no `ACTION_CALL` and no new permission.
9. `BreathingViewModel`'s constructor is byte-identical and no recorded value reaches the screen.
10. `SettingsFocus.ANCHORS` still brings the anchors section into view and `SettingsFocus.DATA`
    still brings the data section into view.
11. `profile_open_report` and `profile_open_settings` are still reachable without scrolling at
    100% font.
12. No `.dp` or `.sp` literal outside `ui/theme` is left undocumented.
13. No composable in `ui/components/` owns state, performs a side effect, navigates, or sets a
    `testTag`.
14. No gesture modifier, `rememberSaveable`, or `SavedStateHandle` key is added, removed, or moved.

## Android compatibility

`minSdk` 26, `targetSdk` 36, `compileSdk` 36.1 — unchanged. No new dependency, permission, or
resource. `MsCard` replaces `Surface` at the same nesting depth and adds no recomposition scope;
`MsSegmentedControl` replaces a `Row` of `FilterChip`s with a `Row` of `Box`es and removes one
`horizontalScroll` state per row. Process death, rotation, back navigation, and offline behavior
are unaffected because no state is added or removed.

## Acceptance criteria

- [ ] **REGRESSION**: `connectedDebugAndroidTest` passes **239 + new**, with no pre-existing test
      file modified.
- [ ] **REGRESSION**: `test` passes **428 + new**, with no pre-existing test file modified.
- [ ] **DIFF**: `git diff --name-status af273f9 HEAD -- app/src/test app/src/androidTest` shows
      only `A` lines and zero `M` lines. `git diff --check` passes.
- [ ] **DIFF**: a sorted diff of every `testTag` call site across the five screens is empty at 79
      tags, and a sorted diff of every double-quoted literal shows no change outside KDoc and
      `LazyColumn` item keys.
- [ ] **LINT/BUILD**: `lint` reports 0 errors and the unchanged 22-warning baseline;
      `assembleDebug` passes.
- [ ] **UNIT**: `MsRemainingScreensContrastTest` reproduces every figure in D-7's table, asserts
      each rejected design value fails 3:1, asserts each adopted replacement clears it on both `bg`
      and `card` in both themes, and asserts the segmented-control container and the row separators
      stay deliberately below it.
- [ ] **UNIT**: `MsDimensionAuditTest` finds zero undocumented `.dp`/`.sp` literals outside
      `ui/theme`, and reports the 39 documented ones by file (D-14).
- [ ] **INSTRUMENTED**: `SettingsVisualTest` asserts every segment of all three choice rows shares
      one width to within 1 px at 100% **and** 200% font (L-6), that each reaches 48 dp, and that
      no segment label is clipped at 200% (D-5).
- [ ] **INSTRUMENTED**: `SettingsVisualTest` asserts `SettingsFocus.DATA` brings `export_backup`
      into view and `SettingsFocus.ANCHORS` brings `anchor_2` into view (Invariant 10).
- [ ] **INSTRUMENTED**: `ProfileReportVisualTest` asserts the `score_form` row's gaps are even and
      that the trailing action has a gutter equal to the inter-element gap (L-5), and that
      `profile_open_report` and `profile_open_settings` are displayed without a scroll at 100% font
      (Invariant 11).
- [ ] **INSTRUMENTED**: `SafetyVisualTest` asserts the resource card's order is name → actions →
      detail, that every crisis action and every narrow plan control reaches 48 dp on both axes at
      100% and 200% font, and that the plan rows keep canonical step order.
- [ ] **INSTRUMENTED**: `BreathingVisualTest` asserts the cue sits within the circle's bounds, that
      the circle keeps no semantics, and that every control reaches 48 dp at both scales.
- [ ] **UI/ACCESSIBILITY**: `@Preview` composables over all five screens in light and dark at 100%
      and 200% font, debug-only, touching no `ViewModel` and no DAO.
- [ ] **MANUAL**: installed-app capture on the API 36 emulator, compared screen by screen against
      `docs/design/reference/`: Settings top, middle, and bottom in dark against the three dark
      captures; Settings top and bottom in light against the two light captures; Breathing in light
      against `light-breathing.png`.
- [ ] **MANUAL**: installed-app capture of Profile, Report, Safety, and dark Breathing, compared
      against the **HTML only**, with the absence of any screenshot stated in the report rather
      than implied away.
- [ ] **MANUAL**: all five screens at 200% font with nothing clipped. Emulator font scale, night
      mode, and rotation restored to `1.0`, `no`, and enabled, and the app's data cleared.

## Rollout, migration, and rollback

No migration. Room stays at schema 7; the JSON backup stays at version 7; the records CSV header
stays byte-identical. Nothing this phase writes is persisted, so rollback is `git revert` of the
phase's commits with no data consequence and no user-visible state to reconcile.

## Task decomposition

1. Freeze this spec — oracle: a documentation commit before any application-code edit.
2. `MsRemainingScreensContrastTest`, `MsDimensionAuditTest` (expected red), and the D-5 component
   correction — oracle: `.\gradlew.bat test`.
3. Settings: eyebrows, segmented controls, anchor card, switch card, data and import rows, dialog
   labels, `MsSpacing` — oracle: the full `SettingsImportScreenTest` and
   `NavigationTest.holdSettingIsReachableAndPersistsAcrossRecreation`.
4. Profile and Report: cards, underlined fields, chips, actions, L-5, dialog labels, `MsSpacing` —
   oracle: the full `ProfileReportScreenTest` and `NavigationTest`'s four Profile/Report tests.
5. Safety: resource cards, headings, plan rows, editor, dialog labels, `MsSpacing` — oracle: the
   full `SafetyScreenTest` and `SafetyIntentTest`.
6. Breathing: circle, cue, lengths, close pill, paragraphs, `MsSpacing` — oracle: the full
   `BreathingScreenTest`.
7. The four visual tests and the previews — oracle: `connectedDebugAndroidTest`.
8. `MsDimensionAuditTest` green, then full verification and installed-app capture — oracle: all
   four Gradle oracles plus the manual matrix.

## Open questions and approval gates

None blocking. Three points are **flagged rather than left silent**:

1. **`SPEC-visual-foundation.md` D-22's L-5 row names the wrong screen.** The row it describes
   lives on Profile in MindScale, not on Settings. D-3 corrects the record and implements the
   correction where the row actually is. Moving the row to match the screenshot would be a
   navigation change.
2. **`SafetyCopy`'s crisis numbers and `VERIFIED_ON` are a point-in-time fact last checked
   2026-08-05 and are overdue for re-verification.** A stale number in a safety feature is a real
   harm, not a cosmetic bug. It is deliberately not done in a visual phase, and it remains
   `PROJECT_STATE.md` "Next tasks" item 2.
3. **Three of the five screens this phase owns have no reference screenshot in either theme.** Any
   statement that Profile, Report, or Safety "matches the design" would be unfalsifiable. The
   completion report says what was compared against a picture and what was compared against the
   HTML.
