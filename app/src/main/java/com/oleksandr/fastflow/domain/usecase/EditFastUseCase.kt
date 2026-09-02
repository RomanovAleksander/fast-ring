package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.FastEditResult
import com.oleksandr.fastflow.domain.logic.FastEditValidator
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import javax.inject.Inject

/**
 * Rewrites a fast's times.
 *
 * Editing an end time can change a day's status and therefore the streak, so
 * alarms are re-planned afterwards like any other write (SPEC 6, phase 4).
 */
class EditFastUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val clock: AppClock,
) {
    suspend operator fun invoke(fast: Fast): FastEditResult {
        val now = clock.nowMillis()
        val result = FastEditValidator.validate(fast.copy(updatedAt = now), now)
        if (result is FastEditResult.Valid) {
            fastRepository.update(result.fast)
            alarmScheduler.rescheduleAll()
            widgetUpdater.refresh()
        }
        return result
    }
}
