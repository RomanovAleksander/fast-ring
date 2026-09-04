package com.oleksandr.fastflow.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Stored fast (SPEC 4.1). Times are epoch millis in UTC. */
@Entity(
    tableName = "fasts",
    indices = [Index("startMillis"), Index("endMillis")],
)
data class FastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: String,
    /** Goal frozen at start, so editing a plan never rewrites history. */
    val targetMinutes: Int,
    /** `null` for extended plans. */
    val eatingWindowMinutes: Int?,
    val startMillis: Long,
    /** `null` means the fast is still running. */
    val endMillis: Long?,
    /** Zone at start, so the calendar survives travel (SPEC 7, case 10). */
    val timeZoneId: String,
    @ColumnInfo(defaultValue = "NULL") val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
