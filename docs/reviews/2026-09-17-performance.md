# R-8 long-history derivation measurement

Date: 2026-09-17

Scope: bounded measurement of `EpisodeEngineKt.deriveInsights`; no optimization or production/test-source edit.

The existing compiled debug classes were exercised through a standalone Java harness in
`build/review/HistoryBenchmark.java`. Synthetic rows used UTC timestamps, daily seven-hour
sleep spans, and five `ENTRY` ratings per day. Each case received one warm-up call followed by
three timed calls; the reported value is the median. The range was `NINETY_DAYS`, hold duration
`EIGHT`, and the same fixed `now` was used for every run.

| Synthetic history | Rows | Median | Three runs |
| --- | ---: | ---: | ---: |
| 1 year, daily sleep + 5 entries/day | 2,190 | 16.522 ms | 14.930 / 16.522 / 17.260 ms |
| 5 years, daily sleep + 5 entries/day | 10,950 | 69.582 ms | 68.100 / 69.582 / 71.014 ms |
| high bound: 20,000 sleeps + 20,000 entries | 40,000 | 2,059.431 ms | 1,997.205 / 2,059.431 / 2,395.193 ms |

This confirms the flagged long-history risk is material at an artificial upper bound: the
current repeated sleep-list scans can reach roughly two seconds in this JVM run. Five years of
daily data remained under 100 ms in the same process. The result supports prioritizing a bounded
algorithm follow-up before histories near 20,000 records become a normal supported case; it does
not authorize or include that rewrite.

These are machine-only timings from a Windows desktop JVM against existing compiled debug
classes, not Android device latency or a release build measurement. They are not a flaky test
assertion and contain no real user data. Harness source and generated class files remain under
ignored `build/review/`.
