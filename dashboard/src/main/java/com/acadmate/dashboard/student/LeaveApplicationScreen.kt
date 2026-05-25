package com.acadmate.dashboard.student

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.ui.window.DialogProperties
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
            onApply = { start, end, reason, medicalIssue, attachmentUri ->
                viewModel.submitLeaveRequest(start, end, reason, medicalIssue, attachmentUri)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveDialog(
    onDismiss: () -> Unit,
    onApply: (Long, Long, String, String?, Uri?) -> Unit,
    isSubmitting: Boolean
) {
    var reason by remember { mutableStateOf("") }
    var medicalIssue by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    val dateRangePickerState = rememberDateRangePickerState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachmentUri = uri
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) {
                    fileName = c.getString(nameIndex)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        title = { Text("Apply for Leave", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select date range and provide a reason for your absence. You can also attach a medical certificate if applicable.", style = MaterialTheme.typography.bodySmall)
                
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.height(400.dp),
                    title = null,
                    headline = null,
                    showModeToggle = false
                )

                AcadMateTextField(
                    value = medicalIssue,
                    onValueChange = { medicalIssue = it },
                    label = "Medical Issue (Optional)",
                    placeholder = "e.g. Fever, Fracture"
                )

                AcadMateTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Reason for Leave",
                    placeholder = "e.g. Personal emergency or detailed medical reason",
                    modifier = Modifier.height(100.dp),
                    singleLine = false
                )

                OutlinedCard(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.UploadFile, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = fileName ?: "Attach Supporting Document",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Medical Certificate or Prescription",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AcadMateButton(
                text = "Submit to Admin",
                onClick = { 
                    val start = dateRangePickerState.selectedStartDateMillis ?: System.currentTimeMillis()
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    onApply(start, end, reason, medicalIssue.ifBlank { null }, attachmentUri)
                },
                enabled = reason.isNotBlank() && 
                          dateRangePickerState.selectedStartDateMillis != null && 
                          !isSubmitting,
                loading = isSubmitting
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
