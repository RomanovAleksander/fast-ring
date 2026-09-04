package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.StatsCalculator
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastStats
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The statistics screen, checked against numbers worked out by hand.
 *
 * Every total here is one a user could recompute on paper, which is the point:
 * a statistic that cannot be argued with is the only kind worth showing.
 */
class StatsCalculatorTest {

    private val kyiv: ZoneId = ZoneId.of("Europe/Kyiv")

    private fun at(text: String): Long =
        LocalDateTime.parse(text).atZone(kyiv).toInstant().toEpochMilli()

    private fun fast(
        id: Long,
        from: String,
        to: String?,
        targetMinutes: Int = 16 * 60,
        eatingMinutes: Int? = 8 * 60,
    ) = Fast(
        id = id,
        planId = "16:8",
        targetMinutes = targetMinutes,
        eatingWindowMinutes = eatingMinutes,
        startMillis = at(from),
        endMillis = to?.let { at(it) },
        timeZoneId = kyiv.id,
        createdAt = at(from),
        updatedAt = at(from),
    )

    /** Monday to Wednesday of the same ISO week, so week and month agree. */
    private val hitTheGoal = fast(1, "2025-03-10T20:00", "2025-03-11T12:00")   // 16 h, met
    private val endedShort = fast(2, "2025-03-11T20:00", "2025-03-12T10:00")   // 14 h, 87.5 %
    private val compensating = fast(3, "2025-03-12T15:00", "2025-03-13T08:00") // 17 h, met
    private val today: LocalDate = LocalDate.parse("2025-03-13")

    private val all = listOf(hitTheGoal, endedShort, compensating)

    @Test
    fun noFastsMeansEveryTotalIsZero() {
        assertEquals(FastStats(), StatsCalculator.compute(emptyList(), today))
    }

    @Test
    fun aRunningFastAloneStillReadsAsZero() {
        val running = fast(1, "2025-03-13T20:00", null)
        assertEquals(FastStats(), StatsCalculator.compute(listOf(running), today))
    }

    @Test
    fun countsDurationsExactly() {
        val stats = StatsCalculator.compute(all, today)

        assertEquals(3, stats.totalFasts)
        assertEquals(Duration.ofHours(16 + 14 + 17), stats.totalAllTime)
        assertEquals(Duration.ofHours(17), stats.longestDuration)
        // 47 h over three fasts = 15 h 40 min.
        assertEquals(Duration.ofHours(15).plusMinutes(40), stats.averageDuration)
    }

    @Test
    fun onlyTheShortOneCountsAsEndedEarly() {
        assertEquals(1, StatsCalculator.compute(all, today).endedEarlyFasts)
    }

    /**
     * The 14-hour fast fell 2 h short, and the next one started 5 h into an
     * 8-hour eating window, so the shortfall is covered and the day is earned
     * (SPEC 3.4b). All three therefore count as successful.
     */
    @Test
    fun compensationCountsTowardsSuccesses() {
        assertEquals(3, StatsCalculator.compute(all, today).successfulFasts)

        // Without the fast that compensates it, the short one is not a success.
        val withoutCompensation = listOf(hitTheGoal, endedShort)
        assertEquals(1, StatsCalculator.compute(withoutCompensation, today).successfulFasts)
    }

    @Test
    fun aRunningFastIsExcludedFromTotalsButStillScoresTheOneBeforeIt() {
        val running = fast(4, "2025-03-13T13:00", null)
        val stats = StatsCalculator.compute(all + running, today)

        assertEquals("a running fast has no final duration to total", 3, stats.totalFasts)
        assertEquals(Duration.ofHours(47), stats.totalAllTime)
    }

    @Test
    fun weekAndMonthCountTheFastsThatStartedInThem() {
        val stats = StatsCalculator.compute(all, today)

        // 10, 11 and 12 March 2025 are Mon-Wed of the week holding the 13th.
        assertEquals(Duration.ofHours(47), stats.totalThisWeek)
        assertEquals(Duration.ofHours(47), stats.totalThisMonth)

        // A week later nothing falls in the current week, but March still holds.
        val nextWeek = StatsCalculator.compute(all, LocalDate.parse("2025-03-20"))
        assertEquals(Duration.ZERO, nextWeek.totalThisWeek)
        assertEquals(Duration.ofHours(47), nextWeek.totalThisMonth)

        // ...and in April neither does.
        val april = StatsCalculator.compute(all, LocalDate.parse("2025-04-03"))
        assertEquals(Duration.ZERO, april.totalThisMonth)
        assertEquals(Duration.ofHours(47), april.totalAllTime)
    }
}
