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
        UserFormDialog(
            user = editingUser,
            onDismiss = { 
                showAddDialog = false
                editingUser = null
            },
            onSave = { regNo, name, email, role, phone ->
                if (editingUser != null) {
                    viewModel.updateUser(editingUser!!.id, name, email, role, phone)
                } else {
                    viewModel.createInstitutionalUser(regNo, name, email, role, phone)
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
    onDismiss: () -> Unit,
    onSave: (String, String, String, UserRole, String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user?.role ?: UserRole.STUDENT) }
    var regNo by remember { mutableStateOf(user?.regNo ?: "") }
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phoneNumber by remember { mutableStateOf(user?.phoneNumber ?: "") }
    var expanded by remember { mutableStateOf(false) }

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
            Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                // 1. SELECT ROLE FIRST (Moved to top)
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        AcadMateTextField(
                            value = selectedRole.name,
                            onValueChange = {},
                            readOnly = true,
                            label = "User Role",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = user == null // Don't allow changing role during edit
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            UserRole.values().forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.name) },
                                    onClick = {
                                        selectedRole = role
                                        expanded = false
                                        // Auto-truncate or clear ID if role changes to a smaller requirement
                                        if (regNo.length > when(role){
                                            UserRole.ADMIN -> 4
                                            UserRole.FACULTY -> 6
                                            UserRole.STUDENT -> 8
                                        }) {
                                            regNo = regNo.take(when(role){
                                                UserRole.ADMIN -> 4
                                                UserRole.FACULTY -> 6
                                                UserRole.STUDENT -> 8
                                            })
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. ID WITH DYNAMIC CONSTRAINT
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
                    readOnly = user != null,
                    supportingText = {
                        Text(
                            text = "Required: $expectedIdLength digits for ${selectedRole.name}",
                            color = if (regNo.length == expectedIdLength) Color(0xFF10B981) 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                AcadMateTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    modifier = Modifier.fillMaxWidth()
                )
                
                AcadMateTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "user@university.edu",
                    modifier = Modifier.fillMaxWidth()
                )

                AcadMateTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 13) phoneNumber = it },
                    label = "Phone Number",
                    placeholder = "10-digit mobile",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            AcadMateButton(
                text = if (user == null) "Create" else "Save Changes",
                onClick = { onSave(regNo, name, email, selectedRole, phoneNumber) },
                enabled = regNo.length == expectedIdLength && name.isNotBlank() && email.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
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
