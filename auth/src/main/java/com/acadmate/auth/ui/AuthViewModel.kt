package com.acadmate.auth.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.core.model.UserRole
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.acadmate.core.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val onboardingDataStore: OnboardingDataStore,
    private val userRepository: com.acadmate.core.db.UserRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null
    private var verificationId: String? = null
    private var currentPhoneNumber: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    val selectedRole: StateFlow<UserRole?> = onboardingDataStore.selectedRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        initializeGoogleSignIn()
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification or instant verification
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            _uiState.value = AuthUiState.Error("Verification failed: ${e.message}")
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            this@AuthViewModel.verificationId = verificationId
            this@AuthViewModel.resendToken = token
            _uiState.value = AuthUiState.OtpSent("+91${currentPhoneNumber ?: "phone"}")
        }

        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            this@AuthViewModel.verificationId = verificationId
        }
    }

    private fun initializeGoogleSignIn() {
        val clientId = context.getString(com.acadmate.core.R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun loginWithRegNo(regNo: String, pass: String) {
        val cleanRegNo = regNo.trim()
        val cleanPass = pass.trim()

        if (cleanRegNo.isEmpty() || cleanPass.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter Reg No and password")
            return
        }
        
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val selectedRole = onboardingDataStore.selectedRole.first()
                if (selectedRole == null) {
                    _uiState.value = AuthUiState.Error("Role not selected")
                    return@launch
                }

                // Role-Specific Validation
                if (!ValidationUtils.isValidRegistrationNumber(cleanRegNo, selectedRole)) {
                    _uiState.value = AuthUiState.Error(ValidationUtils.getRegistrationNumberErrorMessage(selectedRole))
                    return@launch
                }

                // Password Strength Validation
                if (!ValidationUtils.isStrongPassword(cleanPass)) {
                    _uiState.value = AuthUiState.Error(ValidationUtils.getPasswordStrengthErrorMessage())
                    return@launch
                }

                val userDoc = firestore.collection("users")
                    .whereEqualTo("regNo", cleanRegNo)
                    .whereEqualTo("role", selectedRole.name)
                    .get()
                    .await()

                if (userDoc.isEmpty) {
                    _uiState.value = AuthUiState.Error("Account not found for this Role")
                    return@launch
                }

                val userData = userDoc.documents[0]
                val email = userData.getString("email") ?: ""
                val phoneNumber = userData.getString("phoneNumber") ?: ""
                val storedPassword = userData.getString("password") ?: ""
                val isFirstLogin = userData.getBoolean("isFirstLogin") ?: false

                if (email.isEmpty()) {
                    _uiState.value = AuthUiState.Error("Email not associated with this ID")
                    return@launch
                }

                // If it's an Admin-created user logging in for the first time
                if (isFirstLogin) {
                    if (cleanPass == storedPassword) {
                        val maskedPhone = if (phoneNumber.length >= 10) {
                            "******${phoneNumber.takeLast(4)}"
                        } else "registered phone"
                        
                        _uiState.value = AuthUiState.RequirePhoneInput("ACTIVATE:$cleanRegNo", maskedPhone)
                    } else {
                        _uiState.value = AuthUiState.Error("Incorrect password for institutional account")
                    }
                    return@launch
                }

                try {
                    val result = auth.signInWithEmailAndPassword(email, cleanPass).await()
                    if (result.user != null) {
                        userRepository.syncUserData(result.user!!.uid)
                        
                        val maskedPhone = if (phoneNumber.length >= 10) {
                            "******${phoneNumber.takeLast(4)}"
                        } else "linked phone"

                        _uiState.value = AuthUiState.RequirePhoneInput(result.user!!.uid, maskedPhone)
                    }
                } catch (e: Exception) {
                    val errorMsg = when {
                        e.message?.contains("password") == true -> "Incorrect password. Please try again."
                        e.message?.contains("user-not-found") == true -> "No account found for this email."
                        else -> e.message ?: "Authentication failed"
                    }
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("password") == true -> "Incorrect password. Please try again."
                    e.message?.contains("user-not-found") == true -> "No account found for this email."
                    else -> e.message ?: "Authentication failed"
                }
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        }
    }

    fun loginWithPhone(phone: String, activity: android.app.Activity) {
        // Clean the phone number: remove any non-digit characters
        val cleanPhone = phone.replace(Regex("[^\\d]"), "")
        
        // Extract last 10 digits for validation and prefixing
        val last10Digits = if (cleanPhone.length >= 10) cleanPhone.takeLast(10) else cleanPhone

        if (last10Digits.length != 10 || !last10Digits.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("Please enter a valid 10-digit phone number")
            return
        }

        currentPhoneNumber = last10Digits // Store the current phone number

        _uiState.value = AuthUiState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$last10Digits")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneAndSendOtp(inputPhone: String, userId: String, activity: android.app.Activity) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val userDoc = if (userId.startsWith("ACTIVATE:")) {
                    val regNo = userId.substringAfter("ACTIVATE:")
                    val query = firestore.collection("users").whereEqualTo("regNo", regNo).get().await()
                    query.documents.firstOrNull()
                } else {
                    firestore.collection("users").document(userId).get().await()
                }

                if (userDoc == null || !userDoc.exists()) {
                    _uiState.value = AuthUiState.Error("User record not found")
                    return@launch
                }

                val registeredPhone = userDoc.getString("phoneNumber") ?: ""
                
                val cleanInput = inputPhone.replace(Regex("[^\\d]"), "").takeLast(10)
                val cleanRegistered = registeredPhone.replace(Regex("[^\\d]"), "").takeLast(10)

                if (cleanInput == cleanRegistered) {
                    loginWithPhone(cleanInput, activity)
                } else {
                    _uiState.value = AuthUiState.Error("Entered phone number does not match registered one")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Verification failed: ${e.message}")
            }
        }
    }

    fun verifyOtp(otp: String) {
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("Please enter a valid 6-digit OTP")
            return
        }

        if (verificationId == null) {
            _uiState.value = AuthUiState.Error("Verification ID not found. Please request OTP again.")
            return
        }

        _uiState.value = AuthUiState.Loading

        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
        signInWithPhoneAuthCredential(credential)
    }

    fun resendOtp(phone: String, activity: android.app.Activity) {
        if (phone.length != 10) {
            _uiState.value = AuthUiState.Error("Invalid phone number")
            return
        }

        if (resendToken == null) {
            _uiState.value = AuthUiState.Error("Cannot resend OTP. Please try again later.")
            return
        }

        currentPhoneNumber = phone // Update current phone number

        _uiState.value = AuthUiState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken!!)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun loginWithGoogle() {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                // Try silent sign-in first
                val account = googleSignInClient?.silentSignIn()?.await()

                if (account != null && account.idToken != null) {
                    signInWithGoogleIdToken(account.idToken!!)
                } else {
                    // Silent sign-in failed, user needs to sign in manually
                    _uiState.value = AuthUiState.Error("Please sign in manually")
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Unauth not supported") == true || e.message?.contains("gRPC") == true -> 
                        "Google Play Services error: Please ensure you are signed in to a Google account on this device."
                    else -> "Google Sign-In failed: ${e.message}"
                }
                _uiState.value = AuthUiState.Error(errorMsg)
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()

                if (result.user != null) {
                    // Sync user data to local DB before proceeding
                    userRepository.syncUserData(result.user!!.uid)
                    onboardingDataStore.setOnboardingCompleted(true)
                    _uiState.value = AuthUiState.Verified(result.user!!.uid)
                } else {
                    _uiState.value = AuthUiState.Error("Sign-in failed")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Firebase sign-in error: ${e.message}")
            }
        }
    }

    fun getGoogleSignInIntent(): android.content.Intent? {
        return googleSignInClient?.signInIntent
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                userRepository.clearLocalData()
                onboardingDataStore.clearAll()
                auth.signOut()
                googleSignInClient?.signOut()?.await()
                resetState()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Sign-out failed: ${e.message}")
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    try {
                        // Link phone credential if it's first login verification
                        user.linkWithCredential(credential).await()
                        
                        // Check if we need to migrate document from regNo to UID
                        val userByUid = firestore.collection("users").document(user.uid).get().await()
                        if (!userByUid.exists()) {
                            // Document might be stored under regNo (Admin created)
                            // We need to find it and migrate
                            val usersByPhone = firestore.collection("users")
                                .whereEqualTo("phoneNumber", user.phoneNumber ?: "")
                                .get()
                                .await()
                            
                            val adminCreatedDoc = usersByPhone.documents.firstOrNull { 
                                it.getBoolean("isFirstLogin") == true 
                            }

                            if (adminCreatedDoc != null) {
                                val data = adminCreatedDoc.data?.toMutableMap() ?: mutableMapOf()
                                data["id"] = user.uid
                                data["isFirstLogin"] = false
                                data["updatedAt"] = System.currentTimeMillis()
                                
                                // Create new document with UID
                                firestore.collection("users").document(user.uid).set(data).await()
                                
                                // Delete old document (regNo based)
                                firestore.collection("users").document(adminCreatedDoc.id).delete().await()
                            }
                        } else {
                            // Document already exists under UID, just update flag
                            firestore.collection("users").document(user.uid)
                                .update("isFirstLogin", false)
                                .await()
                        }
                    } catch (e: Exception) {
                        // If user is already linked to this phone, we just proceed
                        // This happens on subsequent logins where MFA is required but already linked
                        if (e.message?.contains("already been linked") == true || 
                            e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            Log.d("AuthViewModel", "User already linked, proceeding as verified")
                        } else {
                            throw e
                        }
                    }
                        
                    userRepository.syncUserData(user.uid)
                    onboardingDataStore.setOnboardingCompleted(true)
                    _uiState.value = AuthUiState.Verified(user.uid)
                } else {
                    val result = auth.signInWithCredential(credential).await()
                    if (result.user != null) {
                        userRepository.syncUserData(result.user!!.uid)
                        onboardingDataStore.setOnboardingCompleted(true)
                        _uiState.value = AuthUiState.Verified(result.user!!.uid)
                    } else {
                        _uiState.value = AuthUiState.Error("Sign-in failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Verification error: ${e.message}")
            }
        }
    }
}
