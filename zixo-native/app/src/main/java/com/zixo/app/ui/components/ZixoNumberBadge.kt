package com.zixo.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentGreen = Color(0xFF00E676)
private val TextSecondary = Color(0xFF90A4AE)
private val CardBackground = Color(0xFF1A2A32)

@Composable
fun ZixoNumberBadge(
    zixoNumber: String,
    modifier: Modifier = Modifier,
) {
    val formattedNumber = formatZixoNumber(zixoNumber)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = AccentGreen.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Zixo Number",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
            )

            Text(
                text = formattedNumber,
                color = AccentGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Formats an 8-digit number as two 4-digit blocks separated by a space.
 * e.g., "12345678" -> "1234 5678"
 */
private fun formatZixoNumber(number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (digits.length >= 8) {
        "${digits.substring(0, 4)} ${digits.substring(4, 8)}"
    } else {
        digits
    }
}
