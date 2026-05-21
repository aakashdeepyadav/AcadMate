package com.acadmate.admin.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.TimetableEntity
import com.acadmate.core.model.Subject
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.SectionHeader
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import java.util.Calendar

@Serializable
data class AiTimetableSlot(
    val dayIndex: Int,
    val subject: String,
    val faculty: String = "",
    val startTime: String,
    val endTime: String,
    val room: String
)

@Serializable
data class AiTimetableResponse(
    val schedule: List<AiTimetableSlot>
)

data class SubjectConstraint(
    val subject: Subject,
    val classesPerWeek: Int
)

sealed class AiGeneratorState {
    object Idle : AiGeneratorState()
    object Generating : AiGeneratorState()
    data class Preview(val slots: List<TimetableEntity>) : AiGeneratorState()
    data class Success(val message: String) : AiGeneratorState()
    data class Error(val message: String) : AiGeneratorState()
}

@HiltViewModel
class AiTimetableViewModel @Inject constructor(
    private val generativeModel: GenerativeModel
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val _uiState = MutableStateFlow<AiGeneratorState>(AiGeneratorState.Idle)
    val uiState: StateFlow<AiGeneratorState> = _uiState

    private val _availableSubjects = MutableStateFlow<List<Subject>>(emptyList())
    val availableSubjects: StateFlow<List<Subject>> = _availableSubjects

    private val _allCourses = MutableStateFlow<List<Course>>(emptyList())
    val allCourses: StateFlow<List<Course>> = _allCourses

    private val _allFaculty = MutableStateFlow<List<String>>(emptyList())
    val allFaculty: StateFlow<List<String>> = _allFaculty

    private val _constraints = MutableStateFlow<List<SubjectConstraint>>(emptyList())
    val constraints: StateFlow<List<SubjectConstraint>> = _constraints

    init {
        loadSubjects()
        loadCourses()
        loadFaculty()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("subjects").get().await()
                _availableSubjects.value = snapshot.toObjects(Subject::class.java)
            } catch (e: Exception) {}
        }
    }

    private fun loadCourses() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("courses").get().await()
                _allCourses.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Course::class.java)?.copy(id = doc.id)
                }
            } catch (e: Exception) {}
        }
    }

    private fun loadFaculty() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("role", "FACULTY")
                    .get()
                    .await()
                _allFaculty.value = snapshot.documents.map { it.getString("name") ?: "Unknown" }
            } catch (e: Exception) {}
        }
    }

    fun addConstraint(subject: Subject, count: Int) {
        val current = _constraints.value.toMutableList()
        current.removeAll { it.subject.id == subject.id }
        current.add(SubjectConstraint(subject, count))
        _constraints.value = current
    }

    fun removeConstraint(subjectId: String) {
        _constraints.value = _constraints.value.filter { it.subject.id != subjectId }
    }

    fun generateSchedule(
        startTime: String, 
        endTime: String, 
        lunchTime: String, 
        lunchDuration: Int, 
        customSuggestions: String = "",
        availableRooms: String = ""
    ) {
        _uiState.value = AiGeneratorState.Generating
        viewModelScope.launch {
            try {
                val subjectRules = _constraints.value.joinToString("\n") { 
                    "- ${it.subject.name} (${it.subject.code}): ${it.classesPerWeek} classes per week" 
                }
                
                val facultyInfo = _allCourses.value.joinToString("\n") { 
                    "- Subject: ${it.name} -> Faculty: ${it.assignedFaculty ?: "TBA"}"
                }

                val prompt = """
                    You are an expert academic scheduler. Generate a JSON timetable for a 5-day week (Monday to Friday, dayIndex 1 to 5).
                    
                    Core Constraints:
                    - College Hours: $startTime to $endTime
                    - Lunch Break: $lunchTime for $lunchDuration minutes.
                    - Subject Requirements:
                    $subjectRules
                    
                    Faculty Assignments (DO NOT use "TBA" if faculty is listed here):
                    $facultyInfo
                    
                    ${if (availableRooms.isNotBlank()) "Available Rooms to use: $availableRooms" else "Use realistic room numbers like B-101, Lab-2, etc."}
                    
                    Additional Instructions/Preferences:
                    ${if (customSuggestions.isNotBlank()) customSuggestions else "None provided. Use standard academic distribution."}
                    
                    Rules:
                    1. Classes must be between 45 to 60 minutes long.
                    2. No subject should have more than 2 classes in a single day.
                    3. Ensure no overlaps between slots.
                    4. Return ONLY a valid JSON object in this format: {"schedule": [{"dayIndex": 1, "subject": "Subject Name", "startTime": "HH:mm", "endTime": "HH:mm", "room": "Room No", "faculty": "Faculty Name"}]}
                    5. Use ONLY the provided Available Rooms if specified.
                    6. Assign the correct faculty to each subject based on the 'Faculty Assignments' list provided above.
                """.trimIndent()

                val result = generativeModel.generateContent(prompt)
                val responseText = result.text?.replace("```json", "")?.replace("```", "")?.trim() ?: throw Exception("AI returned empty response")
                
                val aiResponse = json.decodeFromString<AiTimetableResponse>(responseText)
                val entities = aiResponse.schedule.map { item: AiTimetableSlot ->
                    // Priority 1: Use faculty assigned by AI (if valid)
                    // Priority 2: Auto-assign from course mapping
                    // Priority 3: Leave blank
                    
                    val courseMappingFaculty = _allCourses.value.find { 
                        it.name.equals(item.subject, ignoreCase = true) || it.code.equals(item.subject, ignoreCase = true) ||
                        item.subject.contains(it.name, ignoreCase = true)
                    }?.assignedFaculty

                    val finalFaculty = when {
                        !item.faculty.isNullOrBlank() && item.faculty != "TBA" -> item.faculty
                        !courseMappingFaculty.isNullOrBlank() -> courseMappingFaculty
                        else -> ""
                    }

                    TimetableEntity(
                        id = "AI_${java.util.UUID.randomUUID()}",
                        dayOfWeek = item.dayIndex,
                        subject = item.subject,
                        faculty = finalFaculty,
                        startTime = item.startTime,
                        endTime = item.endTime,
                        room = item.room,
                        color = 0xFF4A90E2.toInt()
                    )
                }
                
                _uiState.value = AiGeneratorState.Preview(entities)
            } catch (e: Exception) {
                _uiState.value = AiGeneratorState.Error("Generation failed: ${e.message}")
            }
        }
    }

    private suspend fun logAdminAction(title: String, type: com.acadmate.core.model.ActionType, description: String) {
        try {
            val actionData = hashMapOf(
                "title" to title,
                "timestamp" to System.currentTimeMillis(),
                "type" to type.name,
                "description" to description
            )
            firestore.collection("admin_logs").add(actionData).await()
        } catch (e: Exception) {}
    }

    fun applySchedule(slots: List<TimetableEntity>) {
        viewModelScope.launch {
            _uiState.value = AiGeneratorState.Generating
            try {
                // Batch write to Firestore
                val batch = firestore.batch()
                
                // Optional: Clear existing timetable first? For now, we just add.
                // Better to clear the days that are in the new schedule.
                val daysToClear = slots.map { it.dayOfWeek }.distinct()
                for (day in daysToClear) {
                    val existing = firestore.collection("global_timetable")
                        .whereEqualTo("dayOfWeek", day)
                        .get()
                        .await()
                    existing.documents.forEach { batch.delete(it.reference) }
                }

                slots.forEach { slot ->
                    val docRef = firestore.collection("global_timetable").document()
                    val map = hashMapOf(
                        "id" to docRef.id,
                        "dayOfWeek" to slot.dayOfWeek,
                        "subject" to slot.subject,
                        "faculty" to slot.faculty,
                        "startTime" to slot.startTime,
                        "endTime" to slot.endTime,
                        "room" to slot.room,
                        "color" to slot.color
                    )
                    batch.set(docRef, map)
                }

                batch.commit().await()
                logAdminAction("AI Timetable Applied", com.acadmate.core.model.ActionType.INSTITUTION_UPDATED, "New schedule generated by AI was applied for ${daysToClear.size} days.")
                _uiState.value = AiGeneratorState.Success("Timetable applied successfully for ${daysToClear.size} days!")
            } catch (e: Exception) {
                _uiState.value = AiGeneratorState.Error("Failed to apply: ${e.message}")
            }
        }
    }

    fun reset() {
        _uiState.value = AiGeneratorState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTimetableGeneratorScreen(
    viewModel: AiTimetableViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val subjects by viewModel.availableSubjects.collectAsState()
    val constraints by viewModel.constraints.collectAsState()
    val allFaculty by viewModel.allFaculty.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Schedule Builder", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            when (val state = uiState) {
                is AiGeneratorState.Idle -> {
                    ConstraintsInputForm(subjects, constraints, viewModel)
                }
                is AiGeneratorState.Generating -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Gemini is optimizing your schedule...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is AiGeneratorState.Preview -> {
                    TimetablePreviewList(state.slots, allFaculty, viewModel)
                }
                is AiGeneratorState.Success -> {
                    SuccessState(state.message, onBackClick)
                }
                is AiGeneratorState.Error -> {
                    ErrorState(state.message, onRetry = { viewModel.reset() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstraintsInputForm(
    availableSubjects: List<Subject>,
    constraints: List<SubjectConstraint>,
    viewModel: AiTimetableViewModel
) {
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var lunchTime by remember { mutableStateOf("13:00") }
    var lunchDuration by remember { mutableStateOf("60") }

    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var classesPerWeek by remember { mutableStateOf("4") }
    var subjectExpanded by remember { mutableStateOf(false) }
    var customSuggestions by remember { mutableStateOf("") }
    var availableRooms by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Working Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AcadMateCard(variant = CardVariant.Flat) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AcadMateTextField(value = startTime, onValueChange = { startTime = it }, label = "Start", modifier = Modifier.weight(1f))
                        AcadMateTextField(value = endTime, onValueChange = { endTime = it }, label = "End", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AcadMateTextField(value = lunchTime, onValueChange = { lunchTime = it }, label = "Lunch Start", modifier = Modifier.weight(1f))
                        AcadMateTextField(value = lunchDuration, onValueChange = { lunchDuration = it }, label = "Mins", modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text("Room Inventory (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AcadMateCard(variant = CardVariant.Elevated) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AcadMateTextField(
                        value = availableRooms,
                        onValueChange = { availableRooms = it },
                        label = "Available Rooms",
                        placeholder = "e.g. B-101, B-102, Lab-5, Audi-1",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Leave empty for automatic generation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Text("AI Custom Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AcadMateCard(variant = CardVariant.Glass) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AcadMateTextField(
                        value = customSuggestions,
                        onValueChange = { customSuggestions = it },
                        label = "E.g. No Android class on Monday",
                        placeholder = "Tell the AI about your specific preferences...",
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        singleLine = false
                    )
                }
            }
        }

        item {
            Text("Subject Requirements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AcadMateCard(variant = CardVariant.Elevated) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = subjectExpanded,
                        onExpandedChange = { subjectExpanded = !subjectExpanded }
                    ) {
                        AcadMateTextField(
                            value = selectedSubject?.name ?: "Select Subject",
                            onValueChange = {},
                            readOnly = true,
                            label = "Add Subject Rule",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                            availableSubjects.forEach { sub ->
                                DropdownMenuItem(text = { Text(sub.name) }, onClick = {
                                    selectedSubject = sub
                                    subjectExpanded = false
                                })
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AcadMateTextField(
                            value = classesPerWeek, 
                            onValueChange = { classesPerWeek = it }, 
                            label = "Classes/Week", 
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        AcadMateButton(
                            text = "Add Rule",
                            onClick = { 
                                selectedSubject?.let { viewModel.addConstraint(it, classesPerWeek.toIntOrNull() ?: 1) }
                            },
                            enabled = selectedSubject != null
                        )
                    }
                }
            }
        }

        items(constraints) { constraint ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = Box(Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))).let { null }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(constraint.subject.name, fontWeight = FontWeight.Bold)
                        Text("${constraint.classesPerWeek} sessions required", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { viewModel.removeConstraint(constraint.subject.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            AcadMateButton(
                text = "Generate Academic Schedule",
                onClick = { 
                    viewModel.generateSchedule(
                        startTime, 
                        endTime, 
                        lunchTime, 
                        lunchDuration.toIntOrNull() ?: 60, 
                        customSuggestions,
                        availableRooms
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = constraints.isNotEmpty()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetablePreviewList(slots: List<TimetableEntity>, facultyList: List<String>, viewModel: AiTimetableViewModel) {
    var previewSlots by remember { mutableStateOf(slots) }
    var editingSlot by remember { mutableStateOf<TimetableEntity?>(null) }
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

    if (editingSlot != null) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("Assign Faculty") },
            text = {
                Column {
                    Text(editingSlot!!.subject, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        AcadMateTextField(
                            value = editingSlot!!.faculty.ifBlank { "Select Faculty" },
                            onValueChange = {},
                            readOnly = true,
                            label = "Assigned Faculty",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Unassigned") }, onClick = {
                                previewSlots = previewSlots.map { if (it.id == editingSlot!!.id) it.copy(faculty = "") else it }
                                editingSlot = editingSlot!!.copy(faculty = "")
                                expanded = false
                            })
                            facultyList.forEach { faculty ->
                                DropdownMenuItem(text = { Text(faculty) }, onClick = {
                                    previewSlots = previewSlots.map { if (it.id == editingSlot!!.id) it.copy(faculty = faculty) else it }
                                    editingSlot = editingSlot!!.copy(faculty = faculty)
                                    expanded = false
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { editingSlot = null }) { Text("Done") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("AI Schedule Preview", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(previewSlots) { slot ->
                AcadMateCard(
                    variant = CardVariant.Flat,
                    onClick = { editingSlot = slot }
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(days.getOrNull(slot.dayOfWeek - 1) ?: "Day", fontWeight = FontWeight.Bold)
                            Text(slot.startTime, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(slot.subject, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (slot.faculty.isBlank()) "Faculty Unassigned" else "Faculty: ${slot.faculty}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (slot.faculty.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text("${slot.startTime} - ${slot.endTime} • ${slot.room}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { 
                            previewSlots = previewSlots.filter { it.id != slot.id }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Slot", tint = Color.Gray)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f)) {
                Text("Start Over")
            }
            AcadMateButton(
                text = "Apply Real Schedule",
                onClick = { viewModel.applySchedule(previewSlots) },
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

@Composable
fun SuccessState(message: String, onFinish: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Success!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(message, textAlign = TextAlign.Center, color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            AcadMateButton(text = "Go to Dashboard", onClick = onFinish)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("AI Generation Error", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(message, textAlign = TextAlign.Center, color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            AcadMateButton(text = "Retry", onClick = onRetry)
        }
    }
}
