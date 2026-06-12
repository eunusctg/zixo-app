package com.zexo.app.ui.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.zexo.app.data.model.User
import com.zexo.app.ui.navigation.Screen
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    navController: NavHostController,
    viewModel: NewChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to chat when created
    LaunchedEffect(uiState.createdChatId) {
        uiState.createdChatId?.let { chatId ->
            val otherUserId = uiState.createdChatOtherUserId ?: ""
            navController.navigate(Screen.Chat.createRoute(chatId, otherUserId)) {
                popUpTo(Screen.NewChat.route) { inclusive = true }
            }
            viewModel.clearCreatedChatId()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "New Chat",
                            fontWeight = FontWeight.SemiBold,
                            color = ZexoTextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ZexoTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ZexoBackground
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZexoBackground)
        ) {
            // ── Search Bar ──────────────────────────────────────────
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                isSearching = uiState.isSearching,
                onQrClick = {
                    navController.navigate(Screen.QRScanner.route)
                },
                onClear = {
                    viewModel.updateSearchQuery("")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Content ─────────────────────────────────────────────
            if (uiState.searchQuery.isNotBlank()) {
                // Search results
                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ZexoPrimary, strokeWidth = 2.dp)
                    }
                } else if (uiState.searchResults.isEmpty()) {
                    EmptySearchResult(query = uiState.searchQuery)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        item {
                            SectionHeader(title = "Search Results")
                        }
                        items(uiState.searchResults, key = { it.uid }) { user ->
                            UserItem(
                                user = user,
                                isOnline = viewModel.isUserOnline(user.uid),
                                lastSeenText = viewModel.getLastSeenText(user.uid),
                                onClick = { viewModel.startChat(user.uid) },
                                isCreatingChat = uiState.isCreatingChat
                            )
                        }
                    }
                }
            } else {
                // All Users
                if (uiState.isLoadingAll) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ZexoPrimary, strokeWidth = 2.dp)
                    }
                } else if (uiState.allUsers.isEmpty()) {
                    EmptySearchResult(query = "")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        item {
                            SectionHeader(title = "All Users")
                        }
                        items(uiState.allUsers, key = { it.uid }) { user ->
                            UserItem(
                                user = user,
                                isOnline = viewModel.isUserOnline(user.uid),
                                lastSeenText = viewModel.getLastSeenText(user.uid),
                                onClick = { viewModel.startChat(user.uid) },
                                isCreatingChat = uiState.isCreatingChat
                            )
                        }
                    }
                }
            }

            // ── Error ───────────────────────────────────────────────
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = ZexoSurfaceLight,
                    contentColor = ZexoRed
                ) {
                    Text(error, fontSize = 13.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Search Bar with QR button
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    onQrClick: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = ZexoSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = ZexoTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Name, email, Zixo #, username, phone",
                        color = ZexoTextSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = ZexoTextPrimary,
                    unfocusedTextColor = ZexoTextPrimary,
                    cursorColor = ZexoPrimary
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ZexoPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = ZexoTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onQrClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR",
                    tint = ZexoSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Section Header
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = ZexoPrimary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════
//  User Item
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun UserItem(
    user: User,
    isOnline: Boolean,
    lastSeenText: String,
    onClick: () -> Unit,
    isCreatingChat: Boolean
) {
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
                    .background(ZexoSurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatar.isBlank()) {
                    Text(
                        text = user.displayName.take(1).uppercase(),
                        fontSize = 22.sp,
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
            // Online dot
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(ZexoGreen)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(ZexoGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name, username, Zixo number, status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZexoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 1.dp)
            ) {
                if (user.username.isNotBlank()) {
                    Text(
                        text = user.username,
                        fontSize = 12.sp,
                        color = ZexoTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (user.zixoNumber.isNotBlank()) {
                    Text(
                        text = user.zixoNumber,
                        fontSize = 11.sp,
                        color = ZexoSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (lastSeenText.isNotBlank()) {
                Text(
                    text = lastSeenText,
                    fontSize = 11.sp,
                    color = if (isOnline) ZexoGreen else ZexoTextSecondary
                )
            }
        }

        // Chat icon
        if (isCreatingChat) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ZexoPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                Icons.Default.Chat,
                contentDescription = "Start chat",
                tint = ZexoPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Empty search state
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmptySearchResult(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = ZexoTextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (query.isBlank()) "No users found" else "No results for \"$query\"",
                color = ZexoTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (query.isBlank()) "Users will appear here once they sign up"
                        else "Try a different search term",
                color = ZexoTextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}
