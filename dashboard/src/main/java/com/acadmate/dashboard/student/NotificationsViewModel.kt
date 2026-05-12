package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.datastore.OnboardingDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val onboardingDataStore: OnboardingDataStore
) : ViewModel() {

    val attendanceAlertsEnabled = onboardingDataStore.attendanceAlertsEnabled
    val assignmentRemindersEnabled = onboardingDataStore.assignmentRemindersEnabled
    val examNotificationsEnabled = onboardingDataStore.examNotificationsEnabled
    val smartInsightsEnabled = onboardingDataStore.smartInsightsEnabled

    fun setAttendanceAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            onboardingDataStore.setAttendanceAlertsEnabled(enabled)
        }
    }

    fun setAssignmentRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            onboardingDataStore.setAssignmentRemindersEnabled(enabled)
        }
    }

    fun setExamNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            onboardingDataStore.setExamNotificationsEnabled(enabled)
        }
    }

    fun setSmartInsightsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            onboardingDataStore.setSmartInsightsEnabled(enabled)
        }
    }
}
