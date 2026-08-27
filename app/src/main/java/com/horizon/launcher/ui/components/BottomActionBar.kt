package com.horizon.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizon.launcher.ui.theme.AccentCyan

data class ActionButton(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun BottomActionBar(
    isDarkTheme: Boolean,
    isLandscape: Boolean,
    onOpenBrowser: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenControllers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPower: () -> Unit,
    onOpenActiveApps: () -> Unit,
    onOpenLauncherPicker: () -> Unit,
    onOpenAllApps: () -> Unit,
    focusRequesters: List<FocusRequester> = emptyList(),
    modifier: Modifier = Modifier
) {
    val activeButtons = listOf(
        ActionButton(
            id = "browser",
            title = "Navegador",
            icon = Icons.Default.Language,
            color = Color(0xFF2196F3),
            onClick = onOpenBrowser
        ),
        ActionButton(
            id = "gallery",
            title = "Galería",
            icon = Icons.Default.PhotoLibrary,
            color = Color(0xFF00BCD4),
            onClick = onOpenGallery
        ),
        ActionButton(
            id = "active_apps",
            title = "Activas",
            icon = Icons.Default.Layers,
            color = Color(0xFF9C27B0),
            onClick = onOpenActiveApps
        ),
        ActionButton(
            id = "controllers",
            title = "Mandos",
            icon = Icons.Default.Gamepad,
            color = Color(0xFF607D8B),
            onClick = onOpenControllers
        ),
        ActionButton(
            id = "settings",
            title = "Ajustes",
            icon = Icons.Default.Settings,
            color = Color(0xFF78909C),
            onClick = onOpenSettings
        ),
        ActionButton(
            id = "switch_launcher",
            title = "Launcher",
            icon = Icons.Default.SwapHoriz,
            color = Color(0xFF673AB7),
            onClick = onOpenLauncherPicker
        ),
        ActionButton(
            id = "power",
            title = "Espera",
            icon = Icons.Default.PowerSettingsNew,
            color = Color(0xFF546E7A),
            onClick = onOpenPower
        ),
        ActionButton(
            id = "all_apps",
            title = "Todas",
            icon = Icons.Default.GridView,
            color = AccentCyan,
            onClick = onOpenAllApps
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = if (isLandscape) 10.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(1.dp)
                .background(if (isDarkTheme) Color(0xFF424242) else Color(0xFFDCDCDC))
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            activeButtons.forEachIndexed { idx, btn ->
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                val itemRequester = focusRequesters.getOrNull(idx) ?: remember { FocusRequester() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .focusRequester(itemRequester)
                        .focusable(interactionSource = interactionSource)
                        .clickable { btn.onClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 48.dp else 42.dp)
                            .clip(CircleShape)
                            .background(btn.color)
                            .border(
                                if (isFocused) 3.5.dp else 2.dp,
                                if (isFocused) Color.Yellow else if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = btn.icon,
                            contentDescription = btn.title,
                            tint = Color.White,
                            modifier = Modifier.size(if (isLandscape) 24.dp else 20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = btn.title,
                        fontSize = if (isLandscape) 10.5.sp else 8.5.sp,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFocused) AccentCyan else if (isDarkTheme) Color.LightGray else Color.DarkGray
                    )
                }
            }
        }
    }
}
