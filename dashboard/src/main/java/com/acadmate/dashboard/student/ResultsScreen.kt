package com.acadmate.dashboard.student

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acadmate.designsystem.components.MeshBackground
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.SectionHeader
import com.acadmate.designsystem.theme.LocalSpacing

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acadmate.core.db.SemesterResultEntity
import com.acadmate.core.db.SubjectGradeEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSemester by remember { mutableStateOf<SemesterResultEntity?>(null) }
    var showSimulator by remember { mutableStateOf(false) }
    
    // Set initial selected semester when data loads
    LaunchedEffect(uiState.results) {
        if (selectedSemester == null && uiState.results.isNotEmpty()) {
            selectedSemester = uiState.results.last()
        }
    }

    if (showSimulator) {
        GpaSimulatorDialog(
            currentCgpa = uiState.currentCgpa,
            totalCredits = uiState.results.sumOf { it.credits },
            onDismiss = { showSimulator = false }
        )
    }

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )

    MeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Academic Results",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSimulator = true }) {
                            Icon(Icons.Default.Insights, contentDescription = "GPA Simulator", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .graphicsLayer(alpha = alpha),
                    contentPadding = PaddingValues(LocalSpacing.current.md),
                    verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg)
                ) {
                    // Overall Performance Section
                    item {
                        CgpaCard(cgpa = uiState.currentCgpa)
                    }

                    // Semester Selector
                    item {
                        SectionHeader(
                            title = "Semester Results",
                            actionText = "GPA Simulator",
                            onActionClick = { showSimulator = true }
                        )
                        Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
                        SemesterSelector(
                            semesters = uiState.results,
                            selectedSemester = selectedSemester,
                            onSemesterSelected = { selectedSemester = it }
                        )
                    }

                    // Selected Semester Details
                    selectedSemester?.let { semester ->
                        item {
                            SemesterSummaryCard(semester = semester)
                        }

                        items(semester.subjects) { subject ->
                            SubjectGradeCard(subject = subject)
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CgpaCard(cgpa: Float) {
    AcadMateCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSpacing.current.md),
        variant = CardVariant.Gradient,
        gradientColors = listOf(Color(0xFF2B5876), Color(0xFF4E4376)),
        cornerRadius = 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Cumulative GPA",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.2f", cgpa),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Out of 10.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            // Decorative Ring
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = (cgpa / 10f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "Top 10%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GpaTrendChart(results: List<SemesterResultEntity>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer

    AcadMateCard(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        variant = CardVariant.Flat,
        backgroundColor = Color.White,
        cornerRadius = 16.dp,
        contentPadding = 16.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)) {
            val width = size.width
            val height = size.height
            
            // Draw grid lines
            for (i in 0..4) {
                val y = height - (i * (height / 4))
                drawLine(
                    color = surfaceColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (results.isEmpty()) return@Canvas

            val stepX = width / (results.size.coerceAtLeast(2) - 1).toFloat()
            val path = Path()
            val points = mutableListOf<Offset>()

            results.forEachIndexed { index, result ->
                // Map SGPA (6.0 to 10.0) to height
                val normalizedY = ((result.sgpa - 6f) / 4f).coerceIn(0f, 1f)
                val x = index * stepX
                val y = height - (normalizedY * height)
                val point = Offset(x, y)
                points.add(point)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw line
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            )

            // Draw points
            points.forEach { point ->
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
fun SemesterSelector(
    semesters: List<SemesterResultEntity>,
    selectedSemester: SemesterResultEntity?,
    onSemesterSelected: (SemesterResultEntity) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        semesters.forEach { result ->
            val isSelected = result.id == selectedSemester?.id
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSemesterSelected(result) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Sem ${result.id}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SemesterSummaryCard(semester: SemesterResultEntity) {
    AcadMateCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSpacing.current.md),
        variant = CardVariant.Flat,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SGPA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = String.format("%.2f", semester.sgpa),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Credits Earned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${semester.credits}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "PASS",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun GpaSimulatorDialog(
    currentCgpa: Float,
    totalCredits: Int,
    onDismiss: () -> Unit
) {
    var targetSgpa by remember { mutableFloatStateOf(8.5f) }
    var upcomingCredits by remember { mutableIntStateOf(24) }

    val estimatedCgpa = remember(targetSgpa, upcomingCredits) {
        val currentPoints = currentCgpa * totalCredits
        val upcomingPoints = targetSgpa * upcomingCredits
        (currentPoints + upcomingPoints) / (totalCredits + upcomingCredits)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GPA Simulator", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Predict your new CGPA by simulating your performance in the current semester.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Column {
                    Text("Target SGPA: ${String.format("%.1f", targetSgpa)}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = targetSgpa,
                        onValueChange = { targetSgpa = it },
                        valueRange = 0f..10f,
                        steps = 100
                    )
                }

                Column {
                    Text("Current Credits: $upcomingCredits", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = upcomingCredits.toFloat(),
                        onValueChange = { upcomingCredits = it.toInt() },
                        valueRange = 12f..30f,
                        steps = 18
                    )
                }

                AcadMateCard(
                    variant = CardVariant.Flat,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Projected CGPA", fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%.2f", estimatedCgpa),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
fun SubjectGradeCard(subject: SubjectGradeEntity) {
    AcadMateCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSpacing.current.md),
        variant = CardVariant.Flat,
        backgroundColor = Color.White,
        cornerRadius = 12.dp,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grade Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = when(subject.grade.firstOrNull()) {
                            'A' -> Color(0xFFE8F5E9)
                            'B' -> Color(0xFFE3F2FD)
                            'C' -> Color(0xFFFFF3E0)
                            else -> Color(0xFFFFEBEE)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subject.grade,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = when(subject.grade.firstOrNull()) {
                        'A' -> Color(0xFF2E7D32)
                        'B' -> Color(0xFF1565C0)
                        'C' -> Color(0xFFE65100)
                        else -> Color(0xFFC62828)
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.subjectName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${subject.code} • ${subject.credits} Credits",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
