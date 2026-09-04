package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.FastEditError
import com.oleksandr.fastflow.domain.logic.FastStateResolver
import com.oleksandr.fastflow.domain.model.FastState
import com.oleksandr.fastflow.domain.model.FastingPlans
import com.oleksandr.fastflow.domain.usecase.EndFastUseCase
import com.oleksandr.fastflow.domain.usecase.StartFastResult
import com.oleksandr.fastflow.domain.usecase.StartFastUseCase
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Starting a fast at the moment it really began.
 *
 * "I had lunch, then decided to skip dinner, and only pressed start at eight" —
 * the fast began at lunch, and the record has to say so or every total after it
 * is short.
 */
class BackdatedStartTest {

    private val kyiv: ZoneId = ZoneId.of("Europe/Kyiv")
    private val plan = FastingPlans.default

    private fun at(text: String): Long =
        LocalDateTime.parse(text).atZone(kyiv).toInstant().toEpochMilli()

    private class Harness(
        val clock: FakeClock,
        val fasts: InMemoryFastRepository,
        val start: StartFastUseCase,
        val end: EndFastUseCase,
    )

    private fun harness(now: Long): Harness {
        val clock = FakeClock(now, kyiv)
        val fasts = InMemoryFastRepository()
        val plans = InMemoryPlanRepository()
        val settings = InMemorySettingsRepository()
        val alarms = RecordingAlarmScheduler()
        val widget = RecordingWidgetUpdater()
        return Harness(
            clock = clock,
            fasts = fasts,
            start = StartFastUseCase(fasts, plans, settings, alarms, widget, clock),
            end = EndFastUseCase(fasts, alarms, widget, clock),
        )
    }

    @Test
    fun theRecordKeepsTheChosenStartNotTheMomentOfPressing() = runBlocking {
        val h = harness(at("2025-03-10T20:00"))

        val result = h.start(startMillis = at("2025-03-10T13:00"))

        assertTrue(result is StartFastResult.Started)
        assertEquals(at("2025-03-10T13:00"), h.fasts.current.single().startMillis)
    }

    /**
     * Seven hours of it had already passed when the button was pressed, so a
     * 16-hour goal is 7/16 of the way in rather than at zero.
     */
    @Test
    fun elapsedTimeCountsFromTheChosenStart() = runBlocking {
        val pressedAt = at("2025-03-10T20:00")
        val h = harness(pressedAt)
        h.start(startMillis = at("2025-03-10T13:00"))

        val fast = h.fasts.current.single()
        assertEquals(7 * 60L, fast.elapsed(pressedAt).toMinutes())
        assertEquals(plan.fastingMinutes, fast.targetMinutes)
    }

    /** Backdating far enough puts the goal behind you the moment you start. */
    @Test
    fun aStartOldEnoughToPassTheGoalResolvesStraightToOvertime() = runBlocking {
        val pressedAt = at("2025-03-11T20:00")
        val h = harness(pressedAt)
        h.start(startMillis = at("2025-03-11T02:00")) // 18 h ago, goal is 16 h

        val state = FastStateResolver.resolve(
            activeFast = h.fasts.current.single(),
            activePlan = plan,
            lastFinished = null,
            lastPlan = null,
            nowMillis = pressedAt,
        )
        assertTrue(state is FastState.Overtime)
    }

    @Test
    fun aStartInTheFutureIsRefused() = runBlocking {
        val h = harness(at("2025-03-10T20:00"))

        val result = h.start(startMillis = at("2025-03-10T22:00"))

        assertEquals(StartFastResult.InvalidTime(FastEditError.START_IN_FUTURE), result)
        assertTrue("nothing may be recorded", h.fasts.current.isEmpty())
    }

    @Test
    fun aStartOlderThanTheBackdateLimitIsRefused() = runBlocking {
        val h = harness(at("2025-03-20T20:00"))

        val result = h.start(startMillis = at("2025-03-01T20:00"))

        assertEquals(StartFastResult.InvalidTime(FastEditError.START_TOO_FAR_BACK), result)
    }

    /**
     * Two fasts covering the same hours would be counted twice in the totals,
     * and the compensation rule would read the overlap as an instant restart.
     */
    @Test
    fun aStartBeforeThePreviousFastEndedIsRefused() = runBlocking {
        val h = harness(at("2025-03-10T20:00"))
        h.start(startMillis = at("2025-03-09T20:00"))
        h.clock.now = at("2025-03-10T12:00")
        h.end()

        h.clock.now = at("2025-03-10T20:00")
        val result = h.start(startMillis = at("2025-03-10T10:00")) // inside the finished fast

        assertEquals(StartFastResult.InvalidTime(FastEditError.OVERLAPS_PREVIOUS), result)
        assertEquals("only the finished fast survives", 1, h.fasts.current.size)

        // One minute after it ended is fine.
        val ok = h.start(startMillis = at("2025-03-10T12:01"))
        assertTrue(ok is StartFastResult.Started)
    }
}
