package com.oleksandr.fastflow.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.logic.AlarmType
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import com.oleksandr.fastflow.domain.usecase.EndFastUseCase
import com.oleksandr.fastflow.domain.usecase.StartFastUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Turns a fired alarm into a notification, and runs the notification's action
 * buttons (SPEC 3.3).
 *
 * Stopping a fast from the notification must work without opening the app, so
 * the work happens here rather than in an Activity.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var fastRepository: FastRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var widgetUpdater: WidgetUpdater
    @Inject lateinit var startFast: StartFastUseCase
    @Inject lateinit var endFast: EndFastUseCase
    @Inject lateinit var clock: AppClock

    override fun onReceive(context: Context, intent: Intent) {
        // Broadcast receivers get about ten seconds; goAsync keeps the process
        // alive while the database work finishes.
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        scope.launch {
            try {
                notificationHelper.ensureChannels()
                when (intent.action) {
                    ACTION_STOP_FAST -> {
                        endFast()
                        notificationHelper.cancelOngoing()
                    }

                    ACTION_START_FAST -> startFast()

                    else -> handleAlarm(context, intent)
                }
                // Whatever happened, alarms and the widget must match state again.
                alarmScheduler.rescheduleAll()
                widgetUpdater.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleAlarm(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE)
            ?.let { name -> AlarmType.entries.firstOrNull { it.name == name } }
            ?: return
        val milestoneMinutes = intent.getIntExtra(EXTRA_MILESTONE_MINUTES, -1)

        when {
            milestoneMinutes > 0 -> notificationHelper.showEvent(
                id = NotificationHelper.ID_MILESTONE + milestoneMinutes,
                title = context.getString(
                    R.string.notif_milestone_title,
                    context.getString(
                        R.string.plan_extended_hours,
                        DurationFormat.hoursLabel(milestoneMinutes),
                    ),
                ),
                text = context.getString(R.string.notif_milestone_text),
            )

            type == AlarmType.FAST_TARGET_REACHED -> {
                val active = fastRepository.getActive() ?: return
                notificationHelper.showEvent(
                    id = NotificationHelper.ID_TARGET,
                    title = context.getString(R.string.notif_target_title),
                    text = context.getString(
                        R.string.notif_target_text,
                        DurationFormat.hhmm(active.targetMinutes * 60_000L),
                    ),
                )
            }

            type == AlarmType.EATING_WINDOW_ENDS_SOON -> {
                val minutes = settingsRepository.get().eatingEndReminderMinutes ?: return
                notificationHelper.showReminder(
                    id = NotificationHelper.ID_EATING_SOON,
                    title = context.getString(R.string.notif_eating_soon_title),
                    text = context.getString(
                        R.string.notif_eating_soon_text,
                        DurationFormat.compact(minutes * 60_000L),
                    ),
                )
            }

            type == AlarmType.EATING_WINDOW_ENDED -> {
                if (settingsRepository.get().autoStartNextFast) {
                    startFast()
                } else {
                    notificationHelper.showEvent(
                        id = NotificationHelper.ID_EATING_ENDED,
                        title = context.getString(R.string.notif_eating_ended_title),
                        text = context.getString(R.string.notif_eating_ended_text),
                        startAction = true,
                    )
                }
            }

            type == AlarmType.DAILY_REMINDER -> notificationHelper.showReminder(
                id = NotificationHelper.ID_DAILY,
                title = context.getString(R.string.notif_daily_title),
                text = context.getString(R.string.notif_daily_text),
            )

            type == AlarmType.STILL_FASTING_CHECK -> {
                if (fastRepository.getActive() == null) return
                notificationHelper.showEvent(
                    id = NotificationHelper.ID_STILL_FASTING,
                    title = context.getString(R.string.notif_still_fasting_title),
                    text = context.getString(R.string.notif_still_fasting_text),
                )
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "com.oleksandr.fastflow.action.ALARM"
        const val ACTION_STOP_FAST = "com.oleksandr.fastflow.action.STOP_FAST"
        const val ACTION_START_FAST = "com.oleksandr.fastflow.action.START_FAST"

        const val EXTRA_TYPE = "type"
        const val EXTRA_MILESTONE_MINUTES = "milestoneMinutes"
    }
}
