package com.acadmate.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "syllabus_gaps")
data class SyllabusGapEntity(
    @PrimaryKey val topic: String,
    val coverage: Float,
    val status: String, // "Covered", "Partial", "Missing"
    val subjectId: String
)
