package com.oleksandr.fastflow.data.repository

import com.oleksandr.fastflow.data.local.PlanDao
import com.oleksandr.fastflow.data.local.toDomain
import com.oleksandr.fastflow.data.local.toEntity
import com.oleksandr.fastflow.domain.model.FastingPlan
import com.oleksandr.fastflow.domain.model.FastingPlans
import com.oleksandr.fastflow.domain.repository.PlanRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PlanRepositoryImpl @Inject constructor(
    private val dao: PlanDao,
) : PlanRepository {

    override fun observeAll(): Flow<List<FastingPlan>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: String): FastingPlan? = dao.getById(id)?.toDomain()

    override suspend fun upsert(plan: FastingPlan) = dao.upsert(plan.toEntity())

    /**
     * Writes the built-in protocols on first launch.
     *
     * Upsert rather than insert, so re-running it after an update refreshes
     * preset definitions without touching the user's custom plans.
     */
    override suspend fun seedPresets() {
        dao.upsertAll(FastingPlans.presets.map { it.toEntity() })
    }
}
