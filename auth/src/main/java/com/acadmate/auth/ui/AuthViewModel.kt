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
    
    private var pendingEmail: String? = null
    private var pendingPass: String? = null
    private var pendingRegNo: String? = null

    companion object {
        private var sharedVerificationId: String? = null
        private var sharedResendToken: PhoneAuthProvider.ForceResendingToken? = null
        private var sharedCurrentPhoneNumber: String? = null
    }

    private var verificationId: String?
        get() = sharedVerificationId.also { Log.d("AuthViewModel", "Getting verificationId: $it") }
        set(value) { 
            Log.d("AuthViewModel", "Setting verificationId: $value")
            sharedVerificationId = value 
        }

    private var resendToken: PhoneAuthProvider.ForceResendingToken?
        get() = sharedResendToken
        set(value) { 
            Log.d("AuthViewModel", "Setting resendToken: $value")
            sharedResendToken = value 
        }

    private var currentPhoneNumber: String?
        get() = sharedCurrentPhoneNumber
        set(value) { 
            Log.d("AuthViewModel", "Setting currentPhoneNumber: $value")
            sharedCurrentPhoneNumber = value 
        }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private var forcePasswordChangeUserId: String? = null

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
            Log.d("AuthViewModel", "onCodeSent: $verificationId")
            this@AuthViewModel.verificationId = verificationId
            this@AuthViewModel.resendToken = token
            _uiState.value = AuthUiState.OtpSent("+91${currentPhoneNumber ?: "phone"}")
        }

        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            Log.d("AuthViewModel", "onCodeAutoRetrievalTimeOut: $verificationId")
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

                // Registration Number Validation
                if (!ValidationUtils.isValidRegistrationNumber(cleanRegNo, selectedRole)) {
                    _uiState.value = AuthUiState.Error(ValidationUtils.getRegistrationNumberErrorMessage(selectedRole))
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
                        
                        pendingEmail = email
                        pendingPass = cleanPass
                        pendingRegNo = cleanRegNo
                        
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
                        e.message?.contains("password", ignoreCase = true) == true || 
                        e.message?.contains("credential", ignoreCase = true) == true -> "Incorrect password. Please check your credentials."
                        e.message?.contains("user-not-found", ignoreCase = true) == true ||
                        e.message?.contains("no user record", ignoreCase = true) == true -> "No account found for this institutional ID."
                        else -> e.message ?: "Authentication failed"
                    }
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("password", ignoreCase = true) == true ||
                    e.message?.contains("credential", ignoreCase = true) == true -> "Incorrect password. Please check your credentials."
                    e.message?.contains("user-not-found", ignoreCase = true) == true -> "Account record missing."
                    else -> e.message ?: "Login failed"
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
                var userDoc = if (userId.startsWith("ACTIVATE:")) {
                    val regNo = userId.substringAfter("ACTIVATE:")
                    val query = firestore.collection("users").whereEqualTo("regNo", regNo).get().await()
                    query.documents.firstOrNull()
                } else {
                    firestore.collection("users").document(userId).get().await()
                }

                // If not found by UID, try searching by phoneNumber field
                if (userDoc == null || !userDoc.exists()) {
                    val cleanPhone = inputPhone.replace(Regex("[^\\d]"), "").takeLast(10)
                    val phoneQuery = firestore.collection("users")
                        .whereIn("phoneNumber", listOf(cleanPhone, "+91$cleanPhone"))
                        .get()
                        .await()
                    userDoc = phoneQuery.documents.firstOrNull()
                }

                if (userDoc == null || !userDoc.exists()) {
                    _uiState.value = AuthUiState.Error("User record not found. Please contact Admin.")
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

    fun resetPassword(regNo: String) {
        val cleanRegNo = regNo.trim()
        Log.d("AuthViewModel", "resetPassword: regNo=$cleanRegNo")
        if (cleanRegNo.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your Registration Number to reset password")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val selectedRoleFlow = onboardingDataStore.selectedRole.first()
                Log.d("AuthViewModel", "resetPassword: selectedRole=$selectedRoleFlow")
                if (selectedRoleFlow == null) {
                    _uiState.value = AuthUiState.Error("Role not selected. Please select a role first.")
                    return@launch
                }

                val userDoc = firestore.collection("users")
                    .whereEqualTo("regNo", cleanRegNo)
                    .whereEqualTo("role", selectedRoleFlow.name)
                    .get()
                    .await()

                if (userDoc.isEmpty) {
                    Log.w("AuthViewModel", "resetPassword: No user found for regNo=$cleanRegNo and role=${selectedRoleFlow.name}")
                    _uiState.value = AuthUiState.Error("No account found with this Registration Number for the selected role")
                    return@launch
                }

                val email = userDoc.documents[0].getString("email") ?: ""
                Log.d("AuthViewModel", "resetPassword: found email=$email")
                if (email.isEmpty()) {
                    _uiState.value = AuthUiState.Error("No email associated with this account. Contact Admin.")
                    return@launch
                }

                auth.sendPasswordResetEmail(email).await()
                Log.d("AuthViewModel", "resetPassword: email sent successfully to $email")
                _uiState.value = AuthUiState.PasswordResetSent("Password reset link sent to $email. Please check your inbox.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "resetPassword: Error sending reset email", e)
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    fun verifyOtp(otp: String) {
        Log.d("AuthViewModel", "verifyOtp: otp=$otp, verificationId=$verificationId")
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("Please enter a valid 6-digit OTP")
            return
        }

        if (verificationId == null) {
            Log.e("AuthViewModel", "verifyOtp: verificationId is null")
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
        forcePasswordChangeUserId = null
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun signOut() {
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

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                // If there's an existing currentUser (from email/password login step), 
                // we link it with the phone credential.
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    try {
                        // Attempt to link. If already linked, this is fine.
                        currentUser.linkWithCredential(credential).await()
                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (msg.contains("already been linked", ignoreCase = true) || 
                            msg.contains("provider-already-linked", ignoreCase = true) ||
                            e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            Log.d("AuthViewModel", "User already linked or collision, proceeding")
                        } else {
                            throw e
                        }
                    }

                    // Success! Finalize the session
                    finalizeUserSession(currentUser)
                } else {
                    // Institutional Login Case (Admin created user) - NO PREVIOUS SESSION
                    if (pendingEmail != null && pendingPass != null) {
                        try {
                            // 1. First, we MUST verify the credential by signing in with it directly
                            // This confirms the phone belongs to the user
                            val phoneAuthResult = auth.signInWithCredential(credential).await()
                            val phoneUser = phoneAuthResult.user ?: throw Exception("Failed to sign in with phone")

                            // 2. Now link the email/password identity to this verified phone user
                            // or create the email account and link it. 
                            val emailCredential = com.google.firebase.auth.EmailAuthProvider.getCredential(pendingEmail!!, pendingPass!!)
                            
                            try {
                                phoneUser.linkWithCredential(emailCredential).await()
                            } catch (linkError: Exception) {
                                val msg = linkError.message ?: ""
                                if (msg.contains("already been linked", ignoreCase = true) || 
                                    msg.contains("provider-already-linked", ignoreCase = true) ||
                                    linkError is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                    Log.d("AuthViewModel", "Email already linked or collision, proceeding")
                                } else {
                                    throw linkError
                                }
                            }
                            
                            finalizeUserSession(phoneUser)
                        } catch (e: Exception) {
                            auth.signOut() // CLEANUP: Ensure no half-logged-in state
                            throw e
                        }
                    } else {
                        // Standard Phone-only Login
                        val result = auth.signInWithCredential(credential).await()
                        if (result.user != null) {
                            finalizeUserSession(result.user!!)
                        } else {
                            _uiState.value = AuthUiState.Error("Sign-in failed")
                        }
                    }
                }
            } catch (e: Exception) {
                auth.signOut() // IMPORTANT: Clear session on any failure
                _uiState.value = AuthUiState.Error("Verification error: ${e.message}")
            }
        }
    }

    private suspend fun finalizeUserSession(user: com.google.firebase.auth.FirebaseUser) {
        // Treat regNo as the authoritative document ID or find by email
        val userDocQuery = if (pendingRegNo != null) {
            firestore.collection("users").whereEqualTo("regNo", pendingRegNo).get().await()
        } else {
            firestore.collection("users").whereEqualTo("email", user.email ?: "").get().await()
        }
        
        val adminCreatedDoc = userDocQuery.documents.firstOrNull()

        if (adminCreatedDoc != null) {
            val isFirstLogin = adminCreatedDoc.getBoolean("isFirstLogin") ?: false
            
            firestore.collection("users").document(adminCreatedDoc.id)
                .update(
                    "id", user.uid,
                    "updatedAt", System.currentTimeMillis()
                )
                .await()

            if (isFirstLogin) {
                _uiState.value = AuthUiState.ForcePasswordChange(user.uid)
                // Clear pending data
                pendingEmail = null
                pendingPass = null
                pendingRegNo = null
                return
            }
        }

        userRepository.syncUserData(user.uid)
        onboardingDataStore.setOnboardingCompleted(true)
        _uiState.value = AuthUiState.Verified(user.uid)
        
        // Clear pending data
        pendingEmail = null
        pendingPass = null
        pendingRegNo = null
    }

    fun updateInstitutionalPassword(newPass: String) {
        if (!ValidationUtils.isStrongPassword(newPass)) {
            _uiState.value = AuthUiState.Error(ValidationUtils.getPasswordStrengthErrorMessage())
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = auth.currentUser
                if (user != null) {
                    // 1. Update Firebase Auth Password
                    user.updatePassword(newPass).await()
                    
                    // 2. Update Firestore and mark isFirstLogin = false
                    val userDocQuery = firestore.collection("users")
                        .whereEqualTo("id", user.uid)
                        .get()
                        .await()
                    
                    val doc = userDocQuery.documents.firstOrNull()
                    if (doc != null) {
                        firestore.collection("users").document(doc.id)
                            .update(
                                "password", newPass, // Update to new password
                                "isFirstLogin", false, // SET TO FALSE NOW
                                "updatedAt", System.currentTimeMillis()
                            ).await()
                    }

                    // 3. Instead of signing out, finalize the session
                    userRepository.syncUserData(user.uid)
                    onboardingDataStore.setOnboardingCompleted(true)
                    _uiState.value = AuthUiState.Verified(user.uid)
                } else {
                    _uiState.value = AuthUiState.Error("Session expired. Please start over.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Failed to update password: ${e.message}")
            }
        }
    }
}
