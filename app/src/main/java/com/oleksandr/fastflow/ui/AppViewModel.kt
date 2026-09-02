package com.oleksandr.fastflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state: which palette to paint with, and whether onboarding is done. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(),
    )

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingDone(true) }
    }
}
