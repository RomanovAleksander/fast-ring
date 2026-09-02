package com.oleksandr.fastflow.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Manual day marks. Reserved for the v1.1 rest-day feature (SPEC 3.4); the
 * table exists from v1 so adding it later needs no migration.
 */
@Entity(tableName = "day_marks")
data class DayMarkEntity(
    /** ISO `yyyy-MM-dd`. */
    @PrimaryKey val date: String,
    val mark: String,
)
