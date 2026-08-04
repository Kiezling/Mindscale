package com.kieslingdev.mindscale.insights

import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.HoldDuration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeEngineTest {
    private val hour = 3_600_000L

    @Test
    fun explicitZero_sleepPausesAwakeDurationAndAuc() {
        val snapshot = derive(
            listOf(
                entry(1, 0, 5, listOf("work")),
                sleep(2, 2 * hour, 4 * hour),
                entry(3, 6 * hour, 0)
            ),
            now = 8 * hour
        )

        val episode = snapshot.recentEpisodes.single()
        assertEquals(EpisodeEndReason.EXPLICIT_ZERO, episode.endReason)
        assertEquals(4 * hour, episode.awakeDurationMillis)
        assertEquals(20.0, episode.intensityHours, 0.0001)
        assertEquals(1, episode.sleepCount)
        assertEquals(listOf("work"), episode.chips)
    }

    @Test
    fun assumedHoldAdvancesThroughSleepUsingAwakeHours() {
        val snapshot = derive(
            listOf(entry(1, 0, 5), sleep(2, 2 * hour, 12 * hour)),
            hold = HoldDuration.EIGHT,
            now = 20 * hour
        )

        val episode = snapshot.recentEpisodes.single()
        assertEquals(EpisodeEndReason.ASSUMED_HOLD, episode.endReason)
        assertEquals(18 * hour, episode.endMillis)
        assertEquals(8 * hour, episode.awakeDurationMillis)
        assertEquals(40.0, episode.intensityHours, 0.0001)
    }

    @Test
    fun positiveChangeResetsHoldButRemainsOneEpisode() {
        val snapshot = derive(
            listOf(entry(1, 0, 4), entry(2, 7 * hour, 6)),
            hold = HoldDuration.EIGHT,
            now = 20 * hour
        )

        val episode = snapshot.recentEpisodes.single()
        assertEquals(0L, episode.onsetMillis)
        assertEquals(15 * hour, episode.endMillis)
        assertEquals(6, episode.peak)
        assertEquals(15 * hour, episode.awakeDurationMillis)
    }

    @Test
    fun sameTimestampUsesHighestIdAndDoesNotCreateZeroDurationEpisode() {
        val snapshot = derive(
            listOf(
                entry(1, 0, 9, listOf("older")),
                entry(2, 0, 0)
            ),
            now = hour
        )

        assertTrue(snapshot.recentEpisodes.isEmpty())
        assertEquals(null, snapshot.summary.peak)
    }

    @Test
    fun holdAwareOnsetClassificationHandlesAssumedGap() {
        val source = listOf(entry(1, 0, 5))

        assertFalse(isOnsetAt(source, 7 * hour, HoldDuration.EIGHT))
        assertTrue(isOnsetAt(source, 8 * hour, HoldDuration.EIGHT))
        assertTrue(isOnsetAt(source, 9 * hour, HoldDuration.EIGHT))
    }

    @Test
    fun overlappingSleepsAreUnionedAndNeverDoubleSubtracted() {
        val snapshot = derive(
            listOf(
                entry(1, 0, 5),
                sleep(2, hour, 4 * hour),
                sleep(3, 3 * hour, 5 * hour),
                entry(4, 6 * hour, 0)
            ),
            now = 7 * hour
        )

        val episode = snapshot.recentEpisodes.single()
        assertEquals(2 * hour, episode.awakeDurationMillis)
        assertEquals(10.0, episode.intensityHours, 0.0001)
        assertEquals(1, episode.sleepCount)
    }

    @Test
    fun rangeAucIsClippedAndCarriedEpisodeIsDisclosed() {
        val zone = ZoneOffset.UTC
        val now = Instant.parse("2026-01-31T12:00:00Z")
        val onset = Instant.parse("2026-01-30T20:00:00Z").toEpochMilli()
        val snapshot = deriveInsights(
            rows = listOf(entry(1, onset, 10)),
            hold = HoldDuration.SIXTEEN,
            now = now,
            zoneId = zone,
            range = InsightRange.ONE_DAY
        )

        assertTrue(snapshot.facts.first().detail!!.contains("1 carried in"))
        assertTrue(snapshot.facts.any { it.text.contains("120.0 intensity-hours") })
    }

    @Test
    fun carriedEpisodeDurationStatisticIsClippedButEpisodeDetailRemainsFull() {
        val zone = ZoneOffset.UTC
        val rangeStart = Instant.parse("2026-01-31T00:00:00Z").toEpochMilli()
        val onset = Instant.parse("2026-01-30T20:00:00Z").toEpochMilli()
        val end = Instant.parse("2026-01-31T04:00:00Z").toEpochMilli()
        val snapshot = deriveInsights(
            rows = listOf(entry(1, onset, 5), entry(2, end, 0)),
            hold = HoldDuration.SIXTEEN,
            now = Instant.parse("2026-01-31T12:00:00Z"),
            zoneId = zone,
            range = InsightRange.ONE_DAY
        )

        assertEquals(rangeStart, snapshot.rangeStartMillis)
        assertEquals(4 * hour, snapshot.summary.typicalLengthMillis)
        assertEquals(8 * hour, snapshot.recentEpisodes.single().awakeDurationMillis)
    }

    @Test
    fun springForwardRasterUsesTwentyThreeHourLocalDay() {
        val zone = ZoneId.of("America/Chicago")
        val now = Instant.parse("2026-03-09T04:00:00Z") // Mar 8, 11 PM CDT
        val dayStart = Instant.parse("2026-03-08T06:00:00Z").toEpochMilli()
        val snapshot = deriveInsights(
            rows = listOf(entry(1, dayStart, 0)),
            hold = HoldDuration.SIXTEEN,
            now = now,
            zoneId = zone,
            range = InsightRange.ONE_DAY
        )

        val day = snapshot.rasterDays.single()
        assertEquals(23 * hour, day.endMillis - day.startMillis)
        assertTrue(day.segments.any { it.state == RasterState.FUTURE })
    }

    @Test
    fun futureFactBecomesNextInvalidationBoundary() {
        val now = Instant.ofEpochMilli(10 * hour)
        val future = 11 * hour
        val snapshot = deriveInsights(
            rows = listOf(entry(1, 0, 0), entry(2, future, 7)),
            hold = HoldDuration.SIXTEEN,
            now = now,
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        )

        assertEquals(future, snapshot.nextInvalidationMillis)
    }

    @Test
    fun futureSleepEndIsAnInvalidationBoundaryAndPausesHoldUntilThen() {
        val now = 2 * hour
        val wake = 5 * hour
        val snapshot = derive(
            listOf(entry(1, 0, 5), sleep(2, hour, wake)),
            hold = HoldDuration.EIGHT,
            now = now
        )

        assertEquals(wake, snapshot.nextInvalidationMillis)
        assertEquals(EpisodeEndReason.ONGOING, snapshot.recentEpisodes.single().endReason)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidClosedSleepFailsRatherThanBeingRewritten() {
        derive(listOf(entry(1, 0, 4), sleep(2, 2 * hour, hour)), now = 3 * hour)
    }

    @Test
    fun everyHoldChoiceEndsAtItsAwakeHourCap() {
        HoldDuration.entries.forEach { hold ->
            val episode = derive(listOf(entry(1, 0, 4)), hold = hold, now = 30 * hour)
                .recentEpisodes.single()
            assertEquals(hold.hours * hour, episode.endMillis)
            assertEquals(EpisodeEndReason.ASSUMED_HOLD, episode.endReason)
        }
    }

    @Test
    fun multipleAndTouchingSleepsAdvanceHoldWithoutDoubleCounting() {
        val episode = derive(
            listOf(
                entry(1, 0, 5),
                sleep(2, hour, 2 * hour),
                sleep(3, 2 * hour, 4 * hour),
                sleep(4, 6 * hour, 7 * hour)
            ),
            hold = HoldDuration.EIGHT,
            now = 20 * hour
        ).recentEpisodes.single()

        assertEquals(12 * hour, episode.endMillis)
        assertEquals(8 * hour, episode.awakeDurationMillis)
        assertEquals(2, episode.sleepCount)
    }

    @Test
    fun openSleepKeepsEpisodeOngoingAndDoesNotScheduleHoldExpiry() {
        val snapshot = derive(
            listOf(entry(1, 0, 5), sleep(2, hour, null)),
            hold = HoldDuration.EIGHT,
            now = 20 * hour
        )

        val episode = snapshot.recentEpisodes.single()
        assertEquals(EpisodeEndReason.ONGOING, episode.endReason)
        assertEquals(hour, episode.awakeDurationMillis)
        assertEquals(24 * hour, snapshot.nextInvalidationMillis)
    }

    @Test
    fun assumedEndingAllowsLaterEpisodeRestart() {
        val snapshot = derive(
            listOf(entry(1, 0, 5), entry(2, 10 * hour, 3)),
            hold = HoldDuration.EIGHT,
            now = 12 * hour
        )

        assertEquals(2, snapshot.recentEpisodes.size)
        assertEquals(EpisodeEndReason.ONGOING, snapshot.recentEpisodes[0].endReason)
        assertEquals(EpisodeEndReason.ASSUMED_HOLD, snapshot.recentEpisodes[1].endReason)
    }

    @Test
    fun fallBackRasterUsesTwentyFiveHourLocalDay() {
        val zone = ZoneId.of("America/Chicago")
        val now = Instant.parse("2026-11-02T05:00:00Z")
        val dayStart = Instant.parse("2026-11-01T05:00:00Z").toEpochMilli()
        val snapshot = deriveInsights(
            rows = listOf(entry(1, dayStart, 0)),
            hold = HoldDuration.SIXTEEN,
            now = now,
            zoneId = zone,
            range = InsightRange.ONE_DAY
        )

        assertEquals(25 * hour, snapshot.rasterDays.single().endMillis - snapshot.rasterDays.single().startMillis)
    }

    @Test
    fun rasterKeepsNoDataWellAndFutureTextualStatesDistinct() {
        val snapshot = derive(listOf(entry(1, 2 * hour, 0)), now = 12 * hour)

        assertEquals(RasterState.NO_DATA, snapshot.rasterStateAt(hour).first)
        assertEquals(RasterState.WELL, snapshot.rasterStateAt(3 * hour).first)
        assertEquals(RasterState.FUTURE, snapshot.rasterStateAt(13 * hour).first)
    }

    @Test
    fun clearDaysUseEligibleAwakeDaysAndExcludeEntirelySleepingDay() {
        val zone = ZoneOffset.UTC
        val day = 24 * hour
        val snapshot = deriveInsights(
            rows = listOf(
                entry(1, 0, 0),
                sleep(2, 0, day),
                entry(3, day, 6),
                entry(4, day + hour, 0)
            ),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(2 * day + 12 * hour),
            zoneId = zone,
            range = InsightRange.THREE_DAYS
        )

        assertEquals(2, snapshot.summary.eligibleDays)
        assertEquals(1, snapshot.summary.clearDays)
    }

    @Test
    fun medianDurationAndRoundingHandleEvenEpisodesAndMinuteCarry() {
        val snapshot = derive(
            listOf(
                entry(1, 0, 4), entry(2, 2 * hour, 0),
                entry(3, 3 * hour, 6), entry(4, 7 * hour, 0)
            ),
            now = 8 * hour
        )

        assertEquals(3 * hour, snapshot.summary.typicalLengthMillis)
        assertEquals("1h", formatDuration(59 * 60_000L + 30_000L))
        assertEquals("2m", formatDuration(90_000L))
    }

    @Test
    fun insightGrammarAvoidsInferentialClaimsAndAlwaysShowsClearDayDenominator() {
        val snapshot = derive(listOf(entry(1, 0, 4), entry(2, hour, 0)), now = 2 * hour)
        val rendered = snapshot.facts.flatMap { listOfNotNull(it.text, it.detail) }.joinToString(" ").lowercase()

        listOf("causes", "predicts", "correlation", "significant", "diagnosis").forEach {
            assertFalse(rendered.contains(it))
        }
        assertTrue(snapshot.facts.any { it.text.contains("eligible days") })
    }

    @Test
    fun entryChartUsesStepStatesSourceMetadataAndSleepGaps() {
        val snapshot = deriveInsights(
            rows = listOf(
                entry(1, 0, 5, listOf("work"), note = "hard morning"),
                sleep(2, 2 * hour, 4 * hour),
                entry(3, 6 * hour, 0)
            ),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(8 * hour),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        )

        val chart = snapshot.entryChart
        assertEquals(listOf(0L to 2 * hour, 4 * hour to 6 * hour), chart.segments.filter { it.value == 5 }.map { it.startMillis to it.endMillis })
        assertEquals(listOf("work"), chart.segments.first().chips)
        assertEquals("hard morning", chart.segments.first().note)
        assertEquals(listOf(2 * hour to 4 * hour), chart.sleeps.map { it.startMillis to it.endMillis })
        assertEquals(0, chart.readingAt(7 * hour).value ?: 0)
        assertEquals(EntryChartState.ASLEEP, chart.readingAt(3 * hour).state)
    }

    @Test
    fun assumedHoldDropsStepToMetadataFreeWellState() {
        val chart = deriveInsights(
            rows = listOf(entry(1, 0, 7, note = "source")),
            hold = HoldDuration.EIGHT,
            now = Instant.ofEpochMilli(12 * hour),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        ).entryChart

        assertEquals(7, chart.readingAt(7 * hour).value)
        val afterHold = chart.segments.single { 9 * hour in it.startMillis until it.endMillis }
        assertEquals(0, afterHold.value)
        assertEquals(null, afterHold.sourceEntryMillis)
        assertEquals(null, afterHold.note)
    }

    @Test
    fun markersAreOrderedClippedAndNeverChangeEpisodes() {
        val now = 12 * hour
        val snapshot = deriveInsights(
            rows = listOf(
                entry(1, 0, 5),
                marker(9, 4 * hour, "second"),
                marker(8, 4 * hour, "first"),
                marker(10, 13 * hour, "future")
            ),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(now),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        )

        assertEquals(listOf(8L, 9L), snapshot.entryChart.markers.map(EntryChartMarker::id))
        assertEquals(listOf("first", "second"), snapshot.entryChart.readingAt(4 * hour).markers.map(EntryChartMarker::text))
        assertEquals(1, snapshot.summary.episodeCount)
        assertEquals(13 * hour, snapshot.nextInvalidationMillis)
    }

    @Test
    fun carriedChartStateClipsGeometryButRetainsSourceTimestamp() {
        val now = Instant.parse("2026-01-31T04:00:00Z")
        val source = Instant.parse("2026-01-30T22:00:00Z").toEpochMilli()
        val chart = deriveInsights(
            rows = listOf(entry(1, source, 4, note = "before range")),
            hold = HoldDuration.EIGHT,
            now = now,
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        ).entryChart

        assertEquals(Instant.parse("2026-01-31T00:00:00Z").toEpochMilli(), chart.segments.first().startMillis)
        assertEquals(source, chart.segments.first().sourceEntryMillis)
    }

    private fun derive(
        rows: List<EpisodeSourceRow>,
        hold: HoldDuration = HoldDuration.SIXTEEN,
        now: Long
    ): InsightsSnapshot = deriveInsights(
        rows = rows,
        hold = hold,
        now = Instant.ofEpochMilli(now),
        zoneId = ZoneOffset.UTC,
        range = InsightRange.THIRTY_DAYS
    )

    private fun entry(
        id: Long,
        ts: Long,
        value: Int,
        chips: List<String> = emptyList(),
        note: String? = null
    ) = EpisodeSourceRow("ENTRY", id, ts, null, value, chips, note = note)

    private fun sleep(id: Long, start: Long, end: Long?) =
        EpisodeSourceRow("SLEEP", id, start, end, null, null)

    private fun marker(id: Long, ts: Long, text: String) =
        EpisodeSourceRow("MARKER", id, ts, null, null, null, text = text)
}
