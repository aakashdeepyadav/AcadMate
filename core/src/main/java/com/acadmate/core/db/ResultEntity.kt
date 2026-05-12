package com.acadmate.core.db

data class SemesterResultEntity(
    val id: String = "",
    val semesterName: String = "",
    val sgpa: Float = 0f,
    val credits: Int = 0,
    val subjects: List<SubjectGradeEntity> = emptyList()
)

data class SubjectGradeEntity(
    val subjectName: String = "",
    val code: String = "",
    val grade: String = "",
    val credits: Int = 0
)
