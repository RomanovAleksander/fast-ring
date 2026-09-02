package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import javax.inject.Inject

/**
 * Puts a deleted fast back after an undo.
 *
 * This inserts rather than updates: the row is already gone, and re-inserting
 * with the original id keeps history stable.
 */
class RestoreFastUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(fast: Fast) {
        fastRepository.insert(fast)
        alarmScheduler.rescheduleAll()
        widgetUpdater.refresh()
    }
}
