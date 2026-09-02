package com.oleksandr.fastflow.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String,
    /** Locale-neutral label such as "16:8"; the UI localises what it shows. */
    val name: String,
    val fastingMinutes: Int,
    /** `null` marks an extended plan with no eating window. */
    val eatingMinutes: Int?,
    val isPreset: Boolean,
    val sortOrder: Int,
)
