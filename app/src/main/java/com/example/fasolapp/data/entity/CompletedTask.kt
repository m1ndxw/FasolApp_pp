package com.example.fasolapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "completed_tasks",
    foreignKeys = [
        ForeignKey(entity = Task::class, parentColumns = ["id"], childColumns = ["taskId"]),
        ForeignKey(entity = Employee::class, parentColumns = ["id"], childColumns = ["employeeId"])
    ])
data class CompletedTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val employeeId: Int,
    val completionDate: Long
)