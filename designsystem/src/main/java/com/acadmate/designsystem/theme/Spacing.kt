package com.acadmate.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale based on a 4px baseline grid.
 * All component spacing should use multiples of these values.
 */
data class Spacing(
    /** 4dp — Micro gaps, icon-to-text inline */
    val xs: Dp = 4.dp,
    /** 8dp — Tight internal padding, chip content */
    val sm: Dp = 8.dp,
    /** 12dp — Compact list item gaps */
    val smd: Dp = 12.dp,
    /** 16dp — Standard padding, card content, input fields */
    val md: Dp = 16.dp,
    /** 20dp — Card internal padding */
    val mdl: Dp = 20.dp,
    /** 24dp — Section spacing, generous gutter */
    val lg: Dp = 24.dp,
    /** 32dp — Large section gaps */
    val xl: Dp = 32.dp,
    /** 40dp — Page-level margins */
    val xxl: Dp = 40.dp,
    /** 48dp — Exterior margins, hero spacing */
    val xxxl: Dp = 48.dp
)
