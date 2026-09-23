# MindScale owner-feedback implementation handoff

Updated 2026-09-23. Status: IMPLEMENTED — VERIFIED LOCALLY. Final evidence: `docs/reviews/2026-09-23-owner-feedback.md`. The preparation/pause and next-action sections below are historical; the explicit resume and final verification supersede them.

## Continuation and authority

User requested preparation with only 2% weekly usage remaining, then continuation in a fresh task after they use a reset. Usage tool confirmed 98% used. Do not redeem a reset or start the successor before the user resumes. When resumed, use saved MindScale project `3eecebe4-aef4-476a-91d0-a7e34d85eed8` directly (`environment: local`), preserving its dirty checkout. Do not create a clean worktree and lose prior changes.

Checkout: `S:\Android\AndroidProjects\MindScale`; branch `codex/release-readiness`; HEAD `937ab49f169cfccffec94bf640fe99b14246ba51`. Extensive prior release-readiness and simplification work is uncommitted, including the screens being changed. Preserve all of it. No app changes, builds, tests, installation, commit, push or publication occurred in this preparation pass.

Read repo AGENTS.md, PROJECT_STATE.md, this spec, and relevant FAILED_PATHS.md entries. Current request supersedes conflicting presentation requirements in SPEC-simplification.md and older visual-only specs, only within the scope below. No new approval needed for requested implementation. No authority to commit/push/publish, alter toolchain/signing, erase user records, or run tests on the personal phone. Adding a user-operated Delete note action is authorized; exercising it uses synthetic emulator data.

Carry forward user's continuation policy: manage context autonomously; prefer a fresh task only when substantial obsolete history makes a concise handoff useful; record branch/dirty work/evidence/next action first; settle workers and preserve checkout; confirm successor started and stop duplicate work. Use cheapest capable workers only for independent work likely to save usage/time. No handoff-only chains. Do not bypass pause, limits or approvals.

## Acceptance criteria

### Track, branding and notes

1. Remove excessive empty space above the numpad. The question-mark help control belongs below the numpad AND Sleep/Wake controls, aligned right. Do not leave an empty readout/help container or divider reserving that space. Preserve useful transient feedback and help accessibility, paused behavior and capture semantics.
2. Make MindScale wordmark modestly larger (start around 10–15%, assess visually), centered at top on Track, Log and Insights. Retain page identity and Profile access; accommodate 200% font without overlap.
3. Make current `Mark an event` look like a small, unmistakable button (visible boundary/fill and button semantics). User wrote “Mark and Event”; existing functional label can remain grammatically correct. Maintain >=48dp touch target despite compact visual appearance.
4. Give Track history an explicit `Recent Logs` section header, including a sensible empty state.
5. Put rich-note formatting controls ABOVE the text field in both Track and Log. Add an explicit `Delete note` action for an existing note; remove only the note, persist null, retain rating/value/time/chips and all other records. Clearly distinguish it from deleting the record. Preserve failure/retry/conflict behavior and editor composition/selection. Prefer a draft-clear then existing save flow or dedicated targeted action with appropriate confirmation; never implement deletion as entry deletion.

### Log

6. Match rating entries to Track Recent Logs visual treatment: number/band, metadata, notes, separators and action hierarchy. Reuse a small shared row/presentation component where useful without hiding differences in event/sleep records. Preserve chronological grouping/filtering and note privacy.
7. Add event editing from each event row, including event text and timestamp using existing shared date/time controls. Save/Cancel, validation, busy/failure/missing-record behavior and restoration must be explicit. Preserve ID and unrelated data; targeted update, not delete/reinsert. DAO currently has insert/observe/count/delete only; add update/read support and meaningful persistence tests, with no schema migration required. Reuse current event-text and future-time rules.

### Insights

8. `What you recorded`: visible slight rounded transitions instead of apparent right angles. Existing visual projection already implements a 15-minute easing window; investigate why it appears sharp at wider ranges. Use a screen-space-aware rendering treatment if needed, bounded to real contiguous intervals. Preserve exact recording points/readouts, no overshoot, no bridges across sleep/unknown/hold expiry, and unchanged analytical values. Smooth stroke and fill consistently.
9. Reduce event-line snapping substantially. Nearby ratings must remain selectable even when an event is close in time. Prefer nearest rendered target within a small pixel tolerance, with explicit tie policy. Verify dense event/rating cases across range/zoom sizes and preserve accessible previous/next actions.
10. Place collapsed `Day-by-day view` immediately AFTER the What you recorded chart.
11. Remove the entire prose-heavy `Episodes` facts section: “episode(s) touched this range,” waking-time <=16h tally, duplicate clear-day sentence, highest/median prose, and assumed-ending counts. Do not remove underlying analytical safeguards or factual uncertainty from data. Selected dates should make the active range clear.
12. Keep Episodes, Typical length, Clear days and Peak in the top summary; add the existing median metric and total logged burden there as compact, clearly labeled values with units. Verify the median's existing definition before adding it: do not substitute a different median or duplicate Typical length. Use responsive wrapping (e.g. two rows of three) with no horizontal clipping. Burden is descriptive intensity-hours, never a clinical score.
13. Rename `Each episode` to `Episodes in this Time Range`; independently collapsed by default with accessible expanded state and saved UI state. Keep episode information concise and visual; no assumed-ending prose tally.
14. `Days between onsets`: ten buckets in TWO ROWS OF FIVE, slimmer and entirely within screen width. Preserve bucket boundaries, counts and comparable scale; no horizontal scroll to see the visual.
15. `Time of day it started`: replace current presentation with one 24-hour bar chart, one interval per local hour. Dynamic integer Y axis = number of episode starts in that hour. All 24 bars and axes visible together, no horizontal scrolling. Legible sparse hour ticks are acceptable; every hour/count accessible by selection and semantics. Zero-data, single-start and tied counts must work. Do not alter onset or DST definitions.

### Clinician summary

16. Redesign for highly relevant, quick-to-scan information with visuals wherever useful. Start with selected date range, compact factual metrics, the recorded course, and only concise relevant episode/event details. Remove redundant prose and boilerplate metric narration. Retain minimal essential context about observation gaps, recorded versus inferred time and units without a wall of text. No diagnosis, causal inference or unsupported clinical claims.
17. Inspect both report screen and its copy/export output. Make the screen visually useful and keep the exported summary similarly brief and consistent. Preserve privacy controls, chosen range, factual computations and explicitly included profile/scores when applicable. Do not add a new export format/dependency merely to create visuals. A text export can remain text, concisely structured.

## Code map from initial inspection

Paths below relative to `app/src/main/java/com/kieslingdev/mindscale/`.

- `MindScaleApp.kt:233` MindScaleHeader: center wordmark currently MindScale only for Track; other root pages use page title and place brand left. Check both ordinary and >=1.5 fontScale branches and shared MsWordmark sizing.
- `track/TrackScreen.kt:175` TrackScreen item order; `:324` ReadoutAndHelpRow always includes HelpToggle and padded hairline even without readout; `:758` MarkerSection plain label; `:1050` EntryRow and `:1159` actions; `:1429` NoteDialog. History has no section-title item.
- `ui/components/RichNoteEditor.kt:123` Column contains BasicTextField before toolbar Row at :171. Move visual order without changing the pending-emission/composition repair.
- `track/TrackViewModel.kt:518` note save and `log/LogViewModel.kt:252` saveNote already target entryDao.updateNote. Track includes baseline conflict validation; retain it.
- `log/LogScreen.kt:396` LogItemRow; `:508` InlineEditPanel; `:549` InlineNotePanel. LogEvent currently exposes edit for entries only. `data/MarkerDao.kt` has no update. Extend LogModels/LogEvent/LogViewModel and FakeMarkerDao as needed.
- `insights/InsightsScreen.kt:261` state and item order; SummaryStrip then day-by-day then entry chart; More details currently wraps facts, episodes and distributions. `:478` Each episode, `:643` OnsetGapSection, `:742` OnsetTimeSection, `:1130` EntryChartSection.
- Chart/report supplemental findings will be appended below. Line numbers are starting pointers, not stable after edits.

## Supplemental chart/report findings

- `insights/InsightsScreen.kt:1162` EntryStepChart already samples smoothstep transitions 16 times and uses rounded stroke caps. Increasing samples alone may not fix the perceptual near-vertical transition at a broad time scale; verify on-screen.
- `insights/EntryChartVisualProjection.kt:5` visual-only 15-minute projection. Keep analytical values/readouts unchanged.
- `InsightsScreen.kt:107` ChartEventSnapRadius is 24dp; `:1415` chartInstantFromPosition gives nearby markers priority. Start around 6–8dp, but ensure closest-rating selection rather than relying on radius alone.
- `insights/InsightsModels.kt:137` InsightSummary already has intensityHours and typicalLengthMillis (median EPISODE DURATION).
- `insights/EpisodeEngine.kt:141` existing fact median is median EPISODE PEAK, not median of all recorded intensities. Promote this computation with an honest `Median peak` label to the summary. Keep `Typical length` as duration. `:190` builds summary; `:390` accumulates burden.
- `report/ClinicianReport.kt:106` builds text sections; `:130` burden and `:149` median duration/peak. `report/ReportScreen.kt:117` is currently one text card plus copy/share/save actions. `report/ReportProfileViewModel.kt:134` generates/publishes report. Use the structured snapshot for on-screen visuals and keep text export brief.

Read-only worker completed; no workers remain editing or running for this request.

## Verification and next action

### Implementation decisions — 2026-09-23

- Median peak uses the existing median of episode peaks; Typical length remains median episode duration. Analytical episode/hold/DST definitions are unchanged.
- Chart rounding is a visual-only projection of at least eight screen pixels where a real contiguous measured interval permits it. Exact source points/readouts and analytical values remain authoritative. Nearest rendered rating/event within 8dp wins; equal-distance ties prefer the rating, then the earlier instant.
- Delete note clears the draft; Save persists null through the existing targeted note update. Event edits trim and validate text using capture rules, retain ID, and update only timestamp/text.
- Dimension audit: Insights drops four obsolete histogram width/gap dimensions and gains one documented hour-tick label width (31→28); Log gains a 42dp rating circle matching Track (2→3); Report gains a documented 320dp metric wrapping breakpoint (1). The audit retains exact counts and rejects undocumented literals.
- The clinician summary screen and text share one structured factual presentation. Latest six exact ratings use discrete bars, clearly labeled as a subset; events/scores retain omitted counts. This introduces no new export format or dependency.

Next: read the supplemental chart/report map; implement in small groups (shared header/Track/notes, Log event update and row parity, Insights visuals and report). Review the existing diff before modifying each file. Add focused tests for note deletion retaining the record, event persistence/cancel/invalid time/missing row, nearest-target selection and analytical invariance. Update old UI assertions for renamed/moved/collapsed sections.

Use repo Gradle wrapper and existing Android Studio JDK. `ANDROID_HOME=S:\Android\Sdk`, `ANDROID_AVD_HOME=S:\Android\Avd`, `GRADLE_USER_HOME=S:\Android\Gradle`; read repo setup for exact Quail 2 JDK path. Run `test`, `lint`, `assembleDebug`, and connected UI tests on isolated API36 review emulator. Inspect changed UI with realistic synthetic data in Light/Dark, small-phone width and 200% text. Capture chart rounding, dense hover selection, both complete distribution charts and collapsed sections. User screenshots are not required to begin.

Last PRIOR verification (2026-09-17, not re-run now): 480 JVM tests, lint 0 errors/26 warnings, debug assembly; 281 distinct API36 cases passed across full run plus targeted reruns. See `docs/reviews/2026-09-17-simplification.md`. These results do not verify this new request. Existing API26 runtime and spoken TalkBack gaps remain.

Known dead ends: saved AVD clone ran out of storage; use isolated MindScaleReview_API36 under build/review/avd. RichNoteEditor must not replace local composition state with every delayed StateFlow echo (FAILED_PATHS.md:88). NumberPicker tests must target the active dialog root (:99). Do not revert those repairs.
