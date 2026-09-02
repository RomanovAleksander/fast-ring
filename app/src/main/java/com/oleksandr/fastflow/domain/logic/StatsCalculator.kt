package com.oleksandr.fastflow.domain.logic

import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastStats
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.WeekFields

/** Totals for the statistics screen (SPEC 3.4). */
object StatsCalculator {

    /**
     * @param fasts every recorded fast; running ones are ignored for totals,
     *   since their duration is not final.
     * @param today used to bound the week and month buckets. A fast is counted
     *   in the period it started in.
     */
    fun compute(fasts: List<Fast>, today: LocalDate): FastStats {
        val finished = fasts.filter { !it.isActive }
        if (finished.isEmpty()) return FastStats()

        val durations = finished.mapNotNull { it.actualDuration }
        val totalMillis = durations.sumOf { it.toMillis() }

        val successes = finished.count { FastScoring.outcome(it, nextOf(fasts, it)).isSuccess }
        val endedEarly = finished.count { fast ->
            val actual = fast.actualDuration ?: Duration.ZERO
            actual < Duration.ofMinutes(fast.targetMinutes.toLong())
        }

        val weekFields = WeekFields.ISO
        val thisWeek = today.get(weekFields.weekOfWeekBasedYear())
        val thisWeekYear = today.get(weekFields.weekBasedYear())

        val weekMillis = finished
            .filter {
                val d = it.startDate
                d.get(weekFields.weekOfWeekBasedYear()) == thisWeek &&
                    d.get(weekFields.weekBasedYear()) == thisWeekYear
            }
            .sumOf { it.actualDuration?.toMillis() ?: 0L }

        val monthMillis = finished
            .filter { it.startDate.year == today.year && it.startDate.month == today.month }
            .sumOf { it.actualDuration?.toMillis() ?: 0L }

        return FastStats(
            totalFasts = finished.size,
            successfulFasts = successes,
            endedEarlyFasts = endedEarly,
            averageDuration = Duration.ofMillis(totalMillis / finished.size),
            longestDuration = durations.maxOrNull() ?: Duration.ZERO,
            totalThisWeek = Duration.ofMillis(weekMillis),
            totalThisMonth = Duration.ofMillis(monthMillis),
            totalAllTime = Duration.ofMillis(totalMillis),
        )
    }

    /** The fast that follows [fast] chronologically, needed to score compensation. */
    private fun nextOf(all: List<Fast>, fast: Fast): Fast? =
        all.filter { it.startMillis > fast.startMillis }.minByOrNull { it.startMillis }
}
