package com.horizon.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizon.launcher.model.UserProfile
import com.horizon.launcher.ui.theme.AccentCyan
import com.horizon.launcher.ui.theme.AccentRed
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun TopStatusBar(
    userProfile: UserProfile,
    batteryLevel: Int,
    isDarkTheme: Boolean,
    isLandscape: Boolean,
    onToggleTheme: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000L)
        }
    }

    val textColor = if (isDarkTheme) Color.White else Color(0xFF2D2D2D)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isLandscape) 24.dp else 14.dp,
                vertical = if (isLandscape) 12.dp else 8.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Profile Avatar & Account Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 8.dp),
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
                .clickable { onToggleTheme() }
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isLandscape) 46.dp else 40.dp)
                    .clip(CircleShape)
                    .background(AccentRed)
                    .border(
                        if (isFocused) 3.5.dp else 2.dp,
                        if (isFocused) Color.Yellow else AccentCyan,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile.photoBitmap != null) {
                    Image(
                        bitmap = userProfile.photoBitmap.asImageBitmap(),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    val initial = userProfile.name.take(1).uppercase()
                    if (initial.isNotEmpty() && initial != "U") {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = if (isLandscape) 22.sp else 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Profile",
                            tint = Color.White,
                            modifier = Modifier.size(if (isLandscape) 26.dp else 22.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = userProfile.name,
                    fontSize = if (isLandscape) 15.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (isLandscape && !userProfile.email.isNullOrBlank()) {
                    Text(
                        text = userProfile.email,
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color.LightGray else Color.Gray
                    )
                }
            }
        }

        // Right Status indicators: Wi-Fi, Real-time Battery, Adapting Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 16.dp else 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Wi-Fi",
                tint = textColor,
                modifier = Modifier.size(if (isLandscape) 20.dp else 16.dp)
            )

            // Real Battery Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(if (isLandscape) 22.dp else 18.dp)
                        .height(if (isLandscape) 12.dp else 10.dp)
                        .border(1.2.dp, textColor)
                        .padding(1.2.dp)
                ) {
                    val fillRatio = (batteryLevel.coerceIn(0, 100) / 100f)
                    val batteryColor = when {
                        batteryLevel <= 15 -> Color.Red
                        batteryLevel <= 30 -> Color(0xFFFF9800)
                        else -> textColor
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillRatio)
                            .background(batteryColor)
                    )
                }
                Text(
                    text = "$batteryLevel%",
                    fontSize = if (isLandscape) 13.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }

            // Adapted Clock Display for Portrait & Landscape
            if (isLandscape) {
                Text(
                    text = currentTime,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            } else {
                // Stacked compact vertical clock: HH on top, mm on bottom without colon
                val hours = if (currentTime.contains(":")) currentTime.substringBefore(":") else "00"
                val minutes = if (currentTime.contains(":")) currentTime.substringAfter(":") else "00"
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = hours,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 12.sp,
                        color = textColor
                    )
                    Text(
                        text = minutes,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 12.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}
