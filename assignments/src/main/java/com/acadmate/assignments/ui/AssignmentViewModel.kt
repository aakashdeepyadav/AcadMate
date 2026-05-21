package com.acadmate.assignments.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.Assignment
import com.acadmate.core.model.UserRole
import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.core.db.AssignmentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AssignmentViewModel @Inject constructor(
    private val onboardingDataStore: OnboardingDataStore,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val assignments: StateFlow<List<Assignment>> = assignmentRepository.getAllAssignments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess.asStateFlow()

    private val _facultyCourses = MutableStateFlow<List<String>>(emptyList())
    val facultyCourses: StateFlow<List<String>> = _facultyCourses.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val selectedFiles: StateFlow<List<android.net.Uri>> = _selectedFiles.asStateFlow()

    private val _submissionStatus = MutableStateFlow<String?>(null)
    val submissionStatus: StateFlow<String?> = _submissionStatus.asStateFlow()

    init {
        syncData()
        loadFacultyCourses()
    }

    fun checkSubmissionStatus(assignmentId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val doc = firestore.collection("submissions")
                    .document("${userId}_$assignmentId")
                    .get()
                    .await()
                
                if (doc.exists()) {
                    _submissionStatus.value = doc.getString("status") ?: "SUBMITTED"
                } else {
                    _submissionStatus.value = null
                }
            } catch (e: Exception) {
                _submissionStatus.value = null
            }
        }
    }

    fun onFileSelected(uri: android.net.Uri) {
        _selectedFiles.value = _selectedFiles.value + uri
    }

    fun removeFile(uri: android.net.Uri) {
        _selectedFiles.value = _selectedFiles.value - uri
    }

    private fun syncData() {
        // ... existing syncData implementation ...
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val role = onboardingDataStore.selectedRole.first()
                val userId = auth.currentUser?.uid ?: return@launch
                val isFaculty = role == UserRole.FACULTY
                
                assignmentRepository.syncAssignments(isFaculty, userId)
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFacultyCourses() {
        // ... existing loadFacultyCourses implementation ...
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

    fun uploadAssignment(assignment: Assignment) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val uploadedUrls = mutableListOf<String>()
                
                // Upload each selected file to Firebase Storage
                _selectedFiles.value.forEach { uri ->
                    val fileName = "guidelines/${UUID.randomUUID()}"
                    val ref = storage.reference.child(fileName)
                    ref.putFile(uri).await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    uploadedUrls.add(downloadUrl)
                }

                val finalAssignment = assignment.copy(guidelineUrls = uploadedUrls)

                firestore.collection("assignments")
                    .add(finalAssignment)
                    .await()
                
                _uploadSuccess.value = true
                _selectedFiles.value = emptyList()
                syncData()
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
                // Upload real file to Firebase Storage
                val fileName = "submissions/${studentId}_$assignmentId.pdf"
                val ref = storage.reference.child(fileName)
                ref.putFile(fileUri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                
                val userDoc = firestore.collection("users").document(studentId).get().await()
                val studentName = userDoc.getString("name") ?: "Student"
                val regNo = userDoc.getString("regNo") ?: studentId

                val submissionData = hashMapOf(
                    "assignmentId" to assignmentId,
                    "studentId" to studentId,
                    "studentName" to studentName,
                    "regNo" to regNo,
                    "fileUrl" to downloadUrl,
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
