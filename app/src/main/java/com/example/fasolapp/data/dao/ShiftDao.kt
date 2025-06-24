package com.example.fasolapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import com.example.fasolapp.data.entity.Shift

@Dao
interface ShiftDao {
    @Insert
    suspend fun insertShift(shift: Shift)

    @Update
    suspend fun updateShift(shift: Shift)

    @Delete
    suspend fun deleteShift(shift: Shift)

    @Query("SELECT * FROM shifts WHERE employeeId = :employeeId AND endTime IS NULL")
    suspend fun getActiveShift(employeeId: Int): Shift?

    @Query("SELECT * FROM shifts WHERE employeeId = :employeeId AND startTime >= :startTime AND endTime <= :endTime")
    suspend fun getShiftsInRange(employeeId: Int, startTime: Long, endTime: Long): List<Shift>

    @Query("SELECT * FROM shifts")
    suspend fun getAllShifts(): List<Shift>
}