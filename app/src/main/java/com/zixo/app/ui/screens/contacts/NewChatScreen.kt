package com.zixo.app.ui.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zixo.app.data.model.ZixoUser
import com.zixo.app.data.repository.AuthRepository
import com.zixo.app.data.repository.ChatRepository
import com.zixo.app.data.repository.UserRepository
import androidx.lifecycle.viewModelScope
import com.zixo.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewChatUiState(val users: List<ZixoUser> = emptyList(), val searchQuery: String = "", val isLoading: Boolean = true, val openingChatId: String? = null, val openingOtherUserId: String? = null)

@HiltViewModel
class NewChatViewModel @Inject constructor(private val userRepository: UserRepository, private val authRepository: AuthRepository, private val chatRepository: ChatRepository) : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(NewChatUiState())
    val uiState: StateFlow<NewChatUiState> = _uiState.asStateFlow()

    init { loadUsers() }

    private fun loadUsers() { viewModelScope.launch { val uid = authRepository.firebaseUser?.uid ?: return@launch; _uiState.value = _uiState.value.copy(users = userRepository.getAllUsers(uid).getOrDefault(emptyList()), isLoading = false) } }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch { val uid = authRepository.firebaseUser?.uid ?: return@launch; if (query.isBlank()) loadUsers() else _uiState.value = _uiState.value.copy(users = userRepository.searchUsers(query, uid).getOrDefault(emptyList())) }
    }

    fun openChatWithUser(otherUserId: String) { val currentUserId = authRepository.firebaseUser?.uid ?: return; viewModelScope.launch { chatRepository.getOrCreateChat(currentUserId, otherUserId).getOrNull()?.let { chatId -> _uiState.value = _uiState.value.copy(openingChatId = chatId, openingOtherUserId = otherUserId) } } }

    fun clearOpeningChatId() { _uiState.value = _uiState.value.copy(openingChatId = null, openingOtherUserId = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatPlaceholder(onBack: () -> Unit, onChatCreated: (String, String) -> Unit = { _, _ -> }, viewModel: NewChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.openingChatId) { uiState.openingChatId?.let { chatId -> val otherUserId = uiState.openingOtherUserId ?: ""; if (otherUserId.isNotEmpty()) onChatCreated(chatId, otherUserId); viewModel.clearOpeningChatId() } }

    Column(modifier = Modifier.fillMaxSize().background(ZixoBg)) {
        Surface(color = ZixoSurface, tonalElevation = 2.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ZixoText) }
                Text("New Chat", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ZixoText)
            }
        }
        OutlinedTextField(value = uiState.searchQuery, onValueChange = viewModel::onSearchQueryChange, modifier = Modifier.fillMaxWidth().padding(12.dp), placeholder = { Text("Search users...", color = ZixoTextSecondary) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = ZixoTextSecondary) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ZixoText, unfocusedTextColor = ZixoText, focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight, cursorColor = ZixoPrimary, focusedContainerColor = ZixoSurface, unfocusedContainerColor = ZixoSurface), shape = RoundedCornerShape(24.dp), singleLine = true)

        if (uiState.isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ZixoPrimary) } }
        else if (uiState.users.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("No users found", color = ZixoTextSecondary, fontSize = 16.sp); Spacer(modifier = Modifier.height(4.dp)); Text("Try a different search", color = ZixoTextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) } } }
        else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.users, key = { it.uid }) { user ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.openChatWithUser(user.uid) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box { AsyncImage(model = user.avatar.ifEmpty { null }, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape).background(ZixoSurfaceLight)); if (user.online) { Box(modifier = Modifier.align(Alignment.BottomEnd).size(10.dp).clip(CircleShape).background(ZixoOnline)) } }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) { Text(user.displayName, color = ZixoText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("@${user.username.removePrefix("@")}", color = ZixoTextSecondary, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}
