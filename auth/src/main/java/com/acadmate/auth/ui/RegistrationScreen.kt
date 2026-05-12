package com.acadmate.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.core.model.UserRole
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.designsystem.theme.LocalSpacing

import com.acadmate.designsystem.components.AcadMateLogo

data class CountryCode(
    val code: String,
    val flag: String
)

val countryCodes = listOf(
    CountryCode("+91", "🇮🇳"),
    CountryCode("+1", "🇺🇸"),
    CountryCode("+44", "🇬🇧")
)

@Composable
fun RegistrationScreen(
    onRegistrationComplete: (String) -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var phoneNumber by remember { mutableStateOf("") }
    val selectedCountry = countryCodes[0]

    val scrollState = rememberScrollState()

    // Handle state changes
    LaunchedEffect(uiState) {
        if (uiState is RegistrationUiState.OtpSent) {
            onRegistrationComplete(phoneNumber)
        }
        if (uiState is RegistrationUiState.ProfileSetup) {
            // Auto-verified during registration
            onRegistrationComplete(phoneNumber)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            Spacer(modifier = Modifier.height(LocalSpacing.current.xl))

            // Professional Branding Section (Using LoginHeader style)
            RegistrationHeader()

            Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

            AcadMateCard(
                variant = CardVariant.Elevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalSpacing.current.md)
            ) {
                Column {
                    // Role tabs
                    TabRow(
                        selectedTabIndex = if (selectedRole == UserRole.STUDENT) 0 else 1,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            val index = if (selectedRole == UserRole.STUDENT) 0 else 1
                            if (index < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[index]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedRole == UserRole.STUDENT,
                            onClick = { selectedRole = UserRole.STUDENT },
                            text = { 
                                Text(
                                    "Student",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedRole == UserRole.STUDENT) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                        Tab(
                            selected = selectedRole == UserRole.FACULTY,
                            onClick = { selectedRole = UserRole.FACULTY },
                            text = { 
                                Text(
                                    "Faculty",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedRole == UserRole.FACULTY) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

                    // Content based on role
                    AnimatedContent(
                        targetState = selectedRole,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = "RegistrationFormAnimation"
                    ) { role ->
                        when (role) {
                            UserRole.STUDENT -> StudentRegistrationForm(
                                phoneNumber = phoneNumber,
                                onPhoneNumberChange = { if (it.length <= 10) phoneNumber = it },
                                selectedCountry = selectedCountry,
                                onRegister = { activity -> viewModel.registerWithPhone(phoneNumber, UserRole.STUDENT, activity) },
                                uiState = uiState
                            )
                            UserRole.FACULTY -> FacultyRegistrationForm(
                                phoneNumber = phoneNumber,
                                onPhoneNumberChange = { if (it.length <= 10) phoneNumber = it },
                                selectedCountry = selectedCountry,
                                onRegister = { activity -> viewModel.registerWithPhone(phoneNumber, UserRole.FACULTY, activity) },
                                uiState = uiState
                            )
                            else -> {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

            // Back to login link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalSpacing.current.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBackToLogin() }
                )
            }
            
            Spacer(modifier = Modifier.height(LocalSpacing.current.lg))
        }
    }
}

@Composable
fun RegistrationHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = LocalSpacing.current.md)
    ) {
        AcadMateLogo(iconSize = 100.dp, showText = false)

        Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Join the smart academic community",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.7f)
        )
    }
}

@Composable
fun StudentRegistrationForm(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountry: CountryCode,
    onRegister: (android.app.Activity) -> Unit,
    uiState: RegistrationUiState
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
    ) {
        // Phone input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs)
        ) {
            AcadMateTextField(
                value = "${selectedCountry.flag} ${selectedCountry.code}",
                onValueChange = {},
                label = "Code",
                readOnly = true,
                modifier = Modifier.weight(0.35f),
                enabled = false
            )

            AcadMateTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = "Phone Number",
                placeholder = "00000 00000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(0.65f),
                isError = uiState is RegistrationUiState.Error,
                errorMessage = if (uiState is RegistrationUiState.Error) uiState.message else ""
            )
        }

        AcadMateButton(
            text = "Create Account",
            onClick = { activity?.let { onRegister(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneNumber.length == 10 && uiState !is RegistrationUiState.Loading,
            loading = uiState is RegistrationUiState.Loading
        )

        Text(
            text = "By registering, you agree to our Terms of Service and Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LocalSpacing.current.md)
        )
    }
}

@Composable
fun FacultyRegistrationForm(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountry: CountryCode,
    onRegister: (android.app.Activity) -> Unit,
    uiState: RegistrationUiState
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
    ) {
        // Phone input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs)
        ) {
            AcadMateTextField(
                value = "${selectedCountry.flag} ${selectedCountry.code}",
                onValueChange = {},
                label = "Code",
                readOnly = true,
                modifier = Modifier.weight(0.35f),
                enabled = false
            )

            AcadMateTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = "Phone Number",
                placeholder = "00000 00000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(0.65f),
                isError = uiState is RegistrationUiState.Error,
                errorMessage = if (uiState is RegistrationUiState.Error) uiState.message else ""
            )
        }

        AcadMateButton(
            text = "Register as Faculty",
            onClick = { activity?.let { onRegister(it) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneNumber.length == 10 && uiState !is RegistrationUiState.Loading,
            loading = uiState is RegistrationUiState.Loading
        )

        Text(
            text = "Your institutional credentials will be verified by the administration",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LocalSpacing.current.md)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    AcadMateTheme {
        RegistrationScreen(
            onRegistrationComplete = {},
            onBackToLogin = {}
        )
    }
}
