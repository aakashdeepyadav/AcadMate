package com.acadmate.auth.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.designsystem.theme.LocalSpacing
import com.acadmate.designsystem.components.AcadMateLogo
import com.acadmate.designsystem.components.MeshBackground
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    phoneNumber: String,
    onVerificationSuccess: (AuthUiState) -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Countdown timer
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000)
            timeLeft--
        } else {
            canResend = true
        }
    }

    // Handle verification success
    if (uiState is AuthUiState.Verified || uiState is AuthUiState.ForcePasswordChange) {
        LoginSuccessScreen {
            onVerificationSuccess(uiState)
        }
        return
    }

    MeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackToLogin) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = LocalSpacing.current.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(LocalSpacing.current.lg))
                
                AcadMateLogo(iconSize = 100.dp, showText = false)
                
                Spacer(modifier = Modifier.height(LocalSpacing.current.md))

                Text(
                    text = "Verify Phone",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "A 6-digit code has been sent to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(LocalSpacing.current.xl))

                AcadMateCard(
                    variant = CardVariant.Elevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = LocalSpacing.current.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg)
                    ) {
                        OtpInput(
                            otp = otp,
                            onOtpChange = { newOtp ->
                                if (newOtp.length <= 6 && newOtp.all { it.isDigit() }) {
                                    otp = newOtp
                                    if (newOtp.length == 6 && uiState !is AuthUiState.Loading) {
                                        viewModel.verifyOtp(newOtp)
                                    }
                                }
                            },
                            isError = uiState is AuthUiState.Error
                        )

                        if (uiState is AuthUiState.Error) {
                            Text(
                                text = (uiState as AuthUiState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = LocalSpacing.current.md)
                            )
                        }

                        AcadMateButton(
                            text = "Verify & Continue",
                            onClick = { viewModel.verifyOtp(otp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LocalSpacing.current.md),
                            enabled = otp.length == 6 && uiState !is AuthUiState.Loading,
                            loading = uiState is AuthUiState.Loading
                        )
                    }
                }

                Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

                // Resend Section
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = canResend,
                        transitionSpec = {
                            fadeIn() + slideInVertically { it / 2 } togetherWith fadeOut() + slideOutVertically { -it / 2 }
                        },
                        label = "ResendTransition"
                    ) { resendReady ->
                        if (!resendReady) {
                            Text(
                                text = "Resend code in ${timeLeft}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Didn't receive code? ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Resend Now",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        val phoneDigits = phoneNumber.replace(Regex("[^\\d]"), "")
                                        val activity = context as? android.app.Activity
                                        if (activity != null) {
                                            val digitsOnly = if (phoneDigits.startsWith("91") && phoneDigits.length > 10) 
                                                phoneDigits.substring(2) else phoneDigits
                                            viewModel.resendOtp(digitsOnly, activity)
                                        }
                                        timeLeft = 60
                                        canResend = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "Change Phone Number",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier
                        .padding(bottom = LocalSpacing.current.xl)
                        .clickable { onBackToLogin() }
                )
            }
        }
    }
}

@Composable
fun OtpInput(
    otp: String,
    onOtpChange: (String) -> Unit,
    isError: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    Box(contentAlignment = Alignment.Center) {
        // Hidden TextField for system interactions (Autofill, Keyboard, etc.)
        BasicTextField(
            value = otp,
            onValueChange = {
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    onOtpChange(it)
                }
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = { it() } // Just the field itself
        )

        // Visual Boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { focusRequester.requestFocus() }
        ) {
            repeat(6) { index ->
                val char = otp.getOrNull(index)?.toString() ?: ""
                val isFocused = otp.length == index || (index == 5 && otp.length == 6)

                Surface(
                    modifier = Modifier
                        .size(width = 45.dp, height = 54.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                             else if (isError) BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                             else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                textAlign = TextAlign.Center,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        // Cursor animation for focused box
                        if (isFocused && otp.length < 6) {
                            val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                            val cursorAlpha by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "cursorAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .alpha(cursorAlpha)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Request focus on initial launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(showBackground = true)
@Composable
fun OtpScreenPreview() {
    AcadMateTheme {
        OtpScreen(
            phoneNumber = "+91 9876543210",
            onVerificationSuccess = {},
            onBackToLogin = {}
        )
    }
}
