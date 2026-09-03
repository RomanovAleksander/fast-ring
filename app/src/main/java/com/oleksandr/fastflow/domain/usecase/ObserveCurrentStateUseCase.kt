package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.FastStateResolver
import com.oleksandr.fastflow.domain.model.FastState
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.PlanRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * The current position in the state machine, as a stream.
 *
 * Combines the stored fasts with a one-second tick: FASTING becomes OVERTIME
 * and EATING becomes IDLE purely from the clock, with nothing written, so the
 * transition has to be driven by time rather than by a database change.
 */
class ObserveCurrentStateUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val planRepository: PlanRepository,
    private val clock: AppClock,
) {
    operator fun invoke(): Flow<FastState> = combine(
        fastRepository.observeActive(),
        fastRepository.observeLastFinished(),
        planRepository.observeAll(),
        secondTicker(),
    ) { active, lastFinished, plans, _ ->
        FastStateResolver.resolve(
            activeFast = active,
            activePlan = active?.let { fast -> plans.firstOrNull { it.id == fast.planId } },
            lastFinished = lastFinished,
            lastPlan = lastFinished?.let { fast -> plans.firstOrNull { it.id == fast.planId } },
            nowMillis = clock.nowMillis(),
        )
    }

    // Deliberately NOT distinctUntilChanged: while a fast runs, the state is
    // equal from one second to the next (same fast, same plan), so dedup would
    // swallow every tick and the on-screen timer would freeze.

    private fun secondTicker(): Flow<Long> = flow {
        while (true) {
            val now = clock.nowMillis()
            emit(now)
            // Sleep to the next whole second so the display stays in step with
            // the wall clock instead of drifting by the work done each tick.
            delay(TICK_MILLIS - now % TICK_MILLIS)
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
