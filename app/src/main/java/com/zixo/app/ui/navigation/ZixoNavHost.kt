package com.zixo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zixo.app.ui.screens.auth.AuthScreen
import com.zixo.app.ui.screens.auth.AuthViewModel
import com.zixo.app.ui.screens.chat.ChatScreen
import com.zixo.app.ui.screens.home.HomeScreen
import com.zixo.app.ui.screens.profile.EditProfileScreen
import com.zixo.app.ui.screens.status.CreateStatusScreen

@Composable
fun ZixoNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    val startDestination = if (currentUser != null) Screen.Home.route else Screen.Auth.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onChatClick = { chatId, otherUserId ->
                    navController.navigate(Screen.Chat.createRoute(chatId, otherUserId))
                },
                onNewChatClick = {
                    navController.navigate(Screen.NewChat.route)
                },
                onEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onCreateStatus = {
                    navController.navigate(Screen.CreateStatus.route)
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            ChatScreen(
                chatId = chatId,
                otherUserId = otherUserId,
                onBack = { navController.popBackStack() },
                onAudioCall = { uid ->
                    navController.navigate(Screen.CallScreen.createRoute("audio", uid))
                },
                onVideoCall = { uid ->
                    navController.navigate(Screen.CallScreen.createRoute("video", uid))
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NewChat.route) {
            com.zixo.app.ui.screens.contacts.NewChatPlaceholder(
                onBack = { navController.popBackStack() },
                onChatCreated = { chatId, otherUserId ->
                    navController.navigate(Screen.Chat.createRoute(chatId, otherUserId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.CreateStatus.route) {
            CreateStatusScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CallScreen.route,
            arguments = listOf(
                navArgument("callType") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("callId") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            navController.popBackStack()
        }
    }
}
