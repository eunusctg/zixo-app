package com.zixo.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zixo.app.data.repository.AuthState
import com.zixo.app.ui.chat.ChatMessageScreen
import com.zixo.app.ui.chat.GroupChatScreen
import com.zixo.app.ui.components.ZixoBottomNav
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.contacts.ContactListScreen
import com.zixo.app.ui.main.HomeScreen
import com.zixo.app.ui.screens.auth.AuthScreen
import com.zixo.app.ui.screens.auth.AuthViewModel
import com.zixo.app.ui.screens.calls.CallsScreen
import com.zixo.app.ui.settings.EditProfileScreen
import com.zixo.app.ui.settings.SettingsScreen
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.settings.SubPages.AccountSecurityScreen
import com.zixo.app.ui.settings.SubPages.ChatConfigScreen
import com.zixo.app.ui.settings.SubPages.NotificationManagerScreen
import com.zixo.app.ui.settings.SubPages.PrivacyCenterScreen
import com.zixo.app.ui.settings.SubPages.StorageDataHubScreen
import com.zixo.app.ui.status.StatusTabScreen
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

// ──────────────────────────────────────────────
// Route Constants
// ──────────────────────────────────────────────

object ZixoRoutes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val CHATS = "chats"
    const val CONTACTS = "contacts"
    const val CALLS = "calls"
    const val STATUS = "status"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "edit_profile"
    const val ACCOUNT_SECURITY = "account_security"
    const val PRIVACY_CENTER = "privacy_center"
    const val CHAT_CONFIG = "chat_config"
    const val NOTIFICATION_MANAGER = "notification_manager"
    const val STORAGE_DATA_HUB = "storage_data_hub"
    const val CHAT_MESSAGE = "chat_message/{threadId}"
    const val GROUP_CHAT = "group_chat/{threadId}"

    fun chatMessageRoute(threadId: String) = "chat_message/$threadId"
    fun groupChatRoute(threadId: String) = "group_chat/$threadId"
}

// ──────────────────────────────────────────────
// Routes that display the bottom navigation bar
// ──────────────────────────────────────────────

private val BOTTOM_NAV_ROUTES = setOf(
    ZixoRoutes.HOME,
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
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    // Navigate based on auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == ZixoRoutes.AUTH) {
                    navController.navigate(ZixoRoutes.HOME) {
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

        // ── Home (main tab host) ─────────────────────
        composable(route = ZixoRoutes.HOME) {
            HomeScreen(
                onChatClick = { threadId ->
                    navController.navigate(ZixoRoutes.chatMessageRoute(threadId))
                },
                onGroupChatClick = { threadId ->
                    navController.navigate(ZixoRoutes.groupChatRoute(threadId))
                },
            )
        }

        // ── Settings (with bottom nav) ──────────────
        composable(route = ZixoRoutes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                viewModel = settingsViewModel,
            )
        }

        // ── Chat Message Screen (keyboard-safe) ────
        composable(
            route = ZixoRoutes.CHAT_MESSAGE,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId") ?: return@composable
            ChatMessageScreen(
                threadId = threadId,
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Group Chat Screen ──────────────────────
        composable(
            route = ZixoRoutes.GROUP_CHAT,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId") ?: return@composable
            GroupChatScreen(
                threadId = threadId,
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Edit Profile (Liquid Glass) ────────────
        composable(route = ZixoRoutes.EDIT_PROFILE) {
            EditProfileScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        // ── Settings Sub-Pages (Liquid Glass) ─────
        composable(route = ZixoRoutes.ACCOUNT_SECURITY) {
            AccountSecurityScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(route = ZixoRoutes.PRIVACY_CENTER) {
            PrivacyCenterScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(route = ZixoRoutes.CHAT_CONFIG) {
            ChatConfigScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(route = ZixoRoutes.NOTIFICATION_MANAGER) {
            NotificationManagerScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(route = ZixoRoutes.STORAGE_DATA_HUB) {
            StorageDataHubScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Main Scaffold with Bottom Navigation + Glass BG
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
        ZixoRoutes.HOME -> 0
        ZixoRoutes.SETTINGS -> 1
        else -> 0
    }

    ZixoGlassBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                if (showBottomNav) {
                    ZixoBottomNav(
                        selectedIndex = selectedTabIndex,
                        onItemSelected = { index ->
                            val targetRoute = when (index) {
                                0 -> ZixoRoutes.HOME
                                1 -> ZixoRoutes.SETTINGS
                                else -> ZixoRoutes.HOME
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
                    .padding(innerPadding),
            ) {
                ZixoNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Placeholder for screens not yet fully implemented
// ──────────────────────────────────────────────

@Composable
private fun SubScreenPlaceholder(
    title: String,
    onBackClick: () -> Unit,
) {
    ZixoGlassBackground {
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
