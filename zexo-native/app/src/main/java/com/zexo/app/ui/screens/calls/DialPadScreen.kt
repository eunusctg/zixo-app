package com.zexo.app.ui.screens.calls

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zexo.app.data.model.User
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.UserRepository
import com.zexo.app.ui.navigation.Screen
import com.zexo.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// DialPad ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class DialPadViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : androidx.lifecycle.ViewModel() {

    private val _enteredNumber = mutableStateOf("")
    val enteredNumber: State<String> = _enteredNumber

    private val _foundUser = mutableStateOf<User?>(null)
    val foundUser: State<User?> = _foundUser

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun onDigitPress(digit: String) {
        if (_enteredNumber.value.length < 12) {
            _enteredNumber.value += digit
            _foundUser.value = null
            _error.value = null
        }
    }

    fun onDelete() {
        if (_enteredNumber.value.isNotEmpty()) {
            _enteredNumber.value = _enteredNumber.value.dropLast(1)
            _foundUser.value = null
            _error.value = null
        }
    }

    fun onLongDelete() {
        _enteredNumber.value = ""
        _foundUser.value = null
        _error.value = null
    }

    fun searchAndCall() {
        val number = _enteredNumber.value.trim()
        if (number.isEmpty()) return

        val uid = authRepository.currentUid ?: return
        _isSearching.value = true
        _error.value = null

        kotlinx.coroutines.MainScope().launch {
            val result = userRepository.searchUsers(number, uid)
            _isSearching.value = false

            result.onSuccess { users ->
                // First try exact Zixo number match
                val exactMatch = users.firstOrNull {
                    it.zixoNumber.equals(number, ignoreCase = true)
                }
                if (exactMatch != null) {
                    _foundUser.value = exactMatch
                } else if (users.isNotEmpty()) {
                    _foundUser.value = users.first()
                } else {
                    _error.value = "No user found with Zixo number $number"
                }
            }.onFailure {
                _error.value = "Search failed. Please try again."
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DialPad Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialPadScreen(
    navController: NavController,
    viewModel: DialPadViewModel = hiltViewModel()
) {
    val enteredNumber by viewModel.enteredNumber
    val foundUser by viewModel.foundUser
    val isSearching by viewModel.isSearching
    val error by viewModel.error
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Call Zixo Number",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZexoSurface,
                    titleContentColor = ZexoTextPrimary
                )
            )
        },
        containerColor = ZexoBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Number Display
            NumberDisplay(
                number = enteredNumber,
                userName = foundUser?.displayName,
                userAvatar = foundUser?.avatar
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (error != null) {
                    Text(
                        text = error!!,
                        color = ZexoRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Dial Pad Grid
            DialPadGrid(
                onDigitPress = { viewModel.onDigitPress(it) },
                onDelete = { viewModel.onDelete() },
                onLongDelete = { viewModel.onLongDelete() },
                enteredNumber = enteredNumber
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Call Buttons
            CallButtons(
                isSearching = isSearching,
                foundUser = foundUser,
                enteredNumber = enteredNumber,
                onAudioCall = {
                    if (foundUser != null) {
                        context.startActivity(
                            CallActivity.createOutgoingIntent(
                                context,
                                foundUser!!.uid,
                                isVideo = false
                            )
                        )
                    } else {
                        viewModel.searchAndCall()
                    }
                },
                onVideoCall = {
                    if (foundUser != null) {
                        context.startActivity(
                            CallActivity.createOutgoingIntent(
                                context,
                                foundUser!!.uid,
                                isVideo = true
                            )
                        )
                    } else {
                        viewModel.searchAndCall()
                    }
                },
                onSearch = { viewModel.searchAndCall() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Number Display
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NumberDisplay(
    number: String,
    userName: String?,
    userAvatar: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ZexoSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (number.isEmpty()) {
                Text(
                    text = "Enter Zixo Number",
                    color = ZexoTextSecondary.copy(alpha = 0.5f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatZixoNumber(number),
                        color = ZexoTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    if (userName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ZexoGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = userName,
                                color = ZexoGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatZixoNumber(number: String): String {
    // Format: ZIXO-XXX-XXX
    return if (number.startsWith("ZIXO", ignoreCase = true)) {
        val digits = number.removePrefix("ZIXO").removePrefix("zixo")
        if (digits.length > 3) {
            "ZIXO ${digits.take(3)} ${digits.drop(3)}"
        } else if (digits.isNotEmpty()) {
            "ZIXO $digits"
        } else {
            "ZIXO"
        }
    } else {
        number.chunked(3).joinToString(" ")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dial Pad Grid
// ─────────────────────────────────────────────────────────────────────────────

data class DialKey(
    val digit: String,
    val letters: String = ""
)

@Composable
fun DialPadGrid(
    onDigitPress: (String) -> Unit,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
    enteredNumber: String
) {
    val keys = listOf(
        DialKey("1", ""),
        DialKey("2", "ABC"),
        DialKey("3", "DEF"),
        DialKey("4", "GHI"),
        DialKey("5", "JKL"),
        DialKey("6", "MNO"),
        DialKey("7", "PQRS"),
        DialKey("8", "TUV"),
        DialKey("9", "WXYZ"),
        DialKey("*", ""),
        DialKey("0", "+"),
        DialKey("#", "")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    DialKeyButton(
                        key = key,
                        onClick = { onDigitPress(key.digit) }
                    )
                }
            }
        }

        // Backspace row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (enteredNumber.isNotEmpty()) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Filled.Backspace,
                        contentDescription = "Delete",
                        tint = ZexoTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DialKeyButton(
    key: DialKey,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(ZexoSurfaceLight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = key.digit,
                color = ZexoTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            if (key.letters.isNotEmpty()) {
                Text(
                    text = key.letters,
                    color = ZexoTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Call Buttons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CallButtons(
    isSearching: Boolean,
    foundUser: User?,
    enteredNumber: String,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Audio Call Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledIconButton(
                onClick = if (foundUser != null) onAudioCall else onSearch,
                modifier = Modifier.size(64.dp),
                enabled = enteredNumber.isNotEmpty() && !isSearching,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ZexoGreen,
                    disabledContainerColor = ZexoSurfaceLight
                )
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = "Audio Call",
                        modifier = Modifier.size(28.dp),
                        tint = if (enteredNumber.isNotEmpty()) Color.White else ZexoTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Audio",
                color = if (enteredNumber.isNotEmpty()) ZexoGreen else ZexoTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Video Call Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledIconButton(
                onClick = if (foundUser != null) onVideoCall else onSearch,
                modifier = Modifier.size(64.dp),
                enabled = enteredNumber.isNotEmpty() && !isSearching,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ZexoPrimary,
                    disabledContainerColor = ZexoSurfaceLight
                )
            ) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = "Video Call",
                    modifier = Modifier.size(28.dp),
                    tint = if (enteredNumber.isNotEmpty()) Color.White else ZexoTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Video",
                color = if (enteredNumber.isNotEmpty()) ZexoPrimary else ZexoTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // Show found user info
    AnimatedVisibility(
        visible = foundUser != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (foundUser != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ZexoSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ZexoPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (foundUser!!.avatar.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = foundUser!!.avatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = foundUser!!.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = foundUser!!.displayName,
                            color = ZexoTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = foundUser!!.zixoNumber,
                            color = ZexoPrimaryLight,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Found",
                        tint = ZexoGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
