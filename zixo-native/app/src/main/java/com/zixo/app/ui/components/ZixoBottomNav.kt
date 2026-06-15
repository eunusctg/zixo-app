package com.zixo.app.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zixo.app.ui.theme.NeonMint
import com.zixo.app.ui.theme.TextSecondary

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
        label = "Contacts",
        icon = {
            Icon(
                imageVector = Icons.Outlined.Contacts,
                contentDescription = "Contacts",
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

/**
 * iOS Liquid Glass-styled bottom navigation bar.
 * Fixed at exactly 85dp height for comfortable touch targets.
 * Uses the liquidGlassNavItem modifier for the frosted glass visual.
 * Floats smoothly above the fluid background with the Liquid Glass effect.
 */
@Composable
fun ZixoBottomNav(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier
            .height(85.dp)
            .liquidGlassNavItem(),
        containerColor = Color.Transparent,
        contentColor = NeonMint,
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
                    selectedIconColor = NeonMint,
                    selectedTextColor = NeonMint,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = NeonMint.copy(alpha = 0.15f),
                ),
            )
        }
    }
}
