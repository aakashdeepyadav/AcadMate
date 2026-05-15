package com.acadmate.assignments.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.Assignment
import com.acadmate.core.model.UserRole
import com.acadmate.core.datastore.OnboardingDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AssignmentViewModel @Inject constructor(
    private val onboardingDataStore: OnboardingDataStore
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _assignments = MutableStateFlow<List<Assignment>>(emptyList())
    val assignments: StateFlow<List<Assignment>> = _assignments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess

    private val _facultyCourses = MutableStateFlow<List<String>>(emptyList())
    val facultyCourses: StateFlow<List<String>> = _facultyCourses

    init {
        loadAssignments()
        loadFacultyCourses()
    }

    private fun loadFacultyCourses() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = firestore.collection("users").document(userId).get().await()
                val facultyName = userDoc.getString("name") ?: ""
                
                if (facultyName.isNotBlank()) {
                    val snapshot = firestore.collection("courses")
                        .whereEqualTo("assignedFaculty", facultyName)
                        .get()
                        .await()
                    _facultyCourses.value = snapshot.documents.map { it.getString("name") ?: "" }.filter { it.isNotBlank() }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadAssignments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val role = onboardingDataStore.selectedRole.first()
                val userId = auth.currentUser?.uid ?: return@launch

                val query = if (role == UserRole.FACULTY) {
                    firestore.collection("assignments").whereEqualTo("facultyId", userId)
                } else {
                    firestore.collection("assignments")
                }

                val result = query.get().await()
                _assignments.value = result.toObjects(Assignment::class.java)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadAssignment(assignment: Assignment) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                firestore.collection("assignments")
                    .add(assignment)
                    .await()
                _uploadSuccess.value = true
                loadAssignments()
            } catch (e: Exception) {
                _uploadSuccess.value = false
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun resetUploadState() {
        _uploadSuccess.value = false
    }

    fun submitAssignment(assignmentId: String, fileUri: android.net.Uri, studentId: String) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                // In a real app, upload file to Firebase Storage first
                // For this demo, we'll simulate the URL
                val simulatedUrl = "https://firebasestorage.googleapis.com/v0/b/acadmate.appspot.com/o/submissions%2F$studentId.pdf"
                
                val submissionData = hashMapOf(
                    "assignmentId" to assignmentId,
                    "studentId" to studentId,
                    "fileUrl" to simulatedUrl,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "SUBMITTED"
                )
                
                firestore.collection("submissions")
                    .document("${studentId}_$assignmentId")
                    .set(submissionData)
                    .await()
                
                _uploadSuccess.value = true
            } catch (e: Exception) {
                _uploadSuccess.value = false
            } finally {
                _isUploading.value = false
            }
        }
    }
}
