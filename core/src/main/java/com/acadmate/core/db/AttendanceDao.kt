package com.acadmate.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAttendanceForUser(userId: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(records: List<AttendanceEntity>)

    @Query("SELECT * FROM attendance_records WHERE syncStatus = 0")
    suspend fun getUnsyncedRecords(): List<AttendanceEntity>

    @Query("UPDATE attendance_records SET syncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}
