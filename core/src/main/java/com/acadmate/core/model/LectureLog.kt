package com.acadmate.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LectureLog(
    val id: String = "",
    val facultyId: String = "",
    val subjectId: String = "",
    val topicCovered: String = "",
    val unitTitle: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val assignmentAttachedId: String? = null
)
