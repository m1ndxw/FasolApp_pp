package com.example.fasolapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fasolapp.data.entity.CompletedTask

@Dao
interface CompletedTaskDao {
    @Insert
    suspend fun insertCompletedTask(completedTask: CompletedTask)

    @Query("SELECT COUNT(*) FROM completed_tasks WHERE employeeId = :employeeId AND completionDate BETWEEN :startTime AND :endTime")
    suspend fun countCompletedTasks(employeeId: Int, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM completed_tasks WHERE taskId = :taskId AND employeeId = :employeeId")
    suspend fun countCompletedTask(taskId: Int, employeeId: Int): Int

    @Query("SELECT * FROM completed_tasks WHERE employeeId = :employeeId AND completionDate >= :startTime")
    suspend fun getCompletedTasksByUser(employeeId: Int, startTime: Long): List<CompletedTask>
}