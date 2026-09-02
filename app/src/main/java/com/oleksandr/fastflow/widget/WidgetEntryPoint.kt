package com.oleksandr.fastflow.widget

import android.content.Context
import com.oleksandr.fastflow.domain.AppClock
import com.oleksandr.fastflow.domain.repository.FastRepository
import com.oleksandr.fastflow.domain.repository.PlanRepository
import com.oleksandr.fastflow.domain.repository.SettingsRepository
import com.oleksandr.fastflow.domain.usecase.EndFastUseCase
import com.oleksandr.fastflow.domain.usecase.StartFastUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hilt access for Glance.
 *
 * A GlanceAppWidget is not an Android entry point Hilt can inject into, so the
 * widget pulls its dependencies out of the singleton graph by hand.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun fastRepository(): FastRepository
    fun planRepository(): PlanRepository
    fun settingsRepository(): SettingsRepository
    fun startFastUseCase(): StartFastUseCase
    fun endFastUseCase(): EndFastUseCase
    fun clock(): AppClock

    companion object {
        fun from(context: Context): WidgetEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
    }
}
