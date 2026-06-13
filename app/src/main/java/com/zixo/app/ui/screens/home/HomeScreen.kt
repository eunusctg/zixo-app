package com.zixo.app.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zixo.app.data.model.CallRecord
import com.zixo.app.data.model.Chat
import com.zixo.app.data.model.ZixoUser
import com.zixo.app.ui.screens.settings.SettingsScreen
import com.zixo.app.ui.screens.status.StatusTabScreen
import com.zixo.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onChatClick: (String, String) -> Unit,
    onNewChatClick: () -> Unit,
    onEditProfile: () -> Unit,
    onCreateStatus: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchActive by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ZixoBg,
        bottomBar = {
            ZixoBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = onNewChatClick,
                    containerColor = ZixoPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "New Chat", tint = ZixoBg)
                }
                1 -> FloatingActionButton(
                    onClick = { },
                    containerColor = ZixoPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Dialpad, contentDescription = "Dial", tint = ZixoBg)
                }
                2 -> FloatingActionButton(
                    onClick = onCreateStatus,
                    containerColor = ZixoPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add Status", tint = ZixoBg)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ZixoBg)
        ) {
            TopBar(
                searchActive = searchActive,
                onSearchToggle = { searchActive = it },
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                selectedTab = selectedTab
            )

            if (selectedTab == 1) {
                CallFilterTabs(
                    selectedFilter = uiState.selectedCallFilter,
                    onFilterChange = viewModel::onCallFilterChange
                )
            }

            when (selectedTab) {
                0 -> ChatsList(
                    chats = viewModel.getFilteredChats(),
                    userProfiles = uiState.userProfiles,
                    currentUserId = currentUser?.uid ?: "",
                    onChatClick = onChatClick,
                    isLoading = uiState.isLoading
                )
                1 -> CallsList(
                    calls = viewModel.getFilteredCalls(),
                    userProfiles = uiState.userProfiles,
                    currentUserId = currentUser?.uid ?: "",
                    isLoading = uiState.isLoading
                )
                2 -> StatusTabScreen(
                    onCreateStatus = onCreateStatus
                )
                3 -> SettingsScreen(
                    onBack = { selectedTab = 0 },
                    onEditProfile = onEditProfile
                )
            }
        }
    }
}

@Composable
private fun ZixoBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        "Chats" to Icons.Default.Chat,
        "Calls" to Icons.Default.Call,
        "Status" to Icons.Default.AddCircle,
        "Settings" to Icons.Default.Settings
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ZixoSurface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = ZixoSurface,
            contentColor = ZixoText,
            modifier = Modifier.height(72.dp),
            tonalElevation = 0.dp
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                val isSelected = selectedTab == index
                val tint by animateColorAsState(
                    if (isSelected) ZixoPrimary else ZixoTextSecondary,
                    label = "tabTint$index"
                )
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(label, color = tint, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = ZixoPrimary.copy(alpha = 0.12f))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(searchActive: Boolean, onSearchToggle: (Boolean) -> Unit, searchQuery: String, onSearchQueryChange: (String) -> Unit, selectedTab: Int) {
    val title = when (selectedTab) { 0 -> "Zixo"; 1 -> "Calls"; 2 -> "Status"; else -> "Settings" }
    Surface(color = ZixoSurface, tonalElevation = 2.dp) {
        if (searchActive) {
            SearchBar(query = searchQuery, onQueryChange = onSearchQueryChange, onSearch = {}, active = true, onActiveChange = onSearchToggle, modifier = Modifier.fillMaxWidth().padding(8.dp), placeholder = { Text("Search...", color = ZixoTextSecondary) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = ZixoTextSecondary) }, trailingIcon = { IconButton(onClick = { onSearchToggle(false); onSearchQueryChange("") }) { Icon(Icons.Default.Close, null, tint = ZixoTextSecondary) } }, colors = SearchBarDefaults.colors()) {}
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ZixoText)
                if (selectedTab < 2) {
                    Row { IconButton(onClick = { onSearchToggle(true) }) { Icon(Icons.Default.Search, null, tint = ZixoTextSecondary) } }
                }
            }
        }
    }
}

@Composable
private fun CallFilterTabs(selectedFilter: String, onFilterChange: (String) -> Unit) {
    val filters = listOf("all" to "All", "missed" to "Missed", "outgoing" to "Outgoing", "incoming" to "Incoming")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        filters.forEach { (key, label) ->
            val isSelected = selectedFilter == key
            FilterChip(selected = isSelected, onClick = { onFilterChange(key) }, label = { Text(label, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ZixoPrimary.copy(alpha = 0.2f), selectedLabelColor = ZixoPrimary, containerColor = ZixoSurface, labelColor = ZixoTextSecondary), shape = RoundedCornerShape(20.dp), border = FilterChipDefaults.filterChipBorder(borderColor = ZixoHighlight, selectedBorderColor = ZixoPrimary, enabled = true, selected = isSelected))
        }
    }
}

@Composable
private fun ChatsList(chats: List<Chat>, userProfiles: Map<String, ZixoUser>, currentUserId: String, onChatClick: (String, String) -> Unit, isLoading: Boolean) {
    if (isLoading && chats.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ZixoPrimary) }
    } else if (chats.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("No chats yet", color = ZixoTextSecondary, fontSize = 16.sp); Spacer(modifier = Modifier.height(4.dp)); Text("Start a new conversation!", color = ZixoTextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) } }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chats, key = { it.id }) { chat ->
                val otherId = chat.getOtherUserId(currentUserId)
                val profile = userProfiles[otherId]
                ChatItem(chat = chat, profile = profile, currentUserId = currentUserId, onClick = { onChatClick(chat.id, otherId) })
            }
        }
    }
}

@Composable
private fun ChatItem(chat: Chat, profile: ZixoUser?, currentUserId: String, onClick: () -> Unit) {
    Surface(color = ZixoSurface.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(model = profile?.avatar?.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(52.dp).clip(CircleShape).background(ZixoSurfaceLight))
                if (profile?.online == true) { Box(modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(ZixoOnline)) }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile?.displayName ?: "User", color = ZixoText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(chat.lastMessage.ifEmpty { "No messages yet" }, color = ZixoTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                if (chat.lastMessageTime > 0) { Text(formatTimestamp(chat.lastMessageTime), color = ZixoTextSecondary, fontSize = 11.sp) }
                if (chat.unreadCount > 0) { Surface(color = ZixoPrimary, shape = CircleShape, modifier = Modifier.padding(top = 4.dp)) { Text("${chat.unreadCount}", color = ZixoBg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }
            }
        }
    }
}

@Composable
private fun CallsList(calls: List<CallRecord>, userProfiles: Map<String, ZixoUser>, currentUserId: String, isLoading: Boolean) {
    if (isLoading && calls.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ZixoPrimary) }
    } else if (calls.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("No calls yet", color = ZixoTextSecondary, fontSize = 16.sp); Spacer(modifier = Modifier.height(4.dp)); Text("Your call history will appear here", color = ZixoTextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) } }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) { items(calls, key = { it.id }) { call -> CallItem(call = call, userProfiles = userProfiles, currentUserId = currentUserId) } }
    }
}

@Composable
private fun CallItem(call: CallRecord, userProfiles: Map<String, ZixoUser>, currentUserId: String) {
    val isCaller = call.callerId == currentUserId
    val otherId = if (isCaller) call.receiverId else call.callerId
    val profile = userProfiles[otherId]
    val otherName = if (isCaller) call.receiverName else call.callerName
    Surface(color = ZixoSurface.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = profile?.avatar?.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape).background(ZixoSurfaceLight))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(otherName.ifEmpty { "Unknown" }, color = if (call.isMissed()) ZixoError else ZixoText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = when { call.isMissed() -> ZixoError; call.isIncoming() -> ZixoPrimary; else -> ZixoTextSecondary })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(when { call.isMissed() -> "Missed"; call.isIncoming() -> "Incoming"; else -> "Outgoing" } + if (call.isVideo()) " (Video)" else "", color = ZixoTextSecondary, fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (call.timestamp > 0) { Text(formatTimestamp(call.timestamp), color = ZixoTextSecondary, fontSize = 11.sp) }
                if (call.duration > 0) { Text(formatDuration(call.duration), color = ZixoTextSecondary, fontSize = 11.sp) }
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
private fun formatDuration(seconds: Long): String = String.format("%d:%02d", seconds / 60, seconds % 60)
