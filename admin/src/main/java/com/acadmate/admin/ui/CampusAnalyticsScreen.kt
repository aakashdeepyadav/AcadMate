package com.acadmate.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusAnalyticsScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AdminUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AdminUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Institutional Performance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
                            AnalyticsSmallCard(
                                title = "Avg Attendance",
                                value = "${state.avgAttendance.toInt()}%",
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsSmallCard(
                                title = "Pass Percentage",
                                value = "${state.passPercentage.toInt()}%",
                                icon = Icons.Default.BarChart,
                                color = Color(0xFF6C5CE7),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        AcadMateCard(variant = CardVariant.Elevated) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Departmental Attendance", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (state.departmentAttendance.isEmpty()) {
                                    Text("No departmental data available", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                } else {
                                    val colors = listOf(Color(0xFF4A90E2), Color(0xFF10B981), Color(0xFFF5A623), Color(0xFFE17055), Color(0xFF6C5CE7))
                                    state.departmentAttendance.entries.forEachIndexed { index, entry ->
                                        DepartmentRow(
                                            name = entry.key,
                                            percentage = entry.value,
                                            color = colors[index % colors.size]
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        AcadMateCard(variant = CardVariant.Glass) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("System Statistics", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Total Students: ${state.totalStudents}")
                                Text("Total Faculty: ${state.totalFaculty}")
                                Text("Active Classes: ${state.activeClasses}")
                            }
                        }
                    }
                }
            }
            is AdminUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AnalyticsSmallCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    AcadMateCard(modifier = modifier, variant = CardVariant.Flat) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun DepartmentRow(name: String, percentage: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodySmall)
            Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
