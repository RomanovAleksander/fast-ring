package com.oleksandr.fastflow.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.model.FastingPlan
import com.oleksandr.fastflow.domain.model.ThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Preference-backed settings store (SPEC 4.1). */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ACTIVE_PLAN_ID = stringPreferencesKey("activePlanId")
        val AUTO_START_NEXT = booleanPreferencesKey("autoStartNextFast")
        val EATING_END_REMINDER = intPreferencesKey("eatingEndReminderMin")
        val DAILY_REMINDER = intPreferencesKey("dailyReminderMinuteOfDay")
        val MILESTONES = booleanPreferencesKey("milestonesEnabled")
        val PALETTE = stringPreferencesKey("palette")
        val CLOCK_24H = booleanPreferencesKey("use24HourClock")
        val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
        val BATTERY_HINT_SHOWN = booleanPreferencesKey("batteryHintShown")
    }

    /** Sentinel for "off", since a preferences key is either absent or set. */
    private companion object {
        const val OFF = -1
        const val DEFAULT_EATING_END_REMINDER_MINUTES = 60
    }

    val settings: Flow<AppSettings> = context.preferencesStore.data.map { prefs ->
        AppSettings(
            activePlanId = prefs[Keys.ACTIVE_PLAN_ID] ?: FastingPlan.DEFAULT_ID,
            autoStartNextFast = prefs[Keys.AUTO_START_NEXT] ?: false,
            // Three distinct cases: never set (use the default), explicitly
            // switched off, or a real value. Collapsing the first two made
            // "off" read back as the default, so the row looked stuck.
            eatingEndReminderMinutes = when (val stored = prefs[Keys.EATING_END_REMINDER]) {
                null -> DEFAULT_EATING_END_REMINDER_MINUTES
                OFF -> null
                else -> stored
            },
            dailyReminderMinuteOfDay = prefs[Keys.DAILY_REMINDER]?.takeIf { it != OFF },
            milestonesEnabled = prefs[Keys.MILESTONES] ?: true,
            palette = ThemePalette.fromName(prefs[Keys.PALETTE]),
            use24HourClock = prefs[Keys.CLOCK_24H],
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            batteryHintShown = prefs[Keys.BATTERY_HINT_SHOWN] ?: false,
        )
    }

    suspend fun setActivePlanId(id: String) = edit { it[Keys.ACTIVE_PLAN_ID] = id }

    suspend fun setAutoStartNextFast(enabled: Boolean) = edit { it[Keys.AUTO_START_NEXT] = enabled }

    suspend fun setEatingEndReminderMinutes(minutes: Int?) =
        edit { it[Keys.EATING_END_REMINDER] = minutes ?: OFF }

    suspend fun setDailyReminderMinuteOfDay(minuteOfDay: Int?) =
        edit { it[Keys.DAILY_REMINDER] = minuteOfDay ?: OFF }

    suspend fun setMilestonesEnabled(enabled: Boolean) = edit { it[Keys.MILESTONES] = enabled }

    suspend fun setPalette(palette: ThemePalette) = edit { it[Keys.PALETTE] = palette.name }

    suspend fun setUse24HourClock(use24: Boolean?) = edit { prefs ->
        if (use24 == null) prefs.remove(Keys.CLOCK_24H) else prefs[Keys.CLOCK_24H] = use24
    }

    suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.ONBOARDING_DONE] = done }

    suspend fun setBatteryHintShown(shown: Boolean) = edit { it[Keys.BATTERY_HINT_SHOWN] = shown }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.preferencesStore.edit(block)
    }
}
