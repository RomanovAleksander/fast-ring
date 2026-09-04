package com.oleksandr.fastflow.di

import com.oleksandr.fastflow.alarm.AlarmSchedulerImpl
import com.oleksandr.fastflow.data.repository.FastRepositoryImpl
import com.oleksandr.fastflow.data.repository.PlanRepositoryImpl
import com.oleksandr.fastflow.data.repository.SettingsRepositoryImpl
import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.SystemAppClock
import com.oleksandr.fastflow.domain.repository.AlarmScheduler
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.PlanRepository
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import com.oleksandr.fastflow.widget.GlanceWidgetUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFastRepository(impl: FastRepositoryImpl): FastRepository

    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AlarmSchedulerImpl): AlarmScheduler

    @Binds
    @Singleton
    abstract fun bindWidgetUpdater(impl: GlanceWidgetUpdater): WidgetUpdater

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemAppClock): AppClock
}
