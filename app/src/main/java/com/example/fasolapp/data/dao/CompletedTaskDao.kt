package com.example.fasolapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fasolapp.data.entity.CompletedTask

@Dao
interface CompletedTaskDao {
    @Insert
    suspend fun insertCompletedTask(completedTask: CompletedTask)

    @Query("SELECT * FROM completed_tasks WHERE employeeId = :employeeId AND completionDate >= :startTime AND completionDate <= :endTime")
    suspend fun getCompletedTasksInRange(employeeId: Int, startTime: Long, endTime: Long): List<CompletedTask>

    @Query("SELECT COUNT(*) FROM completed_tasks WHERE employeeId = :employeeId AND completionDate >= :startTime AND completionDate <= :endTime")
    suspend fun countCompletedTasks(employeeId: Int, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM completed_tasks WHERE completionDate >= :startTime AND completionDate <= :endTime")
    suspend fun countAllCompletedTasks(startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM completed_tasks WHERE taskId = :taskId AND employeeId = :employeeId")
    suspend fun countCompletedTask(taskId: Int, employeeId: Int): Int
}