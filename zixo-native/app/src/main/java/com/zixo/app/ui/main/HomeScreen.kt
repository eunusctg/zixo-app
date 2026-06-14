package com.zixo.app.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.ui.components.AvatarComponent
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.contacts.ContactListScreen
import com.zixo.app.ui.screens.calls.CallsScreen
import com.zixo.app.ui.screens.chats.ChatsScreen
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ──────────────────────────────────────────────
// Tab Configuration
// ──────────────────────────────────────────────

/**
 * Represents a tab in the home screen navigation.
 */
enum class HomeTab(val index: Int, val label: String, val icon: ImageVector) {
    CHATS(0, "Chats", Icons.Outlined.Chat),
    CONTACTS(1, "Contacts", Icons.Outlined.Contacts),
    CALLS(2, "Calls", Icons.Outlined.Call),
    STATUS(3, "Status", Icons.Outlined.Edit)
}

// ──────────────────────────────────────────────
// Home Screen
// ──────────────────────────────────────────────

/**
 * The main home screen that hosts tab navigation and content.
 *
 * This replaces the old [ChatsScreen] as the primary view and provides:
 * - [ZixoGlassBackground] for the animated blob background
 * - Top bar with "Zixo" branding and the user's avatar
 * - Tab content area that switches between Chats, Contacts, Calls, and Status
 * - FAB button styled with Liquid Glass for starting new chats
 * - Observes the current selected tab from the bottom nav
 *
 * @param selectedTabIndex The currently selected tab index from the bottom navigation.
 * @param onTabSelected    Callback invoked when the user selects a tab.
 * @param onChatClick      Callback invoked when a chat thread is tapped.
 * @param onContactClick   Callback invoked when a contact is tapped.
 * @param onNewChatClick   Callback invoked when the FAB is pressed.
 * @param currentUserAvatarUrl The URL of the current user's avatar for the top bar.
 * @param currentUserDisplayName The display name of the current user.
 */
@Composable
fun HomeScreen(
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onChatClick: (threadId: String) -> Unit = {},
    onContactClick: (contactUserId: String) -> Unit = {},
    onNewChatClick: () -> Unit = {},
    currentUserAvatarUrl: String? = null,
    currentUserDisplayName: String = ""
) {
    var currentTab by remember { mutableIntStateOf(selectedTabIndex) }

    // Sync with external tab selection
    if (selectedTabIndex != currentTab) {
        currentTab = selectedTabIndex
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                HomeTopBar(
                    currentUserAvatarUrl = currentUserAvatarUrl,
                    currentUserDisplayName = currentUserDisplayName
                )
            },
            floatingActionButton = {
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
                // ── Tab Strip ────────────────────────────
                HomeTabStrip(
                    selectedTab = currentTab,
                    onTabSelected = { index ->
                        currentTab = index
                        onTabSelected(index)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Tab Content Area ─────────────────────
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth }) togetherWith
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -direction * fullWidth })
                    },
                    label = "home_tab_transition"
                ) { tab ->
                    when (tab) {
                        HomeTab.CHATS.index -> {
                            ChatsScreen(onChatClick = onChatClick)
                        }

                        HomeTab.CONTACTS.index -> {
                            ContactListScreen(onContactClick = onContactClick)
                        }

                        HomeTab.CALLS.index -> {
                            CallsScreen()
                        }

                        HomeTab.STATUS.index -> {
                            StatusPlaceholder()
                        }
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
 * Replaces the standard ZixoTopBar to provide a branded home header.
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
// Home Tab Strip
// ──────────────────────────────────────────────

/**
 * Horizontal tab strip for switching between Chats, Contacts, Calls, and Status.
 *
 * Uses the Liquid Glass design with a glass panel background and
 * highlighted selection indicator on the active tab.
 */
@Composable
private fun HomeTabStrip(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = HomeTab.entries

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .liquidGlassCard()
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.index == selectedTab

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isSelected) Modifier.liquidGlassCard() else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab.index) }
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (isSelected) NeonMint else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = tab.label,
                    color = if (isSelected) NeonMint else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Home FAB
// ──────────────────────────────────────────────

/**
 * Floating action button styled with Liquid Glass.
 * The icon varies based on the current tab:
 * - Chats tab: Edit (new chat)
 * - Contacts tab: Person add (find contact)
 * - Calls tab: Dial pad
 * - Status tab: Edit (new status)
 */
@Composable
private fun HomeFab(
    currentTab: Int,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fabIcon = when (currentTab) {
        HomeTab.CHATS.index -> Icons.Outlined.Edit
        HomeTab.CONTACTS.index -> Icons.Outlined.PersonAdd
        HomeTab.CALLS.index -> Icons.Outlined.Dialpad
        HomeTab.STATUS.index -> Icons.Outlined.Edit
        else -> Icons.Outlined.Edit
    }

    val fabDescription = when (currentTab) {
        HomeTab.CHATS.index -> "New chat"
        HomeTab.CONTACTS.index -> "Find contact"
        HomeTab.CALLS.index -> "Dial pad"
        HomeTab.STATUS.index -> "New status"
        else -> "Action"
    }

    FloatingActionButton(
        onClick = onNewChatClick,
        modifier = modifier
            .clip(CircleShape)
            .liquidGlassCard(),
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
            imageVector = fabIcon,
            contentDescription = fabDescription,
            modifier = Modifier.size(24.dp),
            tint = DarkPetrolCharcoal
        )
    }
}

// ──────────────────────────────────────────────
// Status Placeholder
// ──────────────────────────────────────────────

/**
 * Placeholder screen for the Status tab.
 * Will be replaced with a full status implementation in a future iteration.
 */
@Composable
private fun StatusPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Status",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Status updates from your contacts\nwill appear here",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
