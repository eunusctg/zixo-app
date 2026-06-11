package com.zexo.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    data object Profile : Screen("profile/{uid}") {
        fun createRoute(uid: String) = "profile/$uid"
    }
    data object ProfileEdit : Screen("profile_edit")
    data object Settings : Screen("settings")
    data object StatusView : Screen("status_view/{statusId}") {
        fun createRoute(statusId: String) = "status_view/$statusId"
    }
    data object NewStatus : Screen("new_status")
    data object NewChat : Screen("new_chat")
    data object DialPad : Screen("dialpad")
    data object QRScanner : Screen("qr_scanner")
    data object AdminDashboard : Screen("admin_dashboard")
    data object AdminUsers : Screen("admin_users")
    data object AdminSettings : Screen("admin_settings")
    data object AdminLanding : Screen("admin_landing")
}

enum class HomeTab(val label: String) {
    CHATS("Chats"),
    STATUS("Status"),
    CALLS("Calls"),
    SETTINGS("Settings")
}
