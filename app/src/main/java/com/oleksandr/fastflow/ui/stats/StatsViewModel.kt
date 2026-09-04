package com.oleksandr.fastflow.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.FastStats
import com.oleksandr.fastflow.domain.model.HistoryEntry
import com.oleksandr.fastflow.domain.model.Streak
import com.oleksandr.fastflow.domain.usecase.ComputeDayStatusesUseCase
import com.oleksandr.fastflow.domain.usecase.ComputeStatsUseCase
import com.oleksandr.fastflow.domain.usecase.ComputeStreakUseCase
import com.oleksandr.fastflow.domain.usecase.ObserveHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val streak: Streak = Streak(),
    val stats: FastStats = FastStats(),
    /** Newest first; the chart takes the most recent slice. */
    val recent: List<HistoryEntry> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val days: Map<LocalDate, DayInfo> = emptyMap(),
    /** Last twelve weeks, for the heatmap (SPEC 3.4). */
    val heatmap: Map<LocalDate, DayInfo> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
    val nowMillis: Long = 0L,
) {
    val isEmpty: Boolean get() = stats.totalFasts == 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    computeStreak: ComputeStreakUseCase,
    computeStats: ComputeStatsUseCase,
    observeHistory: ObserveHistoryUseCase,
    private val computeDayStatuses: ComputeDayStatusesUseCase,
    private val clock: AppClock,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(clock.today()))

    /**
     * The month grid and the twelve-week heatmap in one stream, so the screen's
     * combine stays within the five-flow overload.
     */
    private val daysFlow = month.flatMapLatest { value ->
        val today = clock.today()
        val heatmapStart = today.minusWeeks(HEATMAP_WEEKS).with(DayOfWeek.MONDAY)
        combine(
            computeDayStatuses(value.atDay(1), value.atEndOfMonth()),
            computeDayStatuses(heatmapStart, today),
        ) { monthDays, heatmapDays -> monthDays to heatmapDays }
    }

    val uiState: StateFlow<StatsUiState> = combine(
        computeStreak(),
        computeStats(),
        observeHistory(),
        daysFlow,
        month,
    ) { streak, stats, history, (days, heatmap), currentMonth ->
        StatsUiState(
            streak = streak,
            stats = stats,
            recent = history,
            month = currentMonth,
            days = days,
            heatmap = heatmap,
            today = clock.today(),
            nowMillis = clock.nowMillis(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(month = YearMonth.from(clock.today()), today = clock.today()),
    )

    /** Jumps the calendar to a month, used when arriving from the week strip. */
    fun showMonth(target: YearMonth) {
        month.value = target
    }

    fun showPreviousMonth() {
        month.value = month.value.minusMonths(1)
    }

    private companion object {
        const val HEATMAP_WEEKS = 12L
    }

    fun showNextMonth() {
        // Browsing past the current month would only show empty cells.
        val limit = YearMonth.from(clock.today())
        if (month.value < limit) month.value = month.value.plusMonths(1)
    }
}
