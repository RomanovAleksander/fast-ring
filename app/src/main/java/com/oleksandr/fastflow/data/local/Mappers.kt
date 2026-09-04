package com.oleksandr.fastflow.data.local

import com.oleksandr.fastflow.data.local.entities.FastEntity
import com.oleksandr.fastflow.data.local.entities.PlanEntity
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastingPlan

/** Room rows are storage; the domain models are what the app reasons about. */

fun FastEntity.toDomain(): Fast = Fast(
    id = id,
    planId = planId,
    targetMinutes = targetMinutes,
    eatingWindowMinutes = eatingWindowMinutes,
    startMillis = startMillis,
    endMillis = endMillis,
    timeZoneId = timeZoneId,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Fast.toEntity(): FastEntity = FastEntity(
    id = id,
    planId = planId,
    targetMinutes = targetMinutes,
    eatingWindowMinutes = eatingWindowMinutes,
    startMillis = startMillis,
    endMillis = endMillis,
    timeZoneId = timeZoneId,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PlanEntity.toDomain(): FastingPlan = FastingPlan(
    id = id,
    name = name,
    fastingMinutes = fastingMinutes,
    eatingMinutes = eatingMinutes,
    isPreset = isPreset,
    sortOrder = sortOrder,
)

fun FastingPlan.toEntity(): PlanEntity = PlanEntity(
    id = id,
    name = name,
    fastingMinutes = fastingMinutes,
    eatingMinutes = eatingMinutes,
    isPreset = isPreset,
    sortOrder = sortOrder,
)
