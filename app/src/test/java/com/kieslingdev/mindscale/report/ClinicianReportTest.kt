package com.kieslingdev.mindscale.report

import com.kieslingdev.mindscale.data.BreathingSession
import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.SafetyPlanStep
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import com.kieslingdev.mindscale.insights.InsightRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicianReportTest {
    private val generatedAt = Instant.parse("2026-08-04T12:00:00Z")

    /**
     * The Report is a boundary object a user hands to a clinician. The safety plan holds
     * contacts' phone numbers and means-restriction details, so it never enters the
     * summary text even though `DataSnapshot` now carries it
     * (`docs/specs/SPEC-safety-card.md`, D-5, Invariant 10).
     */
    @Test
    fun theSafetyPlanNeverEntersTheClinicianSummary() {
        val plan = listOf(
            SafetyPlanItem(1, SafetyPlanStep.PEOPLE_FOR_HELP, 0, "Sam Rivera", "+1 555 010 0199"),
            SafetyPlanItem(2, SafetyPlanStep.ENVIRONMENT_SAFETY, 0, "Sam keeps the spare keys")
        )
        val text = buildClinicianReport(
            DataSnapshot(
                entries = emptyList(),
                sleeps = emptyList(),
                markers = emptyList(),
                settings = TrackSettings(),
                safetyPlan = plan
            ),
            InsightRange.THIRTY_DAYS,
            generatedAt,
            ZoneOffset.UTC
        ).text

        assertFalse(text.contains("Sam Rivera"))
        assertFalse(text.contains("+1 555 010 0199"))
        assertFalse(text.contains("spare keys"))
        assertFalse(text.lowercase().contains("safety plan"))
        assertEquals(
            "A populated plan must not change the summary at all",
            buildClinicianReport(
                DataSnapshot(emptyList(), emptyList(), emptyList(), TrackSettings()),
                InsightRange.THIRTY_DAYS,
                generatedAt,
                ZoneOffset.UTC
            ).text,
            text
        )
    }

    /**
     * A breathing session is an exported fact, not a clinical one. Putting sessions in the
     * summary next to intensity would invite the causal reading that two placebo-controlled
     * trials failed to support (`docs/specs/SPEC-paced-breathing.md`, D-11, Invariant 11).
     */
    @Test
    fun breathingSessionsNeverEnterTheClinicianSummary() {
        val sessions = listOf(
            BreathingSession(1, startedAt = 1_700_000_000_000L, endedAt = 1_700_000_300_000L),
            BreathingSession(2, startedAt = 1_700_000_400_000L, endedAt = 1_700_001_000_000L)
        )
        val withSessions = buildClinicianReport(
            DataSnapshot(
                entries = emptyList(),
                sleeps = emptyList(),
                markers = emptyList(),
                settings = TrackSettings(),
                breathingSessions = sessions
            ),
            InsightRange.THIRTY_DAYS,
            generatedAt,
            ZoneOffset.UTC
        ).text

        assertFalse(withSessions.lowercase().contains("breath"))
        assertFalse(withSessions.lowercase().contains("paced"))
        assertEquals(
            "Recorded sessions must not change the summary at all",
            buildClinicianReport(
                DataSnapshot(emptyList(), emptyList(), emptyList(), TrackSettings()),
                InsightRange.THIRTY_DAYS,
                generatedAt,
                ZoneOffset.UTC
            ).text,
            withSessions
        )
    }

    @Test
    fun emptyThirtyDayReportIsExactAndNonInferential() {
        val report = buildClinicianReport(
            DataSnapshot(emptyList(), emptyList(), emptyList(), TrackSettings()),
            InsightRange.THIRTY_DAYS,
            generatedAt,
            ZoneOffset.UTC
        )

        assertEquals(
            """MINDSCALE — USER-RECORDED CLINICIAN SUMMARY
Window: 2026-07-06 through 2026-08-04 · 30 days
Generated: 2026-08-04T12:00:00Z · current zone Z

MindScale stores and arranges user-recorded information. It does not diagnose,
interpret, administer questionnaires, or provide a clinical assessment.
Times reflect when recording was possible.

RECORDED COURSE
No ratings were recorded in this window.

EVENTS MARKED
No events were marked in this window.

EPISODE STRUCTURE
No derived episode spans touched this window.

TIME OF DAY
Fewer than 6 recorded starts are in this window, so no time-of-day count is shown.

SLEEP
No completed sleep periods had a recorded Wake in this window.
These counts do not establish whether sleep affected later records or later records affected sleep.

EXTERNALLY OBTAINED TOTALS
No externally obtained PHQ-8 or GAD-7 totals are stored in this window.
MindScale did not administer, calculate, or interpret PHQ-8 or GAD-7 totals.

Generated locally on this device. This text may contain sensitive health information.
Review the underlying records before relying on this summary.""",
            report.text
        )
    }

    @Test
    fun populatedReportCapsSensitiveRowsAndPreservesExternalProvenance() {
        val todayStart = Instant.parse("2026-08-04T00:00:00Z").toEpochMilli()
        val markers = (1L..8L).map { Marker(it, todayStart + it * 1_000, "event\n$it") }
        val scores = (0L..4L).map { offset ->
            ExternalScore(
                id = offset + 1,
                instrument = if (offset % 2L == 0L) ExternalInstrument.PHQ_8 else ExternalInstrument.GAD_7,
                total = offset.toInt(),
                assessedEpochDay = LocalDate.of(2026, 8, 4).minusDays(offset).toEpochDay(),
                enteredAt = todayStart + offset
            )
        }
        val report = buildClinicianReport(
            DataSnapshot(
                entries = listOf(
                    Entry(1, todayStart + 1_000, 5),
                    Entry(2, todayStart + 3_601_000, 0)
                ),
                sleeps = listOf(SleepInterval(1, todayStart - 4 * HOUR, todayStart)),
                markers = markers,
                settings = TrackSettings(),
                profile = UserProfile(displayName = "  Ada   Example  "),
                externalScores = scores
            ),
            InsightRange.THIRTY_DAYS,
            generatedAt,
            ZoneOffset.UTC
        ).text

        assertTrue(report.contains("Name: Ada Example"))
        assertTrue(report.contains("event 3"))
        assertFalse(report.contains("event 2"))
        assertTrue(report.contains("2 additional marked events not shown."))
        assertTrue(report.contains("4 additional externally obtained totals not shown.").not())
        assertTrue(report.contains("1 additional externally obtained total not shown."))
        assertTrue(report.contains("PHQ-8 total 0 — entered by the user from a result obtained elsewhere."))
        assertFalse(report.contains("event\n"))
        listOf(" improved ", " worse ", " higher ", " lower ", " severity ", " moderate ", " symptom-free ")
            .forEach { banned -> assertFalse("banned report term $banned", report.lowercase().contains(banned)) }
    }

    @Test
    fun reportFilenameUsesCapturedUtcInstant() {
        assertEquals(
            "mindscale-clinician-summary-20260804-120000.txt",
            clinicianReportFilename(generatedAt)
        )
    }

    private companion object { const val HOUR = 3_600_000L }
}
