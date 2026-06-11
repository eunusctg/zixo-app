package com.zexo.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
//  Main HomeScreen composable
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

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { HomeTab.entries.size }
    )
    val scope = rememberCoroutineScope()

    // Sync pager ↔ tabs
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(HomeTab.entries[pagerState.currentPage])
    }
    LaunchedEffect(selectedTab) {
        val idx = HomeTab.entries.indexOf(selectedTab)
        if (pagerState.currentPage != idx) {
            pagerState.animateScrollToPage(idx)
        }
    }

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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZexoBackground)
        ) {
            // Tab row
            ZexoTabRow(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    viewModel.selectTab(tab)
                    scope.launch {
                        val idx = HomeTab.entries.indexOf(tab)
                        pagerState.animateScrollToPage(idx)
                    }
                }
            )

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 3
            ) { page ->
                when (HomeTab.entries[page]) {
                    HomeTab.CHATS -> ChatsTabContent(viewModel, navController)
                    HomeTab.STATUS -> StatusTabContent(viewModel, navController)
                    HomeTab.CALLS -> CallsTabContent(viewModel, navController)
                    HomeTab.SETTINGS -> SettingsTabContent(viewModel, navController)
                }
            }
        }
    }

    // Dismiss FAB menu on tab change
    LaunchedEffect(selectedTab) {
        fabExpanded = false
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
                        text = "Zexo",
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
                        Text("Search chats, people…", color = ZexoTextSecondary)
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
//  Tab row
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ZexoTabRow(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    Surface(
        color = ZexoSurface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                ZexoTabItem(
                    tab = tab,
                    selected = isSelected,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun ZexoTabItem(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (tab) {
        HomeTab.CHATS -> Icons.Default.Chat
        HomeTab.STATUS -> Icons.Default.CameraAlt
        HomeTab.CALLS -> Icons.Default.Call
        HomeTab.SETTINGS -> Icons.Default.Settings
    }
    val selectedIcon: ImageVector = when (tab) {
        HomeTab.CHATS -> Icons.Filled.Chat
        HomeTab.STATUS -> Icons.Filled.CameraAlt
        HomeTab.CALLS -> Icons.Filled.Call
        HomeTab.SETTINGS -> Icons.Filled.Settings
    }

    val animatedColor by animateColorAsState(
        targetValue = if (selected) ZexoPrimary else ZexoTextSecondary,
        animationSpec = tween(250), label = "tabColor"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(if (selected) ZexoPrimary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = tab.label,
            tint = animatedColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = animatedColor
        )
        // Indicator dot
        AnimatedVisibility(visible = selected) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(16.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ZexoPrimary)
            )
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
    // Only show FAB on Chats and Calls tabs
    if (selectedTab != HomeTab.CHATS && selectedTab != HomeTab.CALLS) return

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(300),
        label = "fabRotation"
    )

    val fabIcon: ImageVector = when (selectedTab) {
        HomeTab.CHATS -> Icons.Default.Chat
        HomeTab.CALLS -> Icons.Default.Call
        else -> Icons.Default.Add
    }

    val fabColor = when (selectedTab) {
        HomeTab.CHATS -> ZexoPrimary
        HomeTab.CALLS -> ZexoSecondary
        else -> ZexoPrimary
    }

    Box(
        modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
    ) {
        // Dimming overlay
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            // Invisible click-away layer – handled by onDismiss calls elsewhere
        }

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
                    HomeTab.CALLS -> CallsFabMenu(navController, onDismiss)
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
        icon = Icons.Default.Star,
        label = "New Status",
        iconTint = ZexoGreen
    ) {
        onDismiss()
        navController.navigate(Screen.NewStatus.route)
    }
    FabMenuItem(
        icon = Icons.Default.Group,
        label = "New Group",
        iconTint = ZexoBlue
    ) {
        onDismiss()
        // TODO: Navigate to NewGroup screen
        navController.navigate(Screen.NewChat.route)
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
private fun CallsFabMenu(navController: NavHostController, onDismiss: () -> Unit) {
    FabMenuItem(
        icon = Icons.Default.QrCodeScanner,
        label = "Scan QR",
        iconTint = ZexoSecondary
    ) {
        onDismiss()
        navController.navigate(Screen.QRScanner.route)
    }
    FabMenuItem(
        icon = Icons.Default.PersonAdd,
        label = "New Contact",
        iconTint = ZexoGreen
    ) {
        onDismiss()
        // TODO: Navigate to NewContact screen
        navController.navigate(Screen.NewChat.route)
    }
    FabMenuItem(
        icon = Icons.Default.Dialpad,
        label = "Dial Pad",
        iconTint = ZexoOrange
    ) {
        onDismiss()
        navController.navigate(Screen.DialPad.route)
    }
    FabMenuItem(
        icon = Icons.Default.Call,
        label = "New Call",
        iconTint = ZexoPrimary
    ) {
        onDismiss()
        navController.navigate(Screen.NewChat.route)
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
                    navController.navigate(Screen.Chat.createRoute(chat.id))
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
                        text = "typing…",
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
    val statuses by viewModel.statuses.collectAsState()
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
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val first = statuses.firstOrNull() ?: return
    val seen = statuses.all { currentUserUid()?.let { uid -> uid in it.seenBy } == true }

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
                    // Navigate to user profile
                    val uid = record.callerId
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
//  Settings Tab (placeholder)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsTabContent(
    viewModel: HomeViewModel,
    navController: NavHostController
) {
    val currentUser by viewModel.currentUser.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Profile card
        item {
            SettingsProfileCard(
                user = currentUser,
                onClick = {
                    currentUser?.uid?.let { uid ->
                        navController.navigate(Screen.Profile.createRoute(uid))
                    }
                },
                onEditClick = {
                    navController.navigate(Screen.ProfileEdit.route)
                }
            )
        }

        // Settings items
        item { SettingsDivider() }

        item {
            SettingsItem(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                tint = ZexoOrange
            ) { navController.navigate(Screen.Settings.route) }
        }
        item {
            SettingsItem(
                icon = Icons.Default.Lock,
                label = "Privacy & Security",
                tint = ZexoPrimary
            ) { navController.navigate(Screen.Settings.route) }
        }
        item {
            SettingsItem(
                icon = Icons.Default.Palette,
                label = "Appearance",
                tint = ZexoSecondary
            ) { navController.navigate(Screen.Settings.route) }
        }
        item {
            SettingsItem(
                icon = Icons.Default.Storage,
                label = "Data & Storage",
                tint = ZexoBlue
            ) { navController.navigate(Screen.Settings.route) }
        }
        item {
            SettingsItem(
                icon = Icons.Default.Language,
                label = "Language",
                tint = ZexoGreen
            ) { navController.navigate(Screen.Settings.route) }
        }

        item { SettingsDivider() }

        item {
            SettingsItem(
                icon = Icons.Default.Help,
                label = "Help & Support",
                tint = ZexoTextSecondary
            ) {}
        }
        item {
            SettingsItem(
                icon = Icons.Default.Info,
                label = "About Zexo",
                tint = ZexoTextSecondary
            ) {}
        }

        item { SettingsDivider() }

        item {
            SettingsItem(
                icon = Icons.Default.Logout,
                label = "Log Out",
                tint = ZexoRed
            ) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}

@Composable
private fun SettingsProfileCard(
    user: User?,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(ZexoBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(ZexoPrimary),
            contentAlignment = Alignment.Center
        ) {
            if (user?.avatar.isNullOrBlank()) {
                Text(
                    text = user?.displayName?.take(1)?.uppercase() ?: "Z",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                AsyncImage(
                    model = user!!.avatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.displayName ?: "Unknown",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZexoTextPrimary
            )
            Text(
                text = user?.bio ?: "",
                fontSize = 13.sp,
                color = ZexoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            user?.zixoNumber?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = ZexoSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(onClick = onEditClick) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = ZexoPrimary
            )
        }
    }
}

@Composable
private fun SettingsItem(
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
private fun SettingsDivider() {
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

// Helper to get current user UID inside composable context
@Composable
private fun currentUserUid(): String? {
    val viewModel: HomeViewModel = hiltViewModel()
    return viewModel.currentUser.collectAsState().value?.uid
}
