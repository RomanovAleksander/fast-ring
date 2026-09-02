package com.oleksandr.fastflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.oleksandr.fastflow.data.local.entities.DayMarkEntity
import com.oleksandr.fastflow.data.local.entities.FastEntity
import com.oleksandr.fastflow.data.local.entities.PlanEntity

/**
 * Schemas are exported from version 1 so future migrations can be written
 * without losing history (SPEC 8).
 */
@Database(
    entities = [FastEntity::class, PlanEntity::class, DayMarkEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fastDao(): FastDao
    abstract fun planDao(): PlanDao
    abstract fun dayMarkDao(): DayMarkDao

    companion object {
        const val NAME = "fastflow.db"
    }
}
