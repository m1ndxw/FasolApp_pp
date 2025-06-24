package com.example.fasolapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.fasolapp.data.entity.Employee

@Dao
interface EmployeeDao {
    @Insert
    suspend fun insertEmployee(employee: Employee)

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("SELECT * FROM employees WHERE login = :login AND password = :password")
    suspend fun getEmployeeByLogin(login: String, password: String): Employee?

    @Query("SELECT fullName FROM employees WHERE id = :employeeId")
    suspend fun getEmployeeNameById(employeeId: Int): String?

    @Query("SELECT * FROM employees")
    suspend fun getAllEmployees(): List<Employee>
}