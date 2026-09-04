package com.oleksandr.fastflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.oleksandr.fastflow.data.local.entities.FastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FastDao {

    @Query("SELECT * FROM fasts WHERE endMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    fun observeActive(): Flow<FastEntity?>

    @Query("SELECT * FROM fasts WHERE endMillis IS NULL ORDER BY startMillis DESC LIMIT 1")
    suspend fun getActive(): FastEntity?

    @Query("SELECT * FROM fasts ORDER BY startMillis DESC")
    fun observeAll(): Flow<List<FastEntity>>

    @Query("SELECT * FROM fasts ORDER BY startMillis DESC")
    suspend fun getAll(): List<FastEntity>

    /** Calendar query from SPEC 4.1: everything overlapping the month. */
    @Query(
        """
        SELECT * FROM fasts
        WHERE startMillis < :toMillis AND (endMillis IS NULL OR endMillis > :fromMillis)
        ORDER BY startMillis DESC
        """,
    )
    fun observeOverlapping(fromMillis: Long, toMillis: Long): Flow<List<FastEntity>>

    @Query("SELECT * FROM fasts WHERE endMillis IS NOT NULL ORDER BY endMillis DESC LIMIT 1")
    fun observeLastFinished(): Flow<FastEntity?>

    @Query("SELECT * FROM fasts WHERE endMillis IS NOT NULL ORDER BY endMillis DESC LIMIT 1")
    suspend fun getLastFinished(): FastEntity?

    @Query("SELECT * FROM fasts WHERE id = :id")
    suspend fun getById(id: Long): FastEntity?

    @Insert
    suspend fun insert(fast: FastEntity): Long

    @Update
    suspend fun update(fast: FastEntity)

    @Query("DELETE FROM fasts WHERE id = :id")
    suspend fun delete(id: Long)
}
