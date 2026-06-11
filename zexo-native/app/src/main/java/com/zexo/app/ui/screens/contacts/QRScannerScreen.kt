package com.zexo.app.ui.screens.contacts

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.zexo.app.data.model.User
import com.zexo.app.ui.navigation.Screen
import com.zexo.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    navController: NavHostController,
    viewModel: QRScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Navigate to chat when created
    LaunchedEffect(uiState.createdChatId) {
        uiState.createdChatId?.let { chatId ->
            navController.navigate(Screen.Chat.createRoute(chatId)) {
                popUpTo(Screen.NewChat.route) { inclusive = true }
            }
            viewModel.clearCreatedChatId()
        }
    }

    // Add Contact dialog
    if (uiState.showAddContactDialog && uiState.scannedUser != null) {
        AddContactDialog(
            user = uiState.scannedUser!!,
            isCreatingChat = uiState.isCreatingChat,
            onDismiss = { viewModel.dismissDialog() },
            onStartChat = { viewModel.startChatWithScannedUser() }
        )
    }

    // Error snack
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Auto-clear after 3s
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Scan QR Code",
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
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZexoBackground)
        ) {
            // Camera preview
            CameraPreview(
                onBarcodeDetected = { barcode ->
                    barcode.rawValue?.let { content ->
                        viewModel.onQrCodeDetected(content)
                    }
                },
                onError = {
                    viewModel.clearError()
                }
            )

            // Scan frame overlay
            ScanFrameOverlay()

            // Bottom info text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error message
                uiState.error?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ZexoRed.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = error,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Loading indicator when fetching user
                if (uiState.isFetchingUser) {
                    CircularProgressIndicator(
                        color = ZexoPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Point the camera at a Zixo QR code",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The QR code contains a Zixo user ID",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Camera Preview with ML Kit Barcode Scanning
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CameraPreview(
    onBarcodeDetected: (Barcode) -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    // Track last scan time to avoid duplicate detections
    var lastScanTime by remember { mutableLongStateOf(0L) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analyzer ->
                            analyzer.setAnalyzer(executor) { imageProxy ->
                                processImageProxy(
                                    imageProxy = imageProxy,
                                    barcodeScanner = barcodeScanner,
                                    onBarcodeDetected = { barcode ->
                                        val now = System.currentTimeMillis()
                                        // Debounce: only process one barcode per 2 seconds
                                        if (now - lastScanTime > 2000L) {
                                            lastScanTime = now
                                            onBarcodeDetected(barcode)
                                        }
                                    },
                                    onError = onError
                                )
                            }
                        }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        onError()
                    }
                } catch (e: Exception) {
                    onError()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcodeDetected: (Barcode) -> Unit,
    onError: () -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.let { barcode ->
                    onBarcodeDetected(barcode)
                }
            }
            .addOnFailureListener {
                onError()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Scan Frame Overlay
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ScanFrameOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        // Transparent scan window
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Transparent)
                .graphicsLayer {
                    // Cut out the center so camera shows through
                    shape = RoundedCornerShape(20.dp)
                    clip = true
                }
        )

        // Frame border
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(2.dp)
        ) {
            // Top-left corner
            CornerAccent(
                modifier = Modifier.align(Alignment.TopStart),
                rotation = 0f
            )
            // Top-right corner
            CornerAccent(
                modifier = Modifier.align(Alignment.TopEnd),
                rotation = 90f
            )
            // Bottom-right corner
            CornerAccent(
                modifier = Modifier.align(Alignment.BottomEnd),
                rotation = 180f
            )
            // Bottom-left corner
            CornerAccent(
                modifier = Modifier.align(Alignment.BottomStart),
                rotation = 270f
            )
        }
    }
}

@Composable
private fun CornerAccent(
    modifier: Modifier = Modifier,
    rotation: Float
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .graphicsLayer { this.rotationZ = rotation }
    ) {
        // Horizontal line
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(36.dp)
                .height(4.dp)
                .background(ZexoPrimary, RoundedCornerShape(2.dp))
        )
        // Vertical line
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(4.dp)
                .height(36.dp)
                .background(ZexoPrimary, RoundedCornerShape(2.dp))
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Add Contact Dialog
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun AddContactDialog(
    user: User,
    isCreatingChat: Boolean,
    onDismiss: () -> Unit,
    onStartChat: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isCreatingChat) onDismiss() },
        containerColor = ZexoSurface,
        titleContentColor = ZexoTextPrimary,
        textContentColor = ZexoTextSecondary,
        title = {
            Text(
                text = "Add Contact",
                fontWeight = FontWeight.SemiBold,
                color = ZexoTextPrimary
            )
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ZexoSurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatar.isBlank()) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZexoPrimary
                        )
                    } else {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = user.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZexoTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.username.isNotBlank()) {
                        Text(
                            text = user.username,
                            fontSize = 13.sp,
                            color = ZexoTextSecondary
                        )
                    }
                    if (user.zixoNumber.isNotBlank()) {
                        Text(
                            text = user.zixoNumber,
                            fontSize = 12.sp,
                            color = ZexoSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartChat,
                enabled = !isCreatingChat,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZexoPrimary,
                    disabledContainerColor = ZexoPrimary.copy(alpha = 0.5f)
                )
            ) {
                if (isCreatingChat) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Starting...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Chat", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            if (!isCreatingChat) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = ZexoTextSecondary)
                }
            }
        }
    )
}
