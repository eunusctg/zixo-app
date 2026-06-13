package com.zixo.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zixo.app.domain.model.UserProfile
import com.zixo.app.ui.components.LiquidGlassCard
import com.zixo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val currentUser by settingsViewModel.currentUser.collectAsState()
    val isSaving by settingsViewModel.isSaving.collectAsState()
    val saveMessage by settingsViewModel.saveMessage.collectAsState()

    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    settingsViewModel.updateAvatar(bytes)
                }
            } catch (e: Exception) {
                // Silently fail — avatar update is non-critical
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZixoBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Top bar
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
                        color = ZixoTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = ZixoTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with change overlay
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(ZixoSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            // Show new selected image
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "New Avatar",
                                modifier = Modifier.size(120.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else if (currentUser?.avatarUrl?.isNotBlank() == true) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(currentUser!!.avatarUrl),
                                contentDescription = "Avatar",
                                modifier = Modifier.size(120.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (currentUser?.displayName ?: "Z").take(1).uppercase(),
                                color = ZixoAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp
                            )
                        }
                    }

                    // Change photo button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(ZixoAccent)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            "Change Photo",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Change Photo",
                    color = ZixoAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Form Fields ──

                // Display Name — editable, max 30 chars
                LiquidGlassCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Display Name", color = ZixoTextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                "${displayName.length}/30",
                                color = if (displayName.length > 30) ZixoError else ZixoTextTertiary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { if (it.length <= 30) displayName = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZixoAccent,
                                unfocusedBorderColor = GlassBorder,
                                cursorColor = ZixoAccent,
                                focusedTextColor = ZixoTextPrimary,
                                unfocusedTextColor = ZixoTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Username — read-only
                LiquidGlassCard {
                    Column {
                        Text("Username", color = ZixoTextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZixoSurface)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = ZixoTextTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "@${currentUser?.username ?: ""}",
                                color = ZixoTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Username cannot be changed.",
                            color = ZixoTextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zixo Number — read-only with copy
                LiquidGlassCard {
                    Column {
                        Text("Zixo Number", color = ZixoTextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZixoSurface)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Badge, null, tint = ZixoAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentUser?.zixoNumber ?: "",
                                    color = ZixoAccent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(
                                    android.content.ClipData.newPlainText("Zixo Number", currentUser?.zixoNumber ?: "")
                                )
                            }) {
                                Icon(Icons.Filled.ContentCopy, "Copy", tint = ZixoTextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number — read-only
                LiquidGlassCard {
                    Column {
                        Text("Phone Number", color = ZixoTextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZixoSurface)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = ZixoTextTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentUser?.phoneNumber?.ifBlank { "Not set" } ?: "Not set",
                                color = ZixoTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Phone number cannot be changed.",
                            color = ZixoTextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bio — editable, max 100 chars
                LiquidGlassCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bio", color = ZixoTextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                "${bio.length}/100",
                                color = if (bio.length > 100) ZixoError else ZixoTextTertiary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 100) bio = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZixoAccent,
                                unfocusedBorderColor = GlassBorder,
                                cursorColor = ZixoAccent,
                                focusedTextColor = ZixoTextPrimary,
                                unfocusedTextColor = ZixoTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save message
                saveMessage?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (it.contains("updated", ignoreCase = true)) ZixoSurface else ZixoErrorBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = it,
                            color = if (it.contains("updated", ignoreCase = true)) ZixoAccent else ZixoError,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Save Changes button
                Button(
                    onClick = {
                        settingsViewModel.updateDisplayName(displayName)
                        settingsViewModel.updateBio(bio)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZixoAccent),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        "Save Changes",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
