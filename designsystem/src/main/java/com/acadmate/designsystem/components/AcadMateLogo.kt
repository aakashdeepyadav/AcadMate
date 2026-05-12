package com.acadmate.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.acadmate.designsystem.R
import com.acadmate.designsystem.theme.MintGreen

@Composable
fun AcadMateLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 80.dp,
    showText: Boolean = true,
    showTagline: Boolean = false,
    isDark: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo Icon using Coil for efficient memory management
        AsyncImage(
            model = R.drawable.acadmate,
            contentDescription = "AcadMate Logo",
            modifier = Modifier
                .size(iconSize)
                .padding(4.dp)
        )

        if (showText) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = if (isDark) Color.White else Color(0xFF2D3436),
                        fontWeight = FontWeight.Black
                    )) {
                        append("Acad")
                    }
                    withStyle(style = SpanStyle(
                        color = MintGreen,
                        fontWeight = FontWeight.Black
                    )) {
                        append("Mate")
                    }
                },
                style = MaterialTheme.typography.displaySmall.copy(
                    letterSpacing = (-1.5).sp,
                    fontSize = (iconSize.value * 0.45).sp
                )
            )

            if (showTagline) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Smart Academic Companion",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isDark) Color.White.copy(alpha = 0.6f)
                           else Color(0xFF64748B),
                    modifier = Modifier.alpha(if (isDark) 1f else 0.7f)
                )
            }
        }
    }
}
