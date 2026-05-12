package com.acadmate.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acadmate.core.model.UserRole
import com.acadmate.designsystem.components.AcadMateButton

import com.acadmate.designsystem.components.AcadMateLogo
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import androidx.compose.ui.graphics.Color

import com.acadmate.designsystem.components.MeshBackground
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "RoleSelectionAlpha"
    )

    MeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .graphicsLayer(alpha = alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AcadMateLogo(
                iconSize = 100.dp,
                showText = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please select your role to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            RoleCard(
                title = "Student",
                description = "Access classes, marks, and AI tutor",
                icon = Icons.Default.Person,
                isSelected = selectedRole == UserRole.STUDENT,
                onClick = { selectedRole = UserRole.STUDENT }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Faculty",
                description = "Manage attendance and student marks",
                icon = Icons.Default.School,
                isSelected = selectedRole == UserRole.FACULTY,
                onClick = { selectedRole = UserRole.FACULTY }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Institution Admin",
                description = "Manage users and institution data",
                icon = Icons.Default.AdminPanelSettings,
                isSelected = selectedRole == UserRole.ADMIN,
                onClick = { selectedRole = UserRole.ADMIN }
            )

            Spacer(modifier = Modifier.weight(1f))

            AcadMateButton(
                text = "Continue",
                onClick = { selectedRole?.let { onRoleSelected(it) } },
                enabled = selectedRole != null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    AcadMateCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.Flat,
        backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
