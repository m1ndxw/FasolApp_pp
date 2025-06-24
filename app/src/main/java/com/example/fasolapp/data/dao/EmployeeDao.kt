package com.example.fasolapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fasolapp.data.entity.Employee

@Dao
interface EmployeeDao {
    // Вставка нового сотрудника в БД
    @Insert
    suspend fun insertEmployee(employee: Employee)

    // Поиск сотрудника по логину и паролю
    @Query("SELECT * FROM employees WHERE login = :login AND password = :password")
    suspend fun getEmployeeByLogin(login: String, password: String): Employee?

    // Получение имени сотрудника по ID
    @Query("SELECT fullName FROM employees WHERE id = :employeeId")
    suspend fun getEmployeeNameById(employeeId: Int): String?

    @Query("SELECT * FROM employees")
    suspend fun getAllEmployees(): List<Employee>
}