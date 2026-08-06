# SPEC-visual-foundation: brand token foundation and app chrome

Status: FROZEN — APPROVED

Owner: Claude Code (Phase 15 of 18), on the user's instruction of 2026-08-05

Date: 2026-08-05

Last verified commit: N/A

## Purpose

MindScale was built from a Claude Design prototype but only ever adopted its behavior, never its
visual identity. This phase installs the identity as a shared foundation — palette, type, shape,
emphasis, spacing, elevation, and a component layer — and applies it to the app chrome, so that
Phases 16 through 18 can dress each screen from one source instead of drifting again.

This phase changes how the app looks. It changes nothing about how it works.

## Sources reconciled

Authority order for every hex, size, tracking, radius, weight, and shadow, highest first:

1. `C:\Users\mckie\Downloads\MindScale-handoff\mindscale\project\MindScale v2.dc.html` — the
   literal `:root` custom-property block at line 14, the `.ms-dark` override at line 15, and the
   inline `style=""` strings throughout. Verified current against live Claude Design project
   `1c630a7b-57ce-4bf0-81b7-9b6716ca7343`.
2. `docs/design/reference/` — the fourteen rendered screenshots, moved into the repository as the
   first task of this phase precisely because the design had never lived here, which is part of
   why the implementation drifted.
3. The prototype's own layout, which is advisory only. See D-22.

`SPEC-full-log.md` D-9 authorized this work by deferring "the shared gold/ink light/dark token
foundation until the later global brand phase, when it can be applied consistently to every
screen. Do not implement a one-screen-only pseudo-theme." That phase is this one.

Screenshot coverage is uneven and is recorded honestly rather than papered over. Dark covers
Track (top, scrolled), Full Log, Insights (top, scrolled), and Settings (top, middle, bottom).
Light covers Track (top, scrolled), Insights raster plus entry chart, Breathing, and Settings
(top, bottom). There is no light Full Log and no dark Breathing. Report, Profile, Safety,
dialogs, the toast, the armed numpad, every empty state, and the gap / onset-time / sleep-count
histograms appear in no screenshot and are derived from the HTML alone.

### Measured starting state

Taken from the tree at `009f33455a8f79263f5a17de16d00f57ebec45d7`, so the acceptance criteria
below are checkable rather than impressionistic:

- 13 of the Material 3 colour roles are set; the rest fall back to the Material defaults.
- 1 of the 15 Material 3 text styles is overridden (`bodyLarge`), and it names
  `FontFamily.Default`, which resolves to Roboto.
- `MaterialTheme` is called with `colorScheme` and `typography` only. `shapes` is never passed.
- 246 hardcoded dimension literals in `app/src/main/java` — 237 `.dp` and 9 `.sp`.
- 158 `Modifier.testTag(...)` call sites in `app/src/main/java`.
- 23 connected test files. 8 of them make 111 `onNodeWithText(...)` assertions, 0
  `onAllNodesWithText(...)`, and 1 `hasText(...)`. Across all 23 there are 286
  `onNodeWithTag(...)` and 4 `onNodeWithContentDescription(...)` assertions.
- 9 `AlertDialog(` call sites across Log, Profile, Safety (2), Track (3), and Settings (2).
- One `ToastBanner` on Track. No `Snackbar` anywhere.
- `androidx.compose.ui.tooling.preview` is already an `implementation` dependency and
  `androidx.compose.ui.tooling` is already a `debugImplementation` dependency. Both are unused.
- Baselines to hold: 370/370 JVM tests, 187/187 connected tests, lint 0 errors at the unchanged
  22-warning baseline, `assembleDebug`.

## Scope

This phase delivers, and nothing else:

- The complete token layer: colour, typography, shape, emphasis, spacing, elevation.
- Instrument Sans bundled into `res/font` as an OFL variable font.
- A shared component layer with previews.
- The app chrome: window background, header, bottom navigation, the shared dialog container,
  and the full-bleed Breathing treatment.
- A debug-only design gallery.

## Non-goals

- Any behavior change whatsoever. See D-1, which is the governing rule of this phase and of
  Phases 16 through 18.
- Per-screen layout work on Track, Full Log, Insights, Settings, Profile, Report, Safety, or the
  Breathing body. Those are Phases 16 through 18.
- Changing `intensityColor`. Deferred to Phase 17 by D-24.
- Retiring all 246 hardcoded dimension literals in this phase. See D-14.
- Any dependency, permission, toolchain, Gradle plugin, manifest permission, Room schema,
  backup, CSV, export, import, or migration change.
- Any network access at runtime, including Downloadable Fonts.
- Dynamic colour. It was removed in Phase 4 and stays removed.
- Localization of the uppercase idiom beyond `Locale.ROOT`. See D-11.

## Decisions

### D-1 — The visual-only rule, as a testable contract

This phase, and Phases 16 through 18, may change only how pixels are painted. Specifically:

No feature is added, removed, renamed, reordered, or rewired. No navigation change, no new entry
point, no control that was not there, no control taken away. No copy edits. No data, storage,
export, or migration change. No dependency, permission, or toolchain change.

The proof is objective: **187/187 connected tests and 370/370 JVM tests must pass unchanged**. A
test that breaks is evidence that behavior changed, and that is a defect in this phase, not a
test to update. No test file may be edited to accommodate a visual change. New tests may be
added; existing assertions may not be relaxed, retargeted, or deleted.

The prototype shows several controls MindScale deliberately does not have — a LINE/STEPS toggle,
ramp settings (`rampMin`), Face ID (`faceId`), sample data, and time-weighted Log day headers.
Each was considered and settled in an earlier spec. None is added back while copying the look.
Equally, nothing the app has is removed because a screenshot does not show it.

The prototype's initials avatar is out by the same rule, without needing a judgment call: it is a
different Profile entry point from the one the app has, and swapping entry points is a
navigation change. The existing `profile_action` text control stays exactly as it is, restyled.

### D-2 — Fidelity: faithful, natively resolved

Copy the elegance, colour, and theme with care. Do not copy the prototype's layout flaws, and do
not copy a value that fails an accessibility floor.

Where the prototype fails contrast, the 48 dp touch target, or 200% font, keep the visual intent
at a compliant value and record the divergence as a numbered decision. Every such divergence in
this phase is D-6, D-7, D-8, D-10, and D-23. There are no unrecorded ones.

Preserved exactly, with no divergence: the palette hues, Instrument Sans, the weight-500 label
idiom, the uppercase letter-spaced label idiom, the 14 dp card radius, 999 dp pills, circular
numpad keys, near-flat elevation, the gold armed-numpad ring, the ink toast pill, and a text-only
bottom navigation with no icons.

### D-3 — Four phases, and exactly what Phase 15 owns

- **P15 — tokens and chrome.** This spec.
- **P16 — Track and Full Log.**
- **P17 — Insights and the intensity ramp.**
- **P18 — Settings, Profile, Report, Safety, Breathing, and the closing audit.**

Phase 15 owns the token layer, the component layer, and the app shell. It does not restyle screen
bodies. Note the consequence and do not mistake it for scope creep: the moment `MaterialTheme`
carries the real palette, type, and shapes, **every screen changes appearance**, because every
screen already reads `MaterialTheme.colorScheme` and `MaterialTheme.typography`. That is what a
token foundation is. What Phases 16 through 18 add is the bespoke per-screen geometry the tokens
alone cannot express.

### D-4 — The design lives in the repository

The fourteen Claude Design screenshots move to `docs/design/reference/` under stable, meaningful
names. They are the visual regression baseline for every phase, and their absence from the
repository is a named cause of the drift this phase exists to correct.

| File | Theme | Screen |
|---|---|---|
| `dark-track-top.png` | dark | Track, top |
| `dark-track-scrolled.png` | dark | Track, scrolled |
| `dark-full-log.png` | dark | Full Log |
| `dark-insights-top.png` | dark | Insights, top |
| `dark-insights-scrolled.png` | dark | Insights, scrolled |
| `dark-settings-top.png` | dark | Settings, top |
| `dark-settings-middle.png` | dark | Settings, middle |
| `dark-settings-bottom.png` | dark | Settings, bottom |
| `light-track-top.png` | light | Track, top |
| `light-track-scrolled.png` | light | Track, scrolled |
| `light-insights-raster-entry-chart.png` | light | Insights raster and entry chart |
| `light-breathing.png` | light | Breathing |
| `light-settings-top.png` | light | Settings, top |
| `light-settings-bottom.png` | light | Settings, bottom |

### D-5 — The palette, taken literally from the prototype

From `MindScale v2.dc.html` lines 14 and 15, verbatim:

| Token | Light | Dark |
|---|---|---|
| `bg` | `#FCFBF9` | `#100E0B` |
| `card` | `#FFFFFF` | `#191612` |
| `ink` | `#17130C` | `#F4F0E8` |
| `onInk` | `#E3CE9F` | `#2A2114` |
| `gold` | `#AE8C4F` | `#C9A96A` |

Light and dark are two different treatments, not one palette flipped. `onInk` is the clearest
evidence: warm bone `#E3CE9F` on light, warm near-black `#2A2114` on dark. Selected segments and
chips invert to an ink fill with `onInk` text in both themes, and the two fills are not each
other's inverse.

Two further literals from the design, adopted as tokens:

| Token | Light | Dark | Source |
|---|---|---|---|
| `sleepBand` | `#DEDAD2` | `#24211C` | line 1326, `sleepC` |
| `crosshair` | `#BE9C5C` | `#BE9C5C` | line 405, chart crosshair stroke |

**`--canvas: #E9E6DF` is deliberately not adopted.** It is the HTML page behind the simulated iOS
device frame, not an app surface. Adopting it would put a colour on screen that the app never
shows. This is recorded because it is an easy and invisible mistake to make from the CSS alone.

### D-6 — A calibrated emphasis scale replaces the prototype's alphas

The prototype expresses its whole hierarchy as `rgba(var(--ink-rgb), a)` across 30 distinct
alpha values from `.03` to `.62`, concentrated at `.35`, `.38`, `.40`, and `.45`. Measured, ink
`#17130C` at `.45` over `#FCFBF9` is **2.99:1** — a WCAG AA failure at any text size, and it is
applied to 9.5 px text. `SPEC-safety-card.md` D-13 and `SPEC-paced-breathing.md` D-14 already
rejected these alphas on their own screens; this decision generalizes that rejection.

Four text emphasis levels replace the thirty. They preserve the prototype's visible ordering at
values that pass AA for normal text on **every** surface in the scheme, not merely on `bg` and
`card`:

| Level | Alpha | Light on bg | Light on card | Dark on bg | Dark on card | Replaces |
|---|---|---|---|---|---|---|
| `primary` | 1.00 | 17.89 | 18.50 | 16.96 | 15.86 | full ink |
| `secondary` | 0.80 | 9.80 | 10.00 | 10.95 | 10.38 | `.55`–`.62` |
| `tertiary` | 0.70 | 6.85 | 6.97 | 8.60 | 8.25 | `.45`–`.52` |
| `quaternary` | 0.60 | 4.77 | 4.85 | 6.53 | 6.34 | `.30`–`.42` |

Ratios are sRGB WCAG 2.x relative-luminance ratios of the alpha-composited foreground against the
stated backdrop, rounded to two decimals, composited in floating point as the compositor does.
The true floor of the scale is `quaternary` on the *dimmest* surface in each theme:
**4.58:1** on light `surfaceContainerHighest` and **5.94:1** on dark `surfaceContainerHighest`.
`MindScaleContrastTest` asserts every level against every surface, not just the two tabulated
here, so a new container step cannot quietly drop a level below the floor.

Non-text ink alphas are a separate scale and are **kept verbatim from the prototype**, because
WCAG imposes no minimum on decorative separators:

| Token | Alpha | Use |
|---|---|---|
| `hairline` | 0.09 | dividers, card borders, the nav top border |
| `hairlineFaint` | 0.07 | settings row separators |
| `outlineDecorative` | 0.16 | inert field underlines, faint control outlines |

An ink border that is the *only* boundary of an interactive control is not decorative and is
governed by D-23, not by this table.

### D-7 — Light-theme gold text diverges; the gold hue does not

The prototype's gold text action colour is `--gold-deep: #9A7B44`, measured **3.84:1** on `bg`.
Gold text appears at 9.5–10.5 px, which is normal text under WCAG, requiring 4.5:1. The colour
fails.

The divergence is confined to text. `#AE8C4F` remains the light-theme gold for every non-text
use — the header rule, the armed-pad ring, chip and card borders — where the applicable floor is
3:1 or none, and where `#AE8C4F` measures 3.15:1 against `card`.

| Token | Light | Dark | Light ratio (bg / card / dimmest) | Dark ratio (bg / card / dimmest) |
|---|---|---|---|---|
| `goldText` | `#7D6539` | `#C9A96A` | 5.35 / 5.54 / 4.70 | 8.60 / 8.04 / 7.15 |
| `gold` (non-text) | `#AE8C4F` | `#C9A96A` | 3.05 / 3.15 / — | 8.60 / 8.04 / — |

`#7D6539` is `#AE8C4F` scaled to 72% luminance along the same hue: the lightest value on that
ramp that clears 4.5:1 on *every* surface in the light scheme. The calibration first reached
`#886D3E` at 78%, which passes on `bg` (4.72) and `card` (4.88) and then drops to 4.14 on
`surfaceContainerHighest`. That candidate is rejected and the rejection is pinned by a test: a
token safe on only two of five surfaces is a token that will eventually be painted on the third.

The cost is recorded honestly — `#7D6539` is visibly darker than the design's `#9A7B44`. What
survives is the hue and the idiom; what changes is the value. The *non-text* gold is unchanged
at `#AE8C4F`, so every rule, ring, chip border, and armed-pad ring on screen is still the
design's exact gold. Only small text is darkened.

The dark theme needs no divergence: `#C9A96A` already measures 8.60:1.

### D-8 — Danger colour: light kept, dark introduced

The prototype uses `#9C3B2E` for destructive affordances in both themes. On light it measures
6.60:1 on `bg` and is adopted unchanged. On dark it measures **2.83:1** and fails.

The prototype simply has no dark-theme danger colour; this is an omission in the source, not a
value to copy. `danger` dark is `#D17466`, measuring 5.87:1 on `bg` and 5.50:1 on `card`.

### D-9 — Every Material 3 colour role is set explicitly

Thirteen roles set and the rest defaulted is how a half-populated scheme leaks Material's purple
into a warm bone-and-gold app. Every role the installed Material 3 `ColorScheme` exposes is
assigned a MindScale token. This is enforced by a reflective JVM test, not by review — and the
test earned its place immediately: the installed Material 3 exposes **47** roles, not the ~35 a
hand-written list would have covered, and the twelve `…Fixed` roles
(`primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`, `onPrimaryFixedVariant`, and the secondary
and tertiary equivalents) were still holding Material's purple and pink after the first pass.

The mapping that carries meaning:

- `background`, `surface` → `bg`.
- **The card is not a Material role.** Material's ladder assumes containers step monotonically
  away from the background; MindScale's card is *lighter* than the page in light and *darker*
  than the brightest container in dark, so it lands on `surfaceContainerLowest` in one theme and
  `surfaceContainer` in the other. Reading a role whose meaning changes between themes is the
  drift this phase exists to end, so MindScale surfaces read `MaterialTheme.ms.card` and the
  Material ladder is populated separately and coherently for any stray Material component.
- `primary` → `goldText`; `onPrimary` → `bg` in both themes (4.72:1 light, 8.60:1 dark). The
  warm `onInk` was considered for light `onPrimary` and rejected at 3.16:1.
- The twelve `…Fixed` roles hold one value across both themes by definition. MindScale uses none
  of them; they are assigned warm neutrals purely so no role is left purple. `onFixed` and
  `onFixedVariant` are both full ink because ink at 70% over `#AE8C4F` measures 3.68:1.
- `outlineVariant` → ink at `hairline`; `outline` → ink at the D-23 control-boundary alpha.
- `error` → `danger`.
- **`inverseSurface` → `ink` and `inverseOnSurface` → `onInk`.** This is the exact Material
  expression of the design's selected-segment and selected-chip treatment, and using the role
  rather than a bespoke pair is what keeps light and dark from collapsing into one flipped
  palette.
- **`surfaceTint` → `Color.Transparent`.** Material's tonal-elevation overlay would tint every
  raised surface toward `primary`. The design is near-flat warm neutrals. Killing the tint at the
  token layer is what makes D-13 hold without auditing every call site.

Dynamic colour stays off, as decided in Phase 4.

### D-10 — Typography: Instrument Sans, bundled, at the design's sizes

Three OFL-licensed **static** Instrument Sans instances are bundled into `res/font`:

| File | Weight | Bytes | SHA-256 |
|---|---|---|---|
| `instrument_sans_regular.ttf` | 400 | 86,232 | `69FD3F7C467C70C1F73B232812407F688F3D87DD7A801EA7281AA97D29CF53D5` |
| `instrument_sans_medium.ttf` | 500 | 86,924 | `56BA599D12B7CF2FFF4EEBF46D29253231B6F49BC5B6CE7733DFBF3D7940BFAE` |
| `instrument_sans_semibold.ttf` | 600 | 87,004 | `7151CF505F897E17B4E9B956293B5A60046EC39DA3923A8FEBA29CE86FD14E12` |

Obtained from the upstream `Instrument/instrument-sans` project at `fonts/ttf`, which is what
`google/fonts` packages; the two projects' `OFL.txt` are byte-identical, and it ships as
`docs/design/InstrumentSans-OFL.txt`.

**Static instances, not the variable font, and `minSdk` is the reason.** The first
implementation bundled the single `InstrumentSans[wdth,wght].ttf` and pinned each weight with
`android:fontVariationSettings`. The platform honours that attribute from **API 28**. MindScale's
minimum is 26, so on API 26 and 27 all three families would have resolved to the variable font's
default instance and the whole weight hierarchy — the 500 identity, the 400 prose, the 600
selected tab — would have silently collapsed into a single weight. Lint surfaced it as an
`UnusedAttribute` warning; the collapse is what actually mattered. Three files cost 260 KB
against the variable font's 194 KB, which is the right trade for a brand that is defined by its
weight.

The Compose `Font(resId, weight, style, variationSettings)` overload was also considered and
rejected: it is annotated experimental, and it carries the same API 28 floor.

There is no new Gradle dependency and no runtime network access — Downloadable Fonts is
explicitly rejected because it would introduce exactly the network dependency MindScale does not
have.

**Unit conversion.** The prototype renders in a 402 CSS-px-wide iOS frame, where 1 CSS px is one
iOS point, which is the same physical construct as one Android dp. The rule is therefore
`1 CSS px = 1 dp`, and `1 CSS px = 1 sp` for type, rounded to the nearest 0.5.

**Weight.** The identity is weight 500, and it is the weight of every label, title, action, and
numeral — 108 of the 113 explicit weights in the source. Body paragraphs specify no weight and
therefore render at 400. The numpad glyph is explicitly 400. Two elements are 600: the selected
bottom-navigation tab and the avatar initials. The app has no avatar (D-1), so 600 survives only
on the selected tab.

**Tracking.** CSS `letter-spacing` is absolute px. Compose `letterSpacing` is expressed in **em**
instead, so tracking scales with the user's font-size setting rather than staying fixed while the
glyphs grow. Each em value is the source px divided by the source font size.

**Numerals.** Every style carrying a number sets `fontFeatureSettings = "tnum"`, matching the
prototype's `font-variant-numeric: tabular-nums`.

The Material 3 slots, all fifteen:

| Slot | Size | Weight | Tracking | Line height | Design origin |
|---|---|---|---|---|---|
| `displayLarge` | 26 sp | 500 | 0 | 1.1 | Track readout number |
| `displayMedium` | 24 sp | 500 | 0 | 1.0 | dialog value |
| `displaySmall` | 21 sp | 400 | 0 | 1.0 | numpad key glyph |
| `headlineLarge` | 19 sp | 500 | 0 | 1.2 | large figures |
| `headlineMedium` | 17 sp | 500 | 0 | 1.2 | section figures |
| `headlineSmall` | 16 sp | 500 | 0 | 1.2 | entry-row dot |
| `titleLarge` | 15 sp | 500 | 0 | 1.3 | screen headings |
| `titleMedium` | 13.5 sp | 500 | 0 | 1.4 | card headings |
| `titleSmall` | 13 sp | 500 | 0 | 1.4 | settings row label |
| `bodyLarge` | 13.5 sp | 400 | 0 | 1.6 | primary paragraph |
| `bodyMedium` | 13 sp | 400 | 0 | 1.6 | secondary paragraph |
| `bodySmall` | 12.5 sp | 400 | 0 | 1.65 | hint paragraph |
| `labelLarge` | 10.5 sp | 500 | 0.229 em | 1.3 | pill and segment labels |
| `labelMedium` | 9.5 sp | 500 | 0.189 em | 1.3 | bare gold text actions |
| `labelSmall` | 9 sp | 500 | 0.244 em | 1.3 | eyebrow labels |

Two styles have no Material slot and live on a `MindScaleTypography` object: `wordmark`
(12 sp / 500 / 0.542 em, the header) and `toast` (11.5 sp / 500 / 0.122 em).

**Recorded divergence.** The 9–10.5 sp label tier is smaller than common Android practice. It is
kept because it is the design's deliberate idiom and because WCAG sets no minimum font size. It
is made safe rather than merely small: every label in that tier is `sp` and scales with the
user's font setting, is painted at a D-6 level measuring at least 4.77:1, and is never the sole
carrier of information. The 200% font oracle is what proves it reflows without clipping.

### D-11 — Uppercase is presentation, never semantics

The design uppercases nearly every label. Uppercasing at the call site would change the semantics
text of 111 `onNodeWithText` assertions across 8 connected test files, and breaking them would
mean behavior changed, which D-1 forbids. It is also a real accessibility defect: all-caps text
is read letter by letter by some TalkBack configurations.

Text is therefore rendered uppercase and *described* in its original case, through one shared
composable and nowhere else:

```kotlin
@Composable
fun MsUppercaseText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
)
```

It renders `text.uppercase(Locale.ROOT)` and restores the original with
`Modifier.clearAndSetSemantics { this.text = AnnotatedString(text) }`.

`clearAndSetSemantics` is specified rather than `semantics`, because the `Text` semantics key
merges by appending: a plain `semantics { text = ... }` block leaves both the uppercased and the
original string on the node, which happens to satisfy `onNodeWithText` but leaves a node that
reads its own label twice. `clearAndSetSemantics` replaces the leaf's configuration outright, and
a merging ancestor — a clickable navigation tab, for instance — still collects the restored
original.

`Locale.ROOT` is specified, not the default locale, so a Turkish or Azeri device cannot turn an
`i` into a dotted capital and silently change the semantics of an assertion.

**This technique is validated before anything depends on it.** Task 2 below builds it, proves it
against a real connected assertion of the exact shape the suite uses, and only then is it applied.
If it does not hold, the phase stops and the finding is escalated rather than worked around by
editing a test.

### D-12 — Shapes, passed to `MaterialTheme` for the first time

| Token | Radius | Design origin |
|---|---|---|
| `extraSmall` | 2 dp | histogram bars, chart marks (7 uses) |
| `small` | 9 dp | small inset panels (3 uses) |
| `medium` | 14 dp | **the card radius** (19 uses, the dominant shape) |
| `large` | 16 dp | dialog and emphasized card (2 uses) |
| `extraLarge` | 24 dp | the numpad wrapper |
| `pill` | 999 dp | pills, chips, segments, toggles (15 uses) |
| `circle` | 50% | numpad keys, entry-row dots, the breathing circle (10 uses) |

`pill` and `circle` have no Material slot and live on a `MindScaleShapes` object. A 10 dp and a
12 dp radius each appear once or twice in the source and are folded into `small` and `medium`
respectively; the difference is not visible at these sizes and a seven-value shape scale is worth
more than an exact transcription of one-off values.

### D-13 — Elevation is near-flat, and separation is carried by hairlines

The entire prototype contains five distinct shadows, of which the common one is
`0 1px 2px rgba(ink, .03)` — visually almost nothing. Cards are separated from the background by
a 1 px hairline border at ink `.09`, not by a shadow.

Therefore: no MindScale surface sets `tonalElevation` or `shadowElevation` above 0 dp except the
two cases the design actually shadows — the dialog container (`0 6px 18px rgba(ink, .22)`
→ 6 dp shadow elevation) and the armed numpad ring, which is a 4 dp gold spread, not a shadow,
and belongs to Phase 16. `surfaceTint = Color.Transparent` from D-9 is what prevents Material
from reintroducing a tonal overlay behind this decision's back.

The current top bar's `Surface(tonalElevation = 1.dp)` is removed by D-16.

### D-14 — A spacing scale, applied to what this phase touches

`MindScaleSpacing` freezes the scale the design actually uses: 2, 4, 6, 8, 9, 10, 12, 14, 16, 18,
20, 22, 24, 30, and 36 dp.

The 246 hardcoded dimension literals are **not** all retired in this phase. Rewriting the numbers
inside screen bodies this phase is not otherwise touching would be a large, untested diff whose
only purpose is tidiness, and every one of those edits is a chance to silently drop one of the 158
testTags. Literals are converted screen by screen, in the phase that already restyles that screen.
Phase 15 converts only the chrome and the component layer. Phase 18's closing audit asserts that
no `.dp` or `.sp` literal outside `ui/theme` remains that is not either a token reference or a
documented one-off.

### D-15 — A shared component layer that carries no behavior

`ui/components/` gains the primitives every screen needs. Each is a pure presentation composable:
it takes content and callbacks, and it owns no state, no side effect, and no navigation.

```kotlin
@Composable fun MsCard(modifier: Modifier = Modifier, emphasized: Boolean = false, content: @Composable ColumnScope.() -> Unit)
@Composable fun MsEyebrow(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified)
@Composable fun MsTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: MsActionTone = MsActionTone.Gold, enabled: Boolean = true)
@Composable fun MsPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false, enabled: Boolean = true)
@Composable fun MsChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun MsSegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)
@Composable fun MsHairline(modifier: Modifier = Modifier, faint: Boolean = false)
@Composable fun MsToastPill(text: String, modifier: Modifier = Modifier)
@Composable fun MsDialog(onDismissRequest: () -> Unit, title: String, confirm: @Composable () -> Unit, dismiss: (@Composable () -> Unit)? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit)
@Composable fun MsUppercaseText(...)  // D-11
enum class MsActionTone { Gold, Muted, Danger }
```

Two rules bind them:

1. **A component never introduces a control.** `MsSegmentedControl` renders segments a caller
   already had; it does not decide what segments exist. Phases 16 through 18 replace bespoke
   markup with these, one control at a time, preserving each control's `testTag`, semantics, and
   callbacks exactly.
2. **A component never sets a `testTag`.** Tags are the caller's, passed through `modifier`, so
   that swapping in a component cannot drop one of the 158 tags the suite depends on.

`MsPillButton`, `MsChip`, and `MsSegmentedControl` express selection with the D-9 inverse pair —
ink fill, `onInk` text — and additionally with weight, never with colour alone.

### D-16 — The header

The design's header is a three-column grid — a 38 dp leading slot, a centred title with a 22 × 1
dp gold rule beneath it, and a 38 dp trailing slot — flat on `bg`, with the title at 12 sp,
weight 500, uppercase, tracked 0.542 em.

The app's header keeps all three of its existing slots and all of its existing content, because
removing any of it would violate D-1:

- **Leading**: the `overlay_back` control on overlay destinations, the `MindScale` wordmark on
  the three root destinations. Both keep their current visibility rules and callbacks. The back
  control becomes the design's 34 dp circular hairline button.
- **Centre**: the destination title, restyled as the design's wordmark treatment — uppercase via
  `MsUppercaseText`, tracked, with the 22 × 1 dp gold rule beneath. Its strings are untouched,
  including `SafetyCopy.TOP_BAR_TITLE` and `BreathingCopy.TOP_BAR_TITLE`.
- **Trailing**: the `profile_action` control, unchanged in placement, tag, semantics, and
  callback, restyled as a bare gold text action. It does **not** become an initials avatar
  (D-1).

`Surface(tonalElevation = 1.dp)` is removed; the header sits flat on `bg` with no divider, as the
design does.

### D-17 — A text-only bottom navigation

The design's navigation is three flush text tabs on `bg` above a 1 px ink-`.09` top border, with
the selected tab in `goldText` at weight 600 and the rest at the `quaternary` emphasis level.
There are no icons. The app currently uses `NavigationBar`/`NavigationBarItem` with three
decorative glyphs — `●`, `≡`, `▦` — that carry no meaning and no content description.

`NavigationBar` cannot render an item without an icon slot, so the bar becomes a `Row` of three
`Selectable` boxes. This is a rendering change, and it is bounded by exactly what must survive:

- the `main_navigation` and `insights_tab` test tags;
- the `Track tab`, `Log tab`, and `Insights tab` content descriptions;
- the visible strings `Track`, `Log`, and `Insights`, restored in original case by D-11, so that
  `onNodeWithText("Log").performClick()` in `NavigationTest` still resolves to a clickable node;
- the `selected` state and the `setRoot` callbacks;
- the rule that the bar is present only on the three root destinations;
- a minimum 48 dp target height.

Dropping the three glyphs is a removal of on-screen marks, so it is called out rather than
smuggled: they are unlabelled decorations that no test asserts and no content description names,
and the design's navigation is text-only by intent. Every tab keeps its text label and its
content description, so nothing that was announced stops being announced.

### D-18 — Breathing renders full-bleed

The Breathing overlay renders with no top bar and no bottom navigation, as the light-theme
screenshot shows.

This is safe under D-1 only because the exit affordance does not live in the chrome: the screen
already has its own `breathing_close` pill rendering `BreathingCopy.STOP` or
`BreathingCopy.CLOSE`, which is precisely the design's `closeBreathe` control, plus system Back
via the existing `BackHandler`. No route out of the screen is removed.

`BreathingCopy.TOP_BAR_TITLE` would otherwise stop being rendered anywhere. Rather than leave a
frozen string dead, the Breathing root sets it as the accessibility pane title
(`Modifier.semantics { paneTitle = BreathingCopy.TOP_BAR_TITLE }`), so it remains in the
semantics tree and is announced on entry. The constant is unchanged, and
`BreathingContentTest`'s string-equality assertions are unaffected.

### D-19 — Dialogs stay `AlertDialog`; only the container is restyled

All 9 `AlertDialog` call sites keep `AlertDialog`. The scrolling `AlertDialog` is what fixed the
Phase 12 clipping defect at 200% font, and replacing it with the prototype's in-flow inline card
would change structure, focus order, dismissal, and back handling — all behavioral.

`MsDialog` wraps `AlertDialog` and applies the design's container: 16 dp radius, `card`
background, a 1 dp `gold` border, the `0 6px 18px rgba(ink,.22)` shadow from D-13, and the
existing scroll behavior untouched. Titles, body content, button callbacks, dismiss behavior, and
every `testTag` pass through unchanged. `onNodeWithText("Backdate entry")`,
`onNodeWithText("Edit entry")`, and `onNodeWithText("Edit note")` must still resolve.

The action labels need no call-site edit to become the design's bare gold text: Material's
`TextButton` resolves its content colour to `colorScheme.primary`, which D-9 sets to `goldText`,
and D-13 leaves no filled container behind it. **Uppercasing** those labels is a per-call-site
change across 9 dialogs and is deferred to the phase that owns each screen. Phase 15 restyles
the container only.

### D-20 — The toast is an ink pill

`MsToastPill` renders the design's toast: `ink` background, `onInk` text, 999 dp radius, 9 × 20 dp
padding, 11.5 sp weight 500 tracked 0.122 em, uppercase by D-11. Measured 11.98:1 light and
13.93:1 dark.

It is defined and previewed in this phase and applied to Track's existing `ToastBanner` in Phase
16, together with the rest of Track. The `TrackEvent.ToastDismissed` contract, the timing, and
every toast string are untouched.

### D-21 — The design gallery is a debug launcher activity, not an app destination

Verification needs a screen showing every token and component together. Adding an
`AppDestination` for it would add a navigation destination to the shipping app, which D-1
forbids outright.

Instead: `DesignGalleryActivity` lives in `src/debug/` only, so it does not exist in the release
source set, the release manifest, or the release APK. The debug manifest declares it exported
with a `LAUNCHER` intent filter, so a debug install shows a second icon. It touches no
`ViewModel`, no DAO, and no database — it renders tokens and components with literal sample
values. `MainActivity`, `MindScaleApp`, and `AppDestination` are unchanged, so the connected
suite, which launches `MainActivity` explicitly, cannot see it.

`androidx.compose.ui.tooling` is already a `debugImplementation` dependency and
`androidx.compose.ui.tooling.preview` an `implementation` dependency. Both are currently unused.
No Gradle dependency is added.

### D-22 — Layout flaws are corrected, and each correction is recorded

The prototype's layout is advisory. Six known flaws are corrected rather than reproduced. Each
correction is spacing and alignment only: **a layout fix may not change what controls exist, what
they do, or how they are reached.**

| # | Flaw | Correction | Phase |
|---|---|---|---|
| L-1 | Numpad last row leaves an asymmetric hole between `0` and `10` | Centre the final row's two keys on the pad's axis, keeping the 3-column rhythm and both keys' size, tags, and long-press behavior | P16 |
| L-2 | Entry-row EDIT / NOTE / DELETE stack vertically with a ragged edge, cramped against the row | Lay the three actions out on one baseline with even gaps and a consistent gutter from the row content | P16 |
| L-3 | Insights summary strip's four columns have unequal widths, so `TYPICAL LENGTH` breaks the rhythm | Equal-weight the four columns | P17 |
| L-4 | Log's FROM / TO fields jam `ALL` against the right edge | Give `ALL` the same trailing gutter as the fields | P16 |
| L-5 | Settings' DATE / PHQ-8 / GAD-7 / ADD row is crowded at the right | Even the row's gaps and give `ADD` a real trailing gutter | P18 |
| L-6 | Settings segmented controls have unequal segment widths | Equal-weight the segments; already the `MsSegmentedControl` contract in D-15 | P18 |

Phase 15 fixes none of these directly — all six live in screen bodies — but L-6 is prevented at
the component layer, and the remaining five are frozen here so that later phases implement a
decided correction rather than reinventing one.

### D-23 — Accessibility floors, which outrank the prototype

- **Text contrast**: at least 4.5:1 for every text colour over every surface it is painted on.
  Enforced by a computed-contrast JVM test over the D-6 and D-7 tables, not by inspection.
- **Non-text contrast**: at least 3:1 for any border, ring, or mark that is the sole boundary or
  the sole state indicator of an interactive control. `outline` is ink at 0.50 in light (3.49:1
  over `card`) and 0.40 in dark (3.51:1 over `card`). Decorative dividers are exempt and keep the
  prototype's faint alphas (D-6).
- **Never colour alone**: every selected state pairs colour with a second signal — a fill, a
  weight change, or a border. The selected navigation tab carries `goldText` *and* weight 600.
- **Touch targets**: at least 48 dp for every interactive element, regardless of the painted size.
  The design's 26 dp help button, 34 dp header buttons, 42 dp entry dots, 44 dp toggles, ~35 dp
  segments, and zero-padding bare text actions all keep their painted size and gain transparent
  padding to a 48 dp target. The painted geometry is the design's; the target is MindScale's.
- **Font scale**: every screen reflows without clipping at 200%. Every size is `sp`; every
  tracking is `em` (D-10); no text container has a fixed height.

### D-24 — `intensityColor` is deferred to Phase 17, with an explicit re-check

`IntensityRamp.kt` currently lerps slate-blue `#6B7A8F` to gold `#AE8C4F` in light and
`#3A4652` to `#C9A96A` in dark. The prototype's `ramp()` at line 890 is one warm interpolation:
light `rgb(240,228,204)` `#F0E4CC` → `rgb(110,82,32)` `#6E5220`, dark `rgb(58,47,28)` `#3A2F1C`
→ `rgb(224,190,122)` `#E0BE7A`. It also clamps to 1..10 over `(v-1)/9`, whereas `intensityColor`
accepts 0..10 over `v/10` and maps 0 to the low anchor.

This is a colour-mapping change and is in scope for the overhaul, but it is **not** frozen here.
It is decided in the Phase 17 spec, which must resolve the 0-versus-1 low-anchor difference
explicitly and re-check `SPEC-track-numpad-logging.md` Invariant 14 — colour is never the sole
carrier of information — because the light theme's warm low anchor `#F0E4CC` sits very close to
the `card` surface and could make a low rating nearly invisible against it.

Phase 15 does not touch `IntensityRamp.kt`.

### D-25 — What is deliberately not copied

Recorded so a later phase does not "restore" one of these while chasing fidelity:

| Not copied | Why |
|---|---|
| LINE / STEPS chart toggle | Settled by `SPEC-insights-entry-chart.md`; the chart is step-only |
| `rampMin` ramp settings | Never specified; adding it is a feature |
| Face ID toggle | No biometric dependency, no permission, never specified |
| Sample-data seeding | Never specified; would write fabricated user data |
| Time-weighted Log day headers | An inference MindScale does not make |
| Initials avatar | A different Profile entry point (D-1) |
| `--canvas: #E9E6DF` | The page behind the device frame, not an app surface (D-5) |
| Immediate delete without confirmation | `SPEC-full-log.md` D-10 |
| In-flow inline dialogs | `AlertDialog` scroll behavior fixed the Phase 12 clipping defect (D-19) |
| `rgba(ink, .35–.45)` text | Fails AA (D-6) |
| `--gold-deep: #9A7B44` as text | Fails AA in light (D-7) |
| `#9C3B2E` as dark-theme danger | Fails AA in dark (D-8) |

## User experience

Nothing about the flow changes. Every screen is reached the same way, every control does the same
thing, and every string is the same string. What changes is that the app renders in Instrument
Sans on the warm bone-and-gold palette, with 14 dp cards on hairline borders, uppercase tracked
labels, near-flat surfaces, and a text-only bottom navigation.

Configuration changes, process death, back navigation, and state restoration are untouched: this
phase adds no state and removes none.

## Frozen interfaces and data contracts

New files, all under `app/src/main/java/com/kieslingdev/mindscale/ui/`:

| File | Contents |
|---|---|
| `theme/Color.kt` | The D-5, D-6, D-7, D-8 tokens as `Color` values |
| `theme/ColorScheme.kt` | The two complete `ColorScheme`s (D-9) |
| `theme/Type.kt` | `FontFamily`, the 15 Material styles, `MindScaleTypography` (D-10) |
| `theme/Shape.kt` | `Shapes` plus `MindScaleShapes` (D-12) |
| `theme/Emphasis.kt` | The four-level scale and its non-text companions (D-6) |
| `theme/Spacing.kt` | `MindScaleSpacing` (D-14) |
| `theme/Theme.kt` | `MindScaleTheme`, now passing `shapes` |
| `components/*.kt` | The D-15 component layer |

`res/font/instrument_sans_variable.ttf` is added. `docs/design/InstrumentSans-OFL.txt` records
the licence.

Changed: `MindScaleApp.kt` (header, bottom navigation, Breathing full-bleed — D-16, D-17, D-18)
and the 9 `AlertDialog` call sites, which route through `MsDialog` (D-19).

`src/debug/` gains `DesignGalleryActivity.kt` and `AndroidManifest.xml` (D-21).

Unchanged, and any diff to them is a defect in this phase: every `ViewModel`, every DAO, every
entity, `Migrations.kt`, every exported schema JSON, `DataExport.kt`, `BackupImport.kt`,
`ImportPreflight.kt`, `RecordsCsvImport.kt`, `SafetyContent.kt`, `BreathingContent.kt`, the main
`AndroidManifest.xml`, `app/build.gradle.kts` dependency and plugin blocks,
`gradle/libs.versions.toml`, `IntensityRamp.kt`, and every file under `src/test/` and
`src/androidTest/` that existed before this phase.

## Invariants

1. Every one of the 158 pre-existing `testTag` values still resolves to a node with the same
   role and the same callback.
2. Every content description asserted by the connected suite is unchanged.
3. Every visible string is byte-identical to before. Case differences on screen are presentation
   only and are not present in semantics (D-11).
4. No `ColorScheme` role is left at a Material default.
5. No text is painted below 4.5:1 against the surface behind it.
6. No interactive element has a touch target below 48 dp.
7. No composable in `ui/components/` owns state, performs a side effect, navigates, or sets a
   `testTag`.
8. `src/debug/` code is unreachable from `MainActivity` and absent from the release APK.
9. No runtime network access is added; the font is a bundled resource.
10. `MaterialTheme` receives `colorScheme`, `typography`, **and** `shapes`.

## Android compatibility

`minSdk` 26, `targetSdk` 36, `compileSdk` 36.1 — unchanged. `FontVariation` requires API 26,
which the minimum already satisfies. Bundling one 194 KB font adds 194 KB to the APK and no
runtime cost beyond a single font load. The component layer adds no recomposition scope that the
bespoke markup it replaces did not already have; components take stable parameters and hoist no
state. Process death, rotation, back navigation, and offline behavior are unaffected because no
state is added or removed.

## Acceptance criteria

- [ ] **REGRESSION**: `.\gradlew.bat connectedDebugAndroidTest` passes 187/187, with no test file
      modified. Verified by `git diff --stat` showing zero changes under `src/androidTest/` for
      files that existed at `009f334`.
- [ ] **REGRESSION**: `.\gradlew.bat test` passes 370/370 plus the new tests below, with no
      pre-existing test file modified.
- [ ] **LINT/BUILD**: `.\gradlew.bat lint` reports 0 errors and the unchanged 22-warning baseline;
      `.\gradlew.bat assembleDebug` passes.
- [ ] **UNIT**: a reflective test over both `ColorScheme` instances asserts every declared role is
      set to a MindScale token and none is a Material default (D-9, Invariant 4).
- [ ] **UNIT**: a computed WCAG contrast test asserts each of the four D-6 emphasis levels, both
      D-7 gold values, and both D-8 danger values meets at least 4.5:1 over `bg` and over `card`
      in its own theme, and reproduces the ratios tabulated in D-6, D-7, and D-8 to two decimals.
- [ ] **UNIT**: a contrast test asserts `outline` reaches at least 3:1 over `card` in both themes
      (D-23).
- [ ] **UNIT**: a typography test asserts all 15 Material slots are set, none names
      `FontFamily.Default`, and every numeric style sets `tnum` (D-10).
- [ ] **INSTRUMENTED**: a new test proves the D-11 contract — a label rendered through
      `MsUppercaseText` is found by `onNodeWithText` in its **original** case, is not found in its
      uppercased case, and when nested in a merging clickable ancestor is still found and clicked
      by original case. This runs and passes **before** any label is converted.
- [ ] **INSTRUMENTED**: a new test asserts the bottom navigation still exposes `main_navigation`,
      `insights_tab`, the three `… tab` content descriptions, and clickable nodes with text
      `Track`, `Log`, and `Insights`, each at least 48 dp tall (D-17).
- [ ] **INSTRUMENTED**: a new test asserts the Breathing destination shows no `main_navigation`
      and no `overlay_back`, still exposes `breathing_close`, still returns to Track on system
      Back, and exposes `BreathingCopy.TOP_BAR_TITLE` as its pane title (D-18).
- [ ] **UI/ACCESSIBILITY**: `@Preview` composables for the theme and for every `ui/components/`
      composable, each in light and dark at 100% and 200% font scale, all rendering without
      clipping.
- [ ] **MANUAL**: the `DesignGalleryActivity` icon appears on a debug install on the API 36
      emulator, renders every token and component in both themes at 100% and 200% font, and is
      absent from a release build (D-21).
- [ ] **MANUAL**: installed-app capture of Track, Full Log, Insights, Settings, and Breathing in
      both themes, compared screen by screen against `docs/design/reference/`, with every
      remaining difference attributed to a Phase 16, 17, or 18 screen body.
- [ ] **FAILURE**: with the font resource deliberately unavailable, the app still renders through
      the family's fallback rather than crashing.
- [ ] **DIFF**: `git diff --stat` shows no change to any file listed as unchanged under "Frozen
      interfaces and data contracts".

## Task decomposition

1. Move the fourteen screenshots to `docs/design/reference/`; update `docs/specs/BACKLOG.md`;
   freeze this spec — oracle: documentation commit before any application-code edit. **Done.**
2. Build `MsUppercaseText` and prove D-11 — oracle: the D-11 instrumented test, green, before
   anything else depends on it.
3. Bundle the font and build `theme/Type.kt` — oracle: the typography unit test plus
   `assembleDebug`.
4. Build `theme/Color.kt`, `ColorScheme.kt`, `Emphasis.kt` — oracle: the reflective role test and
   the contrast tests.
5. Build `theme/Shape.kt`, `Spacing.kt`, and wire `shapes` into `MindScaleTheme` — oracle:
   `.\gradlew.bat test`.
6. Build `ui/components/` with previews — oracle: previews render in light/dark at 100%/200%.
7. Restyle the header, the bottom navigation, and the Breathing chrome — oracle: the two new
   chrome instrumented tests plus the full 187/187 connected suite.
8. Route the 9 `AlertDialog` sites through `MsDialog` — oracle: 187/187 connected, with the three
   dialog-title assertions specifically confirmed.
9. Add `DesignGalleryActivity` in `src/debug/` — oracle: debug launcher present, release APK
   verified to exclude it.
10. Full verification, installed-app capture against `docs/design/reference/`, critical review —
    oracle: all four Gradle oracles plus the manual matrix.

## Rollout, migration, and rollback

No migration. Room stays at schema 7; the JSON backup stays at version 7; the records CSV header
stays byte-identical. Nothing this phase writes is persisted, so rollback is `git revert` of the
phase's commits with no data consequence and no user-visible state to reconcile.

## Open questions and approval gates

None open. The one question that had been left — whether to adopt the prototype's initials avatar
— is closed by D-1 rather than decided on taste: it is a different Profile entry point, and
changing entry points is a navigation change this phase forbids.

The one risk that could stop the phase is D-11. If the uppercase-with-restored-semantics
technique does not survive a real connected assertion, the phase halts at task 2 and escalates
rather than editing a test, because a test edit would be an admission that behavior changed.
