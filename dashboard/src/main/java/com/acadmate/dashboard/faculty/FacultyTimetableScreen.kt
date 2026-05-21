package com.acadmate.dashboard.faculty

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acadmate.dashboard.student.DaySelector
import com.acadmate.dashboard.student.EmptyTimetableState
import com.acadmate.dashboard.student.TimetableEntry
import com.acadmate.dashboard.student.TimetableUiState
import com.acadmate.dashboard.student.TimetableViewModel
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyTimetableScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimetableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val facultyUiState by hiltViewModel<FacultyHomeViewModel>().uiState.collectAsStateWithLifecycle()
    val autoAlarmsEnabled by viewModel.autoAlarmsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val context = androidx.compose.ui.platform.LocalContext.current

    val filteredEntries = remember(uiState.timetableEntries, facultyUiState.name) {
        if (facultyUiState.name.isBlank()) uiState.timetableEntries
        else uiState.timetableEntries.filter { 
            it.faculty.contains(facultyUiState.name, ignoreCase = true) || 
            facultyUiState.name.contains(it.faculty, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Teaching Schedule",
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
                                    val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.acadmate.core.alarms.AutoAlarmWorker>(6, java.util.concurrent.TimeUnit.HOURS)
                                        .addTag("AutoAlarmWork")
                                        .build()
                                    androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                        "AutoAlarmWork",
                                        androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                                        workRequest
                                    )
                                } else {
                                    androidx.work.WorkManager.getInstance(context).cancelUniqueWork("AutoAlarmWork")
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { 
                            viewModel.loadTimetableForDay(uiState.selectedDay)
                        }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Schedule")
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
            DaySelector(
                days = daysOfWeek,
                selectedDay = uiState.selectedDay,
                onDaySelected = { viewModel.loadTimetableForDay(it) }
            )

            Spacer(modifier = Modifier.height(LocalSpacing.current.md))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No classes scheduled for ${uiState.selectedDay}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = LocalSpacing.current.md, vertical = LocalSpacing.current.sm),
                    verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)
                ) {
                    items(filteredEntries) { entry ->
                        FacultyTimetableItemCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun FacultyTimetableItemCard(entry: TimetableEntry) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${entry.room} • ${entry.startTime.format(timeFormatter)} - ${entry.endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Surface(
                color = when {
                    entry.isCurrent -> MaterialTheme.colorScheme.primaryContainer
                    entry.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = when {
                        entry.isCurrent -> "In Progress"
                        entry.isCompleted -> "Completed"
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
}
