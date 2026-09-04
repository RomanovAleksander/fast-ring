package com.oleksandr.fastflow.domain.repository

import com.oleksandr.fastflow.domain.model.FastingPlan
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun observeAll(): Flow<List<FastingPlan>>

    suspend fun getById(id: String): FastingPlan?

    suspend fun upsert(plan: FastingPlan)

    /** Writes the built-in protocols on first launch (SPEC 6, phase 1). */
    suspend fun seedPresets()
}
