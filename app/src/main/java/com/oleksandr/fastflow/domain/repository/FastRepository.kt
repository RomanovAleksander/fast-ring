package com.oleksandr.fastflow.domain.repository

import com.oleksandr.fastflow.domain.model.Fast
import kotlinx.coroutines.flow.Flow

interface FastRepository {
    /** The running fast, or `null`. Only one may be active at a time (SPEC 3.2). */
    fun observeActive(): Flow<Fast?>

    fun observeAll(): Flow<List<Fast>>

    /** Fasts overlapping the half-open range, for the calendar (SPEC 4.1). */
    fun observeOverlapping(fromMillis: Long, toMillis: Long): Flow<List<Fast>>

    /** Most recently finished fast, used to work out the eating window. */
    fun observeLastFinished(): Flow<Fast?>

    suspend fun getActive(): Fast?

    suspend fun getLastFinished(): Fast?

    suspend fun getById(id: Long): Fast?

    suspend fun getAll(): List<Fast>

    suspend fun insert(fast: Fast): Long

    suspend fun update(fast: Fast)

    suspend fun delete(id: Long)
}
