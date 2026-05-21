package com.acadmate.dashboard.faculty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acadmate.core.model.LeaveRequest
import com.acadmate.core.model.LeaveStatus
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
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
                val facultyId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val snapshot = firestore.collection("leave_requests")
                    .whereEqualTo("facultyId", facultyId)
                    .whereEqualTo("status", LeaveStatus.PENDING.name)
                    .get()
                    .await()
                requests = snapshot.toObjects(LeaveRequest::class.java)
            } catch (_: Exception) { } finally {
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
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) { loadRequests() }

    MeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Leave Requests", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadRequests() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "All caught up!", 
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "No pending leave requests to review.",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
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
}

@Composable
fun PendingLeaveItem(
    request: LeaveRequest,
    onAction: (LeaveStatus, String) -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    var responseNote by remember { mutableStateOf("") }

    AcadMateCard(variant = CardVariant.Flat) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = request.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${sdf.format(Date(request.startDate))} - ${sdf.format(Date(request.endDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = Color(0xFFF5A623).copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "PENDING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color(0xFFF5A623),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text(text = "Reason:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = request.reason, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(20.dp))
            AcadMateTextField(
                value = responseNote,
                onValueChange = { responseNote = it },
                label = "Faculty Response (Optional)",
                placeholder = "Add a note for the student..."
            )
            
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onAction(LeaveStatus.REJECTED, responseNote) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }
                AcadMateButton(
                    text = "Approve",
                    onClick = { onAction(LeaveStatus.APPROVED, responseNote) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
