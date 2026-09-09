package com.horizon.launcher.ui.components

import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizon.launcher.gamepad.*
import com.horizon.launcher.sound.SoundEffectManager
import com.horizon.launcher.ui.theme.AccentCyan
import com.horizon.launcher.ui.theme.AccentRed
import com.horizon.launcher.ui.theme.DarkBg
import com.horizon.launcher.ui.theme.LightBg

enum class ControllerHubTab {
    MAPPING, TESTER, BLUETOOTH
}

@Composable
fun ControllersHubDialog(
    isOpen: Boolean,
    mappingRepo: GamepadMappingRepository,
    bluetoothManager: BluetoothControllerManager,
    soundManager: SoundEffectManager,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(ControllerHubTab.MAPPING) }

    // Connected Gamepads
    var connectedGamepads by remember { mutableStateOf(mappingRepo.getConnectedGamepads()) }
    var selectedGamepadDescriptor by remember {
        mutableStateOf(connectedGamepads.firstOrNull()?.descriptor ?: "default")
    }

    // Remap state: if non-null, listening for next button press
    var actionBeingRemapped by remember { mutableStateOf<GamepadAction?>(null) }

    // Input tester last pressed key
    var lastPressedKeyCode by remember { mutableIntStateOf(0) }
    var lastPressedKeyName by remember { mutableStateOf("Ninguno") }

    // Bluetooth states
    val isScanning by bluetoothManager.isScanning.collectAsState()
    val discoveredDevices by bluetoothManager.discoveredDevices.collectAsState()
    val pairedDevices by bluetoothManager.pairedDevices.collectAsState()

    LaunchedEffect(isOpen) {
        connectedGamepads = mappingRepo.getConnectedGamepads()
        if (connectedGamepads.isNotEmpty() && selectedGamepadDescriptor == "default") {
            selectedGamepadDescriptor = connectedGamepads.first().descriptor
        }
        bluetoothManager.refreshPairedDevices()
    }

    DisposableEffect(isOpen) {
        onDispose {
            bluetoothManager.stopScan()
            bluetoothManager.unregisterReceiver()
        }
    }

    val backgroundColor = if (isDarkTheme) DarkBg else LightBg
    val cardBg = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFF2F2F2)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF2D2D2D)
    val subtextColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.copy(alpha = 0.98f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val nativeKeyCode = keyEvent.nativeKeyEvent.keyCode

                    // If currently remapping an action, intercept the pressed key!
                    val remapTarget = actionBeingRemapped
                    if (remapTarget != null) {
                        mappingRepo.setMapping(selectedGamepadDescriptor, remapTarget, nativeKeyCode)
                        soundManager.playSelectSound()
                        Toast.makeText(
                            context,
                            "${remapTarget.displayName} asignado a: ${GamepadAction.getKeyName(nativeKeyCode)}",
                            Toast.LENGTH_SHORT
                        ).show()
                        actionBeingRemapped = null
                        return@onPreviewKeyEvent true
                    }

                    // Update tester
                    lastPressedKeyCode = nativeKeyCode
                    lastPressedKeyName = GamepadAction.getKeyName(nativeKeyCode)

                    if (nativeKeyCode == KeyEvent.KEYCODE_BACK || nativeKeyCode == KeyEvent.KEYCODE_BUTTON_B) {
                        onDismiss()
                        true
                    } else false
                }
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AccentCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = "Mandos",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Centro de Mandos y Controles",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = if (connectedGamepads.isEmpty()) "Sin mandos físicos detectados (usando perfil por defecto)"
                                else "${connectedGamepads.size} mando(s) conectado(s): ${connectedGamepads.joinToString { it.name }}",
                                fontSize = 12.sp,
                                color = subtextColor,
                                maxLines = 1
                            )
                        }
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

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        Triple(ControllerHubTab.MAPPING, "Mapeo de Botones", Icons.Default.Tune),
                        Triple(ControllerHubTab.TESTER, "Probar Mando", Icons.Default.CheckCircleOutline),
                        Triple(ControllerHubTab.BLUETOOTH, "Vincular Bluetooth", Icons.Default.Bluetooth)
                    )

                    tabs.forEach { (tab, label, icon) ->
                        val isSelected = currentTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isSelected) AccentCyan else cardBg)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AccentCyan else if (isDarkTheme) Color(0xFF454545) else Color(0xFFD0D0D0),
                                    shape = RoundedCornerShape(19.dp)
                                )
                                .clickable {
                                    soundManager.playSelectSound()
                                    currentTab = tab
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content View according to Tab
                when (currentTab) {
                    ControllerHubTab.MAPPING -> {
                        MappingTabContent(
                            connectedGamepads = connectedGamepads,
                            selectedDescriptor = selectedGamepadDescriptor,
                            actionBeingRemapped = actionBeingRemapped,
                            mappingRepo = mappingRepo,
                            soundManager = soundManager,
                            isDarkTheme = isDarkTheme,
                            cardBg = cardBg,
                            textColor = textColor,
                            subtextColor = subtextColor,
                            onSelectDescriptor = { selectedGamepadDescriptor = it },
                            onStartRemap = { actionBeingRemapped = it },
                            onCancelRemap = { actionBeingRemapped = null },
                            onApplyPreset = { preset ->
                                mappingRepo.applyPreset(selectedGamepadDescriptor, preset)
                                soundManager.playSelectSound()
                                Toast.makeText(context, "Preset $preset aplicado", Toast.LENGTH_SHORT).show()
                            },
                            onReset = {
                                mappingRepo.resetMappings(selectedGamepadDescriptor)
                                soundManager.playSelectSound()
                                Toast.makeText(context, "Mapeo restablecido", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    ControllerHubTab.TESTER -> {
                        TesterTabContent(
                            lastKeyCode = lastPressedKeyCode,
                            lastKeyName = lastPressedKeyName,
                            cardBg = cardBg,
                            textColor = textColor,
                            subtextColor = subtextColor,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    ControllerHubTab.BLUETOOTH -> {
                        BluetoothTabContent(
                            bluetoothManager = bluetoothManager,
                            isScanning = isScanning,
                            discoveredDevices = discoveredDevices,
                            pairedDevices = pairedDevices,
                            soundManager = soundManager,
                            cardBg = cardBg,
                            textColor = textColor,
                            subtextColor = subtextColor,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MappingTabContent(
    connectedGamepads: List<GamepadDeviceInfo>,
    selectedDescriptor: String,
    actionBeingRemapped: GamepadAction?,
    mappingRepo: GamepadMappingRepository,
    soundManager: SoundEffectManager,
    isDarkTheme: Boolean,
    cardBg: Color,
    textColor: Color,
    subtextColor: Color,
    onSelectDescriptor: (String) -> Unit,
    onStartRemap: (GamepadAction) -> Unit,
    onCancelRemap: () -> Unit,
    onApplyPreset: (String) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Controller Selector
        if (connectedGamepads.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mando:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                connectedGamepads.forEach { gp ->
                    val isSelected = gp.descriptor == selectedDescriptor
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentCyan else cardBg)
                            .clickable {
                                soundManager.playSelectSound()
                                onSelectDescriptor(gp.descriptor)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = gp.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else textColor
                        )
                    }
                }
            }
        }

        // Presets bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Presets rápidos:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            listOf("Switch", "Xbox", "PlayStation").forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBg)
                        .border(1.dp, if (isDarkTheme) Color(0xFF4A4A4A) else Color(0xFFD0D0D0), RoundedCornerShape(8.dp))
                        .clickable { onApplyPreset(preset) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = preset,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCyan
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(cardBg)
                    .clickable { onReset() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Restablecer",
                    fontSize = 11.sp,
                    color = AccentRed
                )
            }
        }

        // Remapping Overlay Notice
        if (actionBeingRemapped != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentCyan.copy(alpha = 0.25f))
                    .border(2.dp, AccentCyan, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Presiona un botón en tu mando para asignar:",
                            fontSize = 13.sp,
                            color = textColor
                        )
                        Text(
                            text = actionBeingRemapped.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentCyan
                        )
                    }
                    IconButton(onClick = onCancelRemap) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar",
                            tint = textColor
                        )
                    }
                }
            }
        }

        // Actions mapping list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GamepadAction.values()) { action ->
                val mappedKeyCode = mappingRepo.getMapping(selectedDescriptor, action)
                val keyName = GamepadAction.getKeyName(mappedKeyCode)
                val isBeingEdited = actionBeingRemapped == action

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBeingEdited) AccentCyan.copy(alpha = 0.2f) else cardBg)
                        .border(
                            width = if (isBeingEdited) 2.dp else 1.dp,
                            color = if (isBeingEdited) AccentCyan else if (isDarkTheme) Color(0xFF3D3D3D) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onStartRemap(action) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = action.description,
                            fontSize = 11.sp,
                            color = subtextColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBeingEdited) AccentCyan else if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFE0E0E0))
                            .border(1.dp, if (isBeingEdited) Color.White else AccentCyan, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBeingEdited) "Presiona botón..." else keyName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBeingEdited) Color.White else AccentCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TesterTabContent(
    lastKeyCode: Int,
    lastKeyName: String,
    cardBg: Color,
    textColor: Color,
    subtextColor: Color,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(AccentCyan.copy(alpha = 0.15f))
                .border(3.dp, AccentCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = "Gamepad",
                tint = AccentCyan,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Probador de Entrada en Tiempo Real",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Presiona cualquier botón en tu control físico para verificar su respuesta.",
            fontSize = 13.sp,
            color = subtextColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(2.dp, if (lastKeyCode != 0) AccentCyan else Color.Transparent, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ÚLTIMO BOTÓN PRESIONADO:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = subtextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastKeyName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentCyan
                )
                if (lastKeyCode != 0) {
                    Text(
                        text = "KeyCode: $lastKeyCode",
                        fontSize = 12.sp,
                        color = subtextColor
                    )
                }
            }
        }
    }
}

@Composable
fun BluetoothTabContent(
    bluetoothManager: BluetoothControllerManager,
    isScanning: Boolean,
    discoveredDevices: List<DiscoveredBluetoothDevice>,
    pairedDevices: List<DiscoveredBluetoothDevice>,
    soundManager: SoundEffectManager,
    cardBg: Color,
    textColor: Color,
    subtextColor: Color,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status & Scan Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth",
                    tint = AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Buscando mandos Bluetooth..." else "Escanear mandos cercanos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isScanning) AccentRed else AccentCyan)
                    .clickable {
                        soundManager.playSelectSound()
                        if (isScanning) {
                            bluetoothManager.stopScan()
                        } else {
                            bluetoothManager.startScan()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isScanning) "Detener" else "Escanear",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (isScanning) {
            LinearProgressIndicator(
                color = AccentCyan,
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Paired devices header
            item {
                Text(
                    text = "MANDOS VINCULADOS (${pairedDevices.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            if (pairedDevices.isEmpty()) {
                item {
                    Text(
                        text = "No hay mandos vinculados previamente.",
                        fontSize = 12.sp,
                        color = subtextColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                items(pairedDevices) { dev ->
                    DeviceRowItem(
                        device = dev,
                        isPaired = true,
                        cardBg = cardBg,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        isDarkTheme = isDarkTheme,
                        onPair = {}
                    )
                }
            }

            // Discovered devices header
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "DISPOSITIVOS DISPONIBLES (${discoveredDevices.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    Text(
                        text = if (isScanning) "Detectando mandos cercanos en modo emparejamiento..." else "Presiona 'Escanear' para buscar mandos en modo vinculación.",
                        fontSize = 12.sp,
                        color = subtextColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                items(discoveredDevices) { dev ->
                    DeviceRowItem(
                        device = dev,
                        isPaired = dev.isBonded,
                        cardBg = cardBg,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        isDarkTheme = isDarkTheme,
                        onPair = {
                            val ok = bluetoothManager.pairDevice(dev.device)
                            if (ok) {
                                Toast.makeText(context, "Vinculando ${dev.name}...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Iniciando emparejamiento con ${dev.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceRowItem(
    device: DiscoveredBluetoothDevice,
    isPaired: Boolean,
    cardBg: Color,
    textColor: Color,
    subtextColor: Color,
    isDarkTheme: Boolean,
    onPair: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(1.dp, if (isDarkTheme) Color(0xFF3E3E3E) else Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = "Dispositivo",
                tint = if (isPaired) AccentCyan else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = device.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = device.address,
                    fontSize = 11.sp,
                    color = subtextColor
                )
            }
        }

        if (isPaired) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Vinculado",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentCyan)
                    .clickable { onPair() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Vincular",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
