package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.DayStatusCalculator
import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.repository.FastRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Day statuses for a date range (SPEC 3.4).
 *
 * Reads the whole history rather than just the range: compensation is decided
 * by the fast that comes *after* the one being scored, which may fall outside
 * the month on screen. A single user's history is small enough for this to be
 * cheaper than the bookkeeping needed to avoid it.
 */
class ComputeDayStatusesUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val clock: AppClock,
) {
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DayInfo>> =
        fastRepository.observeAll().map { fasts ->
            DayStatusCalculator.compute(
                fasts = fasts,
                from = from,
                to = to,
                today = clock.today(),
                nowMillis = clock.nowMillis(),
            )
        }
}
