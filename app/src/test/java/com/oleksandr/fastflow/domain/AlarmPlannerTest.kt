package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.AlarmPlanner
import com.oleksandr.fastflow.domain.logic.AlarmType
import com.oleksandr.fastflow.domain.model.Fast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC 7, case 12: rescheduling must not pile up duplicate alarms. */
class AlarmPlannerTest {

    private val now = 1_760_000_000_000L
    private val hour = 60 * 60 * 1000L

    private fun activeFast(targetMinutes: Int = 16 * 60) = Fast(
        planId = "p16_8",
        targetMinutes = targetMinutes,
        eatingWindowMinutes = 8 * 60,
        startMillis = now,
        endMillis = null,
        timeZoneId = "Europe/Kyiv",
    )

    @Test
    fun `planning twice yields exactly the same alarms`() {
        val first = AlarmPlanner.plan(activeFast(), now)
        val second = AlarmPlanner.plan(activeFast(), now)
        assertEquals(first, second)
    }

    @Test
    fun `request codes are unique so alarms replace rather than stack`() {
        val alarms = AlarmPlanner.plan(
            activeFast = activeFast(),
            nowMillis = now,
            eatingWindowEndMillis = now + 20 * hour,
            dailyReminderMillis = now + 30 * hour,
        )
        val codes = alarms.map { it.requestCode }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `every planned request code is one the scheduler knows how to cancel`() {
        val alarms = AlarmPlanner.plan(
            activeFast = activeFast(),
            nowMillis = now,
            eatingWindowEndMillis = now + 20 * hour,
        )
        val cancellable = AlarmType.allRequestCodes().toSet()
        assertTrue(alarms.all { it.requestCode in cancellable })
    }

    @Test
    fun `the target alarm fires exactly at the goal`() {
        val alarms = AlarmPlanner.plan(activeFast(), now)
        val target = alarms.first { it.type == AlarmType.FAST_TARGET_REACHED && it.milestoneMinutes == null }
        assertEquals(now + 16 * hour, target.triggerAtMillis)
    }

    @Test
    fun `only milestones beyond the goal are scheduled`() {
        val alarms = AlarmPlanner.plan(activeFast(targetMinutes = 16 * 60), now)
        val milestones = alarms.mapNotNull { it.milestoneMinutes }
        assertEquals(listOf(24 * 60, 36 * 60, 48 * 60, 72 * 60), milestones.sorted())
    }

    @Test
    fun `milestones can be switched off`() {
        val alarms = AlarmPlanner.plan(activeFast(), now, milestonesEnabled = false)
        assertTrue(alarms.none { it.milestoneMinutes != null })
    }

    @Test
    fun `past instants are never scheduled`() {
        val started = activeFast().copy(startMillis = now - 40 * hour)
        val alarms = AlarmPlanner.plan(started, now)
        assertTrue(alarms.all { it.triggerAtMillis > now })
    }

    @Test
    fun `the daily reminder is skipped while a fast is running`() {
        val withFast = AlarmPlanner.plan(activeFast(), now, dailyReminderMillis = now + 5 * hour)
        assertTrue(withFast.none { it.type == AlarmType.DAILY_REMINDER })

        val idle = AlarmPlanner.plan(null, now, dailyReminderMillis = now + 5 * hour)
        assertTrue(idle.any { it.type == AlarmType.DAILY_REMINDER })
    }

    @Test
    fun `the eating window warns ahead and then at close`() {
        val alarms = AlarmPlanner.plan(
            activeFast = null,
            nowMillis = now,
            eatingWindowEndMillis = now + 8 * hour,
            eatingEndReminderMinutes = 60,
        )
        assertEquals(
            now + 7 * hour,
            alarms.first { it.type == AlarmType.EATING_WINDOW_ENDS_SOON }.triggerAtMillis,
        )
        assertEquals(
            now + 8 * hour,
            alarms.first { it.type == AlarmType.EATING_WINDOW_ENDED }.triggerAtMillis,
        )
    }
}
