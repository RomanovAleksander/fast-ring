package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.DayStatusCalculator
import com.oleksandr.fastflow.domain.logic.StreakCalculator
import com.oleksandr.fastflow.domain.model.Streak
import com.oleksandr.fastflow.domain.repository.FastRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Current and longest streaks over the whole history.
 *
 * Recomputed from scratch every time, because a day's status can change after
 * the fact once a compensating fast is recorded (SPEC 8).
 */
class ComputeStreakUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val clock: AppClock,
) {
    operator fun invoke(): Flow<Streak> = fastRepository.observeAll().map { fasts ->
        if (fasts.isEmpty()) return@map Streak()

        val today = clock.today()
        val statuses = DayStatusCalculator.compute(
            fasts = fasts,
            from = fasts.minOf { it.startDate },
            to = today,
            today = today,
            nowMillis = clock.nowMillis(),
        )
        StreakCalculator.compute(statuses, today)
    }
}
