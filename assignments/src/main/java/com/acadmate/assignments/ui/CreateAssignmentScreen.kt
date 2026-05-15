package com.acadmate.assignments.ui

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
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssignmentScreen(
    viewModel: AssignmentViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    val facultyCourses by viewModel.facultyCourses.collectAsState()
    
    val facultyId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 604800000 // 1 week from now
    )

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            onBackClick()
            viewModel.resetUploadState()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Assignment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AcadMateCard(variant = CardVariant.Elevated) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Basic Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    AcadMateTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Assignment Title",
                        placeholder = "e.g. Android Components Lab"
                    )

                    AcadMateTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description / Instructions",
                        placeholder = "Provide clear instructions for students...",
                        singleLine = false,
                        modifier = Modifier.height(120.dp)
                    )
                }
            }

            AcadMateCard(variant = CardVariant.Flat) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Target & Deadline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    // Subject Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        AcadMateTextField(
                            value = selectedSubject.ifEmpty { "Select Subject" },
                            onValueChange = {},
                            readOnly = true,
                            label = "Course",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            facultyCourses.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject) },
                                    onClick = {
                                        selectedSubject = subject
                                        expanded = false
                                    }
                                )
                            }
                            if (facultyCourses.isEmpty()) {
                                DropdownMenuItem(text = { Text("No courses assigned") }, onClick = {})
                            }
                        }
                    }

                    // Date Picker Trigger
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Event, null)
                        Spacer(Modifier.width(8.dp))
                        val dateText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))
                        Text("Due Date: $dateText")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AcadMateButton(
                text = "Publish Assignment",
                onClick = {
                    val assignment = Assignment(
                        title = title,
                        description = description,
                        subjectId = selectedSubject,
                        facultyId = facultyId,
                        dueDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    )
                    viewModel.uploadAssignment(assignment)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && description.isNotBlank() && selectedSubject.isNotBlank() && !isUploading,
                loading = isUploading
            )
        }
    }
}
