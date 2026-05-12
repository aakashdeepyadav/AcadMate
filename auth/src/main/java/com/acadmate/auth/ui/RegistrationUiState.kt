package com.acadmate.auth.ui

sealed class RegistrationUiState {
    object Idle : RegistrationUiState()
    object Loading : RegistrationUiState()
    data class OtpSent(val verificationId: String) : RegistrationUiState()
    data class ProfileSetup(val phoneNumber: String, val userId: String) : RegistrationUiState()
    data class Success(val token: String, val userId: String) : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()
}
