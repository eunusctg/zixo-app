package com.zexo.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.zexo.app.data.model.AdminConfig
import com.zexo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    navController: NavHostController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val adminConfig by viewModel.adminConfig.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val message by viewModel.message.collectAsState()

    // Local state for editing
    var maintenanceMode by remember { mutableStateOf(adminConfig.maintenanceMode) }
    var forceUpdate by remember { mutableStateOf(adminConfig.forceUpdate) }
    var registrationEnabled by remember { mutableStateOf(adminConfig.registrationEnabled) }
    var minVersion by remember { mutableStateOf(adminConfig.minVersion) }
    var latestVersion by remember { mutableStateOf(adminConfig.latestVersion) }
    var customMessage by remember { mutableStateOf(adminConfig.message) }

    // Sync when config loads from Firestore
    LaunchedEffect(adminConfig) {
        maintenanceMode = adminConfig.maintenanceMode
        forceUpdate = adminConfig.forceUpdate
        registrationEnabled = adminConfig.registrationEnabled
        minVersion = adminConfig.minVersion
        latestVersion = adminConfig.latestVersion
        customMessage = adminConfig.message
    }

    // SnackBar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    fun saveConfig() {
        viewModel.updateAdminConfig(
            AdminConfig(
                maintenanceMode = maintenanceMode,
                forceUpdate = forceUpdate,
                latestVersion = latestVersion,
                minVersion = minVersion,
                message = customMessage,
                registrationEnabled = registrationEnabled
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
                        text = "App Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZexoTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    // Save button in top bar
                    TextButton(
                        onClick = { saveConfig() },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ZexoPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = ZexoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Save",
                            color = ZexoPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ZexoBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── App Configuration Section ──────────────────────────────
            item {
                SectionHeader(
                    icon = Icons.Default.Build,
                    title = "App Configuration",
                    color = ZexoPrimary
                )
            }

            // Maintenance mode
            item {
                SettingsToggleCard(
                    icon = Icons.Default.Construction,
                    title = "Maintenance Mode",
                    subtitle = "Temporarily disable the app for all users",
                    checked = maintenanceMode,
                    onCheckedChange = { maintenanceMode = it },
                    checkedColor = ZexoRed
                )
            }

            // Force update
            item {
                SettingsToggleCard(
                    icon = Icons.Default.SystemUpdate,
                    title = "Force Update",
                    subtitle = "Require users to update to the latest version",
                    checked = forceUpdate,
                    onCheckedChange = { forceUpdate = it },
                    checkedColor = ZexoOrange
                )
            }

            // Registration enabled
            item {
                SettingsToggleCard(
                    icon = Icons.Default.PersonAdd,
                    title = "Registration Enabled",
                    subtitle = "Allow new users to create accounts",
                    checked = registrationEnabled,
                    onCheckedChange = { registrationEnabled = it },
                    checkedColor = ZexoGreen
                )
            }

            // ── Version Section ────────────────────────────────────────
            item {
                SectionHeader(
                    icon = Icons.Default.NewReleases,
                    title = "Version Management",
                    color = ZexoSecondary
                )
            }

            // Minimum version
            item {
                VersionInputCard(
                    label = "Minimum Version",
                    value = minVersion,
                    onValueChange = { minVersion = it },
                    hint = "e.g. 1.0.0"
                )
            }

            // Latest version
            item {
                VersionInputCard(
                    label = "Latest Version",
                    value = latestVersion,
                    onValueChange = { latestVersion = it },
                    hint = "e.g. 1.3.0"
                )
            }

            // ── Custom Message ─────────────────────────────────────────
            item {
                SectionHeader(
                    icon = Icons.Default.Message,
                    title = "Custom Message",
                    color = ZexoOrange
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ZexoSurface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "This message will be shown to all users (e.g., during maintenance or announcements)",
                            fontSize = 12.sp,
                            color = ZexoTextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = customMessage,
                            onValueChange = { customMessage = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Enter custom message…", color = ZexoTextSecondary)
                            },
                            singleLine = false,
                            maxLines = 4,
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
                    }
                }
            }

            // ── Save Button ────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { saveConfig() },
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
                        text = if (isSaving) "Saving…" else "Save Settings",
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
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color = ZexoPrimary
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ZexoSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (checked) checkedColor.copy(alpha = 0.15f) else ZexoSurfaceLight,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) checkedColor else ZexoTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZexoTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = ZexoTextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = checkedColor,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = ZexoSurfaceLight,
                    uncheckedThumbColor = ZexoTextSecondary
                )
            )
        }
    }
}

@Composable
private fun VersionInputCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String
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
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                leadingIcon = {
                    Icon(
                        Icons.Default.Tag,
                        contentDescription = null,
                        tint = ZexoTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
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
