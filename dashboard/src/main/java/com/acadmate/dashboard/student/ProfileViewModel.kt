package com.acadmate.dashboard.student

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    data class Success(val message: String) : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            // Initial load from local DB
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    val regNo = user.regNo ?: user.id // Fallback to id if regNo missing
                    
                    var attended = 0
                    var percentage = 0f
                    try {
                        val attendanceSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("attendance")
                            .whereEqualTo("studentId", regNo)
                            .get()
                            .await()
                        attended = attendanceSnapshot.size()
                        val total = 136 
                        percentage = if (attended > 0) (attended.toFloat() / total) else 0f
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileViewModel", "Failed to fetch attendance stats", e)
                    }

                    _profileUiState.value = ProfileUiState(
                        name = user.name.ifBlank { "User" },
                        email = user.email,
                        phone = user.phoneNumber,
                        enrollment = user.regNo ?: "N/A",
                        department = user.department ?: "General",
                        role = user.role.name,
                        address = user.address ?: "N/A",
                        overallAttendance = percentage,
                        classesAttended = attended,
                        classesMissed = 0, // Should be calculated from total - attended
                        profilePictureUrl = user.profilePictureUrl
                    )
                } else {
                    // Try to sync if local is empty
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        userRepository.syncUserData(uid)
                    }
                }
            }
        }
    }

    fun updateProfile(name: String, email: String, address: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _updateState.value = ProfileUpdateState.Loading
        viewModelScope.launch {
            val result = userRepository.updateUserProfile(userId, name, email, address)
            if (result.isSuccess) {
                _updateState.value = ProfileUpdateState.Success("Profile updated successfully")
            } else {
                _updateState.value = ProfileUpdateState.Error("Failed to update profile")
            }
        }
    }

    fun uploadProfilePicture(imageUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _updateState.value = ProfileUpdateState.Loading
        viewModelScope.launch {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                val profilePicRef = storageRef.child("profile_pictures/$userId.jpg")
                profilePicRef.putFile(imageUri).await()
                val downloadUrl = profilePicRef.downloadUrl.await()
                
                userRepository.updateProfilePicture(userId, downloadUrl.toString())
                _updateState.value = ProfileUpdateState.Success("Photo updated")
            } catch (e: Exception) {
                _updateState.value = ProfileUpdateState.Error("Upload failed: ${e.message}")
            }
        }
    }

    fun resetState() {
        _updateState.value = ProfileUpdateState.Idle
    }
}
