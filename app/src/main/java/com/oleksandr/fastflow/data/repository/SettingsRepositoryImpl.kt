package com.oleksandr.fastflow.data.repository

import com.oleksandr.fastflow.data.prefs.SettingsDataStore
import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.model.ThemePalette
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsDataStore,
) : SettingsRepository {

    override fun observe(): Flow<AppSettings> = store.settings

    override suspend fun get(): AppSettings = store.settings.first()

    override suspend fun setActivePlanId(id: String) = store.setActivePlanId(id)

    override suspend fun setAutoStartNextFast(enabled: Boolean) =
        store.setAutoStartNextFast(enabled)

    override suspend fun setEatingEndReminderMinutes(minutes: Int?) =
        store.setEatingEndReminderMinutes(minutes)

    override suspend fun setDailyReminderMinuteOfDay(minuteOfDay: Int?) =
        store.setDailyReminderMinuteOfDay(minuteOfDay)

    override suspend fun setMilestonesEnabled(enabled: Boolean) =
        store.setMilestonesEnabled(enabled)

    override suspend fun setPalette(palette: ThemePalette) = store.setPalette(palette)

    override suspend fun setUse24HourClock(use24: Boolean?) = store.setUse24HourClock(use24)

    override suspend fun setOnboardingDone(done: Boolean) = store.setOnboardingDone(done)

    override suspend fun setBatteryHintShown(shown: Boolean) = store.setBatteryHintShown(shown)
}
