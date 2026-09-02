package com.oleksandr.fastflow.domain.repository

import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.model.ThemePalette
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>

    suspend fun get(): AppSettings

    suspend fun setActivePlanId(id: String)

    suspend fun setAutoStartNextFast(enabled: Boolean)

    suspend fun setEatingEndReminderMinutes(minutes: Int?)

    suspend fun setDailyReminderMinuteOfDay(minuteOfDay: Int?)

    suspend fun setMilestonesEnabled(enabled: Boolean)

    suspend fun setPalette(palette: ThemePalette)

    suspend fun setUse24HourClock(use24: Boolean?)

    suspend fun setOnboardingDone(done: Boolean)

    suspend fun setBatteryHintShown(shown: Boolean)
}
