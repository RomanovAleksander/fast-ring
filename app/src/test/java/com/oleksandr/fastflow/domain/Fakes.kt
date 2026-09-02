package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastingPlan
import com.oleksandr.fastflow.domain.model.FastingPlans
import com.oleksandr.fastflow.domain.model.ThemePalette
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.PlanRepository
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A clock the test drives by hand. */
class FakeClock(var now: Long, private val zoneId: ZoneId = ZoneId.of("Europe/Kyiv")) : AppClock {
    override fun nowMillis(): Long = now

    override fun zone(): ZoneId = zoneId

    fun advanceMinutes(minutes: Long) {
        now += minutes * 60_000L
    }

    fun instant(): Instant = Instant.ofEpochMilli(now)
}

class InMemoryFastRepository(initial: List<Fast> = emptyList()) : FastRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1L

    val current: List<Fast> get() = state.value

    override fun observeActive(): Flow<Fast?> = state.map { list -> list.firstOrNull { it.isActive } }

    override fun observeAll(): Flow<List<Fast>> = state

    override fun observeOverlapping(fromMillis: Long, toMillis: Long): Flow<List<Fast>> =
        state.map { list ->
            list.filter { it.startMillis < toMillis && (it.endMillis ?: Long.MAX_VALUE) > fromMillis }
        }

    override fun observeLastFinished(): Flow<Fast?> =
        state.map { list -> list.filter { !it.isActive }.maxByOrNull { it.endMillis ?: 0L } }

    override suspend fun getActive(): Fast? = state.value.firstOrNull { it.isActive }

    override suspend fun getLastFinished(): Fast? =
        state.value.filter { !it.isActive }.maxByOrNull { it.endMillis ?: 0L }

    override suspend fun getById(id: Long): Fast? = state.value.firstOrNull { it.id == id }

    override suspend fun getAll(): List<Fast> = state.value

    override suspend fun insert(fast: Fast): Long {
        val id = nextId++
        state.value = state.value + fast.copy(id = id)
        return id
    }

    override suspend fun update(fast: Fast) {
        state.value = state.value.map { if (it.id == fast.id) fast else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}

class InMemoryPlanRepository(plans: List<FastingPlan> = FastingPlans.presets) : PlanRepository {
    private val state = MutableStateFlow(plans)

    override fun observeAll(): Flow<List<FastingPlan>> = state

    override suspend fun getById(id: String): FastingPlan? = state.value.firstOrNull { it.id == id }

    override suspend fun upsert(plan: FastingPlan) {
        state.value = state.value.filterNot { it.id == plan.id } + plan
    }

    override suspend fun seedPresets() {
        state.value = FastingPlans.presets
    }
}

class InMemorySettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override fun observe(): Flow<AppSettings> = state

    override suspend fun get(): AppSettings = state.value

    override suspend fun setActivePlanId(id: String) {
        state.value = state.value.copy(activePlanId = id)
    }

    override suspend fun setAutoStartNextFast(enabled: Boolean) {
        state.value = state.value.copy(autoStartNextFast = enabled)
    }

    override suspend fun setEatingEndReminderMinutes(minutes: Int?) {
        state.value = state.value.copy(eatingEndReminderMinutes = minutes)
    }

    override suspend fun setDailyReminderMinuteOfDay(minuteOfDay: Int?) {
        state.value = state.value.copy(dailyReminderMinuteOfDay = minuteOfDay)
    }

    override suspend fun setMilestonesEnabled(enabled: Boolean) {
        state.value = state.value.copy(milestonesEnabled = enabled)
    }

    override suspend fun setPalette(palette: ThemePalette) {
        state.value = state.value.copy(palette = palette)
    }

    override suspend fun setUse24HourClock(use24: Boolean?) {
        state.value = state.value.copy(use24HourClock = use24)
    }

    override suspend fun setOnboardingDone(done: Boolean) {
        state.value = state.value.copy(onboardingDone = done)
    }

    override suspend fun setBatteryHintShown(shown: Boolean) {
        state.value = state.value.copy(batteryHintShown = shown)
    }
}

/** Counts reschedules so tests can assert every write triggers exactly one. */
class RecordingAlarmScheduler : AlarmScheduler {
    var rescheduleCount = 0
        private set
    var cancelCount = 0
        private set

    override suspend fun rescheduleAll() {
        rescheduleCount++
    }

    override fun cancelAll() {
        cancelCount++
    }
}

class RecordingWidgetUpdater : WidgetUpdater {
    var refreshCount = 0
        private set

    override suspend fun refresh() {
        refreshCount++
    }
}
