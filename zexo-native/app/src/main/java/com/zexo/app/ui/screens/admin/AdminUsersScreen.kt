package com.zexo.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    navController: NavHostController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val filteredUsers by viewModel.filteredUsers.collectAsState()
    val searchQuery by viewModel.userSearchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    val showDeleteConfirm by viewModel.showDeleteConfirm.collectAsState()
    val message by viewModel.message.collectAsState()

    // Load users on first composition
    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    // SnackBar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // User detail dialog
    if (selectedUser != null) {
        UserDetailDialog(
            user = selectedUser!!,
            isDisabled = viewModel.isUserDisabled(selectedUser!!),
            onDismiss = { viewModel.selectUser(null) },
            onToggleAdmin = { viewModel.toggleAdminRole(selectedUser!!) },
            onToggleDisabled = { viewModel.toggleUserDisabled(selectedUser!!) },
            onDelete = { viewModel.showDeleteConfirmation() }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            containerColor = ZexoSurface,
            title = {
                Text(
                    text = "Delete User",
                    color = ZexoTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete ${selectedUser!!.displayName}? This action cannot be undone.",
                    color = ZexoTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteUser(selectedUser!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = ZexoRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissDeleteConfirmation() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZexoTextPrimary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = ZexoBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = ZexoSurface,
                tonalElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = ZexoTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Users",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZexoTextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${filteredUsers.size}",
                            fontSize = 13.sp,
                            color = ZexoTextSecondary
                        )
                    }

                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateUserSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = {
                            Text("Search users by name, email…", color = ZexoTextSecondary)
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
                                IconButton(onClick = { viewModel.updateUserSearchQuery("") }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = ZexoTextSecondary
                                    )
                                }
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
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZexoPrimary)
            }
        } else if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = ZexoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No users found" else "No users yet",
                        color = ZexoTextSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ZexoBackground),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredUsers, key = { it.uid }) { user ->
                    UserListItem(
                        user = user,
                        isDisabled = viewModel.isUserDisabled(user),
                        onClick = { viewModel.selectUser(user) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  User List Item
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun UserListItem(
    user: User,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isDisabled) ZexoSurfaceVariant.copy(alpha = 0.5f) else ZexoBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online status
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
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                // Online dot
                if (user.online && !isDisabled) {
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

            // Name + email + role
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDisabled) ZexoTextSecondary else ZexoTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Role badge
                    Surface(
                        color = if (user.role == "admin") ZexoPrimary.copy(alpha = 0.15f)
                        else ZexoSurfaceLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = user.role.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user.role == "admin") ZexoPrimary else ZexoTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isDisabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = ZexoRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "DISABLED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZexoRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = ZexoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status indicator
            if (user.online && !isDisabled) {
                Text(
                    text = "Online",
                    fontSize = 11.sp,
                    color = ZexoOnline,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  User Detail Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun UserDetailDialog(
    user: User,
    isDisabled: Boolean,
    onDismiss: () -> Unit,
    onToggleAdmin: () -> Unit,
    onToggleDisabled: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZexoSurface,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(ZexoSurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatar.isBlank()) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZexoPrimary
                        )
                    } else {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Name
                Text(
                    text = user.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZexoTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Email
                Text(
                    text = user.email,
                    fontSize = 13.sp,
                    color = ZexoTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Username
                Text(
                    text = user.username,
                    fontSize = 12.sp,
                    color = ZexoPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Role + status badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (user.role == "admin") ZexoPrimary.copy(alpha = 0.15f)
                        else ZexoSurfaceLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (user.role == "admin") Icons.Default.Shield else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (user.role == "admin") ZexoPrimary else ZexoTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = user.role.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user.role == "admin") ZexoPrimary else ZexoTextSecondary
                            )
                        }
                    }
                    if (user.online) {
                        Surface(
                            color = ZexoOnline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZexoOnline,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (isDisabled) {
                        Surface(
                            color = ZexoRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "DISABLED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZexoRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Toggle Admin
                    DialogActionButton(
                        icon = if (user.role == "admin") Icons.Default.Shield else Icons.Default.Shield,
                        text = if (user.role == "admin") "Revoke Admin Role" else "Grant Admin Role",
                        color = ZexoPrimary,
                        onClick = onToggleAdmin
                    )

                    // Enable/Disable account
                    DialogActionButton(
                        icon = if (isDisabled) Icons.Default.CheckCircle else Icons.Default.Block,
                        text = if (isDisabled) "Enable Account" else "Disable Account",
                        color = if (isDisabled) ZexoGreen else ZexoOrange,
                        onClick = onToggleDisabled
                    )

                    // Delete user
                    DialogActionButton(
                        icon = Icons.Default.DeleteForever,
                        text = "Delete User",
                        color = ZexoRed,
                        onClick = onDelete
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Close",
                    color = ZexoTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun DialogActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
