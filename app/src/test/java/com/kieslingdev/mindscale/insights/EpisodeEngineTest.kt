package com.kieslingdev.mindscale.insights

import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.HourFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeEngineTest {
    private val hour = 3_600_000L
    private val day = 24 * hour

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
    fun onsetGapHistogramRefusesUntilSixOnsetsWithExactSparseCopy() {
        val expectedCopy = listOf(
            "Needs 6 more recorded starts in this range before this chart is shown. " +
                "There are 0 onset-to-onset gaps to count.",
            "Needs 5 more recorded starts in this range before this chart is shown. " +
                "There are 0 onset-to-onset gaps to count.",
            "Needs 4 more recorded starts in this range before this chart is shown. " +
                "These starts make 1 onset-to-onset gap.",
            "Needs 3 more recorded starts in this range before this chart is shown. " +
                "These starts make 2 onset-to-onset gaps.",
            "Needs 2 more recorded starts in this range before this chart is shown. " +
                "These starts make 3 onset-to-onset gaps.",
            "Needs 1 more recorded start in this range before this chart is shown. " +
                "These starts make 4 onset-to-onset gaps."
        )
        expectedCopy.indices.forEach { onsetCount ->
            val onsets = List(onsetCount) { it * day }
            val sparse = derive(episodeRows(onsets), now = 6 * day).onsetGapHistogram

            assertFalse(sparse.isEligible)
            assertEquals(onsetCount, sparse.eligibleOnsetCount)
            assertEquals((onsetCount - 1).coerceAtLeast(0), sparse.gapCount)
            assertEquals(expectedCopy[onsetCount], onsetGapRefusalText(sparse))
        }

        val eligible = derive(episodeRows(List(6) { it * day }), now = 6 * day).onsetGapHistogram
        assertTrue(eligible.isEligible)
        assertEquals(5, eligible.gapCount)
        assertEquals(5, eligible.buckets[1].count)
        assertEquals(eligible.gapCount, eligible.buckets.sumOf(OnsetGapBucket::count))
    }

    @Test
    fun onsetGapHistogramUsesEachExistingRangeSelection() {
        val now = Instant.parse("2026-06-15T18:00:00Z")
        val onsets = List(6) { index -> now.toEpochMilli() - (12L - index * 2L) * hour }

        InsightRange.entries.forEach { range ->
            val histogram = deriveInsights(
                rows = episodeRows(onsets, zeroAfterMillis = hour),
                hold = HoldDuration.SIXTEEN,
                now = now,
                zoneId = ZoneOffset.UTC,
                range = range
            ).onsetGapHistogram

            assertEquals(range.name, 6, histogram.eligibleOnsetCount)
            assertEquals(range.name, 5, histogram.gapCount)
        }
    }

    @Test
    fun onsetGapHistogramReusesHoldDurationEpisodeSplits() {
        val rows = List(6) { index -> entry(index.toLong() + 1, index * 10L * hour, 5) }
        val now = 59 * hour

        val eightHourHold = derive(rows, hold = HoldDuration.EIGHT, now = now).onsetGapHistogram
        val sixteenHourHold = derive(rows, hold = HoldDuration.SIXTEEN, now = now).onsetGapHistogram

        assertTrue(eightHourHold.isEligible)
        assertEquals(5, eightHourHold.gapCount)
        assertFalse(sixteenHourHold.isEligible)
        assertEquals(1, sixteenHourHold.eligibleOnsetCount)
    }

    @Test
    fun onsetGapHistogramUsesFrozenLowerInclusiveBucketsWithoutRounding() {
        val gaps = listOf(
            day / 2,
            day,
            2 * day,
            3 * day,
            4 * day,
            5 * day,
            6 * day,
            7 * day,
            10 * day,
            14 * day
        )
        val onsets = buildList {
            var onset = 0L
            add(onset)
            gaps.forEach { gap -> onset += gap; add(onset) }
        }
        val now = onsets.last() + day
        val snapshot = deriveInsights(
            rows = episodeRows(onsets),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(now),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.SIX_MONTHS
        )

        assertEquals(List(10) { 1 }, snapshot.onsetGapHistogram.buckets.map(OnsetGapBucket::count))
        assertEquals(
            listOf("<1d", "1d", "2d", "3d", "4d", "5d", "6d", "7–9d", "10–13d", "14+d"),
            snapshot.onsetGapHistogram.buckets.map(OnsetGapBucket::visibleLabel)
        )
        assertEquals(
            "1 of 10 onset-to-onset gaps were at least 14 elapsed days.",
            onsetGapBucketReadout(snapshot.onsetGapHistogram, 9)
        )
    }

    @Test
    fun onsetGapHistogramKeepsValuesBelowBoundariesInThePreviousBucket() {
        val gaps = listOf(1, 2, 3, 4, 5, 6, 7, 10, 14).map { it * day - 1L }
        val onsets = buildList {
            var onset = 0L
            add(onset)
            gaps.forEach { gap -> onset += gap; add(onset) }
        }
        val histogram = deriveInsights(
            rows = episodeRows(onsets),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(onsets.last() + day),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.SIX_MONTHS
        ).onsetGapHistogram

        assertEquals(List(9) { 1 } + 0, histogram.buckets.map(OnsetGapBucket::count))
    }

    @Test
    fun onsetGapHistogramRequiresBothOnsetsInsideHalfOpenRange() {
        val rangeStart = Instant.parse("2026-01-31T00:00:00Z").toEpochMilli()
        val now = Instant.parse("2026-01-31T23:00:00Z").toEpochMilli()
        val onsets = listOf(
            rangeStart - hour,
            rangeStart,
            rangeStart + 3 * hour,
            rangeStart + 6 * hour,
            rangeStart + 9 * hour,
            rangeStart + 12 * hour,
            rangeStart + 15 * hour,
            now
        )
        val histogram = deriveInsights(
            rows = episodeRows(onsets, zeroAfterMillis = 30 * 60_000L),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(now),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        ).onsetGapHistogram

        assertEquals(6, histogram.eligibleOnsetCount)
        assertEquals(5, histogram.gapCount)
        assertEquals(5, histogram.buckets[0].count)
    }

    @Test
    fun onsetGapHistogramUsesElapsedTimeAcrossDst() {
        val zone = ZoneId.of("America/Chicago")
        val springOnsets = listOf(
            "2026-03-07T12:00:00-06:00",
            "2026-03-08T12:00:00-05:00",
            "2026-03-09T12:00:00-05:00",
            "2026-03-10T12:00:00-05:00",
            "2026-03-11T12:00:00-05:00",
            "2026-03-12T12:00:00-05:00"
        ).map { Instant.parse(it).toEpochMilli() }
        val histogram = deriveInsights(
            rows = episodeRows(springOnsets),
            hold = HoldDuration.SIXTEEN,
            now = Instant.parse("2026-03-13T12:00:00-05:00"),
            zoneId = zone,
            range = InsightRange.THIRTY_DAYS
        ).onsetGapHistogram

        assertEquals(1, histogram.buckets[0].count)
        assertEquals(4, histogram.buckets[1].count)
    }

    @Test
    fun onsetGapHistogramUsesTwentyFiveElapsedHoursAcrossFallBack() {
        val zone = ZoneId.of("America/Chicago")
        val fallOnsets = listOf(
            "2026-10-31T12:00:00-05:00",
            "2026-11-01T12:00:00-06:00",
            "2026-11-02T12:00:00-06:00",
            "2026-11-03T12:00:00-06:00",
            "2026-11-04T12:00:00-06:00",
            "2026-11-05T12:00:00-06:00"
        ).map { Instant.parse(it).toEpochMilli() }
        val histogram = deriveInsights(
            rows = episodeRows(fallOnsets),
            hold = HoldDuration.SIXTEEN,
            now = Instant.parse("2026-11-06T12:00:00-06:00"),
            zoneId = zone,
            range = InsightRange.THIRTY_DAYS
        ).onsetGapHistogram

        assertEquals(0, histogram.buckets[0].count)
        assertEquals(5, histogram.buckets[1].count)
    }

    @Test
    fun onsetGapHistogramRetainsExtremeGapInOpenEndedBucket() {
        val onsets = listOf(0L, day, 2 * day, 3 * day, 4 * day, 94 * day)
        val histogram = deriveInsights(
            rows = episodeRows(onsets),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(95 * day),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.SIX_MONTHS
        ).onsetGapHistogram

        assertEquals(1, histogram.buckets[9].count)
        assertEquals(5, histogram.buckets.sumOf(OnsetGapBucket::count))
    }

    @Test
    fun onsetTimeCountsRefuseZeroThroughFiveStartsWithExactCopy() {
        val expectedCopy = listOf(
            "Needs 6 more recorded starts in this range before this chart is shown. There are 0 starts to count by hour.",
            "Needs 5 more recorded starts in this range before this chart is shown. There is 1 start to count by hour.",
            "Needs 4 more recorded starts in this range before this chart is shown. There are 2 starts to count by hour.",
            "Needs 3 more recorded starts in this range before this chart is shown. There are 3 starts to count by hour.",
            "Needs 2 more recorded starts in this range before this chart is shown. There are 4 starts to count by hour.",
            "Needs 1 more recorded start in this range before this chart is shown. There are 5 starts to count by hour."
        )

        expectedCopy.indices.forEach { onsetCount ->
            val counts = derive(episodeRows(List(onsetCount) { it * day }), now = 6 * day).onsetTimeCounts
            assertFalse(counts.isEligible)
            assertEquals(onsetCount, counts.eligibleOnsetCount)
            assertEquals(24, counts.buckets.size)
            assertEquals(onsetCount, counts.buckets.sumOf(OnsetHourBucket::count))
            assertEquals(expectedCopy[onsetCount], onsetTimeRefusalText(counts))
        }
    }

    @Test
    fun onsetTimeCountsUseExactLocalHourBoundariesAndMidnight() {
        val onsets = listOf(
            "2026-01-01T00:00:00Z",
            "2026-01-02T01:59:59Z",
            "2026-01-03T02:00:00Z",
            "2026-01-04T12:30:00Z",
            "2026-01-05T23:00:00Z",
            "2026-01-06T23:59:59Z"
        ).map { Instant.parse(it).toEpochMilli() }
        val counts = deriveInsights(
            rows = episodeRows(onsets, zeroAfterMillis = 60_000L),
            hold = HoldDuration.SIXTEEN,
            now = Instant.parse("2026-01-07T00:00:00Z"),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.SEVEN_DAYS
        ).onsetTimeCounts

        assertTrue(counts.isEligible)
        assertEquals(1, counts.buckets[0].count)
        assertEquals(1, counts.buckets[1].count)
        assertEquals(1, counts.buckets[2].count)
        assertEquals(1, counts.buckets[12].count)
        assertEquals(2, counts.buckets[23].count)
        assertEquals(6, counts.buckets.sumOf(OnsetHourBucket::count))
    }

    @Test
    fun onsetTimeCountsUseEveryExistingRangeSelection() {
        val now = Instant.parse("2026-06-15T18:00:00Z")
        val onsets = List(6) { index -> now.toEpochMilli() - (12L - index * 2L) * hour }

        InsightRange.entries.forEach { range ->
            val counts = deriveInsights(
                rows = episodeRows(onsets, zeroAfterMillis = hour / 2),
                hold = HoldDuration.SIXTEEN,
                now = now,
                zoneId = ZoneOffset.UTC,
                range = range
            ).onsetTimeCounts

            assertEquals(range.name, 6, counts.eligibleOnsetCount)
            assertEquals(range.name, 6, counts.buckets.sumOf(OnsetHourBucket::count))
        }
    }

    @Test
    fun onsetTimeCountsRequireOnsetInsideHalfOpenRange() {
        val rangeStart = Instant.parse("2026-01-31T00:00:00Z").toEpochMilli()
        val now = Instant.parse("2026-01-31T23:00:00Z").toEpochMilli()
        val onsets = listOf(
            rangeStart - hour,
            rangeStart,
            rangeStart + 3 * hour,
            rangeStart + 6 * hour,
            rangeStart + 9 * hour,
            rangeStart + 12 * hour,
            rangeStart + 15 * hour,
            now
        )
        val counts = deriveInsights(
            rows = episodeRows(onsets, zeroAfterMillis = 30 * 60_000L),
            hold = HoldDuration.SIXTEEN,
            now = Instant.ofEpochMilli(now),
            zoneId = ZoneOffset.UTC,
            range = InsightRange.ONE_DAY
        ).onsetTimeCounts

        assertEquals(6, counts.eligibleOnsetCount)
        assertEquals(6, counts.buckets.sumOf(OnsetHourBucket::count))
        assertEquals(1, counts.buckets[0].count)
        assertEquals(0, counts.buckets[23].count)
    }

    @Test
    fun onsetTimeCountsReprojectThroughCurrentZoneAndCombineFallBackHour() {
        val fixedUtcOnsets = List(6) { index ->
            Instant.parse("2026-01-0${index + 1}T14:30:00Z").toEpochMilli()
        }
        val utc = deriveInsights(
            episodeRows(fixedUtcOnsets), HoldDuration.SIXTEEN,
            Instant.parse("2026-01-07T00:00:00Z"), ZoneOffset.UTC, InsightRange.SEVEN_DAYS
        ).onsetTimeCounts
        val chicago = deriveInsights(
            episodeRows(fixedUtcOnsets), HoldDuration.SIXTEEN,
            Instant.parse("2026-01-07T00:00:00Z"), ZoneId.of("America/Chicago"), InsightRange.SEVEN_DAYS
        ).onsetTimeCounts
        assertEquals(6, utc.buckets[14].count)
        assertEquals(6, chicago.buckets[8].count)

        val overlapOnsets = listOf(
            "2026-10-28T01:30:00-05:00",
            "2026-10-29T01:30:00-05:00",
            "2026-10-30T01:30:00-05:00",
            "2026-10-31T01:30:00-05:00",
            "2026-11-01T01:30:00-05:00",
            "2026-11-01T01:30:00-06:00"
        ).map { Instant.parse(it).toEpochMilli() }
        val overlap = deriveInsights(
            episodeRows(overlapOnsets, zeroAfterMillis = 60_000L), HoldDuration.SIXTEEN,
            Instant.parse("2026-11-02T06:00:00Z"), ZoneId.of("America/Chicago"), InsightRange.SEVEN_DAYS
        ).onsetTimeCounts
        assertEquals(6, overlap.buckets[1].count)
        assertEquals(0, overlap.buckets[2].count)
    }

    @Test
    fun onsetTimeFourHourWindowWrapsMidnightAndBreaksTiesAtEarliestHour() {
        val base = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()
        val wrapHours = listOf(22, 23, 0, 1, 22, 23)
        val wrapOnsets = wrapHours.mapIndexed { index, hourOfDay -> base + index * day + hourOfDay * hour }
        val wrap = deriveInsights(
            episodeRows(wrapOnsets, zeroAfterMillis = 60_000L), HoldDuration.SIXTEEN,
            Instant.ofEpochMilli(base + 7 * day), ZoneOffset.UTC, InsightRange.THIRTY_DAYS
        ).onsetTimeCounts
        assertEquals(22, wrap.fourHourWindowStartHour)
        assertEquals(6, wrap.fourHourWindowCount)
        assertEquals(
            "6 of 6 recorded starts in this range were recorded from 10:00 PM up to but not including 2:00 AM.",
            onsetTimeFourHourSentence(wrap, HourFormat.TWELVE)
        )

        val tieHours = listOf(0, 4, 8, 12, 16, 20)
        val tieOnsets = tieHours.mapIndexed { index, hourOfDay -> base + index * day + hourOfDay * hour }
        val tie = deriveInsights(
            episodeRows(tieOnsets, zeroAfterMillis = 60_000L), HoldDuration.SIXTEEN,
            Instant.ofEpochMilli(base + 7 * day), ZoneOffset.UTC, InsightRange.THIRTY_DAYS
        ).onsetTimeCounts
        assertEquals(0, tie.fourHourWindowStartHour)
        assertEquals(1, tie.fourHourWindowCount)
    }

    @Test
    fun onsetTimeCoveredDaysUseLocalDatesAcrossDstAndPartialEndpoints() {
        val zone = ZoneId.of("America/Chicago")
        val first = Instant.parse("2026-03-08T05:30:00Z").toEpochMilli() // Mar 7, 11:30 PM CST
        val now = Instant.parse("2026-03-09T05:30:00Z") // Mar 9, 12:30 AM CDT
        val counts = deriveInsights(
            rows = listOf(entry(1, first, 0)),
            hold = HoldDuration.SIXTEEN,
            now = now,
            zoneId = zone,
            range = InsightRange.SEVEN_DAYS
        ).onsetTimeCounts

        assertEquals(3, counts.coveredLocalDayCount)
    }

    @Test
    fun onsetTimeCopyUsesExactTwelveAndTwentyFourHourBoundariesWithoutInference() {
        val counts = derive(episodeRows(List(6) { it * day + 14 * hour }), now = 7 * day).onsetTimeCounts

        assertEquals("2p", onsetHourVisibleLabel(14, HourFormat.TWELVE))
        assertEquals("14", onsetHourVisibleLabel(14, HourFormat.TWENTY_FOUR))
        assertEquals(
            "6 of 6 recorded starts were recorded from 2:00 PM up to but not including 3:00 PM.",
            onsetTimeBucketReadout(counts, 14, HourFormat.TWELVE)
        )
        assertEquals(
            "6 of 6 recorded starts were recorded from 14:00 up to but not including 15:00.",
            onsetTimeBucketReadout(counts, 14, HourFormat.TWENTY_FOUR)
        )
        val generated = listOf(
            onsetTimeDenominator(counts),
            onsetTimeBucketReadout(counts, 14, HourFormat.TWELVE).orEmpty(),
            onsetTimeFourHourSentence(counts, HourFormat.TWELVE)
        ).joinToString(" ").lowercase()
        listOf("typical", "usually", "tends to", "most common", "pattern", "risk", "predicts", "diagnosis")
            .forEach { assertFalse(generated.contains(it)) }
    }

    @Test
    fun onsetTimeCountsReuseHoldAndSleepEpisodeSplitsAndIgnoreMarkers() {
        val rows = buildList {
            repeat(6) { index ->
                add(entry(index.toLong() + 1, index * 10L * hour, 5))
                add(marker(index.toLong() + 20, index * 10L * hour, "context $index"))
            }
        }
        val eight = derive(rows, hold = HoldDuration.EIGHT, now = 70 * hour).onsetTimeCounts
        val sleepPaused = derive(
            rows + sleep(40, hour, 20 * hour),
            hold = HoldDuration.EIGHT,
            now = 70 * hour
        ).onsetTimeCounts
        val twentyFour = derive(rows, hold = HoldDuration.TWENTY_FOUR, now = 70 * hour).onsetTimeCounts

        assertTrue(eight.isEligible)
        assertEquals(4, sleepPaused.eligibleOnsetCount)
        assertFalse(sleepPaused.isEligible)
        assertFalse(twentyFour.isEligible)
        assertEquals(eight.eligibleOnsetCount, eight.buckets.sumOf(OnsetHourBucket::count))
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

    private fun episodeRows(onsets: List<Long>, zeroAfterMillis: Long = hour): List<EpisodeSourceRow> =
        buildList {
            var id = 1L
            onsets.forEach { onset ->
                add(entry(id++, onset, 5))
                add(entry(id++, onset + zeroAfterMillis, 0))
            }
        }
}
