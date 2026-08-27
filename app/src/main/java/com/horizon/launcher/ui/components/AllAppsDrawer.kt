package com.horizon.launcher.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.ui.SearchBarField
import com.horizon.launcher.ui.theme.AccentCyan
import com.horizon.launcher.ui.theme.DarkBg
import com.horizon.launcher.ui.theme.LightBg

@Composable
fun AllAppsDrawer(
    isOpen: Boolean,
    appsList: List<AppModel>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onLaunchApp: (AppModel) -> Unit
) {
    if (!isOpen) return

    var drawerSearchQuery by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val filteredList = remember(appsList, drawerSearchQuery) {
        if (drawerSearchQuery.isBlank()) appsList
        else appsList.filter { it.label.contains(drawerSearchQuery, ignoreCase = true) }
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
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (selectedIndex > 0) {
                                selectedIndex--
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (selectedIndex < filteredList.size - 1) {
                                selectedIndex++
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (selectedIndex + 4 < filteredList.size) {
                                selectedIndex += 4
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (selectedIndex - 4 >= 0) {
                                selectedIndex -= 4
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                            val app = filteredList.getOrNull(selectedIndex)
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
                    .padding(16.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Todas las aplicaciones",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${filteredList.size})",
                            fontSize = 14.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.Medium
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

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar inside Drawer
                SearchBarField(
                    searchQuery = drawerSearchQuery,
                    onQueryChange = {
                        drawerSearchQuery = it
                        selectedIndex = 0
                    },
                    isDarkTheme = isDarkTheme,
                    isFocused = false,
                    focusRequester = remember { FocusRequester() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Full Grid of Apps
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron aplicaciones",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredList) { idx, app ->
                            AppCard(
                                app = app,
                                isSelected = idx == selectedIndex,
                                onSelect = { selectedIndex = idx },
                                onLaunch = {
                                    onLaunchApp(app)
                                    onDismiss()
                                },
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}
