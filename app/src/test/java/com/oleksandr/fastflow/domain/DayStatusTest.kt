package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.DayStatusCalculator
import com.oleksandr.fastflow.domain.logic.FastScoring
import com.oleksandr.fastflow.domain.logic.StreakCalculator
import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.DayStatus
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastOutcome
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mandatory domain cases from SPEC 7. Numbering follows the spec so a
 * failure points straight at the rule it broke.
 */
class DayStatusTest {

    private val kyiv: ZoneId = ZoneId.of("Europe/Kyiv")

    private fun millis(text: String, zone: ZoneId = kyiv): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun fast(
        start: String,
        end: String?,
        targetMinutes: Int = 16 * 60,
        eatingMinutes: Int? = 8 * 60,
        zone: ZoneId = kyiv,
    ) = Fast(
        planId = "p16_8",
        targetMinutes = targetMinutes,
        eatingWindowMinutes = eatingMinutes,
        startMillis = millis(start, zone),
        endMillis = end?.let { millis(it, zone) },
        timeZoneId = zone.id,
    )

    private fun statuses(
        fasts: List<Fast>,
        from: String,
        to: String,
        today: String,
        now: String,
    ) = DayStatusCalculator.compute(
        fasts = fasts,
        from = LocalDate.parse(from),
        to = LocalDate.parse(to),
        today = LocalDate.parse(today),
        nowMillis = millis(now),
    )

    // 1
    @Test
    fun `case 1 - a 16h fast inside one day marks that day success`() {
        val map = statuses(
            fasts = listOf(fast("2025-03-10T06:00", "2025-03-10T22:00")),
            from = "2025-03-10", to = "2025-03-10",
            today = "2025-03-11", now = "2025-03-11T10:00",
        )
        assertEquals(DayStatus.SUCCESS, info(map, "2025-03-10").status)
    }

    // 2
    @Test
    fun `case 2 - an overnight 16h fast marks both days success`() {
        val map = statuses(
            fasts = listOf(fast("2025-03-10T20:00", "2025-03-11T12:00")),
            from = "2025-03-10", to = "2025-03-11",
            today = "2025-03-12", now = "2025-03-12T10:00",
        )
        assertEquals(DayStatus.SUCCESS, info(map, "2025-03-10").status)
        assertEquals(DayStatus.SUCCESS, info(map, "2025-03-11").status)
    }

    // 3
    @Test
    fun `case 3 - a 36h extended fast covers all three days`() {
        val map = statuses(
            fasts = listOf(
                fast("2025-03-10T20:00", "2025-03-12T08:00", targetMinutes = 36 * 60, eatingMinutes = null),
            ),
            from = "2025-03-10", to = "2025-03-12",
            today = "2025-03-13", now = "2025-03-13T10:00",
        )
        assertEquals(DayStatus.SUCCESS, info(map, "2025-03-10").status)
        assertEquals(DayStatus.SUCCESS, info(map, "2025-03-11").status)
        assertEquals(DayStatus.SUCCESS, info(map, "2025-03-12").status)
    }

    // 4
    @Test
    fun `case 4 - ten hours of a sixteen hour goal with a full eating window is partial`() {
        val first = fast("2025-03-10T06:00", "2025-03-10T16:00")
        val next = fast("2025-03-11T00:00", "2025-03-11T16:00")
        val map = statuses(
            fasts = listOf(first, next),
            from = "2025-03-10", to = "2025-03-10",
            today = "2025-03-12", now = "2025-03-12T10:00",
        )
        assertEquals(DayStatus.PARTIAL, info(map, "2025-03-10").status)
        assertEquals(FastOutcome.PARTIAL, FastScoring.outcome(first, next))
    }

    // 4a
    @Test
    fun `case 4a - fourteen and a half hours clears the ninety percent threshold`() {
        val f = fast("2025-03-10T06:00", "2025-03-10T20:30")
        assertEquals(FastOutcome.SUCCESS, FastScoring.outcome(f, null))
    }

    // 4b
    @Test
    fun `case 4b - an early next fast compensates exactly on the boundary`() {
        val short = fast("2025-03-10T00:00", "2025-03-10T13:30")
        val next = fast("2025-03-10T19:00", "2025-03-11T11:00")
        assertEquals(FastOutcome.COMPENSATED, FastScoring.outcome(short, next))
    }

    // 4c
    @Test
    fun `case 4c - half an hour too late to compensate leaves the day partial`() {
        val short = fast("2025-03-10T00:00", "2025-03-10T13:30")
        val next = fast("2025-03-10T19:30", "2025-03-11T11:30")
        assertEquals(FastOutcome.PARTIAL, FastScoring.outcome(short, next))
    }

    // 4d
    @Test
    fun `case 4d - a partial day flips to success once the next fast starts early`() {
        val short = fast("2025-03-10T06:00", "2025-03-10T19:30")

        val before = statuses(
            fasts = listOf(short),
            from = "2025-03-10", to = "2025-03-10",
            today = "2025-03-11", now = "2025-03-11T10:00",
        )
        assertEquals(DayStatus.PARTIAL, info(before, "2025-03-10").status)

        // Next fast five hours later: window shortened by 3h against a 2h30m shortfall.
        val next = fast("2025-03-11T00:30", "2025-03-11T16:30")
        val after = statuses(
            fasts = listOf(short, next),
            from = "2025-03-10", to = "2025-03-10",
            today = "2025-03-12", now = "2025-03-12T10:00",
        )
        assertEquals(DayStatus.SUCCESS, info(after, "2025-03-10").status)
        assertTrue("credited through compensation", info(after, "2025-03-10").compensated)
    }

    // 4e
    @Test
    fun `case 4e - extended fasts are never compensated`() {
        val short = fast(
            "2025-03-10T00:00", "2025-03-11T06:00",
            targetMinutes = 36 * 60, eatingMinutes = null,
        )
        val next = fast("2025-03-11T07:00", "2025-03-11T23:00")
        assertEquals(FastOutcome.PARTIAL, FastScoring.outcome(short, next))
    }

    // 5
    @Test
    fun `case 5 - the best fast of the day wins`() {
        val partial = fast("2025-03-10T00:00", "2025-03-10T08:00")
        val good = fast("2025-03-10T10:00", "2025-03-11T02:00")
        val map = statuses(
            fasts = listOf(partial, good),
            from = "2025-03-10", to = "2025-03-10",
            today = "2025-03-12", now = "2025-03-12T10:00",
        )
        assertEquals(DayStatus.SUCCESS, info(map, "2025-03-10").status)
    }

    // 6
    @Test
    fun `case 6 - a day with no fast is missed and resets the streak`() {
        val fasts = listOf(
            fast("2025-03-10T06:00", "2025-03-10T22:00"),
            fast("2025-03-12T06:00", "2025-03-12T22:00"),
        )
        val map = statuses(
            fasts = fasts,
            from = "2025-03-10", to = "2025-03-13",
            today = "2025-03-13", now = "2025-03-13T10:00",
        )
        assertEquals(DayStatus.MISSED, info(map, "2025-03-11").status)

        val streak = StreakCalculator.compute(map, LocalDate.parse("2025-03-13"))
        assertEquals(1, streak.current)
    }

    // 7
    @Test
    fun `case 7 - a partial day resets the streak just like a missed one`() {
        val fasts = listOf(
            fast("2025-03-10T06:00", "2025-03-10T22:00"),
            fast("2025-03-11T06:00", "2025-03-11T14:00"),
            fast("2025-03-12T06:00", "2025-03-12T22:00"),
        )
        val map = statuses(
            fasts = fasts,
            from = "2025-03-10", to = "2025-03-13",
            today = "2025-03-13", now = "2025-03-13T10:00",
        )
        assertEquals(DayStatus.PARTIAL, info(map, "2025-03-11").status)
        assertEquals(1, StreakCalculator.compute(map, LocalDate.parse("2025-03-13")).current)
    }

    // 7a
    @Test
    fun `case 7a - a compensated day keeps the streak running`() {
        val fasts = listOf(
            fast("2025-03-10T06:00", "2025-03-10T22:00"),
            fast("2025-03-11T06:00", "2025-03-11T19:30"),
            fast("2025-03-12T01:00", "2025-03-12T17:00"),
        )
        val map = statuses(
            fasts = fasts,
            from = "2025-03-10", to = "2025-03-13",
            today = "2025-03-13", now = "2025-03-13T10:00",
        )
        assertEquals(DayStatus.SUCCESS, info(map, "2025-03-11").status)
        assertTrue(info(map, "2025-03-11").compensated)
        assertEquals(3, StreakCalculator.compute(map, LocalDate.parse("2025-03-13")).current)
    }

    // 8
    @Test
    fun `case 8 - an active today counts the streak from yesterday`() {
        val fasts = listOf(
            fast("2025-03-10T06:00", "2025-03-10T22:00"),
            fast("2025-03-11T08:00", null),
        )
        val map = statuses(
            fasts = fasts,
            from = "2025-03-10", to = "2025-03-11",
            today = "2025-03-11", now = "2025-03-11T12:00",
        )
        assertEquals(DayStatus.ACTIVE, info(map, "2025-03-11").status)
        assertEquals(1, StreakCalculator.compute(map, LocalDate.parse("2025-03-11")).current)
    }

    // 9
    @Test
    fun `case 9 - duration across the DST change counts real elapsed time`() {
        // Kyiv leaves summer time on 2025-10-26, so this wall-clock 16h is 17h.
        val f = fast("2025-10-25T20:00", "2025-10-26T12:00")
        assertEquals(Duration.ofHours(17), f.actualDuration)
    }

    // 10
    @Test
    fun `case 10 - the calendar uses the zone stored on the record`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val f = fast("2025-03-10T23:00", "2025-03-11T15:00", zone = tokyo)
        val covered = f.coveredDates(millis("2025-03-12T00:00"))
        assertEquals(listOf(LocalDate.parse("2025-03-10"), LocalDate.parse("2025-03-11")), covered)
    }

    @Test
    fun `a fast ending exactly at midnight does not claim the next day`() {
        val f = fast("2025-03-10T08:00", "2025-03-11T00:00")
        assertEquals(listOf(LocalDate.parse("2025-03-10")), f.coveredDates(millis("2025-03-12T00:00")))
    }

    @Test
    fun `longest streak survives later gaps`() {
        val fasts = listOf(
            fast("2025-03-01T06:00", "2025-03-01T22:00"),
            fast("2025-03-02T06:00", "2025-03-02T22:00"),
            fast("2025-03-03T06:00", "2025-03-03T22:00"),
            fast("2025-03-06T06:00", "2025-03-06T22:00"),
        )
        val map = statuses(
            fasts = fasts,
            from = "2025-03-01", to = "2025-03-07",
            today = "2025-03-07", now = "2025-03-07T10:00",
        )
        val streak = StreakCalculator.compute(map, LocalDate.parse("2025-03-07"))
        assertEquals(3, streak.longest)
        // Today has no fast yet, so the count runs from yesterday, which succeeded.
        assertEquals(1, streak.current)
    }

    @Test
    fun `days before the first fast are neutral, not missed`() {
        val map = statuses(
            fasts = listOf(fast("2025-03-10T06:00", "2025-03-10T22:00")),
            from = "2025-03-08", to = "2025-03-10",
            today = "2025-03-11", now = "2025-03-11T10:00",
        )
        assertEquals(DayStatus.NONE, info(map, "2025-03-08").status)
        assertEquals(DayStatus.NONE, info(map, "2025-03-09").status)
    }

    @Test
    fun `compensation deadline is the latest start that still earns the day`() {
        val short = fast("2025-03-10T00:00", "2025-03-10T13:30")
        val deadline = FastScoring.compensationDeadlineMillis(short)
        assertEquals(millis("2025-03-10T19:00"), deadline)
    }

    @Test
    fun `a fast that met its goal needs no compensation deadline`() {
        val f = fast("2025-03-10T06:00", "2025-03-10T22:00")
        assertEquals(null, FastScoring.compensationDeadlineMillis(f))
    }

    private fun info(map: Map<LocalDate, DayInfo>, date: String) =
        map.getValue(LocalDate.parse(date))
}
