package com.acadmate.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.CommunityGroup
import com.acadmate.core.model.Subject
import com.acadmate.designsystem.components.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminCommunityViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _groups = mutableStateOf<List<CommunityGroup>>(emptyList())
    val groups: State<List<CommunityGroup>> = _groups

    private val _sections = mutableStateOf<List<String>>(emptyList())
    val sections: State<List<String>> = _sections

    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    private val _facultyList = mutableStateOf<List<Pair<String, String>>>(emptyList())
    val facultyList: State<List<Pair<String, String>>> = _facultyList

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        loadGroups()
        loadInstitutionData()
        loadSubjects()
        loadFaculty()
    }

    fun loadGroups() {
        _isLoading.value = true
        firestore.collection("community_groups").get().addOnSuccessListener { snapshot ->
            _groups.value = snapshot.toObjects(CommunityGroup::class.java)
            _isLoading.value = false
        }
    }

    private fun loadInstitutionData() {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("institution").document("config").get().await()
                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    _sections.value = doc.get("sections") as? List<String> ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("subjects").get().await()
                _subjects.value = snapshot.toObjects(Subject::class.java)
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
                _facultyList.value = snapshot.documents.map { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: "Unknown"
                    id to name
                }
            } catch (e: Exception) {}
        }
    }

    fun createGroup(section: String, subject: String, facultyId: String, facultyName: String) {
        val id = "${section}_${subject.replace(" ", "_")}"
        val newGroup = CommunityGroup(
            id = id,
            section = section,
            subject = subject,
            facultyId = facultyId,
            facultyName = facultyName
        )
        firestore.collection("community_groups").document(id).set(newGroup)
            .addOnSuccessListener {
                loadGroups()
            }
    }

    fun deleteGroup(groupId: String) {
        firestore.collection("community_groups").document(groupId).delete()
            .addOnSuccessListener {
                loadGroups()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityManagementScreen(
    viewModel: AdminCommunityViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateGroupDialog(
            sections = viewModel.sections.value,
            subjects = viewModel.subjects.value,
            facultyList = viewModel.facultyList.value,
            onDismiss = { showCreateDialog = false },
            onCreate = { section, subject, facultyId, facultyName ->
                viewModel.createGroup(section, subject, facultyId, facultyName)
                showCreateDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Class Groups", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                items(viewModel.groups.value) { group ->
                    AcadMateCard(variant = CardVariant.Elevated) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Forum, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${group.section} • ${group.subject}", fontWeight = FontWeight.Bold)
                                Text("Faculty: ${group.facultyName}", style = MaterialTheme.typography.bodySmall)
                                Text("CRs: ${group.crIds.size}/2 Assigned", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { viewModel.deleteGroup(group.id) }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    sections: List<String>,
    subjects: List<Subject>,
    facultyList: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var selectedSection by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedFaculty by remember { mutableStateOf<Pair<String, String>?>(null) }

    var sectionExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var facultyExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Create Moderated Group", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                
                // Section Dropdown
                ExposedDropdownMenuBox(
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = !sectionExpanded }
                ) {
                    AcadMateTextField(
                        value = selectedSection,
                        onValueChange = {},
                        readOnly = true,
                        label = "Select Section",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false }
                    ) {
                        sections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(section) },
                                onClick = {
                                    selectedSection = section
                                    sectionExpanded = false
                                }
                            )
                        }
                    }
                }

                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    AcadMateTextField(
                        value = selectedSubject?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = "Select Subject",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text("${subject.name} (${subject.code})") },
                                onClick = {
                                    selectedSubject = subject
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                // Faculty Dropdown
                ExposedDropdownMenuBox(
                    expanded = facultyExpanded,
                    onExpandedChange = { facultyExpanded = !facultyExpanded }
                ) {
                    AcadMateTextField(
                        value = selectedFaculty?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = "Select Faculty",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = facultyExpanded,
                        onDismissRequest = { facultyExpanded = false }
                    ) {
                        facultyList.forEach { faculty ->
                            DropdownMenuItem(
                                text = { Text(faculty.second) },
                                onClick = {
                                    selectedFaculty = faculty
                                    facultyExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    AcadMateButton(
                        text = "Create", 
                        onClick = { 
                            if (selectedSection.isNotBlank() && selectedSubject != null && selectedFaculty != null) {
                                onCreate(selectedSection, selectedSubject!!.name, selectedFaculty!!.first, selectedFaculty!!.second)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedSection.isNotBlank() && selectedSubject != null && selectedFaculty != null
                    )
                }
            }
        }
    }
}
