package com.acadmate.dashboard.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageNotificationsScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val attendanceAlerts by viewModel.attendanceAlertsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val assignmentReminders by viewModel.assignmentRemindersEnabled.collectAsStateWithLifecycle(initialValue = true)
    val examNotifications by viewModel.examNotificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val smartInsights by viewModel.smartInsightsEnabled.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(LocalSpacing.current.md),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
        ) {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            AcadMateCard(variant = CardVariant.Flat) {
                Column {
                    NotificationToggleRow(
                        title = "Attendance Alerts",
                        subtitle = "Notify when attendance is marked or missed",
                        checked = attendanceAlerts,
                        onCheckedChange = { viewModel.setAttendanceAlertsEnabled(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                    )
                    NotificationToggleRow(
                        title = "Assignment Deadlines",
                        subtitle = "Reminders for upcoming submissions",
                        checked = assignmentReminders,
                        onCheckedChange = { viewModel.setAssignmentRemindersEnabled(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                    )
                    NotificationToggleRow(
                        title = "Exams & Results",
                        subtitle = "Updates on schedules and marks",
                        checked = examNotifications,
                        onCheckedChange = { viewModel.setExamNotificationsEnabled(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                    )
                    NotificationToggleRow(
                        title = "AI Smart Insights",
                        subtitle = "Personalized study tips and gap analysis",
                        checked = smartInsights,
                        onCheckedChange = { viewModel.setSmartInsightsEnabled(it) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Note: System critical alerts cannot be disabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
