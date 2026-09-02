package com.oleksandr.fastflow.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.HistoryEntry
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import com.oleksandr.fastflow.domain.usecase.DeleteFastUseCase
import com.oleksandr.fastflow.domain.usecase.EditFastUseCase
import com.oleksandr.fastflow.domain.usecase.ObserveHistoryUseCase
import com.oleksandr.fastflow.domain.usecase.RestoreFastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** History grouped into months, newest first (SPEC 5.3). */
data class HistoryUiState(
    val months: List<HistoryMonth> = emptyList(),
    val use24HourClock: Boolean? = null,
    val recentlyDeleted: Fast? = null,
) {
    val isEmpty: Boolean get() = months.isEmpty()
}

data class HistoryMonth(
    val month: YearMonth,
    val entries: List<HistoryEntry>,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeHistory: ObserveHistoryUseCase,
    settingsRepository: SettingsRepository,
    private val deleteFastUseCase: DeleteFastUseCase,
    private val editFastUseCase: EditFastUseCase,
    private val restoreFastUseCase: RestoreFastUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        observeHistory(),
        settingsRepository.observe(),
    ) { entries, settings -> toUiState(entries, settings) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    fun delete(fast: Fast) {
        viewModelScope.launch { deleteFastUseCase(fast.id) }
    }

    /** Puts a swiped-away record back (SPEC 6, phase 4). */
    fun restore(fast: Fast) {
        viewModelScope.launch { restoreFastUseCase(fast) }
    }

    fun update(fast: Fast) {
        viewModelScope.launch { editFastUseCase(fast) }
    }

    private fun toUiState(entries: List<HistoryEntry>, settings: AppSettings) = HistoryUiState(
        months = entries
            .groupBy { entry ->
                val date = entry.fast.startDate
                YearMonth.of(date.year, date.month)
            }
            .toSortedMap(compareByDescending { it })
            .map { (month, monthEntries) -> HistoryMonth(month, monthEntries) },
        use24HourClock = settings.use24HourClock,
    )
}
