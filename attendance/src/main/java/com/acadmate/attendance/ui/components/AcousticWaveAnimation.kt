package com.acadmate.attendance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AcousticWaveAnimation(modifier: Modifier = Modifier.size(200.dp)) {
    val infiniteTransition = rememberInfiniteTransition(label = "acoustic_waves")
    val waveColor = MaterialTheme.colorScheme.primary

    val animations = List(4) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, delayMillis = index * 500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_$index"
        )
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2

        animations.forEach { anim ->
            val radius = maxRadius * anim.value
            val alpha = 1f - anim.value
            
            drawCircle(
                color = waveColor,
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 4.dp.toPx()),
                alpha = alpha
            )
        }
    }
}
