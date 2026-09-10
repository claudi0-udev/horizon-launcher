package com.horizon.launcher.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizon.launcher.sound.SoundEffectManager
import com.horizon.launcher.ui.theme.AccentCyan
import com.horizon.launcher.ui.theme.DarkBg
import com.horizon.launcher.ui.theme.LightBg
import com.horizon.launcher.update.UpdateInfo
import com.horizon.launcher.update.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    isOpen: Boolean,
    updateInfo: UpdateInfo?,
    updateManager: UpdateManager,
    soundManager: SoundEffectManager,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen || updateInfo == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val backgroundColor = if (isDarkTheme) DarkBg else LightBg
    val cardBg = if (isDarkTheme) Color(0xFF2E2E32) else Color(0xFFF2F2F2)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF2D2D2D)
    val subtextColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .border(2.dp, AccentCyan, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Actualización",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Nueva Versión Disponible",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "v${updateInfo.currentVersion} ➔ ${updateInfo.latestVersion}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentCyan
                                )
                            }
                        }

                        if (!isDownloading) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = textColor
                                )
                            }
                        }
                    }

                    // Release notes
                    Text(
                        text = "Notas de la versión:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = updateInfo.releaseNotes.ifBlank { "Novedades y mejoras de rendimiento de Horizon Launcher." },
                            fontSize = 12.5.sp,
                            color = textColor,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    // Progress or Error
                    if (isDownloading) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Descargando actualización...",
                                    fontSize = 12.sp,
                                    color = subtextColor
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            }
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                color = AccentCyan,
                                trackColor = cardBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isDownloading) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = "Más tarde",
                                    color = subtextColor,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    soundManager.playSelectSound()
                                    if (updateInfo.downloadUrl.endsWith(".apk", ignoreCase = true)) {
                                        isDownloading = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            val result = updateManager.downloadAndInstallApk(updateInfo.downloadUrl) { prog ->
                                                downloadProgress = prog
                                            }
                                            isDownloading = false
                                            if (result.isFailure) {
                                                errorMessage = "Error al descargar: ${result.exceptionOrNull()?.localizedMessage}"
                                                Toast.makeText(context, "Error en la descarga", Toast.LENGTH_SHORT).show()
                                            } else {
                                                onDismiss()
                                            }
                                        }
                                    } else {
                                        // Open release webpage if no direct APK asset
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                                        context.startActivity(intent)
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Descargar",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Actualizar Ahora",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
