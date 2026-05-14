package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.core.db.TimetableEntity
import com.acadmate.core.db.TimetableRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val repository: TimetableRepository,
    val onboardingDataStore: OnboardingDataStore
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    val autoAlarmsEnabled = onboardingDataStore.autoAlarmsEnabled

    fun setAutoAlarmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            onboardingDataStore.setAutoAlarmsEnabled(enabled)
        }
    }

    init {
        // Automatically sync from Firestore on load
        viewModelScope.launch {
            repository.syncGlobalTimetable()
        }
        
        // Initial load for Current Day
        val calendar = java.util.Calendar.getInstance()
        val dayName = when(calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Monday"
            java.util.Calendar.TUESDAY -> "Tuesday"
            java.util.Calendar.WEDNESDAY -> "Wednesday"
            java.util.Calendar.THURSDAY -> "Thursday"
            java.util.Calendar.FRIDAY -> "Friday"
            java.util.Calendar.SATURDAY -> "Saturday"
            java.util.Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
        loadTimetableForDay(dayName)
    }

    fun loadTimetableForDay(dayName: String) {
        val dayIndex = when (dayName) {
            "Monday" -> 1
            "Tuesday" -> 2
            "Wednesday" -> 3
            "Thursday" -> 4
            "Friday" -> 5
            "Saturday" -> 6
            "Sunday" -> 7
            else -> 1
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedDay = dayName, isLoading = true)
            
            repository.getTimetableForDay(dayIndex).collectLatest { entities ->
                val entries = entities.map { entity ->
                    val startTime = LocalTime.parse(entity.startTime)
                    val endTime = LocalTime.parse(entity.endTime)
                    val isCompleted = LocalTime.now().isAfter(endTime) && LocalDate.now().dayOfWeek.value == dayIndex
                    val isCurrent = LocalTime.now().isAfter(startTime) && LocalTime.now().isBefore(endTime) && LocalDate.now().dayOfWeek.value == dayIndex
                    
                    TimetableEntry(
                        id = entity.id,
                        subject = entity.subject,
                        faculty = if (entity.faculty.isBlank()) "Not Assigned" else entity.faculty,
                        room = entity.room,
                        startTime = startTime,
                        endTime = endTime,
                        dayOfWeek = dayName,
                        date = LocalDate.now().plusDays((dayIndex - LocalDate.now().dayOfWeek.value).toLong()),
                        isCurrent = isCurrent,
                        isCompleted = isCompleted,
                        isAlarmSet = entity.isAlarmSet
                    )
                }.sortedBy { it.startTime }

                _uiState.value = _uiState.value.copy(
                    timetableEntries = entries,
                    isLoading = false
                )
            }
        }
    }

    fun toggleAlarm(id: String, isAlarmSet: Boolean) {
        val userId = auth.currentUser?.uid
        viewModelScope.launch {
            repository.updateAlarmStatus(id, isAlarmSet, userId)
        }
    }
}
