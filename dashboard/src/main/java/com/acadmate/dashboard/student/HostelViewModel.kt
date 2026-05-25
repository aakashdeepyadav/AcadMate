package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

data class HostelLeave(
    val id: String = "",
    val studentId: String = "",
    val reason: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)

data class HostelUiState(
    val menu: List<MessItem> = emptyList(),
    val leaves: List<HostelLeave> = emptyList(),
    val roomInfo: String = "Not Assigned",
    val isLoading: Boolean = false
)

@HiltViewModel
class HostelViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(HostelUiState())
    val uiState: StateFlow<HostelUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Fetch Mess Menu
                val menuDoc = firestore.collection("institution").document("hostel").get().await()
                // Fetch Leaves
                val leavesSnapshot = firestore.collection("hostel_leaves")
                    .whereEqualTo("studentId", userId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()
                
                val leaves = leavesSnapshot.documents.map { doc ->
                    HostelLeave(
                        id = doc.id,
                        studentId = doc.getString("studentId") ?: "",
                        reason = doc.getString("reason") ?: "",
                        startDate = doc.getString("startDate") ?: "",
                        endDate = doc.getString("endDate") ?: "",
                        status = doc.getString("status") ?: "PENDING"
                    )
                }

                // Fetch Room Info
                val userDoc = firestore.collection("users").document(userId).get().await()
                val room = userDoc.getString("roomInfo") ?: "Block B, Room 304 (Mock)"

                _uiState.value = HostelUiState(
                    leaves = leaves,
                    roomInfo = room,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun applyLeave(reason: String, from: String, to: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val leave = hashMapOf(
                    "studentId" to userId,
                    "reason" to reason,
                    "startDate" to from,
                    "endDate" to to,
                    "status" to "PENDING",
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("hostel_leaves").add(leave).await()
                loadData()
            } catch (e: Exception) {}
        }
    }
}
