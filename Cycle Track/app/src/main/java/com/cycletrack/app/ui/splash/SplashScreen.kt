package com.cycletrack.app.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.domain.model.UserProfile
import com.cycletrack.app.ui.theme.CycleDarkBlue
import com.cycletrack.app.ui.theme.CycleGreen
import com.cycletrack.app.ui.theme.CycleTextOnDarkSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// SplashViewModel
// ─────────────────────────────────────────────────────────────

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userPreferencesManager.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserProfile()
        )
}

// ─────────────────────────────────────────────────────────────
// SplashScreen
// ─────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Drive animation start
    var animationStarted by remember { mutableStateOf(false) }

    // Scale: 0.4 → 1.0 over 800 ms
    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.4f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    // Alpha: 0 → 1 over 800 ms
    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    // Start animation immediately, then navigate after 2 s
    LaunchedEffect(Unit) {
        animationStarted = true
        delay(2_000L)
        if (userProfile.onboardingDone) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleDarkBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            // Animated bike icon
            Icon(
                imageVector = Icons.Filled.DirectionsBike,
                contentDescription = "CycleTrack Logo",
                tint = CycleGreen,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "CycleTrack",
                color = CycleGreen,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CycleGreen
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Smart Cycling Tracker",
                color = CycleTextOnDarkSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )
        }
    }
}
