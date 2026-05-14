package com.acadmate.dashboard.student

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.SectionHeader
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.designsystem.theme.LocalSpacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalContext
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.acadmate.core.alarms.AlarmItem
import com.acadmate.core.alarms.AndroidAlarmScheduler
import com.acadmate.core.alarms.AutoAlarmWorker

data class TimetableEntry(
    val id: String,
    val subject: String,
    val faculty: String,
    val room: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val dayOfWeek: String,
    val date: LocalDate,
    val isCurrent: Boolean = false,
    val isCompleted: Boolean = false,
    var isAlarmSet: Boolean = false
)

data class TimetableUiState(
    val selectedDay: String = "Monday",
    val timetableEntries: List<TimetableEntry> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimetableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val autoAlarmsEnabled by viewModel.autoAlarmsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Timetable",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Auto-Alarms",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = autoAlarmsEnabled,
                            onCheckedChange = { isEnabled ->
                                viewModel.setAutoAlarmsEnabled(isEnabled)
                                if (isEnabled) {
                                    val workRequest = PeriodicWorkRequestBuilder<AutoAlarmWorker>(24, TimeUnit.HOURS)
                                        .addTag("AutoAlarmWork")
                                        .build()
                                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                        "AutoAlarmWork",
                                        ExistingPeriodicWorkPolicy.UPDATE,
                                        workRequest
                                    )
                                } else {
                                    WorkManager.getInstance(context).cancelUniqueWork("AutoAlarmWork")
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { 
                            // Sync manually
                            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            if (userId != null) {
                                viewModel.loadTimetableForDay(uiState.selectedDay)
                            }
                        }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Timetable")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Day selector
            DaySelector(
                days = daysOfWeek,
                selectedDay = uiState.selectedDay,
                onDaySelected = { viewModel.loadTimetableForDay(it) }
            )

            Spacer(modifier = Modifier.height(LocalSpacing.current.md))

            // Timetable content
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.timetableEntries.isEmpty()) {
                EmptyTimetableState(uiState.selectedDay)
            } else {
                TimetableList(
                    entries = uiState.timetableEntries,
                    onToggleAlarm = { id, isSet -> viewModel.toggleAlarm(id, isSet) }
                )
            }
        }
    }
}

@Composable
fun DaySelector(
    days: List<String>,
    selectedDay: String,
    onDaySelected: (String) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = days.indexOf(selectedDay),
        modifier = Modifier.fillMaxWidth(),
        edgePadding = LocalSpacing.current.md,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[days.indexOf(selectedDay)]),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        days.forEach { day ->
            Tab(
                selected = day == selectedDay,
                onClick = { onDaySelected(day) },
                text = {
                    Text(
                        text = day.take(3), // Mon, Tue, etc.
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (day == selectedDay) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
fun TimetableList(
    entries: List<TimetableEntry>,
    onToggleAlarm: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = LocalSpacing.current.md, vertical = LocalSpacing.current.sm),
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)
    ) {
        items(entries) { entry ->
            TimetableEntryCard(entry = entry, onToggleAlarm = onToggleAlarm)
        }
    }
}

@Composable
fun TimetableEntryCard(
    entry: TimetableEntry,
    onToggleAlarm: (String, Boolean) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val context = LocalContext.current
    val scheduler = remember { AndroidAlarmScheduler(context) }
    var isAlarmSet by remember { mutableStateOf(entry.isAlarmSet) }

    AcadMateCard(
        variant = CardVariant.Flat,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalSpacing.current.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = LocalSpacing.current.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                entry.isCurrent -> MaterialTheme.colorScheme.primary
                                entry.isCompleted -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            }
                        )
                )

                if (!entry.isCompleted) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(60.dp)
                            .background(
                                if (entry.isCurrent)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Class details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = entry.faculty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${entry.room} • ${entry.startTime.format(timeFormatter)} - ${entry.endTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Status indicator
                    Surface(
                        color = when {
                            entry.isCurrent -> MaterialTheme.colorScheme.primaryContainer
                            entry.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = when {
                                entry.isCurrent -> "Now"
                                entry.isCompleted -> "Done"
                                else -> "Upcoming"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                entry.isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                                entry.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            // Action button and Alarm Toggle
            Row {
                IconButton(onClick = {
                    isAlarmSet = !isAlarmSet
                    entry.isAlarmSet = isAlarmSet
                    onToggleAlarm(entry.id, isAlarmSet)
                    
                    val classDateTime = LocalDateTime.of(entry.date, entry.startTime)
                    val alarmTime = classDateTime.minusMinutes(5)
                    
                    val alarmItem = AlarmItem(
                        id = entry.id,
                        time = alarmTime,
                        title = "Upcoming Class: ${entry.subject}",
                        message = "Starts at ${entry.startTime.format(timeFormatter)} in ${entry.room}"
                    )
                    
                    if (isAlarmSet) {
                        scheduler.schedule(alarmItem)
                    } else {
                        scheduler.cancel(alarmItem)
                    }
                }) {
                    Icon(
                        imageVector = if (isAlarmSet) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Toggle Alarm",
                        tint = if (isAlarmSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTimetableState(day: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalSpacing.current.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.EventNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

        Text(
            text = "No classes on $day",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.sm))

        Text(
            text = "Enjoy your free time or review your study materials.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
