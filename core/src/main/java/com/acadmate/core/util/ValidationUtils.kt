package com.acadmate.core.util

import com.acadmate.core.model.UserRole

object ValidationUtils {

    fun isValidRegistrationNumber(regNo: String, role: UserRole): Boolean {
        return when (role) {
            UserRole.STUDENT -> regNo.length == 8 && regNo.all { it.isDigit() }
            UserRole.FACULTY -> regNo.length == 6 && regNo.all { it.isDigit() }
            UserRole.ADMIN -> regNo.length == 4 && regNo.all { it.isDigit() }
        }
    }

    fun getRegistrationNumberErrorMessage(role: UserRole): String {
        return when (role) {
            UserRole.STUDENT -> "Student Registration No must be 8 digits"
            UserRole.FACULTY -> "Faculty Registration No must be 6 digits"
            UserRole.ADMIN -> "Admin Registration No must be 4 digits"
        }
    }

    fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }

    fun getPasswordStrengthErrorMessage(): String {
        return "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character."
    }
}
