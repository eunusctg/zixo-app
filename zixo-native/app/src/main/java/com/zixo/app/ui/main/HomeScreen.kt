package com.zixo.app.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.liquidGlassNavItem
import com.zixo.app.ui.contacts.ContactListScreen
import com.zixo.app.ui.screens.calls.CallsScreen
import com.zixo.app.ui.screens.chats.ChatsScreen
import com.zixo.app.ui.status.StatusTabScreen
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ──────────────────────────────────────────────
// Tab Configuration
// ──────────────────────────────────────────────

/**
 * Represents a tab in the home screen bottom navigation.
 *
 * @param index     Zero-based position in the tab bar.
 * @param label     Display name shown under the icon.
 * @param icon      The outlined [ImageVector] for the unselected state.
 * @param fabIcon   The [ImageVector] to show in the FAB when this tab is active.
 * @param fabLabel  Accessibility label for the FAB on this tab.
 */
enum class HomeTab(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val fabIcon: ImageVector,
    val fabLabel: String
) {
    CHATS(0, "Chats", Icons.Outlined.Chat, Icons.Outlined.Edit, "New chat"),
    STATUS(1, "Status", Icons.Outlined.Edit, Icons.Outlined.Edit, "New status"),
    CALLS(2, "Calls", Icons.Outlined.Call, Icons.Outlined.Dialpad, "Dial pad"),
    CONTACTS(3, "Contacts", Icons.Outlined.Contacts, Icons.Outlined.PersonAdd, "Find contact")
}

// ──────────────────────────────────────────────
// Home Screen
// ──────────────────────────────────────────────

/**
 * The main home screen that hosts the 4-tab bottom navigation and content.
 *
 * Layout (top to bottom):
 * 1. Branded top bar with "Zixo" and user avatar
 * 2. Tab content area (Chats / Status / Calls / Contacts)
 * 3. 85dp liquid-glass bottom navigation bar
 * 4. Floating action button (mint green) above the bottom nav
 *
 * @param navController  Navigation controller for routing to detail screens.
 * @param selectedTabIndex  The currently selected tab index (default 0 = Chats).
 * @param onTabSelected  Callback invoked when the user selects a tab.
 * @param onChatClick    Callback invoked when a chat thread is tapped.
 * @param onGroupChatClick Callback invoked when a group chat thread is tapped.
 * @param onContactClick Callback invoked when a contact is tapped.
 * @param onNewChatClick Callback invoked when the FAB is pressed on the Chats tab.
 * @param onCallClick    Callback invoked when a call is initiated.
 * @param unreadChatCount    Unread count for the Chats tab badge.
 * @param unreadCallsCount   Unread count for the Calls tab badge.
 * @param currentUserAvatarUrl  URL of the current user's avatar.
 * @param currentUserDisplayName Display name of the current user.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onChatClick: (threadId: String) -> Unit = {},
    onGroupChatClick: (threadId: String) -> Unit = {},
    onContactClick: (contactUserId: String) -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onCallClick: (callId: String) -> Unit = {},
    unreadChatCount: Int = 0,
    unreadCallsCount: Int = 0,
    currentUserAvatarUrl: String? = null,
    currentUserDisplayName: String = ""
) {
    var currentTab by remember { mutableIntStateOf(selectedTabIndex) }

    // Sync with external tab selection
    if (selectedTabIndex != currentTab) {
        currentTab = selectedTabIndex
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Full-screen animated glass background ──
        ZixoGlassBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                // ── 85dp Liquid Glass Bottom Nav ─────
                HomeBottomNav(
                    selectedTab = currentTab,
                    onTabSelected = { index ->
                        currentTab = index
                        onTabSelected(index)
                    },
                    unreadChatCount = unreadChatCount,
                    unreadCallsCount = unreadCallsCount
                )
            },
            floatingActionButton = {
                // ── Mint Green FAB ───────────────────
                HomeFab(
                    currentTab = currentTab,
                    onNewChatClick = onNewChatClick
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ── Branded Top Bar ────────────────────
                HomeTopBar(
                    currentUserAvatarUrl = currentUserAvatarUrl,
                    currentUserDisplayName = currentUserDisplayName
                )

                // ── Tab Content Area ──────────────────
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> direction * fullWidth },
                            animationSpec = tween(300)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -direction * fullWidth },
                            animationSpec = tween(300)
                        )
                    },
                    label = "home_tab_transition"
                ) { tab ->
                    when (tab) {
                        HomeTab.CHATS.index -> ChatsTabContent(
                            onChatClick = onChatClick,
                            onGroupChatClick = onGroupChatClick
                        )

                        HomeTab.STATUS.index -> StatusTabContent()

                        HomeTab.CALLS.index -> CallsTabContent()

                        HomeTab.CONTACTS.index -> ContactsTabContent(
                            onContactClick = onContactClick
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Home Top Bar
// ──────────────────────────────────────────────

/**
 * Custom top bar with "Zixo" branding and the user's avatar.
 */
@Composable
private fun HomeTopBar(
    currentUserAvatarUrl: String?,
    currentUserDisplayName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Zixo Branding ─────────────────────────
        Text(
            text = "Zixo",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        // ── User Avatar ───────────────────────────
        AvatarComponent(
            imageUrl = currentUserAvatarUrl,
            name = currentUserDisplayName,
            size = 36.dp,
            isOnline = true
        )
    }
}

// ──────────────────────────────────────────────
// 85dp Liquid Glass Bottom Navigation
// ──────────────────────────────────────────────

/**
 * iOS Liquid Glass-styled bottom navigation bar, exactly 85dp height.
 *
 * Features:
 * - [liquidGlassNavItem] modifier for the frosted glass visual
 * - 4 tabs: Chats, Status, Calls, Contacts
 * - Unread count badges on Chats and Calls tabs
 * - NeonMint accent for selected state
 * - Smooth color transitions on selection change
 * - Floats above the animated ZixoGlassBackground
 */
@Composable
private fun HomeBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    unreadChatCount: Int = 0,
    unreadCallsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .height(85.dp)
            .liquidGlassNavItem(),
        containerColor = Color.Transparent,
        contentColor = NeonMint
    ) {
        HomeTab.entries.forEach { tab ->
            val isSelected = tab.index == selectedTab

            // Badge counts per tab
            val badgeCount = when (tab) {
                HomeTab.CHATS -> unreadChatCount
                HomeTab.CALLS -> unreadCallsCount
                else -> 0
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab.index) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge(
                                    containerColor = NeonMint,
                                    contentColor = DarkPetrolCharcoal
                                ) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonMint,
                    selectedTextColor = NeonMint,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = NeonMint.copy(alpha = 0.15f)
                )
            )
        }
    }
}

// ──────────────────────────────────────────────
// Home FAB
// ──────────────────────────────────────────────

/**
 * Floating action button styled with Liquid Glass in NeonMint green.
 *
 * The icon varies based on the current tab:
 * - Chats tab: Edit (new chat)
 * - Status tab: Edit (new status)
 * - Calls tab: Dial pad
 * - Contacts tab: Person add (find contact)
 */
@Composable
private fun HomeFab(
    currentTab: Int,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tab = HomeTab.entries.find { it.index == currentTab } ?: HomeTab.CHATS

    FloatingActionButton(
        onClick = onNewChatClick,
        modifier = modifier
            .clip(CircleShape),
        containerColor = NeonMint,
        contentColor = DarkPetrolCharcoal,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp
        )
    ) {
        Icon(
            imageVector = tab.fabIcon,
            contentDescription = tab.fabLabel,
            modifier = Modifier.size(24.dp),
            tint = DarkPetrolCharcoal
        )
    }
}

// ──────────────────────────────────────────────
// Tab Content Stubs
// ──────────────────────────────────────────────

/**
 * Chats tab — delegates to the full [ChatsScreen] composable.
 * Will be connected to ChatViewModel by other agents.
 */
@Composable
fun ChatsTabContent(
    onChatClick: (threadId: String) -> Unit = {},
    onGroupChatClick: (threadId: String) -> Unit = {}
) {
    ChatsScreen(onChatClick = onChatClick)
}

/**
 * Status tab — delegates to the full [StatusTabScreen] composable.
 * Will be connected to StatusViewModel by other agents.
 */
@Composable
fun StatusTabContent() {
    StatusTabScreen()
}

/**
 * Calls tab — delegates to the full [CallsScreen] composable.
 * Will be connected to CallsViewModel by other agents.
 */
@Composable
fun CallsTabContent() {
    CallsScreen()
}

/**
 * Contacts tab — delegates to the full [ContactListScreen] composable.
 * Connected to [ContactListViewModel] via Hilt injection.
 */
@Composable
fun ContactsTabContent(
    onContactClick: (contactUserId: String) -> Unit = {}
) {
    ContactListScreen(onContactClick = onContactClick)
}
