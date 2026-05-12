package com.acadmate.dashboard.student

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.LeaveRequest
import com.acadmate.core.model.LeaveStatus
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveApplicationScreen(
    onBackClick: () -> Unit,
    viewModel: LeaveViewModel = hiltViewModel()
) {
    val requests by viewModel.leaveRequests.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    var showApplyDialog by remember { mutableStateOf(false) }

    if (showApplyDialog) {
        ApplyLeaveDialog(
            onDismiss = { showApplyDialog = false },
            onApply = { start, end, reason ->
                viewModel.submitLeaveRequest(start, end, reason)
                showApplyDialog = false
            },
            isSubmitting = isSubmitting
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Applications", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showApplyDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Apply Leave") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (requests.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("No leave requests found", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(requests) { request ->
                        LeaveRequestItem(request)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaveRequestItem(request: LeaveRequest) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val statusColor = when (request.status) {
        LeaveStatus.PENDING -> Color(0xFFF5A623)
        LeaveStatus.APPROVED -> Color(0xFF10B981)
        LeaveStatus.REJECTED -> Color(0xFFEF4444)
    }

    AcadMateCard(variant = CardVariant.Flat) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${sdf.format(Date(request.startDate))} - ${sdf.format(Date(request.endDate))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Applied on ${sdf.format(Date(request.createdAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = request.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Reason: ${request.reason}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (request.responseNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Note: ${request.responseNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ApplyLeaveDialog(
    onDismiss: () -> Unit,
    onApply: (Long, Long, String) -> Unit,
    isSubmitting: Boolean
) {
    var reason by remember { mutableStateOf("") }
    // For demo, we just use current time as start and +2 days as end
    val start = System.currentTimeMillis()
    val end = start + (2 * 24 * 60 * 60 * 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply for Leave", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select date range and reason for your absence.", style = MaterialTheme.typography.bodySmall)
                
                AcadMateTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Reason for Leave",
                    placeholder = "e.g. Medical emergency",
                    modifier = Modifier.height(100.dp),
                    singleLine = false
                )
            }
        },
        confirmButton = {
            AcadMateButton(
                text = "Submit Request",
                onClick = { onApply(start, end, reason) },
                enabled = reason.isNotBlank() && !isSubmitting,
                loading = isSubmitting
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
