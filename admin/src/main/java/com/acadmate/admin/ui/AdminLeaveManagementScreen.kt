package com.acadmate.admin.ui

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.acadmate.core.model.LeaveRequest
import com.acadmate.core.model.LeaveStatus
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeaveManagementScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Approved", "Rejected")
    val leaves by viewModel.leaveRequests.collectAsState()

    val filteredLeaves = remember(leaves, selectedTab) {
        when(selectedTab) {
            0 -> leaves.filter { it.status == LeaveStatus.PENDING }
            1 -> leaves.filter { it.status == LeaveStatus.APPROVED }
            else -> leaves.filter { it.status == LeaveStatus.REJECTED }
        }
    }

    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    if (previewImageUrl != null) {
        MedicalCertificateViewer(
            url = previewImageUrl!!,
            onDismiss = { previewImageUrl = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Management") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (filteredLeaves.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ${tabs[selectedTab]} requests", color = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(LocalSpacing.current.md),
                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
            ) {
                items(filteredLeaves, key = { it.id }) { leave ->
                    LeaveRequestItem(
                        leave = leave,
                        onApprove = { note -> viewModel.updateLeaveStatus(leave.id, LeaveStatus.APPROVED, note) },
                        onReject = { note -> viewModel.updateLeaveStatus(leave.id, LeaveStatus.REJECTED, note) },
                        onViewAttachment = { url -> previewImageUrl = url }
                    )
                }
            }
        }
    }
}

@Composable
fun LeaveRequestItem(
    leave: LeaveRequest,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onViewAttachment: (String) -> Unit = {}
) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var responseNote by remember { mutableStateOf("") }

    AcadMateCard(variant = CardVariant.Elevated) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(leave.studentName, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                AssistChip(
                    onClick = { },
                    label = { Text(leave.status.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = when(leave.status) {
                            LeaveStatus.APPROVED -> Color(0xFF10B981)
                            LeaveStatus.REJECTED -> Color.Red
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val medicalIssue = leave.medicalIssue
            if (!medicalIssue.isNullOrBlank()) {
                Text("Medical Issue:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(medicalIssue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text("Reason:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(leave.reason, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text("${sdf.format(Date(leave.startDate))} to ${sdf.format(Date(leave.endDate))}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            val attachmentUrl = leave.attachmentUrl
            if (!attachmentUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onViewAttachment(attachmentUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Attachment, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View Medical Certificate", style = MaterialTheme.typography.labelLarge)
                }
            }
            
            if (leave.status == LeaveStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                
                AcadMateTextField(
                    value = responseNote,
                    onValueChange = { responseNote = it },
                    label = "Admin Response Note",
                    placeholder = "Optional feedback for student..."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReject(responseNote) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                    AcadMateButton(
                        text = "Approve",
                        onClick = { onApprove(responseNote) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                val responseNoteText = leave.responseNote
                if (!responseNoteText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Response Note:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(responseNoteText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun MedicalCertificateViewer(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(contentAlignment = Alignment.Center) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        // This is crucial: don't load original size if it's too large
                        // limiting to 2048px is usually safe for hardware canvas
                        .size(2048)
                        .build(),
                    contentDescription = "Medical Certificate",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}
