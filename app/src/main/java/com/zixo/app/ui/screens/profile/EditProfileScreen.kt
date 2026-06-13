package com.zixo.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zixo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit, viewModel: EditProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> viewModel.onImageSelected(uri) }

    LaunchedEffect(uiState.saveSuccess) { if (uiState.saveSuccess) onBack() }

    Column(modifier = Modifier.fillMaxSize().background(ZixoBg)) {
        Surface(color = ZixoSurface, tonalElevation = 2.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ZixoText) }
                Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ZixoText)
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(120.dp).shadow(8.dp, CircleShape).clickable { imagePicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                val imageModel = uiState.selectedImageUri ?: uiState.avatarUrl.ifEmpty { null }
                AsyncImage(model = imageModel, contentDescription = null, modifier = Modifier.size(120.dp).clip(CircleShape).background(ZixoSurfaceLight))
                if (imageModel == null) { Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = ZixoTextSecondary, modifier = Modifier.size(36.dp)) }
                Surface(color = ZixoBg.copy(alpha = 0.6f), shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ZixoText, modifier = Modifier.size(20.dp).padding(2.dp)) }
            }
            Text("Tap to change profile photo", color = ZixoTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Surface(color = ZixoSurface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Zixo Number", color = ZixoTextSecondary, fontSize = 11.sp)
                        val num = uiState.zixoNumber
                        if (num.length == 8) { Text("${num.substring(0,4)} ${num.substring(4)}", color = ZixoPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 2.sp) }
                        else if (num.isNotEmpty()) { Text(num, color = ZixoPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                        else { Text("Not assigned", color = ZixoTextSecondary, fontSize = 16.sp) }
                    }
                    IconButton(onClick = { if (uiState.zixoNumber.isNotEmpty()) { val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Zixo Number", uiState.zixoNumber)) } }) { Icon(Icons.Default.ContentCopy, null, tint = ZixoTextSecondary) }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(value = uiState.displayName, onValueChange = viewModel::onDisplayNameChange, label = { Text("Display Name", color = ZixoTextSecondary) }, modifier = Modifier.fillMaxWidth(), singleLine = true, supportingText = { Text("${uiState.displayName.length}/30", color = ZixoTextSecondary, fontSize = 11.sp) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ZixoText, unfocusedTextColor = ZixoText, focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight, cursorColor = ZixoPrimary, focusedContainerColor = ZixoSurface, unfocusedContainerColor = ZixoSurface), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = uiState.username, onValueChange = {}, label = { Text("Username", color = ZixoTextSecondary) }, modifier = Modifier.fillMaxWidth(), readOnly = true, enabled = false, supportingText = { Text("Username cannot be changed.", color = ZixoTextSecondary, fontSize = 11.sp) }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = ZixoTextSecondary, disabledBorderColor = ZixoHighlight, disabledLabelColor = ZixoTextSecondary, disabledContainerColor = ZixoBg), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = uiState.phone.ifEmpty { "Not set" }, onValueChange = {}, label = { Text("Phone Number", color = ZixoTextSecondary) }, modifier = Modifier.fillMaxWidth(), readOnly = true, enabled = false, supportingText = { Text("Phone number cannot be changed.", color = ZixoTextSecondary, fontSize = 11.sp) }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = ZixoTextSecondary, disabledBorderColor = ZixoHighlight, disabledLabelColor = ZixoTextSecondary, disabledContainerColor = ZixoBg), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = uiState.bio, onValueChange = viewModel::onBioChange, label = { Text("Bio / Status", color = ZixoTextSecondary) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, supportingText = { Text("${uiState.bio.length}/100", color = ZixoTextSecondary, fontSize = 11.sp) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ZixoText, unfocusedTextColor = ZixoText, focusedBorderColor = ZixoPrimary, unfocusedBorderColor = ZixoHighlight, cursorColor = ZixoPrimary, focusedContainerColor = ZixoSurface, unfocusedContainerColor = ZixoSurface), shape = RoundedCornerShape(12.dp))
            uiState.error?.let { Spacer(modifier = Modifier.height(8.dp)); Text(it, color = ZixoError, fontSize = 13.sp) }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = viewModel::saveChanges, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ZixoPrimary), enabled = !uiState.isSaving) {
                if (uiState.isSaving) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ZixoBg, strokeWidth = 2.dp) } else { Text("Save Changes", color = ZixoBg, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
