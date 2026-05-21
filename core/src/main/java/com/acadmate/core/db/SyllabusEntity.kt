package com.acadmate.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.acadmate.core.model.SyllabusUnit

@Entity(tableName = "syllabuses")
data class SyllabusEntity(
    @PrimaryKey val subjectCode: String,
    val subjectName: String,
    val description: String,
    val credits: Int,
    val ltp: String,
    val units: List<SyllabusUnit>
)
