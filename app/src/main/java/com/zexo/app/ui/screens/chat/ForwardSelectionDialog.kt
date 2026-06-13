package com.zexo.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zexo.app.data.model.ZixoUser
import com.zexo.app.data.repository.UserRepository
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.ChatRepository
import com.zexo.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForwardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _users = MutableStateFlow<List<ZixoUser>>(emptyList())
    val users: StateFlow<List<ZixoUser>> = _users.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            val uid = authRepository.firebaseUser?.uid ?: return@launch
            val result = userRepository.getAllUsers(uid)
            _users.value = result.getOrDefault(emptyList())
        }
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val uid = authRepository.firebaseUser?.uid ?: return@launch
            if (query.isBlank()) {
                loadUsers()
            } else {
                val result = userRepository.searchUsers(query, uid)
                _users.value = result.getOrDefault(emptyList())
            }
        }
    }

    suspend fun forwardToUser(otherUserId: String): String? {
        val currentUserId = authRepository.firebaseUser?.uid ?: return null
        val result = chatRepository.getOrCreateChat(currentUserId, otherUserId)
        return result.getOrNull()
    }
}

@Composable
fun ForwardSelectionDialog(
    currentUserId: String,
    onForward: (String) -> Unit,
    onDismiss: () -> Unit,
    forwardViewModel: ForwardViewModel = hiltViewModel()
) {
    val users by forwardViewModel.users.collectAsState()
    val searchQuery by forwardViewModel.searchQuery.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZixoSurface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Forward to...", color = ZixoText, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = forwardViewModel::onSearch,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search contacts...", color = ZixoTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = ZixoTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZixoText, unfocusedTextColor = ZixoText,
                        focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight,
                        cursorColor = ZixoPrimary,
                        focusedContainerColor = ZixoBg, unfocusedContainerColor = ZixoBg
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users, key = { it.uid }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    kotlinx.coroutines.runBlocking {
                                        val chatId = forwardViewModel.forwardToUser(user.uid)
                                        chatId?.let { onForward(it) }
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.avatar.ifEmpty { null },
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(ZixoSurfaceLight)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user.displayName,
                                    color = ZixoText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "@${user.username.removePrefix("@")}",
                                    color = ZixoTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ZixoTextSecondary)
            }
        }
    )
}
