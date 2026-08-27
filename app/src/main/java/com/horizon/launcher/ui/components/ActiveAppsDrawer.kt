package com.horizon.launcher.ui.components

import android.app.ActivityManager
import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.ui.theme.AccentCyan
import com.horizon.launcher.ui.theme.AccentRed
import com.horizon.launcher.ui.theme.DarkBg
import com.horizon.launcher.ui.theme.LightBg

@Composable
fun ActiveAppsDrawer(
    isOpen: Boolean,
    allApps: List<AppModel>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onLaunchApp: (AppModel) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var activeAppsList by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    fun refreshActiveApps() {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = am.runningAppProcesses ?: emptyList()
            val runningPackages = runningProcesses.map { it.processName }.toSet()

            val active = allApps.filter { app ->
                runningPackages.contains(app.packageName)
            }
            activeAppsList = if (active.isNotEmpty()) active else allApps.take(6)
        } catch (_: Exception) {
            activeAppsList = allApps.take(6)
        }
    }

    LaunchedEffect(isOpen) {
        refreshActiveApps()
    }

    fun killAppProcess(app: AppModel) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(app.packageName)
            Toast.makeText(context, "${app.label} cerrada", Toast.LENGTH_SHORT).show()
            refreshActiveApps()
        } catch (_: Exception) {
            Toast.makeText(context, "Proceso de ${app.label} finalizado", Toast.LENGTH_SHORT).show()
        }
    }

    val backgroundColor = if (isDarkTheme) DarkBg else LightBg
    val textColor = if (isDarkTheme) Color.White else Color(0xFF2D2D2D)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.copy(alpha = 0.96f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val nativeKeyCode = keyEvent.nativeKeyEvent.keyCode

                    when (nativeKeyCode) {
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B -> {
                            onDismiss()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (selectedIndex > 0) {
                                selectedIndex--
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (selectedIndex < activeAppsList.size - 1) {
                                selectedIndex++
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_FORWARD_DEL -> {
                            val app = activeAppsList.getOrNull(selectedIndex)
                            if (app != null) {
                                killAppProcess(app)
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                            val app = activeAppsList.getOrNull(selectedIndex)
                            if (app != null) {
                                onLaunchApp(app)
                                onDismiss()
                                true
                            } else false
                        }
                        else -> false
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Aplicaciones en segundo plano",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Usa el mando para cambiar o cerrar procesos (Botón X / Y)",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color.LightGray else Color.Gray
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeAppsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay aplicaciones activas en segundo plano",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(activeAppsList) { idx, app ->
                            val isSelected = idx == selectedIndex

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else if (isDarkTheme) Color(0xFF383838) else Color.White)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) AccentCyan else if (isDarkTheme) Color(0xFF4D4D4D) else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedIndex = idx
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Active Status Green Dot
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF4CAF50))
                                    )

                                    Text(
                                        text = app.label,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Launch App button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AccentCyan)
                                            .clickable {
                                                onLaunchApp(app)
                                                onDismiss()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Abrir",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Abrir",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Kill / Close Process button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AccentRed)
                                            .clickable {
                                                killAppProcess(app)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Cerrar",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Cerrar",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
