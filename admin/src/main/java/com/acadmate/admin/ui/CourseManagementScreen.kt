package com.acadmate.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

import com.acadmate.core.model.Subject

data class Course(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val department: String = "",
    val credits: Int = 4,
    val assignedFaculty: String? = null
)

class AdminCourseViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _courses = mutableStateListOf<Course>()
    val courses: List<Course> get() = _courses
    
    private val _subjects = mutableStateListOf<Subject>()
    val subjects: List<Subject> get() = _subjects
    
    private val _facultyList = mutableStateListOf<String>()
    val facultyList: List<String> get() = _facultyList
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: androidx.compose.runtime.State<Boolean> = _isLoading

    init {
        loadCourses()
        loadFaculty()
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("subjects").get().await()
                val subjectList = snapshot.toObjects(Subject::class.java)
                _subjects.clear()
                _subjects.addAll(subjectList)
            } catch (e: Exception) {}
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

    fun addSubject(name: String, code: String, dept: String) {
        viewModelScope.launch {
            try {
                val id = firestore.collection("subjects").document().id
                val newSubject = Subject(id, name, code, dept)
                firestore.collection("subjects").document(id).set(newSubject).await()
                _subjects.add(newSubject)
                logAdminAction("Master Subject Added", com.acadmate.core.model.ActionType.COURSE_ADDED, "Subject $name ($code) added to repository.")
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
                val names = snapshot.documents.map { it.getString("name") ?: "Unknown" }
                _facultyList.clear()
                _facultyList.addAll(names)
            } catch (e: Exception) {}
        }
    }

    fun loadCourses() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("courses").get().await()
                val courseList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Course::class.java)?.copy(id = doc.id)
                }
                _courses.clear()
                _courses.addAll(courseList)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addCourse(name: String, code: String, dept: String, credits: Int, faculty: String?) {
        viewModelScope.launch {
            try {
                val id = firestore.collection("courses").document().id
                val newCourse = Course(id, name, code, dept, credits, faculty)
                firestore.collection("courses").document(id).set(newCourse).await()
                _courses.add(newCourse)
                logAdminAction("Course Instance Launched", com.acadmate.core.model.ActionType.COURSE_ADDED, "$name ($code) assigned to ${faculty ?: "TBA"}")
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            try {
                firestore.collection("courses").document(course.id).set(course).await()
                val index = _courses.indexOfFirst { it.id == course.id }
                if (index != -1) {
                    _courses[index] = course
                }
                logAdminAction("Course Updated", com.acadmate.core.model.ActionType.COURSE_ADDED, "Course ${course.name} configurations modified.")
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("courses").document(courseId).delete().await()
                _courses.removeAll { it.id == courseId }
                logAdminAction("Course Deleted", com.acadmate.core.model.ActionType.SYSTEM_ALERT, "Course ID $courseId removed from active catalog.")
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManagementScreen(
    viewModel: AdminCourseViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    var showCourseDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var selectedFilter by remember { mutableStateOf<Subject?>(null) }

    if (showCourseDialog) {
        CourseDialog(
            course = editingCourse,
            subjects = viewModel.subjects,
            existingCourses = viewModel.courses,
            facultyList = viewModel.facultyList,
            onDismiss = { 
                showCourseDialog = false
                editingCourse = null
            },
            onAddSubjectClick = { showSubjectDialog = true },
            onSave = { name, code, dept, credits, faculty ->
                if (editingCourse != null) {
                    viewModel.updateCourse(editingCourse!!.copy(
                        name = name,
                        code = code,
                        department = dept,
                        credits = credits,
                        assignedFaculty = faculty
                    ))
                } else {
                    viewModel.addCourse(name, code, dept, credits, faculty)
                }
                showCourseDialog = false
                editingCourse = null
            }
        )
    }

    if (showSubjectDialog) {
        SubjectDialog(
            onDismiss = { showSubjectDialog = false },
            onSave = { name, code, dept ->
                viewModel.addSubject(name, code, dept)
                showSubjectDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Course Management", 
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineSmall
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showSubjectDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.LibraryAdd, contentDescription = "Add Subject")
                }
                FloatingActionButton(
                    onClick = { showCourseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Course")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalSpacing.current.md),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsCard(
                    title = "Total Courses",
                    value = viewModel.courses.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                val unassigned = viewModel.courses.count { it.assignedFaculty == null }
                StatsCard(
                    title = "Unassigned",
                    value = unassigned.toString(),
                    color = if (unassigned > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Subject Filter Chips (Optional but good UX)
            if (viewModel.subjects.isNotEmpty()) {
                SubjectFilterChips(
                    subjects = viewModel.subjects,
                    selectedSubject = selectedFilter,
                    onSubjectSelected = { selectedFilter = it }
                )
            }

            // Course List
            if (viewModel.isLoading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = LocalSpacing.current.md,
                        end = LocalSpacing.current.md,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val filteredCourses = if (selectedFilter == null) {
                        viewModel.courses
                    } else {
                        viewModel.courses.filter { it.name == selectedFilter?.name }
                    }

                    items(
                        items = filteredCourses,
                        key = { it.id }
                    ) { course ->
                        CourseItemCard(
                            course = course, 
                            onEditClick = { 
                                editingCourse = course
                                showCourseDialog = true
                            },
                            onDeleteClick = { viewModel.deleteCourse(course.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDialog(
    course: Course? = null,
    subjects: List<Subject>,
    existingCourses: List<Course>,
    facultyList: List<String>,
    onDismiss: () -> Unit,
    onAddSubjectClick: () -> Unit,
    onSave: (String, String, String, Int, String?) -> Unit
) {
    var selectedSubject by remember { 
        mutableStateOf(subjects.find { it.name == course?.name && it.code == course?.code }) 
    }
    
    // Filter out subjects that are already assigned to a course, 
    // but keep the current subject if we're editing.
    val availableSubjects = remember(subjects, existingCourses, course) {
        val assignedSubjectCodes = existingCourses.map { it.code }.toSet()
        subjects.filter { subject ->
            subject.code !in assignedSubjectCodes || subject.code == course?.code
        }
    }
    
    var credits by remember { mutableStateOf(course?.credits?.toString() ?: "4") }
    var selectedFaculty by remember { mutableStateOf<String?>(course?.assignedFaculty) }
    
    var subjectExpanded by remember { mutableStateOf(false) }
    var facultyExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(
            variant = CardVariant.Elevated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentPadding = 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (course == null) "Launch New Course" else "Edit Course Instance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Subject Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = subjectExpanded,
                        onExpandedChange = { subjectExpanded = !subjectExpanded }
                    ) {
                        AcadMateTextField(
                            value = selectedSubject?.name ?: "Select Subject",
                            onValueChange = {},
                            readOnly = true,
                            label = "Course Subject",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subjectExpanded,
                            onDismissRequest = { subjectExpanded = false }
                        ) {
                            availableSubjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text("${subject.name} (${subject.code})") },
                                    onClick = {
                                        selectedSubject = subject
                                        subjectExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Add New Subject", color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    subjectExpanded = false
                                    onAddSubjectClick()
                                }
                            )
                        }
                    }
                }

                AcadMateTextField(
                    value = credits,
                    onValueChange = { if (it.all { c -> c.isDigit() }) credits = it },
                    label = "Credits",
                    placeholder = "4",
                    modifier = Modifier.fillMaxWidth()
                )

                // Faculty Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = facultyExpanded,
                        onExpandedChange = { facultyExpanded = !facultyExpanded }
                    ) {
                        AcadMateTextField(
                            value = selectedFaculty ?: "Unassigned",
                            onValueChange = {},
                            readOnly = true,
                            label = "Assign Faculty",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = facultyExpanded,
                            onDismissRequest = { facultyExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unassigned") },
                                onClick = {
                                    selectedFaculty = null
                                    facultyExpanded = false
                                }
                            )
                            facultyList.forEach { faculty ->
                                DropdownMenuItem(
                                    text = { Text(faculty) },
                                    onClick = {
                                        selectedFaculty = faculty
                                        facultyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    AcadMateButton(
                        text = if (course == null) "Create Course" else "Update",
                        onClick = { 
                            selectedSubject?.let {
                                onSave(it.name, it.code, it.department, credits.toIntOrNull() ?: 4, selectedFaculty) 
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedSubject != null
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(
            variant = CardVariant.Elevated,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentPadding = 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add Master Subject", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                AcadMateTextField(value = name, onValueChange = { name = it }, label = "Subject Name")
                AcadMateTextField(value = code, onValueChange = { code = it }, label = "Subject Code")
                AcadMateTextField(value = dept, onValueChange = { dept = it }, label = "Department")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    AcadMateButton(
                        text = "Add", 
                        onClick = { onSave(name, code, dept) },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && code.isNotBlank()
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(title: String, value: String, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    AcadMateCard(
        modifier = modifier,
        variant = CardVariant.Flat,
        cornerRadius = 16.dp,
        contentPadding = 16.dp
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = color
            )
        }
    }
}

@Composable
fun SubjectFilterChips(
    subjects: List<Subject>,
    selectedSubject: Subject?,
    onSubjectSelected: (Subject?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedSubject == null,
                onClick = { onSubjectSelected(null) },
                label = { Text("All Subjects") }
            )
        }
        items(
            items = subjects,
            key = { it.id }
        ) { subject ->
            FilterChip(
                selected = selectedSubject?.id == subject.id,
                onClick = { onSubjectSelected(subject) },
                label = { Text(subject.name) }
            )
        }
    }
}

@Composable
fun CourseItemCard(
    course: Course, 
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AcadMateCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.Elevated,
        cornerRadius = 16.dp,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Department Icon Box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = course.code,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${course.credits} Credits",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (course.assignedFaculty != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountCircle, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp), 
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = course.assignedFaculty,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp), 
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Faculty Unassigned",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Course",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete Course",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
