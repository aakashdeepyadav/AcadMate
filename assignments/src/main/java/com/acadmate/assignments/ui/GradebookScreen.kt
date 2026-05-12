package com.acadmate.assignments.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class StudentGrade(
    val id: String = "",
    val name: String = "",
    val rollNumber: String = "",
    val isSubmitted: Boolean = false,
    val grade: String? = null,
    val marks: Int? = null,
    val submissionId: String? = null
)

@HiltViewModel
class GradebookViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    var assignmentName by mutableStateOf("Loading...")
    var maxMarks = 100
    
    private val _students = mutableStateListOf<StudentGrade>()
    val students: List<StudentGrade> get() = _students

    private val _assignments = mutableStateListOf<com.acadmate.core.model.Assignment>()
    val assignments: List<com.acadmate.core.model.Assignment> get() = _assignments

    var selectedAssignmentId by mutableStateOf<String?>(null)
    
    init {
        loadAssignments()
    }

    private fun loadAssignments() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("assignments").get().await()
                val list = snapshot.toObjects(com.acadmate.core.model.Assignment::class.java)
                _assignments.clear()
                _assignments.addAll(list)
                if (list.isNotEmpty()) {
                    selectAssignment(list[0].id)
                }
            } catch (_: Exception) {}
        }
    }

    fun selectAssignment(id: String) {
        selectedAssignmentId = id
        val assignment = _assignments.find { it.id == id }
        assignmentName = assignment?.title ?: "Unknown"
        loadSubmissionData(id)
    }

    private fun loadSubmissionData(assignmentId: String) {
        viewModelScope.launch {
            try {
                // Fetch submissions for this assignment
                val submissionSnapshot = firestore.collection("submissions")
                    .whereEqualTo("assignmentId", assignmentId)
                    .get()
                    .await()
                
                // Fetch all students to see who hasn't submitted
                val studentSnapshot = firestore.collection("users")
                    .whereEqualTo("role", "STUDENT")
                    .get()
                    .await()
                
                val studentGrades = studentSnapshot.documents.map { studentDoc ->
                    val subDoc = submissionSnapshot.documents.find { it.getString("studentId") == studentDoc.id }
                    StudentGrade(
                        id = studentDoc.id,
                        name = studentDoc.getString("name") ?: "Unknown",
                        rollNumber = studentDoc.getString("regNo") ?: "N/A",
                        isSubmitted = subDoc != null,
                        marks = subDoc?.getLong("marks")?.toInt(),
                        grade = subDoc?.getString("grade"),
                        submissionId = subDoc?.id
                    )
                }
                _students.clear()
                _students.addAll(studentGrades)
            } catch (e: Exception) {
                assignmentName = "Error loading data"
            }
        }
    }

    fun updateMarks(studentId: String, newMarks: Int?) {
        val index = _students.indexOfFirst { it.id == studentId }
        if (index != -1) {
            val student = _students[index]
            val grade = newMarks?.let { calculateGrade(it) }
            _students[index] = student.copy(marks = newMarks, grade = grade)
            
            // Save to Firestore if submission exists
            student.submissionId?.let { subId ->
                viewModelScope.launch {
                    firestore.collection("submissions").document(subId).update(
                        "marks", newMarks,
                        "grade", grade
                    )
                }
            }
        }
    }

    private fun calculateGrade(marks: Int): String {
        return when {
            marks >= 90 -> "A+"
            marks >= 80 -> "A"
            marks >= 70 -> "B+"
            marks >= 60 -> "B"
            marks >= 50 -> "C"
            else -> "F"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradebookScreen(
    viewModel: GradebookViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gradebook", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Save all grades to backend */ }) {
                        Icon(Icons.Default.Save, contentDescription = "Save All")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Assignment Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.assignments) { assignment ->
                    FilterChip(
                        selected = viewModel.selectedAssignmentId == assignment.id,
                        onClick = { viewModel.selectAssignment(assignment.id) },
                        label = { Text(assignment.title) }
                    )
                }
            }

            // Assignment Header Info
            AcadMateCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalSpacing.current.md),
                variant = CardVariant.Gradient,
                gradientColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                cornerRadius = 16.dp,
                contentPadding = 20.dp
            ) {
                Column {
                    Text(
                        text = "Grading Assignment",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.assignmentName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Marks: ${viewModel.maxMarks}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${viewModel.students.count { it.grade != null }} / ${viewModel.students.size} Graded",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Student List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = LocalSpacing.current.md,
                    end = LocalSpacing.current.md,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.students) { student ->
                    StudentGradingRow(
                        student = student,
                        onMarksChanged = { viewModel.updateMarks(student.id, it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradingRow(
    student: StudentGrade,
    onMarksChanged: (Int?) -> Unit
) {
    AcadMateCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.Flat,
        backgroundColor = Color.White,
        cornerRadius = 12.dp,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = student.name.first().toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${student.rollNumber} • ${if(student.isSubmitted) "Submitted" else "Not Submitted"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if(student.isSubmitted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
            
            // Marks Input
            if (student.isSubmitted) {
                var textValue by remember(student.marks) { mutableStateOf(student.marks?.toString() ?: "") }
                
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            // Ensure it's not more than max marks (100)
                            val intVal = newValue.toIntOrNull()
                            if (intVal == null || intVal <= 100) {
                                textValue = newValue
                                onMarksChanged(intVal)
                            }
                        }
                    },
                    modifier = Modifier.width(72.dp).height(52.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true,
                    placeholder = { Text("-", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Grade Badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (student.grade != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.grade ?: "-",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (student.grade != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No File",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
