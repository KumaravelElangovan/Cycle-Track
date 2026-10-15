package com.cycletrack.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cycletrack.app.ui.history.HistoryScreen
import com.cycletrack.app.ui.home.HomeScreen
import com.cycletrack.app.ui.onboarding.OnboardingScreen
import com.cycletrack.app.ui.profile.ProfileScreen
import com.cycletrack.app.ui.ridedetail.RideDetailScreen
import com.cycletrack.app.ui.settings.SettingsScreen
import com.cycletrack.app.ui.splash.SplashScreen
import com.cycletrack.app.ui.stats.StatsScreen
import com.cycletrack.app.ui.tracking.TrackingScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Home",    Icons.Filled.Home)
    object Track    : Screen("tracking", "Track",   Icons.Filled.DirectionsBike)
    object History  : Screen("history",  "History", Icons.Filled.History)
    object Profile  : Screen("profile",  "Profile", Icons.Filled.Person)

    // Non-bottom-nav screens
    object Splash      : Screen("splash",             "", Icons.Filled.Home)
    object Onboarding  : Screen("onboarding",         "", Icons.Filled.Home)
    object RideDetail  : Screen("ride_detail/{id}",   "", Icons.Filled.Home)
    object Settings    : Screen("settings",           "", Icons.Filled.Home)
    object Stats       : Screen("stats",              "", Icons.Filled.Home)
}

val bottomNavScreens = listOf(Screen.Home, Screen.Track, Screen.History, Screen.Profile)

@Composable
fun AppNavigation(
    startDestination: String = Screen.Splash.route
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavScreens.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartCycling = { navController.navigate(Screen.Track.route) },
                    onViewHistory = { navController.navigate(Screen.History.route) },
                    onViewStats = { navController.navigate(Screen.Stats.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onRideClick = { id -> navController.navigate("ride_detail/$id") }
                )
            }
            composable(Screen.Track.route) {
                TrackingScreen(
                    onNavigateToRideDetail = { id ->
                        navController.navigate("ride_detail/$id")
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onRideClick = { id ->
                        navController.navigate("ride_detail/$id")
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSaved = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "ride_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: return@composable
                RideDetailScreen(workoutId = id, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Stats.route) {
                StatsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
