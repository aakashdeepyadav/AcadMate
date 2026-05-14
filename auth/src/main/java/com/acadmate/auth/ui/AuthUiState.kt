package com.acadmate.auth.ui

import com.acadmate.core.model.UserRole

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class OtpSent(val phoneNumber: String) : AuthUiState()
    data class Verified(val token: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class RequirePhoneVerification(val userId: String, val phoneNumber: String) : AuthUiState()
    data class RequirePhoneInput(val userId: String, val maskedPhone: String) : AuthUiState()
    data class PasswordResetSent(val message: String) : AuthUiState()
}
