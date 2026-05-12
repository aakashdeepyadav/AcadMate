package com.acadmate.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable ORDER BY dayOfWeek, startTime")
    fun getAllTimetableItems(): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable WHERE dayOfWeek = :day ORDER BY startTime")
    fun getTimetableForDay(day: Int): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable WHERE dayOfWeek = :day ORDER BY startTime")
    suspend fun getTimetableForDaySync(day: Int): List<TimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(items: List<TimetableEntity>)

    @Query("UPDATE timetable SET isAlarmSet = :isAlarmSet WHERE id = :id")
    suspend fun updateAlarmStatus(id: String, isAlarmSet: Boolean)

    @Query("DELETE FROM timetable")
    suspend fun clearTimetable()
}
