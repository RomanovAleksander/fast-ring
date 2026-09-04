package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.FastEditError
import com.oleksandr.fastflow.domain.logic.FastEditResult
import com.oleksandr.fastflow.domain.logic.FastEditValidator
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import javax.inject.Inject

sealed interface EndFastResult {
    data class Ended(val fast: Fast) : EndFastResult
    data object NoActiveFast : EndFastResult
    data class InvalidTime(val error: FastEditError) : EndFastResult
}

/** Stops the running fast; the eating window is derived from it afterwards. */
class EndFastUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val clock: AppClock,
) {
    suspend operator fun invoke(endMillis: Long? = null): EndFastResult {
        val active = fastRepository.getActive() ?: return EndFastResult.NoActiveFast
        val now = clock.nowMillis()

        val candidate = active.copy(endMillis = endMillis ?: now, updatedAt = now)

        return when (val validation = FastEditValidator.validate(candidate, now)) {
            is FastEditResult.Invalid -> EndFastResult.InvalidTime(validation.error)
            is FastEditResult.Valid -> {
                fastRepository.update(validation.fast)
                alarmScheduler.rescheduleAll()
                widgetUpdater.refresh()
                EndFastResult.Ended(validation.fast)
            }
        }
    }
}
