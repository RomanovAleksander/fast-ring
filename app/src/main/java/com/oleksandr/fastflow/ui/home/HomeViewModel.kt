package com.oleksandr.fastflow.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.FastScoring
import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.FastState
import com.oleksandr.fastflow.domain.model.Milestones
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import com.oleksandr.fastflow.domain.usecase.ComputeDayStatusesUseCase
import com.oleksandr.fastflow.domain.usecase.EditFastUseCase
import com.oleksandr.fastflow.domain.usecase.EndFastUseCase
import com.oleksandr.fastflow.domain.usecase.ObserveCurrentStateUseCase
import com.oleksandr.fastflow.domain.usecase.StartFastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeCurrentState: ObserveCurrentStateUseCase,
    computeDayStatuses: ComputeDayStatusesUseCase,
    settingsRepository: SettingsRepository,
    private val startFastUseCase: StartFastUseCase,
    private val endFastUseCase: EndFastUseCase,
    private val editFastUseCase: EditFastUseCase,
    private val fastRepository: FastRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val stateFlow = observeCurrentState()

    /** Recomputed when the calendar date rolls over, not on every tick. */
    private val weekFlow = stateFlow
        .map { clock.today() }
        .distinctUntilChanged()
        .flatMapLatest { today ->
            val monday = today.with(DayOfWeek.MONDAY)
            computeDayStatuses(monday, monday.plusDays(6)).map { it.values.toList() }
        }

    val uiState: StateFlow<HomeUiState> = combine(
        stateFlow,
        weekFlow,
        settingsRepository.observe(),
    ) { state, week, settings ->
        toUiState(state, week, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    fun startFast(startMillis: Long? = null) {
        viewModelScope.launch { startFastUseCase(startMillis = startMillis) }
    }

    fun endFast(endMillis: Long? = null) {
        viewModelScope.launch { endFastUseCase(endMillis = endMillis) }
    }

    /** Moves the running fast's start time ("I started at 20:00"). */
    fun editStart(startMillis: Long) {
        viewModelScope.launch {
            val active = fastRepository.getActive() ?: return@launch
            editFastUseCase(active.copy(startMillis = startMillis))
        }
    }

    private fun toUiState(
        state: FastState,
        week: List<DayInfo>,
        settings: AppSettings,
    ): HomeUiState {
        val now = clock.nowMillis()
        val base = HomeUiState(
            week = week,
            use24HourClock = settings.use24HourClock,
            nowMillis = now,
        )

        return when (state) {
            FastState.Idle -> base.copy(
                phase = HomePhase.IDLE,
                planId = settings.activePlanId,
            )

            is FastState.Fasting -> {
                val elapsed = state.fast.elapsed(now).toMillis()
                val target = state.fast.targetMinutes * 60_000L
                base.copy(
                    phase = HomePhase.FASTING,
                    planId = state.plan.id,
                    planName = state.plan.name,
                    planFastingMinutes = state.plan.fastingMinutes,
                    planEatingMinutes = state.plan.eatingMinutes,
                    elapsedMillis = elapsed,
                    targetMillis = target,
                    remainingMillis = (target - elapsed).coerceAtLeast(0L),
                    outerProgress = state.fast.completionRatio(now),
                    // The eating ring stays an empty track until the goal lands.
                    innerProgress = 0f,
                    showInnerRing = !state.plan.isExtended,
                    startMillis = state.fast.startMillis,
                    plannedEndMillis = state.fast.startMillis + target,
                    // What stopping right now would cost, and how to win it back.
                    stopEarlyDeadlineMillis = FastScoring.compensationDeadlineMillis(
                        state.fast.copy(endMillis = now),
                    ),
                )
            }

            is FastState.Overtime -> {
                val elapsed = state.fast.elapsed(now).toMillis()
                val target = state.fast.targetMinutes * 60_000L
                val elapsedMinutes = (elapsed / 60_000L).toInt()
                val nextMilestone = Milestones.next(elapsedMinutes)
                val previousMilestone = Milestones.thresholdsMinutes
                    .lastOrNull { it <= elapsedMinutes } ?: state.fast.targetMinutes

                base.copy(
                    phase = HomePhase.OVERTIME,
                    planId = state.plan.id,
                    planName = state.plan.name,
                    planFastingMinutes = state.plan.fastingMinutes,
                    planEatingMinutes = state.plan.eatingMinutes,
                    elapsedMillis = elapsed,
                    targetMillis = target,
                    overtimeMillis = elapsed - target,
                    outerProgress = 1f,
                    // Inner ring now runs from the last milestone to the next.
                    innerProgress = nextMilestone?.let { next ->
                        val span = (next - previousMilestone).coerceAtLeast(1)
                        ((elapsedMinutes - previousMilestone).toFloat() / span).coerceIn(0f, 1f)
                    } ?: 1f,
                    showInnerRing = nextMilestone != null,
                    nextMilestoneMinutes = nextMilestone,
                    startMillis = state.fast.startMillis,
                    plannedEndMillis = state.fast.startMillis + target,
                )
            }

            is FastState.Eating -> {
                val windowMillis = (state.previousFast.eatingWindowMinutes ?: 0) * 60_000L
                val remaining = (state.windowEndsAtMillis - now).coerceAtLeast(0L)
                base.copy(
                    phase = HomePhase.EATING,
                    planId = state.plan.id,
                    planName = state.plan.name,
                    planFastingMinutes = state.plan.fastingMinutes,
                    planEatingMinutes = state.plan.eatingMinutes,
                    outerProgress = 1f,
                    // Counts down: starts full and melts away (SPEC 5.2).
                    innerProgress = if (windowMillis > 0) {
                        (remaining.toFloat() / windowMillis).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    showInnerRing = true,
                    eatingEndsAtMillis = state.windowEndsAtMillis,
                    eatingRemainingMillis = remaining,
                    creditDeadlineMillis = state.creditDeadlineMillis,
                    startMillis = state.previousFast.startMillis,
                    plannedEndMillis = state.previousFast.endMillis,
                )
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
