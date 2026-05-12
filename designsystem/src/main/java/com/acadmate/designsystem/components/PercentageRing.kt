package com.acadmate.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.designsystem.theme.AttendanceGreen
import com.acadmate.designsystem.theme.CriticalRed
import com.acadmate.designsystem.theme.WarningAmber

@Composable
fun PercentageRing(
    percentage: Float,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color? = null,
    backgroundColor: androidx.compose.ui.graphics.Color? = null,
    textColor: androidx.compose.ui.graphics.Color? = null
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "percentage"
    )

    // Color-coded: green ≥75%, amber 50-74%, red <50%
    val ringColor = color ?: when {
        percentage >= 75f -> AttendanceGreen
        percentage >= 50f -> WarningAmber
        else -> CriticalRed
    }

    val trackColor = backgroundColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Animated progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedPercentage * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = textColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PercentageRingPreview() {
    AcadMateTheme {
        PercentageRing(percentage = 78f)
    }
}

@Preview(showBackground = true)
@Composable
fun PercentageRingLowPreview() {
    AcadMateTheme {
        PercentageRing(percentage = 42f)
    }
}
