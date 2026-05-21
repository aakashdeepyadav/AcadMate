package com.acadmate.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "semester_results",
    indices = [Index(value = ["userId"])]
)
@Serializable
data class SemesterResultEntity(
    @PrimaryKey val id: String, // Semester ID (e.g., "6")
    val userId: String,
    val semesterName: String,
    val sgpa: Float,
    val credits: Int,
    val subjects: List<SubjectGradeEntity>
)

@Serializable
data class SubjectGradeEntity(
    val subjectName: String = "",
    val code: String = "",
    val grade: String = "",
    val credits: Int = 0
)
