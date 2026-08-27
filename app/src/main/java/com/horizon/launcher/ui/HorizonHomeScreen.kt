package com.horizon.launcher.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.ui.components.AppCard
import com.horizon.launcher.ui.components.BottomActionBar
import com.horizon.launcher.ui.components.TopStatusBar
import com.horizon.launcher.ui.theme.AccentCyan
import com.horizon.launcher.ui.theme.DarkBg
import com.horizon.launcher.ui.theme.LightBg
import kotlinx.coroutines.launch

enum class FilterCategory {
    ALL, GAMES, APPS
}

enum class FocusedSection {
    TOP_BAR, CAROUSEL, BOTTOM_BAR
}

@Composable
fun HorizonHomeScreen(
    appsList: List<AppModel>,
    isLoading: Boolean,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLaunchApp: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(FilterCategory.ALL) }
    var selectedAppIndex by remember { mutableIntStateOf(0) }
    var focusedSection by remember { mutableStateOf(FocusedSection.CAROUSEL) }

    val filteredApps = remember(appsList, selectedCategory) {
        when (selectedCategory) {
            FilterCategory.ALL -> appsList
            FilterCategory.GAMES -> appsList.filter { it.isGame }
            FilterCategory.APPS -> appsList.filter { !it.isGame }
        }
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smooth auto scroll on Gamepad D-Pad Left / Right
    LaunchedEffect(selectedAppIndex, filteredApps.size) {
        if (filteredApps.isNotEmpty() && selectedAppIndex in filteredApps.indices) {
            lazyListState.animateScrollToItem(selectedAppIndex)
        }
    }

    val topBarFocusRequester = remember { FocusRequester() }
    val bottomBarFocusRequesters = remember { List(8) { FocusRequester() } }

    val backgroundColor = if (isDarkTheme) DarkBg else LightBg

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                val nativeKeyCode = keyEvent.nativeKeyEvent.keyCode

                when {
                    // D-Pad Left / Right: Navigate Carousel Items
                    nativeKeyCode == KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (focusedSection == FocusedSection.CAROUSEL && selectedAppIndex > 0) {
                            selectedAppIndex--
                            true
                        } else false
                    }
                    nativeKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (focusedSection == FocusedSection.CAROUSEL && selectedAppIndex < filteredApps.size - 1) {
                            selectedAppIndex++
                            true
                        } else false
                    }
                    // D-Pad Down: Move focus down
                    nativeKeyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                        when (focusedSection) {
                            FocusedSection.TOP_BAR -> {
                                focusedSection = FocusedSection.CAROUSEL
                                true
                            }
                            FocusedSection.CAROUSEL -> {
                                focusedSection = FocusedSection.BOTTOM_BAR
                                try {
                                    bottomBarFocusRequesters.firstOrNull()?.requestFocus()
                                } catch (_: Exception) {}
                                true
                            }
                            else -> false
                        }
                    }
                    // D-Pad Up: Move focus up
                    nativeKeyCode == KeyEvent.KEYCODE_DPAD_UP -> {
                        when (focusedSection) {
                            FocusedSection.BOTTOM_BAR -> {
                                focusedSection = FocusedSection.CAROUSEL
                                true
                            }
                            FocusedSection.CAROUSEL -> {
                                focusedSection = FocusedSection.TOP_BAR
                                try {
                                    topBarFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                                true
                            }
                            else -> false
                        }
                    }
                    // Button A / Enter: Launch selected app
                    nativeKeyCode == KeyEvent.KEYCODE_BUTTON_A ||
                    nativeKeyCode == KeyEvent.KEYCODE_ENTER ||
                    nativeKeyCode == KeyEvent.KEYCODE_DPAD_CENTER -> {
                        if (focusedSection == FocusedSection.CAROUSEL && filteredApps.isNotEmpty()) {
                            val targetApp = filteredApps.getOrNull(selectedAppIndex)
                            if (targetApp != null) {
                                onLaunchApp(targetApp)
                                true
                            } else false
                        } else false
                    }
                    // Button Y / X: Cycle Category Filters
                    nativeKeyCode == KeyEvent.KEYCODE_BUTTON_Y -> {
                        selectedCategory = when (selectedCategory) {
                            FilterCategory.ALL -> FilterCategory.GAMES
                            FilterCategory.GAMES -> FilterCategory.APPS
                            FilterCategory.APPS -> FilterCategory.ALL
                        }
                        selectedAppIndex = 0
                        true
                    }
                    // Button X: Toggle Theme
                    nativeKeyCode == KeyEvent.KEYCODE_BUTTON_X -> {
                        onToggleTheme()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Status Bar
            TopStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                focusRequester = topBarFocusRequester
            )

            // 2. Category Tabs & Carousel
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilterCategory.values().forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val catLabel = when (cat) {
                            FilterCategory.ALL -> "Todas (${appsList.size})"
                            FilterCategory.GAMES -> "Juegos (${appsList.count { it.isGame }})"
                            FilterCategory.APPS -> "Aplicaciones (${appsList.count { !it.isGame }})"
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) AccentCyan else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedCategory = cat
                                    selectedAppIndex = 0
                                    focusedSection = FocusedSection.CAROUSEL
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = catLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else if (isDarkTheme) Color.LightGray else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentCyan)
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay aplicaciones en esta categoría",
                            color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyRow(
                        state = lazyListState,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        itemsIndexed(filteredApps) { index, app ->
                            AppCard(
                                app = app,
                                isSelected = (index == selectedAppIndex && focusedSection == FocusedSection.CAROUSEL),
                                onSelect = {
                                    selectedAppIndex = index
                                    focusedSection = FocusedSection.CAROUSEL
                                },
                                onLaunch = { onLaunchApp(app) },
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }

            // 3. Bottom Action Bar
            BottomActionBar(
                isDarkTheme = isDarkTheme,
                onOpenSettings = { },
                onOpenAllApps = {
                    selectedCategory = FilterCategory.ALL
                    selectedAppIndex = 0
                    focusedSection = FocusedSection.CAROUSEL
                },
                focusRequesters = bottomBarFocusRequesters
            )
        }
    }
}
