package com.acadmate.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.designsystem.theme.CriticalRed
import com.acadmate.designsystem.theme.GradientPrimary

enum class ButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Danger
}

@Composable
fun AcadMateButton(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Primary,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val buttonShape = RoundedCornerShape(16.dp)
    
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ButtonPressScale"
    )

    val content: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = when (variant) {
                        ButtonVariant.Ghost -> MaterialTheme.colorScheme.primary
                        ButtonVariant.Secondary -> MaterialTheme.colorScheme.primary
                        else -> Color.White
                    },
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }

    val buttonModifier = modifier
        .height(52.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (variant == ButtonVariant.Primary && enabled && !loading) {
                Modifier.shadow(
                    elevation = 6.dp, // Added back some elevation for the glow
                    shape = buttonShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            } else Modifier
        )

    when (variant) {
        ButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = buttonShape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            enabled = enabled && !loading,
            interactionSource = interactionSource
        ) {
            content()
        }
        ButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ),
            modifier = buttonModifier,
            shape = buttonShape,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            enabled = enabled && !loading,
            interactionSource = interactionSource
        ) {
            content()
        }
        ButtonVariant.Danger -> Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = CriticalRed.copy(alpha = 0.1f),
                contentColor = CriticalRed
            ),
            modifier = buttonModifier,
            shape = buttonShape,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            enabled = enabled && !loading,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            interactionSource = interactionSource
        ) {
            content()
        }
        else -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            enabled = enabled && !loading,
            interactionSource = interactionSource
        ) {
            content()
        }
    }
}

