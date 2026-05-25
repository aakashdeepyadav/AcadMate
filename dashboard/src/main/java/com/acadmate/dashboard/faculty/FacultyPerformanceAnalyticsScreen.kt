package com.acadmate.dashboard.faculty

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyPerformanceAnalyticsScreen(
    viewModel: FacultyPerformanceViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val atRiskStudents by viewModel.atRiskStudents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Insights") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "At-Risk Students",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "Students with <75% attendance or low internal scores",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (atRiskStudents.isEmpty()) {
                item {
                    Text("No students currently at risk.", modifier = Modifier.padding(vertical = 24.dp), color = Color.Gray)
                }
            }

            items(atRiskStudents) { student ->
                RiskStudentCard(student)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Subject Performance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AcadMateCard(variant = CardVariant.Elevated) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Average Score Trend", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(100.dp)) // Placeholder for chart
                        Text("Data visualization coming soon", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
                    }
                }
            }
        }
    }
}

data class RiskStudent(
    val id: String,
    val name: String,
    val attendance: Float,
    val remark: String
)

@Composable
fun RiskStudentCard(student: RiskStudent) {
    AcadMateCard(variant = CardVariant.Flat) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("Attendance: ${student.attendance}%", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                Text(student.remark, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            OutlinedButton(onClick = { /* Message Student */ }, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.MailOutline, contentDescription = "Email", modifier = Modifier.size(18.dp))
            }
        }
    }
}
