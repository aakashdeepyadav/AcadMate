package com.acadmate.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.auth.ui.AuthViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.acadmate.ai.exam.MockExamContainer
import androidx.navigation.navDeepLink
import com.acadmate.ai.syllabus.SyllabusUploadScreen
import com.acadmate.ai.syllabus.SyllabusResultScreen
import com.acadmate.ai.syllabus.SyllabusBrowserScreen
import com.acadmate.ai.syllabus.SyllabusAiViewModel
import com.acadmate.ai.syllabus.SyllabusUiState
import com.acadmate.ai.tutor.AiTutorScreen
import com.acadmate.attendance.ui.screens.AttendanceSecurityWrapper
import com.acadmate.attendance.ui.screens.AttendanceHistoryScreen
import com.acadmate.attendance.ui.screens.FacultyMarkAttendanceScreen
import com.acadmate.attendance.ui.screens.FacultySessionAttendanceScreen
import com.acadmate.attendance.ui.screens.MarkAttendanceScreen
import com.acadmate.dashboard.faculty.FacultyHomeScreen
import com.acadmate.dashboard.faculty.FacultyTimetableScreen
import com.acadmate.dashboard.student.StudentHomeScreen
import com.acadmate.dashboard.student.AiSuiteScreen
import com.acadmate.dashboard.student.TimetableScreen
import com.acadmate.dashboard.student.ProfileScreen
import com.acadmate.dashboard.student.ManageNotificationsScreen
import com.acadmate.dashboard.student.LeaveApplicationScreen
import com.acadmate.dashboard.student.LeaveViewModel
import com.acadmate.dashboard.faculty.LeaveManagementScreen
import com.acadmate.auth.ui.ProfileSetupScreen
import com.acadmate.auth.ui.LoginScreen
import com.acadmate.auth.ui.OtpScreen
import com.acadmate.auth.ui.RegistrationScreen
import com.acadmate.auth.ui.RegistrationViewModel
import com.acadmate.ui.onboarding.OnboardingScreen
import com.acadmate.ui.onboarding.RoleSelectionScreen
import com.acadmate.ui.onboarding.OnboardingViewModel
import com.acadmate.core.model.UserRole
import com.acadmate.ui.splash.SplashScreen
import com.acadmate.admin.ui.AdminDashboardScreen
import com.acadmate.dashboard.faculty.SyllabusManagementScreen
import com.acadmate.admin.ui.CreateUserScreen
import com.acadmate.admin.ui.CampusSetupScreen
import com.acadmate.assignments.ui.AssignmentListScreen
import com.acadmate.assignments.ui.CreateAssignmentScreen
import com.acadmate.assignments.ui.AssignmentViewModel
import com.acadmate.ai.focus.FocusModeScreen
import com.acadmate.ai.lecture.LectureNotesScreen
import com.acadmate.ai.doubt.DoubtMarketplaceScreen
import com.acadmate.ai.notice.NoticeBoardScreen
import com.acadmate.dashboard.student.ResultsScreen
import com.acadmate.assignments.ui.GradebookScreen
import com.acadmate.assignments.ui.ResourceManagerScreen
import com.acadmate.admin.ui.CourseManagementScreen
import com.acadmate.admin.ui.AuditLogScreen
import com.acadmate.admin.ui.TimetableManagementScreen
import com.acadmate.admin.ui.AiTimetableGeneratorScreen
import com.acadmate.assignments.ui.AssignmentDetailScreen

import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.auth.ui.AppLockScreen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

@Composable
fun SimplePlaceholderScreen(
    title: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "$title screen is coming soon!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MainShell(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val dataStore = onboardingViewModel.onboardingDataStore
    
    val authViewModel: AuthViewModel = hiltViewModel()
    val attendanceViewModel: com.acadmate.attendance.domain.AttendanceViewModel = hiltViewModel()
    
    // App Lock State
    var isAppUnlocked by remember { mutableStateOf(false) }
    val isLockEnabled by dataStore.isAppLockEnabled.collectAsStateWithLifecycle(initialValue = false)
    val isLoggedIn = authViewModel.isUserLoggedIn()

    if (isLoggedIn && isLockEnabled && !isAppUnlocked) {
        AppLockScreen(
            onUnlockSuccess = { isAppUnlocked = true },
            onSignOut = {
                authViewModel.signOut()
                navController.navigate(Routes.AuthGraph) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
            dataStore = dataStore
        )
    } else {
        val role by attendanceViewModel.userRole.collectAsStateWithLifecycle()

        // Show bottom bar only for top-level destinations within the functional graphs
        val showBottomBar = currentDestination?.hierarchy?.any { 
            it.route?.contains("MainGraph") == true ||
            it.route?.contains("AdminGraph") == true ||
            it.route?.contains("AssignmentGraph") == true
        } == true && currentDestination?.hierarchy?.any { 
            it.route?.contains("AuthGraph") == true ||
            it.route?.contains("Splash") == true 
        } != true

        Scaffold(
            bottomBar = {
                if (showBottomBar && role != null) {
                    AcadMateBottomNavigation(
                        currentDestination = when {
                            currentDestination?.hasRoute(Routes.Home::class) == true -> "home"
                            currentDestination?.hasRoute(Routes.Attendance::class) == true -> "attendance"
                            currentDestination?.hasRoute(Routes.AiSuite::class) == true -> "ai_suite"
                            currentDestination?.hasRoute(Routes.Timetable::class) == true -> "schedule"
                            currentDestination?.hasRoute(Routes.Profile::class) == true -> "profile"
                            currentDestination?.hasRoute(Routes.AssignmentGraph::class) == true -> "tasks"
                            currentDestination?.hasRoute(Routes.ResourceManager::class) == true -> "materials"
                            currentDestination?.hasRoute(Routes.CreateUser::class) == true -> "users"
                            currentDestination?.hasRoute(Routes.CourseManagement::class) == true -> "courses"
                            else -> "home"
                        },
                        userRole = role!!,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            content(padding)
        }
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: Any = Routes.Splash
) {
    MainShell(navController) { padding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            composable<Routes.Splash> {
                SplashScreen(
                    onNavigateToOnboarding = {
                        navController.navigate(Routes.AuthGraph) {
                            popUpTo<Routes.Splash> { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Routes.MainGraph) {
                            popUpTo<Routes.Splash> { inclusive = true }
                        }
                    },
                    onNavigateToAdmin = {
                        navController.navigate(Routes.AdminGraph) {
                            popUpTo<Routes.Splash> { inclusive = true }
                        }
                    }
                )
            }

            // Auth Graph
            navigation<Routes.AuthGraph>(startDestination = Routes.Onboarding) {
                composable<Routes.Onboarding> {
                    OnboardingScreen(
                        onGetStarted = {
                            navController.navigate(Routes.RoleSelection) {
                                popUpTo<Routes.Onboarding> { inclusive = true }
                            }
                        },
                        onSkip = {
                            navController.navigate(Routes.RoleSelection) {
                                popUpTo<Routes.Onboarding> { inclusive = true }
                            }
                        }
                    )
                }
                composable<Routes.RoleSelection> {
                    val viewModel: OnboardingViewModel = hiltViewModel()
                    RoleSelectionScreen(
                        onRoleSelected = { role ->
                            viewModel.saveRole(role)
                            navController.navigate(Routes.Login)
                        }
                    )
                }
                composable<Routes.Login> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        try {
                            navController.getBackStackEntry(Routes.AuthGraph)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    
                    val viewModel: AuthViewModel = if (parentEntry != null) {
                        hiltViewModel(parentEntry)
                    } else {
                        hiltViewModel()
                    }

                    LoginScreen(
                        viewModel = viewModel,
                        onOtpSent = { phoneNumber ->
                            navController.navigate(Routes.Otp(phoneNumber))
                        },
                        onLoginSuccess = { _ ->
                            navController.navigate(Routes.MainGraph) {
                                popUpTo(Routes.AuthGraph) { inclusive = true }
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable<Routes.Otp> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.AuthGraph)
                    }
                    val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                    val route: Routes.Otp = backStackEntry.toRoute()

                    OtpScreen(
                        phoneNumber = route.phoneNumber,
                        viewModel = viewModel,
                        onVerificationSuccess = {
                            navController.navigate(Routes.MainGraph) {
                                popUpTo(Routes.AuthGraph) { inclusive = true }
                            }
                        },
                        onBackToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
                composable<Routes.Register>(
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) }
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.AuthGraph)
                    }
                    val viewModel: RegistrationViewModel = hiltViewModel(parentEntry)
                    RegistrationScreen(
                        viewModel = viewModel,
                        onRegistrationComplete = { phoneNumber: String ->
                            navController.navigate(Routes.RegisterOtp(phoneNumber)) {
                                popUpTo<Routes.Login> { inclusive = false }
                            }
                        },
                        onBackToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
                composable<Routes.RegisterOtp> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.AuthGraph)
                    }
                    val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                    val route: Routes.RegisterOtp = backStackEntry.toRoute()
                    OtpScreen(
                        phoneNumber = route.phoneNumber,
                        viewModel = viewModel,
                        onVerificationSuccess = {
                            navController.navigate(Routes.RegisterProfile(route.phoneNumber, "verified"))
                        },
                        onBackToLogin = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<Routes.RegisterProfile> { backStackEntry ->
                    val route: Routes.RegisterProfile = backStackEntry.toRoute()
                    val viewModel: RegistrationViewModel = hiltViewModel()
                    ProfileSetupScreen(
                        phoneNumber = route.phoneNumber,
                        viewModel = viewModel,
                        onComplete = {
                            navController.navigate(Routes.MainGraph) {
                                popUpTo<Routes.AuthGraph> { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Main Graph
            navigation<Routes.MainGraph>(startDestination = Routes.Home) {
                composable<Routes.Home>(
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                    deepLinks = listOf(
                        navDeepLink<Routes.Home>(basePath = "acadmate://home")
                    )
                ) {
                    val attendanceViewModel: com.acadmate.attendance.domain.AttendanceViewModel = hiltViewModel()
                    val role by attendanceViewModel.userRole.collectAsStateWithLifecycle()

                    when (role) {
                        UserRole.ADMIN -> {
                            val authViewModel: AuthViewModel = hiltViewModel()
                            AdminDashboardScreen(
                                onAddUserClick = { navController.navigate(Routes.CreateUser) },
                                onManageCoursesClick = { navController.navigate(Routes.CourseManagement) },
                                onScheduleClick = { navController.navigate(Routes.TimetableManagement) },
                                onAiScheduleClick = { navController.navigate(Routes.AiTimetableGenerator) },
                                onAuditLogClick = { navController.navigate(Routes.AuditLog) },
                                onSettingsClick = { navController.navigate(Routes.CampusSetup) },
                                onSignOut = {
                                    authViewModel.signOut()
                                    navController.navigate(Routes.AuthGraph) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            )
                        }
                        UserRole.FACULTY -> {
                            FacultyHomeScreen(
                                onActionClick = { action, classId ->
                                    when (action) {
                                        "Mark Attendance" -> navController.navigate(Routes.FacultyMarkAttendance(classId ?: "General"))
                                        "Post Assignment" -> navController.navigate(Routes.CreateAssignment)
                                        "Notice Board" -> navController.navigate(Routes.NoticeBoard)
                                        "Materials" -> navController.navigate(Routes.ResourceManager)
                                        "Leave Management" -> navController.navigate(Routes.LeaveManagement)
                                        "Timetable" -> navController.navigate(Routes.Timetable)
                                        "View Attendance" -> navController.navigate(Routes.Attendance)
                                        "Gradebook" -> navController.navigate(Routes.Gradebook)
                                        "Manage Syllabus" -> navController.navigate(Routes.ManageSyllabus)
                                    }
                                },
                                onClassClick = { classId, hour ->
                                    navController.navigate(Routes.FacultyAttendance(classId = classId, hour = hour))
                                },
                                onProfileClick = { navController.navigate(Routes.Profile) }
                            )
                        }
                        UserRole.STUDENT -> {
                            val studentHomeViewModel: com.acadmate.dashboard.student.StudentHomeViewModel = hiltViewModel()
                            val studentUiState by studentHomeViewModel.uiState.collectAsStateWithLifecycle()
                            
                            StudentHomeScreen(
                                viewModel = studentHomeViewModel,
                                onActionClick = { action ->
                                    when (action) {
                                        "AI Tutor" -> navController.navigate(Routes.AiChat())
                                        "Syllabus" -> navController.navigate(Routes.Syllabus)
                                        "Lecture" -> navController.navigate(Routes.LectureNotes)
                                        "Doubt" -> navController.navigate(Routes.DoubtMarketplace)
                                        "Focus" -> navController.navigate(Routes.FocusMode)
                                        "Mark Attendance" -> navController.navigate(Routes.ActiveSessionsBrowser)
                                        "Assignments" -> navController.navigate(Routes.AssignmentGraph)
                                        "Materials" -> navController.navigate(Routes.ResourceManager)
                                        "Attendance" -> navController.navigate(Routes.Attendance)
                                        "Timetable" -> navController.navigate(Routes.Timetable)
                                        "Leave" -> navController.navigate(Routes.LeaveApplication)
                                        "Results" -> navController.navigate(Routes.Results)
                                        "Notice Board" -> navController.navigate(Routes.NoticeBoard)
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(Routes.Profile)
                                }
                            )
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }

                composable<Routes.MarkAttendance>(
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                    deepLinks = listOf(
                        navDeepLink<Routes.MarkAttendance>(basePath = "acadmate://attendance/mark")
                    )
                ) { backStackEntry ->
                    val route: Routes.MarkAttendance = backStackEntry.toRoute()
                    AttendanceSecurityWrapper(
                        classId = route.classId,
                        onNavigateBack = { navController.popBackStack() },
                        onGoToProfile = { navController.navigate(Routes.Profile) }
                    )
                }

                composable<Routes.ActiveSessionsBrowser> {
                    com.acadmate.attendance.ui.screens.ActiveSessionsBrowserScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSessionClick = { classId -> 
                            navController.navigate(Routes.MarkAttendance(classId)) 
                        }
                    )
                }

                composable<Routes.FacultyAttendance> { backStackEntry ->
                    val route: Routes.FacultyAttendance = backStackEntry.toRoute()
                    FacultySessionAttendanceScreen(
                        classId = route.classId,
                        hour = route.hour,
                        date = route.date,
                        viewModel = hiltViewModel(),
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Routes.FacultyMarkAttendance> { backStackEntry ->
                    val route: Routes.FacultyMarkAttendance = backStackEntry.toRoute()
                    FacultyMarkAttendanceScreen(
                        classId = route.classId,
                        onBackClick = { navController.popBackStack() },
                        onViewAttendanceClick = { classId ->
                            navController.navigate(Routes.FacultyAttendance(classId = classId))
                        }
                    )
                }

                composable<Routes.QrScanner> {
                    MarkAttendanceScreen(
                        viewModel = hiltViewModel(),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<Routes.AiSuite>(
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() }
                ) {
                    AiSuiteScreen(
                        onFeatureClick = { featureId ->
                            when (featureId) {
                                "tutor" -> navController.navigate(Routes.AiChat())
                                "lecture_notes" -> navController.navigate(Routes.LectureNotes)
                                "mock_exam" -> navController.navigate(Routes.MockExamSetup)
                                "interview_prep" -> navController.navigate(Routes.AiChat())
                                "study_planner" -> navController.navigate(Routes.AiChat())
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<Routes.LectureNotes> {
                    com.acadmate.ai.lecture.LectureNotesScreen(onBackClick = { navController.popBackStack() })
                }

                composable<Routes.NoticeBoard> {
                    NoticeBoardScreen(onBackClick = { navController.popBackStack() })
                }

                // AI Features (Detail screens slide up)
                composable<Routes.AiChat>(
                    enterTransition = { slideInVertically(initialOffsetY = { it }) },
                    exitTransition = { slideOutVertically(targetOffsetY = { it }) },
                    deepLinks = listOf(
                        navDeepLink<Routes.AiChat>(basePath = "acadmate://ai/chat")
                    )
                ) { _ ->
                    AiTutorScreen(
                        viewModel = hiltViewModel(),
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Routes.Syllabus>(
                    enterTransition = { slideInVertically(initialOffsetY = { it }) }
                ) {
                    SyllabusBrowserScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Routes.ManageSyllabus> {
                    com.acadmate.dashboard.faculty.SyllabusManagementScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Routes.MockExamSetup>(
                    enterTransition = { slideInVertically(initialOffsetY = { it }) }
                ) {
                    MockExamContainer(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Routes.Attendance>(
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() }
                ) {
                    AttendanceHistoryScreen(
                        viewModel = hiltViewModel()
                    )
                }

                composable<Routes.Timetable> {
                    val onboardingDataStore: OnboardingDataStore = hiltViewModel<com.acadmate.dashboard.student.TimetableViewModel>().onboardingDataStore
                    val role by onboardingDataStore.selectedRole.collectAsState(initial = null)
                    
                    if (role == UserRole.FACULTY) {
                        FacultyTimetableScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    } else {
                        TimetableScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }

                composable<Routes.LeaveApplication> {
                    LeaveApplicationScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Routes.LeaveManagement> {
                    LeaveManagementScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Routes.Results> {
                    ResultsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<Routes.Profile> {
                val authViewModel: AuthViewModel = hiltViewModel()
                val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                val attendanceViewModel: com.acadmate.attendance.domain.AttendanceViewModel = hiltViewModel()
                
                val role by attendanceViewModel.userRole.collectAsStateWithLifecycle()
                val isDarkModeStored by onboardingViewModel.onboardingDataStore.isDarkMode.collectAsState(initial = null)
                val isDarkMode = isDarkModeStored ?: androidx.compose.foundation.isSystemInDarkTheme()

                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Routes.AuthGraph) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onNotificationsClick = { navController.navigate(Routes.Notifications) },
                    onPrivacyClick = { navController.navigate(Routes.Privacy) },
                    onHelpClick = { navController.navigate(Routes.Help) },
                    onAboutClick = { navController.navigate(Routes.About) },
                    onPushNotificationsClick = { navController.navigate(Routes.ManageNotifications) },
                    isDarkMode = isDarkMode,
                    onDarkModeToggle = { onboardingViewModel.setDarkMode(it) },
                    userRole = role
                )
            }

                composable<Routes.ManageNotifications> {
                    ManageNotificationsScreen(onBackClick = { navController.popBackStack() })
                }

                composable<Routes.Notifications> {
                    NoticeBoardScreen(onBackClick = { navController.popBackStack() })
                }

                composable<Routes.Privacy> {
                    com.acadmate.dashboard.settings.PrivacyScreen(onBackClick = { navController.popBackStack() })
                }

                composable<Routes.Help> {
                    com.acadmate.dashboard.settings.HelpScreen(onBackClick = { navController.popBackStack() })
                }

                composable<Routes.About> {
                    com.acadmate.dashboard.settings.AboutScreen(onBackClick = { navController.popBackStack() })
                }
            }

            // Admin Graph
            navigation<Routes.AdminGraph>(startDestination = Routes.AdminDashboard) {
                composable<Routes.AdminDashboard> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    AdminDashboardScreen(
                        onAddUserClick = { navController.navigate(Routes.CreateUser) },
                        onManageCoursesClick = { navController.navigate(Routes.CourseManagement) },
                        onSyllabusClick = { navController.navigate(Routes.ManageSyllabus) },
                        onScheduleClick = { navController.navigate(Routes.TimetableManagement) },
                        onAiScheduleClick = { navController.navigate(Routes.AiTimetableGenerator) },
                        onAuditLogClick = { navController.navigate(Routes.AuditLog) },
                        onSettingsClick = { navController.navigate(Routes.CampusSetup) },
                        onSignOut = {
                            authViewModel.signOut()
                            navController.navigate(Routes.AuthGraph) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }
                composable<Routes.ManageSyllabus> {
                    SyllabusManagementScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.CreateUser> {
                    CreateUserScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.CampusSetup> {
                    CampusSetupScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.CourseManagement> {
                    CourseManagementScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.TimetableManagement> {
                    TimetableManagementScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.AiTimetableGenerator> {
                    AiTimetableGeneratorScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.CourseManagement> {
                    CourseManagementScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.TimetableManagement> {
                    TimetableManagementScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.AiTimetableGenerator> {
                    AiTimetableGeneratorScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.AuditLog> {
                    AuditLogScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Assignment Graph
            navigation<Routes.AssignmentGraph>(startDestination = Routes.AssignmentList) {
                composable<Routes.AssignmentList> {
                    val attendanceViewModel: com.acadmate.attendance.domain.AttendanceViewModel = hiltViewModel()
                    val role by attendanceViewModel.userRole.collectAsStateWithLifecycle()
                    
                    AssignmentListScreen(
                        isFaculty = role == UserRole.FACULTY,
                        onAddAssignmentClick = { navController.navigate(Routes.CreateAssignment) },
                        onAssignmentClick = { id -> navController.navigate(Routes.AssignmentDetail(id)) }
                    )
                }
                composable<Routes.CreateAssignment> {
                    CreateAssignmentScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.AssignmentDetail> { backStackEntry ->
                    val route: Routes.AssignmentDetail = backStackEntry.toRoute()
                    AssignmentDetailScreen(
                        assignmentId = route.assignmentId,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable<Routes.Gradebook> {
                    GradebookScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Routes.ResourceManager> {
                    val attendanceViewModel: com.acadmate.attendance.domain.AttendanceViewModel = hiltViewModel()
                    val role by attendanceViewModel.userRole.collectAsStateWithLifecycle()
                    
                    ResourceManagerScreen(
                        isFaculty = role == UserRole.FACULTY,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
