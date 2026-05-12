package com.acadmate.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Soft-Geometric shape language.
 * - extraSmall (4dp): Chips, small badges, input fields
 * - small (8dp): Standard containers, cards
 * - medium (16dp): Large cards, buttons, modals
 * - large (20dp): Quick action tiles, elevated cards
 * - extraLarge (28dp): Hero cards, prominent surfaces
 */
val AcadMateShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
