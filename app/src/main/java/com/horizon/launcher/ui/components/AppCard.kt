package com.horizon.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.ui.theme.AccentCyan
import com.horizon.launcher.ui.theme.AccentRed

@Composable
fun AppCard(
    app: AppModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLaunch: () -> Unit,
    isDarkTheme: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() },
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val activeFocus = isSelected || isFocused

    val scale by animateFloatAsState(
        targetValue = if (activeFocus) 1.14f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
    )

    val borderColor = if (activeFocus) AccentCyan else if (isDarkTheme) Color(0xFF4A4A4A) else Color(0xFFD0D0D0)
    val borderWidth = if (activeFocus) 4.dp else 1.5.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .scale(scale)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onSelect()
                }
            }
            .focusable(interactionSource = interactionSource)
            .clickable {
                if (activeFocus) {
                    onLaunch()
                } else {
                    onSelect()
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(
                    elevation = if (activeFocus) 12.dp else 4.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkTheme) Color(0xFF383838) else Color.White)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = remember(app.icon) {
                app.icon?.toBitmap()?.asImageBitmap()
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            } else {
                Text(
                    text = app.label.take(1).uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
            }

            if (app.isGame) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(AccentRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GAME",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = app.label,
            fontSize = 13.sp,
            fontWeight = if (activeFocus) FontWeight.Bold else FontWeight.Normal,
            color = if (activeFocus) AccentCyan else if (isDarkTheme) Color.White else Color(0xFF2D2D2D),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(130.dp)
        )
    }
}
