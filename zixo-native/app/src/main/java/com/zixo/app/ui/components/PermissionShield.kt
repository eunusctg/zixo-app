package com.zixo.app.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

// ════════════════════════════════════════════════════════════════
// Permission Groups
// ════════════════════════════════════════════════════════════════

/**
 * Represents the permission groups required by different app features.
 *
 * Each group bundles related Android permissions together so the UI can
 * request them atomically and present a unified explanation to the user.
 */
enum class PermissionGroup(
    val permissions: List<String>,
    val label: String,
    val icon: ImageVector,
    val rationale: String,
) {
    MICROPHONE_ONLY(
        permissions = listOf(Manifest.permission.RECORD_AUDIO),
        label = "Microphone",
        icon = Icons.Outlined.Mic,
        rationale = "Zixo needs microphone access for voice messages and calls."
    ),
    CAMERA_ONLY(
        permissions = listOf(Manifest.permission.CAMERA),
        label = "Camera",
        icon = Icons.Outlined.CameraAlt,
        rationale = "Zixo needs camera access for video calls and photos."
    ),
    CAMERA_AND_MICROPHONE(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        ),
        label = "Camera & Microphone",
        icon = Icons.Outlined.CameraAlt,
        rationale = "Zixo needs camera and microphone access for video calls."
    ),
    LOCATION(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        label = "Location",
        icon = Icons.Outlined.LocationOn,
        rationale = "Zixo needs location access to share your location."
    ),
    MEDIA_READ(
        permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        },
        label = "Media & Files",
        icon = Icons.Outlined.PermMedia,
        rationale = "Zixo needs access to your photos and videos to share media."
    ),
    CALL(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        ),
        label = "Call Access",
        icon = Icons.Outlined.Mic,
        rationale = "Zixo needs microphone and camera access for calls."
    ),
    VIDEO_CALL(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        ),
        label = "Video Call Access",
        icon = Icons.Outlined.CameraAlt,
        rationale = "Zixo needs microphone and camera access for video calls."
    );

    /**
     * Returns only the permissions that are not yet granted for the given context.
     */
    fun ungrantedPermissions(context: Context): List<String> =
        permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED
        }

    /**
     * Whether all permissions in this group are granted for the given context.
     */
    fun areAllGranted(context: Context): Boolean =
        ungrantedPermissions(context).isEmpty()
}

// ════════════════════════════════════════════════════════════════
// Permission Shield State
// ════════════════════════════════════════════════════════════════

/**
 * State holder for tracking permission grant results.
 */
@Stable
class PermissionShieldState(
    initialGranted: Map<PermissionGroup, Boolean> = emptyMap()
) {
    var grantedGroups by mutableStateOf(initialGranted)
        private set

    var isRequesting by mutableStateOf(false)
        private set

    var deniedPermissions by mutableStateOf<List<String>>(emptyList())
        private set

    var isPermanentlyDenied by mutableStateOf(false)
        private set

    /** Whether a specific permission group has been fully granted. */
    fun isGranted(group: PermissionGroup): Boolean =
        grantedGroups[group] == true

    /** Updates the granted state for a permission group. */
    fun updateGroup(group: PermissionGroup, isGranted: Boolean) {
        grantedGroups = grantedGroups + (group to isGranted)
    }

    /** Marks that a permission request is in progress. */
    fun setRequesting(requesting: Boolean) {
        isRequesting = requesting
    }

    /** Updates the list of denied permissions. */
    fun setDenied(permissions: List<String>) {
        deniedPermissions = permissions
    }

    /** Marks that the user has permanently denied a permission (checked "Don't ask again"). */
    fun setPermanentlyDenied(denied: Boolean) {
        isPermanentlyDenied = denied
    }

    /** Resets all state. */
    fun reset() {
        grantedGroups = emptyMap()
        isRequesting = false
        deniedPermissions = emptyList()
        isPermanentlyDenied = false
    }
}

// ════════════════════════════════════════════════════════════════
// PermissionGate — Declarative Permission Wrapper
// ════════════════════════════════════════════════════════════════

/**
 * A composable that wraps content requiring specific permissions.
 *
 * Three states are rendered:
 * 1. **Permission granted** → shows [content] normally
 * 2. **Permission not granted** → shows a liquid glass permission request UI
 *    with an explanation and "Grant Permission" button
 * 3. **Permanently denied** → shows an "Open Settings" button that navigates
 *    to the system app settings page
 *
 * The rationale dialog is liquid glass styled to match the app's visual language.
 *
 * Usage:
 * ```kotlin
 * PermissionGate(
 *     requiredPermission = PermissionGroup.CAMERA_AND_MICROPHONE,
 *     onPermissionsResult = { granted ->
 *         if (granted) startVideoCall()
 *     }
 * ) {
 *     // Content shown only when permissions are granted
 *     VideoCallView()
 * }
 * ```
 *
 * @param requiredPermission The permission group that must be granted.
 * @param onPermissionsResult Callback invoked with whether all permissions were granted.
 * @param content The composable content to render when permissions are granted.
 */
@Composable
fun PermissionGate(
    requiredPermission: PermissionGroup,
    onPermissionsResult: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val state = remember { PermissionShieldState() }

    // Check current grant status
    val allGranted = remember(requiredPermission) {
        requiredPermission.areAllGranted(context)
    }

    // Permission request launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allResultsGranted = results.values.all { it }
        state.updateGroup(requiredPermission, allResultsGranted)
        state.setRequesting(false)

        val denied = results.filter { !it.value }.keys.toList()
        state.setDenied(denied)

        // Check if any permission was permanently denied
        // (user checked "Don't ask again" and still denied)
        if (!allResultsGranted && denied.isNotEmpty()) {
            val permanentlyDenied = denied.any { perm ->
                !context.shouldShowRequestPermissionRationale(perm) &&
                        ContextCompat.checkSelfPermission(context, perm) !=
                        PackageManager.PERMISSION_GRANTED
            }
            state.setPermanentlyDenied(permanentlyDenied)
        }

        onPermissionsResult(allResultsGranted)
    }

    // Dialog state for rationale
    var showRationaleDialog by remember { mutableStateOf(false) }

    if (allGranted || state.isGranted(requiredPermission)) {
        // ── Permission already granted — render content ──────────
        content()
    } else if (state.isPermanentlyDenied) {
        // ── Permanently denied — show settings redirect ──────────
        PermissionDeniedView(
            permissionGroup = requiredPermission,
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    } else {
        // ── Permission not yet granted — show request UI ─────────
        PermissionRequestView(
            permissionGroup = requiredPermission,
            isRequesting = state.isRequesting,
            onRequestPermission = {
                // Check if we should show rationale first
                val shouldShowRationale = requiredPermission.permissions.any { perm ->
                    context.shouldShowRequestPermissionRationale(perm)
                }

                if (shouldShowRationale) {
                    showRationaleDialog = true
                } else {
                    state.setRequesting(true)
                    val ungranted = requiredPermission.ungrantedPermissions(context)
                    if (ungranted.isNotEmpty()) {
                        launcher.launch(ungranted.toTypedArray())
                    } else {
                        state.updateGroup(requiredPermission, true)
                        onPermissionsResult(true)
                    }
                }
            }
        )

        // ── Liquid Glass Rationale Dialog ────────────────────────
        if (showRationaleDialog) {
            PermissionRationaleDialog(
                permissionGroup = requiredPermission,
                onConfirm = {
                    showRationaleDialog = false
                    state.setRequesting(true)
                    val ungranted = requiredPermission.ungrantedPermissions(context)
                    if (ungranted.isNotEmpty()) {
                        launcher.launch(ungranted.toTypedArray())
                    } else {
                        state.updateGroup(requiredPermission, true)
                        onPermissionsResult(true)
                    }
                },
                onDismiss = {
                    showRationaleDialog = false
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
// PermissionShield — Imperative Permission Check
// ════════════════════════════════════════════════════════════════

/**
 * Imperative permission check composable that auto-requests permissions
 * when the composable enters composition.
 *
 * Unlike [PermissionGate], this does not render a permission request UI.
 * It simply checks if permissions are granted and auto-requests them if not.
 * Use this for cases where you need to gate an action but don't want to
 * replace UI content.
 *
 * @param requiredPermissions The permission group that must be granted.
 * @param onPermissionsResult Callback invoked with whether all permissions were granted.
 * @param content The composable content to render when permissions are granted.
 */
@Composable
fun PermissionShield(
    requiredPermissions: PermissionGroup,
    onPermissionsResult: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val state = remember { PermissionShieldState() }

    val allGranted = remember(requiredPermissions) {
        requiredPermissions.areAllGranted(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allResultsGranted = results.values.all { it }
        state.updateGroup(requiredPermissions, allResultsGranted)
        state.setRequesting(false)

        val denied = results.filter { !it.value }.keys.toList()
        state.setDenied(denied)

        onPermissionsResult(allResultsGranted)
    }

    if (allGranted || state.isGranted(requiredPermissions)) {
        content()
    } else {
        LaunchedEffect(requiredPermissions) {
            if (!state.isRequesting) {
                state.setRequesting(true)
                val ungranted = requiredPermissions.ungrantedPermissions(context)
                if (ungranted.isNotEmpty()) {
                    launcher.launch(ungranted.toTypedArray())
                } else {
                    state.updateGroup(requiredPermissions, true)
                    onPermissionsResult(true)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Liquid Glass Permission UI Components
// ════════════════════════════════════════════════════════════════

/** Semi-transparent dark surface for liquid glass panels. */
private val GlassSurfaceColor = Color(0x1A1A2A32)
/** High-gloss border for liquid glass panels. */
private val GlassBorderColor = Color(0x33FFFFFF)

/**
 * Permission request view styled with the Liquid Glass design language.
 *
 * Displays the permission icon, label, and rationale text,
 * along with a "Grant Permission" button.
 */
@Composable
private fun PermissionRequestView(
    permissionGroup: PermissionGroup,
    isRequesting: Boolean,
    onRequestPermission: () -> Unit,
) {
    // Pulsing animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "perm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Pulsing Icon ──
            Box(
                modifier = Modifier
                    .size(72.dp * pulseScale)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassSurfaceColor)
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = permissionGroup.icon,
                    contentDescription = permissionGroup.label,
                    tint = NeonMint,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = permissionGroup.label,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = permissionGroup.rationale,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onRequestPermission,
                enabled = !isRequesting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMint,
                    contentColor = Color(0xFF003A1F),
                    disabledContainerColor = NeonMint.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isRequesting) "Requesting…" else "Grant Permission",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Permission denied view with "Open Settings" option.
 *
 * Shown when the user has permanently denied a permission
 * (checked "Don't ask again").
 */
@Composable
private fun PermissionDeniedView(
    permissionGroup: PermissionGroup,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x33FF5252))
                    .border(1.dp, Color(0x55FF5252), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = permissionGroup.icon,
                    contentDescription = permissionGroup.label,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "${permissionGroup.label} Access Denied",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You've denied this permission. To use this feature, " +
                        "please enable it in Settings.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMint,
                    contentColor = Color(0xFF003A1F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Open Settings",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Liquid Glass-styled rationale dialog shown before the system permission request.
 *
 * Explains *why* the app needs the permission and offers
 * "Continue" / "Not Now" buttons.
 */
@Composable
private fun PermissionRationaleDialog(
    permissionGroup: PermissionGroup,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GlassSurfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(24.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Icon ──
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonMint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = permissionGroup.icon,
                        contentDescription = null,
                        tint = NeonMint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Allow ${permissionGroup.label}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = permissionGroup.rationale,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Action Buttons ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x1AFFFFFF),
                            contentColor = TextSecondary
                        )
                    ) {
                        Text(
                            text = "Not Now",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMint,
                            contentColor = Color(0xFF003A1F)
                        )
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// Context Extension Utilities
// ════════════════════════════════════════════════════════════════

/**
 * Utility function to check if a specific permission is granted.
 */
fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Utility function to check if all permissions in a group are granted.
 */
fun Context.arePermissionsGranted(group: PermissionGroup): Boolean =
    group.areAllGranted(this)
