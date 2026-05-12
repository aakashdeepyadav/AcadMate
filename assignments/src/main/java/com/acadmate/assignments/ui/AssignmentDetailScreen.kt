package com.acadmate.assignments.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.Assignment
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailScreen(
    assignmentId: String,
    onBackClick: () -> Unit,
    viewModel: AssignmentViewModel = hiltViewModel()
) {
    val assignments by viewModel.assignments.collectAsState()
    val assignment = assignments.find { it.id == assignmentId }
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            viewModel.resetUploadState()
            selectedUri = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assignment Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (assignment == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AcadMateCard(variant = CardVariant.Elevated) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = assignment.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(assignment.dueDate))
                            Text("Due $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = assignment.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                AcadMateCard(variant = CardVariant.Flat) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Your Submission", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        
                        if (selectedUri != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Selected: PDF Document", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { selectedUri = null }) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { launcher.launch("application/pdf") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AttachFile, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Attach PDF")
                            }
                        }
                    }
                }

                AcadMateButton(
                    text = "Submit Assignment",
                    onClick = { 
                        selectedUri?.let { viewModel.submitAssignment(assignment.id, it, userId) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedUri != null && !isUploading,
                    loading = isUploading
                )
                
                if (uploadSuccess) {
                    Text(
                        "✓ Submitted Successfully",
                        color = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
