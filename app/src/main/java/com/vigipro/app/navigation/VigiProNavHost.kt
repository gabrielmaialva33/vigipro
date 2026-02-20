package com.vigipro.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.vigipro.feature.accesscontrol.AccessControlScreen
import com.vigipro.feature.auth.LoginScreen
import com.vigipro.feature.dashboard.DashboardScreen
import com.vigipro.feature.dashboard.EventTimelineScreen
import com.vigipro.feature.devices.addcamera.AddCameraScreen
import com.vigipro.feature.player.PlayerScreen
import com.vigipro.feature.settings.SettingsScreen

@Composable
fun VigiProNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login",
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
                onNavigateToEditCamera = { cameraId ->
                    navController.navigate("edit_camera/$cameraId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAccessControl = {
                    navController.navigate("access_control")
                },
                onNavigateToEventTimeline = {
                    navController.navigate("event_timeline")
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
                onCameraSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = "edit_camera/{cameraId}",
            arguments = listOf(navArgument("cameraId") { type = NavType.StringType }),
        ) {
            AddCameraScreen(
                onBack = { navController.popBackStack() },
                onCameraSaved = { navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable("event_timeline") {
            EventTimelineScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable("access_control") {
            AccessControlScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "access_control/redeem/{code}",
            arguments = listOf(navArgument("code") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://vigipro.app/invite/{code}" },
            ),
        ) {
            // Redeem invite - for now redirect to access control
            AccessControlScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
