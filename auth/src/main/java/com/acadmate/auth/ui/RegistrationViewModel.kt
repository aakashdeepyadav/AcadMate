package com.acadmate.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

import com.acadmate.core.model.UserRole
import com.acadmate.core.util.ValidationUtils
import com.acadmate.core.datastore.OnboardingDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val onboardingDataStore: OnboardingDataStore
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState

    private var currentPhoneNumber: String? = null

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification or instant verification
            viewModelScope.launch {
                try {
                    val result = auth.signInWithCredential(credential).await()
                    if (result.user != null) {
                        _uiState.value = RegistrationUiState.ProfileSetup(currentPhoneNumber ?: "", result.user!!.uid)
                    }
                } catch (e: Exception) {
                    _uiState.value = RegistrationUiState.Error("Auto-verification failed: ${e.message}")
                }
            }
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            _uiState.value = RegistrationUiState.Error("Verification failed: ${e.message}")
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            this@RegistrationViewModel.verificationId = verificationId
            this@RegistrationViewModel.resendToken = token
            _uiState.value = RegistrationUiState.OtpSent(verificationId)
        }
    }

    fun registerWithPhone(phone: String, role: UserRole, activity: android.app.Activity) {
        if (phone.length != 10) {
            _uiState.value = RegistrationUiState.Error("Invalid phone number")
            return
        }

        currentPhoneNumber = phone
        _uiState.value = RegistrationUiState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtpForRegistration(otp: String, phoneNumber: String) {
        if (otp.length != 6) {
            _uiState.value = RegistrationUiState.Error("Invalid OTP")
            return
        }

        if (verificationId == null) {
            _uiState.value = RegistrationUiState.Error("Verification ID not found")
            return
        }

        _uiState.value = RegistrationUiState.Loading
        viewModelScope.launch {
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                val result = auth.signInWithCredential(credential).await()

                if (result.user != null) {
                    _uiState.value = RegistrationUiState.ProfileSetup(phoneNumber, result.user!!.uid)
                } else {
                    _uiState.value = RegistrationUiState.Error("Verification failed")
                }
            } catch (e: Exception) {
                _uiState.value = RegistrationUiState.Error("Verification error: ${e.message}")
            }
        }
    }

    fun completeProfileSetup(
        name: String,
        email: String,
        password: String,
        enrollmentNumber: String?,
        department: String?,
        role: UserRole,
        profilePictureUrl: String? = null
    ) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _uiState.value = RegistrationUiState.Error("Please fill all required fields")
            return
        }

        // Validate Password Strength
        if (!ValidationUtils.isStrongPassword(password)) {
            _uiState.value = RegistrationUiState.Error(ValidationUtils.getPasswordStrengthErrorMessage())
            return
        }

        // Validate Institutional ID Requirements (Enhanced Alphanumeric Rules)
        if (enrollmentNumber == null || !ValidationUtils.isValidRegistrationNumber(enrollmentNumber, role)) {
            _uiState.value = RegistrationUiState.Error(ValidationUtils.getRegistrationNumberErrorMessage(role))
            return
        }

        _uiState.value = RegistrationUiState.Loading
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.value = RegistrationUiState.Error("User not authenticated")
                    return@launch
                }

                // Link Email and Password for future Institutional Login
                try {
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                    currentUser.linkWithCredential(credential).await()
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("already been linked", ignoreCase = true) || 
                        msg.contains("provider-already-linked", ignoreCase = true)) {
                        // Already linked, we can proceed
                        android.util.Log.d("RegistrationViewModel", "Email already linked, proceeding")
                    } else if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                         // Email already in use by ANOTHER account
                         _uiState.value = RegistrationUiState.Error("This email is already registered with another account")
                         return@launch
                    } else {
                        throw e
                    }
                }

                val result = userRepository.saveUserProfile(
                    userId = currentUser.uid,
                    name = name,
                    email = email,
                    phoneNumber = currentUser.phoneNumber ?: "",
                    enrollmentNumber = enrollmentNumber,
                    department = department,
                    role = role.name.lowercase(),
                    profilePictureUrl = profilePictureUrl
                )

                if (result.isSuccess) {
                    onboardingDataStore.setOnboardingCompleted(true)
                    _uiState.value = RegistrationUiState.Success(
                        token = "registration_token_${System.currentTimeMillis()}",
                        userId = currentUser.uid
                    )
                } else {
                    _uiState.value = RegistrationUiState.Error("Failed to save profile: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = RegistrationUiState.Error("Profile setup error: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = RegistrationUiState.Idle
    }
}
