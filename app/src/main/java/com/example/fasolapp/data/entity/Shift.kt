package com.example.fasolapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val startTime: Long,
    var endTime: Long? = null
)