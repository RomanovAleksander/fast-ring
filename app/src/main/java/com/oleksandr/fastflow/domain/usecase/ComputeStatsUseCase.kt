package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.StatsCalculator
import com.oleksandr.fastflow.domain.model.FastStats
import com.oleksandr.fastflow.domain.repository.FastRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ComputeStatsUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val clock: AppClock,
) {
    operator fun invoke(): Flow<FastStats> = fastRepository.observeAll().map { fasts ->
        StatsCalculator.compute(fasts, clock.today())
    }
}
