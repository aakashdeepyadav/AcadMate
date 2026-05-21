package com.acadmate.ai.exam

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import java.util.Locale
import com.acadmate.ai.exam.MockExamViewModel
import com.acadmate.ai.exam.Question
import com.acadmate.ai.exam.ExamUiState
import com.acadmate.ai.exam.ExamResult
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExamScreen(
    viewModel: MockExamViewModel,
    questions: List<Question>,
    currentIndex: Int,
    timeLeft: Int
) {
    val question = questions[currentIndex]
    val canGoNext = currentIndex < questions.size - 1
    val canSubmit = currentIndex == questions.size - 1

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Top Progress and Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Question ${currentIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / questions.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                TimerDisplay(timeLeft)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    text = question.text,
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Options
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                question.options.forEachIndexed { index, option ->
                    OptionCard(
                        text = option,
                        isSelected = question.selectedIndex == index,
                        onClick = { viewModel.selectOption(currentIndex, index) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.previousQuestion() },
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Previous")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = { if (canSubmit) viewModel.submitExam() else viewModel.nextQuestion() },
                    enabled = question.selectedIndex != null,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(if (canSubmit) "Submit Quiz" else "Next")
                }
            }
        }
    }
}

@Composable
fun TimerDisplay(seconds: Int) {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    
    val color = when {
        seconds < 10 -> Color.Red
        seconds < 30 -> Color(0xFFFFA500)
        else -> MaterialTheme.colorScheme.primary
    }

    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (seconds < 10) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = timeStr,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OptionCard(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun Modifier.alphaModifier(value: Float) = this.alpha(value)
