package com.acadmate.ai.exam

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultScreen(
    result: ExamResult,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val percentage = result.score.toFloat() / result.total
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quiz Finished!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))

        ScoreRing(percentage, result.score, result.total)

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                label = "Strong Topics",
                items = result.strongTopics,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                label = "Weak Topics",
                items = result.weakTopics,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Question Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(result.questions) { question ->
                ReviewItem(question)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Go Home")
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Retry Weak Topics")
            }
        }
    }
}

@Composable
fun ScoreRing(percentage: Float, score: Int, total: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = if (percentage > 0.7f) Color(0xFF4CAF50) else if (percentage > 0.4f) Color(0xFFFFC107) else Color(0xFFF44336),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "$score / $total",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun InfoCard(label: String, items: List<String>, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                Text("• $item", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ReviewItem(question: Question) {
    var expanded by remember { mutableStateOf(false) }
    val isCorrect = question.selectedIndex == question.correctIndex

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.alpha(0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Explanation:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(question.explanation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

