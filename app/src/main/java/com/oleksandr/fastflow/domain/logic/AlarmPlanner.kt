package com.oleksandr.fastflow.domain.logic

import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.Milestones

/** Every kind of alarm the app schedules (SPEC 3.3). */
enum class AlarmType(val requestCode: Int) {
    FAST_TARGET_REACHED(1001),
    EATING_WINDOW_ENDS_SOON(1002),
    EATING_WINDOW_ENDED(1003),
    DAILY_REMINDER(1004),

    /** Overdue nudge at twice the goal (SPEC 8). */
    STILL_FASTING_CHECK(1005),
    ;

    companion object {
        /** Milestones get codes above the fixed ones, one per threshold. */
        const val MILESTONE_BASE_REQUEST_CODE = 2000

        fun milestoneRequestCode(minutes: Int): Int =
            MILESTONE_BASE_REQUEST_CODE + Milestones.thresholdsMinutes.indexOf(minutes)

        /** Every code the scheduler may have created, for a blanket cancel. */
        fun allRequestCodes(): List<Int> =
            entries.map { it.requestCode } +
                Milestones.thresholdsMinutes.indices.map { MILESTONE_BASE_REQUEST_CODE + it }
    }
}

/** One alarm to hand to AlarmManager. */
data class PlannedAlarm(
    val type: AlarmType,
    val requestCode: Int,
    val triggerAtMillis: Long,
    /** Set for [AlarmType.MILESTONE_BASE_REQUEST_CODE] entries. */
    val milestoneMinutes: Int? = null,
)

/**
 * Works out which alarms an app state needs.
 *
 * Pure on purpose: SPEC 7 case 12 requires `rescheduleAll()` to be idempotent,
 * and that is far easier to prove on a function than on AlarmManager.
 */
object AlarmPlanner {

    /**
     * @param activeFast the running fast, if any.
     * @param eatingWindowEndMillis when the current eating window closes, if one is open.
     * @param dailyReminderMillis next daily reminder instant, if enabled.
     * @param eatingEndReminderMinutes lead time for the "window closes soon" alarm; null = off.
     */
    fun plan(
        activeFast: Fast?,
        nowMillis: Long,
        eatingWindowEndMillis: Long? = null,
        dailyReminderMillis: Long? = null,
        eatingEndReminderMinutes: Int? = 60,
        milestonesEnabled: Boolean = true,
    ): List<PlannedAlarm> {
        val alarms = mutableListOf<PlannedAlarm>()

        if (activeFast != null) {
            val targetAt = activeFast.startMillis + activeFast.targetMinutes * 60_000L
            if (targetAt > nowMillis) {
                alarms += PlannedAlarm(
                    AlarmType.FAST_TARGET_REACHED,
                    AlarmType.FAST_TARGET_REACHED.requestCode,
                    targetAt,
                )
            }

            // "Still fasting?" at double the goal (SPEC 8).
            val overdueAt = activeFast.startMillis + activeFast.targetMinutes * 2 * 60_000L
            if (overdueAt > nowMillis) {
                alarms += PlannedAlarm(
                    AlarmType.STILL_FASTING_CHECK,
                    AlarmType.STILL_FASTING_CHECK.requestCode,
                    overdueAt,
                )
            }

            if (milestonesEnabled) {
                Milestones.above(activeFast.targetMinutes).forEach { minutes ->
                    val at = activeFast.startMillis + minutes * 60_000L
                    if (at > nowMillis) {
                        alarms += PlannedAlarm(
                            type = AlarmType.FAST_TARGET_REACHED,
                            requestCode = AlarmType.milestoneRequestCode(minutes),
                            triggerAtMillis = at,
                            milestoneMinutes = minutes,
                        )
                    }
                }
            }
        }

        if (eatingWindowEndMillis != null) {
            if (eatingEndReminderMinutes != null) {
                val soonAt = eatingWindowEndMillis - eatingEndReminderMinutes * 60_000L
                if (soonAt > nowMillis) {
                    alarms += PlannedAlarm(
                        AlarmType.EATING_WINDOW_ENDS_SOON,
                        AlarmType.EATING_WINDOW_ENDS_SOON.requestCode,
                        soonAt,
                    )
                }
            }
            if (eatingWindowEndMillis > nowMillis) {
                alarms += PlannedAlarm(
                    AlarmType.EATING_WINDOW_ENDED,
                    AlarmType.EATING_WINDOW_ENDED.requestCode,
                    eatingWindowEndMillis,
                )
            }
        }

        if (dailyReminderMillis != null && dailyReminderMillis > nowMillis && activeFast == null) {
            alarms += PlannedAlarm(
                AlarmType.DAILY_REMINDER,
                AlarmType.DAILY_REMINDER.requestCode,
                dailyReminderMillis,
            )
        }

        return alarms.sortedBy { it.triggerAtMillis }
    }
}
