package com.acadmate.dashboard.student

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.platform.LocalContext
import com.acadmate.core.db.UserRepository
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = FirebaseAuth.getInstance().currentUser?.displayName ?: "",
    val email: String = FirebaseAuth.getInstance().currentUser?.email ?: "",
    val phone: String = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: "",
    val enrollment: String = "",
    val department: String = "",
    val role: String = "",
    val address: String = "N/A",
    val overallAttendance: Float = 0.78f,
    val classesAttended: Int = 35,
    val classesMissed: Int = 10,
    val profilePictureUrl: String? = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState = ProfileUiState(),
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onPushNotificationsClick: () -> Unit = {},
    isDarkMode: Boolean = false,
    onDarkModeToggle: (Boolean) -> Unit = {},
    viewModel: ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val updateState by viewModel.updateState.collectAsState()
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadProfilePicture(it)
        }
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            is ProfileUpdateState.Success -> {
                snackbarHostState.showSnackbar((updateState as ProfileUpdateState.Success).message)
                viewModel.resetState()
            }
            is ProfileUpdateState.Error -> {
                snackbarHostState.showSnackbar((updateState as ProfileUpdateState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            uiState = uiState,
            onDismiss = { showEditDialog = false },
            onSave = { name, email, address ->
                viewModel.updateProfile(name, email, address)
                showEditDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Profile", 
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineSmall
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        MeshBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Profile Avatar & Name ────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = LocalSpacing.current.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(
                                    3.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                            ) {
                                if (uiState.profilePictureUrl != null) {
                                    AsyncImage(
                                        model = uiState.profilePictureUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center, 
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            text = if (uiState.name.isNotEmpty()) uiState.name.take(1).uppercase() else "?",
                                            style = MaterialTheme.typography.displayMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                                
                                if (updateState is ProfileUpdateState.Loading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = Color.White,
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            }
                            
                            Surface(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                shadowElevation = 4.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Photo",
                                    modifier = Modifier.padding(8.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(LocalSpacing.current.md))

                        Text(
                            text = uiState.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.email,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // ── Attendance Overview Card ─────────────────────────────────────
                    AcadMateCard(
                        variant = CardVariant.Flat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LocalSpacing.current.md)
                    ) {
                        Column {
                            Text(
                                text = "ATTENDANCE OVERVIEW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(LocalSpacing.current.md))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatColumn(
                                    value = "${(uiState.overallAttendance * 100).toInt()}%",
                                    label = "Overall",
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatColumn(
                                    value = uiState.classesAttended.toString(),
                                    label = "Attended",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                StatColumn(
                                    value = uiState.classesMissed.toString(),
                                    label = "Missed",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

                    // ── Personal Information Card ────────────────────────────────────
                    AcadMateCard(
                        variant = CardVariant.Flat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LocalSpacing.current.md)
                    ) {
                        Column {
                            Text(
                                text = "PERSONAL INFORMATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(LocalSpacing.current.md))
                            InfoRow("Phone", uiState.phone)
                            InfoRow("Email", uiState.email)
                            InfoRow("Address", uiState.address)
                            InfoRow("Enrollment", uiState.enrollment)
                            InfoRow("Department", uiState.department)
                            InfoRow("Role", uiState.role, isLast = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

                    // ── Quick Actions ────────────────────────────────────────────────
                    Column(modifier = Modifier.padding(horizontal = LocalSpacing.current.md)) {
                        SectionHeader(title = "Quick Actions")
                        Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.smd)
                        ) {
                            ProfileQuickAction(
                                icon = Icons.Default.Notifications,
                                label = "Notifications",
                                modifier = Modifier.weight(1f),
                                onClick = onNotificationsClick
                            )
                            ProfileQuickAction(
                                icon = Icons.Default.Shield,
                                label = "Privacy",
                                modifier = Modifier.weight(1f),
                                onClick = onPrivacyClick
                            )
                            ProfileQuickAction(
                                icon = Icons.AutoMirrored.Filled.HelpOutline,
                                label = "Help",
                                modifier = Modifier.weight(1f),
                                onClick = onHelpClick
                            )
                            ProfileQuickAction(
                                icon = Icons.Default.Info,
                                label = "About",
                                modifier = Modifier.weight(1f),
                                onClick = onAboutClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

                    // ── Settings Section ─────────────────────────────────────────────
                    AcadMateCard(
                        variant = CardVariant.Flat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LocalSpacing.current.md)
                    ) {
                        Column {
                            SettingsRow(
                                icon = Icons.Default.DarkMode,
                                title = "Dark Mode",
                                subtitle = "Toggle app theme",
                                onClick = { onDarkModeToggle(!isDarkMode) },
                                trailingContent = {
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = onDarkModeToggle,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            SettingsRow(
                                icon = Icons.Default.Notifications,
                                title = "Push Notifications",
                                subtitle = "Manage preferences",
                                onClick = onPushNotificationsClick
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            SettingsRow(
                                icon = Icons.AutoMirrored.Filled.ExitToApp,
                                title = "Sign Out",
                                subtitle = "Log out of your account",
                                onClick = onSignOut,
                                isDanger = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(LocalSpacing.current.xxxl))
                }
                
                // Full screen loading overlay if needed (optional, already have one on the image)
                if (updateState is ProfileUpdateState.Loading && uiState.profilePictureUrl == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    uiState: ProfileUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(uiState.name) }
    var email by remember { mutableStateOf(uiState.email) }
    var address by remember { mutableStateOf(uiState.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                AcadMateTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    placeholder = "Enter your name",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                AcadMateTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Institutional Email",
                    placeholder = "Enter your email",
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                AcadMateTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Address",
                    placeholder = "Enter your home address",
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        },
        confirmButton = {
            AcadMateButton(
                text = "Save Changes",
                onClick = { onSave(name, email, address) },
                enabled = name.isNotEmpty() && email.isNotEmpty()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoRow(label: String, value: String, isLast: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    if (!isLast) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f),
            thickness = 0.5.dp
        )
    }
}

@Composable
fun ProfileQuickAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            color = if (isDanger)
                MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(LocalSpacing.current.smd))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    AcadMateTheme(darkTheme = false) {
        ProfileScreen()
    }
}
