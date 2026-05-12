package com.acadmate.core.db

import androidx.room.TypeConverter
import com.acadmate.core.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.name
    }

    @TypeConverter
    fun toUserRole(role: String): UserRole {
        return UserRole.fromString(role)
    }
}
