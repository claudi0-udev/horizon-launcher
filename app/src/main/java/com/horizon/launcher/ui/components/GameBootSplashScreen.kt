package com.horizon.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.ui.theme.AccentCyan

@Composable
fun GameBootSplashScreen(
    app: AppModel?,
    isDarkTheme: Boolean
) {
    AnimatedVisibility(
        visible = app != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180))
    ) {
        if (app == null) return@AnimatedVisibility

        val scale by animateFloatAsState(
            targetValue = 1.15f,
            animationSpec = tween(400),
            label = "bootScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color.Black.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.scale(scale)
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFF0F0F0))
                        .border(3.5.dp, AccentCyan, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val customBitmap = app.customBitmap
                    val defaultBitmap = app.icon?.toBitmap()?.asImageBitmap()

                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap.asImageBitmap(),
                            contentDescription = app.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (defaultBitmap != null) {
                        Image(
                            bitmap = defaultBitmap,
                            contentDescription = app.label,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Iniciando ${app.label}...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color.Black
                )

                Spacer(modifier = Modifier.height(14.dp))

                CircularProgressIndicator(
                    color = AccentCyan,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}
