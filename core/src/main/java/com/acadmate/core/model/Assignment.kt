package com.acadmate.core.model

data class Assignment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val subjectId: String = "",
    val facultyId: String = "",
    val dueDate: Long = 0,
    val fileUrl: String? = null,
    val guidelineUrls: List<String> = emptyList(), // PDF/DOC guidelines
    val createdAt: Long = System.currentTimeMillis()
)

data class Submission(
    val id: String = "",
    val assignmentId: String = "",
    val studentId: String = "",
    val fileUrl: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val grade: String? = null,
    val feedback: String? = null
)
