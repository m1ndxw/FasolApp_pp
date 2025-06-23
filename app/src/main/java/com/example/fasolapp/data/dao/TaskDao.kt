package com.example.fasolapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fasolapp.data.entity.Task

@Dao
interface TaskDao {
    @Insert
    suspend fun insertTasks(tasks: List<Task>)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}