package com.acadmate.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.attendance.domain.FacultyAttendanceViewModel
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.MeshBackground
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.acadmate.attendance.domain.StudentAttendanceRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyMarkAttendanceScreen(
    classId: String,
    onBackClick: () -> Unit,
    onViewAttendanceClick: (String) -> Unit = {},
    viewModel: FacultyAttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBroadcasting = uiState.isSessionActive
    var timeLeft by remember { mutableStateOf(600) } // 10 minutes
    
    var selectedSubject by remember { mutableStateOf(if (classId == "General") "" else classId) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(classId) {
        if (!isBroadcasting) {
            viewModel.loadSessionAttendance(classId)
        }
    }

    // Auto-select first assigned course if none selected and it's a general request
    LaunchedEffect(uiState.assignedCourses) {
        if (selectedSubject.isBlank() && uiState.assignedCourses.isNotEmpty()) {
            selectedSubject = uiState.assignedCourses.first()
        }
    }

    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            timeLeft = 600
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isBroadcasting) "Live Session: $selectedSubject" else "Attendance Setup") },
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
        ) {
            if (isBroadcasting) {
                // Broadcast Status Card
                BroadcastStatusCard(
                    timeLeft = timeLeft,
                    presentCount = uiState.students.count { it.isPresent },
                    totalCount = uiState.students.size
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Live Student List ($selectedSubject)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontWeight = FontWeight.Bold
                )
                
                // Live Student List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presentStudents = uiState.students.filter { it.isPresent }
                    if (presentStudents.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Waiting for students to mark attendance...", color = Color.Gray)
                            }
                        }
                    }
                    items(presentStudents) { student ->
                        LiveStudentItem(student)
                    }
                }
            } else {
                // Setup Card
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    AcadMateCard(variant = CardVariant.Elevated) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Podcasts, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Ready to start attendance?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Subject Selection
                            Text("Select Subject for this Session:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedSubject.ifBlank { "No assigned subjects" },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                )
                                
                                if (uiState.assignedCourses.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        uiState.assignedCourses.forEach { subject ->
                                            DropdownMenuItem(
                                                text = { Text(subject) },
                                                onClick = {
                                                    selectedSubject = subject
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("This will broadcast an ultrasonic signal for 10 minutes.", textAlign = TextAlign.Center, color = Color.Gray)
                        }
                    }
                }
            }

            // Bottom Actions
            Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AcadMateButton(
                        text = if (isBroadcasting) "Stop Session" else "Start Session",
                        onClick = { 
                            if (isBroadcasting) viewModel.stopAttendanceSession() 
                            else viewModel.startAttendanceSession(selectedSubject) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isBroadcasting || selectedSubject.isNotBlank()
                    )
                    
                    if (isBroadcasting) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { onViewAttendanceClick(selectedSubject) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manual Entry / View All Students")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BroadcastStatusCard(timeLeft: Int, presentCount: Int, totalCount: Int) {
    AcadMateCard(
        variant = CardVariant.Flat,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Time Remaining", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("Presence", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$presentCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/$totalCount",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveStudentItem(student: StudentAttendanceRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (student.profilePictureUrl != null) {
                    AsyncImage(
                        model = student.profilePictureUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(student.studentName.take(1))
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(student.studentName, fontWeight = FontWeight.Bold)
                Text(student.enrollmentNumber, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981))
        }
    }
}
