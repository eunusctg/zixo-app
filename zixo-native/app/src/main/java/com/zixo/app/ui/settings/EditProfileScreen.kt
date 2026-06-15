package com.zixo.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoNumberBadge
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Edit Profile Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Profile editing screen with Liquid Glass aesthetic.
 *
 * Features:
 * - Large avatar with camera overlay to change photo
 * - Display name input (GlassOutlinedTextField)
 * - About/Bio input (GlassOutlinedTextField, multi-line)
 * - Zixo Number (read-only, displayed with ZixoNumberBadge)
 * - Username (read-only, displayed with label)
 * - Save button (NeonMint)
 * - Photo picker via ActivityResultContracts.PickVisualMedia
 *
 * All fields bound to [SettingsViewModel] — no dummy data.
 */
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Local editable state ──
    var displayName by remember(userProfile.displayName) {
        mutableStateOf(userProfile.displayName)
    }
    var bio by remember(userProfile.bio) {
        mutableStateOf(userProfile.bio)
    }
    var avatarUrl by remember(userProfile.avatarUrl) {
        mutableStateOf(userProfile.avatarUrl)
    }

    // ── Photo picker ──
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            avatarUrl = it.toString()
            viewModel.updateAvatarUrl(it.toString())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZixoGlassBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ──
            ZixoTopBar(
                title = "Edit Profile",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Avatar with camera overlay ────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A2A32))
                            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile avatar",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Camera overlay
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonMint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change photo",
                                    tint = Color(0xFF0B1519),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // ── Display Name ──────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "DISPLAY NAME",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    GlassOutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        placeholder = { Text("Enter your display name") },
                        singleLine = true,
                        maxLength = 50,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── About / Bio ───────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ABOUT / BIO",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    GlassOutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        placeholder = { Text("Tell us about yourself") },
                        singleLine = false,
                        maxLength = 200,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Zixo Number (read-only) ───────────────────────────────
                ZixoNumberBadge(
                    zixoNumber = userProfile.zixoNumber,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Username (read-only) ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "USERNAME",
                        color = NeonMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@${userProfile.username}",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Read-only",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // ── Save Button ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonMint)
                        .clickable {
                            if (displayName.isNotBlank()) {
                                viewModel.updateDisplayName(displayName)
                            }
                            viewModel.updateBio(bio)
                        }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Save Changes",
                        color = Color(0xFF0B1519),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
