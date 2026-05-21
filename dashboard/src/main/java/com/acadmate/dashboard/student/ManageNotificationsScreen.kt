package com.acadmate.dashboard.student

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsStateWithLifecycle(initialValue = null)
    val isFaculty = userRole == com.acadmate.core.model.UserRole.FACULTY

    val attendanceAlerts by viewModel.attendanceAlertsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val assignmentReminders by viewModel.assignmentRemindersEnabled.collectAsStateWithLifecycle(initialValue = true)
    val examNotifications by viewModel.examNotificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val smartInsights by viewModel.smartInsightsEnabled.collectAsStateWithLifecycle(initialValue = false)
    
    val sessionReports by viewModel.sessionReportsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val securityAnomalies by viewModel.securityAnomaliesEnabled.collectAsStateWithLifecycle(initialValue = true)
    val leaveRequests by viewModel.leaveRequestsEnabled.collectAsStateWithLifecycle(initialValue = true)

    val autoAlarms by viewModel.autoAlarmsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val alarmVibration by viewModel.alarmVibrationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val alarmVolume by viewModel.alarmVolume.collectAsStateWithLifecycle(initialValue = 70)
    val alarmMinutesBefore by viewModel.alarmMinutesBefore.collectAsStateWithLifecycle(initialValue = 60)
    val alarmToneUri by viewModel.alarmTone.collectAsStateWithLifecycle(initialValue = "Default")

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                viewModel.setAlarmTone(uri.toString())
            }
        }
    }

    val currentRingtoneName = remember(alarmToneUri) {
        if (alarmToneUri == "Default") "Default"
        else {
            try {
                RingtoneManager.getRingtone(context, Uri.parse(alarmToneUri)).getTitle(context)
            } catch (e: Exception) {
                "Custom Tone"
            }
        }
    }

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
                    if (!isFaculty) {
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
                    } else {
                        NotificationToggleRow(
                            title = "Session Reports",
                            subtitle = "Receive daily summaries of your classes",
                            checked = sessionReports,
                            onCheckedChange = { viewModel.setSessionReportsEnabled(it) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                        )
                        NotificationToggleRow(
                            title = "Security Anomalies",
                            subtitle = "Real-time alerts for proxy attempts",
                            checked = securityAnomalies,
                            onCheckedChange = { viewModel.setSecurityAnomaliesEnabled(it) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                        )
                    }

                    NotificationToggleRow(
                        title = if (isFaculty) "Assignment Submissions" else "Assignment Deadlines",
                        subtitle = if (isFaculty) "Notify when students submit work" else "Reminders for upcoming submissions",
                        checked = assignmentReminders,
                        onCheckedChange = { viewModel.setAssignmentRemindersEnabled(it) }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                    )
                    
                    if (isFaculty) {
                        NotificationToggleRow(
                            title = "Leave Requests",
                            subtitle = "Notify when students apply for leave",
                            checked = leaveRequests,
                            onCheckedChange = { viewModel.setLeaveRequestsEnabled(it) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                        )
                    }

                    NotificationToggleRow(
                        title = "Exams & Scheduling",
                        subtitle = "Updates on dates and schedules",
                        checked = examNotifications,
                        onCheckedChange = { viewModel.setExamNotificationsEnabled(it) }
                    )
                    
                    if (!isFaculty) {
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
            }

            Text(
                text = "Class Auto Alarm",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            AcadMateCard(variant = CardVariant.Flat) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    NotificationToggleRow(
                        title = "Smart Auto-Alarm",
                        subtitle = if (isFaculty) "Ring before your FIRST session" else "Ring 1 hour before your FIRST class",
                        checked = autoAlarms,
                        onCheckedChange = { viewModel.setAutoAlarmsEnabled(it) }
                    )
                    
                    if (autoAlarms) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                        )
                        
                        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                            Text("Time Before Class", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${alarmMinutesBefore}m", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp))
                                Slider(
                                    value = alarmMinutesBefore.toFloat(),
                                    onValueChange = { viewModel.setAlarmMinutesBefore(it.toInt()) },
                                    valueRange = 10f..120f,
                                    steps = 10,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                            Text("Alarm Volume", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${alarmVolume}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp))
                                Slider(
                                    value = alarmVolume.toFloat(),
                                    onValueChange = { viewModel.setAlarmVolume(it.toInt()) },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        NotificationToggleRow(
                            title = "Vibration",
                            subtitle = "Vibrate during alarm",
                            checked = alarmVibration,
                            onCheckedChange = { viewModel.setAlarmVibrationEnabled(it) }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Alarm Tone", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(text = currentRingtoneName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = { 
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, if (alarmToneUri == "Default") null else Uri.parse(alarmToneUri))
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                }
                                ringtonePickerLauncher.launch(intent)
                            }) {
                                Text("Change")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
