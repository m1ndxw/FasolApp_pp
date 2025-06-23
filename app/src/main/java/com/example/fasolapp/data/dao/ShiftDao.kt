package com.example.fasolapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fasolapp.data.entity.Shift

@Dao
interface ShiftDao {
    // Вставка новой смены
    @Insert
    suspend fun insertShift(shift: Shift)

    // Обновление смены
    @Update
    suspend fun updateShift(shift: Shift)

    // Получение активной смены для сотрудника (без завершения)
    @Query("SELECT * FROM shifts WHERE employeeId = :employeeId AND endTime IS NULL")
    suspend fun getActiveShift(employeeId: Int): Shift?

    // Получение смен за период времени
    @Query("SELECT * FROM shifts WHERE employeeId = :employeeId AND startTime >= :startTime AND endTime <= :endTime")
    suspend fun getShiftsInRange(employeeId: Int, startTime: Long, endTime: Long): List<Shift>
}