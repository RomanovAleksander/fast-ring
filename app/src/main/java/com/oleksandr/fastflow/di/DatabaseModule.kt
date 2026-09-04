package com.oleksandr.fastflow.di

import android.content.Context
import androidx.room.Room
import com.oleksandr.fastflow.data.local.AppDatabase
import com.oleksandr.fastflow.data.local.DayMarkDao
import com.oleksandr.fastflow.data.local.FastDao
import com.oleksandr.fastflow.data.local.PlanDao
import com.oleksandr.fastflow.data.prefs.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideFastDao(database: AppDatabase): FastDao = database.fastDao()

    @Provides
    fun providePlanDao(database: AppDatabase): PlanDao = database.planDao()

    @Provides
    fun provideDayMarkDao(database: AppDatabase): DayMarkDao = database.dayMarkDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)
}
