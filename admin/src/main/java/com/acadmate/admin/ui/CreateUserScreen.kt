package com.acadmate.admin.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.db.UserEntity
import com.acadmate.core.model.UserRole
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val users by viewModel.usersList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val reader = inputStream?.bufferedReader()
                    val header = reader?.readLine() // Skip header
                    
                    val users = mutableListOf<Map<String, String>>()
                    reader?.forEachLine { line ->
                        val parts = line.split(",")
                        if (parts.size >= 5) {
                            users.add(mapOf(
                                "regNo" to parts[0].trim(),
                                "name" to parts[1].trim(),
                                "email" to parts[2].trim(),
                                "role" to parts[3].trim().uppercase(),
                                "phoneNumber" to parts[4].trim()
                            ))
                        }

                    }
                    inputStream?.close()
                    
                    if (users.isNotEmpty()) {
                        viewModel.bulkCreateUsers(users)
                        snackbarHostState.showSnackbar("Importing ${users.size} users...")
                    } else {
                        snackbarHostState.showSnackbar("No valid user data found in CSV")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("CSV Error: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AdminUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AdminUiState.Error).message)
        }
    }

    val filteredUsers = remember(users, searchQuery) {
        users.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            (it.regNo?.contains(searchQuery) == true) 
        }
    }

    if (showAddDialog || editingUser != null) {
        val departments by viewModel.departments.collectAsState()
        val sections by viewModel.sections.collectAsState()
        
        UserFormDialog(
            user = editingUser,
            departments = departments,
            sections = sections,
            onDismiss = { 
                showAddDialog = false
                editingUser = null
            },
            onSave = { regNo, name, email, role, phone, dept, sec ->
                if (editingUser != null) {
                    viewModel.updateUser(editingUser!!.id, name, email, role, phone, dept, sec)
                } else {
                    viewModel.createInstitutionalUser(regNo, name, email, role, phone, dept, sec)
                }
                showAddDialog = false
                editingUser = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { csvPickerLauncher.launch("text/*") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Bulk Upload CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name or ID") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found matching '$searchQuery'", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserListItem(
                            user = user,
                            onEdit = { editingUser = user },
                            onDelete = { 
                                viewModel.deleteUser(user.id)
                                scope.launch { snackbarHostState.showSnackbar("User removed") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormDialog(
    user: UserEntity? = null,
    departments: List<String> = emptyList(),
    sections: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, String, String, UserRole, String, String, String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user?.role ?: UserRole.STUDENT) }
    var regNo by remember { mutableStateOf(user?.regNo ?: "") }
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phoneNumber by remember { mutableStateOf(user?.phoneNumber ?: "") }
    var department by remember { mutableStateOf(user?.department ?: "") }
    var section by remember { mutableStateOf(user?.section ?: "") }
    
    var roleExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    // Dynamic ID Length Constraint
    val expectedIdLength = when (selectedRole) {
        UserRole.ADMIN -> 4
        UserRole.FACULTY -> 6
        UserRole.STUDENT -> 8
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "Add New User" else "Edit User Info") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
            ) {
                // 1. SELECT ROLE
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    AcadMateTextField(
                        value = selectedRole.name,
                        onValueChange = {},
                        readOnly = true,
                        label = "User Role",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = user == null
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        UserRole.values().forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    selectedRole = role
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. ID
                AcadMateTextField(
                    value = regNo,
                    onValueChange = { input ->
                        if (input.all { char -> char.isDigit() } && input.length <= expectedIdLength) {
                            regNo = input
                        }
                    },
                    label = "Enrollment / UID",
                    placeholder = "Enter $expectedIdLength digits",
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = user != null
                )

                AcadMateTextField(value = name, onValueChange = { name = it }, label = "Full Name", modifier = Modifier.fillMaxWidth())
                AcadMateTextField(value = email, onValueChange = { email = it }, label = "Email", modifier = Modifier.fillMaxWidth())

                // 3. DEPARTMENT DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = deptExpanded,
                    onExpandedChange = { deptExpanded = !deptExpanded }
                ) {
                    AcadMateTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = "Department",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (departments.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = deptExpanded,
                            onDismissRequest = { deptExpanded = false }
                        ) {
                            departments.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept) },
                                    onClick = {
                                        department = dept
                                        deptExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. SECTION DROPDOWN
                ExposedDropdownMenuBox(
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = !sectionExpanded }
                ) {
                    AcadMateTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = "Section",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (sections.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = sectionExpanded,
                            onDismissRequest = { sectionExpanded = false }
                        ) {
                            sections.forEach { sec ->
                                DropdownMenuItem(
                                    text = { Text(sec) },
                                    onClick = {
                                        section = sec
                                        sectionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                AcadMateTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 13) phoneNumber = it },
                    label = "Phone Number",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            AcadMateButton(
                text = if (user == null) "Create" else "Save Changes",
                onClick = { onSave(regNo, name, email, selectedRole, phoneNumber, department, section) },
                enabled = regNo.length == expectedIdLength && name.isNotBlank() && email.isNotBlank()
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun UserListItem(user: UserEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    AcadMateCard(variant = CardVariant.Flat, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, fontWeight = FontWeight.Bold)
                Text(text = "${user.role.name} • ${user.regNo ?: "No ID"}", style = MaterialTheme.typography.bodySmall)
                Text(text = user.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
