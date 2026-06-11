package com.zexo.app.ui.screens.status

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.animation.core.tween
import com.zexo.app.data.model.Status
import com.zexo.app.data.repository.AuthRepository
import com.zexo.app.data.repository.StatusRepository
import com.zexo.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Gradient Presets
// ─────────────────────────────────────────────────────────────────────────────

data class GradientPreset(
    val name: String,
    val startColor: Color,
    val endColor: Color,
    val startHex: String,
    val endHex: String
)

val gradientPresets = listOf(
    GradientPreset("Purple Haze", Color(0xFF6C5CE7), Color(0xFFA855F7), "#6C5CE7", "#A855F7"),
    GradientPreset("Ocean Blue", Color(0xFF0369A1), Color(0xFF03DAC6), "#0369A1", "#03DAC6"),
    GradientPreset("Sunset", Color(0xFFFF6B6B), Color(0xFFFF9800), "#FF6B6B", "#FF9800"),
    GradientPreset("Forest", Color(0xFF059669), Color(0xFF25D366), "#059669", "#25D366"),
    GradientPreset("Midnight", Color(0xFF1E1B4B), Color(0xFF4338CA), "#1E1B4B", "#4338CA"),
    GradientPreset("Rose", Color(0xFFE11D48), Color(0xFFF472B6), "#E11D48", "#F472B6"),
    GradientPreset("Ember", Color(0xFFDC2626), Color(0xFFF59E0B), "#DC2626", "#F59E0B"),
    GradientPreset("Arctic", Color(0xFF0EA5E9), Color(0xFF8B5CF6), "#0EA5E9", "#8B5CF6")
)

// ─────────────────────────────────────────────────────────────────────────────
// New Status ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class NewStatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val authRepository: AuthRepository
) : androidx.lifecycle.ViewModel() {

    private val _text = mutableStateOf("")
    val text: State<String> = _text

    private val _selectedGradientIndex = mutableStateOf(0)
    val selectedGradientIndex: State<Int> = _selectedGradientIndex

    private val _isWhiteText = mutableStateOf(true)
    val isWhiteText: State<Boolean> = _isWhiteText

    private val _isPosting = mutableStateOf(false)
    val isPosting: State<Boolean> = _isPosting

    private val _postSuccess = mutableStateOf(false)
    val postSuccess: State<Boolean> = _postSuccess

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun updateText(newText: String) {
        _text.value = newText
    }

    fun selectGradient(index: Int) {
        _selectedGradientIndex.value = index
    }

    fun toggleTextColor() {
        _isWhiteText.value = !_isWhiteText.value
    }

    fun postStatus() {
        val content = _text.value.trim()
        if (content.isEmpty()) {
            _error.value = "Please write something"
            return
        }

        val uid = authRepository.currentUid ?: return
        val userName = authRepository.currentUser?.displayName ?: "Unknown"
        val userAvatar = authRepository.currentUser?.photoUrl?.toString() ?: ""
        val gradient = gradientPresets[_selectedGradientIndex.value]

        _isPosting.value = true
        _error.value = null

        val backgroundColor = "${gradient.startHex},${gradient.endHex}"
        val textColor = if (_isWhiteText.value) "#FFFFFF" else "#000000"

        val status = Status(
            userId = uid,
            userName = userName,
            userAvatar = userAvatar,
            type = "text",
            content = content,
            backgroundColor = backgroundColor,
            textColor = textColor
        )

        kotlinx.coroutines.MainScope().launch {
            val result = statusRepository.createStatus(status)
            _isPosting.value = false
            result.onSuccess {
                _postSuccess.value = true
            }.onFailure {
                _error.value = "Failed to post status. Please try again."
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// New Status Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewStatusScreen(
    navController: NavController,
    viewModel: NewStatusViewModel = hiltViewModel()
) {
    val text by viewModel.text
    val selectedGradientIndex by viewModel.selectedGradientIndex
    val isWhiteText by viewModel.isWhiteText
    val isPosting by viewModel.isPosting
    val postSuccess by viewModel.postSuccess
    val error by viewModel.error

    // Navigate back after successful post
    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            delay(800L)
            navController.previousBackStackEntry?.savedStateHandle?.set("status_posted", true)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New Status",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        enabled = !isPosting
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.postStatus() },
                        enabled = text.trim().isNotEmpty() && !isPosting
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ZexoPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Post",
                                color = if (text.trim().isNotEmpty()) ZexoPrimary else ZexoTextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Preview
            StatusPreview(
                text = text,
                gradientIndex = selectedGradientIndex,
                isWhiteText = isWhiteText
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Text Input
            StatusTextInput(
                text = text,
                onTextChange = { viewModel.updateText(it) },
                enabled = !isPosting
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Text Color Toggle
            TextColorToggle(
                isWhiteText = isWhiteText,
                onToggle = { viewModel.toggleTextColor() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Background Color Picker
            BackgroundColorPicker(
                selectedIndex = selectedGradientIndex,
                onSelect = { viewModel.selectGradient(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Character count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${text.length} / 500",
                    color = if (text.length > 500) ZexoRed else ZexoTextSecondary,
                    fontSize = 12.sp
                )
            }

            // Error message
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ZexoRed.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = ZexoRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error!!,
                                color = ZexoRed,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Post Button (bottom)
            Button(
                onClick = { viewModel.postStatus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = text.trim().isNotEmpty() && !isPosting && text.length <= 500,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZexoPrimary,
                    disabledContainerColor = ZexoSurfaceLight
                )
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Posting...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Post Status",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Preview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatusPreview(
    text: String,
    gradientIndex: Int,
    isWhiteText: Boolean
) {
    val gradient = gradientPresets[gradientIndex]
    val textColor = if (isWhiteText) Color.White else Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(gradient.startColor, gradient.endColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "Type something...",
                    color = textColor.copy(alpha = 0.4f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            } else {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            // Preview badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PREVIEW",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Text Input
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatusTextInput(
    text: String,
    onTextChange: (String) -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = text,
        onValueChange = { if (it.length <= 500) onTextChange(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "What's on your mind?",
                color = ZexoTextSecondary.copy(alpha = 0.5f)
            )
        },
        enabled = enabled,
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZexoPrimary,
            unfocusedBorderColor = ZexoSurfaceLight,
            focusedContainerColor = ZexoSurface,
            unfocusedContainerColor = ZexoSurface,
            cursorColor = ZexoPrimary,
            focusedTextColor = ZexoTextPrimary,
            unfocusedTextColor = ZexoTextPrimary
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Text Color Toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TextColorToggle(
    isWhiteText: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.FormatColorText,
            contentDescription = null,
            tint = ZexoTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Text Color",
            color = ZexoTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))

        // White option
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .then(
                    if (isWhiteText) {
                        Modifier.border(3.dp, ZexoPrimary, CircleShape)
                    } else {
                        Modifier.border(1.dp, ZexoSurfaceLight, CircleShape)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isWhiteText) onToggle() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isWhiteText) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = ZexoPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Black option
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .then(
                    if (!isWhiteText) {
                        Modifier.border(3.dp, ZexoPrimary, CircleShape)
                    } else {
                        Modifier.border(1.dp, ZexoSurfaceLight, CircleShape)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (isWhiteText) onToggle() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isWhiteText) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = ZexoPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background Color Picker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BackgroundColorPicker(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(
            text = "Background",
            color = ZexoTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Grid of gradient options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            gradientPresets.forEachIndexed { index, gradient ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(gradient.startColor, gradient.endColor)
                            )
                        )
                        .then(
                            if (index == selectedIndex) {
                                Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                            } else {
                                Modifier.border(1.dp, ZexoSurfaceLight, RoundedCornerShape(12.dp))
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index == selectedIndex) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Gradient name label
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "gradientName"
        ) { index ->
            Text(
                text = gradientPresets[index].name,
                color = ZexoTextSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
