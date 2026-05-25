package com.acadmate.auth.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acadmate.designsystem.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onNavigateToOtp: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var newPhone by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.UpdateOtpSent) {
            onNavigateToOtp((uiState as AuthUiState.UpdateOtpSent).target)
        }
        if (uiState is AuthUiState.UpdateSuccess) {
            // Success handled by snackbar usually, but can pop back
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Security Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Phone") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Email") })
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (selectedTab) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Update Mobile Number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("A verification OTP will be sent to your new mobile number.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        
                        AcadMateTextField(
                            value = newPhone,
                            onValueChange = { if (it.length <= 10) newPhone = it },
                            label = "New Phone Number",
                            placeholder = "10-digit number",
                            leadingIcon = { Icon(Icons.Default.Phone, null) }
                        )

                        AcadMateButton(
                            text = "Send OTP",
                            onClick = { 
                                (context as? Activity)?.let { viewModel.startPhoneUpdate(newPhone, it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = newPhone.length == 10 && uiState !is AuthUiState.Loading,
                            loading = uiState is AuthUiState.Loading
                        )
                    }
                }
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Update Email Address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("A verification link will be sent to your new email. Please click the link to confirm the change.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                        AcadMateTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = "New Email Address",
                            placeholder = "name@institution.com",
                            leadingIcon = { Icon(Icons.Default.Email, null) }
                        )

                        AcadMateButton(
                            text = "Update Email",
                            onClick = { viewModel.startEmailUpdate(newEmail) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = newEmail.contains("@") && uiState !is AuthUiState.Loading,
                            loading = uiState is AuthUiState.Loading
                        )
                    }
                }
            }
            
            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
