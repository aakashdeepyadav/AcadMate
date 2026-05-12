package com.acadmate.attendance.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acadmate.attendance.data.AttendanceSession
import com.acadmate.attendance.data.MonthlySummary
import com.acadmate.attendance.domain.AttendanceViewModel
import com.acadmate.attendance.ui.components.LowAttendanceWarning
import com.acadmate.core.model.ActionType
import com.acadmate.core.model.AdminAction
import com.acadmate.core.model.UserRole
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    viewModel: AttendanceViewModel
) {
    val role by viewModel.userRole.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when(role) {
                            UserRole.FACULTY -> "Faculty Attendance"
                            UserRole.ADMIN -> "Institutional Attendance"
                            else -> "My Attendance"
                        },
                        fontWeight = FontWeight.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (role) {
                UserRole.FACULTY -> {
                    val facultyViewModel: com.acadmate.attendance.domain.FacultyAttendanceViewModel = hiltViewModel()
                    FacultyAttendanceHistoryScreen(facultyViewModel)
                }
                UserRole.ADMIN -> AdminAttendanceHistoryScreen()
                else -> StudentAttendanceHistoryScreen(viewModel)
            }
        }
    }
}

@Composable
fun StudentAttendanceHistoryScreen(
    viewModel: AttendanceViewModel
) {
    val history by viewModel.attendanceHistory.collectAsStateWithLifecycle()
    val monthlySummaries by viewModel.monthlySummaries.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Low attendance warning
        val currentMonth = YearMonth.now()
            val currentMonthSummary = monthlySummaries.find {
                it.month == currentMonth.monthValue && it.year == currentMonth.year
            }

            currentMonthSummary?.let {
                LowAttendanceWarning(
                    attendancePercentage = it.attendancePercentage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab navigation
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Sessions") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Monthly") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Calendar") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab content
            when (selectedTabIndex) {
                0 -> SessionsTab(history = history, viewModel = viewModel)
                1 -> MonthlyTab(monthlySummaries = monthlySummaries)
                2 -> CalendarTab(history = history)
            }
        }
}

@Composable
fun SessionsTab(
    history: List<AttendanceSession>,
    viewModel: AttendanceViewModel
) {
    if (history.isEmpty()) {
        EmptyStateScreen()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group by month
            val groupedByMonth = history.groupBy {
                YearMonth.of(it.date.year, it.date.month)
            }

            groupedByMonth.forEach { (yearMonth, sessions) ->
                item {
                    MonthHeader(yearMonth = yearMonth)
                }

                items(sessions) { session ->
                    AttendanceSessionCard(session = session)
                }
            }
        }
    }
}

@Composable
fun AttendanceSessionCard(session: AttendanceSession) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot and Subject
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (session.attended)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            CircleShape
                        )
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.subject,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = session.faculty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time and status
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${session.startTime} - ${session.endTime}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (session.attended) "✓ Present" else "✗ Absent",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (session.attended)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun MonthHeader(yearMonth: YearMonth) {
    Text(
        text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

@Composable
fun MonthlyTab(monthlySummaries: List<MonthlySummary>) {
    if (monthlySummaries.isEmpty()) {
        EmptyStateScreen()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(monthlySummaries) { summary ->
                MonthlySummaryCard(summary = summary)
            }
        }
    }
}

@Composable
fun MonthlySummaryCard(summary: MonthlySummary) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = YearMonth.of(summary.year, summary.month)
                        .format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${summary.classesAttended}/${summary.totalClasses} classes attended",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Animated percentage ring
            AttendancePercentageRing(
                percentage = summary.attendancePercentage,
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
fun AttendancePercentageRing(
    percentage: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
        )

        // Percentage text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = String.format("%.0f%%", percentage),
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    percentage >= 75f -> MaterialTheme.colorScheme.primary
                    percentage >= 50f -> Color(0xFFFFC107)
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
fun CalendarTab(history: List<AttendanceSession>) {
    // Simple calendar heatmap view
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Attendance Heatmap",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Simple grid of days
            val days = history.groupBy { it.date.dayOfMonth }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(31 / 7) { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(7) { day ->
                            val dayNum = week * 7 + day + 1
                            if (dayNum <= 31) {
                                val isPresent = days[dayNum]?.any { it.attended } ?: false
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            if (isPresent)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.shapes.small
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPresent)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FacultyAttendanceHistoryScreen(
    viewModel: com.acadmate.attendance.domain.FacultyAttendanceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Teaching Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Review attendance records for your sessions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.students.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Assessment, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("No records found for this session", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.students) { student ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = student.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Reg No: ${student.enrollmentNumber}", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (student.isPresent) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (student.isPresent) "PRESENT" else "ABSENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (student.isPresent) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAttendanceHistoryScreen() {
    val firestore = FirebaseFirestore.getInstance()
    var stats by remember { mutableStateOf<List<AdminAction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = firestore.collection("attendance")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
            
            stats = snapshot.documents.map { doc ->
                AdminAction(
                    id = doc.id,
                    title = "Attendance Marked",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    type = ActionType.ATTENDANCE_ANALYTICS_GENERATED,
                    description = "Student marked present in ${doc.getString("subject") ?: "Unknown"}"
                )
            }
        } catch (e: Exception) {
            // Log error
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Institutional Attendance Log",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Real-time feed of campus-wide presence activity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (stats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No attendance activity recorded yet", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(stats) { log ->
                    AdminLogItem(log)
                }
            }
        }
    }
}

@Composable
fun AdminLogItem(action: AdminAction) {
    AcadMateCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.Flat,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(LocalSpacing.current.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
            Text(
                text = dateFormat.format(Date(action.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun EmptyStateScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No attendance records yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mark your attendance to see history",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
