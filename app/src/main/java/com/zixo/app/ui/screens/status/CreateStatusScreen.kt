package com.zixo.app.ui.screens.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zixo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStatusScreen(onBack: () -> Unit, viewModel: StatusViewModel = hiltViewModel()) {
    var textContent by remember { mutableStateOf("") }
    var selectedBgColor by remember { mutableStateOf("#1A2A32") }
    var selectedSymbols by remember { mutableStateOf<List<String>>(emptyList()) }

    val bgColors = listOf("#1A2A32" to "Petrol", "#0B1519" to "Slate", "#1B5E20" to "Forest", "#4A148C" to "Purple", "#B71C1C" to "Crimson", "#0D47A1" to "Ocean", "#E65100" to "Sunset", "#004D40" to "Teal", "#1A237E" to "Indigo", "#3E2723" to "Cocoa")
    val symbols3D = listOf("\u2728", "\u2B50", "\uD83C\uDF1F", "\uD83D\uDD25", "\u2764\uFE0F", "\uD83D\uDC4D", "\uD83C\uDF89", "\uD83D\uDE80", "\u26A1", "\uD83C\uDF08", "\uD83C\uDF0A", "\uD83D\uDCAE", "\u2600\uFE0F", "\uD83C\uDF19", "\uD83D\uDC8E")

    Column(modifier = Modifier.fillMaxSize().background(ZixoBg)) {
        Surface(color = ZixoSurface, tonalElevation = 2.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ZixoText) }
                Text("Create Status", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ZixoText)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
            Surface(color = parseHexColor(selectedBgColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (textContent.isNotEmpty()) { Text(textContent, color = ZixoText, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 34.sp, modifier = Modifier.padding(24.dp)) } else { Text("Type a status...", color = ZixoTextSecondary, fontSize = 18.sp) }
                    if (selectedSymbols.isNotEmpty()) { LazyRow(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(selectedSymbols) { Text(it, fontSize = 32.sp) } } }
                }
            }
        }
        OutlinedTextField(value = textContent, onValueChange = { textContent = it }, label = { Text("Status text", color = ZixoTextSecondary) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ZixoText, unfocusedTextColor = ZixoText, focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight, cursorColor = ZixoPrimary, focusedContainerColor = ZixoSurface, unfocusedContainerColor = ZixoSurface), shape = RoundedCornerShape(12.dp), maxLines = 3)
        Text("Background", color = ZixoTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
        LazyRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bgColors) { (hex, _) -> Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(parseHexColor(hex)).border(width = if (selectedBgColor == hex) 3.dp else 1.dp, color = if (selectedBgColor == hex) ZixoPrimary else ZixoHighlight, shape = CircleShape).clickable { selectedBgColor = hex }) }
        }
        Text("3D Symbols", color = ZixoTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp))
        LazyRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(symbols3D) { symbol -> TextButton(onClick = { selectedSymbols = if (selectedSymbols.contains(symbol)) selectedSymbols - symbol else selectedSymbols + symbol }, modifier = Modifier.size(40.dp).border(width = if (selectedSymbols.contains(symbol)) 2.dp else 0.dp, color = if (selectedSymbols.contains(symbol)) ZixoPrimary else Color.Transparent, shape = RoundedCornerShape(8.dp))) { Text(symbol, fontSize = 22.sp) } }
        }
        Button(onClick = { if (textContent.isNotBlank()) { viewModel.createStatus(textContent, selectedBgColor, selectedSymbols); onBack() } }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ZixoPrimary), enabled = textContent.isNotBlank()) { Text("Post Status", color = ZixoBg, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
}

private fun parseHexColor(hex: String): Color = try { Color(0xFF000000 or hex.removePrefix("#").toLong(16)) } catch (e: Exception) { ZixoSurface }
