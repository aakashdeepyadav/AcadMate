package com.acadmate.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assignments",
    indices = [
        Index(value = ["facultyId"]),
        Index(value = ["dueDate"])
    ]
)
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val subjectId: String,
    val facultyId: String,
    val dueDate: Long,
    val fileUrl: String? = null,
    val createdAt: Long
)
