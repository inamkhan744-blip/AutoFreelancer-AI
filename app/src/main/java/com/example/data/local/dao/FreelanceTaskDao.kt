package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FreelanceTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Freelance Tasks in Room Database.
 */
@Dao
interface FreelanceTaskDao {

    @Query("SELECT * FROM freelance_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<FreelanceTaskEntity>>

    @Query("SELECT * FROM freelance_tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: String): Flow<List<FreelanceTaskEntity>>

    @Query("SELECT * FROM freelance_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): FreelanceTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: FreelanceTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<FreelanceTaskEntity>)

    @Update
    suspend fun updateTask(task: FreelanceTaskEntity)

    @Query("UPDATE freelance_tasks SET status = :newStatus, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTaskStatus(id: String, newStatus: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTask(task: FreelanceTaskEntity)

    @Query("DELETE FROM freelance_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM freelance_tasks")
    suspend fun clearAllTasks()
}
