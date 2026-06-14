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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zixo.app.ui.components.SectionHeader
import com.zixo.app.ui.components.SwitchItem
import com.zixo.app.ui.components.ZixoTopBar
import com.zixo.app.ui.theme.BackgroundGradientEnd
import com.zixo.app.ui.theme.BackgroundGradientStart
import com.zixo.app.ui.theme.DarkPetrolCharcoal
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextPrimary
import com.zixo.app.ui.theme.TextSecondary

@Composable
fun AdvancedNetworkScreen(
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
                title = "Network",
                showBackButton = true,
                onBackClick = onBackClick,
            )

            SectionHeader(title = "LiveKit Server")

            var liveKitUrl by remember(uiState.liveKitUrl) {
                mutableStateOf(uiState.liveKitUrl)
            }

            OutlinedTextField(
                value = liveKitUrl,
                onValueChange = { newValue ->
                    liveKitUrl = newValue
                    viewModel.setLiveKitUrl(newValue)
                },
                label = { Text("LiveKit URL", color = TextSecondary) },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonMint,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                    focusedLabelColor = NeonMint,
                    cursorColor = NeonMint,
                    focusedContainerColor = DarkPetrolCharcoal,
                    unfocusedContainerColor = DarkPetrolCharcoal,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            var sipPrefix by remember(uiState.sipOutboundPrefix) {
                mutableStateOf(uiState.sipOutboundPrefix)
            }

            OutlinedTextField(
                value = sipPrefix,
                onValueChange = { newValue ->
                    sipPrefix = newValue
                    viewModel.setSipOutboundPrefix(newValue)
                },
                label = { Text("SIP Outbound Prefix", color = TextSecondary) },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonMint,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                    focusedLabelColor = NeonMint,
                    cursorColor = NeonMint,
                    focusedContainerColor = DarkPetrolCharcoal,
                    unfocusedContainerColor = DarkPetrolCharcoal,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "WebRTC")

            SwitchItem(
                title = "Simulcast",
                subtitle = "Send multiple quality layers for adaptive streaming",
                checked = uiState.simulcastEnabled,
                onCheckedChange = viewModel::setSimulcastEnabled,
            )

            SwitchItem(
                title = "Force TURN Relay",
                subtitle = "Route all calls through TURN server for privacy",
                checked = uiState.forceTurnRelay,
                onCheckedChange = viewModel::setForceTurnRelay,
            )
        }
    }
}
