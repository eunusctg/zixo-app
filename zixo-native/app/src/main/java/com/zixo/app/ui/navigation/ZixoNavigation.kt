package com.zixo.app.ui.navigation

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.platform.LocalContext
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
import com.zixo.app.domain.repository.AuthState
import com.zixo.app.ui.chat.ChatMessageScreen
import com.zixo.app.ui.chat.GroupChatScreen
import com.zixo.app.ui.components.CallScreenOverlay
import com.zixo.app.ui.components.ZixoBottomNav
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.contacts.ContactListScreen
import com.zixo.app.ui.main.HomeScreen
import com.zixo.app.ui.screens.auth.AuthScreen
import com.zixo.app.ui.screens.auth.AuthViewModel
import com.zixo.app.ui.screens.calls.CallsScreen
import com.zixo.app.ui.settings.EditProfileScreen
import com.zixo.app.ui.settings.SettingsScreen
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.settings.SubPages.AboutUsScreen
import com.zixo.app.ui.settings.SubPages.AccountSecurityScreen
import com.zixo.app.ui.settings.SubPages.ChatConfigScreen
import com.zixo.app.ui.settings.SubPages.ContactUsScreen
import com.zixo.app.ui.settings.SubPages.NotificationManagerScreen
import com.zixo.app.ui.settings.SubPages.PrivacyCenterScreen
import com.zixo.app.ui.settings.SubPages.PrivacyPolicyScreen
import com.zixo.app.ui.settings.SubPages.StorageDataHubScreen
import com.zixo.app.ui.settings.SubPages.OpenSourceLicensesScreen
import com.zixo.app.ui.settings.SubPages.TermsOfServiceScreen
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════
// Type-Safe Route Definitions
// ════════════════════════════════════════════════════════════════

/**
 * Sealed class of all navigable routes in the Zixo application.
 *
 * Routes with path parameters use the `{param}` syntax and expose a
 * `createRoute()` helper so callers never construct route strings by hand.
 */
sealed class ZixoRoute(val route: String) {
    data object Auth : ZixoRoute("auth")
    data object Home : ZixoRoute("home")
    data object Chat : ZixoRoute("chat/{threadId}") {
        fun createRoute(threadId: String) = "chat/$threadId"
    }
    data object GroupChat : ZixoRoute("group_chat/{threadId}") {
        fun createRoute(threadId: String) = "group_chat/$threadId"
    }
    data object ContactList : ZixoRoute("contacts")
    data object Settings : ZixoRoute("settings")
    data object EditProfile : ZixoRoute("edit_profile")
    data object AccountSecurity : ZixoRoute("account_security")
    data object PrivacyCenter : ZixoRoute("privacy_center")
    data object ChatConfig : ZixoRoute("chat_config")
    data object NotificationManager : ZixoRoute("notification_manager")
    data object StorageDataHub : ZixoRoute("storage_data_hub")
    data object AboutUs : ZixoRoute("about_us")
    data object ContactUs : ZixoRoute("contact_us")
    data object PrivacyPolicy : ZixoRoute("privacy_policy")
    data object TermsOfService : ZixoRoute("terms_of_service")
    data object OpenSourceLicenses : ZixoRoute("open_source_licenses")
    data object CallScreen : ZixoRoute("call/{callId}") {
        fun createRoute(callId: String) = "call/$callId"
    }
}

// ════════════════════════════════════════════════════════════════
// Routes that display the bottom navigation bar
// ════════════════════════════════════════════════════════════════

/**
 * Routes that display the outer bottom navigation bar.
 * HOME is excluded because HomeScreen manages its own internal 85dp bottom nav
 * with 4 tabs (Chats, Status, Calls, Contacts). Only SETTINGS shows the outer nav.
 */
private val BOTTOM_NAV_ROUTES = setOf(
    ZixoRoute.Settings.route,
)

// ════════════════════════════════════════════════════════════════
// Animation Constants
// ════════════════════════════════════════════════════════════════

private const val ANIM_DURATION_MS = 350
private const val OVERLAY_ANIM_DURATION_MS = 300

// ════════════════════════════════════════════════════════════════
// Default Animated Transitions — Fade + Slide
// ════════════════════════════════════════════════════════════════

private val DefaultEnterTransition: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition = {
    fadeIn(animationSpec = tween(ANIM_DURATION_MS)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIM_DURATION_MS),
            )
}

private val DefaultExitTransition: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition = {
    fadeOut(animationSpec = tween(ANIM_DURATION_MS)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIM_DURATION_MS),
            )
}

private val DefaultPopEnterTransition: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition = {
    fadeIn(animationSpec = tween(ANIM_DURATION_MS)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIM_DURATION_MS),
            )
}

private val DefaultPopExitTransition: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition = {
    fadeOut(animationSpec = tween(ANIM_DURATION_MS)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIM_DURATION_MS),
            )
}

/** Fade-only transition used for the Auth screen (no directional slide). */
private val FadeEnterTransition: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition = {
    fadeIn(animationSpec = tween(ANIM_DURATION_MS))
}

private val FadeExitTransition: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition = {
    fadeOut(animationSpec = tween(ANIM_DURATION_MS))
}

/** Scale + fade for the fullscreen Call overlay. */
private val CallEnterTransition: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.EnterTransition = {
    fadeIn(animationSpec = tween(OVERLAY_ANIM_DURATION_MS)) +
            scaleIn(
                initialScale = 0.85f,
                animationSpec = tween(OVERLAY_ANIM_DURATION_MS)
            )
}

private val CallExitTransition: AnimatedContentTransitionScope<*>.() -> androidx.compose.animation.ExitTransition = {
    fadeOut(animationSpec = tween(OVERLAY_ANIM_DURATION_MS)) +
            scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(OVERLAY_ANIM_DURATION_MS)
            )
}

// ════════════════════════════════════════════════════════════════
// NavHost
// ════════════════════════════════════════════════════════════════

/**
 * The navigation graph host for the entire Zixo application.
 *
 * Auth-gating:
 * - On first composition, starts at [ZixoRoute.Auth].
 * - When [AuthState.Authenticated] is emitted, navigates to [ZixoRoute.Home].
 * - When [AuthState.Unauthenticated] is emitted, navigates back to [ZixoRoute.Auth].
 *
 * Deep linking from FCM notifications is handled via [deepLinkCallId].
 */
@Composable
fun ZixoNavHost(
    navController: NavHostController,
    deepLinkCallId: String? = null,
    modifier: Modifier = Modifier,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    // ── Navigate based on auth state changes ─────────────────────
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == ZixoRoute.Auth.route) {
                    navController.navigate(ZixoRoute.Home.route) {
                        popUpTo(ZixoRoute.Auth.route) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != null && currentRoute != ZixoRoute.Auth.route) {
                    navController.navigate(ZixoRoute.Auth.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            }
            AuthState.Loading -> { /* waiting for initial auth check */ }
        }
    }

    // ── Handle FCM deep link for incoming calls ──────────────────
    LaunchedEffect(deepLinkCallId) {
        if (deepLinkCallId != null && authState is AuthState.Authenticated) {
            navController.navigate(ZixoRoute.CallScreen.createRoute(deepLinkCallId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = ZixoRoute.Auth.route,
        modifier = modifier,
        enterTransition = DefaultEnterTransition,
        exitTransition = DefaultExitTransition,
        popEnterTransition = DefaultPopEnterTransition,
        popExitTransition = DefaultPopExitTransition,
    ) {
        // ── Auth ───────────────────────────────────────────────
        composable(
            route = ZixoRoute.Auth.route,
            enterTransition = FadeEnterTransition,
            exitTransition = FadeExitTransition,
        ) {
            AuthScreen()
        }

        // ── Home (main tab host — contains internal tab navigation) ──
        composable(route = ZixoRoute.Home.route) {
            HomeScreen(
                navController = navController,
                onChatClick = { threadId ->
                    navController.navigate(ZixoRoute.Chat.createRoute(threadId))
                },
                onGroupChatClick = { threadId ->
                    navController.navigate(ZixoRoute.GroupChat.createRoute(threadId))
                },
                onContactClick = { contactUserId ->
                    // Zero-trust: only mutual contacts can navigate to chat
                    // Navigate to the contact list to find or create a thread
                    navController.navigate(ZixoRoute.ContactList.route)
                },
                onNewChatClick = {
                    // Open contact list for new chat creation
                    navController.navigate(ZixoRoute.ContactList.route)
                },
                onCallClick = { callId ->
                    navController.navigate(ZixoRoute.CallScreen.createRoute(callId))
                },
            )
        }

        // ── Contact List ───────────────────────────────────────
        composable(route = ZixoRoute.ContactList.route) {
            ContactListScreen(
                onContactClick = { contactUserId ->
                    // Navigate to chat with the contact (zero-trust: mutual only)
                },
            )
        }

        // ── Settings (with bottom nav) ──────────────────────────
        composable(route = ZixoRoute.Settings.route) {
            SettingsScreen(
                navController = navController,
                viewModel = settingsViewModel,
            )
        }

        // ── Chat Message Screen ─────────────────────────────────
        composable(
            route = ZixoRoute.Chat.route,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId") ?: return@composable
            ChatMessageScreen(
                threadId = threadId,
                navController = navController,
                onBack = { navController.popBackStack() },
            )
        }

        // ── Group Chat Screen ───────────────────────────────────
        composable(
            route = ZixoRoute.GroupChat.route,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId") ?: return@composable
            GroupChatScreen(
                threadId = threadId,
                navController = navController,
                onBack = { navController.popBackStack() },
            )
        }

        // ── Edit Profile ────────────────────────────────────────
        composable(route = ZixoRoute.EditProfile.route) {
            EditProfileScreen(
                navController = navController,
            )
        }

        // ── Settings Sub-Pages ──────────────────────────────────
        composable(route = ZixoRoute.AccountSecurity.route) {
            AccountSecurityScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(route = ZixoRoute.PrivacyCenter.route) {
            PrivacyCenterScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(route = ZixoRoute.ChatConfig.route) {
            ChatConfigScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(route = ZixoRoute.NotificationManager.route) {
            NotificationManagerScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(route = ZixoRoute.StorageDataHub.route) {
            StorageDataHubScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        // ── About Us ───────────────────────────────────────────
        composable(route = ZixoRoute.AboutUs.route) {
            AboutUsScreen(
                onBackClick = { navController.popBackStack() },
                onPrivacyPolicyClick = {
                    navController.navigate(ZixoRoute.PrivacyPolicy.route)
                },
                onTermsOfServiceClick = {
                    navController.navigate(ZixoRoute.TermsOfService.route)
                },
                onLicensesClick = {
                    navController.navigate(ZixoRoute.OpenSourceLicenses.route)
                },
            )
        }

        // ── Contact Us ─────────────────────────────────────────
        composable(route = ZixoRoute.ContactUs.route) {
            ContactUsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Privacy Policy ─────────────────────────────────────
        composable(route = ZixoRoute.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Terms of Service ──────────────────────────────────
        composable(route = ZixoRoute.TermsOfService.route) {
            TermsOfServiceScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Open Source Licenses ───────────────────────────────
        composable(route = ZixoRoute.OpenSourceLicenses.route) {
            OpenSourceLicensesScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Call Screen (fullscreen overlay) ────────────────────
        composable(
            route = ZixoRoute.CallScreen.route,
            arguments = listOf(navArgument("callId") { type = NavType.StringType }),
            enterTransition = CallEnterTransition,
            exitTransition = CallExitTransition,
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
            CallScreenOverlay(
                callId = callId,
                onEndCall = { navController.popBackStack() },
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Main Scaffold with Bottom Navigation + Glass BG
// ════════════════════════════════════════════════════════════════

/**
 * Root scaffold that wraps the [ZixoNavHost] with:
 * - [ZixoGlassBackground] for the animated blob atmosphere
 * - Bottom navigation bar on applicable routes
 * - Transparent container color so the glass background shows through
 *
 * @param navController  The navigation controller. Defaults to `rememberNavController()`.
 * @param deepLinkCallId Optional FCM notification call ID for deep linking.
 */
@Composable
fun ZixoNavigation(
    navController: NavHostController = rememberNavController(),
    deepLinkCallId: String? = null,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showBottomNav by remember(currentRoute) {
        derivedStateOf { currentRoute in BOTTOM_NAV_ROUTES }
    }

    val selectedTabIndex = when (currentRoute) {
        ZixoRoute.Home.route -> 0
        ZixoRoute.Settings.route -> 1
        else -> 0
    }

    ZixoGlassBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomNav) {
                    ZixoBottomNav(
                        selectedIndex = selectedTabIndex,
                        onItemSelected = { index ->
                            val targetRoute = when (index) {
                                0 -> ZixoRoute.Home.route
                                1 -> ZixoRoute.Settings.route
                                else -> ZixoRoute.Home.route
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
                    deepLinkCallId = deepLinkCallId,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
