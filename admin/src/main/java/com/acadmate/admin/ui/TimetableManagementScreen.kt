package com.acadmate.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acadmate.core.db.TimetableEntity
import com.acadmate.core.model.Subject
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminTimetableViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _timetable = mutableStateListOf<TimetableEntity>()
    val timetable: List<TimetableEntity> get() = _timetable

    private val _subjects = mutableStateListOf<Subject>()
    val subjects: List<Subject> get() = _subjects

    private val _facultyList = mutableStateListOf<String>()
    val facultyList: List<String> get() = _facultyList

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _selectedDay = mutableStateOf(1)
    val selectedDay: State<Int> = _selectedDay

    init {
        loadSubjects()
        loadFaculty()
        loadTimetable(1)
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
        loadTimetable(day)
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("subjects").get().await()
                _subjects.clear()
                _subjects.addAll(snapshot.toObjects(Subject::class.java))
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
                _facultyList.clear()
                _facultyList.addAll(snapshot.documents.map { it.getString("name") ?: "Unknown" })
            } catch (e: Exception) {}
        }
    }

    fun loadTimetable(day: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("global_timetable")
                    .whereEqualTo("dayOfWeek", day)
                    .get()
                    .await()
                
                val items = snapshot.documents.map { doc ->
                    TimetableEntity(
                        id = doc.id,
                        dayOfWeek = doc.getLong("dayOfWeek")?.toInt() ?: day,
                        subject = doc.getString("subject") ?: "",
                        faculty = doc.getString("faculty") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        room = doc.getString("room") ?: "",
                        color = doc.getLong("color")?.toInt() ?: 0xFF4A90E2.toInt()
                    )
                }
                _timetable.clear()
                _timetable.addAll(items.sortedBy { it.startTime })
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTimetableItem(subject: String, faculty: String, start: String, end: String, room: String) {
        viewModelScope.launch {
            try {
                val id = firestore.collection("global_timetable").document().id
                val entity = TimetableEntity(
                    id = id,
                    dayOfWeek = _selectedDay.value,
                    subject = subject,
                    faculty = faculty,
                    startTime = start,
                    endTime = end,
                    room = room,
                    color = 0xFF4A90E2.toInt() // Default color
                )
                
                val map = hashMapOf(
                    "id" to id,
                    "dayOfWeek" to _selectedDay.value,
                    "subject" to subject,
                    "faculty" to faculty,
                    "startTime" to start,
                    "endTime" to end,
                    "room" to room,
                    "color" to entity.color
                )
                
                firestore.collection("global_timetable").document(id).set(map).await()
                _timetable.add(entity)
                _timetable.sortBy { it.startTime }
            } catch (e: Exception) {}
        }
    }

    fun deleteTimetableItem(id: String) {
        viewModelScope.launch {
            try {
                firestore.collection("global_timetable").document(id).delete().await()
                _timetable.removeAll { it.id == id }
            } catch (e: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableManagementScreen(
    viewModel: AdminTimetableViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    if (showAddDialog) {
        TimetableDialog(
            subjects = viewModel.subjects,
            facultyList = viewModel.facultyList,
            onDismiss = { showAddDialog = false },
            onSave = { subject, faculty, start, end, room ->
                viewModel.addTimetableItem(subject, faculty, start, end, room)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Management", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Slot")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Day Selector
            ScrollableTabRow(
                selectedTabIndex = viewModel.selectedDay.value - 1,
                edgePadding = 16.dp,
                containerColor = Color.Transparent
            ) {
                days.forEachIndexed { index, name ->
                    Tab(
                        selected = viewModel.selectedDay.value == index + 1,
                        onClick = { viewModel.setSelectedDay(index + 1) },
                        text = { Text(name) }
                    )
                }
            }

            if (viewModel.isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (viewModel.timetable.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No classes scheduled for this day", color = Color.Gray)
                            }
                        }
                    }
                    items(viewModel.timetable) { item ->
                        TimetableItemCard(item, onDelete = { viewModel.deleteTimetableItem(item.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableDialog(
    subjects: List<Subject>,
    facultyList: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedFaculty by remember { mutableStateOf<String?>(null) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var room by remember { mutableStateOf("B-401") }

    var subjectExpanded by remember { mutableStateOf(false) }
    var facultyExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(variant = CardVariant.Elevated, contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Schedule Class Slot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    AcadMateTextField(
                        value = selectedSubject?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = "Subject",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(text = { Text(sub.name) }, onClick = {
                                selectedSubject = sub
                                subjectExpanded = false
                            })
                        }
                    }
                }

                // Faculty Dropdown
                ExposedDropdownMenuBox(
                    expanded = facultyExpanded,
                    onExpandedChange = { facultyExpanded = !facultyExpanded }
                ) {
                    AcadMateTextField(
                        value = selectedFaculty ?: "Unassigned (TBA)",
                        onValueChange = {},
                        readOnly = true,
                        label = "Faculty",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = facultyExpanded, onDismissRequest = { facultyExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Unassigned (TBA)") },
                            onClick = {
                                selectedFaculty = null
                                facultyExpanded = false
                            }
                        )
                        facultyList.forEach { fac ->
                            DropdownMenuItem(text = { Text(fac) }, onClick = {
                                selectedFaculty = fac
                                facultyExpanded = false
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AcadMateTextField(value = startTime, onValueChange = { startTime = it }, label = "Start Time", modifier = Modifier.weight(1f))
                    AcadMateTextField(value = endTime, onValueChange = { endTime = it }, label = "End Time", modifier = Modifier.weight(1f))
                }

                AcadMateTextField(value = room, onValueChange = { room = it }, label = "Room/Lab", modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    AcadMateButton(
                        text = "Schedule",
                        onClick = { onSave(selectedSubject?.name ?: "", selectedFaculty ?: "", startTime, endTime, room) },
                        modifier = Modifier.weight(1f),
                        enabled = selectedSubject != null
                    )
                }
            }
        }
    }
}

@Composable
fun TimetableItemCard(item: TimetableEntity, onDelete: () -> Unit) {
    AcadMateCard(variant = CardVariant.Flat, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("${item.startTime} - ${item.endTime} • ${item.room}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Text("Faculty: ${item.faculty}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
