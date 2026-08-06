# SPEC-track-and-log-visual: applying the brand foundation to Track and Full Log

Status: IMPLEMENTED — VERIFIED LOCALLY

Owner: Claude Code (Phase 16 of 18), on the user's instruction of 2026-08-06

Date: 2026-08-06

Frozen documentation commit: `ecd6247`

Verified implementation commits: `23fb1fe` (contrast test, component corrections), `2a6e8c8` (Track and Log restyle), `45b2810` (visual tests and the two defects capture found)

## Purpose

Phase 15 installed MindScale's visual identity as a shared foundation — palette, type, shape,
emphasis, spacing, elevation, a component layer — and applied it to the app chrome. It
deliberately left every screen body carrying its pre-Phase-15 layout, so the app is currently a
correct foundation under unfinished rooms.

This phase dresses the first two rooms: **Track and Full Log**. It changes how they look. It
changes nothing about how they work.

## Sources reconciled

Authority order for every hex, size, tracking, radius, and shadow, highest first:

1. The current user instruction and its explicit boundaries.
2. `docs/specs/SPEC-visual-foundation.md`, D-1 through D-25 — frozen, and binding on this phase.
   D-1 (visual-only), D-11 (uppercase as presentation), D-15 (the component contract), D-22 (the
   recorded layout fixes) and D-23 (the accessibility floors) bind it directly.
3. `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html` — the
   literal inline `style=""` strings. Line anchors used: 60-69 the paused card, 71-86 the
   prompt/readout/help row, 88-95 the help card, 97 the divider idiom, 99-105 the pad wrapper and
   grid, 107-116 the backdate card, 118-130 the onset-chip card, 132-141 Sleep/Wake, 143-155 the
   marker block, 157-166 the check-in block, 168-172 the breathing pill, 174-182 the Track empty
   state, 184-246 the Recent list and its rows, 248-250 the Safety link, 255-261 the Log filter
   row and count line, 262-267 the Log day header, 268-292 the Log row and its actions, 1204
   `keyBase`, 1205 `order`, 1218 `padWrapStyle`, 1222 `toggleBase`, 1223 `chipBtn`, 1240
   `dotStyle`, 1276 `numS`, 1719-1720 `sleepStyle`/`wakeStyle`.
4. `docs/design/reference/` — the rendered screenshots.
5. The prototype's own layout, which is advisory only (D-22).

### Screenshot coverage, stated honestly

| Surface | Reference |
|---|---|
| Track top, light | `light-track-top.png` |
| Track scrolled, light | `light-track-scrolled.png` |
| Track top, dark | `dark-track-top.png` |
| Track scrolled, dark | `dark-track-scrolled.png` |
| Full Log, dark | `dark-full-log.png` |

**There is no light Full Log screenshot.** Light Full Log is derived from the HTML and from the
dark capture, and no claim is made that it was matched against a reference that does not exist.

Covered by no screenshot at all, and derived from the HTML alone: the armed numpad, the backdate
dialog, the onset-chip prompt, the help card, the check-in card, the anchor card, the paused card,
the Track empty state, the Log empty states, the Log inline edit and note panels, the Log message
and read-error banners, and the toast. The anchor prompt, the Log message banner, the Log
read-error banner, and the Track/Log inline retry affordances appear nowhere in the prototype at
all — they are MindScale's own, and their treatment is derived from the design's card and action
idioms rather than copied.

### Measured starting state

Taken from the tree at `238b0ba25a9e5f040e3693ca70d4d91ef8b168e0`, so the acceptance criteria are
checkable rather than impressionistic:

- `TrackScreen.kt` is 1089 lines; `LogScreen.kt` is 453 lines.
- 52 `testTag` call sites across the two files.
- 60 hardcoded `.dp` literals in `TrackScreen.kt` and 26 in `LogScreen.kt`.
- Track renders `ToastBanner` as a full-width `Surface` on `inverseSurface`, not the D-20 pill.
- The numpad renders 1-9 as 12 dp rounded squares on `primaryContainer` and 0/10 as 56 dp-tall
  pills on `secondaryContainer` — neither circular, and the armed state is not rendered at all.
- The entry-row dot is a 40 dp circle filled with `intensityColor`.
- Track's and Log's dialog action labels are sentence case.
- Baselines to hold: 399/399 JVM tests, 206/206 connected tests, lint 0 errors at the unchanged
  22-warning baseline, `assembleDebug`.

## Scope

This phase delivers, and nothing else:

- Track's body: the readout and help row, the help card, the numpad and its armed state, the
  onset-chip card, the Sleep/Wake toggles, the marker block, the check-in card, the anchor card,
  the paused card, the empty state, the recent-entry rows, the breathing and safety links, and
  the toast.
- Full Log's body: the filter row and count line, the message and read-error banners, the day
  headers, the record rows, the inline edit and note panels, and both empty states.
- Uppercasing the dialog action labels on Track's three dialogs, Log's delete dialog, and Log's
  date picker (D-19 deferred this to the phase that owns each screen).
- Converting Track's and Log's dimension literals to `MsSpacing`, per D-14.
- The three D-22 layout corrections this phase owns: L-1, L-2, L-4.
- Four carry-over corrections to the shared component layer that this phase is the first to
  need (D-15 below).

## Non-goals

- Any behavior change whatsoever. See D-1.
- Insights (Phase 17). Settings, Profile, Report, Safety, Breathing, and the closing audit
  (Phase 18).
- `IntensityRamp.kt`, which is untouched. Its colour mapping stays a Phase 17 decision (D-8
  below records that Track stops calling it, and why that does not pre-empt Phase 17).
- Material's `DatePicker` internals. Only its two action labels are restyled.
- Any dependency, permission, toolchain, Gradle plugin, manifest, Room schema, backup, CSV,
  export, import, or migration change.
- Any new string, control, entry point, or navigation path.

## Decisions

### D-1 — The visual-only rule, as this phase's testable contract

Only how pixels are painted may change. No feature is added, removed, renamed, reordered, or
rewired. No navigation change, no new entry point, no control that was not there, no control taken
away. No copy edits. No data, storage, export, or migration change. No dependency, permission, or
toolchain change.

The proof is objective and the baselines have moved since Phase 15:
**206/206 connected tests and 399/399 JVM tests must pass unchanged.** A test that breaks is
evidence that behavior changed, and that is a defect in this phase, not a test to update. No
pre-existing test file may be edited. New test files may be added; existing assertions may not be
relaxed, retargeted, or deleted.

Checked mechanically rather than by inspection:

```
git diff --name-status 238b0ba HEAD -- app/src/test app/src/androidTest
```

Every line must be an `A`. A single `M` is a defect in this phase.

`SPEC-visual-foundation.md` Invariant 3 also binds: **every visible string stays byte-identical.**
Case differences on screen are presentation only and are absent from semantics (D-11). One
consequence is recorded rather than discovered later — see D-9 on the transient readout, where the
design splits a value from its band and MindScale's copy composes them into one string.

### D-2 — Fidelity: careful, not pixel-perfect

Copy the elegance, colour, and theme with care. Do not copy the prototype's layout flaws, and do
not copy a value that fails an accessibility floor. Every such divergence in this phase is D-4,
D-5, D-6, D-7, D-8, D-9, D-11, and D-12. There are no unrecorded ones.

Preserved exactly, with no divergence: circular numpad keys, the 42 dp circular entry-row dot, the
gold armed-pad ring and its 4 dp spread, 14 dp cards, 999 dp pills, the ink toast pill, near-flat
elevation, the design's centred narrow column widths, and the uppercase letter-spaced label idiom
through `MsUppercaseText`.

### D-3 — Uppercase remains presentation, and `MsUppercaseText` is the only means

Every label this phase uppercases goes through `MsUppercaseText` (D-11). Nothing is uppercased at
a call site, in a string constant, or with `text-transform`-style styling that would reach
semantics.

Two consequences that decide implementation shape:

- **Onset chips and the user's own words are never uppercased.** `chipBtn` at line 1223 sets no
  `text-transform`, and `MsChip` already renders plain `Text` for that reason. Marker text, note
  previews, and Log meta strings are likewise left in their own case.
- **Dialog action labels are uppercased by wrapping the label, not by replacing the button.**
  Each dialog action stays a Material `TextButton` and only its `Text` becomes
  `MsUppercaseText(label, style = labelMedium)` with `Color.Unspecified`, so it inherits
  `TextButton`'s content colour including its disabled fade. Replacing the button with
  `MsTextAction` would move the `Disabled` semantics property onto a different node, and
  `TrackScreenTest` asserts `onNodeWithText("Save").assertIsNotEnabled()` three times. The label
  changes; the control does not.

### D-4 — Control-boundary contrast: the design's own borders, measured

This is the largest divergence in the phase and it is not a judgment call. D-23 requires **at least
3:1 for any border, ring, or mark that is the sole boundary or the sole state indicator of an
interactive control**, and exempts decorative separators. Almost every control in Track and Log is
drawn as transparent-with-a-hairline, so in almost every case the border **is** the only boundary
— the fills either side of it are `card` on `bg`, which are 1.03:1 apart in light and 1.30:1 in
dark and therefore carry no boundary at all.

Ratios are sRGB WCAG 2.x relative-luminance ratios of the alpha-composited border against the
stated backdrop, composited in floating point as the compositor does, rounded to two decimals.
`bg` and `card` are both tabulated because a ring sits on the boundary between two surfaces and
must clear the floor against both. **Every figure below was produced by
`MsControlBoundaryContrastTest` rather than by hand**, so the tables and the assertions agree to
the decimal and cannot drift apart.

| Element | Design value | Light bg / card | Dark bg / card | Verdict |
|---|---|---|---|---|
| Numpad key border, at rest | `rgba(ink,.10)` | 1.24 / 1.24 | 1.26 / 1.30 | **fails** |
| Numpad key border, armed | `rgba(174,140,79,.55)` | 1.76 / 1.78 | 2.62 / 2.61 | **fails** |
| Pad wrapper border, armed | `var(--gold)` | 3.05 / 3.15 | 8.60 / 8.04 | passes |
| Sleep/Wake border, at rest | `rgba(ink,.13)` | 1.31 / 1.32 | 1.37 / 1.41 | **fails** |
| Onset chip border, unselected | `rgba(ink,.14)` | 1.35 / 1.35 | 1.42 / 1.48 | **fails** |
| Breathing pill border | `rgba(174,140,79,.45)` | 1.57 / 1.60 | 2.62 / 2.61 | **fails** |
| Log From/To underline | `rgba(ink,.16)` | 1.41 / 1.41 | 1.51 / 1.57 | **fails** |
| Marker input border | `rgba(ink,.14)` | 1.35 / 1.35 | 1.42 / 1.48 | **fails** |
| Help button ring | `rgba(ink,.16)` | 1.41 / 1.41 | 1.51 / 1.57 | **fails** |

The compliant replacements, and nothing else changes about these controls:

| Element | Adopted | Light bg / card | Dark bg / card |
|---|---|---|---|
| Every ink control boundary above | `MaterialTheme.ms.outline` — ink `.50` light, `.40` dark | 3.47 / 3.49 | 3.47 / 3.51 |
| Numpad key border, armed | `MaterialTheme.ms.gold` at full opacity | 3.05 / 3.15 | 8.60 / 8.04 |
| Breathing pill border | `MaterialTheme.ms.gold` at full opacity | 3.05 / 3.15 | 8.60 / 8.04 |
| Marker input border | Material's `outline` role, which D-9 of the foundation already sets to the same ink alpha | 3.47 / 3.49 | 3.47 / 3.51 |

Two constraints come out of the arithmetic and are recorded so a later phase does not trip on
them:

1. **Full gold is a compliant control boundary on `bg` and `card` only.** Light gold `#AE8C4F`
   measures 2.67:1 on `surfaceContainerHighest`. Nothing in Track or Log paints a gold control
   boundary on a container step, and nothing later should without re-measuring.
2. **`outline` is the ink control-boundary token and already has a pinned test.** This phase reuses
   it rather than inventing a second ink alpha, so `MindScaleContrastTest`'s existing
   `theControlBoundaryOutlineClearsThreeToOne` keeps covering it.

One row in the failing table deserves a correction to the framing rather than to the value, because
measurement changed the diagnosis. **The armed ring's 55% alpha is unsalvageable in light and
salvageable in dark.** At 55% of the *theme's* gold the dark ring measures 3.30:1 and passes; the
prototype nonetheless fails in dark at 2.61:1, because `keyBase` hardcodes the light triple
`rgba(174,140,79,.55)` on an element whose sibling `padWrapStyle` uses the theme-aware
`var(--gold)`. A per-theme alpha would therefore have been legal in dark and illegal in light. Full
theme gold is chosen instead because it is one value for both themes rather than two, and because
the pad wrapper's armed border — the mark a user actually reads as "armed" — is already full gold
in the design and already passes.

The cost is recorded honestly: at ink `.50` the numpad keys' rings are visibly firmer than the
design's near-invisible `.10`, and the pad reads as twelve outlined circles rather than as twelve
suggestions of circles. The hue, the radius, the size, the spacing, and the near-flat surface are
all the design's. Only the alpha moves, and it moves because 1.24:1 is not a boundary a sighted
user with low contrast sensitivity can find.

Deliberately **not** raised, because they are decorative and WCAG imposes no minimum on them
(D-6 of the foundation):

| Element | Design value | Light bg / card | Dark bg / card | Why exempt |
|---|---|---|---|---|
| Entry-row dot ring | `rgba(174,140,79,.5)` | 1.67 / 1.69 | 2.98 / 2.96 | The row is not clickable; the dot is a mark, not a control |
| Kind-badge pill border | `rgba(174,140,79,.4)` | 1.49 / 1.50 | 2.31 / 2.33 | A non-interactive badge around text that is itself ≥4.5:1 |
| Row and day separators | `rgba(ink,.08–.09)` | 1.20 | 1.26 | Separators; `MsHairline` and `ms.hairline` |

The dark dot ring at 2.96:1 comes within four hundredths of the control floor, and it is exempt
because it is decorative rather than because it is faint. That distinction is asserted rather than
assumed: the exemption tests check these values stay *below* 3:1, so a later phase that "fixes"
them into heavy borders loses the design's most characteristic mark and breaks a test on the way.

### D-5 — The armed pad, and why its state is not carried by colour alone

The app currently renders no armed state on the numpad at all, although `TrackUiState.armedCapture`
has carried it since Phase 2. The design does, in three simultaneous marks (lines 1218 and 1204),
and this phase adopts all three:

1. The pad wrapper's border goes from `transparent` to gold. **A border appears where there was
   none.**
2. A 4 dp gold spread appears outside that border — `box-shadow: 0 0 0 4px rgba(174,140,79,.09)`,
   rendered as a concentric ring rather than a shadow, because D-13 keeps elevation near-flat.
   **The pad's outer geometry changes.**
3. Every key's ring changes hue from ink to gold.

D-23's "never colour alone" is satisfied by (1) and (2), which are presence-versus-absence
changes rather than colour changes, and independently by two signals that already exist and are
untouched: the armed Sleep or Wake toggle changes from an ink outline to a gold outline **and** its
content description already reads "Sleep armed. Tap a number to log falling asleep."

Only mark (3) needed a value change, and it is in D-4: the key's armed ring is full `gold`
(3.15:1 light, 8.04:1 dark) rather than the design's 55% gold (1.78:1 light, 2.61:1 dark as the
prototype paints it). The pad wrapper's armed border is the design's `var(--gold)` unchanged,
because it already passes.

A recorded prototype omission, adopted as a correction rather than copied: `keyBase`'s armed border
is the hardcoded literal `rgba(174,140,79,.55)`, which is the **light** gold, while
`padWrapStyle` on the same element uses the theme-aware `var(--gold)`. So the prototype paints a
light-theme ring in dark mode, and it costs it the floor: the theme's own dark gold at the same
alpha measures 3.30:1 and would have passed. MindScale uses the theme's gold in both places, at
full opacity, which is legal in both themes.

### D-6 — L-1: the numpad's last row is centred on the pad's axis

`order` at line 1205 is `[1,2,3,4,5,6,7,8,9,0,null,10]` — a 3-column grid whose final row places
`0` in column one, an empty cell in column two, and `10` in column three. D-22 records this as
flaw L-1 and its correction as: centre the final row's two keys on the pad's axis, keeping the
3-column rhythm and both keys' size, tags, and long-press behavior.

Stated precisely, because the recorded wording deserves the correction it is owed: the prototype's
hole is *symmetric* about the pad's vertical axis. What is wrong with it is density, not left-right
balance — a two-thirds-empty final row under three full rows leaves the pad bottom-heavy in a way
the reference capture makes plain. The correction D-22 specifies fixes exactly that, and this spec
implements it as written.

Implementation, which is what makes "keeping both keys' size" true rather than approximate: the pad
measures its own content width once with `BoxWithConstraints` and derives the column width as
`(maxWidth - 2 × gap) / 3`. All twelve keys are laid out at that exact width, and the final row is
a `Row` with `Arrangement.spacedBy(gap, Alignment.CenterHorizontally)`. Weighted spacers were
rejected: four children at weights `0.5, 1, 1, 0.5` consume three gaps instead of two and would
render `0` and `10` narrower than `1` through `9`.

`TrackScreenTest.numpad_renders12KeysInFrozenOrderAndGrouping` is the net and it still holds: all
of `1..9` sit above both edge keys, and `0`'s left edge stays left of `10`'s.

### D-7 — The numpad keys are circles, and how that reconciles with Invariant 12

`keyBase` at line 1204 is `border-radius:50%`, `aspect-ratio:1`, `background:var(--card)`,
`font-size:21px`, `font-weight:400`, `font-variant-numeric:tabular-nums`, and every one of the
twelve keys — including `0` and `10` — is drawn from it. The reference captures show twelve
identical circles. The user's instruction for this phase names "circular numpad keys" among the
things to preserve exactly, and so does D-2 of the foundation.

The app currently renders `1..9` as 12 dp rounded squares on `primaryContainer` and `0`/`10` as
56 dp-tall pills on `secondaryContainer`, with a source comment citing
`SPEC-track-numpad-logging.md` **Invariant 12**: "Numpad key order and grouping is frozen:
`[1,2,3,4,5,6,7,8,9]` as a 3×3 grid, then `0` and `10` in a separate, visually distinct group
below — this ordering is a product decision from the mockup, not incidental layout."

The apparent conflict is surfaced rather than silently resolved, and it resolves without bending
anything:

- What Invariant 12 freezes is **order and grouping**, and it justifies itself by appeal to the
  mockup. Both survive intact: `1..9` in a 3×3 grid, then `0` and `10` in their own group below,
  in that order.
- The mockup Invariant 12 appeals to does **not** distinguish that group by shape or tone. It
  distinguishes it by position — a separate final row, set off by the grid's own gap and by the
  empty cell. Shape and tone were the Phase 1 implementation's means, not the mockup's.
- After L-1 the group is still visually distinct, and by the mockup's own means: it is the only
  row that breaks the 3-column alignment, sitting centred on the axis while every row above is
  flush to it.

So: all twelve keys become `card`-filled circles at the grid's column width with a 1 dp `outline`
ring (D-4) and a `displaySmall` 21 sp weight-400 tabular glyph, and `0` and `10` remain a separate
group below, distinguished by position. The pad wrapper is 24 dp radius (`extraLarge`), 16 dp
padding, capped at 288 dp and centred, with a 14 dp grid gap.

This is flagged in the completion report as the one place this phase reinterprets an invariant
frozen by an earlier spec, on the authority of the current instruction and of D-2.

### D-8 — The entry-row dot, and why `intensityColor` is left alone

`dotStyle` at line 1240 is a 42 dp circle with **no background**: `border:1px solid
rgba(174,140,79,.5)`, `border-radius:50%`, a 16 px weight-500 tabular glyph, and a text colour of
`var(--ink)` — or `rgba(ink,.4)` when the value is `0`. The dark reference capture shows exactly
that: unfilled circles with a faint gold ring.

The app fills the dot with `intensityColor(entry.value, isDark)` and picks black or white text from
the fill's luminance. Adopting the design's dot therefore removes Track's only call to
`intensityColor`.

That does not pre-empt D-24 of the foundation, which defers the ramp's **colour mapping** to Phase
17, and the reasoning is worth recording because it looks like a collision and is not:

- `IntensityRamp.kt` is not modified. Not one line.
- `intensityColor` keeps three callers in `InsightsScreen.kt` — the raster and both legend swatches
  — so Phase 17 still owns exactly the decision D-24 describes.
- The prototype's `ramp()` is used for chart marks and never for a row dot. Track's row was the
  one place MindScale had wired the ramp somewhere the design does not use it.
- D-24's specific worry — that the warm low anchor `#F0E4CC` sits close to `card` and could make a
  low rating nearly invisible — is a worry about a *filled* swatch. An unfilled dot cannot have it.
- `SPEC-track-numpad-logging.md` Invariant 14, colour is never the sole carrier of value
  information, is strengthened rather than weakened: the value is still rendered as a numeral, and
  it is no longer *also* encoded as a fill.

Values `0` render their numeral at `inkQuaternary`, not the design's `rgba(ink,.4)`, which measures
2.43:1 and fails as text (D-6 of the foundation). `inkQuaternary` is the faintest compliant level
and preserves the design's intent of a receded zero.

### D-9 — L-2: the entry row, and the one visible string the design would have split

The row becomes the design's structure: a top `hairline`, then a 42 dp dot, a 14 dp gutter, and the
content column.

**L-2.** D-22 records that the prototype stacks EDIT / NOTE / DELETE vertically with a ragged edge,
cramped against the row content, and specifies: one baseline, even gaps, consistent gutter.

The three actions are therefore laid out horizontally on one baseline with even
`MsSpacing.lgPlus` gaps, right-aligned, on **their own line beneath the dot-and-content row**
rather than in the prototype's third grid column. The placement is derived rather than copied, and
the arithmetic is the reason:

Each action must reach a 48 dp touch target (D-23), so three of them need 144 dp before gaps —
172 dp with two 14 dp gaps, 186 dp with a gutter. On the API 36 emulator's 411 dp width the row is
379 dp, leaving 137 dp for the content column, which wraps a 23-character timestamp onto two or
three lines at 13 sp. At 200% font the actions need roughly 214 dp and the content column falls to
95 dp at 26 sp, and on a 320 dp device it falls to single digits. Putting the actions on their own
full-width line gives the content the whole width, so it stops wrapping — the row ends up **no
taller** than the third-column layout at 100% font and strictly shorter at 200%.

This changes no control, no tag, no semantics, and no reachability. `NavigationTest` finds these
actions by content description in the **unmerged** tree and reaches them with `performScrollTo()`
before `performClick()`, so each action keeps its content description on the same node that carries
its click action: `MsTextAction` applies the caller's `modifier` and its own `clickable` to one
layout node, exactly as `TextButton` did.

All three actions use `MsActionTone.Muted`. The design paints all three at `rgba(ink,.4)` and turns
Delete red only on `:hover`, which Android has no analogue for; a permanently red Delete would
emphasize destruction more than the design does.

**The content column.** Line one is the formatted timestamp at `titleSmall` `inkPrimary`. Line two
carries the band label, the kind badge, and the chips. Line three is the note preview at
`bodySmall` `inkTertiary`, indented to clear the dot as the design does.

- The band label becomes `MsUppercaseText` at `labelSmall` `inkQuaternary`.
- The kind badge becomes the design's pill: 999 dp radius, `rgba(gold,.4)` border (decorative,
  D-4), `goldText` label, uppercase and tracked. `entry_badge_$id` stays on it.
- The chips stay plain `Text` in the user's own case at `bodySmall` `inkTertiary`.
  `entry_chips_$id` stays on them.

**The one string the design would have split, and is not.** The design's transient readout is a
26 px number beside an 11 px tracked gold band label — two elements. MindScale's readout is one
string, `"${value} · ${band}"`, and Invariant 3 requires it to stay byte-identical. It is therefore
rendered as a **single `Text` holding one `AnnotatedString`** with two spans — the value at
`displayLarge` and `" · " + band` uppercased at `labelLarge` in `goldText` — and its semantics are
set with `clearAndSetSemantics` to the original mixed-case string plus the existing content
description. The characters on screen are the characters that were always there; only their size
and case differ, which is precisely what D-11 already licenses. Nothing is split, added, or
removed.

### D-10 — The toast becomes the ink pill

Track's `ToastBanner` becomes `MsToastPill` (D-20), centred rather than full-bleed so the pill
hugs its text as the design's does. `toast_banner` moves to the pill itself.
`TrackEvent.ToastDismissed`, the timing, and every toast string are untouched — this phase changes
one `Surface` into one `Box`.

### D-11 — Log's filter row, and L-4

The design's From and To are `<label>` elements pairing an eyebrow with a bottom-bordered date
input. MindScale's are `OutlinedButton`s that open a `DatePicker`, whose single label is either the
formatted date or the word `From`/`To`. **No eyebrow is added**, because the design's eyebrow is a
second visible string per field and adding it would be a copy addition. Each field renders exactly
the one string it renders today.

Each becomes the design's underlined field: no fill, no ring, a 1 dp bottom rule in
`ms.outline` (D-4 — the rule is the control's only boundary), and its label at `bodySmall` in
`inkSecondary` when a date is set and `inkQuaternary` when it is not. Both keep their tags,
content descriptions, callbacks, `weight(1f)`, and a 48 dp target.

The field label sets **no** `maxLines` and no ellipsis. `MMM d, yyyy` fits one line at 200% font on
the API 36 emulator — checked by capture, not assumed — but a single ellipsized line on a narrower
device would render a date as `Aug 6, 2…`, and losing characters is worse than taking a second
line. This is Phase 15's clipped-tab defect applied in advance rather than repeated.

**L-4.** D-22 records that the prototype jams `ALL` against the right edge and specifies: give
`ALL` the same trailing gutter as the fields. `ALL` becomes `MsTextAction` with
`MsActionTone.Muted` and a trailing gutter of `MsSpacing.lgPlus`, equal to the gap between the
three elements, so it is inset from the row's right edge by the same amount that separates it from
the To field. `log_all_button` is unchanged.

The count line below stays one string at `bodySmall` `inkQuaternary`, and `log_record_count`
stays on it. The filter error uses `ms.danger`.

### D-12 — Log's rows, banners, day headers, and panels

- **Day header**: `MsUppercaseText` at `labelLarge` in `goldText`, matching the design's 10 px
  tracked gold-deep label. `log_day_$date` unchanged. The design's right-hand `g.meta` summary is
  **not** added — MindScale has no such string.
- **Row**: a top `MsHairline`, then the design's `34 dp | 1fr | auto` proportions with the numeral
  at `titleLarge`. The numeral's colour follows the design's intent at compliant levels:
  `inkPrimary` for a rating, `inkQuaternary` for a `0` rating and for the sleep em-dash — the
  design's `rgba(ink,.28)` measures below 4.5:1 — and `goldText` for the event `×`. Log rows carry
  two or three actions depending on the record type, unchanged.

  **Log's row actions follow D-9 rather than the design, and that is a divergence worth naming.**
  Unlike Track's Recent row, the design's Log row at line 277 already lays its three actions out
  horizontally in the third grid column — `display:flex;gap:14px;justify-content:flex-end` — so Log
  has no L-2 flaw to fix. MindScale still moves them to their own line, for one reason: the design
  fits them inline only because its actions are 9 px labels with no touch target, roughly 12 dp
  tall and 30 dp wide. D-23's 48 dp floor is MindScale's addition and it outranks the prototype, and
  three 48 dp targets plus gaps need about 186 dp — which on a 320 dp device at 200% font leaves the
  row content roughly 16 dp. The cost is stated plainly: **Log's rows are about 1.6× taller than the
  design's**, so fewer records fit on a screen. The alternative was a row whose content collapses to
  nothing at large font on a small device, and keeping Track and Log incoherent with each other.
- **Time and meta**: time at `bodySmall` `inkSecondary`, meta at `bodySmall` `inkQuaternary`,
  still two lines with ellipsis. Note preview at `bodySmall` `inkTertiary`, indented to clear the
  numeral column.
- **Message banner**: the current `inverseSurface` surface with a `Dismiss` action becomes an
  `MsCard`. The reason is contrast, not taste: `MsTextAction`'s three tones are all designed for a
  `bg` or `card` backdrop, and `goldText` on `ink` is unreadable. A card keeps the banner coherent
  with every other Track and Log banner and lets `Dismiss` be a normal muted action. `log_message`
  unchanged.
- **Read-error banner**: error text in `ms.danger` with `Retry` as a gold `MsTextAction`.
  `log_read_error` unchanged.
- **Inline edit and note panels**: the eleven value chips and the onset chips become `MsChip`; the
  timestamp and note fields stay `OutlinedTextField`, which now inherits a compliant Material
  `outline`. `Save` and `Cancel` become `MsTextAction`. Every tag is unchanged, and both panels
  keep the design's indent from the numeral column.
- **Empty states**: `MsEyebrow` plus a `bodyMedium` paragraph, left-aligned as the design's empty
  state is, rather than centred. Both strings unchanged; `log_empty_state` unchanged.
- **Date picker**: `DatePickerDialog` and `DatePicker` are untouched apart from `OK` and `Cancel`,
  which are uppercased per D-3.

### D-13 — Track's cards, banners, and centred column widths

Each of these keeps every string, tag, content description, and callback, and changes only how it
is painted:

| Surface | Treatment |
|---|---|
| Paused card | `MsCard` + `MsEyebrow("Tracking paused")` + `bodyLarge` at `inkSecondary` + gold and muted `MsTextAction`s |
| Help card | `MsCard` + three `bodySmall` paragraphs, the first two at `inkSecondary` and the third at `inkTertiary`, as the design grades them |
| Onset-chip card | `MsCard(emphasized = true)` + `MsEyebrow` + a `FlowRow` of `MsChip` + centred gold and muted `MsTextAction`s |
| Anchor card | `MsCard(emphasized = true)` + `MsEyebrow` + `bodyMedium` + two `MsTextAction`s. Not in the prototype; derived from the card idiom above |
| Check-in block | The design's hairline-separated block rather than a card: `MsHairline` + `MsEyebrow` + `bodySmall` + gold and muted actions |
| Track empty state | `MsHairline` + `MsEyebrow("Nothing recorded yet")` + a left-aligned `bodyMedium` paragraph |
| Help toggle | `MsCircularHeaderButton` at 26 dp with an `outline` ring (D-4, D-15) |
| Breathing link | `MsPillButton` with a full-gold border (D-4) |
| Safety link | `MsTextAction` at `MsActionTone.Muted`, matching the design's faint 11.5 px link |

The design's centred narrow column widths are adopted with `widthIn(max = …)` so they shrink on a
narrow device rather than clipping: 288 dp for the pad, the onset card, and the backdate-prompt
width; 256 dp for the Sleep/Wake row and the marker block.

**The readout and the help toggle share one row**, as the design's lines 73-86 do: the readout
column takes the remaining width and the 26 dp help button sits at its trailing edge, top-aligned,
over the 1 dp hairline the design puts at line 97 between the prompt and the pad. Today they are two
separate list items, and combining them is layout only — both remain present exactly when they are
present today, and the help toggle still renders when there is no readout.

One ordering consequence, recorded rather than discovered later: the anchor prompt currently renders
*between* the readout and the help toggle, so after the merge it renders below both. Nothing changes
about what controls exist, what they do, or how they are reached; the help toggle simply moves ahead
of a card that appears once, and only in the session where the app offers to set anchors.

Because the readout is absent most of the time and MindScale has no `padPrompt` string (D-19), the
row is usually an empty column beside the help button. That leaves visible space above the hairline
that the design fills with its prompt. Adding a string there would be a copy addition, so the space
stays.

### D-14 — Sleep/Wake stays a screen-local composable

The Sleep and Wake toggles have three visual states — at rest, armed, and (for Sleep) an open
interval — which `sleepStyle` and `wakeStyle` at lines 1719-1720 paint as a muted ink outline, a
gold outline, and an ink fill with `onInk` lettering respectively. `MsPillButton` models two
states, and widening it to three would be bending a shared component to one screen's need.

So Sleep/Wake is a screen-local composable built **from** the token and component layer —
`MsShapes.pill`, `MsUppercaseText`, `MsSpacing`, `ms.outline`, `ms.gold`, `ms.ink`/`ms.onInk` — and
not a new shared primitive. D-15 of the foundation constrains what a shared component may do; it
does not require every control to be one.

Its at-rest border is `ms.outline` (D-4). The design dims an unavailable Wake to `rgba(ink,.28)`,
which fails as text, so Wake renders at `inkQuaternary` without an open interval and
`inkTertiary` with one — two compliant levels preserving the design's ordering. **Wake stays
enabled in both cases**, exactly as today; its content description already says
"Wake. Disabled: tap Sleep first, nothing is currently open." Changing that would be behavioral.

### D-15 — Four carry-over corrections to the shared component layer

Phase 16 is the first phase to put these Phase 15 components on a real screen, and doing so surfaced
that each carries a border that is a control's only boundary at an alpha below D-23's floor — plus,
in `MsChip`, a touch target applied to the wrong box. They are corrected here rather than left for
Phase 18, because by then Settings, Profile, Report, and Safety would depend on them too, and
because shipping them non-compliant while claiming D-23 holds would be false.

| Component | Was | Now | Reason |
|---|---|---|---|
| `MsPillButton` | unselected border `gold.copy(alpha = 0.45f)` — 1.60:1 | `gold` at full opacity — 3.15:1 | The pill's only boundary; the hue is unchanged |
| `MsChip` | unselected border `outlineDecorative`, ink `.16` — 1.41:1 | `outline`, ink `.50`/`.40` — 3.49:1/3.51:1 | An unselected chip has no fill, so the border is its only boundary |
| `MsChip` | `defaultMinSize(minHeight = 48.dp)` on the **painted pill**, leaving a chip labelled `0` 33 dp wide | the design's compact pill inside a transparent 48 dp **square** | D-23's own technique: the painted geometry is the design's, the target is MindScale's. Found by capture — see the record |
| `MsCircularHeaderButton` | ring `outlineDecorative` — 1.41:1; fixed 34 dp; fixed `titleLarge` glyph | ring `outline`; `size` and `textStyle` parameters, defaulting to today's values | The ring is the back control's only boundary; Track's 26 dp help button is the same control at a different size |

`MsCircularHeaderButton`'s two new parameters are defaulted, so the chrome's call site in
`MindScaleApp.kt` is unchanged and the header back button changes only in ring contrast. That
change is visible on overlay destinations and is an improvement, not a regression: a 1.41:1 ring is
a boundary a low-contrast-sensitivity user cannot find.

No component gains state, a side effect, navigation, or a `testTag`. D-15 and Invariant 7 of the
foundation still hold.

### D-16 — Every tag, description, and unmerged-tree shape survives

The 52 `testTag` call sites across `TrackScreen.kt` and `LogScreen.kt` keep their exact values, and
each still resolves to a node with the same role and the same callback. Read the tag list before
restructuring, not after.

Four shapes the connected suite depends on that a restyle could silently break, and how each is
preserved:

1. **`numpad_key_0` through `numpad_key_10`** keep tap and long-press on one `pointerInput` with
   `detectTapGestures`, unchanged. `numpad_key_7`'s long-press opens the backdate dialog and
   `TrackDialogSavedStateTest` asserts raw-draft restoration across recreation; `NavigationTest`
   long-presses key 7 and clicks keys 8, 9, and 10 on the live app.
2. **The entry-row actions** are found by content description with `useUnmergedTree = true` and
   reached with `performScrollTo()`. The content description and the click action stay on one
   layout node.
3. **`EntryRow`'s merging row** keeps `semantics(mergeDescendants = true)` with its composed
   content description, and the badge, chips, and band remain descendant leaves so
   `onNodeWithText("asleep", useUnmergedTree = true)` and the merged-tree
   `onNodeWithText("moderate")` both still resolve. Every uppercased leaf restores its original
   through `clearAndSetSemantics`, which D-11's connected test already proved survives a merging
   ancestor.
4. **`breathing_link`** keeps its content description and a 48 dp minimum height, which
   `MsPillButton` provides through `defaultMinSize`.

### D-17 — 200% font is an oracle, not an aspiration

Phase 15's one installed-app defect was a single-line label clipping at 200%, found by capture and
by no test. The three tightest elements in this phase are the numpad, the entry row's three
actions, and Log's From/To/All row.

- The **numpad** is geometric: key size derives from the pad width, not from text, so 200% font
  grows only the glyph inside a fixed circle. `10` at 42 sp is roughly 46 dp inside a 76 dp circle.
- The **entry-row and Log-row actions** sit on their own full-width line (D-9), which is what makes
  them fit at 200% on every device width rather than squeezing the content column to nothing.
- **Log's From/To/All row** keeps its two weighted fields, and `ALL` is measured before them, so
  the fields absorb the growth. `FlowRow` is used for the action groups so the pathological narrow
  case wraps rather than clips.

No text container in either screen takes a fixed height. Every size is `sp` and every tracking is
`em`, per D-10 of the foundation.

### D-18 — `MsSpacing` conversion, and the documented one-offs

D-14 of the foundation converts literals in the phase that already restyles the screen. All 86
`.dp` literals in `TrackScreen.kt` and `LogScreen.kt` are converted to `MsSpacing` references or to
named private constants carrying a comment, so Phase 18's closing audit finds no undocumented
literal.

The scale gains one value, alongside the existing `headerButton`:

- `helpButton = 26.dp` — the design's help-toggle diameter at line 84.

The remaining one-offs are geometry the scale should not absorb, and each is a named private
constant in the screen that owns it: the 42 dp entry-row dot, the 34 dp Log numeral column, the
288 dp pad and onset-card cap, the 256 dp Sleep/Wake and marker cap, the 4 dp armed spread, and the
two note-preview indents that clear the dot and numeral columns.

### D-19 — What is deliberately not copied

Recorded so a later phase does not "restore" one of these while chasing fidelity:

| Not copied | Why |
|---|---|
| `Look around with sample data` on the empty state | Would write fabricated user data; D-25 of the foundation |
| The `RECENT` / `Last 10 of 122` list header | Two strings MindScale does not have; adding them is copy |
| `padPrompt` — "How intense is it right now?" | A string MindScale does not have |
| The Log day header's right-hand summary | A string MindScale does not have, and a time-weighted one would be an inference |
| The From/To eyebrows | A second visible string per field; D-11 above |
| Splitting the readout into number and band | One frozen string; D-9 above |
| `rgba(ink,.28–.45)` as text | Fails AA; D-6 of the foundation |
| `rgba(ink,.10–.16)` as a control boundary | Fails the 3:1 non-text floor; D-4 above |
| `rgba(174,140,79,.45–.55)` as a control boundary | Fails the 3:1 non-text floor; D-4 above |
| The prototype's in-flow inline backdate and edit cards | `AlertDialog`'s scroll behavior fixed the Phase 12 clipping defect; D-19 of the foundation |
| The prototype's hardcoded light gold in dark mode | An omission in the source; D-5 above |

## User experience

Nothing about the flow changes. Every screen is reached the same way, every control does the same
thing, and every string is the same string. What changes is that Track becomes the design's screen
— a centred pad of twelve outlined circles that grows a gold ring when armed, an ink toast pill, a
26 sp readout, hairline-separated cards, and entry rows built on a 42 dp gold-ringed dot — and Full
Log becomes the design's underlined filter row over gold day headers and hairline-separated rows.

Configuration changes, process death, back navigation, and state restoration are untouched: this
phase adds no state and removes none.

## Frozen interfaces and data contracts

Changed:

| File | Change |
|---|---|
| `track/TrackScreen.kt` | Body restyle, L-1, L-2, the armed pad, `MsSpacing` conversion, dialog labels |
| `log/LogScreen.kt` | Body restyle, L-4, `MsSpacing` conversion, dialog labels |
| `ui/components/MsControls.kt` | The three D-15 carry-over corrections |
| `ui/theme/Spacing.kt` | `helpButton` added |

Added:

| File | Contents |
|---|---|
| `app/src/test/…/ui/theme/MsControlBoundaryContrastTest.kt` | The D-4 table, computed |
| `app/src/androidTest/…/track/TrackVisualTest.kt` | L-1, the armed pad, the toast pill, L-2, touch targets, 200% |
| `app/src/androidTest/…/log/LogVisualTest.kt` | L-4, one-baseline actions, chip targets, day headers, 200% |
| `app/src/debug/…/designgallery/TrackLogPreviews.kt` | `@Preview` over both screens, light/dark at 100%/200% |

Unchanged, and any diff to them is a defect in this phase: every `ViewModel`, every DAO, every
entity, `TrackUiState.kt`, `TrackEvent`, `LogUiState`, `LogEvent`, `Migrations.kt`, every exported
schema JSON, `DataExport.kt`, `BackupImport.kt`, `ImportPreflight.kt`, `RecordsCsvImport.kt`,
`SafetyContent.kt`, `BreathingContent.kt`, `IntensityRamp.kt`, `Band.kt`, `MindScaleApp.kt`, the
main `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, and every file
under `src/test/` and `src/androidTest/` that existed before this phase.

## Invariants

1. Every one of the 52 pre-existing `testTag` values in Track and Log still resolves to a node with
   the same role and the same callback.
2. Every content description asserted by the connected suite is unchanged, and each still sits on
   the node that carries the matching click action.
3. Every visible string is byte-identical. Case and size differences on screen are presentation
   only and are absent from semantics.
4. No text is painted below 4.5:1 against the surface behind it.
5. No border, ring, or mark that is the sole boundary or sole state indicator of an interactive
   control is below 3:1 against either adjacent surface.
6. The armed pad state is carried by at least one signal that is not colour.
7. No interactive element has a touch target below 48 dp.
8. Both screens reflow at 200% font with nothing clipped.
9. No composable in `ui/components/` owns state, performs a side effect, navigates, or sets a
   `testTag`.
10. `IntensityRamp.kt` is byte-identical, and `intensityColor` still has callers.
11. No `.dp` or `.sp` literal remains in either screen that is not a token reference or a
    commented one-off.

## Android compatibility

`minSdk` 26, `targetSdk` 36, `compileSdk` 36.1 — unchanged. No new dependency, permission, or
resource beyond what Phase 15 already bundled. `BoxWithConstraints` adds one subcomposition to the
numpad, which composes once per Track render and holds no state. `FlowRow` is already used on both
screens. Process death, rotation, back navigation, and offline behavior are unaffected because no
state is added or removed.

## Acceptance criteria

- [x] **REGRESSION**: `connectedDebugAndroidTest` passes **226/226** — the 206 baseline plus 20 new
      — with no pre-existing test file modified.
- [x] **REGRESSION**: `test` passes **411/411** — the 399 baseline plus 12 new — with no
      pre-existing test file modified.
- [x] **DIFF**: `git diff --name-status 238b0ba HEAD -- app/src/test app/src/androidTest` shows
      three `A` lines and zero `M` lines. `git diff --check` passes.
- [x] **LINT/BUILD**: `lint` reports 0 errors and the unchanged 22-warning baseline;
      `assembleDebug` passes.
- [x] **UNIT**: `MsControlBoundaryContrastTest` reproduces every figure in D-4's tables to two
      decimals, asserts each design value fails 3:1, and asserts every adopted replacement clears it
      on both `bg` and `card` in both themes. Writing it corrected five of the figures this spec
      had derived by hand and changed the diagnosis on one row — see the record below.
- [x] **UNIT**: the same test asserts that full light gold does **not** clear 3:1 on
      `surfaceContainerHighest`, pinning D-4's constraint 1.
- [x] **UNIT**: the same test asserts the decorative exemptions stay deliberately below 3:1, so the
      exemption is a decision rather than an oversight, and that the three failing text alphas are
      replaced by compliant emphasis levels that keep the design's ordering.
- [x] **INSTRUMENTED**: `TrackVisualTest` asserts the final numpad row is centred on the pad's axis
      to within 1.5 px, that both keys moved inboard of the prototype's columns, that all twelve keys
      share one width and one height, that the final row keeps the grid's gap, and that `0` stays
      left of `10` below the grid (L-1). **This is the assertion that caught a real defect** — see
      the record below.
- [x] **INSTRUMENTED**: `TrackVisualTest` asserts the three entry-row actions share one vertical
      centre and have even gaps (L-2), at 100% **and** at 200% font, and that each is a 48 dp target.
- [x] **INSTRUMENTED**: `TrackVisualTest` asserts arming the pad moves and resizes no key, so the
      rings are drawn in space reserved in every state. Restated honestly from the frozen wording:
      it does **not** assert that a mark appears, because a border colour is not in the semantics
      tree. The armed rings' appearance is verified by installed-app capture instead.
- [x] **INSTRUMENTED**: `TrackVisualTest` asserts every numpad key, both Sleep/Wake toggles, and the
      help toggle reach 48 dp, and that the toast is a centred pill narrower than the screen rather
      than the full-bleed banner it replaced.
- [x] **INSTRUMENTED**: `LogVisualTest` asserts `ALL` has a trailing gutter equal to the
      inter-element gap and is not flush against the row edge (L-4), that From and To stay
      equal-width 48 dp targets, that the row-action group shares one baseline with even gaps at
      100% and 200% font, that a Delete-only row aligns to the same trailing edge as a full row, and
      that the day header sits above its first row.
- [x] **INSTRUMENTED**: `LogVisualTest` asserts every one of the eleven inline-edit value chips
      reaches 48 dp on **both** axes and that the painted pill stays shorter than its touch target.
      **This pins the second defect found by capture** — see the record below.
- [x] **UI/ACCESSIBILITY**: thirteen `@Preview` composables over Track and Full Log in light and
      dark at 100% and 200% font, plus the armed pad and both empty states, in
      `src/debug/…/TrackLogPreviews.kt`. They compile and are debug-only. Stated honestly: they were
      **not** rendered in the IDE preview pane in this session; the same theme and scale
      combinations were verified by installed-app capture instead, which is the stronger oracle and
      is what found both defects.
- [x] **MANUAL**: installed-app capture on the API 36 emulator, compared screen by screen against
      `docs/design/reference/`: Track top and scrolled in light and dark, Track at 200% font top and
      scrolled, Full Log in light and dark, and Full Log at 200% font. Light Full Log is compared
      against the HTML and the dark capture only, because **no light Full Log screenshot exists** —
      that gap is stated rather than papered over.
- [x] **MANUAL**: installed-app capture of surfaces no screenshot covers — the armed pad in light,
      the toast pill twice, the readout, the entry row with its badge, the help card, the marker
      input, the backdate dialog, the delete-confirm dialog, the date picker at 200% font, a set
      filter field at 200% font, the Log inline edit panel, and the Track and Log empty states.
- [x] **MANUAL**: both screens at 200% font with nothing clipped, and emulator font scale, night
      mode, and rotation restored to `1.0`, `no`, and enabled, with the app's data cleared.

Not met, and stated rather than quietly dropped:

- [ ] **MANUAL**: the onset-chip prompt, the check-in card, the anchor card, the paused card, the
      Log inline **note** panel, and Log's filtered-empty and message and read-error banners were
      not captured on the installed app. Each needs state the app will not enter on request: the
      check-in card needs 40 entries and 60 days since the last check-in, the anchor prompt is a
      one-shot flag, the onset-chip prompt needs the episode engine to classify a capture as an
      onset, and the two Log banners need an injected mutation or read failure. What does cover
      them: the connected suite asserts each one's presence, tag, strings, and actions, and each is
      built from primitives whose rendering *was* captured — `MsCard` by the help card, `MsEyebrow`
      by both empty states, `MsTextAction` by the marker and dialog actions, and `MsChip` by the Log
      inline edit panel. Their geometry is therefore compositional rather than novel, but it is not
      the same as having looked at them.
- [ ] **REVIEW**: no critical-tier review pass was spent on this phase. The 226 connected tests, the
      411 JVM tests, the mechanical `testTag` and string diffs, and the capture matrix were treated
      as the evidence instead. This is recorded as a gap, not as a claim of equivalence.

## Implementation and verification record

Completed 2026-08-06 on `agent/phase16-track-and-log`.

**Two defects were found by building and looking rather than by reasoning, and both are now pinned
by tests.**

1. **The numpad's columns were 2 px unequal.** `(maxWidth - gap * 2) / 3` is a fractional `Dp` that
   rounds up to the same pixel width for all three keys, so each row overshot its constraint and
   Compose measured the *last* key in every row 2 px narrower than its siblings. L-1 requires all
   twelve keys to share one width, so this was a real failure of the correction rather than a
   rounding curiosity. The pad now floors in integer pixels — `(constraints.maxWidth - gapPx * 2) / 3`
   — so `3 × key + 2 × gap` fits exactly. Found by `TrackVisualTest`'s own assertion on its first
   run, which is the argument for asserting geometry numerically instead of looking at a screenshot.
2. **`MsChip` grew its painted pill to the touch target instead of padding to it.** D-23 says the
   painted geometry is the design's and the target is MindScale's; Phase 15's `MsChip` applied
   `defaultMinSize(minHeight = 48.dp)` to the pill itself, so an eleven-chip value row read as a row
   of tall ovals — and the same chip labelled `0` was left only **33 dp wide**, failing the very
   floor it was over-satisfying vertically. The pill is now the design's compact shape inside a
   transparent 48 dp square. Found by installed-app capture of Log's inline edit panel, which is the
   only surface in the app where eleven narrow chips sit side by side, and pinned by two new
   `LogVisualTest` assertions.

**Measurement corrected the spec twice more, before implementation depended on it.** Writing
`MsControlBoundaryContrastTest` moved five of D-4's hand-derived figures by a hundredth — the tables
now hold what the code computes, not what arithmetic on paper produced — and it changed one
diagnosis outright: the armed key ring's 55% alpha is unsalvageable in light at 1.78:1 but **would
have passed in dark at 3.30:1**. The prototype fails in dark at 2.61:1 only because `keyBase`
hardcodes the light gold triple on an element whose sibling `padWrapStyle` uses the theme-aware
`var(--gold)`. Full theme gold is adopted because it is one value legal in both themes rather than
an alpha legal in one.

**One risk was closed before it could become a defect.** The Log filter field originally set
`maxLines = 1` with an ellipsis. `MMM d, yyyy` fits one line at 200% font on the API 36 emulator —
checked by capture — but on a narrower device that would have rendered a date as `Aug 6, 2…`, and
losing characters is worse than taking a second line. The constraint is removed. This is Phase 15's
clipped-tab lesson applied in advance rather than repeated.

**A fourth carry-over correction joined the three D-15 named.** `MsChip`'s touch-target treatment
above is the fourth Phase 15 component defect this phase found by being the first to put those
components on a real screen. That is the argument for correcting them here rather than letting
Phase 18 inherit them into four more screens.

Accepted consequences, stated rather than discovered later:

- **Log's rows are about 1.6× taller than the design's**, because its three actions moved to their
  own line. The design fits them inline only because its actions are 9 px labels with no touch
  target; D-23's 48 dp floor is MindScale's addition and it outranks the prototype. See D-12.
- **Track's prompt row is usually empty beside the help toggle**, because MindScale has no
  `padPrompt` string and adding one would be a copy addition. See D-13.
- **The help toggle now renders above the anchor prompt** rather than below it, a consequence of
  merging the readout and help row as the design does. No control changed. See D-13.

Honest gaps beyond the two unmet criteria above: spoken TalkBack output was not audited, only the
semantics tree; the armed rings' colour is verified by capture rather than by an assertion, because
a border is not in the semantics tree; and the six surfaces listed as uncaptured were reasoned about
compositionally rather than looked at.

## Rollout, migration, and rollback

No migration. Room stays at schema 7; the JSON backup stays at version 7; the records CSV header
stays byte-identical. Nothing this phase writes is persisted, so rollback is `git revert` of the
phase's commits with no data consequence and no user-visible state to reconcile.

## Task decomposition

1. Freeze this spec — oracle: a documentation commit before any application-code edit.
2. `MsControlBoundaryContrastTest` and the D-15 component corrections — oracle:
   `.\gradlew.bat test`.
3. Track's numpad: circles, the `outline` ring, the armed wrapper and spread, L-1 — oracle
   `TrackVisualTest`'s L-1 assertions plus the existing numpad and navigation tests.
4. Track's entry row: the dot, the badge, L-2 — oracle: `TrackVisualTest` plus
   `NavigationTest.editAndNoteDialogRawDrafts_surviveActivityRecreation`.
5. Track's remaining surfaces: readout and help row, cards, banners, toggles, marker, links,
   toast, empty state, `MsSpacing` — oracle: the full `TrackScreenTest`.
6. Log: filter row and L-4, banners, day headers, rows, panels, empty states, `MsSpacing` —
   oracle: the full `LogScreenTest`.
7. Dialog action labels on all five sites — oracle: the three `assertIsNotEnabled` assertions
   specifically confirmed.
8. Previews, then full verification and installed-app capture — oracle: all four Gradle oracles
   plus the manual matrix.

## Open questions and approval gates

None open. One point is **flagged rather than left silent**: D-7 reinterprets
`SPEC-track-numpad-logging.md` Invariant 12's phrase "visually distinct group" as satisfied by
position rather than by shape and tone. The order and grouping that invariant freezes are
untouched, the mockup it appeals to draws all twelve keys identically, and the current instruction
names circular numpad keys among the things to preserve exactly. This is recorded in the completion
report so the reader sees the reconciliation rather than discovering it in a diff.
