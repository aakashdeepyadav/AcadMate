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
    private val onboardingDataStore: OnboardingDataStore
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
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                repository.syncTimetable(userId)
            }
        } else {
            // For testing/portfolio, if no user is logged in, we might want to populate dummy CSE data
            viewModelScope.launch {
                populateDummyCseDataIfNeeded()
            }
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

    private suspend fun populateDummyCseDataIfNeeded() {
        // We will seed the local DB if it's empty so the portfolio isn't blank
        val mondayClasses = repository.getTimetableForDaySync(1)
        if (mondayClasses.isEmpty()) {
            val cseClasses = listOf(
                TimetableEntity("1", 1, "Data Structures & Algorithms", "", "09:00", "10:30", "Lab 3", 0),
                TimetableEntity("2", 1, "Operating Systems", "", "11:00", "12:30", "Room 402", 0),
                TimetableEntity("3", 1, "Computer Networks", "", "14:00", "15:30", "Room 405", 0),
                TimetableEntity("4", 2, "Database Management Systems", "", "09:00", "10:30", "Room 301", 0),
                TimetableEntity("5", 2, "Compiler Design", "", "11:00", "12:30", "Lab 2", 0),
                TimetableEntity("6", 3, "Artificial Intelligence", "", "10:00", "11:30", "Room 501", 0),
                TimetableEntity("7", 3, "Software Engineering", "", "13:00", "14:30", "Room 402", 0),
                TimetableEntity("8", 4, "Data Structures Lab", "", "09:00", "12:00", "Lab 3", 0),
                TimetableEntity("9", 5, "Computer Networks Lab", "", "14:00", "17:00", "Lab 4", 0)
            )
            repository.insertTimetable(cseClasses)
        }
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
