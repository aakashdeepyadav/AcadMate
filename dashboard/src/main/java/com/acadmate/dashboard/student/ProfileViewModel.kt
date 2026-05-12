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

    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState

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
