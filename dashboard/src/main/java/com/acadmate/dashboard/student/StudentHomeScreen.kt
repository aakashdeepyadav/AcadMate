package com.acadmate.dashboard.student

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.*
import com.acadmate.designsystem.components.AcadMateLogo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.graphicsLayer
import java.util.*

@Composable
fun StudentHomeScreen(
    viewModel: StudentHomeViewModel,
    onActionClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "DashboardAlpha"
    )

    // Show a prominent alert if a live session is active
    val isLiveSessionActive = uiState.currentClass != null && uiState.smartSuggestion?.contains("live session", ignoreCase = true) == true

    MeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                if (isLiveSessionActive) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { onActionClick("Mark Attendance") }) {
                                Text("VERIFY NOW", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = SoftBlue,
                        contentColor = Color.White
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Podcasts, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Live Class: ${uiState.nextClass}")
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .graphicsLayer(alpha = alpha),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
            ) {
            // Premium Top Bar
            item {
                HomeTopBar(uiState, onProfileClick, onActionClick, onRefresh = { viewModel.loadDashboardData() })
            }

            // Personalized Greeting
            item {
                GreetingSection(uiState.studentName)
            }

            // Visual Attendance Hero
            item {
                EnhancedAttendanceHero(
                    percentage = uiState.attendancePercentage,
                    currentClass = uiState.currentClass,
                    nextClass = uiState.nextClass,
                    timeLeft = uiState.nextClassIn,
                    onMarkAttendance = { onActionClick("Mark Attendance") }
                )
            }

            // Quick Stats glass row
            item {
                QuickStatsRow(uiState)
            }

            // Academic Services Grid
            item {
                CategoryGridCard(
                    title = "Academic Hub",
                    actions = listOf(
                        QuickActionItem("Attendance", Icons.Default.CheckCircle, SoftBlue),
                        QuickActionItem("Timetable", Icons.Default.Event, AccentPurple),
                        QuickActionItem("Results", Icons.Default.Assessment, AccentEmerald),
                        QuickActionItem("Assignments", Icons.Default.Assignment, AccentPink),
                        QuickActionItem("Materials", Icons.Default.Folder, Color(0xFF6C5CE7)),
                        QuickActionItem("Notice Board", Icons.Default.Campaign, AccentCyan)
                    ),
                    onActionClick = onActionClick
                )
            }

            // AI Suite Category
            item {
                CategoryGridCard(
                    title = "AI Powerhouse",
                    actions = listOf(
                        QuickActionItem("AI Tutor", Icons.Default.AutoAwesome, AccentPurple),
                        QuickActionItem("Syllabus", Icons.AutoMirrored.Filled.MenuBook, Color(0xFFF59E0B)),
                        QuickActionItem("Lecture", Icons.Default.GraphicEq, AccentPink)
                    ),
                    onActionClick = onActionClick
                )
            }

            // Smart Tools Row
            item {
                FeaturedToolsRow(uiState, onActionClick)
            }

            // AI Insights
            if (uiState.insights.isNotEmpty()) {
                item {
                    AiInsightsSection(uiState.insights, onDismiss = { viewModel.dismissInsight(it) })
                }
            }

            // Learning Path Progress
            item {
                CourseProgressSection(uiState.courseProgress, onActionClick)
            }

            // Today's Schedule (Polished)
            item {
                SectionHeader(
                    title = "Upcoming Sessions",
                    modifier = Modifier.padding(horizontal = LocalSpacing.current.md)
                )
                TodayScheduleSection(uiState.schedule, onActionClick)
            }

            // Deadlines
            item {
                UpcomingDeadlinesSection(uiState.deadlines)
            }
        }
    }
}
}

@Composable
fun CourseProgressSection(progressMap: Map<String, Float>, onActionClick: (String) -> Unit) {
    if (progressMap.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = LocalSpacing.current.md)) {
        SectionHeader(
            title = "Syllabus Progress",
            actionText = "Full Syllabus",
            onActionClick = { onActionClick("Syllabus") }
        )
        Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
        AcadMateCard(variant = CardVariant.Flat) {
            Column(modifier = Modifier.padding(16.dp)) {
                progressMap.entries.take(3).forEach { (subject, progress) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActionClick("Syllabus") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                subject, 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.bodyMedium, 
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = SoftBlue,
                            trackColor = SoftBlue.copy(alpha = 0.1f)
                        )
                    }
                    if (subject != progressMap.keys.take(3).last()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTopBar(uiState: HomeUiState, onProfileClick: () -> Unit, onActionClick: (String) -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LocalSpacing.current.md, vertical = LocalSpacing.current.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable { onProfileClick() }
                .shadow(4.dp, CircleShape),
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            border = BorderStroke(2.dp, Brush.linearGradient(GradientBrand))
        ) {
            if (uiState.profilePictureUrl != null) {
                AsyncImage(
                    model = uiState.profilePictureUrl,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        uiState.studentName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SoftBlue
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onActionClick("Search") },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onActionClick("Notice Board") },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.5f), CircleShape)
            ) {
                Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun GreetingSection(name: String) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column(modifier = Modifier.padding(horizontal = LocalSpacing.current.md)) {
        Text(
            text = "$greeting,",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (name.isNotEmpty()) name.split(" ").first() else "Scholar",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EnhancedAttendanceHero(
    percentage: Float,
    currentClass: String?,
    nextClass: String?,
    timeLeft: String,
    onMarkAttendance: () -> Unit
) {
    AcadMateCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSpacing.current.md),
        variant = CardVariant.Gradient,
        gradientColors = listOf(SoftBlue, Color(0xFF3B82F6)),
        cornerRadius = 24.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Pattern Decoration
            Icon(
                Icons.Default.Waves,
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .alpha(0.1f),
                tint = Color.White
            )

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Attendance Integrity",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2).sp
                            ),
                            color = Color.White
                        )
                    }
                    
                    // Circular Progress Ring
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier.size(80.dp),
                            color = Color.White,
                            strokeWidth = 8.dp,
                            trackColor = Color.White.copy(0.2f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            "Goal",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = Color.White.copy(0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val displayText = when {
                            currentClass != null -> "Ongoing: $currentClass"
                            nextClass != null -> "Next Class: $nextClass in $timeLeft"
                            else -> "No more classes today"
                        }
                        
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onMarkAttendance,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    enabled = currentClass != null
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = if (currentClass != null) SoftBlue else Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (currentClass != null) "Verify My Attendance" else "Waiting for Session", 
                        fontWeight = FontWeight.ExtraBold, 
                        color = if (currentClass != null) SoftBlue else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun QuickStatsRow(uiState: HomeUiState) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = LocalSpacing.current.md),
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)
    ) {
        item {
            StatMiniCard("CGPA", uiState.cgpaEstimate.toString(), Icons.Default.Insights, AccentEmerald)
        }
        item {
            StatMiniCard("Tasks", uiState.deadlines.size.toString(), Icons.Default.TaskAlt, AccentPink)
        }
        item {
            StatMiniCard("Streak", "12 Days", Icons.Default.Whatshot, WarningAmber)
        }
    }
}

@Composable
fun StatMiniCard(label: String, value: String, icon: ImageVector, color: Color) {
    AcadMateCard(
        variant = CardVariant.Glass,
        modifier = Modifier.width(110.dp),
        contentPadding = 12.dp
    ) {
        Column {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CategoryGridCard(
    title: String,
    actions: List<QuickActionItem>,
    onActionClick: (String) -> Unit
) {
    AcadMateCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSpacing.current.md),
        variant = CardVariant.Flat,
        backgroundColor = MaterialTheme.colorScheme.surface,
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val rows = actions.chunked(4)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowItems.forEach { item ->
                        GridActionItem(item, onClick = { onActionClick(item.title) })
                    }
                    repeat(4 - rowItems.size) { Spacer(modifier = Modifier.width(72.dp)) }
                }
                if (rowItems != rows.last()) Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GridActionItem(item: QuickActionItem, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(item.color.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .border(1.dp, item.color.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(26.dp), tint = item.color)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FeaturedToolsRow(uiState: HomeUiState, onActionClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = LocalSpacing.current.md)) {
        SectionHeader(title = "AI Smart Assist")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md),
            contentPadding = PaddingValues(vertical = LocalSpacing.current.xs)
        ) {
            item {
                VerticalFeaturedCard(
                    title = "Course Syllabus",
                    subtitle = "View your curriculum",
                    color = Color(0xFFEEF2FF),
                    icon = Icons.Default.MenuBook,
                    iconTint = SoftBlue,
                    onClick = { onActionClick("Syllabus") }
                )
            }
            item {
                VerticalFeaturedCard(
                    title = "Lecture Summarizer",
                    subtitle = "Audio to Notes",
                    color = Color(0xFFFDF2F8),
                    icon = Icons.Default.GraphicEq,
                    iconTint = AccentPink,
                    onClick = { onActionClick("Lecture") }
                )
            }
            item {
                VerticalFeaturedCard(
                    title = "Exam Simulation",
                    subtitle = "Test your skills",
                    color = Color(0xFFECFDF5),
                    icon = Icons.Default.School,
                    iconTint = AccentEmerald,
                    onClick = { onActionClick("AI Tutor") }
                )
            }
        }
    }
}

@Composable
fun VerticalFeaturedCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    AcadMateCard(
        modifier = Modifier
            .width(150.dp)
            .height(170.dp),
        backgroundColor = color,
        cornerRadius = 24.dp,
        contentPadding = 18.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconTint.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black.copy(0.8f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(0.5f)
                )
            }
        }
    }
}

@Composable
fun AiInsightsSection(insights: List<AiInsight>, onDismiss: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = LocalSpacing.current.md)) {
        SectionHeader(title = "Contextual Insights")
        insights.forEach { insight ->
            AcadMateCard(
                variant = CardVariant.Glass,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LocalSpacing.current.sm),
                contentPadding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        insight.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDismiss(insight.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TodayScheduleSection(schedule: List<ScheduleItem>, onActionClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = LocalSpacing.current.md)) {
        schedule.forEach { item ->
            ScheduleRow(item, onActionClick)
        }
    }
}

@Composable
fun ScheduleRow(item: ScheduleItem, onActionClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(55.dp)) {
            Text(
                item.time.split(" - ").first(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (item.isCurrent) SoftBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(if (item.isCurrent) 12.dp else 8.dp)
                    .background(if (item.isCurrent) SoftBlue else MaterialTheme.colorScheme.outline, CircleShape)
            )
        }

        AcadMateCard(
            modifier = Modifier
                .weight(1f)
                .clickable { onActionClick("Timetable") }
                .then(if (item.isCurrent) Modifier.border(2.dp, SoftBlue.copy(0.3f), RoundedCornerShape(18.dp)) else Modifier),
            variant = CardVariant.Flat,
            backgroundColor = if (item.isCurrent) Color.White else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
            cornerRadius = 18.dp
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${item.faculty} • ${item.room}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.isCurrent) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SoftBlue)
                }
            }
        }
    }
}

@Composable
fun UpcomingDeadlinesSection(deadlines: List<Deadline>) {
    Column(modifier = Modifier.padding(horizontal = LocalSpacing.current.md)) {
        SectionHeader(title = "Urgent Actions")
        deadlines.forEach { deadline ->
            AcadMateCard(
                variant = CardVariant.Flat,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                backgroundColor = if (deadline.urgency == Urgency.HIGH) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (deadline.urgency == Urgency.HIGH) Color.Red else AccentEmerald, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(deadline.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(deadline.dueDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class QuickActionItem(val title: String, val icon: ImageVector, val color: Color)
