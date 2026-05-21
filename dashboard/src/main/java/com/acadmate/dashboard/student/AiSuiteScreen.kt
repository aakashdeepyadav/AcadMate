package com.acadmate.dashboard.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.MeshBackground
import com.acadmate.designsystem.components.SectionHeader
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.designsystem.theme.LocalSpacing
import com.acadmate.designsystem.theme.MintGreen
import com.acadmate.designsystem.theme.SoftBlue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer

data class AiFeature(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val gradientColors: List<Color>,
    val isEnabled: Boolean = true
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSuiteScreen(
    onFeatureClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "AiSuiteAlpha"
    )

    val aiFeatures = listOf(
        AiFeature(
            id = "tutor",
            title = "AI Tutor",
            description = "Get instant help with your studies, ask questions, and get personalized explanations",
            icon = Icons.Default.SmartToy,
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))
        ),
        AiFeature(
            id = "lecture_notes",
            title = "Lecture Notes",
            description = "Record lectures and get AI-generated structured notes with key points",
            icon = Icons.Default.Mic,
            gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399))
        ),
        AiFeature(
            id = "mock_exam",
            title = "Smart Quizzes",
            description = "Search for available faculty quizzes or generate practice MCQs for yourself",
            icon = Icons.Default.Quiz,
            gradientColors = listOf(Color(0xFFEF4444), Color(0xFFF87171))
        ),
        AiFeature(
            id = "interview_prep",
            title = "Tech Interview Prep",
            description = "Practice technical interviews with an AI bot tailored for CSE students",
            icon = Icons.Default.Code,
            gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF22D3EE))
        ),
        AiFeature(
            id = "study_planner",
            title = "Study Planner",
            description = "Get AI-generated study schedules based on your performance and deadlines",
            icon = Icons.Default.Schedule,
            gradientColors = listOf(Color(0xFFEC4899), Color(0xFFF472B6))
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Suite",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        MeshBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .graphicsLayer(alpha = alpha),
                contentPadding = PaddingValues(bottom = LocalSpacing.current.lg)
            ) {
            item {
                AiSuiteHeader()
                Spacer(modifier = Modifier.height(LocalSpacing.current.lg))
            }

            item {
                SectionHeader(
                    title = "AI Features",
                    modifier = Modifier.padding(horizontal = LocalSpacing.current.md)
                )
                Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
            }

            items(aiFeatures.chunked(2)) { rowFeatures ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LocalSpacing.current.md, vertical = 6.dp)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
                ) {
                    rowFeatures.forEach { feature ->
                        AiFeatureCard(
                            feature = feature,
                            onClick = { onFeatureClick(feature.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    if (rowFeatures.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(LocalSpacing.current.lg))
                AiStatsSection()
            }
        }
        }
    }
}

@Composable
fun AiSuiteHeader() {
    AcadMateCard(
        variant = CardVariant.Flat,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSpacing.current.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalSpacing.current.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(LocalSpacing.current.md))

            Text(
                text = "Your AI Study Companion",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Powered by advanced AI to enhance your learning experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AiFeatureCard(
    feature: AiFeature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AcadMateCard(
        variant = CardVariant.Flat,
        modifier = modifier.heightIn(min = 180.dp),
        onClick = if (feature.isEnabled) onClick else null,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(feature.gradientColors.first().copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = feature.gradientColors.first(),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp,
                        letterSpacing = 0.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!feature.isEnabled) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "COMING SOON",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun AiStatsSection() {
    AcadMateCard(
        variant = CardVariant.Flat,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSpacing.current.md),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
        ) {
            Text(
                text = "Your AI Learning Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = "12",
                    label = "Sessions",
                    icon = Icons.Default.PlayArrow
                )
                StatColumn(
                    value = "45",
                    label = "Questions",
                    icon = Icons.Default.QuestionAnswer
                )
                StatColumn(
                    value = "8.2h",
                    label = "Study Time",
                    icon = Icons.Default.Schedule
                )
            }
        }
    }
}

@Composable
fun StatColumn(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = LocalSpacing.current.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
