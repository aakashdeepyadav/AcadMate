package com.acadmate.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.acadmate.attendance.domain.FacultyAttendanceViewModel
import com.acadmate.attendance.domain.StudentAttendanceRecord
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultySessionAttendanceScreen(
    classId: String,
    hour: Int? = null,
    date: String? = null,
    viewModel: FacultyAttendanceViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(classId, hour, date) {
        viewModel.loadSessionAttendance(classId, hour, date)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.className.ifEmpty { classId }, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.sessionTime.ifEmpty { "Active Session" }, 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val presentCount = uiState.students.count { it.isPresent }
                val totalCount = uiState.students.size
                
                AttendanceStatCard(
                    label = "Present",
                    count = presentCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                AttendanceStatCard(
                    label = "Absent",
                    count = (totalCount - presentCount).toString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search by name or Reg No") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Student List
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                val filteredStudents = uiState.students.filter {
                    it.studentName.contains(searchQuery, ignoreCase = true) ||
                            it.enrollmentNumber.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredStudents) { student ->
                        StudentAttendanceItem(
                            student = student,
                            onToggle = { viewModel.toggleAttendance(student.studentId, student.isPresent) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceItem(
    student: StudentAttendanceRecord,
    onToggle: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(student.studentName, fontWeight = FontWeight.Bold)
                if (student.hasAnomaly) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Warning, 
                        contentDescription = "Anomaly Detected",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        supportingContent = { 
            Column {
                Text(student.enrollmentNumber)
                if (student.hasAnomaly) {
                    Text(
                        student.anomalyReason ?: "Security Alert",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (student.profilePictureUrl != null) {
                    AsyncImage(
                        model = student.profilePictureUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            student.studentName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (student.isPresent) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Attendance",
                    tint = if (student.isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
fun AttendanceStatCard(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    AcadMateCard(
        modifier = modifier,
        variant = CardVariant.Outlined
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}
