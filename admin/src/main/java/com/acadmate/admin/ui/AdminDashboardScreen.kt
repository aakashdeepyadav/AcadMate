package com.acadmate.admin.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.SectionHeader
import com.acadmate.designsystem.theme.LocalSpacing
import kotlinx.coroutines.launch
import com.acadmate.designsystem.components.MeshBackground
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.acadmate.core.model.AdminAction
import com.acadmate.core.model.ActionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onAddUserClick: () -> Unit,
    onManageCoursesClick: () -> Unit = {},
    onSyllabusClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onAiScheduleClick: () -> Unit = {},
    onAuditLogClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween<Float>(durationMillis = 800),
        label = "AdminDashboardAlpha"
    )

    if (showAnnouncementDialog) {
        AnnouncementDialog(
            onDismiss = { showAnnouncementDialog = false },
            onPost = { title, content ->
                viewModel.postAnnouncement(title, content)
                showAnnouncementDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Announcement posted successfully")
                }
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
                        "Institution Admin", 
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineSmall
                    ) 
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp, 
                            contentDescription = "Sign Out", 
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddUserClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add User")
            }
        }
    ) { padding ->
        MeshBackground {
            when (val state = uiState) {
                is AdminUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }
                is AdminUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .graphicsLayer(alpha = alpha),
                        contentPadding = PaddingValues(LocalSpacing.current.md),
                        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
                    ) {
                    item {
                        Column {
                            Text(
                                text = "System Overview",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = state.institutionName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }

                    item {
                        SectionHeader(title = "Institution Stats")
                        Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
                        ) {
                            StatCard(
                                title = "Students",
                                count = state.totalStudents.toString(),
                                icon = Icons.Default.Groups,
                                color = Color(0xFF4A90E2),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Faculty",
                                count = state.totalFaculty.toString(),
                                icon = Icons.Default.AssignmentInd,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
                        ) {
                            StatCard(
                                title = "Total Courses",
                                count = state.totalCourses.toString(),
                                icon = Icons.Default.LibraryBooks,
                                color = Color(0xFFF5A623),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = if (state.pendingApprovals > 0) "Security Alerts" else "System Health",
                                count = if (state.pendingApprovals > 0) "${state.pendingApprovals}" else "Healthy",
                                icon = if (state.pendingApprovals > 0) Icons.Default.GppBad else Icons.Default.Dns,
                                color = if (state.pendingApprovals > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                                trend = if (state.pendingApprovals > 0) "Anomalies Detected" else "99.9% Uptime",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        SectionHeader(title = "Administrative Hub")
                        Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
                        QuickActionsGrid(
                            onAddUser = onAddUserClick,
                            onManageCourses = onManageCoursesClick,
                            onSchedule = onScheduleClick,
                            onSyllabus = onSyllabusClick,
                            onPostAnnouncement = { showAnnouncementDialog = true },
                            onAuditLog = onAuditLogClick,
                            onSettingsClick = onSettingsClick,
                            onAiScheduleClick = onAiScheduleClick,
                            onComingSoon = { feature ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("$feature module coming soon")
                                }
                            }
                        )
                    }

                    item {
                        SectionHeader(title = "Institutional Activity")
                    }

                    items(state.recentActions) { action ->
                        ActionItem(action)
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(LocalSpacing.current.xl))
                    }
                }
            }
            is AdminUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.error, 
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadAdminDashboard() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun QuickActionsGrid(
    onAddUser: () -> Unit,
    onManageCourses: () -> Unit,
    onSchedule: () -> Unit,
    onSyllabus: () -> Unit,
    onPostAnnouncement: () -> Unit,
    onAuditLog: () -> Unit,
    onSettingsClick: () -> Unit,
    onAiScheduleClick: () -> Unit = {},
    onComingSoon: (String) -> Unit
) {
    val actions = listOf(
        QuickAction("User Mgmt", Icons.Default.People, Color(0xFF6C5CE7), onAddUser),
        QuickAction("Course Mgmt", Icons.Default.Class, Color(0xFF00B894), onManageCourses),
        QuickAction("Syllabus", Icons.Default.MenuBook, Color(0xFF0EA5E9), onSyllabus),
        QuickAction("Schedule", Icons.Default.CalendarMonth, Color(0xFFE17055), onSchedule),
        QuickAction("AI Scheduler", Icons.Default.AutoAwesome, Color(0xFF6C5CE7), onAiScheduleClick),
        QuickAction("Announcements", Icons.Default.Campaign, Color(0xFF0984E3), onPostAnnouncement),
        QuickAction("Audit Logs", Icons.Default.Shield, Color(0xFF2D3436), onAuditLog)
    )

    Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
        for (i in actions.indices step 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
            ) {
                for (j in 0..2) {
                    if (i + j < actions.size) {
                        QuickActionItem(
                            action = actions[i + j],
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun QuickActionItem(
    action: QuickAction,
    modifier: Modifier = Modifier
) {
    AcadMateCard(
        onClick = action.onClick,
        modifier = modifier,
        variant = CardVariant.Flat
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = action.color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    trend: String? = null,
    modifier: Modifier = Modifier
) {
    AcadMateCard(
        modifier = modifier,
        variant = CardVariant.Glass,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                
                if (trend != null) {
                    Surface(
                        color = if (trend.contains("Anomalies") || trend.contains("Alert")) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) 
                            else Color(0xFF10B981).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = trend,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (trend.contains("Anomalies") || trend.contains("Alert")) 
                                MaterialTheme.colorScheme.error 
                                else Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = count, 
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title, 
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ActionItem(action: AdminAction) {
    val (icon, iconColor) = when(action.type) {
        ActionType.USER_CREATED -> Icons.Default.PersonAdd to Color(0xFF00B894)
        ActionType.INSTITUTION_UPDATED -> Icons.Default.AutoAwesome to Color(0xFF6C5CE7)
        ActionType.ANNOUNCEMENT_POSTED -> Icons.Default.Campaign to Color(0xFFE17055)
        ActionType.COURSE_ADDED -> Icons.Default.LibraryAdd to Color(0xFF0984E3)
        ActionType.ATTENDANCE_ANALYTICS_GENERATED -> Icons.Default.Insights to Color(0xFFF1C40F)
        ActionType.SYSTEM_ALERT -> Icons.Default.ReportProblem to Color(0xFFD63031)
    }

    AcadMateCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.Flat,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        cornerRadius = 16.dp,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title, 
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (action.description.isNotEmpty()) {
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Text(
                text = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(action.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun AnnouncementDialog(
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(
            variant = CardVariant.Elevated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentPadding = 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Post Announcement",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                AcadMateTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title",
                    placeholder = "e.g. Holiday Notice"
                )
                
                AcadMateTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = "Content",
                    placeholder = "Enter announcement details...",
                    modifier = Modifier.height(120.dp),
                    singleLine = false
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    AcadMateButton(
                        text = "Post",
                        onClick = { onPost(title, content) },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && content.isNotBlank()
                    )
                }
            }
        }
    }
}
