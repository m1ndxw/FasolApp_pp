package com.example.fasolapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fasolapp.data.entity.Task

@Dao
interface TaskDao {
    @Query("INSERT INTO tasks (name, startTime, endTime) VALUES (:name, :startTime, :endTime)")
    suspend fun insertTask(name: String, startTime: String, endTime: String)

    suspend fun insertTasks(tasks: List<Task>) {
        tasks.forEach { insertTask(it.name, it.startTime, it.endTime) }
    }

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}