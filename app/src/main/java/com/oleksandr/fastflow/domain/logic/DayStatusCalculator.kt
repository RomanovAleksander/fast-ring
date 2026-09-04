package com.oleksandr.fastflow.domain.logic

import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.DayStatus
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastOutcome
import java.time.LocalDate

/**
 * Turns a list of fasts into per-day calendar statuses (SPEC 3.4).
 *
 * A day's status can change after the fact: compensation depends on when the
 * *next* fast started, so a PARTIAL day becomes SUCCESS the moment an early
 * next fast is recorded (SPEC 7, case 4d). Statuses are therefore always
 * recomputed from the full list, never cached per day.
 */
object DayStatusCalculator {

    /** A fast paired with its score. Kept positional: ids are 0 until Room assigns them. */
    private data class Scored(val fast: Fast, val outcome: FastOutcome)

    fun compute(
        fasts: List<Fast>,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
        nowMillis: Long,
    ): Map<LocalDate, DayInfo> {
        val ordered = fasts.sortedBy { it.startMillis }
        val firstDate = ordered.firstOrNull()?.startDate

        // Each fast is scored against the one that follows it chronologically.
        val scored = ordered.mapIndexed { index, fast ->
            Scored(fast, FastScoring.outcome(fast, ordered.getOrNull(index + 1)))
        }

        // Index by covered day, so each calendar cell is a cheap lookup.
        val byDate = mutableMapOf<LocalDate, MutableList<Scored>>()
        scored.forEach { entry ->
            entry.fast.coveredDates(nowMillis).forEach { date ->
                byDate.getOrPut(date) { mutableListOf() } += entry
            }
        }

        val result = linkedMapOf<LocalDate, DayInfo>()
        var date = from
        while (!date.isAfter(to)) {
            result[date] = dayInfo(
                date = date,
                covering = byDate[date].orEmpty(),
                today = today,
                nowMillis = nowMillis,
                firstDate = firstDate,
            )
            date = date.plusDays(1)
        }
        return result
    }

    private fun dayInfo(
        date: LocalDate,
        covering: List<Scored>,
        today: LocalDate,
        nowMillis: Long,
        firstDate: LocalDate?,
    ): DayInfo {
        val ratio = covering.maxOfOrNull { it.fast.completionRatio(nowMillis) } ?: 0f

        if (covering.isEmpty()) {
            // MISSED only inside the recorded history; before it, and in the
            // future, the day simply has no meaning yet.
            val status = if (firstDate != null && date < today && !date.isBefore(firstDate)) {
                DayStatus.MISSED
            } else {
                DayStatus.NONE
            }
            return DayInfo(date, status, compensated = false, completionRatio = 0f)
        }

        val successes = covering.filter { it.outcome.isSuccess }

        // A day earned outright outranks one still running, which outranks a
        // day that fell short; two fasts in a day take the best (SPEC 7, case 5).
        return when {
            successes.isNotEmpty() -> DayInfo(
                date = date,
                status = DayStatus.SUCCESS,
                // ↺ only when nothing hit the threshold on its own.
                compensated = successes.none { it.outcome == FastOutcome.SUCCESS },
                completionRatio = ratio,
            )

            covering.any { it.fast.isActive } ->
                DayInfo(date, DayStatus.ACTIVE, compensated = false, completionRatio = ratio)

            else -> DayInfo(date, DayStatus.PARTIAL, compensated = false, completionRatio = ratio)
        }
    }
}
