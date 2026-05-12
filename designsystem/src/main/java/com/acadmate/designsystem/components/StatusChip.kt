package com.acadmate.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.designsystem.theme.AttendanceGreen
import com.acadmate.designsystem.theme.CriticalRed
import com.acadmate.designsystem.theme.WarningAmber

enum class AttendanceStatus {
    Attending,
    Absent,
    Pending
}

@Composable
fun StatusChip(
    status: AttendanceStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, dotColor, text) = when (status) {
        AttendanceStatus.Attending -> Triple(
            AttendanceGreen.copy(alpha = 0.1f),
            AttendanceGreen,
            "Attending"
        )
        AttendanceStatus.Absent -> Triple(
            CriticalRed.copy(alpha = 0.1f),
            CriticalRed,
            "Absent"
        )
        AttendanceStatus.Pending -> Triple(
            WarningAmber.copy(alpha = 0.1f),
            WarningAmber,
            "Pending"
        )
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "dotAlpha"
    )

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(8.dp)
        ) {
            drawCircle(
                color = dotColor.copy(alpha = animatedAlpha),
                radius = size.minDimension / 2
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = dotColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatusChipPreview() {
    AcadMateTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(status = AttendanceStatus.Attending)
            StatusChip(status = AttendanceStatus.Absent)
            StatusChip(status = AttendanceStatus.Pending)
        }
    }
}
