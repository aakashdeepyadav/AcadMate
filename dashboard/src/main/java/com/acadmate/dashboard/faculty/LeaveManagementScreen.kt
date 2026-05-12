package com.acadmate.dashboard.faculty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acadmate.core.model.LeaveRequest
import com.acadmate.core.model.LeaveStatus
import com.acadmate.designsystem.components.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveManagementScreen(
    onBackClick: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    
    var requests by remember { mutableStateOf<List<LeaveRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadRequests() {
        scope.launch {
            isLoading = true
            try {
                val snapshot = firestore.collection("leave_requests")
                    .whereEqualTo("status", LeaveStatus.PENDING.name)
                    .get()
                    .await()
                requests = snapshot.toObjects(LeaveRequest::class.java)
            } catch (e: Exception) { } finally {
                isLoading = false
            }
        }
    }

    fun updateStatus(requestId: String, status: LeaveStatus, note: String) {
        scope.launch {
            try {
                val facultyId = FirebaseAuth.getInstance().currentUser?.uid ?: "Faculty"
                firestore.collection("leave_requests").document(requestId)
                    .update(
                        mapOf(
                            "status" to status.name,
                            "handledBy" to facultyId,
                            "responseNote" to note
                        )
                    ).await()
                loadRequests()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(Unit) { loadRequests() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (requests.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No pending requests", color = Color.Gray)
                        }
                    }
                }
                items(requests) { request ->
                    PendingLeaveItem(
                        request = request,
                        onAction = { status, note -> updateStatus(request.id, status, note) }
                    )
                }
            }
        }
    }
}

@Composable
fun PendingLeaveItem(
    request: LeaveRequest,
    onAction: (LeaveStatus, String) -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    var responseNote by remember { mutableStateOf("") }

    AcadMateCard(variant = CardVariant.Flat) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(request.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${sdf.format(Date(request.startDate))} - ${sdf.format(Date(request.endDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier.background(Color(0xFFF5A623).copy(alpha = 0.1f), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("PENDING", color = Color(0xFFF5A623), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(request.reason, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(16.dp))
            AcadMateTextField(
                value = responseNote,
                onValueChange = { responseNote = it },
                label = "Faculty Response (Optional)",
                placeholder = "Reason for approval/rejection"
            )
            
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onAction(LeaveStatus.REJECTED, responseNote) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }
                Button(
                    onClick = { onAction(LeaveStatus.APPROVED, responseNote) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve")
                }
            }
        }
    }
}
