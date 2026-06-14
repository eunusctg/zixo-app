package com.zixo.app.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Centralized Runtime Permission Shield for the Zixo application.
 *
 * Injects an automated runtime interceptor before opening any camera view,
 * voice text, chat instance, or calling route. The UI must verify and
 * trigger standard system permissions smoothly before accessing:
 * - RECORD_AUDIO (Microphone access)
 * - CAMERA (Video hardware)
 * - ACCESS_FINE_LOCATION & ACCESS_COARSE_LOCATION (Location)
 * - READ_MEDIA_IMAGES & READ_MEDIA_VIDEO (Files/Media on API 33+)
 */

/**
 * Represents the permission groups required by different app features.
 */
enum class PermissionGroup(val permissions: List<String>) {
    CAMERA_AND_MICROPHONE(
        permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
        }
    ),
    MICROPHONE_ONLY(
        permissions = listOf(Manifest.permission.RECORD_AUDIO)
    ),
    CAMERA_ONLY(
        permissions = listOf(Manifest.permission.CAMERA)
    ),
    LOCATION(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ),
    MEDIA_READ(
        permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    ),
    CALL(
        permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
        }
    ),
    VIDEO_CALL(
        permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
        }
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

    /**
     * Whether a specific permission group has been fully granted.
     */
    fun isGranted(group: PermissionGroup): Boolean =
        grantedGroups[group] == true

    /**
     * Updates the granted state for a permission group.
     */
    fun updateGroup(group: PermissionGroup, isGranted: Boolean) {
        grantedGroups = grantedGroups + (group to isGranted)
    }

    /**
     * Marks that a permission request is in progress.
     */
    fun setRequesting(requesting: Boolean) {
        isRequesting = requesting
    }

    /**
     * Updates the list of denied permissions.
     */
    fun setDenied(permissions: List<String>) {
        deniedPermissions = permissions
    }

    /**
     * Resets all state.
     */
    fun reset() {
        grantedGroups = emptyMap()
        isRequesting = false
        deniedPermissions = emptyList()
    }
}

/**
 * Composable that checks and requests runtime permissions before allowing
 * access to protected features.
 *
 * Usage:
 * ```kotlin
 * PermissionShield(
 *     requiredPermissions = PermissionGroup.CAMERA_AND_MICROPHONE,
 *     onPermissionsResult = { allGranted ->
 *         if (allGranted) {
 *             // Open camera / start call
 *         } else {
 *             // Show permission denied message
 *         }
 *     }
 * ) {
 *     // Content to show when permissions are already granted
 *     // or after the permission flow completes
 * }
 * ```
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

    // Check if all permissions are already granted
    val allGranted = remember(requiredPermissions) {
        requiredPermissions.areAllGranted(context)
    }

    // Launcher for requesting permissions
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

    // If permissions are already granted, render content immediately
    if (allGranted || state.isGranted(requiredPermissions)) {
        content()
    } else {
        // Auto-request permissions if not already requesting
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

        // Show a permission request placeholder while waiting
        // The calling screen should handle the visual representation
    }
}

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
