package com.vigipro.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.firebase.auth.FirebaseAuth
import com.vigipro.feature.accesscontrol.AccessControlScreen
import com.vigipro.feature.auth.LoginScreen
import com.vigipro.feature.discover.DiscoverScreen
import com.vigipro.feature.dashboard.AlertDigestScreen
import com.vigipro.feature.dashboard.DashboardScreen
import com.vigipro.feature.dashboard.EventTimelineScreen
import com.vigipro.feature.devices.addcamera.AddCameraScreen
import com.vigipro.feature.player.PlayerScreen
import com.vigipro.feature.player.multiview.MultiviewScreen
import com.vigipro.feature.player.recordings.PlaybackScreen
import com.vigipro.feature.player.recordings.RecordingsScreen
import com.vigipro.feature.settings.SettingsScreen
import com.vigipro.feature.sites.SitesScreen

private const val ANIM_DURATION = 350
private val animEasing = FastOutSlowInEasing

@Composable
fun VigiProNavHost() {
    val navController = rememberNavController()

    // FirebaseAuth.currentUser is synchronous — reads from local cache
    // If user was previously logged in, skip discover and go straight to dashboard
    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser != null) "dashboard" else "discover"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIM_DURATION, easing = animEasing),
            ) + fadeIn(animationSpec = tween(ANIM_DURATION / 2, delayMillis = ANIM_DURATION / 4))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIM_DURATION, easing = animEasing),
                targetOffset = { it / 5 },
            ) + fadeOut(animationSpec = tween(ANIM_DURATION / 2))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIM_DURATION, easing = animEasing),
                initialOffset = { it / 5 },
            ) + fadeIn(animationSpec = tween(ANIM_DURATION / 2, delayMillis = ANIM_DURATION / 4))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIM_DURATION, easing = animEasing),
            ) + fadeOut(animationSpec = tween(ANIM_DURATION / 2))
        },
    ) {
        // Discover — home screen sem login
        composable(
            route = "discover",
            enterTransition = { fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 1.08f) },
            popEnterTransition = { fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f) },
        ) {
            DiscoverScreen(
                onNavigateToPlayer = { cameraId ->
                    navController.navigate("player/$cameraId")
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                },
                onNavigateToAddCamera = {
                    navController.navigate("add_camera")
                },
            )
        }

        // Login — fade + scale (special transition)
        composable(
            route = "login",
            enterTransition = { fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 1.08f) },
        ) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("discover") { inclusive = true }
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
                onNavigateToSites = {
                    navController.navigate("sites")
                },
                onNavigateToEventTimeline = {
                    navController.navigate("event_timeline")
                },
                onNavigateToMultiview = {
                    navController.navigate("multiview")
                },
                onNavigateToRecordings = {
                    navController.navigate("recordings")
                },
                onNavigateToAlertDigest = {
                    navController.navigate("alert_digest")
                },
            )
        }

        composable("multiview") {
            MultiviewScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { cameraId ->
                    navController.navigate("player/$cameraId")
                },
            )
        }

        // Player — slide up (immersive feel)
        composable(
            route = "player/{cameraId}",
            arguments = listOf(navArgument("cameraId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(ANIM_DURATION, easing = animEasing),
                ) + fadeIn(tween(ANIM_DURATION / 2))
            },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(ANIM_DURATION, easing = animEasing),
                ) + fadeOut(tween(ANIM_DURATION / 2))
            },
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
                    navController.navigate("discover") {
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

        composable("alert_digest") {
            AlertDigestScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable("recordings") {
            RecordingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPlayback = { filePath ->
                    navController.navigate("playback/${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                },
            )
        }

        composable(
            route = "playback/{filePath}",
            arguments = listOf(navArgument("filePath") { type = NavType.StringType }),
        ) { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("filePath") ?: ""
            PlaybackScreen(
                filePath = filePath,
                onBack = { navController.popBackStack() },
            )
        }

        composable("sites") {
            SitesScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable("access_control") {
            AccessControlScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSites = { navController.navigate("sites") },
            )
        }

        composable(
            route = "access_control/redeem/{code}",
            arguments = listOf(navArgument("code") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://vigipro.app/invite/{code}" },
            ),
        ) {
            AccessControlScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSites = { navController.navigate("sites") },
            )
        }
    }
}
