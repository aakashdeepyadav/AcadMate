package com.acadmate.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Resource(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val subjectCode: String = "",
    val fileUrl: String = "",
    val fileType: String = "PDF",
    val uploadedBy: String = "",
    val facultyName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
