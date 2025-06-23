package com.example.fasolapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fasolapp.data.entity.CompletedTask
import com.example.fasolapp.data.entity.Employee
import com.example.fasolapp.data.entity.Shift
import com.example.fasolapp.data.entity.Task
import com.example.fasolapp.data.dao.CompletedTaskDao
import com.example.fasolapp.data.dao.EmployeeDao
import com.example.fasolapp.data.dao.ShiftDao
import com.example.fasolapp.data.dao.TaskDao

@Database(entities = [Employee::class, Task::class, Shift::class, CompletedTask::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // Получение DAO для сотрудников
    abstract fun employeeDao(): EmployeeDao

    // Получение DAO для задач
    abstract fun taskDao(): TaskDao

    // Получение DAO для смен
    abstract fun shiftDao(): ShiftDao

    // Получение DAO для выполненных задач
    abstract fun completedTaskDao(): CompletedTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Создание или получение экземпляра базы данных
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fasol_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}