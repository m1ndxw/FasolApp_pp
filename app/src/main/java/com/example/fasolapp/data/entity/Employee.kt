package com.example.fasolapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val login: String,
    val password: String,
    val role: String // "Cashier" or "Manager"
)