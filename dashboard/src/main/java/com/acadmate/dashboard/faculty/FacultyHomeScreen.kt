package com.acadmate.dashboard.faculty

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.SectionHeader
import com.acadmate.designsystem.theme.LocalSpacing
import com.acadmate.designsystem.components.MeshBackground
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun FacultyHomeScreen(
    viewModel: FacultyHomeViewModel = hiltViewModel(),
    onActionClick: (String, String?) -> Unit,
    onClassClick: (String, Int?) -> Unit,
    onLectureLogClick: () -> Unit = {},
    onPerformanceAnalyticsClick: () -> Unit = {},
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween<Float>(durationMillis = 800),
        label = "FacultyDashboardAlpha"
    )

    MeshBackground {
        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .graphicsLayer(alpha = alpha),
                contentPadding = PaddingValues(LocalSpacing.current.md),
                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
            ) {
                item {
                    FacultyHeader(
                        name = uiState.name, 
                        sessionCount = uiState.upcomingClasses.size,
                        isAssigned = uiState.isAssignedAnySubject,
                        onProfileClick = onProfileClick, 
                        onRefresh = { viewModel.refresh() }
                    )
                }

                if (uiState.isAssignedAnySubject) {
                    // Analytics Overview
                    item {
                        FacultyAnalyticsRow(
                            avgAttendance = uiState.averageAttendance,
                            assignmentCount = uiState.assignmentCount
                        )
                    }

                    // Live Class Status - High Priority
                    val liveClass = uiState.upcomingClasses.find { it.isLive }
                    if (liveClass != null) {
                        item {
                            LiveClassControlCard(
                                liveClass = liveClass,
                                onActionClick = onActionClick,
                                onClassClick = onClassClick
                            )
                        }
                    }

                    // Management Hub (Quick Actions)
                    item {
                        SectionHeader(title = "Faculty Hub")
                        val currentOrNextClass = uiState.upcomingClasses.find { it.isLive }?.id 
                            ?: uiState.upcomingClasses.firstOrNull()?.id
                        QuickActionGrid(
                            defaultClassId = currentOrNextClass,
                            onActionClick = onActionClick,
                            onLectureLogClick = onLectureLogClick,
                            onPerformanceAnalyticsClick = onPerformanceAnalyticsClick
                        )
                    }

                    // AI Tools for Faculty
                    item {
                        SectionHeader(title = "Smart Teaching")
                        FacultyAiTools(onActionClick)
                    }

                    // Recent Attendance Trends
                    item {
                        SectionHeader(title = "Attendance Analytics")
                        AttendanceTrendCard(uiState.attendanceTrends)
                    }

                    // Today's Teaching Schedule
                    item {
                        SectionHeader(title = "Today's Sessions")
                    }

                    if (uiState.upcomingClasses.isEmpty()) {
                        item {
                            EmptyScheduleState()
                        }
                    } else {
                        items(uiState.upcomingClasses) { facultyClass ->
                            ClassItemCard(facultyClass, onClassClick)
                        }
                    }
                } else {
                    item {
                        UnassignedFacultyState()
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun AttendanceTrendCard(trends: List<Float>) {
    AcadMateCard(variant = CardVariant.Flat) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Last 7 Days (Average)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                trends.forEach { height ->
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .fillMaxHeight(height)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
}

@Composable
fun FacultyAnalyticsRow(avgAttendance: String, assignmentCount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
    ) {
        StatMiniCard(
            label = "Avg Attendance",
            value = avgAttendance,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = Color(0xFFE0F2FE),
            tint = Color(0xFF0284C7),
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            label = "Assignments",
            value = assignmentCount,
            icon = Icons.Default.PendingActions,
            color = Color(0xFFFEF2F2),
            tint = Color(0xFFDC2626),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatMiniCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    AcadMateCard(
        modifier = modifier,
        backgroundColor = color,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun QuickActionGrid(
    defaultClassId: String?,
    onActionClick: (String, String?) -> Unit,
    onLectureLogClick: () -> Unit = {},
    onPerformanceAnalyticsClick: () -> Unit = {}
) {
    val actions = listOf(
        FacultyAction("Take Attendance", Icons.AutoMirrored.Filled.FactCheck, Color(0xFF4F46E5), "Mark Attendance"),
        FacultyAction("Assignments", Icons.AutoMirrored.Filled.Assignment, Color(0xFFF59E0B), "Post Assignment"),
        FacultyAction("Gradebook", Icons.Default.Grade, Color(0xFF8B5CF6), "Gradebook"),
        FacultyAction("Lecture Logs", Icons.Default.HistoryEdu, Color(0xFF10B981), "Lecture Logs"),
        FacultyAction("Analytics", Icons.Default.Insights, Color(0xFFEC4899), "Analytics"),
        FacultyAction("Materials", Icons.AutoMirrored.Filled.LibraryBooks, Color(0xFF0EA5E9), "Materials"),
        FacultyAction("Community", Icons.Default.Forum, Color(0xFF34D399), "Community"),
        FacultyAction("Timetable", Icons.Default.CalendarMonth, Color(0xFF6C5CE7), "Timetable"),
        FacultyAction("Announce", Icons.Default.Campaign, Color(0xFF2D3436), "Notice Board")
    )

    Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
        actions.chunked(3).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)
            ) {
                rowActions.forEach { action ->
                    AcadMateCard(
                        modifier = Modifier.weight(1f).height(100.dp),
                        variant = CardVariant.Elevated,
                        onClick = { 
                            when(action.route) {
                                "Lecture Logs" -> onLectureLogClick()
                                "Analytics" -> onPerformanceAnalyticsClick()
                                else -> {
                                    val effectiveId = if (action.route == "Mark Attendance") defaultClassId else null
                                    onActionClick(action.route, effectiveId) 
                                }
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(action.icon, contentDescription = null, tint = action.color, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = action.title, 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                if (rowActions.size < 3) {
                    repeat(3 - rowActions.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun FacultyAiTools(onActionClick: (String, String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md),
        contentPadding = PaddingValues(vertical = LocalSpacing.current.sm)
    ) {
        item {
            AiFeatureCard(
                title = "Lesson Planner",
                subtitle = "Generate Draft",
                icon = Icons.Default.AutoStories,
                color = Color(0xFFEEF2FF),
                tint = Color(0xFF4F46E5),
                onClick = { onActionClick("Lesson Planner", null) }
            )
        }
        item {
            AiFeatureCard(
                title = "Quiz Generator",
                subtitle = "Smart MCQ",
                icon = Icons.Default.EditNote,
                color = Color(0xFFFFF7ED),
                tint = Color(0xFFD97706),
                onClick = { onActionClick("Quiz Generator", null) }
            )
        }
    }
}

@Composable
fun AiFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    tint: Color,
    onClick: () -> Unit
) {
    AcadMateCard(
        modifier = Modifier.width(160.dp).height(100.dp),
        backgroundColor = color,
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun ClassItemCard(facultyClass: FacultyClass, onClassClick: (String, Int?) -> Unit) {
    AcadMateCard(
        variant = CardVariant.Elevated,
        modifier = Modifier.fillMaxWidth().clickable { 
            // Extract hour from time string like "09:00 AM - 10:30 AM"
            val hour = try {
                val timePart = facultyClass.time.split(" ")[0]
                val hourPart = timePart.split(":")[0].toInt()
                val isPm = facultyClass.time.contains("PM", ignoreCase = true)
                if (isPm && hourPart < 12) hourPart + 12 else if (!isPm && hourPart == 12) 0 else hourPart
            } catch (e: Exception) { null }
            
            onClassClick(facultyClass.id, hour) 
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${facultyClass.id} - ${facultyClass.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${facultyClass.time} • ${facultyClass.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyScheduleState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No teaching sessions today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LiveClassControlCard(
    liveClass: FacultyClass,
    onActionClick: (String, String?) -> Unit,
    onClassClick: (String, Int?) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    AcadMateCard(
        variant = CardVariant.Gradient,
        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF9333EA)),
        cornerRadius = 28.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Podcasts,
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .alpha(0.1f),
                tint = Color.White
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer(alpha = pulseAlpha),
                            color = Color(0xFF34D399),
                            shape = CircleShape
                        ) {}
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "LIVE SESSION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = liveClass.room,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = liveClass.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                
                Text(
                    text = "ID: ${liveClass.id} • ${liveClass.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onActionClick("Mark Attendance", liveClass.id) },
                        modifier = Modifier.weight(1.2f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START BEACON", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    
                    OutlinedButton(
                        onClick = { onClassClick(liveClass.id, null) },
                        modifier = Modifier.weight(0.8f).height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("STUDENTS", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Broadcasts secure ultrasonic signal to prevent proxy attendance.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun LazyItemScope.UnassignedFacultyState() {
    Box(
        modifier = Modifier.fillParentMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AcadMateCard(
            variant = CardVariant.Elevated,
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.School, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Profile Pending Assignment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You are not yet assigned to any subjects. Please contact the administrator to set up your teaching courses and syllabus.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FacultyHeader(name: String, sessionCount: Int, isAssigned: Boolean, onProfileClick: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Welcome, $name",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            val sessionText = when {
                !isAssigned -> "Academic profile pending setup"
                sessionCount == 0 -> "Your schedule is clear for today"
                sessionCount == 1 -> "You have 1 class session today"
                else -> "Ready for your $sessionCount scheduled classes?"
            }
            Text(
                sessionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp))
            }
        }
    }
}

data class FacultyAction(val title: String, val icon: ImageVector, val color: Color, val route: String)
