package com.zixo.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zixo.app.ui.components.GlassOutlinedTextField
import com.zixo.app.ui.components.ZixoGlassBackground
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.components.diagonalMeshGradient
import com.zixo.app.ui.components.liquidGlassCard
import com.zixo.app.ui.screens.editprofile.EditProfileViewModel
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary
import com.zixo.app.ui.theme.TextTertiary
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════════
// Color constants for the Edit Profile glass design
// ════════════════════════════════════════════════════════════════

private val SnackbarBackground = Color(0xFF1E3239)
private val DisabledFieldBackground = Color(0x0D1A2A32)
private val DestructiveRed = Color(0xFFFF5252)

// ════════════════════════════════════════════════════════════════
// Edit Profile Screen — iOS Liquid Glass Aesthetic
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ── Photo picker launcher ──────────────────────────────────────
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        viewModel.onImageSelected(uri)
    }

    // ── Snackbar for save success ─────────────────────────────────
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Profile updated successfully!",
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.clearSaveSuccess()
        }
    }

    // ── Snackbar for save error ────────────────────────────────────
    LaunchedEffect(uiState.saveError) {
        if (uiState.saveError != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = uiState.saveError ?: "Failed to update profile. Please try again.",
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.clearSaveError()
        }
    }

    Scaffold(
        topBar = {
            ZixoTopBar(
                title = "Edit Profile",
                showBackButton = true,
                onBackClick = { navController.popBackStack() },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SnackbarBackground,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Animated glass background
            ZixoGlassBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ──────────────────────────────────────────────────
                // 1. Profile Photo Section (diagonalMeshGradient)
                // ──────────────────────────────────────────────────
                ProfilePhotoGlassSection(
                    photoUrl = if (uiState.selectedImageUri != null) {
                        uiState.selectedImageUri.toString()
                    } else {
                        uiState.photoUrl
                    },
                    selectedImageUri = uiState.selectedImageUri,
                    displayName = uiState.displayName,
                    onPickImage = {
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ──────────────────────────────────────────────────
                // 2. Zixo Number Card (read-only, liquidGlassCard)
                // ──────────────────────────────────────────────────
                ZixoNumberGlassCard(
                    zixoNumber = uiState.zixoNumber,
                    onCopyToClipboard = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Zixo Number", uiState.zixoNumber)
                        clipboard.setPrimaryClip(clip)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Zixo number copied!",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ──────────────────────────────────────────────────
                // 3. Form Fields Card (liquidGlassCard)
                // ──────────────────────────────────────────────────
                FormFieldsGlassCard(
                    displayName = uiState.displayName,
                    onDisplayNameChanged = viewModel::onDisplayNameChange,
                    username = uiState.username,
                    phoneNumber = uiState.phoneNumber,
                    bio = uiState.bio,
                    onBioChanged = viewModel::onBioChange,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ──────────────────────────────────────────────────
                // 4. Save Changes Button
                // ──────────────────────────────────────────────────
                SaveChangesGlassButton(
                    enabled = uiState.hasChanges && !uiState.isSaving,
                    isSaving = uiState.isSaving,
                    onClick = viewModel::onSaveChanges
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Profile Photo Glass Section — diagonalMeshGradient card
// ════════════════════════════════════════════════════════════════

@Composable
private fun ProfilePhotoGlassSection(
    photoUrl: String?,
    selectedImageUri: android.net.Uri?,
    displayName: String,
    onPickImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .diagonalMeshGradient()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPickImage
            )
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 96dp circular avatar with camera icon overlay
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2A32)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        color = TextPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Camera icon overlay
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(NeonMint)
                    .border(
                        width = 2.dp,
                        color = BackgroundGradientEnd,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Change profile photo",
                    tint = Color(0xFF003A1F),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Tap to change profile photo",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Zixo Number Glass Card — read-only with QR thumbnail & copy
// ════════════════════════════════════════════════════════════════

@Composable
private fun ZixoNumberGlassCard(
    zixoNumber: String,
    onCopyToClipboard: () -> Unit
) {
    val formattedNumber = formatZixoNumber(zixoNumber)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = NeonMint.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label
        Text(
            text = "Zixo Number",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Formatted number in monospace NeonMint
        Text(
            text = formattedNumber,
            color = NeonMint,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // QR code thumbnail + Copy button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // QR code thumbnail
            if (zixoNumber.isNotBlank()) {
                QrCodeThumbnail(
                    data = "zixo://user/$zixoNumber",
                    size = 56.dp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Copy button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonMint.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = NeonMint.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onCopyToClipboard)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = NeonMint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Copy",
                    color = NeonMint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// QR Code Thumbnail (small version for the Zixo number card)
// ════════════════════════════════════════════════════════════════

@Composable
private fun QrCodeThumbnail(
    data: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    if (data.isBlank()) return

    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }

    val qrBitmap = remember(data, sizePx) {
        generateQrBitmapThumbnail(data, sizePx)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier
                .size(size - 8.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

private fun generateQrBitmapThumbnail(data: String, sizePx: Int): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(
                x, y,
                if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            )
        }
    }
    return bitmap
}

// ════════════════════════════════════════════════════════════════
// Form Fields Glass Card — Display Name, Username, Phone, Bio
// ════════════════════════════════════════════════════════════════

@Composable
private fun FormFieldsGlassCard(
    displayName: String,
    onDisplayNameChanged: (String) -> Unit,
    username: String,
    phoneNumber: String?,
    bio: String,
    onBioChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassCard()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        // ── Display Name ───────────────────────────────────────────
        FieldLabel(text = "Display Name")

        Spacer(modifier = Modifier.height(6.dp))

        GlassOutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Enter display name",
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 15.sp
                )
            },
            maxLength = EditProfileViewModel.DISPLAY_NAME_MAX_LENGTH,
            singleLine = true,
        )

        // Character counter
        CharacterCounter(
            current = displayName.length,
            max = EditProfileViewModel.DISPLAY_NAME_MAX_LENGTH
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Username (read-only) ────────────────────────────────────
        FieldLabel(text = "Username")

        Spacer(modifier = Modifier.height(6.dp))

        GlassOutlinedTextField(
            value = username,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(DisabledFieldBackground, RoundedCornerShape(12.dp)),
            readOnly = true,
            enabled = false,
        )

        FieldNote(text = "Username cannot be changed.")

        Spacer(modifier = Modifier.height(20.dp))

        // ── Phone Number (read-only) ────────────────────────────────
        FieldLabel(text = "Phone")

        Spacer(modifier = Modifier.height(6.dp))

        GlassOutlinedTextField(
            value = phoneNumber ?: "Not set",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(DisabledFieldBackground, RoundedCornerShape(12.dp)),
            readOnly = true,
            enabled = false,
        )

        FieldNote(text = "Phone number cannot be changed.")

        Spacer(modifier = Modifier.height(20.dp))

        // ── Bio / Status (multi-line) ───────────────────────────────
        FieldLabel(text = "Bio")

        Spacer(modifier = Modifier.height(6.dp))

        GlassOutlinedTextField(
            value = bio,
            onValueChange = onBioChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Write something about yourself\u2026",
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 15.sp
                )
            },
            maxLength = EditProfileViewModel.BIO_MAX_LENGTH,
            singleLine = false,
        )

        // Character counter
        CharacterCounter(
            current = bio.length,
            max = EditProfileViewModel.BIO_MAX_LENGTH
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Reusable field label & note composables
// ════════════════════════════════════════════════════════════════

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun FieldNote(text: String) {
    Text(
        text = text,
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
private fun CharacterCounter(current: Int, max: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "$current/$max",
            color = if (current >= max) {
                DestructiveRed
            } else {
                TextTertiary
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ════════════════════════════════════════════════════════════════
// Save Changes Glass Button
// ════════════════════════════════════════════════════════════════

@Composable
private fun SaveChangesGlassButton(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonMint,
            contentColor = Color(0xFF003A1F),
            disabledContainerColor = NeonMint.copy(alpha = 0.35f),
            disabledContentColor = Color(0xFF003A1F).copy(alpha = 0.5f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFF003A1F),
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text = "Save Changes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Helper: Format 8-digit number as "XXXX XXXX"
// ════════════════════════════════════════════════════════════════

private fun formatZixoNumber(number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (digits.length >= 8) {
        "${digits.substring(0, 4)} ${digits.substring(4, 8)}"
    } else {
        digits
    }
}
