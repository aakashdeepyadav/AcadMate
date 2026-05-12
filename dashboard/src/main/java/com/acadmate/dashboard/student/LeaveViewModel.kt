package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.LeaveRequest
import com.acadmate.core.model.LeaveStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LeaveViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _leaveRequests = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val leaveRequests: StateFlow<List<LeaveRequest>> = _leaveRequests

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    init {
        loadMyRequests()
    }

    fun loadMyRequests() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("leave_requests")
                    .whereEqualTo("studentId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                _leaveRequests.value = snapshot.toObjects(LeaveRequest::class.java)
            } catch (e: Exception) { }
        }
    }

    fun submitLeaveRequest(startDate: Long, endDate: Long, reason: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Student"
        
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val request = LeaveRequest(
                    id = UUID.randomUUID().toString(),
                    studentId = userId,
                    studentName = userName,
                    startDate = startDate,
                    endDate = endDate,
                    reason = reason,
                    status = LeaveStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )
                firestore.collection("leave_requests").document(request.id).set(request).await()
                loadMyRequests()
            } catch (e: Exception) { } finally {
                _isSubmitting.value = false
            }
        }
    }
}
