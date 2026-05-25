package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.LeaveRequest
import com.acadmate.core.model.LeaveStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.net.Uri
import com.acadmate.core.db.UserRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _leaveRequests = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val leaveRequests: StateFlow<List<LeaveRequest>> = _leaveRequests

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    init {
        observeUserAndLoadRequests()
    }

    private fun observeUserAndLoadRequests() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            // Sync to ensure we have regNo locally
            userRepository.syncUserData(userId)
            
            userRepository.getCurrentUser().collectLatest { user ->
                val regNo = user?.regNo
                if (regNo != null) {
                    loadMyRequests(regNo)
                }
            }
        }
    }

    private fun loadMyRequests(regNo: String) {
        viewModelScope.launch {
            try {
                // Using a snapshot listener for real-time updates
                firestore.collection("leave_requests")
                    .whereEqualTo("studentId", regNo)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("LeaveViewModel", "Error loading leaves: ${e.message}")
                            return@addSnapshotListener
                        }
                        
                        val requests = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                doc.toObject(LeaveRequest::class.java)
                            } catch (parseError: Exception) {
                                Log.e("LeaveViewModel", "Error parsing leave: ${parseError.message}")
                                null
                            }
                        } ?: emptyList()
                        
                        // Sort locally to avoid needing a complex Firestore index immediately
                        _leaveRequests.value = requests.sortedByDescending { it.createdAt }
                    }
            } catch (e: Exception) {
                Log.e("LeaveViewModel", "Unexpected error: ${e.message}")
            }
        }
    }

    fun submitLeaveRequest(
        startDate: Long, 
        endDate: Long, 
        reason: String, 
        medicalIssue: String?,
        attachmentUri: Uri?
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val user = userRepository.getCurrentUser().first()
                val regNo = user?.regNo ?: return@launch
                val userName = user.name

                var attachmentUrl: String? = null
                
                // Upload attachment if present
                if (attachmentUri != null) {
                    val fileRef = storage.reference.child("leave_attachments/${UUID.randomUUID()}")
                    fileRef.putFile(attachmentUri).await()
                    attachmentUrl = fileRef.downloadUrl.await().toString()
                }

                val request = LeaveRequest(
                    id = UUID.randomUUID().toString(),
                    studentId = regNo,
                    studentName = userName,
                    facultyId = "ADMIN",
                    facultyName = "Admin",
                    startDate = startDate,
                    endDate = endDate,
                    reason = reason,
                    medicalIssue = medicalIssue,
                    attachmentUrl = attachmentUrl,
                    status = LeaveStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )
                firestore.collection("leave_requests").document(request.id).set(request).await()
                // loadMyRequests is handled by the snapshot listener now
            } catch (e: Exception) {
                Log.e("LeaveViewModel", "Error submitting leave: ${e.message}")
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
