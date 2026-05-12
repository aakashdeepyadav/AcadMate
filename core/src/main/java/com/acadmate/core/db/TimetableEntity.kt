package com.acadmate.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey val id: String,
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)
    val subject: String,
    val faculty: String,
    val startTime: String, // "HH:mm"
    val endTime: String,
    val room: String,
    val color: Int,
    val isAlarmSet: Boolean = false
)
