package com.zexo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zexo.app.ui.screens.auth.AuthScreen
import com.zexo.app.ui.screens.home.HomeScreen
import com.zexo.app.ui.screens.chat.ChatScreen
import com.zexo.app.ui.screens.profile.ProfileScreen
import com.zexo.app.ui.screens.profile.ProfileEditScreen
import com.zexo.app.ui.screens.settings.SettingsScreen
import com.zexo.app.ui.screens.status.StatusViewScreen
import com.zexo.app.ui.screens.status.NewStatusScreen
import com.zexo.app.ui.screens.contacts.NewChatScreen
import com.zexo.app.ui.screens.calls.DialPadScreen
import com.zexo.app.ui.screens.contacts.QRScannerScreen
import com.zexo.app.ui.screens.admin.AdminDashboardScreen
import com.zexo.app.ui.screens.admin.AdminUsersScreen
import com.zexo.app.ui.screens.admin.AdminSettingsScreen
import com.zexo.app.ui.screens.admin.AdminLandingScreen
import com.zexo.app.ui.screens.splash.SplashScreen

@Composable
fun ZexoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Auth.route) {
            AuthScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(navController = navController, chatId = chatId)
        }
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            ProfileScreen(navController = navController, uid = uid)
        }
        composable(Screen.ProfileEdit.route) {
            ProfileEditScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = Screen.StatusView.route,
            arguments = listOf(navArgument("statusId") { type = NavType.StringType })
        ) { backStackEntry ->
            val statusId = backStackEntry.arguments?.getString("statusId") ?: ""
            StatusViewScreen(navController = navController, statusId = statusId)
        }
        composable(Screen.NewStatus.route) {
            NewStatusScreen(navController = navController)
        }
        composable(Screen.NewChat.route) {
            NewChatScreen(navController = navController)
        }
        composable(Screen.DialPad.route) {
            DialPadScreen(navController = navController)
        }
        composable(Screen.QRScanner.route) {
            QRScannerScreen(navController = navController)
        }
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(navController = navController)
        }
        composable(Screen.AdminUsers.route) {
            AdminUsersScreen(navController = navController)
        }
        composable(Screen.AdminSettings.route) {
            AdminSettingsScreen(navController = navController)
        }
        composable(Screen.AdminLanding.route) {
            AdminLandingScreen(navController = navController)
        }
    }
}
