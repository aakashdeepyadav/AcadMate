package com.acadmate.designsystem.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun MeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MeshBackground")
    val offset1 by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1500f,
        targetValue = -500f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    // Using a combination of the background color and brand colors with very low alpha
    val backgroundColor = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
    val tertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .background(
                Brush.radialGradient(
                    colors = listOf(primary, Color.Transparent),
                    center = Offset(offset1, offset2 * 0.5f),
                    radius = 1800f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(secondary, Color.Transparent),
                    center = Offset(offset2, offset1),
                    radius = 1500f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(tertiary, Color.Transparent),
                    center = Offset(offset1 * 0.8f, offset2 * 1.2f),
                    radius = 2000f
                )
            )
    ) {
        content()
    }
}
