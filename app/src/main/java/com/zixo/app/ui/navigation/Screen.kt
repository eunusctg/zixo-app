package com.zixo.app.ui.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Chat : Screen("chat/{chatId}/{otherUserId}") {
        fun createRoute(chatId: String, otherUserId: String) = "chat/$chatId/$otherUserId"
    }
    data object Settings : Screen("settings")
    data object EditProfile : Screen("edit_profile")
    data object NewChat : Screen("new_chat")
    data object CallScreen : Screen("call/{callType}/{otherUserId}/{callId}") {
        fun createRoute(callType: String, otherUserId: String, callId: String = "") = "call/$callType/$otherUserId/$callId"
    }
    data object CreateStatus : Screen("create_status")
    data object StatusViewer : Screen("status_viewer/{statusId}") {
        fun createRoute(statusId: String) = "status_viewer/$statusId"
    }
}

enum class BottomTab(val route: String, val label: String) {
    CHATS("tab_chats", "Chats"),
    CALLS("tab_calls", "Calls"),
    STATUS("tab_status", "Status"),
    SETTINGS("tab_settings", "Settings")
}
