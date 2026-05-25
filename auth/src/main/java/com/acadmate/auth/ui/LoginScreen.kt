package com.acadmate.auth.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.UserRole
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.theme.AcadMateTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.acadmate.designsystem.theme.LocalSpacing
import com.acadmate.designsystem.components.AcadMateLogo
import com.acadmate.designsystem.components.MeshBackground

@Composable
fun LoginScreen(
    onOtpSent: (String) -> Unit,
    onLoginSuccess: (AuthUiState) -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    var regNo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var lastPhoneInputState by remember { mutableStateOf<AuthUiState.RequirePhoneInput?>(null) }
    
    // Smooth entrance state
    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isInitialized = true
    }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle verification success state internally to show animation
    var showSuccess by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Handle state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.RequirePhoneInput -> {
                lastPhoneInputState = uiState as AuthUiState.RequirePhoneInput
            }
            is AuthUiState.OtpSent -> {
                onOtpSent((uiState as AuthUiState.OtpSent).phoneNumber)
            }
            is AuthUiState.RequirePhoneVerification -> {
                onOtpSent((uiState as AuthUiState.RequirePhoneVerification).phoneNumber)
            }
            is AuthUiState.Verified -> {
                showSuccess = true
            }
            is AuthUiState.ForcePasswordChange -> {
                showSuccess = true
            }
            is AuthUiState.Idle -> {
                lastPhoneInputState = null
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
                if (lastPhoneInputState == null) {
                    viewModel.resetState()
                }
            }
            is AuthUiState.PasswordResetSent -> {
                snackbarHostState.showSnackbar((uiState as AuthUiState.PasswordResetSent).message)
                showForgotPasswordDialog = false
                viewModel.resetState()
            }
            else -> {}
        }
    }

    if (showSuccess) {
        LoginSuccessScreen {
            onLoginSuccess(uiState)
        }
        return
    }

    val currentPhoneInputState = if (uiState is AuthUiState.RequirePhoneInput) {
        uiState as AuthUiState.RequirePhoneInput
    } else if ((uiState is AuthUiState.Loading || uiState is AuthUiState.Error) && lastPhoneInputState != null) {
        lastPhoneInputState
    } else {
        null
    }

    // Handle System Back button
    BackHandler(enabled = currentPhoneInputState != null) {
        viewModel.resetState()
    }

    MeshBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            if (currentPhoneInputState != null) {
                                viewModel.resetState()
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(LocalSpacing.current.md))

                    // Branding Section
                    AnimatedVisibility(
                        visible = isInitialized,
                        enter = fadeIn(tween(800)) + slideInVertically(initialOffsetY = { -50 })
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AcadMateLogo(iconSize = 100.dp, showText = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

                    // Form Container with AnimatedContent
                    AnimatedVisibility(
                        visible = isInitialized,
                        enter = fadeIn(tween(800, 200)) + slideInVertically(initialOffsetY = { 50 })
                    ) {
                        // Capture state to prevent crash during animation
                        val capturedPhoneState = remember(currentPhoneInputState) { currentPhoneInputState }
                        
                        AnimatedContent(
                            targetState = currentPhoneInputState != null,
                            transitionSpec = {
                                if (targetState) {
                                    // Transitioning to Phone Verification
                                    (fadeIn(tween(400)) + slideInHorizontally(tween(400), initialOffsetX = { it })) togetherWith
                                    (fadeOut(tween(400)) + slideOutHorizontally(tween(400), targetOffsetX = { -it }))
                                } else {
                                    // Transitioning back to Login
                                    (fadeIn(tween(400)) + slideInHorizontally(tween(400), initialOffsetX = { -it })) togetherWith
                                    (fadeOut(tween(400)) + slideOutHorizontally(tween(400), targetOffsetX = { it }))
                                }
                            },
                            label = "LoginFormTransition"
                        ) { isPhoneVerification ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = LocalSpacing.current.lg),
                                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
                            ) {
                                if (isPhoneVerification && capturedPhoneState != null) {
                                    PhoneVerificationForm(
                                        state = capturedPhoneState,
                                        phoneNumber = phoneNumber,
                                        onPhoneNumberChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) phoneNumber = it },
                                        uiState = uiState,
                                        onSendOtp = { phoneNumber, userId, activity ->
                                            viewModel.verifyPhoneAndSendOtp(phoneNumber, userId, activity)
                                        },
                                        onBack = { viewModel.resetState() }
                                    )
                                } else {
                                    CredentialLoginForm(
                                        selectedRole = selectedRole,
                                        regNo = regNo,
                                        onRegNoChange = { input ->
                                            val maxLength = when (selectedRole) {
                                                UserRole.STUDENT -> 8
                                                UserRole.FACULTY -> 6
                                                UserRole.ADMIN -> 4
                                                else -> Int.MAX_VALUE
                                            }
                                            if (input.length <= maxLength && input.all { char -> char.isDigit() }) {
                                                regNo = input
                                            }
                                        },
                                        password = password,
                                        onPasswordChange = { password = it },
                                        uiState = uiState,
                                        onLogin = { viewModel.loginWithRegNo(regNo, password) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(LocalSpacing.current.lg))
                    
                    // Footer Section
                    AnimatedVisibility(
                        visible = isInitialized && currentPhoneInputState == null,
                        enter = fadeIn(tween(800, 400))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = LocalSpacing.current.lg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                                Text(
                                    text = "  Institutional Login  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                            }

                            Spacer(modifier = Modifier.height(LocalSpacing.current.md))

                            Text(
                                text = "Forgot Password? Reset Here",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showForgotPasswordDialog = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(LocalSpacing.current.xxl))
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        var resetRegNo by remember { mutableStateOf(regNo) }
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your Registration Number. We will send a password reset link to your registered email address.")
                    Spacer(modifier = Modifier.height(16.dp))
                    AcadMateTextField(
                        value = resetRegNo,
                        onValueChange = { resetRegNo = it },
                        label = "Registration Number",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resetPassword(resetRegNo) },
                    enabled = resetRegNo.isNotBlank() && uiState !is AuthUiState.Loading
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CredentialLoginForm(
    selectedRole: UserRole?,
    regNo: String,
    onRegNoChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    uiState: AuthUiState,
    onLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Sign in to your ${selectedRole?.name?.lowercase() ?: "academic"} account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.xs))

        AcadMateTextField(
            value = regNo,
            onValueChange = onRegNoChange,
            label = "Registration Number",
            placeholder = when (selectedRole) {
                UserRole.STUDENT -> "8 digits"
                UserRole.FACULTY -> "6 digits"
                UserRole.ADMIN -> "4 digits"
                else -> ""
            },
            leadingIcon = {
                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = uiState is AuthUiState.Error && regNo.isEmpty()
        )

        AcadMateTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "••••••••",
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        AcadMateButton(
            text = "Sign In",
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = regNo.isNotEmpty() && password.isNotEmpty() && uiState !is AuthUiState.Loading,
            loading = uiState is AuthUiState.Loading,
            icon = Icons.AutoMirrored.Filled.Login
        )
    }
}

@Composable
fun PhoneVerificationForm(
    state: AuthUiState.RequirePhoneInput,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    uiState: AuthUiState,
    onSendOtp: (String, String, android.app.Activity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
        Text(
            text = "Verify Linked Phone",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "For security, please confirm the phone number ending in ${state.maskedPhone.takeLast(4)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.xs))

        AcadMateTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = "Phone Number",
            placeholder = "00000 00000",
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = uiState is AuthUiState.Error,
            errorMessage = if (uiState is AuthUiState.Error) uiState.message else ""
        )

        AcadMateButton(
            text = "Send Verification OTP",
            onClick = { 
                val activity = context as? android.app.Activity
                if (activity != null) onSendOtp(phoneNumber, state.userId, activity)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneNumber.length == 10 && uiState !is AuthUiState.Loading,
            loading = uiState is AuthUiState.Loading,
            icon = Icons.AutoMirrored.Filled.Send
        )
        
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = uiState !is AuthUiState.Loading
        ) {
            Text("Back to Login", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AcadMateTheme {
        LoginScreen(onOtpSent = {}, onLoginSuccess = {}, onBack = {})
    }
}
