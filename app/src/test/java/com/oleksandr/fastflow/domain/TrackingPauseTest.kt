package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.FastStateResolver
import com.oleksandr.fastflow.domain.model.FastState
import com.oleksandr.fastflow.domain.model.FastingPlans
import com.oleksandr.fastflow.domain.usecase.SetTrackingPausedUseCase
import com.oleksandr.fastflow.domain.usecase.StartFastUseCase
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pausing tracking: the user wants to start the next fast later, or to stop
 * being tracked at all for a while.
 *
 * The distinction that matters is pause vs idle. Idle still counts the eating
 * window down and lets the next fast begin by itself; paused counts nothing
 * and starts nothing.
 */
class TrackingPauseTest {

    private val kyiv: ZoneId = ZoneId.of("Europe/Kyiv")
    private val start = LocalDateTime.parse("2025-03-10T20:00").atZone(kyiv).toInstant().toEpochMilli()
    private val plan = FastingPlans.default

    private fun resolve(
        activeFast: com.oleksandr.fastflow.domain.model.Fast? = null,
        lastFinished: com.oleksandr.fastflow.domain.model.Fast? = null,
        nowMillis: Long,
        paused: Boolean,
    ): FastState = FastStateResolver.resolve(
        activeFast = activeFast,
        activePlan = activeFast?.let { plan },
        lastFinished = lastFinished,
        lastPlan = lastFinished?.let { plan },
        nowMillis = nowMillis,
        trackingPaused = paused,
    )

    private fun finishedFast() = com.oleksandr.fastflow.domain.model.Fast(
        id = 1,
        planId = plan.id,
        targetMinutes = plan.fastingMinutes,
        eatingWindowMinutes = plan.eatingMinutes,
        startMillis = start,
        endMillis = start + plan.fastingMinutes * 60_000L,
        timeZoneId = kyiv.id,
        createdAt = start,
        updatedAt = start,
    )

    @Test
    fun pausingDuringTheEatingWindowStopsTheCountdown() {
        val finished = finishedFast()
        val oneHourIn = finished.endMillis!! + 60 * 60_000L

        assertTrue(resolve(lastFinished = finished, nowMillis = oneHourIn, paused = false) is FastState.Eating)
        assertEquals(
            FastState.Paused,
            resolve(lastFinished = finished, nowMillis = oneHourIn, paused = true),
        )
    }

    @Test
    fun resumingReturnsToTheWindowItLeftWhenItIsStillOpen() {
        val finished = finishedFast()
        val oneHourIn = finished.endMillis!! + 60 * 60_000L

        val resumed = resolve(lastFinished = finished, nowMillis = oneHourIn, paused = false)
        assertTrue(resumed is FastState.Eating)

        // ...and drops to idle rather than back into the window once it closed.
        val afterWindow = finished.endMillis!! + (plan.eatingMinutes!! + 1) * 60_000L
        assertEquals(FastState.Idle, resolve(lastFinished = finished, nowMillis = afterWindow, paused = false))
    }

    @Test
    fun anEmptyHistoryPausesToo() {
        assertEquals(FastState.Paused, resolve(nowMillis = start, paused = true))
        assertEquals(FastState.Idle, resolve(nowMillis = start, paused = false))
    }

    @Test
    fun aRunningFastIgnoresTheFlag() {
        val running = finishedFast().copy(endMillis = null)
        val state = resolve(activeFast = running, nowMillis = start + 60_000L, paused = true)
        assertTrue("a running fast must never be hidden by the pause", state is FastState.Fasting)
    }

    @Test
    fun startingAFastLiftsThePause() = runBlocking {
        val clock = FakeClock(start, kyiv)
        val fasts = InMemoryFastRepository()
        val plans = InMemoryPlanRepository()
        val settings = InMemorySettingsRepository()
        val alarms = RecordingAlarmScheduler()
        val widget = RecordingWidgetUpdater()

        SetTrackingPausedUseCase(settings, alarms, widget)(true)
        assertTrue(settings.get().trackingPaused)
        // Pausing has to re-plan, or the queue keeps the alarms it just voided.
        assertTrue(alarms.rescheduleCount > 0)

        StartFastUseCase(fasts, plans, settings, alarms, widget, clock)()
        assertFalse("starting is itself a resume", settings.get().trackingPaused)
    }
}
