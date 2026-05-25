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
                if (!ValidationUtils.isValidRegistrationNumber(cleanRegNo, selectedRole)) {
                    _uiState.value = AuthUiState.Error(ValidationUtils.getRegistrationNumberErrorMessage(selectedRole))
                    return@launch
                }
                val userDoc = firestore.collection("users").whereEqualTo("regNo", cleanRegNo).whereEqualTo("role", selectedRole.name).get().await()
                if (userDoc.isEmpty) {
                    _uiState.value = AuthUiState.Error("Account not found for this Role")
                    return@launch
                }
                val userData = userDoc.documents[0]
                val email = userData.getString("email") ?: ""
                val phoneNumber = userData.getString("phoneNumber") ?: ""
                val storedPassword = userData.getString("password") ?: ""
                val isFirstLogin = userData.getBoolean("isFirstLogin") ?: false

                if (isFirstLogin && cleanPass == storedPassword) {
                    pendingEmail = email; pendingPass = cleanPass; pendingRegNo = cleanRegNo
                    _uiState.value = AuthUiState.RequirePhoneInput("ACTIVATE:$cleanRegNo", if (phoneNumber.length >= 10) "******${phoneNumber.takeLast(4)}" else "registered phone")
                } else {
                    val result = auth.signInWithEmailAndPassword(email, cleanPass).await()
                    if (result.user != null) {
                        userRepository.syncUserData(result.user!!.uid)
                        _uiState.value = AuthUiState.RequirePhoneInput(result.user!!.uid, if (phoneNumber.length >= 10) "******${phoneNumber.takeLast(4)}" else "linked phone")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun loginWithPhone(phone: String, activity: android.app.Activity) {
        val cleanPhone = phone.replace(Regex("[^\\d]"), "").takeLast(10)
        if (cleanPhone.length != 10) {
            _uiState.value = AuthUiState.Error("Please enter a valid 10-digit phone number")
            return
        }
        currentPhoneNumber = cleanPhone
        _uiState.value = AuthUiState.Loading
        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber("+91$cleanPhone").setTimeout(60L, TimeUnit.SECONDS).setActivity(activity).setCallbacks(callbacks).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneAndSendOtp(inputPhone: String, userId: String, activity: android.app.Activity) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val userDoc = if (userId.startsWith("ACTIVATE:")) {
                    firestore.collection("users").whereEqualTo("regNo", userId.substringAfter("ACTIVATE:")).get().await().documents.firstOrNull()
                } else firestore.collection("users").document(userId).get().await()

                val registeredPhone = userDoc?.getString("phoneNumber") ?: ""
                if (inputPhone.takeLast(10) == registeredPhone.takeLast(10)) loginWithPhone(inputPhone.takeLast(10), activity)
                else _uiState.value = AuthUiState.Error("Phone number mismatch")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Verification failed: ${e.message}")
            }
        }
    }

    fun resetPassword(regNo: String) {
        val cleanRegNo = regNo.trim()
        if (cleanRegNo.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter Reg No")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val role = onboardingDataStore.selectedRole.first() ?: throw Exception("Role not selected")
                val userDoc = firestore.collection("users").whereEqualTo("regNo", cleanRegNo).whereEqualTo("role", role.name).get().await()
                if (userDoc.isEmpty) throw Exception("No account found")
                val email = userDoc.documents[0].getString("email") ?: throw Exception("No email found")
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = AuthUiState.PasswordResetSent("Reset link sent to $email")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Reset failed")
            }
        }
    }

    fun verifyOtp(otp: String) {
        if (otp.length != 6) {
            _uiState.value = AuthUiState.Error("Invalid OTP")
            return
        }
        if (pendingUpdateType != null) {
            val cred = PhoneAuthProvider.getCredential(verificationId!!, otp)
            confirmPhoneUpdate(cred)
        } else {
            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
            signInWithPhoneAuthCredential(credential)
        }
    }

    fun resendOtp(phone: String, activity: android.app.Activity) {
        loginWithPhone(phone, activity)
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
        forcePasswordChangeUserId = null
        pendingUpdateType = null
        pendingUpdateValue = null
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signOut() {
        userRepository.clearLocalData()
        onboardingDataStore.clearAll()
        auth.signOut()
        googleSignInClient?.signOut()?.await()
        resetState()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    try { currentUser.linkWithCredential(credential).await() } catch (e: Exception) {}
                    finalizeUserSession(currentUser)
                } else if (pendingEmail != null && pendingPass != null) {
                    val phoneUser = auth.signInWithCredential(credential).await().user ?: throw Exception("Phone login failed")
                    val emailCred = com.google.firebase.auth.EmailAuthProvider.getCredential(pendingEmail!!, pendingPass!!)
                    try { phoneUser.linkWithCredential(emailCred).await() } catch (e: Exception) {}
                    finalizeUserSession(phoneUser)
                } else {
                    val result = auth.signInWithCredential(credential).await()
                    if (result.user != null) finalizeUserSession(result.user!!)
                    else throw Exception("Sign-in failed")
                }
            } catch (e: Exception) {
                auth.signOut()
                _uiState.value = AuthUiState.Error("Verification error: ${e.message}")
            }
        }
    }

    private suspend fun finalizeUserSession(user: com.google.firebase.auth.FirebaseUser) {
        val query = if (pendingRegNo != null) firestore.collection("users").whereEqualTo("regNo", pendingRegNo).get().await()
        else firestore.collection("users").whereEqualTo("email", user.email ?: "").get().await()
        
        val doc = query.documents.firstOrNull()
        if (doc != null) {
            firestore.collection("users").document(doc.id).update("id", user.uid, "updatedAt", System.currentTimeMillis()).await()
            if (doc.getBoolean("isFirstLogin") == true) {
                _uiState.value = AuthUiState.ForcePasswordChange(user.uid)
                pendingEmail = null; pendingPass = null; pendingRegNo = null
                return
            }
        }
        userRepository.syncUserData(user.uid)
        onboardingDataStore.setOnboardingCompleted(true)
        _uiState.value = AuthUiState.Verified(user.uid)
        pendingEmail = null; pendingPass = null; pendingRegNo = null
    }

    fun updateInstitutionalPassword(newPass: String) {
        if (!ValidationUtils.isStrongPassword(newPass)) {
            _uiState.value = AuthUiState.Error(ValidationUtils.getPasswordStrengthErrorMessage())
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = auth.currentUser ?: throw Exception("Session expired")
                user.updatePassword(newPass).await()
                val doc = firestore.collection("users").whereEqualTo("id", user.uid).get().await().documents.firstOrNull()
                if (doc != null) firestore.collection("users").document(doc.id).update("password", newPass, "isFirstLogin", false, "updatedAt", System.currentTimeMillis()).await()
                userRepository.syncUserData(user.uid)
                _uiState.value = AuthUiState.Verified(user.uid)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Update failed: ${e.message}")
            }
        }
    }

    // --- Secure Profile Updates ---
    private var pendingUpdateValue: String? = null
    private var pendingUpdateType: UpdateType? = null

    fun startPhoneUpdate(newPhone: String, activity: Activity) {
        val cleanPhone = newPhone.replace(Regex("[^\\d]"), "").takeLast(10)
        if (cleanPhone.length != 10) {
            _uiState.value = AuthUiState.Error("Invalid phone number")
            return
        }
        pendingUpdateValue = cleanPhone
        pendingUpdateType = UpdateType.PHONE
        _uiState.value = AuthUiState.Loading
        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber("+91$cleanPhone").setTimeout(60L, TimeUnit.SECONDS).setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) { confirmPhoneUpdate(credential) }
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) { _uiState.value = AuthUiState.Error(e.message ?: "Failed") }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id; resendToken = token
                    _uiState.value = AuthUiState.UpdateOtpSent("+91$cleanPhone", UpdateType.PHONE)
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun confirmPhoneUpdate(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = auth.currentUser ?: throw Exception("Session expired")
                user.updatePhoneNumber(credential).await()
                val doc = firestore.collection("users").whereEqualTo("id", user.uid).get().await().documents.firstOrNull()
                doc?.reference?.update("phoneNumber", "+91$pendingUpdateValue")?.await()
                userRepository.syncUserData(user.uid)
                _uiState.value = AuthUiState.UpdateSuccess("Phone number updated successfully")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Update failed: ${e.message}")
            }
        }
    }

    fun startEmailUpdate(newEmail: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _uiState.value = AuthUiState.Error("Invalid email address")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = auth.currentUser ?: throw Exception("Session expired")
                user.verifyBeforeUpdateEmail(newEmail).await()
                val doc = firestore.collection("users").whereEqualTo("id", user.uid).get().await().documents.firstOrNull()
                doc?.reference?.update("email", newEmail)?.await() // Update Firestore too
                _uiState.value = AuthUiState.UpdateSuccess("Verification link sent to $newEmail")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Update failed: ${e.message}")
            }
        }
    }
}
