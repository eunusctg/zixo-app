package com.zexo.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.zexo.app.data.model.CallRecord
import com.zexo.app.data.model.Chat
import com.zexo.app.data.model.Status
import com.zexo.app.data.model.User
import com.zexo.app.ui.navigation.HomeTab
import com.zexo.app.ui.navigation.Screen
import com.zexo.app.ui.theme.*
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════
//  Main HomeScreen composable — Bottom Navigation layout
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // FAB expanded state
    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ZexoBackground,
        topBar = {
            HomeTopBar(
                currentUser = currentUser,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onClearSearch = { viewModel.clearSearch() },
                isSearching = isSearching,
                onAvatarClick = {
                    currentUser?.uid?.let { uid ->
                        navController.navigate(Screen.Profile.createRoute(uid))
                    }
                },
                onAdminClick = {
                    navController.navigate(Screen.AdminLanding.route)
                },
                isAdmin = viewModel.isAdmin
            )
        },
        floatingActionButton = {
            AnimatedFab(
                selectedTab = selectedTab,
                expanded = fabExpanded,
                onExpandToggle = { fabExpanded = !fabExpanded },
                onDismiss = { fabExpanded = false },
                navController = navController
            )
        },
        bottomBar = {
            ZexoBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZexoBackground)
        ) {
            when (selectedTab) {
                HomeTab.CHATS -> ChatsTabContent(viewModel, navController)
                HomeTab.STATUS -> StatusTabContent(viewModel, navController)
                HomeTab.CALLS -> CallsTabContent(viewModel, navController)
                HomeTab.SETTINGS -> SettingsTabContent(viewModel, navController)
            }
        }
    }

    // Dismiss FAB menu on tab change
    LaunchedEffect(selectedTab) {
        fabExpanded = false
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Bottom Navigation Bar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ZexoBottomNavBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    Surface(
        color = ZexoSurface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = ZexoTextPrimary,
            tonalElevation = 0.dp,
            modifier = Modifier.height(64.dp)
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val icon: ImageVector = when (tab) {
                    HomeTab.CHATS -> if (isSelected) Icons.Filled.Chat else Icons.Outlined.Chat
                    HomeTab.STATUS -> if (isSelected) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt
                    HomeTab.CALLS -> if (isSelected) Icons.Filled.Call else Icons.Outlined.Call
                    HomeTab.SETTINGS -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                }

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ZexoPrimary,
                        selectedTextColor = ZexoPrimary,
                        unselectedIconColor = ZexoTextSecondary,
                        unselectedTextColor = ZexoTextSecondary,
                        indicatorColor = ZexoPrimary.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Top bar with search
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun HomeTopBar(
    currentUser: User?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    isSearching: Boolean,
    onAvatarClick: () -> Unit,
    onAdminClick: () -> Unit,
    isAdmin: Boolean
) {
    var searchActive by remember { mutableStateOf(false) }

    Surface(
        color = ZexoSurface,
        tonalElevation = 2.dp
    ) {
        Column {
            // Main header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ZexoPrimary)
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser?.avatar.isNullOrBlank()) {
                        Text(
                            text = currentUser?.displayName?.take(1)?.uppercase() ?: "Z",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    } else {
                        AsyncImage(
                            model = currentUser!!.avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Zixo number
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zixo",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZexoTextPrimary
                    )
                    currentUser?.zixoNumber?.let { zixo ->
                        Text(
                            text = zixo,
                            fontSize = 11.sp,
                            color = ZexoSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action icons
                if (isAdmin) {
                    IconButton(onClick = onAdminClick) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = ZexoSecondary
                        )
                    }
                }

                IconButton(onClick = { searchActive = !searchActive }) {
                    Icon(
                        if (searchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ZexoTextPrimary
                    )
                }
            }

            // Search bar
            AnimatedVisibility(
                visible = searchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = {
                        Text("Search chats, people\u2026", color = ZexoTextSecondary)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = ZexoTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = ZexoTextSecondary
                                )
                            }
                        }
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ZexoPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZexoPrimary,
                        unfocusedBorderColor = ZexoSurfaceLight,
                        focusedContainerColor = ZexoBackground,
                        unfocusedContainerColor = ZexoBackground,
                        focusedTextColor = ZexoTextPrimary,
                        unfocusedTextColor = ZexoTextPrimary,
                        cursorColor = ZexoPrimary
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Animated FAB with dropdown menus
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun AnimatedFab(
    selectedTab: HomeTab,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onDismiss: () -> Unit,
    navController: NavHostController
) {
    // Only show FAB on Chats and Status tabs
    if (selectedTab != HomeTab.CHATS && selectedTab != HomeTab.STATUS) return

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(300),
        label = "fabRotation"
    )

    val fabColor = when (selectedTab) {
        HomeTab.CHATS -> ZexoPrimary
        HomeTab.STATUS -> ZexoGreen
        else -> ZexoPrimary
    }

    Box(
        modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
    ) {
        // Dropdown menu items (shown above FAB)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                when (selectedTab) {
                    HomeTab.CHATS -> ChatFabMenu(navController, onDismiss)
                    HomeTab.STATUS -> StatusFabMenu(navController, onDismiss)
                    else -> {}
                }
            }
        }

        // Main FAB button
        FloatingActionButton(
            onClick = onExpandToggle,
            containerColor = fabColor,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Action",
                tint = Color.White,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabMenuItem(
    icon: ImageVector,
    label: String,
    backgroundColor: Color = ZexoSurfaceLight,
    iconTint: Color = ZexoPrimary,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = ZexoTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ChatFabMenu(navController: NavHostController, onDismiss: () -> Unit) {
    FabMenuItem(
        icon = Icons.Default.QrCodeScanner,
        label = "Scan QR",
        iconTint = ZexoSecondary
    ) {
        onDismiss()
        navController.navigate(Screen.QRScanner.route)
    }
    FabMenuItem(
        icon = Icons.Default.Chat,
        label = "New Chat",
        iconTint = ZexoPrimary
    ) {
        onDismiss()
        navController.navigate(Screen.NewChat.route)
    }
}

@Composable
private fun StatusFabMenu(navController: NavHostController, onDismiss: () -> Unit) {
    FabMenuItem(
        icon = Icons.Default.CameraAlt,
        label = "New Status",
        iconTint = ZexoGreen
    ) {
        onDismiss()
        navController.navigate(Screen.NewStatus.route)
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Chats Tab
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ChatsTabContent(
    viewModel: HomeViewModel,
    navController: NavHostController
) {
    val chats by viewModel.filteredChats.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ZexoPrimary)
        }
        return
    }

    // Show search results if searching
    if (searchQuery.isNotBlank()) {
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = ZexoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No results for \"$searchQuery\"",
                        color = ZexoTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(searchResults, key = { it.uid }) { user ->
                    SearchUserItem(
                        user = user,
                        isOnline = viewModel.isUserOnline(user.uid),
                        onClick = {
                            navController.navigate(Screen.Profile.createRoute(user.uid))
                        }
                    )
                }
            }
        }
        return
    }

    if (chats.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Chat,
            title = "No conversations yet",
            subtitle = "Start a new chat to connect with people"
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(chats, key = { it.id }) { chat ->
            ChatItem(
                chat = chat,
                viewModel = viewModel,
                onClick = {
                    val otherUser = viewModel.getOtherUser(chat)
                    val otherUserId = otherUser?.uid ?: chat.participants.firstOrNull { it != viewModel.currentUser.value?.uid } ?: ""
                    navController.navigate(Screen.Chat.createRoute(chat.id, otherUserId))
                },
                onAvatarClick = {
                    val otherUser = viewModel.getOtherUser(chat)
                    otherUser?.uid?.let { uid ->
                        navController.navigate(Screen.Profile.createRoute(uid))
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatItem(
    chat: Chat,
    viewModel: HomeViewModel,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val otherUser = viewModel.getOtherUser(chat)
    val unread = viewModel.getUnreadCount(chat)
    val isOnline = otherUser?.uid?.let { viewModel.isUserOnline(it) } == true
    val isGroup = chat.isGroup

    val displayName = if (isGroup) chat.groupName else otherUser?.displayName ?: "Unknown"
    val avatarUrl = if (isGroup) chat.groupAvatar else otherUser?.avatar ?: ""
    val typing = chat.typing.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(ZexoBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ZexoSurfaceLight)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isBlank()) {
                    if (isGroup) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = ZexoTextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Text(
                            text = displayName.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZexoPrimary
                        )
                    }
                } else {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            // Online dot
            if (isOnline && !isGroup) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(ZexoOnline)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(ZexoOnline)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name + last message
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZexoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Time
                chat.lastMessageTime?.let { ts ->
                    Text(
                        text = formatTimestamp(ts),
                        fontSize = 11.sp,
                        color = if (unread > 0) ZexoPrimary else ZexoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (typing) {
                    Text(
                        text = "typing\u2026",
                        fontSize = 13.sp,
                        color = ZexoPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = chat.lastMessage.ifBlank { "No messages yet" },
                        fontSize = 13.sp,
                        color = ZexoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unread badge
                if (unread > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(minOf(22.dp + (unread.toString().length * 4).dp, 36.dp))
                            .height(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(ZexoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unread > 99) "99+" else unread.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchUserItem(
    user: User,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(ZexoBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ZexoSurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatar.isBlank()) {
                    Text(
                        text = user.displayName.take(1).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZexoPrimary
                    )
                } else {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ZexoOnline)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = user.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZexoTextPrimary
            )
            Text(
                text = user.username,
                fontSize = 12.sp,
                color = ZexoTextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Status Tab
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun StatusTabContent(
    viewModel: HomeViewModel,
    navController: NavHostController
) {
    val groupedStatuses by viewModel.groupedStatuses.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // My Status
        item {
            val myStatuses = groupedStatuses[currentUser?.uid]
            MyStatusItem(
                hasStatus = myStatuses.isNullOrEmpty().not(),
                onClick = {
                    if (myStatuses.isNullOrEmpty()) {
                        navController.navigate(Screen.NewStatus.route)
                    } else {
                        myStatuses.firstOrNull()?.id?.let { sid ->
                            navController.navigate(Screen.StatusView.createRoute(sid))
                        }
                    }
                }
            )
        }

        if (groupedStatuses.isNotEmpty()) {
            item {
                Text(
                    text = "Recent updates",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZexoTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Other people's statuses
        groupedStatuses.entries
            .filter { it.key != currentUser?.uid }
            .forEach { (userId, userStatuses) ->
                item(key = "status_$userId") {
                    UserStatusItem(
                        statuses = userStatuses,
                        isOnline = viewModel.isUserOnline(userId),
                        currentUid = currentUser?.uid ?: "",
                        onClick = {
                            userStatuses.firstOrNull()?.id?.let { sid ->
                                navController.navigate(Screen.StatusView.createRoute(sid))
                            }
                        },
                        onAvatarClick = {
                            navController.navigate(Screen.Profile.createRoute(userId))
                        }
                    )
                }
            }

        if (groupedStatuses.size <= 1) {
            item {
                EmptyState(
                    icon = Icons.Default.CameraAlt,
                    title = "No status updates",
                    subtitle = "Statuses from your contacts will appear here"
                )
            }
        }
    }
}

@Composable
private fun MyStatusItem(
    hasStatus: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(ZexoBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ZexoSurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = ZexoTextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
            // Add button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ZexoPrimary)
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add status",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "My Status",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZexoTextPrimary
            )
            Text(
                text = if (hasStatus) "Tap to view" else "Tap to add status update",
                fontSize = 13.sp,
                color = ZexoTextSecondary
            )
        }
    }
}

@Composable
private fun UserStatusItem(
    statuses: List<Status>,
    isOnline: Boolean,
    currentUid: String,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val first = statuses.firstOrNull() ?: return
    val seen = statuses.all { currentUid in it.seenBy }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(ZexoBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status ring avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (seen) ZexoSurfaceLight else ZexoPrimary
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(ZexoBackground)
                .padding(2.dp)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            if (first.userAvatar.isBlank()) {
                Text(
                    text = first.userName.take(1).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZexoPrimary
                )
            } else {
                AsyncImage(
                    model = first.userAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .offset(x = (-14).dp, y = 14.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ZexoOnline)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = first.userName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZexoTextPrimary
            )
            Text(
                text = formatTimestamp(first.createdAt),
                fontSize = 13.sp,
                color = ZexoTextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Calls Tab
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CallsTabContent(
    viewModel: HomeViewModel,
    navController: NavHostController
) {
    val callHistory by viewModel.callHistory.collectAsState()

    if (callHistory.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Call,
            title = "No call history",
            subtitle = "Your call history will appear here"
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(callHistory, key = { it.id }) { record ->
            CallItem(
                record = record,
                onClick = {
                    val uid = if (record.direction == "outgoing") record.receiverId else record.callerId
                    navController.navigate(Screen.Profile.createRoute(uid))
                }
            )
        }
    }
}

@Composable
private fun CallItem(
    record: CallRecord,
    onClick: () -> Unit
) {
    val isOutgoing = record.direction == "outgoing"
    val isVideo = record.type == "video"

    val directionIcon = when (record.direction) {
        "outgoing" -> Icons.Default.CallMade
        "incoming" -> Icons.Default.CallReceived
        "missed" -> Icons.Default.CallEnd
        else -> Icons.Default.Call
    }
    val directionTint = when (record.direction) {
        "missed" -> ZexoRed
        "incoming" -> ZexoGreen
        else -> ZexoTextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(ZexoBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(ZexoSurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            val avatarUrl = if (isOutgoing) record.receiverAvatar else record.callerAvatar
            val name = if (isOutgoing) record.receiverName else record.callerName
            if (avatarUrl.isBlank()) {
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZexoPrimary
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isOutgoing) record.receiverName else record.callerName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (record.direction == "missed") ZexoRed else ZexoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = directionIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = directionTint
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatCallInfo(record),
                    fontSize = 13.sp,
                    color = ZexoTextSecondary
                )
            }
        }

        // Call action
        IconButton(onClick = onClick) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Call back",
                tint = ZexoSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Settings Tab — Shows settings inline (no navigation needed)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsTabContent(
    viewModel: HomeViewModel,
    navController: NavHostController
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Logout", fontWeight = FontWeight.SemiBold, color = ZexoTextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to logout?",
                    color = ZexoTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    // Navigate to auth and clear back stack
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Logout", color = ZexoRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = ZexoTextSecondary)
                }
            },
            containerColor = ZexoSurface
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ZexoBackground),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Profile card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(Screen.ProfileEdit.route)
                    }
                    .background(ZexoBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GradientProfileStart, GradientProfileEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser?.avatar.isNullOrBlank()) {
                        Text(
                            text = currentUser?.displayName?.take(1)?.uppercase() ?: "Z",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        AsyncImage(
                            model = currentUser!!.avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.displayName ?: "Unknown",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZexoTextPrimary
                    )
                    Text(
                        text = currentUser?.bio ?: "",
                        fontSize = 13.sp,
                        color = ZexoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    currentUser?.zixoNumber?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = ZexoSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = { navController.navigate(Screen.ProfileEdit.route) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = ZexoPrimary
                    )
                }
            }
        }

        item { SettingsSectionDivider() }

        // Account
        item {
            SettingsSectionLabel("Account")
        }
        item {
            SettingsRow(
                icon = Icons.Default.Person,
                label = "Edit Profile",
                tint = ZexoPrimary
            ) { navController.navigate(Screen.ProfileEdit.route) }
        }
        item {
            SettingsRow(
                icon = Icons.Default.Key,
                label = "Change Password",
                tint = ZexoOrange
            ) { navController.navigate(Screen.Settings.route) }
        }

        item { SettingsSectionDivider() }

        // Preferences
        item {
            SettingsSectionLabel("Preferences")
        }
        item {
            SettingsRow(
                icon = Icons.Default.Palette,
                label = "Appearance & Theme",
                tint = ZexoSecondary
            ) { navController.navigate(Screen.Settings.route) }
        }
        item {
            SettingsRow(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                tint = ZexoOrange
            ) { navController.navigate(Screen.Settings.route) }
        }

        item { SettingsSectionDivider() }

        // Privacy
        item {
            SettingsSectionLabel("Privacy & Security")
        }
        item {
            SettingsRow(
                icon = Icons.Default.Lock,
                label = "Privacy Settings",
                tint = ZexoPrimary
            ) { navController.navigate(Screen.Settings.route) }
        }
        item {
            SettingsRow(
                icon = Icons.Default.Fingerprint,
                label = "Biometric Lock",
                tint = ZexoGreen
            ) { navController.navigate(Screen.Settings.route) }
        }

        item { SettingsSectionDivider() }

        // About & Logout
        item {
            SettingsSectionLabel("About")
        }
        item {
            SettingsRow(
                icon = Icons.Default.Info,
                label = "About Zixo",
                tint = ZexoTextSecondary
            ) { /* Show version info */ }
        }

        item { SettingsSectionDivider() }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
                    .background(ZexoBackground)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ZexoRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = ZexoRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Log Out",
                    fontSize = 15.sp,
                    color = ZexoRed,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(ZexoBackground)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = ZexoTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = ZexoTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = ZexoPrimary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = ZexoSurfaceLight,
        thickness = 1.dp
    )
}

// ═══════════════════════════════════════════════════════════════════════
//  Shared composables
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ZexoSurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = ZexoTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZexoTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = ZexoTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Utility functions
// ═══════════════════════════════════════════════════════════════════════

private fun formatTimestamp(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> {
            val date = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            date.format(java.util.Date(ts))
        }
    }
}

private fun formatCallInfo(record: CallRecord): String {
    val time = record.timestamp?.let { formatTimestamp(it) } ?: ""
    val duration = if (record.duration > 0) {
        val mins = record.duration / 60
        val secs = record.duration % 60
        "($mins:${secs.toString().padStart(2, '0')})"
    } else ""
    val direction = when (record.direction) {
        "outgoing" -> "Outgoing"
        "incoming" -> "Incoming"
        "missed" -> "Missed"
        else -> record.direction.replaceFirstChar { it.uppercase() }
    }
    return "$direction $duration $time".trim()
}
