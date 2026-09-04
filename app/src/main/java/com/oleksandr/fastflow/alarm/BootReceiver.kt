package com.oleksandr.fastflow.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Rebuilds every alarm after a reboot.
 *
 * AlarmManager forgets its queue across restarts, so without this a 36-hour
 * fast started before bedtime would silently never fire (SPEC 3.3).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                notificationHelper.ensureChannels()
                alarmScheduler.rescheduleAll()
                widgetUpdater.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            // Some OEMs, Samsung among them, send this instead after an update.
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
        )
    }
}
