package com.zixo.app.ui.screens.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.ui.components.NavigationItem
import com.zixo.app.ui.components.SectionHeader
import com.zixo.app.ui.components.SwitchItem
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart

@Composable
fun AdvancedSecurityScreen(
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
                title = "Security",
                showBackButton = true,
                onBackClick = onBackClick,
            )

            SectionHeader(title = "Self-Destruct Messages")

            SwitchItem(
                title = "App Switcher Privacy Blur",
                subtitle = "Blur app content in the recent apps switcher",
                icon = Icons.Filled.AppShortcut,
                checked = uiState.appSwitcherPrivacyBlur,
                onCheckedChange = viewModel::setAppSwitcherPrivacyBlur,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "Encryption")

            NavigationItem(
                title = "Encryption Key",
                subtitle = "View your end-to-end encryption key fingerprint",
                icon = Icons.Filled.Fingerprint,
                onClick = { /* Navigation handled by nav graph */ },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "Data")

            NavigationItem(
                title = "Clear Cache",
                subtitle = "Free up storage by clearing cached media",
                icon = Icons.Filled.DeleteSweep,
                onClick = { /* Placeholder */ },
            )
        }
    }
}
