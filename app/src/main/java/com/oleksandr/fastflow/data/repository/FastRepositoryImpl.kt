package com.oleksandr.fastflow.data.repository

import com.oleksandr.fastflow.data.local.FastDao
import com.oleksandr.fastflow.data.local.toDomain
import com.oleksandr.fastflow.data.local.toEntity
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.repository.FastRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FastRepositoryImpl @Inject constructor(
    private val dao: FastDao,
) : FastRepository {

    override fun observeActive(): Flow<Fast?> = dao.observeActive().map { it?.toDomain() }

    override fun observeAll(): Flow<List<Fast>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeOverlapping(fromMillis: Long, toMillis: Long): Flow<List<Fast>> =
        dao.observeOverlapping(fromMillis, toMillis).map { rows -> rows.map { it.toDomain() } }

    override fun observeLastFinished(): Flow<Fast?> =
        dao.observeLastFinished().map { it?.toDomain() }

    override suspend fun getActive(): Fast? = dao.getActive()?.toDomain()

    override suspend fun getLastFinished(): Fast? = dao.getLastFinished()?.toDomain()

    override suspend fun getById(id: Long): Fast? = dao.getById(id)?.toDomain()

    override suspend fun getAll(): List<Fast> = dao.getAll().map { it.toDomain() }

    override suspend fun insert(fast: Fast): Long = dao.insert(fast.toEntity())

    override suspend fun update(fast: Fast) = dao.update(fast.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)
}
