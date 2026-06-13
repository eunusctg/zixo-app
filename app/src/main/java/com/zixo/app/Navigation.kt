package com.zixo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zixo.app.data.local.PreferencesDataStore
import com.zixo.app.data.repository.AuthRepositoryImpl
import com.zixo.app.data.repository.ChatRepositoryImpl
import com.zixo.app.data.repository.SettingsRepositoryImpl
import com.zixo.app.data.repository.StatusRepositoryImpl
import com.zixo.app.domain.model.AuthState
import com.zixo.app.ui.auth.AuthViewModel
import com.zixo.app.ui.auth.LoginScreen
import com.zixo.app.ui.chat.ChatMessageScreen
import com.zixo.app.ui.chat.ChatViewModel
import com.zixo.app.ui.main.HomeScreen
import com.zixo.app.ui.settings.EditProfileScreen
import com.zixo.app.ui.settings.SettingsScreen
import com.zixo.app.ui.settings.SettingsViewModel
import com.zixo.app.ui.status.StatusTabScreen
import com.zixo.app.ui.status.StatusViewModel

@Composable
fun ZixoNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Repository singletons
    val dataStore = remember { PreferencesDataStore(context) }
    val authRepo = remember { AuthRepositoryImpl(dataStore) }
    val settingsRepo = remember { SettingsRepositoryImpl(dataStore) }
    val chatRepo = remember { ChatRepositoryImpl() }
    val statusRepo = remember { StatusRepositoryImpl() }

    val authState by authRepo.authState.collectAsState()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Toast.makeText(context, "Some features may not work without permissions", Toast.LENGTH_SHORT).show()
        }
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions)
        }
    }

    val startDestination = when (authState) {
        is AuthState.Authenticated -> "home"
        is AuthState.Unauthenticated -> "login"
        else -> "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            val authViewModel = remember { AuthViewModel(authRepo) }
            LoginScreen(
                authViewModel = authViewModel,
                onAuthComplete = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            val chats by chatRepo.chats.collectAsState()
            val callHistory by chatRepo.callHistory.collectAsState()
            val currentUser by authRepo.currentUser.collectAsState()

            // Start realtime listeners
            LaunchedEffect(Unit) {
                chatRepo.startListening()
                statusRepo.startListening()
            }

            HomeScreen(
                currentUser = currentUser,
                chats = chats,
                callHistory = callHistory,
                onChatClick = { chatId ->
                    navController.navigate("chat/$chatId")
                },
                onNewChat = { /* TODO: New chat screen */ },
                onCallClick = { userId -> /* TODO: Initiate call */ },
                onStatusClick = {
                    navController.navigate("status")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatViewModel = remember { ChatViewModel(chatRepo) }
            val currentUser by authRepo.currentUser.collectAsState()

            LaunchedEffect(chatId) {
                chatViewModel.loadChat(chatId)
            }

            ChatMessageScreen(
                chatViewModel = chatViewModel,
                currentUserId = currentUser?.uid ?: "",
                chatName = "Chat", // TODO: Load chat name
                chatAvatar = "",
                onBack = { navController.popBackStack() }
            )
        }

        composable("status") {
            val statusViewModel = remember { StatusViewModel(statusRepo) }

            LaunchedEffect(Unit) {
                statusViewModel.startListening()
            }

            StatusTabScreen(statusViewModel = statusViewModel)
        }

        composable("settings") {
            val settingsViewModel = remember { SettingsViewModel(settingsRepo, authRepo) }
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onEditProfile = { navController.navigate("edit_profile") },
                onSignOut = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("edit_profile") {
            val settingsViewModel = remember { SettingsViewModel(settingsRepo, authRepo) }
            EditProfileScreen(
                settingsViewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
