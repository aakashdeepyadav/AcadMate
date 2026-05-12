package com.acadmate.attendance.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ProgressIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                StepDot(
                    index = index,
                    currentStep = currentStep,
                    modifier = Modifier.weight(1f)
                )

                if (index < totalSteps - 1) {
                    StepConnector(
                        isCompleted = index < currentStep - 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StepDot(index: Int, currentStep: Int, modifier: Modifier = Modifier) {
    val isCompleted = index < currentStep
    val isCurrent = index == currentStep - 1

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primary
            isCurrent -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "dotColor"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .size(32.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StepConnector(isCompleted: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .padding(horizontal = 2.dp)
            .background(
                if (isCompleted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    )
}

@Composable
fun BleRadarAnimation(isScanning: Boolean, modifier: Modifier = Modifier) {
    val rotation1 by animateFloatAsState(
        targetValue = if (isScanning) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar1"
    )

    val rotation2 by animateFloatAsState(
        targetValue = if (isScanning) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar2"
    )

    val pulseAlpha by animateFloatAsState(
        targetValue = if (isScanning) 0.3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(180.dp)
                .rotate(rotation1)
                .background(Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )

        // Middle ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .rotate(-rotation2)
                .background(Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )

        // Inner pulsing ring
        Box(
            modifier = Modifier
                .size(60.dp)
                .alpha(pulseAlpha)
                .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
        )

        // Center dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@Composable
fun StatusIcon(status: StatusType, modifier: Modifier = Modifier) {
    val color = when (status) {
        StatusType.Success -> MaterialTheme.colorScheme.primary
        StatusType.Error -> MaterialTheme.colorScheme.error
        StatusType.Pending -> MaterialTheme.colorScheme.tertiary
    }

    val icon = when (status) {
        StatusType.Success -> Icons.Filled.Check
        StatusType.Error -> Icons.Filled.Close
        StatusType.Pending -> Icons.Filled.Check  // Placeholder
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = modifier.size(24.dp),
        tint = color
    )
}

enum class StatusType {
    Success, Error, Pending
}

@Composable
fun FaceOvalOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Dim background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Oval cutout
        Box(
            modifier = Modifier
                .size(width = 250.dp, height = 350.dp)
                .background(
                    Color.Transparent,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                )
                .border(2.dp, MaterialTheme.colorScheme.primary)
        )

        // Landmark dots (corners)
        val dotsPositions = listOf(
            Modifier
                .align(Alignment.TopStart)
                .padding(40.dp),
            Modifier
                .align(Alignment.TopEnd)
                .padding(40.dp),
            Modifier
                .align(Alignment.BottomStart)
                .padding(40.dp),
            Modifier
                .align(Alignment.BottomEnd)
                .padding(40.dp)
        )

        dotsPositions.forEach { position ->
            Box(
                modifier = position
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
fun AnimatedMapPinDrop(isDropping: Boolean, modifier: Modifier = Modifier) {
    val translateY by animateFloatAsState(
        targetValue = if (isDropping) 0f else -200f,
        animationSpec = tween(durationMillis = 1500),
        label = "pinDrop"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Campus boundary indicator
        Box(
            modifier = Modifier
                .size(200.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )

        // Dropping pin
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Location",
            modifier = Modifier
                .size(40.dp)
                .padding(top = translateY.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AttendanceStatusBanner(
    message: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer
            )
            .padding(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun LowAttendanceWarning(attendancePercentage: Float, modifier: Modifier = Modifier) {
    if (attendancePercentage < 75f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFFFE082),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = "⚠️ Low attendance: ${String.format("%.1f", attendancePercentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5B00)
            )
        }
    }
}
