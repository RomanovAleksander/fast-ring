package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.FastEditError
import com.oleksandr.fastflow.domain.logic.FastStateResolver
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastState
import com.oleksandr.fastflow.domain.model.FastingPlans
import com.oleksandr.fastflow.domain.usecase.EndFastResult
import com.oleksandr.fastflow.domain.usecase.EndFastUseCase
import com.oleksandr.fastflow.domain.usecase.StartFastResult
import com.oleksandr.fastflow.domain.usecase.StartFastUseCase
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The IDLE → FASTING → OVERTIME → EATING → IDLE walk from SPEC 3.2, which the
 * phase-2 checklist asks to confirm by hand on the device.
 */
class FastLifecycleTest {

    private val kyiv: ZoneId = ZoneId.of("Europe/Kyiv")
    private val start = LocalDateTime.parse("2025-03-10T20:00").atZone(kyiv).toInstant().toEpochMilli()

    private val plan16to8 = FastingPlans.default

    private fun harness(now: Long = start): Harness {
        val clock = FakeClock(now, kyiv)
        val fasts = InMemoryFastRepository()
        val plans = InMemoryPlanRepository()
        val settings = InMemorySettingsRepository()
        val alarms = RecordingAlarmScheduler()
        val widget = RecordingWidgetUpdater()
        return Harness(
            clock = clock,
            fasts = fasts,
            alarms = alarms,
            widget = widget,
            start = StartFastUseCase(fasts, plans, settings, alarms, widget, clock),
            end = EndFastUseCase(fasts, alarms, widget, clock),
        )
    }

    private class Harness(
        val clock: FakeClock,
        val fasts: InMemoryFastRepository,
        val alarms: RecordingAlarmScheduler,
        val widget: RecordingWidgetUpdater,
        val start: StartFastUseCase,
        val end: EndFastUseCase,
    )

    private fun state(h: Harness): FastState {
        val active = h.fasts.current.firstOrNull { it.isActive }
        val lastFinished = h.fasts.current.filter { !it.isActive }.maxByOrNull { it.endMillis ?: 0L }
        return FastStateResolver.resolve(
            activeFast = active,
            activePlan = plan16to8,
            lastFinished = lastFinished,
            lastPlan = plan16to8,
            nowMillis = h.clock.now,
        )
    }

    @Test
    fun `the full cycle walks idle to fasting to overtime to eating and back`() = runBlocking {
        val h = harness()
        assertTrue("starts idle", state(h) is FastState.Idle)

        assertTrue(h.start() is StartFastResult.Started)
        assertTrue("running", state(h) is FastState.Fasting)

        // One minute short of the 16h goal.
        h.clock.advanceMinutes(16 * 60 - 1)
        assertTrue("still short of the goal", state(h) is FastState.Fasting)

        h.clock.advanceMinutes(1)
        assertTrue("goal reached", state(h) is FastState.Overtime)

        // Keeps counting past the goal until the user stops it.
        h.clock.advanceMinutes(65)
        assertTrue("overtime persists", state(h) is FastState.Overtime)

        assertTrue(h.end() is EndFastResult.Ended)
        val eating = state(h)
        assertTrue("eating window opens", eating is FastState.Eating)
        assertEquals(
            h.clock.now + 8 * 60 * 60_000L,
            (eating as FastState.Eating).windowEndsAtMillis,
        )

        // The goal was met, so there is nothing to compensate.
        assertNull(eating.creditDeadlineMillis)

        h.clock.advanceMinutes(8 * 60)
        assertTrue("window closed, back to idle", state(h) is FastState.Idle)
    }

    @Test
    fun `a second fast cannot start while one is running`() = runBlocking {
        val h = harness()
        assertTrue(h.start() is StartFastResult.Started)
        assertEquals(StartFastResult.AlreadyRunning, h.start())
        assertEquals(1, h.fasts.current.size)
    }

    @Test
    fun `every write reschedules alarms and refreshes the widget`() = runBlocking {
        val h = harness()
        h.start()
        assertEquals(1, h.alarms.rescheduleCount)
        assertEquals(1, h.widget.refreshCount)

        h.clock.advanceMinutes(16 * 60)
        h.end()
        assertEquals(2, h.alarms.rescheduleCount)
        assertEquals(2, h.widget.refreshCount)
    }

    @Test
    fun `a rejected start writes nothing and schedules nothing`() = runBlocking {
        val h = harness()
        val eightDaysAgo = h.clock.now - 8 * 24 * 60 * 60 * 1000L

        val result = h.start(startMillis = eightDaysAgo)
        assertEquals(StartFastResult.InvalidTime(FastEditError.START_TOO_FAR_BACK), result)
        assertTrue(h.fasts.current.isEmpty())
        assertEquals(0, h.alarms.rescheduleCount)
    }

    @Test
    fun `a backdated start within a week is accepted`() = runBlocking {
        val h = harness()
        val sixHoursAgo = h.clock.now - 6 * 60 * 60_000L

        val result = h.start(startMillis = sixHoursAgo)
        assertTrue(result is StartFastResult.Started)
        assertEquals(sixHoursAgo, h.fasts.current.single().startMillis)
    }

    @Test
    fun `ending short of the goal exposes the compensation deadline`() = runBlocking {
        val h = harness()
        h.start()
        // 13h30m of a 16h goal: 2h30m short, so the window shrinks by that much.
        h.clock.advanceMinutes(13 * 60 + 30)
        h.end()

        val eating = state(h) as FastState.Eating
        val deadline = eating.creditDeadlineMillis
        assertNotNull("a short fast can still earn the day", deadline)
        assertEquals(h.clock.now + (8 * 60 - 150) * 60_000L, deadline)
    }

    @Test
    fun `an extended fast goes straight back to idle with no eating window`() = runBlocking {
        val h = harness()
        h.start(planId = "h36")

        h.clock.advanceMinutes(36 * 60)
        assertTrue(state(h) is FastState.Overtime)

        h.end()
        assertTrue("extended plans have no eating window", state(h) is FastState.Idle)
    }

    @Test
    fun `ending when nothing runs is refused`() = runBlocking {
        val h = harness()
        assertEquals(EndFastResult.NoActiveFast, h.end())
    }

    @Test
    fun `the fast freezes the plan numbers at start`() = runBlocking {
        val h = harness()
        h.start()
        val fast: Fast = h.fasts.current.single()
        assertEquals(16 * 60, fast.targetMinutes)
        assertEquals(8 * 60, fast.eatingWindowMinutes)
        assertEquals(kyiv.id, fast.timeZoneId)
    }
}
