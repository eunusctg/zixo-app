package com.zixo.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AccentGreen = Color(0xFF00E676)
private val TextSecondary = Color(0xFF90A4AE)
private val NavBackground = Color(0xFF1A2A32)

data class BottomNavItem(
    val label: String,
    val icon: @Composable () -> Unit,
)

private val bottomNavItems = listOf(
    BottomNavItem(
        label = "Chats",
        icon = {
            Icon(
                imageVector = Icons.Outlined.Chat,
                contentDescription = "Chats",
                modifier = Modifier.size(24.dp),
            )
        },
    ),
    BottomNavItem(
        label = "Calls",
        icon = {
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = "Calls",
                modifier = Modifier.size(24.dp),
            )
        },
    ),
    BottomNavItem(
        label = "Settings",
        icon = {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp),
            )
        },
    ),
)

@Composable
fun ZixoBottomNav(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = NavBackground,
        contentColor = AccentGreen,
    ) {
        bottomNavItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = item.icon,
                label = {
                    Text(
                        text = item.label,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentGreen,
                    selectedTextColor = AccentGreen,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = AccentGreen.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
