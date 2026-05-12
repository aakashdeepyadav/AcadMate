package com.acadmate.admin.ui

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
    var searchQuery by remember { mutableStateOf("") }
    val users by viewModel.usersList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { regNo, name, email, role, phone ->
                viewModel.createInstitutionalUser(regNo, name, email, role, phone)
                showAddDialog = false
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
fun AddUserDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, UserRole, String) -> Unit
) {
    var regNo by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                AcadMateTextField(
                    value = regNo,
                    onValueChange = { if (it.all { char -> char.isDigit() }) regNo = it },
                    label = "Registration Number",
                    modifier = Modifier.fillMaxWidth()
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
                    label = "Institutional Email",
                    placeholder = "user@university.edu",
                    modifier = Modifier.fillMaxWidth()
                )
                AcadMateTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 10) phoneNumber = it },
                    label = "Phone Number",
                    placeholder = "10-digit mobile",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        AcadMateTextField(
                            value = selectedRole.name,
                            onValueChange = {},
                            readOnly = true,
                            label = "Role",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AcadMateButton(
                text = "Create",
                onClick = { onAdd(regNo, name, email, selectedRole, phoneNumber) },
                enabled = regNo.isNotBlank() && name.isNotBlank() && email.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UserListItem(user: UserEntity, onDelete: () -> Unit) {
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
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
