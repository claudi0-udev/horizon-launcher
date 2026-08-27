package com.horizon.launcher.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizon.launcher.admin.LauncherAdminReceiver
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.model.UserProfile
import com.horizon.launcher.ui.components.AllAppsDrawer
import com.horizon.launcher.ui.components.AppCard
import com.horizon.launcher.ui.components.BottomActionBar
import com.horizon.launcher.ui.components.TopStatusBar
import com.horizon.launcher.ui.theme.AccentCyan
import com.horizon.launcher.ui.theme.DarkBg
import com.horizon.launcher.ui.theme.LightBg

enum class FilterCategory {
    ALL, GAMES, APPS
}

enum class FocusedSection {
    TOP_BAR, SEARCH_BAR, CAROUSEL, BOTTOM_BAR
}

@Composable
fun HorizonHomeScreen(
    appsList: List<AppModel>,
    userProfile: UserProfile,
    batteryLevel: Int,
    isLoading: Boolean,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLaunchApp: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var selectedCategory by remember { mutableStateOf(FilterCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppIndex by remember { mutableIntStateOf(0) }
    var focusedSection by remember { mutableStateOf(FocusedSection.CAROUSEL) }
    var isAllAppsDrawerOpen by remember { mutableStateOf(false) }

    val filteredApps = remember(appsList, selectedCategory, searchQuery) {
        appsList.filter { app ->
            val matchesCategory = when (selectedCategory) {
                FilterCategory.ALL -> true
                FilterCategory.GAMES -> app.isGame
                FilterCategory.APPS -> !app.isGame
            }
            val matchesSearch = searchQuery.isBlank() || app.label.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    LaunchedEffect(selectedAppIndex, filteredApps.size) {
        if (filteredApps.isNotEmpty() && selectedAppIndex in filteredApps.indices) {
            if (isLandscape) {
                lazyListState.animateScrollToItem(selectedAppIndex)
            } else {
                lazyGridState.animateScrollToItem(selectedAppIndex)
            }
        }
    }

    val topBarFocusRequester = remember { FocusRequester() }
    val searchBarFocusRequester = remember { FocusRequester() }
    val bottomBarFocusRequesters = remember { List(6) { FocusRequester() } }

    val backgroundColor = if (isDarkTheme) DarkBg else LightBg

    fun launchBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_BROWSER)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No se encontró un navegador de internet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchGallery() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_GALLERY)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No se encontró una aplicación de galería", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchControllersSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No se pudo abrir la configuración de mandos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir la configuración del sistema", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchPowerStandby() {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val compName = ComponentName(context, LauncherAdminReceiver::class.java)
            if (dpm.isAdminActive(compName)) {
                dpm.lockNow()
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permite apagar la pantalla al presionar Modo de Espera.")
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                val nativeKeyCode = keyEvent.nativeKeyEvent.keyCode

                when {
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
                    nativeKeyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                        when (focusedSection) {
                            FocusedSection.TOP_BAR -> {
                                focusedSection = FocusedSection.SEARCH_BAR
                                try { searchBarFocusRequester.requestFocus() } catch (_: Exception) {}
                                true
                            }
                            FocusedSection.SEARCH_BAR -> {
                                focusedSection = FocusedSection.CAROUSEL
                                true
                            }
                            FocusedSection.CAROUSEL -> {
                                if (!isLandscape && selectedAppIndex + 3 < filteredApps.size) {
                                    selectedAppIndex += 3
                                } else {
                                    focusedSection = FocusedSection.BOTTOM_BAR
                                    try { bottomBarFocusRequesters.firstOrNull()?.requestFocus() } catch (_: Exception) {}
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    nativeKeyCode == KeyEvent.KEYCODE_DPAD_UP -> {
                        when (focusedSection) {
                            FocusedSection.BOTTOM_BAR -> {
                                focusedSection = FocusedSection.CAROUSEL
                                true
                            }
                            FocusedSection.CAROUSEL -> {
                                if (!isLandscape && selectedAppIndex - 3 >= 0) {
                                    selectedAppIndex -= 3
                                } else {
                                    focusedSection = FocusedSection.SEARCH_BAR
                                    try { searchBarFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                true
                            }
                            FocusedSection.SEARCH_BAR -> {
                                focusedSection = FocusedSection.TOP_BAR
                                try { topBarFocusRequester.requestFocus() } catch (_: Exception) {}
                                true
                            }
                            else -> false
                        }
                    }
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
                    nativeKeyCode == KeyEvent.KEYCODE_BUTTON_Y -> {
                        selectedCategory = when (selectedCategory) {
                            FilterCategory.ALL -> FilterCategory.GAMES
                            FilterCategory.GAMES -> FilterCategory.APPS
                            FilterCategory.APPS -> FilterCategory.ALL
                        }
                        selectedAppIndex = 0
                        true
                    }
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
                userProfile = userProfile,
                batteryLevel = batteryLevel,
                isDarkTheme = isDarkTheme,
                isLandscape = isLandscape,
                onToggleTheme = onToggleTheme,
                focusRequester = topBarFocusRequester
            )

            // 2. Search & Apps Content Section
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBarField(
                            searchQuery = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                selectedAppIndex = 0
                            },
                            isDarkTheme = isDarkTheme,
                            isFocused = focusedSection == FocusedSection.SEARCH_BAR,
                            focusRequester = searchBarFocusRequester,
                            modifier = Modifier.width(280.dp)
                        )

                        CategoryTabs(
                            selectedCategory = selectedCategory,
                            appsList = appsList,
                            isDarkTheme = isDarkTheme,
                            onSelectCategory = { cat ->
                                selectedCategory = cat
                                selectedAppIndex = 0
                                focusedSection = FocusedSection.CAROUSEL
                            }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SearchBarField(
                            searchQuery = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                selectedAppIndex = 0
                            },
                            isDarkTheme = isDarkTheme,
                            isFocused = focusedSection == FocusedSection.SEARCH_BAR,
                            focusRequester = searchBarFocusRequester,
                            modifier = Modifier.fillMaxWidth(0.96f)
                        )

                        CategoryTabs(
                            selectedCategory = selectedCategory,
                            appsList = appsList,
                            isDarkTheme = isDarkTheme,
                            onSelectCategory = { cat ->
                                selectedCategory = cat
                                selectedAppIndex = 0
                                focusedSection = FocusedSection.CAROUSEL
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // App Grid / Carousel Content
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
                            text = if (searchQuery.isNotEmpty()) "No se encontraron apps con \"$searchQuery\"" else "No hay aplicaciones en esta categoría",
                            color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    if (isLandscape) {
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
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = lazyGridState,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
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
            }

            // 3. Bottom Action Bar
            BottomActionBar(
                isDarkTheme = isDarkTheme,
                isLandscape = isLandscape,
                onOpenBrowser = { launchBrowser() },
                onOpenGallery = { launchGallery() },
                onOpenControllers = { launchControllersSettings() },
                onOpenSettings = { launchSystemSettings() },
                onOpenPower = { launchPowerStandby() },
                onOpenAllApps = {
                    isAllAppsDrawerOpen = true
                },
                focusRequesters = bottomBarFocusRequesters
            )
        }

        // Fullscreen All Apps Drawer Modal
        AllAppsDrawer(
            isOpen = isAllAppsDrawerOpen,
            appsList = appsList,
            isDarkTheme = isDarkTheme,
            onDismiss = { isAllAppsDrawerOpen = false },
            onLaunchApp = { app ->
                onLaunchApp(app)
                isAllAppsDrawerOpen = false
            }
        )
    }
}

@Composable
fun SearchBarField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    isDarkTheme: Boolean,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val searchInteractionSource = remember { MutableInteractionSource() }
    val isSearchFocused by searchInteractionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkTheme) Color(0xFF3B3B3B) else Color.White)
            .border(
                width = if (isSearchFocused || isFocused) 2.5.dp else 1.dp,
                color = if (isSearchFocused || isFocused) AccentCyan else if (isDarkTheme) Color(0xFF555555) else Color(0xFFD0D0D0),
                shape = RoundedCornerShape(20.dp)
            )
            .focusRequester(focusRequester)
            .focusable(interactionSource = searchInteractionSource)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = if (isDarkTheme) Color.LightGray else Color.Gray,
                modifier = Modifier.size(18.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Buscar app o juego...",
                        color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Search",
                    tint = if (isDarkTheme) Color.LightGray else Color.Gray,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

@Composable
fun CategoryTabs(
    selectedCategory: FilterCategory,
    appsList: List<AppModel>,
    isDarkTheme: Boolean,
    onSelectCategory: (FilterCategory) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterCategory.values().forEach { cat ->
            val isSelected = selectedCategory == cat
            val catLabel = when (cat) {
                FilterCategory.ALL -> "Más Usadas (${appsList.size})"
                FilterCategory.GAMES -> "Juegos (${appsList.count { it.isGame }})"
                FilterCategory.APPS -> "Apps (${appsList.count { !it.isGame }})"
            }

            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) AccentCyan else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectCategory(cat) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = catLabel,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else if (isDarkTheme) Color.LightGray else Color.DarkGray
                )
            }
        }
    }
}
