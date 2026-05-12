package com.acadmate.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.acadmate.core.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val regNo: String? = null,
    val department: String? = null,
    val role: UserRole,
    val profilePictureUrl: String? = null,
    val address: String? = null,
    val isFirstLogin: Boolean = true,
    val institutionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
