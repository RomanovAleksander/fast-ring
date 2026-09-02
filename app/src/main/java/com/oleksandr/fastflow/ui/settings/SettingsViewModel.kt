package com.oleksandr.fastflow.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oleksandr.fastflow.data.export.HistoryJson
import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.HistoryCsv
import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.model.FastingPlan
import com.oleksandr.fastflow.domain.model.ThemePalette
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.PlanRepository
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val plans: List<FastingPlan> = emptyList(),
) {
    val activePlan: FastingPlan?
        get() = plans.firstOrNull { it.id == settings.activePlanId }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val planRepository: PlanRepository,
    private val fastRepository: FastRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observe(),
        planRepository.observeAll(),
    ) { appSettings, planList -> SettingsUiState(appSettings, planList) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setActivePlan(id: String) = launchAndReschedule { settingsRepository.setActivePlanId(id) }

    fun setAutoStart(enabled: Boolean) = launchAndReschedule { settingsRepository.setAutoStartNextFast(enabled) }

    fun setEatingEndReminder(minutes: Int?) =
        launchAndReschedule { settingsRepository.setEatingEndReminderMinutes(minutes) }

    fun setDailyReminder(minuteOfDay: Int?) =
        launchAndReschedule { settingsRepository.setDailyReminderMinuteOfDay(minuteOfDay) }

    fun setMilestones(enabled: Boolean) = launchAndReschedule { settingsRepository.setMilestonesEnabled(enabled) }

    fun setPalette(palette: ThemePalette) {
        viewModelScope.launch { settingsRepository.setPalette(palette) }
    }

    fun setUse24HourClock(use24: Boolean?) {
        viewModelScope.launch { settingsRepository.setUse24HourClock(use24) }
    }

    suspend fun exportCsv(): String = HistoryCsv.export(fastRepository.getAll())

    suspend fun exportJson(): String =
        HistoryJson.export(fastRepository.getAll(), clock.nowMillis())

    /** Appends imported records; existing history is left alone. */
    suspend fun importJson(text: String): Result<Int> = runCatching {
        val imported = HistoryJson.import(text)
        imported.forEach { fastRepository.insert(it) }
        alarmScheduler.rescheduleAll()
        imported.size
    }

    suspend fun createCustomPlan(fastingMinutes: Int, eatingMinutes: Int?) {
        val plan = FastingPlan(
            id = FastingPlan.CUSTOM_ID,
            name = FastingPlan.CUSTOM_ID,
            fastingMinutes = fastingMinutes,
            eatingMinutes = eatingMinutes,
            isPreset = false,
            sortOrder = Int.MAX_VALUE,
        )
        planRepository.upsert(plan)
        settingsRepository.setActivePlanId(plan.id)
        alarmScheduler.rescheduleAll()
    }

    /**
     * Any setting that feeds the alarm plan has to re-plan afterwards, or the
     * change only takes effect at the next write (CLAUDE.md).
     */
    private fun launchAndReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            alarmScheduler.rescheduleAll()
        }
    }
}
