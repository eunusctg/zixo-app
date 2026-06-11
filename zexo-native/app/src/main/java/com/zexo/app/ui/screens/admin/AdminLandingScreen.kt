package com.zexo.app.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLandingScreen(
    navController: NavHostController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val landingConfig by viewModel.landingConfig.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val message by viewModel.message.collectAsState()

    // Local editable state
    var heroTitle by remember { mutableStateOf(landingConfig.heroTitle) }
    var heroSubtitle by remember { mutableStateOf(landingConfig.heroSubtitle) }
    var heroDescription by remember { mutableStateOf(landingConfig.heroDescription) }
    var features by remember { mutableStateOf(landingConfig.features) }
    var ctaText by remember { mutableStateOf(landingConfig.ctaText) }

    // New feature input
    var newFeature by remember { mutableStateOf("") }

    // Preview toggle
    var showPreview by remember { mutableStateOf(false) }

    // Sync when config loads from Firestore
    LaunchedEffect(landingConfig) {
        heroTitle = landingConfig.heroTitle
        heroSubtitle = landingConfig.heroSubtitle
        heroDescription = landingConfig.heroDescription
        features = landingConfig.features
        ctaText = landingConfig.ctaText
    }

    // SnackBar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    fun saveLanding() {
        viewModel.updateLandingConfig(
            LandingConfig(
                heroTitle = heroTitle,
                heroSubtitle = heroSubtitle,
                heroDescription = heroDescription,
                features = features,
                ctaText = ctaText
            )
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
                        text = "Landing Page",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZexoTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    // Preview toggle
                    TextButton(onClick = { showPreview = !showPreview }) {
                        Icon(
                            if (showPreview) Icons.Default.Edit else Icons.Default.Preview,
                            contentDescription = null,
                            tint = ZexoSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showPreview) "Edit" else "Preview",
                            color = ZexoSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (showPreview) {
            // ── Preview Mode ───────────────────────────────────────────
            LandingPreview(
                heroTitle = heroTitle,
                heroSubtitle = heroSubtitle,
                heroDescription = heroDescription,
                features = features,
                ctaText = ctaText,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // ── Edit Mode ──────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ZexoBackground),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Hero Section ────────────────────────────────────────
                item {
                    SectionHeader(
                        icon = Icons.Default.AutoAwesome,
                        title = "Hero Section",
                        color = ZexoPrimary
                    )
                }

                item {
                    EditFieldCard(
                        label = "Hero Title",
                        value = heroTitle,
                        onValueChange = { heroTitle = it },
                        hint = "e.g., Welcome to Zexo",
                        singleLine = true
                    )
                }

                item {
                    EditFieldCard(
                        label = "Hero Subtitle",
                        value = heroSubtitle,
                        onValueChange = { heroSubtitle = it },
                        hint = "e.g., Connect Freely",
                        singleLine = true
                    )
                }

                item {
                    EditFieldCard(
                        label = "Hero Description",
                        value = heroDescription,
                        onValueChange = { heroDescription = it },
                        hint = "Brief description of your app…",
                        singleLine = false,
                        maxLines = 4
                    )
                }

                // ── Features Section ───────────────────────────────────
                item {
                    SectionHeader(
                        icon = Icons.Default.Star,
                        title = "Features List",
                        color = ZexoSecondary
                    )
                }

                // Existing features
                itemsIndexed(features, key = { index, _ -> "feature_$index" }) { index, feature ->
                    FeatureItem(
                        index = index + 1,
                        text = feature,
                        onRemove = {
                            features = features.toMutableList().apply { removeAt(index) }
                        }
                    )
                }

                // Add new feature
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ZexoSurface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newFeature,
                                onValueChange = { newFeature = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text("Add a feature…", color = ZexoTextSecondary, fontSize = 13.sp)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ZexoPrimary,
                                    unfocusedBorderColor = ZexoSurfaceLight,
                                    focusedContainerColor = ZexoBackground,
                                    unfocusedContainerColor = ZexoBackground,
                                    focusedTextColor = ZexoTextPrimary,
                                    unfocusedTextColor = ZexoTextPrimary,
                                    cursorColor = ZexoPrimary
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                            )
                            IconButton(
                                onClick = {
                                    if (newFeature.isNotBlank()) {
                                        features = features + newFeature.trim()
                                        newFeature = ""
                                    }
                                },
                                enabled = newFeature.isNotBlank()
                            ) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = "Add",
                                    tint = if (newFeature.isNotBlank()) ZexoGreen else ZexoTextSecondary
                                )
                            }
                        }
                    }
                }

                // ── CTA Button ─────────────────────────────────────────
                item {
                    SectionHeader(
                        icon = Icons.Default.TouchApp,
                        title = "Call to Action",
                        color = ZexoOrange
                    )
                }

                item {
                    EditFieldCard(
                        label = "CTA Button Text",
                        value = ctaText,
                        onValueChange = { ctaText = it },
                        hint = "e.g., Get Started",
                        singleLine = true
                    )
                }

                // ── Save Button ────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { saveLanding() },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZexoPrimary,
                            disabledContainerColor = ZexoSurfaceLight
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSaving) "Saving…" else "Save Landing Page",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Preview Composable
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LandingPreview(
    heroTitle: String,
    heroSubtitle: String,
    heroDescription: String,
    features: List<String>,
    ctaText: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ZexoBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero section with gradient background
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ZexoPrimary.copy(alpha = 0.3f),
                                ZexoBackground
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Logo placeholder
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ZexoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = heroTitle,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZexoTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = heroSubtitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZexoSecondary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = heroDescription,
                        fontSize = 14.sp,
                        color = ZexoTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // CTA Button
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = ZexoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = ctaText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Features section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Features",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZexoTextPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                features.forEachIndexed { index, feature ->
                    FeaturePreviewItem(
                        index = index,
                        text = feature
                    )
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FeaturePreviewItem(
    index: Int,
    text: String
) {
    val colors = listOf(ZexoPrimary, ZexoSecondary, ZexoGreen, ZexoOrange, ZexoBlue, ZexoAccent)
    val color = colors[index % colors.size]
    val icons = listOf(
        Icons.Default.Lock,
        Icons.Default.Call,
        Icons.Default.CameraAlt,
        Icons.Default.Devices,
        Icons.Default.Speed,
        Icons.Default.Favorite
    )
    val icon = icons[index % icons.size]

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ZexoSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ZexoTextPrimary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Reusable Components
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun EditFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ZexoSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZexoTextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(hint, color = ZexoTextSecondary)
                },
                singleLine = singleLine,
                maxLines = maxLines,
                shape = RoundedCornerShape(10.dp),
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
        }
    }
}

@Composable
private fun FeatureItem(
    index: Int,
    text: String,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ZexoSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Feature number
            Surface(
                color = ZexoPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$index",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZexoPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ZexoTextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = ZexoRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
