package com.oleksandr.fastflow.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.AlarmPlanner
import com.oleksandr.fastflow.domain.logic.AlarmType
import com.oleksandr.fastflow.domain.logic.PlannedAlarm
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager-backed scheduler (SPEC 3.3).
 *
 * Idempotent by construction: every known request code is cancelled before
 * anything is planned again, so calling this twice cannot leave duplicates
 * (SPEC 7, case 12).
 */
@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fastRepository: FastRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
    private val clock: AppClock,
) : AlarmScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    override suspend fun rescheduleAll() {
        cancelAll()

        val now = clock.nowMillis()
        val active = fastRepository.getActive()
        val lastFinished = fastRepository.getLastFinished()
        val settings = settingsRepository.get()

        // Paused: cancelAll above already emptied the queue, and nothing goes
        // back in — no eating-window countdown, no daily nudge, no auto-start.
        if (settings.trackingPaused && active == null) {
            notificationHelper.cancelOngoing()
            return
        }

        // The eating window only exists between a finished daily fast and the
        // next one.
        val eatingWindowEnd = if (active == null && lastFinished != null) {
            val end = lastFinished.endMillis
            val window = lastFinished.eatingWindowMinutes
            if (end != null && window != null) (end + window * 60_000L).takeIf { it > now } else null
        } else {
            null
        }

        AlarmPlanner.plan(
            activeFast = active,
            nowMillis = now,
            eatingWindowEndMillis = eatingWindowEnd,
            dailyReminderMillis = settings.dailyReminderMinuteOfDay?.let { nextDailyReminder(it, now) },
            eatingEndReminderMinutes = settings.eatingEndReminderMinutes,
            milestonesEnabled = settings.milestonesEnabled,
        ).forEach(::schedule)

        // The ongoing notification mirrors the same state (SPEC 3.3).
        if (active != null) {
            notificationHelper.showOngoing(
                fast = active,
                nowMillis = now,
                targetReached = now >= active.startMillis + active.targetMinutes * 60_000L,
            )
        } else {
            notificationHelper.cancelOngoing()
        }
    }

    override fun cancelAll() {
        AlarmType.allRequestCodes().forEach { requestCode ->
            pendingIntent(requestCode, null, mutable = false, create = false)?.let { intent ->
                alarmManager.cancel(intent)
                intent.cancel()
            }
        }
    }

    private fun schedule(alarm: PlannedAlarm) {
        val intent = pendingIntent(
            requestCode = alarm.requestCode,
            configure = { it.putExtra(AlarmReceiver.EXTRA_TYPE, alarm.type.name)
                .putExtra(AlarmReceiver.EXTRA_MILESTONE_MINUTES, alarm.milestoneMinutes ?: -1) },
            mutable = false,
            create = true,
        ) ?: return

        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.triggerAtMillis,
                intent,
            )
        } else {
            // Falls back to an inexact alarm; the UI shows a banner offering to
            // turn exact alarms back on (SPEC 3.3).
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.triggerAtMillis,
                intent,
            )
        }
    }

    /**
     * True when the system will honour an exact alarm.
     *
     * From Android 13 the app holds USE_EXACT_ALARM, which is granted at
     * install for alarm-clock style apps and cannot be revoked.
     */
    fun canScheduleExact(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> true
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
        else -> true
    }

    private fun pendingIntent(
        requestCode: Int,
        configure: ((Intent) -> Intent)? = null,
        mutable: Boolean,
        create: Boolean,
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM)
            .let { configure?.invoke(it) ?: it }

        var flags = if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        flags = flags or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE

        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /** Today at the chosen minute, or tomorrow if that moment already passed. */
    private fun nextDailyReminder(minuteOfDay: Int, nowMillis: Long): Long {
        val zone = clock.zone()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        val candidate = today.atTime(time).atZone(zone).toInstant().toEpochMilli()
        return if (candidate > nowMillis) {
            candidate
        } else {
            today.plusDays(1).atTime(time).atZone(zone).toInstant().toEpochMilli()
        }
    }
}
