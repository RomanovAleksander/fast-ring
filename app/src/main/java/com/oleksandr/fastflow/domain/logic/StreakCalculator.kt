package com.oleksandr.fastflow.domain.logic

import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.DayStatus
import com.oleksandr.fastflow.domain.model.Streak
import java.time.LocalDate

/**
 * Counts successful days in a row (SPEC 3.4).
 *
 * Strict by design: PARTIAL breaks a streak exactly like MISSED, with no
 * weekend or rest-day exceptions.
 */
object StreakCalculator {

    fun compute(statuses: Map<LocalDate, DayInfo>, today: LocalDate): Streak =
        Streak(
            current = currentStreak(statuses, today),
            longest = longestStreak(statuses),
        )

    private fun currentStreak(statuses: Map<LocalDate, DayInfo>, today: LocalDate): Int {
        val todayStatus = statuses[today]?.status ?: DayStatus.NONE

        // Today only counts once it is earned; while it is still running (or has
        // not started) the streak is measured from yesterday (SPEC 7, case 8).
        val anchor = when (todayStatus) {
            DayStatus.SUCCESS -> today
            DayStatus.ACTIVE, DayStatus.NONE, DayStatus.REST -> today.minusDays(1)
            DayStatus.PARTIAL, DayStatus.MISSED -> return 0
        }

        var count = 0
        var date = anchor
        while (statuses[date]?.status == DayStatus.SUCCESS) {
            count++
            date = date.minusDays(1)
        }
        return count
    }

    private fun longestStreak(statuses: Map<LocalDate, DayInfo>): Int {
        if (statuses.isEmpty()) return 0
        val first = statuses.keys.min()
        val last = statuses.keys.max()

        var best = 0
        var run = 0
        var date = first
        while (!date.isAfter(last)) {
            if (statuses[date]?.status == DayStatus.SUCCESS) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
            date = date.plusDays(1)
        }
        return best
    }
}
