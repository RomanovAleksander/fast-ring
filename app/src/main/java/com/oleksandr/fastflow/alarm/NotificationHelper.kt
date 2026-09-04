package com.oleksandr.fastflow.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.oleksandr.fastflow.MainActivity
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.domain.model.Fast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the notification channels and every notification the app posts
 * (SPEC 3.3).
 *
 * All channels are silent: the app vibrates instead, so a 3 a.m. milestone
 * never makes a sound.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        val ongoing = NotificationChannel(
            CHANNEL_ONGOING,
            context.getString(R.string.channel_ongoing_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_ongoing_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        val events = NotificationChannel(
            CHANNEL_EVENTS,
            context.getString(R.string.channel_events_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_events_desc)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }

        manager.createNotificationChannels(listOf(ongoing, events, reminders))
    }

    /**
     * The persistent timer notification.
     *
     * `setUsesChronometer` lets the system count the seconds, which is why the
     * app needs no foreground service (SPEC 2).
     */
    fun buildOngoing(fast: Fast, nowMillis: Long, targetReached: Boolean): Notification {
        val elapsedMillis = fast.elapsed(nowMillis).toMillis()
        val targetMillis = fast.targetMinutes * 60_000L

        val title = if (targetReached) {
            context.getString(R.string.notif_ongoing_overtime)
        } else {
            context.getString(R.string.notif_ongoing_title)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(fast.startMillis)
            .setShowWhen(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openAppIntent())
            .addAction(
                0,
                context.getString(R.string.notif_action_stop),
                broadcast(AlarmReceiver.ACTION_STOP_FAST, REQUEST_STOP_ACTION),
            )

        if (targetReached) {
            val overtimeMillis = elapsedMillis - targetMillis
            val nextMilestone = com.oleksandr.fastflow.domain.model.Milestones
                .next((elapsedMillis / 60_000L).toInt())
            builder.setContentText(
                if (nextMilestone != null) {
                    context.getString(
                        R.string.notif_ongoing_detail,
                        DurationFormat.hhmm(elapsedMillis),
                        DurationFormat.compact(overtimeMillis),
                        context.getString(
                            R.string.plan_extended_hours,
                            DurationFormat.hoursLabel(nextMilestone),
                        ),
                        DurationFormat.compact(nextMilestone * 60_000L - elapsedMillis),
                    )
                } else {
                    DurationFormat.hhmm(elapsedMillis)
                },
            )
        }

        return builder.build()
    }

    fun showOngoing(fast: Fast, nowMillis: Long, targetReached: Boolean) {
        notify(ID_ONGOING, buildOngoing(fast, nowMillis, targetReached))
    }

    fun cancelOngoing() = manager.cancel(ID_ONGOING)

    /** Goal reached, milestones and window events (SPEC 3.3). */
    fun showEvent(id: Int, title: String, text: String, startAction: Boolean = false) {
        val builder = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent())

        if (startAction) {
            builder.addAction(
                0,
                context.getString(R.string.notif_action_start),
                broadcast(AlarmReceiver.ACTION_START_FAST, REQUEST_START_ACTION),
            )
        }
        notify(id, builder.build())
    }

    fun showReminder(id: Int, title: String, text: String) {
        notify(
            id,
            NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setSilent(true)
                .setContentIntent(openAppIntent())
                .build(),
        )
    }

    /**
     * Posts only if the user granted notifications; on Android 13+ a missing
     * grant throws otherwise.
     */
    private fun notify(id: Int, notification: Notification) {
        if (!manager.areNotificationsEnabled()) return
        runCatching { manager.notify(id, notification) }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN_APP,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun broadcast(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val CHANNEL_ONGOING = "ch_ongoing"
        const val CHANNEL_EVENTS = "ch_events"
        const val CHANNEL_REMINDERS = "ch_reminders"

        const val ID_ONGOING = 1
        const val ID_TARGET = 2
        const val ID_MILESTONE = 3
        const val ID_EATING_SOON = 4
        const val ID_EATING_ENDED = 5
        const val ID_DAILY = 6
        const val ID_STILL_FASTING = 7

        private const val REQUEST_OPEN_APP = 900
        private const val REQUEST_STOP_ACTION = 901
        private const val REQUEST_START_ACTION = 902

        /** SPEC 3.3: 0-200-100-200. */
        private val VIBRATION_PATTERN = longArrayOf(0, 200, 100, 200)
    }
}
