package com.acadmate.core.db

import androidx.room.TypeConverter
import com.acadmate.core.model.UserRole
import com.acadmate.core.model.SyllabusUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(role: String): UserRole = UserRole.fromString(role)

    @TypeConverter
    fun fromSyllabusUnitList(units: List<SyllabusUnit>): String = Json.encodeToString(units)

    @TypeConverter
    fun toSyllabusUnitList(data: String): List<SyllabusUnit> = Json.decodeFromString(data)

    @TypeConverter
    fun fromSubjectGradeList(grades: List<SubjectGradeEntity>): String = Json.encodeToString(grades)

    @TypeConverter
    fun toSubjectGradeList(data: String): List<SubjectGradeEntity> = Json.decodeFromString(data)
}
