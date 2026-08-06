# SPEC-insights-visual: applying the brand foundation to Insights, and resolving the intensity ramp

Status: IMPLEMENTED — VERIFIED LOCALLY

Owner: Claude Code (Phase 17 of 18), on the user's instruction of 2026-08-06

Date: 2026-08-06

Frozen documentation commit: `c1420c7`

Verified implementation commits: `4dd99eb` (the ramp and its two contrast tests), and the screen
restyle plus the visual test and previews recorded in the implementation section below

## Purpose

Phase 15 installed MindScale's visual identity as a shared foundation and applied it to the app
chrome. Phase 16 dressed Track and Full Log. This phase dresses **Insights**, and it resolves the
one decision both earlier phases explicitly deferred: what `intensityColor` should be
(`SPEC-visual-foundation.md` D-24).

It changes how Insights looks. It changes nothing about how it works.

## Sources reconciled

Authority order for every hex, size, tracking, radius, and shadow, highest first:

1. The current user instruction and its explicit boundaries.
2. `docs/specs/SPEC-visual-foundation.md`, D-1 through D-25 — frozen, and binding here. D-1
   (visual-only), D-6/D-7/D-8 (the emphasis and gold/danger calibrations), D-11 (uppercase as
   presentation), D-15 (the component contract), D-22 L-3 (the one layout fix this phase owns),
   D-23 (the accessibility floors), and D-24 (the ramp deferral this phase resolves).
3. `docs/specs/SPEC-track-and-log-visual.md`, D-1 through D-19 — frozen. D-4's measured
   control-boundary contrast table and D-15's four Phase 15 component corrections are reused here
   rather than rediscovered, and D-8 records why Track stopped calling `intensityColor` without
   pre-empting this phase.
4. The pre-existing test suite. This is not a stylistic authority but it is a **binding** one:
   D-1 forbids editing any pre-existing test file, and `IntensityRampTest` turns out to constrain
   the ramp decision more tightly than the design does. See D-4 below.
5. `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html` — the
   literal inline `style=""` strings. Line anchors used: 328-330 the range-chip row, 335-341 the
   empty state, 345-353 the summary strip, 355-382 the raster panel and its legend, 384-412 the
   entry chart, 414-424 the Episodes fact card, 426-438 the Each-episode card, 440-471 the gap
   histogram, 473-500 the onset-hour histogram, 502-514 the sleep block, 516-518 the report link,
   890 `ramp()`, 1311-1390 the insights model block, 1450-1462 `gapBars`, 1464-1483 `hourBars`,
   1504-1513 `summary` and `rangeChips`.
6. `docs/design/reference/` — the rendered screenshots.
7. The prototype's own layout, which is advisory only (D-22 of the foundation).

### Screenshot coverage, stated honestly

| Surface | Reference |
|---|---|
| Insights top, dark — range chips, summary strip, raster, legend, raster caveat | `dark-insights-top.png` |
| Insights scrolled, dark — entry chart, chart caveat, Episodes, Each episode | `dark-insights-scrolled.png` |
| Insights raster and entry chart, light | `light-insights-raster-entry-chart.png` |

**There is no light Insights top or scrolled capture, and no dark entry-chart capture.** Light
range chips, the light summary strip, and the dark entry chart's own panel are derived from the
HTML and from the captures that do exist, and no claim is made that they were matched against a
reference that does not exist. This is the same discipline Phase 16 held for the missing light
Full Log screenshot.

Covered by no screenshot at all, and derived from the HTML alone or from MindScale's own idioms:
the gap histogram, the onset-hour histogram, the sleep-count cells, every refusal panel, the
loading state, the range-empty line, the stale-snapshot error banner, the selected state of every
histogram cell, and the report link. The refusal panels, the error banner, the range-empty line,
the sleep-count cells and the live readouts appear nowhere in the prototype at all — they are
MindScale's own, and their treatment is derived from the design's card and action idioms rather
than copied.

### Measured starting state

Taken from the tree at `18ab15d746abe21a4651547f2e850d7fe7256d09`, so the acceptance criteria are
checkable rather than impressionistic:

- `InsightsScreen.kt` is 1149 lines; `IntensityRamp.kt` is 42 lines and byte-identical to its
  Phase 1 form.
- 23 `testTag` call sites in `InsightsScreen.kt`, four of them templated
  (`insight_range_${range.name}`, `onset_gap_bucket_${index}`, `onset_time_hour_${hour}`,
  `sleep_category_$index`). `insights_tab` lives in the chrome and is untouched.
- 93 hardcoded `.dp` literals and 0 `.sp` literals in `InsightsScreen.kt`.
- `intensityColor` has exactly three call sites, all in `InsightsScreen.kt`: the raster's per-cell
  fill at line 720 and the two legend swatches at lines 757-758.
- The raster panel, the entry-chart panel, every fact row, every episode row and every refusal
  panel is a `Surface(tonalElevation = 1.dp)` on `surface`, not the design's hairline-bordered
  `card`.
- The range chips are Material `FilterChip`s. The histogram cells are `Surface(onClick = …)` on
  `surfaceVariant`/`secondaryContainer` with `outlineVariant`/`onSecondaryContainer` borders.
- No label on the screen is uppercased; the design uppercases every section title, every eyebrow,
  and every raster row label.
- Baselines to hold: 411/411 JVM tests, 226/226 connected tests, lint 0 errors at the unchanged
  22-warning baseline, `assembleDebug`.

## Scope

This phase delivers, and nothing else:

- `IntensityRamp.kt`: the resolved colour mapping and anchors (D-4, D-5, D-6).
- Insights' body: the range chips, the error banner, the loading and empty states, the summary
  strip, the raster panel and its legend, the entry chart and its legend, the Episodes fact list,
  the Each-episode list, the gap histogram, the onset-hour histogram, the sleep-count cells, every
  refusal panel, every live readout and caveat, and the report link.
- The one D-22 layout correction this phase owns: **L-3**.
- Converting `InsightsScreen.kt`'s 93 dimension literals to `MsSpacing`, per D-14 of the
  foundation.

## Non-goals

- Any behavior change whatsoever. See D-1.
- Settings, Profile, Report, Safety, Breathing, and the closing audit (Phase 18).
- Track and Full Log, which Phase 16 finished. `TrackScreen.kt` and `LogScreen.kt` are untouched.
- Re-litigating the entry chart's step-only rendering, which `SPEC-insights-entry-chart.md`
  settled. No LINE/STEPS toggle and no interpolation (D-18).
- The raster's day/hour grid geometry, the histogram bucket boundaries, and every derivation in
  `EpisodeEngine.kt`, which are frozen by Phases 5, 6, 8, 9 and 10 and are not touched.
- Any dependency, permission, toolchain, Gradle plugin, manifest, Room schema, backup, CSV,
  export, import, or migration change.
- Any new string, control, entry point, or navigation path.

## Decisions

### D-1 — The visual-only rule, as this phase's testable contract

Only how pixels are painted may change. No feature is added, removed, renamed, reordered, or
rewired. No navigation change, no new entry point, no control that was not there, no control taken
away. No copy edits. No data, storage, export, or migration change. No dependency, permission, or
toolchain change.

The proof is objective and the baselines have moved again:
**226/226 connected tests and 411/411 JVM tests must pass unchanged.** A test that breaks is
evidence that behavior changed, and that is a defect in this phase, not a test to update. No
pre-existing test file may be edited. New test files may be added; existing assertions may not be
relaxed, retargeted, or deleted.

Checked mechanically rather than by inspection:

```
git diff --name-status 18ab15d HEAD -- app/src/test app/src/androidTest
```

Every line must be an `A`. A single `M` is a defect in this phase.

`SPEC-visual-foundation.md` Invariant 3 also binds: **every visible string stays byte-identical.**
Case differences on screen are presentation only and are absent from semantics (D-11).

**This rule is not decorative here.** It is what decides D-4, the largest decision in the phase.

### D-2 — Fidelity: careful, not pixel-perfect

Copy the elegance, colour, and theme with care. Do not copy the prototype's layout flaws, and do
not copy a value that fails an accessibility floor. Every such divergence in this phase is D-4,
D-7, D-8, D-9, D-10, D-12, D-13, and D-18. There are no unrecorded ones.

Preserved exactly, with no divergence: the raster's day/hour grid geometry and its one-row-per-day
projection, the entry chart's step-only line with no interpolation, the histogram bucket cells and
their boundaries, 14 dp cards on hairline borders, near-flat elevation, 999 dp pills, 2 dp bar and
swatch radii, and the uppercase letter-spaced label idiom through `MsUppercaseText`.

### D-3 — Uppercase remains presentation, and the one named exception

Every label this phase uppercases goes through `MsUppercaseText` (D-11 of the foundation). Nothing
is uppercased at a call site or in a string constant.

**One element the design uppercases is deliberately left in its own case: the raster's row date
labels.** The design paints them `text-transform:uppercase` at 7.5 px. MindScale's are produced by
`DateTimeFormatter.ofPattern("MMM d")` under the *default* locale and are already excluded from
the semantics tree by `clearAndSetSemantics { }`, because the raster's readout is what speaks a
day. Uppercasing a locale-formatted date is exactly the class of bug `Locale.ROOT` exists to
prevent — and routing it through `MsUppercaseText` would *add* thirty date strings to a semantics
tree that today holds none of them, next to a node whose `contentDescription` and
`stateDescription` are asserted by `InsightsScreenTest`. The label keeps its formatter's case and
its empty semantics. This is a divergence from the design and it is recorded rather than smuggled.

**A second set of labels is left in its own case, and this one was corrected during
implementation rather than foreseen: the histogram bucket boundaries and clock hours.** They were
first built as `MsUppercaseText`, which rendered `<1d` as `<1D` and `12a` as `12A`. The design's
own `gapBars` and `hourBars` labels at lines 1461 and 1469 set neither `text-transform` nor
tracking, because a bucket boundary is *data* rather than a label — the same distinction that keeps
an onset chip in the user's own words. Both are plain `Text` now.

Onset chips, note previews, marker text, and the user's own words are never uppercased, as
`SPEC-track-and-log-visual.md` D-3 already established.

### D-4 — The intensity ramp: what the prototype does, what a pre-existing test forbids, and what is adopted

This resolves D-24 of the foundation. It is the largest decision in the phase, and it is settled by
measurement and by a test that already exists — not by taste.

**What ships today.** `intensityColor` lerps slate-blue `#6B7A8F` → gold `#AE8C4F` in light and
`#3A4652` → `#C9A96A` in dark, over `v/10` for `v` in `0..10`, with `0` mapped to the low anchor.

**What the prototype does.** `ramp()` at line 890 is one warm interpolation per theme — light
`#F0E4CC` → `#6E5220`, dark `#3A2F1C` → `#E0BE7A` — clamped to `1..10` over `(v-1)/9`, with no
defined treatment for `0`.

**Finding 1 — the prototype's low anchors are invisible in *both* themes, not only in light.**
D-24 flagged light `#F0E4CC`. Measured, dark `#3A2F1C` fails too:

| Design anchor | vs `card` | vs `bg` | vs the asleep fill |
|---|---|---|---|
| light `#F0E4CC` (intensity 1) | **1.26** | **1.22** | **1.11** |
| dark `#3A2F1C` (intensity 1) | **1.38** | **1.47** | **1.22** |

At 1.26:1 against `card` and 1.11:1 against the asleep fill, a light-theme rating of **1 is
indistinguishable from "nothing recorded" and from "asleep"**. In a symptom tracker that is not a
cosmetic weakness: it renders a month in which the user logged as a month in which they did not.
`SPEC-track-numpad-logging.md` Invariant 14 and D-24's own worry both point at exactly this.

**Finding 2 — the prototype's light ramp runs the opposite way, and a pre-existing JVM test
forbids it.** The design's light ramp descends in relative luminance, from 0.784 at intensity 1 to
0.095 at intensity 10. `IntensityRampTest`
(`app/src/test/…/ui/theme/IntensityRampTest.kt`, five tests, pre-existing since Phase 1) asserts:

1. light ramp luminance is **monotonically non-decreasing** from 0 to 10;
2. dark ramp luminance is monotonically non-decreasing from 0 to 10;
3. light and dark differ at both 0 and 10;
4. `intensityColor(-1)` and `intensityColor(11)` throw `IllegalArgumentException`.

Adopting the design's light ramp verbatim would fail (1). D-1 forbids editing that file and
requires all 411 JVM tests to pass, and the current user instruction says so in terms: *"no
pre-existing test file modified… a single `M` is a defect in this phase."* The design HTML is
authority 5 in this spec's own ordering; D-1 is authority 2. **The light ramp therefore keeps a
non-decreasing luminance direction.**

**Finding 3 — the endpoint separation is paid for by compliance, not by the constraint.** This
table was corrected by `MsIntensityRampContrastTest` after the spec was first drafted, and the
correction changes the argument rather than a decimal:

| Light ramp | intensity 1 | intensity 10 | endpoint separation | min vs `card` |
|---|---|---|---|---|
| the design as drawn (rejected) | `#F0E4CC` 1.26 | `#6E5220` 7.26 | **5.77** | **1.26** |
| the design's direction, low anchor raised to clear 3:1 | `#A28C65` 3.24 | `#6E5220` 7.26 | 2.24 | 3.24 |
| **adopted — the design's own pair, ascending** | `#6E5220` 7.26 | `#AE8C4F` 3.15 | **2.31** | 3.15 |

The design's raw pair really is far wider end to end — and it buys that width by putting one end
where it cannot be seen. Raise that end to the palest point on the design's own line that clears
3:1, and its separation collapses to 2.24, *below* the adopted ramp's 2.31. So most of the width
is lost to the accessibility floor whichever direction is chosen, and the direction the
pre-existing test allows costs nothing further. What it does cost is stated plainly in "the
accepted consequence" below.

**Finding 4 — the app's own dark ramp also fails, and always has.** Today's dark low anchor
`#3A4652` measures 1.87:1 against `card` and its intensity 1 measures 2.19:1. That is a real
pre-existing defect this phase fixes, and it was found by measuring rather than by the brief.

**The adopted ramp.**

```kotlin
private val LightRampLow  = Color(0xFF6E5220)   // the design's own light warm-brown anchor
private val LightRampHigh = Color(0xFFAE8C4F)   // MaterialTheme.ms.gold, light — unchanged
private val DarkRampLow   = Color(0xFF856F46)   // a point on the design's dark ramp line
private val DarkRampHigh  = Color(0xFFC9A96A)   // MaterialTheme.ms.gold, dark — unchanged

val fraction = (value.coerceIn(1, 10) - 1) / 9f
```

Four properties make this the right shape rather than a compromise:

1. **One rule, both themes.** The ramp runs from a dim warm brown to *the theme's own gold*. In
   light that gold is `#AE8C4F`; in dark it is `#C9A96A`. Intensity 10 is therefore painted the
   same colour as the armed pad ring, the header rule, the day headers and the episode peak — the
   colour this app already uses to mean "look here". The slate-blue was the last non-brand hue in
   the app and it goes.
2. **Both anchors come from the design.** `#6E5220` is the design's own light ramp endpoint.
   `#856F46` is the point on the design's own dark ramp line `#3A2F1C` → `#E0BE7A` at which
   intensity 1 first clears 3:1 against `card`, `bg` **and** the asleep fill with margin. Neither
   is invented.
3. **The mapping is the prototype's.** `(v-1)/9` clamped to `1..10`, so the two legend swatches
   labelled `1` and `10` are literally the ramp's two anchors and the legend promises what the
   raster paints. Under `v/10` the low anchor is a colour no cell can ever be, because the engine
   never emits intensity 0 (D-5).
4. **Every value clears the floor.** Minimum against `card` is 3.15:1 in light (at intensity 10)
   and 3.74:1 in dark (at intensity 1); minimum against `bg` is 3.05:1 and 4.00:1.

`MsIntensityRampContrastTest` computes all of this. `intensityColor` keeps its `require(value in
0..10)` and its three call sites, and the file's KDoc is rewritten to describe the new anchors.

**The accepted consequence, stated rather than discovered later.** On a light page, luminance
ascending means *prominence descending*: a rating of 1 is a solid dark brown at 7.26:1 while a
rating of 10 is gold at 3.15:1, so a mild day carries more visual weight than a severe one. The
design intends the opposite. Three things make it tolerable and none of them makes it invisible:
the direction is forced by a pre-existing test, not chosen; the hue still travels from muted brown
to saturated brand gold, which is the direction the legend labels and the dark theme reinforce;
and the dark theme — where the raster is most often read, and the only theme with a scrolled
reference capture — runs dim-to-bright with prominence and intensity increasing together. **This
is the one place in the phase where a frozen constraint and the design point in opposite
directions, and it is flagged in the completion report rather than left in a diff.**

Finding 3 also removes the tempting counter-argument. The design's direction is not more legible
once it is made compliant; it is very slightly less. The whole of its extra width lives in the part
of the ramp the 3:1 floor deletes.

### D-5 — The 0-versus-1 low anchor, resolved by the engine rather than by preference

D-24 required this phase to resolve the difference explicitly. It resolves without a judgment call:

**`0` and `1` render the same colour, and no cell can ever be `0`.**

`EpisodeEngine.kt:384` is `if (entry.value == 0) return@forEachIndexed` — a zero-valued entry never
produces an `IntensitySegment`. `buildRaster` therefore classifies that instant as
`RasterState.WELL`, "nothing recorded", by construction, and `RasterState.INTENSITY` carries only
`1..10`. The `segment.intensity ?: 0` elvis at the raster's call site is defensive and unreachable
from any valid state; it is left exactly as it is, because removing it would be a behavior change
and because the clamp makes it harmless.

So `intensityColor(0)` maps to the low anchor — the prototype's own treatment of its undefined
case — and it still must not throw, because `IntensityRampTest` asserts `intensityColor(0, light)
!= intensityColor(0, dark)`. Both hold: `#6E5220 != #856F46`.

Recorded so a later reader does not re-derive it: **the reason a zero rating is not a faint colour
is that it is not a colour at all.** A rating of 0 is the design's `wellC` — the card itself — and
the raster caveat already says so in words the phase may not edit: *"Plain space is awake time with
nothing recorded."*

### D-6 — Invariant 14 re-checked, at every place a fill carries a value

`SPEC-track-numpad-logging.md` Invariant 14: *colour is never the sole carrier of value
information — every value indicator is paired with the numeric value as text.* Phase 16 leaned on
it to remove Track's filled entry dot. This phase must not weaken it, and it is the reason the
ramp's floor is set where D-4 sets it.

There are exactly three places in the app where `intensityColor` fills a shape, and each is checked
here rather than assumed:

| Site | What carries the value independently of colour | Verdict |
|---|---|---|
| Raster cell fill | The touch/drag readout, which is a `Polite` live region *and* the raster's `stateDescription`, and which spells the value: `"… · intensity 7"`. Four TalkBack custom actions move the readout by hour and by day. Below it, the entry chart plots the same values against a labelled `10`/`5`/`0` axis with its own readout. | **holds** |
| Legend swatch `1` | The literal label `1` beside the swatch | **holds** |
| Legend swatch `10` | The literal label `10` beside the swatch | **holds** |

Two consequences are frozen from this:

1. **The raster's readout, its live region, its `stateDescription` and its four custom actions may
   not be restyled out of existence.** They are what makes the raster's colour legal. This is why
   D-8 keeps the readout on the same row as the section title rather than folding it away.
2. **A ten-step ramp cannot be 3:1 from every other category at once**, and pretending otherwise
   would be arithmetic theatre. On a light page the ramp spans relative luminance 0.095 to 0.283;
   nothing can sit 3:1 from every point of that span except a colour lighter than `#FFFFFF` or
   darker than the darkest step. The floor that binds is therefore the one that matters — **every
   intensity clears 3:1 against the "nothing recorded" ground (`card`) and against `bg`** — and
   category-versus-category separation is carried by the legend and the readout, in words. D-7
   tabulates and asserts every figure so the exemption is a decision rather than an oversight.

### D-7 — Control-boundary and graphical-object contrast in Insights, measured

`SPEC-track-and-log-visual.md` D-4 found that eight of the design's own borders fail D-23's 3:1
non-text floor because almost every control here is drawn as transparent-with-a-hairline. The same
`rgba(ink,.10–.16)` and `rgba(gold,.4–.55)` idioms recur throughout Insights' raster, charts and
histograms, and every one of them is measured against `MaterialTheme.ms.outline` and `.gold` here
rather than re-calibrated. **Every figure below is produced by `MsInsightsContrastTest`, not by
hand**, so the tables and the assertions cannot drift apart.

**Failing, and replaced:**

| Element | Design value | Light bg / card | Dark bg / card | Adopted |
|---|---|---|---|---|
| Range chip border, unselected (line 1512) | `rgba(ink,.16)` | 1.41 / 1.41 | 1.51 / 1.57 | `ms.outline` — 3.47 / 3.49 light, 3.47 / 3.51 dark |
| Histogram cell border, unselected | `rgba(ink,.16)` | 1.41 / 1.41 | 1.51 / 1.57 | `ms.outline` |
| Legend "nothing" swatch border (line 1377) | `rgba(ink,.14)` | 1.35 / 1.35 | 1.42 / 1.48 | `ms.outline` |
| Histogram bar fill (lines 1456, 1469) | `rgba(gold,.85)` | 2.50 / 2.59 | 6.46 / 6.14 | `ms.gold` at full opacity — 3.05 / 3.15 light, 8.60 / 8.04 dark |
| Entry-chart step line (line 400) | `rgba(gold,.95)` | 2.84 / 2.94 | 7.83 / 7.38 | `ms.gold` at full opacity |
| Entry-chart event line (line 398) | `rgba(ink,.35)` | 2.24 / 2.24 | 2.94 / 2.97 | `ms.outline` |

The range chip and the histogram cell are unfilled when unselected, so in both cases the border
**is** the control's only boundary — the same finding, in the same words, that D-4 recorded for the
numpad key and the onset chip. The legend swatch labelled "nothing" is `card` on `card`: without a
boundary it is not a swatch at all. The bar fill and the step line are the graphical objects the
charts exist to draw.

**Deliberately not raised, because each is quiet ground rather than a control or a data mark, and
each is named in words elsewhere:**

| Element | Value | Light bg / card | Dark bg / card | Why exempt |
|---|---|---|---|---|
| Raster ASLEEP fill | `ms.sleepBand`, the design's `sleepC` | 1.35 / 1.39 | 1.20 / 1.12 | A named state the legend labels "asleep" and the readout speaks |
| Raster NO_DATA fill | `surfaceVariant` at `.45`, unchanged | 1.04 / 1.06 | 1.05 / 1.03 | A named state the legend labels "no data"; MindScale's own, absent from the design |
| Raster FUTURE fill | ink at `.08` light, `.14` dark, unchanged, **plus** 45° hatching | 1.17 / 1.18 | 1.42 / 1.48 | Carries a second, non-colour signal; Phase 5's review set these values |
| Chart gridlines and sleep columns | `ms.hairline`, ink `.09` | 1.21 / 1.20 | 1.22 / 1.26 | Separators and ground; D-6 of the foundation |
| Chart zero baseline | `ms.outlineDecorative`, ink `.16` | 1.41 / 1.41 | 1.51 / 1.57 | A rule beside a `10`/`5`/`0` text axis that is itself ≥ 4.5:1 |

Asserted as *below* 3:1 on purpose, exactly as Phase 16 did: the exemption is a decision, and a
later phase that "fixed" the raster's asleep band into a heavy block would lose the design's
quietest surface and break a test on the way.

One constraint carried forward and re-checked here: **full light gold is a compliant boundary on
`bg` and `card` only** (2.67:1 on `surfaceContainerHighest`). Every gold mark this phase paints —
bar fills, step line, day-header labels — sits on `card` or `bg`. Nothing paints gold on a
container step.

### D-8 — The raster panel, and why the legend stays outside it

The panel becomes the design's card: `MsCard` at 14 dp radius on `card` with a 1 dp `ms.hairline`
border and no elevation, 10 dp padding, replacing `Surface(tonalElevation = 1.dp)`. `raster_chart`,
its `contentDescription`, its `stateDescription` and its four `CustomAccessibilityAction`s move to
the card's modifier unchanged.

**The raster's geometry is preserved exactly**: one row per local day, the same
`rowHeight` ladder (20/10/6/4 dp by day count), the same 1 dp inter-row gap, the same
`clearAndSetSemantics { }` on every row Canvas, the same `detectTapGestures` and
`detectHorizontalDragGestures` on the same `pointerInput(day)` keys, and the same fraction
arithmetic. Not one line of the projection changes.

The five state fills are re-expressed on MindScale tokens, and only two of them change value:

| State | Was | Now | Source |
|---|---|---|---|
| `WELL` | `surface` (= `bg`) | `ms.card` | The design's `wellC` at line 1328. The panel is now a card, so "nothing recorded" is the card itself, as the design intends |
| `INTENSITY` | the slate ramp | the D-4 ramp | D-4 |
| `ASLEEP` | `outlineVariant` (ink `.09`) | `ms.sleepBand` | The design's `sleepC` at line 1327. The token was added in Phase 15 for exactly this and has had no caller until now |
| `NO_DATA` | `surfaceVariant` at `.45` | unchanged | MindScale's own state; the design paints it `transparent` |
| `FUTURE` | ink at `.08`/`.14` plus hatching | unchanged | MindScale's own state; Phase 5's review specifically tuned it and the hatching is its non-colour signal |

**The legend stays below the panel, where it is today, and does not move inside it as the design's
does.** This is a behavioral constraint, not a preference:
`InsightsScreenTest.rasterTouchAndAccessibilityActionUseOneExplorationSurface` does
`raster.performTouchInput { click(center) }` and asserts `assertNotNull(explored)`. On the
one-day snapshot that test builds, the panel is a single 20 dp row inside 10 dp padding, so its
centre lands on the Canvas. Moving a hairline, a legend row and its padding inside the panel would
push the centre roughly 38 dp down, past the only row there is, and the click would land on a
legend swatch. The panel's interior therefore stays exactly what it is: day rows and nothing else.

The legend itself takes the design's treatment where it is free to: 16 × 9 dp swatches at 2 dp
radius (`extraSmall`), the "nothing" swatch gaining an `ms.outline` boundary (D-7), and labels at
`labelSmall` in `inkQuaternary`. It keeps its `horizontalScroll`, because changing scroll to wrap
would change how its sixth item is reached.

The design's `12a / 6a / 12p / 6p / 12a` hour axis under the rows is **not** added: it is five
visible strings MindScale does not have, and adding them is a copy addition (D-18).

The section title becomes `MsUppercaseText` at `labelLarge` in `inkTertiary`; the readout keeps its
`Polite` live region, moves to `bodySmall` in `inkTertiary`, and gains `weight(1f)` with
`TextAlign.End` so that MindScale's much longer readout wraps inside the row instead of pushing the
title out of it at 200% font.

### D-9 — L-3: the summary strip, and what the recorded flaw actually is

D-22 records flaw L-3 as *"Insights summary strip's four columns have unequal widths, so
`TYPICAL LENGTH` breaks the rhythm"* and its correction as *"Equal-weight the four columns."*

The correction is implemented as written. The flaw's diagnosis deserves one correction of its own,
in the same spirit as Phase 16's D-6:

- **The flaw is the prototype's, and `dark-insights-top.png` shows it plainly** — its four
  `flex:1` cells each carry `white-space:nowrap` spans, so the strip's four columns render at four
  different widths and `TYPICAL LENGTH` takes the room the others give up.
- **Compose does not reproduce it.** `Modifier.weight(1f)` on four children of a `Row` divides the
  remaining width equally regardless of content, which the current screen already does. So the
  structural half of L-3 already holds today, before this phase edits anything.
- **What this phase owes L-3 is therefore to keep it true while uppercasing the labels, and to
  pin it.** `TYPICAL LENGTH` is fourteen tracked characters at 9 sp; at 200% font it is wider than
  a quarter of a 320 dp screen and must wrap rather than clip or steal width. `InsightsVisualTest`
  asserts the four cells share one width to within 1.5 px at 100% **and** 200% font, so a later
  change cannot quietly reintroduce the prototype's flaw.

The strip otherwise takes the design's treatment at lines 345-353: a `MsHairline` above and below,
`MsSpacing.lg` vertical padding, the value at `headlineLarge` (19 sp, weight 500, tabular) in
`inkPrimary`, and the label as `MsUppercaseText` at `labelSmall` in `inkQuaternary`. `insights_summary`
and each cell's `contentDescription = "$label, $value"` are unchanged —
`populatedSummaryAndEpisodesExposeLabeledSemantics` asserts `listOf("Episodes, 1")` exactly.

### D-10 — The entry chart

The panel becomes `MsCard`, keeping `entry_chart`, its `heightIn(min = 48.dp)`, its
`contentDescription`, its `stateDescription` and all six custom actions on the card's modifier. Its
interior structure — a 24 dp axis column beside a 180 dp Canvas, then the four-tick row — is
unchanged, for the same centre-of-node reason as D-8:
`entryChartTouchAndAccessibilityActionsUseOneExplorationSurface` clicks and swipes the node's
centre and asserts `exploreCalls > 1`.

The marks take the design's values where they pass and D-7's replacements where they do not:

| Mark | Design | Adopted |
|---|---|---|
| Step line | `rgba(gold,.95)`, 1.5 px | `ms.gold`, 2 dp, round caps — unchanged geometry (D-7) |
| Area under the line | `rgba(gold,.16)` | `ms.gold` at `.16` |
| Gridlines at 10 and 5 | `rgba(ink,.07)` | `ms.hairline` |
| Zero baseline | `rgba(ink,.18)` | `ms.outlineDecorative` |
| Sleep columns | `rgba(ink,.055)`, solid | `ms.sleepBand`, solid |
| Event lines | `rgba(ink,.35)` dashed | `ms.outline` dashed (D-7) |
| Crosshair | `#BE9C5C` | `ms.crosshair` — the Phase 15 token, until now unused |
| Selection dot | `var(--ink)`, r=4 | `ms.ink`, 4 dp |
| Axis labels `10`/`5`/`0` | 8.5 px `rgba(ink,.4)` | `labelSmall` in `inkQuaternary` |

**The sleep band loses its diagonal hatching**, which MindScale added and the design does not have.
This is a deliberate reduction and it is safe because the band is redundant reinforcement: the step
line genuinely *stops* across a sleep span, which is itself a non-colour signal, and the readout
says `"asleep"` in words. The raster's FUTURE hatching is **not** removed — there the hatching is
the only thing distinguishing future from no-data.

The step-only rendering, the absence of interpolation, and the absence of a LINE/STEPS toggle are
settled by `SPEC-insights-entry-chart.md` and by D-25 of the foundation and are not revisited.

### D-11 — The fact list and the episode list each become one card

The design draws both as a single 14 dp card whose rows are separated by `rgba(ink,.07)` hairlines
(lines 415-423 and 427-437). MindScale currently draws each fact and each episode as its own
elevated `Surface`, separated by the list's 18 dp gap.

Both collapse into a single `LazyColumn` item holding one `MsCard` of hairline-separated rows. This
is safe on size: `EpisodeEngine` caps `recentEpisodes` at 8 (`EpisodeEngine.kt:199`) and emits at
most six facts, so neither list is unbounded and neither loses meaningful lazy behaviour. The item
keys `fact:$index` and `episode:$onsetMillis` are internal to the list and are asserted by no test;
`performScrollToNode(hasText("Each episode"))` still resolves, and each episode row keeps its
`semantics { contentDescription = … }` node with the same composed string.

Row treatment: fact text at `bodyMedium` in `inkPrimary` with its detail at `bodySmall` in
`inkQuaternary`; episode timestamp at `titleSmall` in `inkPrimary`, detail at `bodySmall` in
`inkQuaternary` and the peak at `titleLarge` in `goldText`, which is the design's `--gold-deep`
peak at a compliant value (D-7 of the foundation). The two section titles become `MsUppercaseText`
at `labelLarge` in `inkTertiary`.

### D-12 — The three histogram and count sections

The gap histogram, the onset-hour histogram and the sleep-count cells share one treatment, and all
three keep every structural guarantee their own specs froze — horizontal reachability, individually
selectable cells with visible count and label geometry, redundant non-colour selection, at-least-48
dp targets, exact spoken semantics, a `Polite` live readout, and parent vertical-scroll ownership.

| Element | Was | Now |
|---|---|---|
| Section title | `titleMedium` | `MsUppercaseText` at `labelLarge` in `inkTertiary` |
| Refusal panel | `Surface(tonalElevation = 1.dp)` | `MsCard` with `bodyMedium` in `inkTertiary` |
| Cell, unselected | `surfaceVariant` fill, 1 dp `outlineVariant` | transparent, 1 dp `ms.outline` (D-7) |
| Cell, selected | `secondaryContainer` fill, 2 dp `onSecondaryContainer` | `ms.ink` fill, 2 dp `ms.ink` — the design's selected idiom |
| Bar | `primary`, square | `ms.gold` on an unselected cell and `ms.onInk` on a selected one, 2 dp top corners (`extraSmall`) as the design draws them |
| Count | `labelLarge` | unchanged size; `inkPrimary` unselected and `onInk` selected |
| Bucket and hour label | `labelMedium` | the D-19 data-label style, in its own case and untracked (D-3) |
| Denominator, readout, caveat | `bodySmall` | unchanged size, at `inkSecondary`/`inkTertiary` levels |

**Selection is never colour alone**: a selected cell changes *fill* (transparent → ink) and border
*width* (1 dp → 2 dp), and its `selected` semantics property is unchanged.

`Surface(onClick = …)` is kept as the cell's root rather than replaced by a `Box` with a gesture
modifier. That is deliberate: three connected tests
(`verticalSwipeOnOnsetGapBucketScrollsParentList`,
`verticalSwipeOnOnsetTimeHourScrollsParentList`, `verticalSwipeOnSleepCategoryScrollsParentList`)
assert that a vertical drag starting on a cell scrolls the parent list, which holds because
`clickable` does not consume vertical drags. This phase changes the `Surface`'s `color`, `border`
and content and nothing else about it.

The design's hover tooltip is not adopted: Android has no hover, and MindScale's live readout is
the accessible equivalent it already ships.

### D-13 — Range chips, banners, empty states, and the report link

| Surface | Treatment |
|---|---|
| Range chips | `MsChip`, which is the design's `chipStyleBase` idiom at line 1510 with the Phase 16 border correction already applied. Each keeps its `testTag`, its `contentDescription`, its `selected` semantics, its callback and a 48 dp target |
| Stale-snapshot error | `MsCard` with the message in `ms.danger` and `Retry` as a gold `MsTextAction`, matching Log's read-error banner (`SPEC-track-and-log-visual.md` D-12). The current `errorContainer` `Surface` would put `goldText` on an unreadable ground |
| Loading | `CircularProgressIndicator` unchanged; it already resolves to `primary`, which is `goldText` |
| Empty state | `MsEyebrow("Nothing to draw yet")` plus the paragraph at `bodyMedium` in `inkSecondary`, left-aligned as the design's is. `Look around with sample data` is **not** added (D-18) |
| Range-empty line | `bodyMedium` in `inkTertiary` |
| Report link | `MsPillButton(selected = true)` — the design's ink-filled pill at line 517, centred rather than full width. `insights_open_report` is unchanged and `NavigationTest` still scrolls to it and clicks it |

Two divergences recorded rather than smuggled:

- **The range chips keep `horizontalScroll` and are not centred** as the design centres them. Six
  48 dp targets and their gaps are 328 dp against a 379 dp content width at 100% font and do not
  fit at 200%; a centred non-scrolling row would put `6M` off-screen at large font, and
  `everyRangeIsReachableSelectedAndInvokesCallback` clicks all six.
- **The range chips take `MsChip`'s untracked label rather than the design's 1.2 px tracking.** A
  shared chip is one control, and this phase does not widen a component's contract for 1.2 px of
  tracking on a two-character label.

### D-14 — Gesture ownership and `SavedStateHandle` selection are untouched

Phases 5, 6, 8, 9 and 10 each froze an invariant about who wins a drag and how a selection
survives process death. This phase adds, removes and reorders no gesture modifier and no state.

- The raster's two `pointerInput(day)` blocks and the chart's two `pointerInput(chart)` blocks keep
  their exact detectors, keys and arithmetic. Horizontal drags explore; vertical drags fall through
  to the `LazyColumn`.
- Every histogram cell stays a `clickable` `Surface`, so a vertical drag on a cell scrolls the
  parent (D-12).
- `selectedOnsetGapBucketIndex`, `selectedOnsetHour`, `selectedSleepCategoryIndex`,
  `exploredInstantMillis` and `chartExploredInstantMillis` are read from `InsightsUiState` and
  written through the existing callbacks. **No `rememberSaveable` is added, removed, or moved
  across a composable boundary**, because Phase 16's two defects both came from restructuring
  around a measurement or sizing boundary and this is the same class of risk.
- `InsightsViewModel.kt` is not touched.

### D-15 — Every tag, description, and unmerged-tree shape survives

The 23 `testTag` call sites in `InsightsScreen.kt` keep their exact values, and each still resolves
to a node with the same role and the same callback. The tag list is read before restructuring, not
after.

Five shapes the connected suite depends on that a restyle could silently break, and how each is
preserved:

1. **`raster_chart` and `entry_chart` are clicked and swiped at their node centre.** Their
   interiors keep their current composition and padding so the centre keeps landing on an
   exploration Canvas (D-8, D-10).
2. **`insights_summary`'s four cells each carry `contentDescription = "$label, $value"`** on the
   column, and `populatedSummaryAndEpisodesExposeLabeledSemantics` matches `listOf("Episodes, 1")`
   exactly. The uppercased label is restored to its original case by `MsUppercaseText`, so the
   description composed from the original string is unchanged.
3. **Every histogram cell keeps `semantics(mergeDescendants = true)` with `role = Role.Button`,
   `selected`, and its exact composed `contentDescription`.** Three tests assert those strings
   character for character.
4. **The three live readouts keep `liveRegion = LiveRegionMode.Polite`** on the node that carries
   their tag; `sleepCountsExposeTwoSelectableAccessibleCellsAndLiveReadout` asserts it directly.
5. **Every visible string reached by `onNodeWithText` stays reachable**, including the two caveats
   asserted by constant (`ONSET_GAP_CAVEAT`, `ONSET_TIME_CAVEAT`, `SLEEP_COUNTS_CAVEAT`), the
   refusal sentences, the readout sentences, `"Nothing to draw yet"`, `"Each episode"`,
   `"No ratings in this range"`, `"Retry"`, and the substring assertions `"work"`,
   `"event: dose change"` and `"06:00 Z"`.

### D-16 — 200% font is an oracle, not an aspiration

Phase 15's and Phase 16's own defects were found by installed-app capture at 200% font and by a
geometry assertion, not by reasoning. The four tightest elements here are the summary strip's four
columns, the range-chip row, the histogram cells' three stacked texts, and the raster's date
column.

- The **summary strip** is asserted equal-width at 100% and 200% (D-9). Its labels wrap; no cell
  takes a fixed height.
- The **range-chip row** scrolls horizontally, so growth moves the sixth chip rather than clipping
  the first.
- The **histogram cells** keep a fixed width and a `heightIn(min = …)`, never a fixed height, so
  the count and label grow downward.
- The **raster date column** keeps its 44 dp width and its `labelSmall` label; at 200% a `MMM d`
  label wraps within the column rather than pushing the Canvas.

No text container on the screen takes a fixed height. Every size is `sp` and every tracking is
`em`, per D-10 of the foundation.

### D-17 — `MsSpacing` conversion, and the documented one-offs

D-14 of the foundation converts literals in the phase that already restyles the screen. All 93
`.dp` literals in `InsightsScreen.kt` are converted to `MsSpacing` references or to named private
constants carrying a comment, so Phase 18's closing audit finds no undocumented literal.

The scale gains nothing. The one-offs are chart geometry that a spacing scale should not absorb,
and each is a named private constant in the screen that owns it: the four raster row heights
(20/10/6/4 dp), the 44 dp raster date column, the 180 dp chart plot height, the 24 dp chart axis
column, the 30 dp tick-row inset, the 24 dp event-snap radius, the 14 dp stripe pitch, the 16 × 9
and 18 × 10 dp legend swatches, the 72/64 dp histogram cell widths, the 88/80 dp bar wells, the
28/26 dp bar widths, the 152/144/96 dp cell minimum heights, and the 96 dp loading well.

### D-19 — The design has two small-label idioms, and `labelSmall` is only one of them

Added during implementation, because installed-app capture found it and no test would have.

`labelSmall` is 9 sp tracked **0.244 em**, which is D-10's transcription of the design's eyebrow
tracking of 2.2–2.4 px. Insights has a second, quieter small-label idiom the foundation did not
separate out: the raster's row dates (line 358), the legend labels (lines 367 and 374), the chart's
axis and tick labels (line 405) and the histogram bar labels (lines 1461, 1469) are tracked
**0.5–0.6 px**, which at 9 sp is 0.067 em. They are data, not identity.

Painting them at the eyebrow's tracking rendered `Jul 8` as `J u l  8`, `nothing` as
`n o t h i n g` and `recorded intensity` as `r e c o r d e d  i n t e n s i t y`. Nothing was
clipped and no assertion could see it; it was simply wrong, in the way the first capture of a
screen usually is.

A private `chartLabelStyle()` in `InsightsScreen.kt` carries `labelSmall.copy(letterSpacing =
0.067.em)` and is used at all eight sites. The eyebrow tracking stays where it belongs — section
titles, the summary strip's labels, `MsEyebrow`. **This is deliberately not pushed into
`ui/theme/Type.kt`**: Insights is the first screen with enough small data labels to need it, and
widening the shared type scale on one screen's evidence is how a scale acquires values nobody can
justify later. Phase 18's closing audit is the right place to decide whether it becomes a
sixteenth style.

### D-18 — What is deliberately not copied

Recorded so a later phase does not "restore" one of these while chasing fidelity:

| Not copied | Why |
|---|---|
| `Look around with sample data` on the empty state | Would write fabricated user data; D-25 of the foundation |
| The LINE / STEPS chart toggle | Settled by `SPEC-insights-entry-chart.md`; the chart is step-only |
| `rampMin` ramp settings | Never specified; adding it is a feature |
| The raster's `12a / 6a / 12p / 6p / 12a` hour axis | Five visible strings MindScale does not have |
| The design's hover tooltips on the raster, chart and both histograms | Android has no hover; the live readout is the accessible equivalent the app already ships |
| The right-hand `{{ onsetN }}` and `{{ f.n }}` count lines where MindScale has no such string | A copy addition |
| The `Sleep, and what came after` five-hour post-wake comparison | Excluded by `SPEC-insights-sleep-counts.md`; it needs a coverage and cohort contract |
| `One page for your doctor` as the report link's label | MindScale's string is `Clinician summary` and Invariant 3 freezes it |
| The prototype's light ramp direction | Fails `IntensityRampTest`; D-4 |
| `#F0E4CC` and `#3A2F1C` as ramp low anchors | 1.26:1 and 1.38:1 against `card`; D-4 |
| The eyebrow's 2.4 px tracking on a raster date or a bar label | The design tracks those 0.5–0.6 px; D-19 |
| Uppercasing a bucket boundary or a clock hour | The design sets no `text-transform` there; D-3 |
| `rgba(ink,.14–.16)` as a control boundary | Fails the 3:1 non-text floor; D-7 |
| `rgba(gold,.85–.95)` as a data mark | Fails the 3:1 non-text floor; D-7 |
| The legend inside the raster panel | Would move the panel's centre off its only row and break a connected test; D-8 |

## User experience

Nothing about the flow changes. Every screen is reached the same way, every control does the same
thing, and every string is the same string. What changes is that Insights becomes the design's
screen — pill range chips over a hairline-ruled summary strip, a hairline-bordered card holding the
day/hour raster, a gold step chart on a matching card, hairline-separated fact and episode rows,
and gold histogram bars that invert to ink when selected — and that the intensity ramp becomes one
warm interpolation into the brand gold instead of a slate-blue gradient.

Configuration changes, process death, back navigation, and state restoration are untouched: this
phase adds no state and removes none.

## Frozen interfaces and data contracts

Changed:

| File | Change |
|---|---|
| `ui/theme/IntensityRamp.kt` | The D-4 anchors and mapping, and its KDoc |
| `insights/InsightsScreen.kt` | Body restyle, L-3, `MsSpacing` conversion |

Added:

| File | Contents |
|---|---|
| `app/src/test/…/ui/theme/MsIntensityRampContrastTest.kt` | The D-4 ramp tables, computed |
| `app/src/test/…/ui/theme/MsInsightsContrastTest.kt` | The D-7 tables, computed |
| `app/src/androidTest/…/insights/InsightsVisualTest.kt` | L-3, touch targets, selected-state geometry, 200% font |
| `app/src/debug/…/designgallery/InsightsPreviews.kt` | `@Preview` over Insights, light/dark at 100%/200% |

Unchanged, and any diff to them is a defect in this phase: every `ViewModel`, every DAO, every
entity, `InsightsModels.kt`, `EpisodeEngine.kt`, `InsightsViewModel.kt`, `TrackScreen.kt`,
`LogScreen.kt`, `Migrations.kt`, every exported schema JSON, `DataExport.kt`, `BackupImport.kt`,
`ImportPreflight.kt`, `RecordsCsvImport.kt`, `SafetyContent.kt`, `BreathingContent.kt`, `Band.kt`,
`MindScaleApp.kt`, every file under `ui/components/`, every file under `ui/theme/` other than
`IntensityRamp.kt`, the main `AndroidManifest.xml`, `app/build.gradle.kts`,
`gradle/libs.versions.toml`, and every file under `src/test/` and `src/androidTest/` that existed
before this phase.

## Invariants

1. Every one of the 23 pre-existing `testTag` values in `InsightsScreen.kt` still resolves to a
   node with the same role and the same callback.
2. Every content description asserted by the connected suite is unchanged, and each still sits on
   the node that carries the matching click action or live region.
3. Every visible string is byte-identical. Case and size differences on screen are presentation
   only and are absent from semantics.
4. No text is painted below 4.5:1 against the surface behind it.
5. No border, ring, or mark that is the sole boundary of an interactive control, and no graphical
   object the charts exist to draw, is below 3:1 against either adjacent surface.
6. Every intensity `1..10` clears 3:1 against `card` and against `bg` in its own theme.
7. `intensityColor` is monotonically non-decreasing in relative luminance over `0..10` in both
   themes, throws outside `0..10`, and returns distinct values for light and dark at both `0` and
   `10` — the four properties `IntensityRampTest` already asserts, unedited.
8. Colour is never the sole carrier of value information: every `intensityColor` fill is
   accompanied by the value as text, in the legend label or the live readout (Invariant 14).
9. Selection on every histogram and count cell is carried by at least one signal that is not
   colour.
10. No interactive element has a touch target below 48 dp.
11. Insights reflows at 200% font with nothing clipped, and the summary strip's four columns stay
    equal-width at both scales.
12. No gesture modifier, `rememberSaveable`, or `SavedStateHandle` key is added, removed, or moved.
13. No composable in `ui/components/` is changed.
14. No `.dp` or `.sp` literal remains in `InsightsScreen.kt` that is not a token reference or a
    commented one-off.

## Android compatibility

`minSdk` 26, `targetSdk` 36, `compileSdk` 36.1 — unchanged. No new dependency, permission, or
resource. Collapsing the fact and episode lists into one `LazyColumn` item each composes at most
six and eight bounded rows respectively. `MsCard` replaces `Surface` at the same nesting depth and
adds no recomposition scope. Process death, rotation, back navigation, and offline behavior are
unaffected because no state is added or removed.

## Acceptance criteria

- [x] **REGRESSION**: `connectedDebugAndroidTest` passes **239/239** — the 226 baseline plus 13 new
      — with no pre-existing test file modified.
- [x] **REGRESSION**: `test` passes **428/428** — the 411 baseline plus 17 new — with no
      pre-existing test file modified, and `IntensityRampTest`'s five tests pass against the new
      anchors **unedited**, which is the whole of D-4's argument.
- [x] **DIFF**: `git diff --name-status 18ab15d HEAD -- app/src/test app/src/androidTest` shows
      three `A` lines and zero `M` lines. `git diff --check` passes.
- [x] **LINT/BUILD**: `lint` reports 0 errors and the unchanged 22-warning baseline;
      `assembleDebug` passes. One new warning appeared and was fixed rather than accepted — see the
      record below.
- [x] **UNIT**: `MsIntensityRampContrastTest` reproduces D-4's tables to two decimals, asserts the
      prototype's two low anchors fail 3:1 against `card`, `bg` and the asleep fill, asserts the
      prototype's light ramp descends in luminance and would therefore fail `IntensityRampTest`,
      asserts today's dark low anchor fails at 1.87:1, and asserts every adopted value `1..10`
      clears 3:1 against `card` and `bg` in both themes. **Writing it corrected D-4's finding 3**
      — see the record below.
- [x] **UNIT**: the same test asserts `intensityColor(0)` equals `intensityColor(1)` in both themes
      and that intensity 10 is the theme's own `gold`, pinning D-5.
- [x] **UNIT**: `MsInsightsContrastTest` reproduces every figure in D-7's tables, asserts each
      rejected design value fails 3:1, asserts each adopted replacement clears it on both `bg` and
      `card` in both themes, and asserts the exempt values stay deliberately below it. Every figure
      passed on the first run against the Phase 16 tables.
- [x] **INSTRUMENTED**: `InsightsVisualTest` asserts the summary strip's four columns share one
      width at 100% **and** 200% font with even gaps (L-3), and that their four values share one
      top edge.
- [x] **INSTRUMENTED**: `InsightsVisualTest` asserts every range chip, every gap bucket, every
      onset hour and both sleep cells reach 48 dp on both axes, at 100% and at 200% font, and that
      all ten gap buckets share one width and one height.
- [x] **INSTRUMENTED**: `InsightsVisualTest` asserts selecting a histogram cell changes neither its
      size nor its position relative to its neighbours, so the 2 dp selected border is drawn in
      space reserved in every state. Restated honestly: it does **not** assert that the fill
      inverts, because a background colour is not in the semantics tree. That is verified by
      installed-app capture instead.
- [x] **INSTRUMENTED**: `InsightsVisualTest` asserts the raster panel is exactly its padded day
      rows and that a centre click still reaches one — the geometric fact D-8 depends on.
- [x] **INSTRUMENTED**: `InsightsVisualTest` asserts the report link is a centred pill narrower
      than the screen, at a 48 dp height.
- [x] **UI/ACCESSIBILITY**: eight `@Preview` composables over Insights in light and dark at 100%
      and 200% font, plus every cell selected at once, the refusal state, the empty state and the
      stale-snapshot banner, in `src/debug/…/InsightsPreviews.kt`. They compile and are debug-only.
      Stated honestly: they were **not** rendered in the IDE preview pane in this session; the same
      theme and scale combinations were verified by installed-app capture instead, which is the
      stronger oracle and is what found this phase's one defect.
- [x] **MANUAL**: installed-app capture on the API 36 emulator against `docs/design/reference/`:
      Insights top in dark against `dark-insights-top.png`; the raster, legend and entry chart in
      light against `light-insights-raster-entry-chart.png`; Insights scrolled in dark against
      `dark-insights-scrolled.png`. Light top and the dark entry chart are compared against the
      HTML only, because **no such screenshot exists** — stated rather than papered over.
- [x] **MANUAL**: capture of surfaces no screenshot covers — the gap histogram and the onset-hour
      histogram with their bars, a **selected** gap bucket showing the ink fill with its bar and
      label inverted to `onInk`, the sleep-counts refusal panel, and the report link as the
      design's centred ink pill.
- [x] **MANUAL**: Insights at 200% font, top and bottom, with nothing clipped. Emulator font scale,
      night mode, and rotation restored to `1.0`, `no`, and enabled, and the app's data cleared.

Not met, and stated rather than quietly dropped:

- [ ] **MANUAL**: the sleep-count **cells**, the loading state, the range-empty line and the
      stale-snapshot error banner were not captured on the installed app. Each needs state the app
      will not enter on request: two completed Sleep/Wake pairs inside the range, a slow Room read,
      a range with entries outside it, and an injected read failure. What does cover them: the
      connected suite asserts each one's presence, tag, strings and actions; each is built from
      primitives whose rendering *was* captured — `MsCard` by the refusal panel, the cell treatment
      by both histograms, `MsTextAction`'s tone by the report link; and all four have `@Preview`
      composables. That is compositional, not the same as having looked at them.
- [ ] **MANUAL**: the histogram cells at 200% font were not captured — the 200% screenshots caught
      the top of the screen and the bottom, and the histograms sit between them at a scroll
      position a screenshot reaches only by accident. They are covered by an assertion instead:
      `everyHistogramCellStillReachesTheTouchTargetFloorAt200PercentFont`.
- [ ] **REVIEW**: no critical-tier review pass was spent on this phase. The 239 connected tests, the
      428 JVM tests, the mechanical `testTag` and string diffs, and the capture matrix were treated
      as the evidence instead. This is recorded as a gap, not as a claim of equivalence.

## Task decomposition

1. Freeze this spec — oracle: a documentation commit before any application-code edit.
2. `MsIntensityRampContrastTest` and `MsInsightsContrastTest`, then `IntensityRamp.kt` — oracle:
   `.\gradlew.bat test`, with `IntensityRampTest` green and unedited.
3. The raster: panel, state fills, legend, title and readout row — oracle:
   `InsightsScreenTest`'s raster tests, specifically the centre-click one.
4. The entry chart: panel, marks, axis, legend, caveat — oracle: `InsightsScreenTest`'s two chart
   tests.
5. The summary strip and L-3, the range chips, the banners and the empty states — oracle:
   `InsightsVisualTest`'s L-3 assertions plus `everyRangeIsReachableSelectedAndInvokesCallback`.
6. The fact and episode cards, the three histogram sections, the report link, `MsSpacing` —
   oracle: the full `InsightsScreenTest` and `NavigationTest`.
7. Previews, then full verification and installed-app capture — oracle: all four Gradle oracles
   plus the manual matrix.

## Implementation and verification record

Completed 2026-08-06 on `agent/phase17-insights-visual`.

**Measurement corrected the spec before any screen code depended on it, which is the reason the
tests are written before the restyle.** D-4's finding 3 originally recorded the design's raw light
pair as separating its endpoints at 2.24:1, and argued from that that obeying `IntensityRampTest`
cost nothing. `MsIntensityRampContrastTest` measured 5.77:1 — the design's raw pair really is far
wider. The finding survives, but the argument had to be rebuilt rather than patched: the design
buys that width by putting one end at 1.26:1 against `card`, and raising that end to the palest
compliant point on its own line collapses it to 2.24:1, *below* the adopted ramp's 2.31:1. So most
of the width is lost to the 3:1 floor whichever direction is chosen, and the direction the
pre-existing test allows costs nothing beyond direction. The table and the surrounding prose now
say that.

**Two findings came out of measuring that the brief did not anticipate.** `SPEC-visual-foundation.md`
D-24 flagged the light low anchor `#F0E4CC`; the dark one fails as well, at 1.38:1 against `card`
and 1.22:1 against the asleep band. And the ramp MindScale has shipped since Phase 1 fails in dark
too, at 1.87:1 — a pre-existing defect this phase fixes rather than a regression it avoids.

**One defect was found by building and looking rather than by reasoning, and it is the one no test
could have caught.** Every small label on the screen was painted at `labelSmall`'s 0.244 em, which
is D-10's transcription of the design's *eyebrow* tracking. The design has a second small-label
idiom at 0.5–0.6 px for data — raster dates, legend labels, axis ticks, bar labels — and at the
eyebrow's tracking `Jul 8` rendered as `J u l  8` and `nothing` as `n o t h i n g`. Nothing was
clipped, every assertion passed, and it was simply wrong. Fixed by a private `chartLabelStyle()`
and recorded as D-19. The same capture showed the histogram bar labels reading `<1D` and `12A`,
because they had been routed through `MsUppercaseText` when the design uppercases neither; both are
plain `Text` now, and D-3 names them.

**Two test-authoring mistakes are worth recording because they are easy to repeat.**
`InsightsVisualTest`'s first run reported gap bucket 4 as 156 px wide against a 189 px reference:
`boundsInRoot` is **clipped** by the horizontal scroller the buckets live in, so measuring equal
widths from it asserts "every visible sliver is the same size", which is not the claim. The test
uses `getUnclippedBoundsInRoot()`. Separately, the selection test called `setContent` twice in one
test, which the Compose rule rejects; it drives a `mutableStateOf` instead, exactly as
`TrackVisualTest.armingThePadDoesNotMoveOrResizeAnyKey` does.

**One new lint warning appeared and was fixed rather than accepted.** The visual test's shared
`@Composable` helper was named `screen`, which trips `ComposableNaming`. Renamed
`InsightsUnderTest`; lint is back at the unchanged 22-warning baseline.

**One constraint was found by reading the connected suite before touching layout, and it changed
the design.** The prototype puts the raster's legend *inside* the raster panel.
`InsightsScreenTest.rasterTouchAndAccessibilityActionUseOneExplorationSurface` clicks that panel's
**centre** and requires the click to reach a day row; on the one-day snapshot it builds, the panel
is exactly its 10 dp padding around one 20 dp row. A legend inside would have pushed the centre
about 38 dp down, past the only row there is. The legend stays below the panel, and
`InsightsVisualTest` now pins both the panel's height and the centre click so a later phase cannot
undo the reasoning by accident.

Accepted consequences, stated rather than discovered later:

- **On a light page a rating of 1 now carries more visual weight than a rating of 10**, because
  `IntensityRampTest` pins the light ramp's luminance direction and the design's direction is the
  other one. This is the phase's one place where a frozen constraint and the design point opposite
  ways, and it is the item flagged in "Open questions" rather than buried here.
- **At 200% font the summary strip's labels wrap to as many as four lines** — `TYPICAL LENGTH`
  becomes `TYPICA / L / LENGT / H`. That is the direct cost of L-3's equal-weight columns, and the
  alternative is the prototype's flaw. Nothing clips, and the columns stay equal, which is what
  D-9 asserts.
- **The Episodes and Each-episode lists each compose as one `LazyColumn` item now**, because the
  design draws them as one card of hairline-separated rows. Both are bounded by the engine — six
  facts and eight episodes — so no laziness that matters was lost.
- **The entry chart's sleep bands lost their diagonal hatching**, adopting the design's solid
  column. It is redundant reinforcement: the step line genuinely stops across a sleep span. The
  raster's FUTURE hatching is *not* removed, because there it is the only thing distinguishing
  future from no-data.

Honest gaps beyond the unmet criteria above: spoken TalkBack output was not audited, only the
semantics tree; the selected cell's inverted fill and every border colour are verified by capture
rather than by an assertion, because neither is in the semantics tree; and the raster's light-theme
`no data` band measures 1.06:1 against the card, so on a page with no records the panel reads as
almost empty — that is the design's own intent and the legend names the state, but it is faint.

## Rollout, migration, and rollback

No migration. Room stays at schema 7; the JSON backup stays at version 7; the records CSV header
stays byte-identical. Nothing this phase writes is persisted, so rollback is `git revert` of the
phase's commits with no data consequence and no user-visible state to reconcile.

## Open questions and approval gates

None blocking. One point is **flagged rather than left silent**, and it is the phase's only place
where a frozen constraint and the design authority point in opposite directions:

**D-4's light ramp runs the opposite way from the prototype's, because `IntensityRampTest` — a
pre-existing JVM test the current instruction forbids editing — asserts the light ramp's luminance
is monotonically non-decreasing, and the prototype's light ramp descends.** The cost is measured
and small: the adopted pair separates its endpoints at 2.31 against the design's 2.24, and both use
the design's own hexes. What it cannot recover is the *direction*: on a light page a rating of 1
now carries more visual weight than a rating of 10.

If that trade is not wanted, the only alternative is to authorize amending `IntensityRampTest` so
the light ramp may descend, and to raise the design's light low anchor from `#F0E4CC` to `#A28C65`
so intensity 1 clears 3:1. That would be one `M` line in the diff the acceptance criteria require
to be all `A`, so it is not taken unilaterally. It is recorded here and in the completion report so
the reader sees the reconciliation rather than discovering it in a diff.
