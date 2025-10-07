package com.example.wodifyplus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.wodifyplus.ui.screens.calendar.CalendarScreen
import com.example.wodifyplus.ui.screens.home.HomeScreen
import com.example.wodifyplus.ui.screens.selection.SelectionScreen
import com.example.wodifyplus.ui.screens.settings.SettingsScreen
import com.example.wodifyplus.ui.screens.stats.StatsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Selection : Screen("selection")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
    object Stats : Screen("stats")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSelection = {
                    navController.navigate(Screen.Selection.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Selection.route) {
            SelectionScreen(
                onNavigateToCalendar = {
                    navController.navigate(Screen.Calendar.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

