package com.oleksandr.fastflow.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.oleksandr.fastflow.data.local.entities.DayMarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayMarkDao {

    @Query("SELECT * FROM day_marks")
    fun observeAll(): Flow<List<DayMarkEntity>>

    @Upsert
    suspend fun upsert(mark: DayMarkEntity)

    @Query("DELETE FROM day_marks WHERE date = :date")
    suspend fun delete(date: String)
}
