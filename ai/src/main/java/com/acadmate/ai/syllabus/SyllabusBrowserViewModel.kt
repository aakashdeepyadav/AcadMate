package com.acadmate.ai.syllabus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.PredefinedSyllabus
import com.acadmate.core.model.SubjectSyllabus
import com.acadmate.core.db.UserRepository
import com.acadmate.core.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class BrowserUiState {
    object Loading : BrowserUiState()
    data class Success(val syllabuses: List<SubjectSyllabus>) : BrowserUiState()
    data class Error(val message: String) : BrowserUiState()
}

@HiltViewModel
class SyllabusBrowserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow<BrowserUiState>(BrowserUiState.Loading)
    val uiState: StateFlow<BrowserUiState> = _uiState
    
    init {
        fetchSyllabuses()
    }
    
    fun fetchSyllabuses() {
        viewModelScope.launch {
            _uiState.value = BrowserUiState.Loading
            try {
                val user = userRepository.getCurrentUser().first()
                val snapshot = firestore.collection("syllabuses").get().await()
                var list = snapshot.toObjects(SubjectSyllabus::class.java)

                // If user is faculty, filter list to only show their assigned courses
                if (user?.role == UserRole.FACULTY) {
                    val assignedCoursesSnapshot = firestore.collection("courses")
                        .whereEqualTo("assignedFaculty", user.name)
                        .get()
                        .await()
                    
                    val assignedCodes = assignedCoursesSnapshot.documents.map { it.getString("code") ?: "" }.toSet()
                    list = list.filter { it.subjectCode in assignedCodes }
                }

                _uiState.value = BrowserUiState.Success(list)
            } catch (e: Exception) {
                _uiState.value = BrowserUiState.Error(e.message ?: "Failed to load syllabus")
            }
        }
    }
}
