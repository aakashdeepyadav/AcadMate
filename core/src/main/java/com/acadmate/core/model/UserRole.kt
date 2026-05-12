package com.acadmate.core.model

enum class UserRole {
    ADMIN,
    FACULTY,
    STUDENT;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.uppercase()) {
                "ADMIN" -> ADMIN
                "FACULTY" -> FACULTY
                else -> STUDENT
            }
        }
    }
}
