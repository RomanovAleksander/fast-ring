package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import javax.inject.Inject

class DeleteFastUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(id: Long) {
        fastRepository.delete(id)
        alarmScheduler.rescheduleAll()
        widgetUpdater.refresh()
    }
}
