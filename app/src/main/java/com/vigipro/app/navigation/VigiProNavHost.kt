package com.vigipro.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vigipro.feature.auth.LoginScreen
import com.vigipro.feature.dashboard.DashboardScreen
import com.vigipro.feature.devices.addcamera.AddCameraScreen
import com.vigipro.feature.player.PlayerScreen
import com.vigipro.feature.settings.SettingsScreen

@Composable
fun VigiProNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard",
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToPlayer = { cameraId ->
                    navController.navigate("player/$cameraId")
                },
                onNavigateToAddCamera = {
                    navController.navigate("add_camera")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
            )
        }

        composable(
            route = "player/{cameraId}",
            arguments = listOf(navArgument("cameraId") { type = NavType.StringType }),
        ) {
            PlayerScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable("add_camera") {
            AddCameraScreen(
                onBack = { navController.popBackStack() },
                onCameraAdded = { navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen()
        }
    }
}
