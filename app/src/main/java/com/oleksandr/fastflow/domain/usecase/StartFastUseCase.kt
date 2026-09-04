package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.FastEditError
import com.oleksandr.fastflow.domain.logic.FastEditResult
import com.oleksandr.fastflow.domain.logic.FastEditValidator
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastingPlan
import com.oleksandr.fastflow.domain.model.FastingPlans
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.PlanRepository
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import javax.inject.Inject

sealed interface StartFastResult {
    data class Started(val fast: Fast) : StartFastResult

    /** Only one fast may run at a time (SPEC 3.2). */
    data object AlreadyRunning : StartFastResult

    data class InvalidTime(val error: FastEditError) : StartFastResult
}

/**
 * Begins a fast, freezing the plan's numbers onto the record so later plan
 * edits never rewrite history.
 */
class StartFastUseCase @Inject constructor(
    private val fastRepository: FastRepository,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val clock: AppClock,
) {
    /**
     * @param startMillis when the fast began; defaults to now. The user may
     *   backdate it up to seven days ("I started at 20:00").
     */
    suspend operator fun invoke(
        startMillis: Long? = null,
        planId: String? = null,
    ): StartFastResult {
        if (fastRepository.getActive() != null) return StartFastResult.AlreadyRunning

        val settings = settingsRepository.get()
        val plan = resolvePlan(planId ?: settings.activePlanId)
        val now = clock.nowMillis()

        val candidate = Fast(
            planId = plan.id,
            targetMinutes = plan.fastingMinutes,
            eatingWindowMinutes = plan.eatingMinutes,
            startMillis = startMillis ?: now,
            endMillis = null,
            timeZoneId = clock.zone().id,
            createdAt = now,
            updatedAt = now,
        )

        // A backdated start must still land after the previous fast finished.
        val previousEnd = fastRepository.getLastFinished()?.endMillis

        return when (val validation = FastEditValidator.validate(candidate, now, previousEnd)) {
            is FastEditResult.Invalid -> StartFastResult.InvalidTime(validation.error)
            is FastEditResult.Valid -> {
                val id = fastRepository.insert(validation.fast)
                // Starting is the clearest possible "I am tracking again", so
                // it lifts the pause rather than leaving a running fast that
                // the paused state would hide.
                if (settings.trackingPaused) settingsRepository.setTrackingPaused(false)
                // Every write is followed by a full reschedule (CLAUDE.md).
                alarmScheduler.rescheduleAll()
                widgetUpdater.refresh()
                StartFastResult.Started(validation.fast.copy(id = id))
            }
        }
    }

    private suspend fun resolvePlan(id: String): FastingPlan =
        planRepository.getById(id) ?: FastingPlans.byId(id) ?: FastingPlans.default
}
