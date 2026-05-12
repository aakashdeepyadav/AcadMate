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
    
    private val _facultyList = mutableStateListOf<String>()
    val facultyList: List<String> get() = _facultyList
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: androidx.compose.runtime.State<Boolean> = _isLoading

    init {
        loadCourses()
        loadFaculty()
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
                val courseList = snapshot.toObjects(Course::class.java)
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
    var editingCourse by remember { mutableStateOf<Course?>(null) }

    if (showCourseDialog) {
        CourseDialog(
            course = editingCourse,
            facultyList = viewModel.facultyList,
            onDismiss = { 
                showCourseDialog = false
                editingCourse = null
            },
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
            FloatingActionButton(
                onClick = { showCourseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.LibraryAdd, contentDescription = "Add Course")
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
                    items(viewModel.courses) { course ->
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
    facultyList: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String?) -> Unit
) {
    var name by remember { mutableStateOf(course?.name ?: "") }
    var code by remember { mutableStateOf(course?.code ?: "") }
    var dept by remember { mutableStateOf(course?.department ?: "") }
    var credits by remember { mutableStateOf(course?.credits?.toString() ?: "4") }
    var selectedFaculty by remember { mutableStateOf<String?>(course?.assignedFaculty) }
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
                    text = if (course == null) "Add New Course" else "Edit Course",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                AcadMateTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Course Name",
                    placeholder = "e.g. Algorithms"
                )
                
                AcadMateTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = "Course Code",
                    placeholder = "e.g. CS201"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AcadMateTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = "Department",
                        placeholder = "e.g. CS",
                        modifier = Modifier.weight(1f)
                    )
                    AcadMateTextField(
                        value = credits,
                        onValueChange = { if (it.all { c -> c.isDigit() }) credits = it },
                        label = "Credits",
                        placeholder = "4",
                        modifier = Modifier.weight(0.5f)
                    )
                }

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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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
                        text = if (course == null) "Add Course" else "Update Course",
                        onClick = { onSave(name, code, dept, credits.toIntOrNull() ?: 4, selectedFaculty) },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && code.isNotBlank() && dept.isNotBlank()
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
fun CourseItemCard(
    course: Course, 
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AcadMateCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.Flat,
        cornerRadius = 12.dp,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Department Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${course.code} • ${course.credits} Credits",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = course.department,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (course.assignedFaculty != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = course.assignedFaculty,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Unassigned",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Edit Course",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Course",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
