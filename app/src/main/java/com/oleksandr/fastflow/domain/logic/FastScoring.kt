package com.oleksandr.fastflow.domain.logic

import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastOutcome

/**
 * Scores a finished fast (SPEC 3.4).
 *
 * All comparisons are integer millisecond arithmetic: the compensation rule is
 * checked for `>=` on an exact boundary (SPEC 7, case 4b), which floating point
 * would make a coin flip.
 */
object FastScoring {

    /** A fast counts as done at 90 % of its goal. */
    const val SUCCESS_NUMERATOR = 9L
    const val SUCCESS_DENOMINATOR = 10L

    fun outcome(fast: Fast, nextFast: Fast?): FastOutcome {
        val endMillis = fast.endMillis ?: return FastOutcome.UNFINISHED
        val targetMillis = fast.targetMinutes * 60_000L
        if (targetMillis <= 0L) return FastOutcome.SUCCESS

        val actualMillis = (endMillis - fast.startMillis).coerceAtLeast(0L)

        // (a) threshold: actual >= 0.9 * target, without dividing.
        if (actualMillis * SUCCESS_DENOMINATOR >= targetMillis * SUCCESS_NUMERATOR) {
            return FastOutcome.SUCCESS
        }

        // (b) compensation. Extended plans have no eating window to shorten,
        // so they cannot be compensated (SPEC 7, case 4e).
        val plannedEatingMillis = fast.eatingWindowMinutes?.times(60_000L)
            ?: return FastOutcome.PARTIAL
        val next = nextFast ?: return FastOutcome.PARTIAL

        val gapMillis = next.startMillis - endMillis
        if (gapMillis < 0L) return FastOutcome.PARTIAL

        val shortfallMillis = targetMillis - actualMillis
        return if (plannedEatingMillis - gapMillis >= shortfallMillis) {
            FastOutcome.COMPENSATED
        } else {
            FastOutcome.PARTIAL
        }
    }

    /**
     * Latest instant at which starting the next fast still earns the day for
     * [fast]. `null` when the day needs no compensation or cannot get it.
     *
     * Drives the "start before HH:MM to keep the day" hint (SPEC 3.2, 5.2).
     */
    fun compensationDeadlineMillis(fast: Fast): Long? {
        val endMillis = fast.endMillis ?: return null
        val targetMillis = fast.targetMinutes * 60_000L
        val actualMillis = (endMillis - fast.startMillis).coerceAtLeast(0L)
        if (actualMillis * SUCCESS_DENOMINATOR >= targetMillis * SUCCESS_NUMERATOR) return null

        val plannedEatingMillis = fast.eatingWindowMinutes?.times(60_000L) ?: return null
        val shortfallMillis = targetMillis - actualMillis
        val allowedGapMillis = plannedEatingMillis - shortfallMillis
        // Shortfall wider than the whole eating window: no start time can pay it back.
        if (allowedGapMillis < 0L) return null
        return endMillis + allowedGapMillis
    }
}
