package com.acadmate.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val subject: String,
    val faculty: String,
    val timestamp: Long, // Epoch millis for easier Room storage
    val status: String, // PRESENT, ABSENT, LATE
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val syncStatus: Int = 0 // 0: Not synced, 1: Synced
)
