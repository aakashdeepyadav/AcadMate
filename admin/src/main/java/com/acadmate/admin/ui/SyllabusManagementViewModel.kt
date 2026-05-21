package com.acadmate.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.SubjectSyllabus
import com.acadmate.core.model.SyllabusUnit
import com.acadmate.core.model.Subject
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class SyllabusUiState {
    object Idle : SyllabusUiState()
    object Loading : SyllabusUiState()
    object Success : SyllabusUiState()
    data class Error(val message: String) : SyllabusUiState()
}

@HiltViewModel
class SyllabusManagementViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow<SyllabusUiState>(SyllabusUiState.Idle)
    val uiState: StateFlow<SyllabusUiState> = _uiState

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("subjects").get().await()
                _subjects.value = snapshot.toObjects(Subject::class.java)
            } catch (e: Exception) {}
        }
    }

    suspend fun getExistingSyllabus(subjectCode: String): SubjectSyllabus? {
        return try {
            firestore.collection("syllabuses").document(subjectCode).get().await()
                .toObject(SubjectSyllabus::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun logAdminAction(title: String, type: com.acadmate.core.model.ActionType, description: String) {
        try {
            val actionData = hashMapOf(
                "title" to title,
                "timestamp" to System.currentTimeMillis(),
                "type" to type.name,
                "description" to description
            )
            firestore.collection("admin_logs").add(actionData).await()
        } catch (e: Exception) {}
    }

    fun publishSyllabus(
        subjectCode: String,
        subjectName: String,
        description: String,
        credits: Int,
        ltp: String,
        units: List<SyllabusUnit>
    ) {
        if (subjectCode.isBlank() || subjectName.isBlank()) {
            _uiState.value = SyllabusUiState.Error("Subject Code and Name cannot be empty.")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = SyllabusUiState.Loading
            try {
                val syllabus = SubjectSyllabus(
                    subjectCode = subjectCode,
                    subjectName = subjectName,
                    description = description,
                    credits = credits,
                    ltp = ltp,
                    units = units
                )
                
                // Using subjectCode as document ID
                firestore.collection("syllabuses")
                    .document(subjectCode)
                    .set(syllabus)
                    .await()
                
                logAdminAction("Syllabus Published", com.acadmate.core.model.ActionType.INSTITUTION_UPDATED, "Curriculum for $subjectName ($subjectCode) was updated.")
                    
                _uiState.value = SyllabusUiState.Success
            } catch (e: Exception) {
                _uiState.value = SyllabusUiState.Error(e.message ?: "Failed to publish syllabus")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = SyllabusUiState.Idle
    }
}
