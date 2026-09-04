package com.oleksandr.fastflow.domain.logic

import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastState
import com.oleksandr.fastflow.domain.model.FastingPlan

/**
 * Derives the app's state machine position from stored data (SPEC 3.2).
 *
 * Pure, so the IDLE → FASTING → OVERTIME → EATING → IDLE walk can be tested
 * without a database or a device clock.
 */
object FastStateResolver {

    /**
     * @param trackingPaused the user switched tracking off; nothing counts and
     *   nothing starts by itself. A running fast still wins, because starting
     *   one clears the pause.
     */
    fun resolve(
        activeFast: Fast?,
        activePlan: FastingPlan?,
        lastFinished: Fast?,
        lastPlan: FastingPlan?,
        nowMillis: Long,
        trackingPaused: Boolean = false,
    ): FastState {
        if (activeFast != null) {
            val plan = activePlan ?: planFrom(activeFast)
            val targetAtMillis = activeFast.startMillis + activeFast.targetMinutes * 60_000L
            return if (nowMillis >= targetAtMillis) {
                FastState.Overtime(activeFast, plan)
            } else {
                FastState.Fasting(activeFast, plan)
            }
        }

        if (trackingPaused) return FastState.Paused

        val endMillis = lastFinished?.endMillis
        val eatingMinutes = lastFinished?.eatingWindowMinutes
        if (lastFinished != null && endMillis != null && eatingMinutes != null) {
            val windowEndMillis = endMillis + eatingMinutes * 60_000L
            if (nowMillis < windowEndMillis) {
                return FastState.Eating(
                    previousFast = lastFinished,
                    plan = lastPlan ?: planFrom(lastFinished),
                    windowEndsAtMillis = windowEndMillis,
                    // Only worth showing while it is still reachable.
                    creditDeadlineMillis = FastScoring
                        .compensationDeadlineMillis(lastFinished)
                        ?.takeIf { it > nowMillis },
                )
            }
        }

        // Extended plans drop straight back to idle when they end (SPEC 3.2).
        return FastState.Idle
    }

    /**
     * Rebuilds a plan from the values frozen onto the fast.
     *
     * Used when the plan row is gone — a deleted custom plan must not blank out
     * a fast that is still running.
     */
    private fun planFrom(fast: Fast) = FastingPlan(
        id = fast.planId,
        name = fast.planId,
        fastingMinutes = fast.targetMinutes,
        eatingMinutes = fast.eatingWindowMinutes,
        isPreset = false,
        sortOrder = Int.MAX_VALUE,
    )
}
