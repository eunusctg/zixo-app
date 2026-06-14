package com.zixo.app.ui.screens.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.ui.components.NavigationItem
import com.zixo.app.ui.components.SectionHeader
import com.zixo.app.ui.components.SwitchItem
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.TextSecondary

@Composable
fun AdvancedDataScreen(
    onBackClick: () -> Unit,
    viewModel: AdvancedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ZixoTopBar(
                title = "Data & Storage",
                showBackButton = true,
                onBackClick = onBackClick,
            )

            SectionHeader(title = "Media")

            NavigationItem(
                title = "Media Compression",
                subtitle = "Current: ${uiState.mediaCompressionProfile.name.lowercase().replace("_", " ")}",
                icon = Icons.Filled.Storage,
                onClick = { /* Placeholder */ },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "Self-Destruct")

            NavigationItem(
                title = "Default Self-Destruct Timer",
                subtitle = "Current: ${uiState.selfDestructDefault.name.lowercase().replace("_", " ")}",
                icon = Icons.Filled.Storage,
                onClick = { /* Placeholder */ },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "Debug")

            SwitchItem(
                title = "Debug Logging",
                subtitle = "Enable verbose logging for troubleshooting",
                icon = Icons.Filled.BugReport,
                checked = uiState.debugLoggingEnabled,
                onCheckedChange = viewModel::setDebugLoggingEnabled,
            )

            if (uiState.debugLoggingEnabled) {
                Text(
                    text = "Debug logs may contain sensitive information. Disable when not needed.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}
