package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import javax.inject.Inject

/**
 * Puts tracking on hold, or takes it off hold again.
 *
 * Pausing is not a fast edit, but it changes what should be scheduled just as
 * much as one does, so it goes through the same reschedule-then-refresh path
 * (CLAUDE.md): while paused the queue stays empty and the widget shows it.
 */
class SetTrackingPausedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(paused: Boolean) {
        settingsRepository.setTrackingPaused(paused)
        alarmScheduler.rescheduleAll()
        widgetUpdater.refresh()
    }
}
