package com.oleksandr.fastflow

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.oleksandr.fastflow.alarm.NotificationHelper
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.PlanRepository
import com.oleksandr.fastflow.widget.WidgetUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class FastApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var planRepository: PlanRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            notificationHelper.ensureChannels()
            planRepository.seedPresets()
            // Alarms are re-planned on every launch: the system may have
            // dropped them, and a swiped-away ongoing notification comes back
            // here too (SPEC 3.3, SPEC 8).
            alarmScheduler.rescheduleAll()
        }

        WidgetUpdateWorker.enqueue(this)
    }
}
