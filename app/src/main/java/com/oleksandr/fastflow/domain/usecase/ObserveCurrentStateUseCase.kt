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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    }.distinctUntilChanged()

    private fun secondTicker(): Flow<Long> = flow {
        while (true) {
            emit(clock.nowMillis())
            delay(TICK_MILLIS)
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
