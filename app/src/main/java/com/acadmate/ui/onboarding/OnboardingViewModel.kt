package com.acadmate.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    val onboardingDataStore: OnboardingDataStore
) : ViewModel() {

    val selectedRole: StateFlow<UserRole?> = onboardingDataStore.selectedRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveRole(role: UserRole) {
        viewModelScope.launch {
            onboardingDataStore.saveSelectedRole(role)
            onboardingDataStore.setOnboardingCompleted(true)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            onboardingDataStore.setDarkMode(enabled)
        }
    }
}
