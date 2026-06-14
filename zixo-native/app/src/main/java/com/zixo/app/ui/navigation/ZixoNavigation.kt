package com.zixo.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zixo.app.data.repository.AuthState
import com.zixo.app.ui.components.ZixoBottomNav
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.screens.advanced.AdvancedDataScreen
import com.zixo.app.ui.screens.advanced.AdvancedNetworkScreen
import com.zixo.app.ui.screens.advanced.AdvancedSecurityScreen
import com.zixo.app.ui.screens.auth.AuthScreen
import com.zixo.app.ui.screens.auth.AuthViewModel
import com.zixo.app.ui.screens.calls.CallsScreen
import com.zixo.app.ui.screens.chats.ChatsScreen
import com.zixo.app.ui.screens.editprofile.EditProfileScreen
import com.zixo.app.ui.screens.encryption.EncryptionKeyScreen
import com.zixo.app.ui.screens.settings.SettingsScreen
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart

// ──────────────────────────────────────────────
// Route Constants
// ──────────────────────────────────────────────

object ZixoRoutes {
    const val AUTH = "auth"
    const val CHATS = "chats"
    const val CALLS = "calls"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "edit_profile"
    const val ADVANCED_NETWORK = "advanced_network"
    const val ADVANCED_SECURITY = "advanced_security"
    const val ADVANCED_DATA = "advanced_data"
    const val ENCRYPTION_KEY = "encryption_key"
    const val BLOCKED_CONTACTS = "blocked_contacts"
    const val CHAT_WALLPAPER = "chat_wallpaper"
    const val NOTIFICATION_TONE = "notification_tone"
}

// ──────────────────────────────────────────────
// Routes that display the bottom navigation bar
// ──────────────────────────────────────────────

private val BOTTOM_NAV_ROUTES = setOf(
    ZixoRoutes.CHATS,
    ZixoRoutes.CALLS,
    ZixoRoutes.SETTINGS,
)

// ──────────────────────────────────────────────
// Animation constants
// ──────────────────────────────────────────────

private const val ANIM_DURATION_MS = 350

// ──────────────────────────────────────────────
// NavHost
// ──────────────────────────────────────────────

@Composable
fun ZixoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Navigate based on auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == ZixoRoutes.AUTH) {
                    navController.navigate(ZixoRoutes.CHATS) {
                        popUpTo(ZixoRoutes.AUTH) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != null && currentRoute != ZixoRoutes.AUTH) {
                    navController.navigate(ZixoRoutes.AUTH) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            }
            AuthState.Loading -> { /* waiting for initial auth check */ }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ZixoRoutes.AUTH,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(ANIM_DURATION_MS)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(ANIM_DURATION_MS),
                    )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(ANIM_DURATION_MS)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(ANIM_DURATION_MS),
                    )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(ANIM_DURATION_MS)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(ANIM_DURATION_MS),
                    )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(ANIM_DURATION_MS)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(ANIM_DURATION_MS),
                    )
        },
    ) {
        // ── Auth ───────────────────────────────────────
        composable(
            route = ZixoRoutes.AUTH,
            enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION_MS)) },
            exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION_MS)) },
        ) {
            AuthScreen()
        }

        // ── Main screens (with bottom nav) ────────────
        composable(route = ZixoRoutes.CHATS) {
            ChatsScreen()
        }

        composable(route = ZixoRoutes.CALLS) {
            CallsScreen()
        }

        composable(route = ZixoRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        // ── Edit Profile ──────────────────────────────
        composable(route = ZixoRoutes.EDIT_PROFILE) {
            EditProfileScreen(navController = navController)
        }

        // ── Advanced Settings ─────────────────────────
        composable(route = ZixoRoutes.ADVANCED_NETWORK) {
            AdvancedNetworkScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(route = ZixoRoutes.ADVANCED_SECURITY) {
            AdvancedSecurityScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(route = ZixoRoutes.ADVANCED_DATA) {
            AdvancedDataScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Encryption Key ────────────────────────────
        composable(route = ZixoRoutes.ENCRYPTION_KEY) {
            EncryptionKeyScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Blocked Contacts ──────────────────────────
        composable(route = ZixoRoutes.BLOCKED_CONTACTS) {
            SubScreenPlaceholder(
                title = "Blocked Contacts",
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Chat Wallpaper ────────────────────────────
        composable(route = ZixoRoutes.CHAT_WALLPAPER) {
            SubScreenPlaceholder(
                title = "Chat Wallpaper",
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Notification Tone ─────────────────────────
        composable(route = ZixoRoutes.NOTIFICATION_TONE) {
            SubScreenPlaceholder(
                title = "Notification Tone",
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

// ──────────────────────────────────────────────
// Main Scaffold with Bottom Navigation
// ──────────────────────────────────────────────

@Composable
fun ZixoMainScaffold(
    navController: NavHostController = rememberNavController(),
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showBottomNav by remember(currentRoute) {
        derivedStateOf { currentRoute in BOTTOM_NAV_ROUTES }
    }

    val selectedTabIndex = when (currentRoute) {
        ZixoRoutes.CHATS -> 0
        ZixoRoutes.CALLS -> 1
        ZixoRoutes.SETTINGS -> 2
        else -> 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomNav) {
                ZixoBottomNav(
                    selectedIndex = selectedTabIndex,
                    onItemSelected = { index ->
                        val targetRoute = when (index) {
                            0 -> ZixoRoutes.CHATS
                            1 -> ZixoRoutes.CALLS
                            2 -> ZixoRoutes.SETTINGS
                            else -> ZixoRoutes.CHATS
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                    )
                )
                .padding(innerPadding),
        ) {
            ZixoNavHost(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ──────────────────────────────────────────────
// Placeholder for screens not yet implemented
// ──────────────────────────────────────────────

@Composable
private fun SubScreenPlaceholder(
    title: String,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            ZixoTopBar(
                title = title,
                showBackButton = true,
                onBackClick = onBackClick,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$title\nComing Soon",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
