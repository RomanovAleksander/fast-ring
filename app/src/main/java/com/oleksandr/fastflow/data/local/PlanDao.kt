package com.oleksandr.fastflow.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oleksandr.fastflow.data.local.entities.PlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM plans ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getById(id: String): PlanEntity?

    @Query("SELECT COUNT(*) FROM plans")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(plan: PlanEntity)

    @Upsert
    suspend fun upsertAll(plans: List<PlanEntity>)

    @Query("DELETE FROM plans WHERE id = :id AND isPreset = 0")
    suspend fun deleteCustom(id: String)
}
