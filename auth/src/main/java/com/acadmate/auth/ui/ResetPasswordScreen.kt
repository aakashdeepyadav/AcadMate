package com.acadmate.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acadmate.core.util.ValidationUtils
import com.acadmate.designsystem.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    onSuccess: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val passwordsMatch = newPassword == confirmPassword && newPassword.isNotEmpty()
    val isPasswordStrong = ValidationUtils.isStrongPassword(newPassword)

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.PasswordResetSent) {
            onSuccess((uiState as AuthUiState.PasswordResetSent).message)
            viewModel.resetState()
        } else if (uiState is AuthUiState.Verified) {
            onSuccess("Password updated successfully!")
        }
    }

    MeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Secure Your Account", fontWeight = FontWeight.Black) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Update Your Password",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "For security reasons, you must change your initial institutional password before continuing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                AcadMateCard(variant = CardVariant.Elevated) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AcadMateTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "New Password",
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        AcadMateTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirm Password",
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                            errorMessage = if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) "Passwords do not match" else ""
                        )

                        if (!isPasswordStrong && newPassword.isNotEmpty()) {
                            Text(
                                text = ValidationUtils.getPasswordStrengthErrorMessage(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (uiState is AuthUiState.Error) {
                            Text(
                                text = (uiState as AuthUiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        AcadMateButton(
                            text = "Update & Continue",
                            onClick = { viewModel.updateInstitutionalPassword(newPassword) },
                            modifier = Modifier.fillMaxWidth(),
                            loading = uiState is AuthUiState.Loading,
                            enabled = passwordsMatch && isPasswordStrong
                        )
                    }
                }
            }
        }
    }
}
