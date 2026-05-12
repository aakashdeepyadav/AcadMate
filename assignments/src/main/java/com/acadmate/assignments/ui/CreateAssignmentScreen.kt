package com.acadmate.assignments.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.Assignment
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssignmentScreen(
    viewModel: AssignmentViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf("") }
    val facultyId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val isUploading by viewModel.isUploading.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            onBackClick()
            viewModel.resetUploadState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Assignment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isUploading
            )

            OutlinedTextField(
                value = subjectId,
                onValueChange = { subjectId = it },
                label = { Text("Subject ID / Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val assignment = Assignment(
                        title = title,
                        description = description,
                        subjectId = subjectId,
                        facultyId = facultyId,
                        dueDate = System.currentTimeMillis() + 604800000 // Default 1 week
                    )
                    viewModel.uploadAssignment(assignment)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && description.isNotBlank() && !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Post Assignment")
                }
            }
        }
    }
}
