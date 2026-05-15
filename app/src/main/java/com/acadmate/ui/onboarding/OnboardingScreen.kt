package com.acadmate.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadmate.R
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.MeshBackground
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.designsystem.theme.LocalSpacing
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val lottieRes: Int,
    val accentColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Zero Friction Attendance",
            subtitle = "Proprietary 5-layer anti-proxy protocol using ultrasonic handshakes and ML liveness.",
            lottieRes = R.raw.never_miss_class,
            accentColor = Color(0xFF6366F1)
        ),
        OnboardingPage(
            title = "Context-Aware AI",
            subtitle = "Your learning, supercharged. Gemini-powered tutors, lecture notes, and exam simulators.",
            lottieRes = R.raw.ai_study_partner,
            accentColor = Color(0xFF8B5CF6)
        ),
        OnboardingPage(
            title = "Elite Command Center",
            subtitle = "A unified operating system for students, faculty, and administrators to eliminate institutional drag.",
            lottieRes = R.raw.everything_one_place,
            accentColor = Color(0xFFEC4899)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    MeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(LocalSpacing.current.md),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (pagerState.currentPage != pages.lastIndex) {
                        TextButton(onClick = onSkip) {
                            Text(
                                "Skip",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    OnboardingPageContent(pages[page], pagerState)
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Page indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 28.dp else 8.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "indicatorWidth"
                            )

                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) pages[index].accentColor
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                            )
                        }
                    }

                    // Main Action
                    Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                        AnimatedContent(
                            targetState = pagerState.currentPage == pages.lastIndex,
                            transitionSpec = {
                                (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { it / 2 })
                            },
                            label = "ActionButton"
                        ) { isLastPage ->
                            if (isLastPage) {
                                AcadMateButton(
                                    text = "Enter the Ecosystem",
                                    onClick = onGetStarted,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = pages[pagerState.currentPage].accentColor
                                    )
                                ) {
                                    Text("Next", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    pagerState: androidx.compose.foundation.pager.PagerState
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(page.lottieRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(320.dp)
                .graphicsLayer {
                    val pageOffset = pagerState.currentPageOffsetFraction
                    alpha = 1f - kotlin.math.abs(pageOffset)
                    scaleX = 1f - kotlin.math.abs(pageOffset) * 0.2f
                    scaleY = 1f - kotlin.math.abs(pageOffset) * 0.2f
                }
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
