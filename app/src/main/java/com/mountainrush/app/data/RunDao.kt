package com.mountainrush.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY startTime DESC")
    fun observeAll(): Flow<List<RunSession>>

    @Query("SELECT * FROM runs ORDER BY startTime DESC")
    suspend fun getAll(): List<RunSession>

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getById(id: Long): RunSession?

    @Insert
    suspend fun insert(run: RunSession): Long

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
