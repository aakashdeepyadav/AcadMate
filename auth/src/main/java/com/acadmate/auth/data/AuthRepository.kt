package com.acadmate.auth.data

interface AuthRepository {
    suspend fun sendOtp(phoneNumber: String): Result<Unit>
    suspend fun verifyOtp(phoneNumber: String, otp: String): Result<String>
    suspend fun loginWithGoogle(idToken: String): Result<String>
}
