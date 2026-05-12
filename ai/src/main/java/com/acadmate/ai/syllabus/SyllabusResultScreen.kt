package com.acadmate.ai.syllabus

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.acadmate.ai.syllabus.UnitItem
import com.acadmate.ai.syllabus.Flashcard
import com.acadmate.ai.syllabus.Mcq
import com.acadmate.ai.syllabus.SyllabusResult
import com.acadmate.designsystem.theme.rememberAcadMateHapticFeedback
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusResultScreen(
    result: SyllabusResult,
    onBackClick: () -> Unit,
    onChatClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Topics", "Gaps", "Flashcards", "MCQs")
    val haptic = rememberAcadMateHapticFeedback()

    LaunchedEffect(selectedTab) {
        haptic.success()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Syllabus Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onChatClick,
                icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                text = { Text("Study with AI Tutor") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> SummaryTab(result.summary)
                    1 -> TopicsTab(result.units)
                    2 -> GapAnalysisTab(result.gapAnalysis)
                    3 -> FlashcardTab(result.flashcards)
                    4 -> McqsTab(result.mcqs)
                }
            }
        }
    }
}

@Composable
fun GapAnalysisTab(gaps: List<GapAnalysisItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Syllabus Gap Radar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Cross-referencing your lecture notes with the syllabus to find missing topics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(gaps) { gap ->
            GapItemCard(gap)
        }
    }
}

@Composable
fun GapItemCard(gap: GapAnalysisItem) {
    val color = when (gap.status) {
        "Covered" -> Color(0xFF4CAF50)
        "Partial" -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(gap.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = color,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        gap.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { gap.coverage },
                modifier = Modifier.fillMaxWidth().clip(CircleShape).height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
            
            Text(
                "${(gap.coverage * 100).toInt()}% match with lectures",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SummaryTab(markdown: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        MarkdownText(
            markdown = markdown,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TopicsTab(units: List<UnitItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(units) { unit ->
            UnitExpandableCard(unit)
        }
    }
}

@Composable
fun UnitExpandableCard(unit: UnitItem) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(unit.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp, start = 8.dp)) {
                    unit.chapters.forEach { chapter ->
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        chapter.concepts.forEach { concept ->
                            Row(
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(concept, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardTab(flashcards: List<Flashcard>) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val card = flashcards.getOrNull(currentIndex)
    val haptic = rememberAcadMateHapticFeedback()

    if (card == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("All cards reviewed!", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { currentIndex = 0 }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Restart")
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FlashcardView(
            card = card,
            onSwipe = { success ->
                if (success) haptic.success() else haptic.heavy()
                currentIndex++
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LinearProgressIndicator(
            progress = { (currentIndex.toFloat() / flashcards.size) },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(CircleShape)
        )
        Text(
            "Progress: $currentIndex / ${flashcards.size}",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun FlashcardView(card: Flashcard, onSwipe: (Boolean) -> Unit) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500)
    )

    Card(
        modifier = Modifier
            .size(300.dp, 400.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { isFlipped = !isFlipped },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                Text(
                    card.question,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                Text(
                    card.answer,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(24.dp)
                        .graphicsLayer { rotationY = 180f }
                )
            }
        }
    }
    
    Row(
        modifier = Modifier.padding(top = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        IconButton(
            onClick = { onSwipe(false) },
            modifier = Modifier.size(64.dp).background(Color.Red.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Review Again", tint = Color.Red)
        }
        IconButton(
            onClick = { onSwipe(true) },
            modifier = Modifier.size(64.dp).background(Color.Green.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Got it", tint = Color.Green)
        }
    }
}

@Composable
fun McqsTab(mcqs: List<Mcq>) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    if (showResult) {
        McqResultView(score = score, total = mcqs.size) {
            showResult = false
            currentQuestionIndex = 0
            score = 0
        }
        return
    }

    val question = mcqs[currentQuestionIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            "Question ${currentQuestionIndex + 1} of ${mcqs.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            question.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        question.options.forEachIndexed { index, option ->
            val haptic = rememberAcadMateHapticFeedback()
            OutlinedButton(
                onClick = {
                    if (index == question.correctAnswerIndex) {
                        score++
                        haptic.success()
                    } else {
                        haptic.error()
                    }
                    if (currentQuestionIndex < mcqs.size - 1) {
                        currentQuestionIndex++
                    } else {
                        showResult = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    option,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
fun McqResultView(score: Int, total: Int, onRestart: () -> Unit) {
    // val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.acadmate.ai.R.raw.confetti))
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /*LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(200.dp)
        )*/
        
        Text("Quiz Completed!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your Score: $score / $total",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
        
        Button(onClick = onRestart, modifier = Modifier.padding(top = 32.dp)) {
            Text("Retake Quiz")
        }
    }
}

// Add these to make the code compile (missing imports/utils)
