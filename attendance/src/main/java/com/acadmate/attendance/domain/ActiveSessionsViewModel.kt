package com.acadmate.attendance.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveSessionRecord(
    val classId: String,
    val facultyId: String,
    val startTime: Long,
    val status: String
)

data class ActiveSessionsUiState(
    val isLoading: Boolean = true,
    val activeSessions: List<ActiveSessionRecord> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ActiveSessionsViewModel @Inject constructor() : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(ActiveSessionsUiState())
    val uiState: StateFlow<ActiveSessionsUiState> = _uiState.asStateFlow()

    init {
        listenToActiveSessions()
    }

    private fun listenToActiveSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            firestore.collection("active_sessions")
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load active sessions: ${error.message}"
                        )
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val sessions = snapshot.documents.mapNotNull { doc ->
                            try {
                                ActiveSessionRecord(
                                    classId = doc.getString("classId") ?: doc.id,
                                    facultyId = doc.getString("facultyId") ?: "Unknown Faculty",
                                    startTime = doc.getLong("startTime") ?: 0L,
                                    status = doc.getString("status") ?: "UNKNOWN"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.startTime }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            activeSessions = sessions,
                            error = null
                        )
                    }
                }
        }
    }
}
