package com.acadmate.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadmate.core.model.UserRole

@Composable
fun AcadMateBottomNavigation(
    currentDestination: String,
    userRole: UserRole,
    onNavigate: (Any) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        val items = when (userRole) {
            UserRole.STUDENT -> listOf(
                BottomNavItem("Home", Icons.Default.Home, Routes.Home, "home"),
                BottomNavItem("Attendance", Icons.Default.CheckCircle, Routes.Attendance, "attendance"),
                BottomNavItem("Schedule", Icons.Default.Event, Routes.Timetable, "schedule"),
                BottomNavItem("AI Suite", Icons.Default.AutoAwesome, Routes.AiSuite, "ai_suite"),
                BottomNavItem("Profile", Icons.Default.Person, Routes.Profile, "profile")
            )
            UserRole.FACULTY -> listOf(
                BottomNavItem("Home", Icons.Default.Home, Routes.Home, "home"),
                BottomNavItem("Attendance", Icons.Default.CheckCircle, Routes.Attendance, "attendance"),
                BottomNavItem("Materials", Icons.Default.LibraryBooks, Routes.ResourceManager, "materials"),
                BottomNavItem("Tasks", Icons.AutoMirrored.Filled.Assignment, Routes.AssignmentGraph, "tasks"),
                BottomNavItem("Profile", Icons.Default.Person, Routes.Profile, "profile")
            )
            UserRole.ADMIN -> listOf(
                BottomNavItem("Home", Icons.Default.Home, Routes.Home, "home"),
                BottomNavItem("Users", Icons.Default.People, Routes.CreateUser, "users"),
                BottomNavItem("Courses", Icons.Default.Class, Routes.CourseManagement, "courses"),
                BottomNavItem("Profile", Icons.Default.Person, Routes.Profile, "profile")
            )
        }

        items.forEach { item ->
            val isSelected = currentDestination == item.routeId
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(24.dp)) },
                label = {
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: Any,
    val routeId: String
)
