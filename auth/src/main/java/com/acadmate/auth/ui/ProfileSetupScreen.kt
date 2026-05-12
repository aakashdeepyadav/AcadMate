package com.acadmate.auth.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.acadmate.core.db.UserRepository
import com.acadmate.core.model.UserRole
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.acadmate.designsystem.components.AcadMateLogo

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileSetupUiState>(ProfileSetupUiState.Idle)
    val uiState: StateFlow<ProfileSetupUiState> = _uiState

    fun uploadProfilePicture(imageUri: android.net.Uri, userId: String) {
        _uiState.value = ProfileSetupUiState.Uploading
        viewModelScope.launch {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                val profilePicRef = storageRef.child("profile_pictures/$userId.jpg")

                val uploadTask = profilePicRef.putFile(imageUri).await()

                val downloadUrl = uploadTask.storage.downloadUrl.await()

                val result = userRepository.updateProfilePicture(userId, downloadUrl.toString())
                if (result.isSuccess) {
                    _uiState.value = ProfileSetupUiState.Success(downloadUrl.toString())
                } else {
                    _uiState.value = ProfileSetupUiState.Error("Failed to update profile picture")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileSetupUiState.Error("Upload failed: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = ProfileSetupUiState.Idle
    }
}

sealed class ProfileSetupUiState {
    object Idle : ProfileSetupUiState()
    object Uploading : ProfileSetupUiState()
    data class Success(val imageUrl: String) : ProfileSetupUiState()
    data class Error(val message: String) : ProfileSetupUiState()
}

@Composable
fun ProfileSetupScreen(
    phoneNumber: String,
    onComplete: () -> Unit,
    viewModel: RegistrationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileSetupViewModel: ProfileSetupViewModel = hiltViewModel()
    val profileSetupUiState by profileSetupViewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var enrollmentNumber by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        uri?.let {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@let
            profileSetupViewModel.uploadProfilePicture(it, userId)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is RegistrationUiState.Success) {
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(LocalSpacing.current.xl))
            
            AcadMateLogo(iconSize = 100.dp, showText = false)

            Spacer(modifier = Modifier.height(LocalSpacing.current.lg))
            
            Text(
                text = "Complete Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Personalize your AcadMate experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(LocalSpacing.current.xl))

            // Profile Picture Section
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(8.dp, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Surface(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    shadowElevation = 4.dp
                ) {
                    if (profileSetupUiState is ProfileSetupUiState.Uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (profileSetupUiState is ProfileSetupUiState.Error) {
                Text(
                    text = (profileSetupUiState as ProfileSetupUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(LocalSpacing.current.xl))

            AcadMateCard(
                variant = CardVariant.Elevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalSpacing.current.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                    AcadMateTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name",
                        placeholder = "John Doe",
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )

                    AcadMateTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Institutional Email",
                        placeholder = "john.doe@university.edu",
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )

                    AcadMateTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Set Password",
                        placeholder = "••••••••",
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
                    )

                    Text(
                        text = "I am a...",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
                    ) {
                        RoleSelector(
                            title = "Student",
                            selected = selectedRole == UserRole.STUDENT,
                            onClick = { selectedRole = UserRole.STUDENT },
                            modifier = Modifier.weight(1f)
                        )
                        RoleSelector(
                            title = "Faculty",
                            selected = selectedRole == UserRole.FACULTY,
                            onClick = { selectedRole = UserRole.FACULTY },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedRole == UserRole.STUDENT) {
                        AcadMateTextField(
                            value = enrollmentNumber,
                            onValueChange = { if (it.all { char -> char.isDigit() }) enrollmentNumber = it },
                            label = "Enrollment Number",
                            placeholder = "8 digits",
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }

                    AcadMateTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = "Department / Course",
                        placeholder = "e.g. Computer Science"
                    )

                    Spacer(modifier = Modifier.height(LocalSpacing.current.md))

                    if (uiState is RegistrationUiState.Error) {
                        Text(
                            text = (uiState as RegistrationUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    AcadMateButton(
                        text = "Finish Setup",
                        onClick = {
                            val uploadedUrl = (profileSetupUiState as? ProfileSetupUiState.Success)?.imageUrl
                            viewModel.completeProfileSetup(
                                name = name,
                                email = email,
                                password = password,
                                enrollmentNumber = enrollmentNumber,
                                department = department,
                                role = selectedRole,
                                profilePictureUrl = uploadedUrl
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        loading = uiState is RegistrationUiState.Loading,
                        enabled = name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && 
                                 department.isNotEmpty() && profileSetupUiState !is ProfileSetupUiState.Uploading
                    )
                }
            }
            Spacer(modifier = Modifier.height(LocalSpacing.current.xl))
        }
    }
}

@Composable
fun RoleSelector(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
