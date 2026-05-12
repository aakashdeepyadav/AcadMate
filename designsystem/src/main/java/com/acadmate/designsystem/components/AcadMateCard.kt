package com.acadmate.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acadmate.designsystem.theme.AcadMateTheme

enum class CardVariant {
    Elevated,
    Outlined,
    Glass,
    Gradient,
    Flat // New variant for fintech-style grouping
}

@Composable
fun AcadMateCard(
    variant: CardVariant = CardVariant.Flat,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    gradientColors: List<Color>? = null,
    cornerRadius: Dp = 16.dp, // Reduced default
    contentPadding: Dp = 16.dp, // Reduced default
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(cornerRadius)
    val primaryColor = MaterialTheme.colorScheme.primary

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CardPressScale"
    )

    val clickModifier = if (onClick != null) {
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    } else {
        Modifier
    }

    val borderModifier = if (borderColor != null) {
        Modifier.border(borderWidth, borderColor, cardShape)
    } else {
        Modifier
    }

    when (variant) {
        CardVariant.Flat -> Box(
            modifier = modifier
                .then(clickModifier)
                .clip(cardShape)
                .then(borderModifier)
                .background(
                    color = backgroundColor ?: MaterialTheme.colorScheme.surfaceContainer
                )
                .padding(contentPadding)
        ) {
            content()
        }
        CardVariant.Elevated -> Card(
            modifier = modifier
                .shadow(
                    elevation = 2.dp, // Reduced from 4dp for "flattening"
                    shape = cardShape,
                    ambientColor = Color.Black.copy(alpha = 0.02f),
                    spotColor = Color.Black.copy(alpha = 0.04f)
                )
                .then(clickModifier),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
                    ?: MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
        CardVariant.Outlined -> OutlinedCard(
            modifier = modifier.then(clickModifier),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor ?: Color.Transparent
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
        CardVariant.Glass -> Box(
            modifier = modifier
                .then(clickModifier)
                .clip(cardShape)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    ),
                    shape = cardShape
                )
                .padding(contentPadding)
        ) {
            content()
        }
        CardVariant.Gradient -> {
            val colors = gradientColors ?: listOf(
                primaryColor,
                MaterialTheme.colorScheme.secondary
            )
            Box(
                modifier = modifier
                    .shadow(
                        elevation = 4.dp, // Reduced from 8dp
                        shape = cardShape,
                        ambientColor = colors.first().copy(alpha = 0.1f),
                        spotColor = colors.first().copy(alpha = 0.15f)
                    )
                    .then(clickModifier)
                    .clip(cardShape)
                    .background(brush = Brush.linearGradient(colors))
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AcadMateCardPreview() {
    AcadMateTheme {
        AcadMateCard {
            Column {
                AcadMateButton(text = "Action", onClick = {})
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AcadMateCardVariantsPreview() {
    AcadMateTheme {
        Column {
            AcadMateCard(variant = CardVariant.Elevated) {
                AcadMateButton(text = "Elevated", onClick = {})
            }
            AcadMateCard(variant = CardVariant.Outlined) {
                AcadMateButton(text = "Outlined", onClick = {})
            }
            AcadMateCard(variant = CardVariant.Glass) {
                AcadMateButton(text = "Glass", onClick = {})
            }
        }
    }
}
