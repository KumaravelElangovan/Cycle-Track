package com.cycletrack.app.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycletrack.app.ui.theme.CycleDarkBlue
import com.cycletrack.app.ui.theme.CycleGreen
import com.cycletrack.app.ui.theme.CycleGreenDark
import com.cycletrack.app.ui.theme.CycleLightBlue
import com.cycletrack.app.ui.theme.CycleMidBlue
import com.cycletrack.app.ui.theme.CycleTextOnDark
import com.cycletrack.app.ui.theme.CycleTextOnDarkSecondary
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// Data model for each onboarding page
// ─────────────────────────────────────────────────────────────

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.DirectionsBike,
        title = "Track Your Rides",
        subtitle = "Real GPS-based distance and speed tracking",
        gradientStart = Color(0xFF0D1B2A),
        gradientEnd = Color(0xFF1A3A5C)
    ),
    OnboardingPage(
        icon = Icons.Filled.Timer,
        title = "Monitor Progress",
        subtitle = "View stats, calories and weekly goals",
        gradientStart = Color(0xFF0A2218),
        gradientEnd = Color(0xFF1A4D30)
    ),
    OnboardingPage(
        icon = Icons.Filled.Lock,
        title = "Your Data Stays Private",
        subtitle = "All data stored locally on device.\nNo cloud required.",
        gradientStart = Color(0xFF1A1A2E),
        gradientEnd = Color(0xFF16213E)
    )
)

// ─────────────────────────────────────────────────────────────
// OnboardingScreen
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Swipeable pages ──────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = onboardingPages[pageIndex]
            OnboardingPageContent(page = page)
        }

        // ── Bottom controls overlay ──────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicator dots
            PageIndicator(
                pageCount = onboardingPages.size,
                currentPage = pagerState.currentPage
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Next / Get Started button
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CycleGreen,
                    contentColor = CycleDarkBlue
                )
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Single onboarding page content
// ─────────────────────────────────────────────────────────────

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(page.gradientStart, page.gradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                // Shift content upward so it doesn't sit behind the button overlay
                .padding(bottom = 180.dp)
        ) {
            // Icon in a circular tinted container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(CycleGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = page.title,
                    tint = CycleGreen,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Title
            Text(
                text = page.title,
                color = CycleTextOnDark,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = page.subtitle,
                color = CycleTextOnDarkSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Page indicator dots
// ─────────────────────────────────────────────────────────────

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 8.dp,
                label = "dot_width_$index"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) CycleGreen else CycleGreen.copy(alpha = 0.35f)
                    )
            )
        }
    }
}
