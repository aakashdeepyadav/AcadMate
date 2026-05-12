package com.acadmate.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.core.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val onboardingDataStore: OnboardingDataStore,
    private val userRepository: com.acadmate.core.db.UserRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Idle)
    val uiState: StateFlow<SplashUiState> = _uiState

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val user = auth.currentUser
            val onboardingCompleted = onboardingDataStore.isOnboardingCompleted.first()
            
            if (user == null) {
                _uiState.value = SplashUiState.NavigateToAuth
            } else {
                // User is authenticated in Firebase
                if (!onboardingCompleted) {
                    // If user is logged in but flag is missing, we try to recover
                    // but we don't force logout unless data is totally missing
                    onboardingDataStore.setOnboardingCompleted(true)
                }

                // 1. Try immediate navigation using local cached data
                val localUser = userRepository.getCurrentUser().first()
                if (localUser != null) {
                    navigateBasedOnRole(localUser.role)
                    // Sync with cloud in the background without blocking the user
                    launch { userRepository.syncUserData(user.uid) }
                } else {
                    // 2. No local data, must sync once before proceeding
                    try {
                        userRepository.syncUserData(user.uid)
                        userRepository.getCurrentUser().first()?.let {
                            onboardingDataStore.saveSelectedRole(it.role)
                            navigateBasedOnRole(it.role)
                        } ?: run {
                            // If sync finishes and still no user, then account might be deleted
                            auth.signOut()
                            _uiState.value = SplashUiState.NavigateToAuth
                        }
                    } catch (_: Exception) {
                        // On sync error, if we have no local data and no internet, we might need to stay on splash or show error
                        // For now, retry or go to auth
                        _uiState.value = SplashUiState.NavigateToAuth
                    }
                }
            }
        }
    }

    private fun navigateBasedOnRole(role: UserRole) {
        if (role == UserRole.ADMIN) {
            _uiState.value = SplashUiState.NavigateToAdmin
        } else {
            _uiState.value = SplashUiState.NavigateToDashboard
        }
    }
}

sealed class SplashUiState {
    object Idle : SplashUiState()
    object NavigateToAuth : SplashUiState()
    object NavigateToDashboard : SplashUiState()
    object NavigateToAdmin : SplashUiState()
}
