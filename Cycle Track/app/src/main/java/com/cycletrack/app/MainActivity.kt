package com.cycletrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.ui.navigation.AppNavigation
import com.cycletrack.app.ui.theme.CycleTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userProfile by preferencesManager.userProfileFlow
                .collectAsStateWithLifecycle(initialValue = null)
            val isDark = userProfile?.darkMode ?: isSystemInDarkTheme()

            CycleTrackTheme(darkTheme = isDark) {
                AppNavigation()
            }
        }
    }
}
