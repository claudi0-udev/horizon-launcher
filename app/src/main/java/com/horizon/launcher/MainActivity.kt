package com.horizon.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.horizon.launcher.data.AppRepository
import com.horizon.launcher.data.BatteryRepository
import com.horizon.launcher.data.FavoritesRepository
import com.horizon.launcher.data.UserProfileRepository
import com.horizon.launcher.gamepad.BluetoothControllerManager
import com.horizon.launcher.gamepad.GamepadMappingRepository
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.model.UserProfile
import com.horizon.launcher.sound.SoundEffectManager
import com.horizon.launcher.ui.HorizonHomeScreen
import com.horizon.launcher.ui.theme.HorizonLauncherTheme
import com.horizon.launcher.update.UpdateManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var batteryRepository: BatteryRepository
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var soundEffectManager: SoundEffectManager
    private lateinit var gamepadMappingRepository: GamepadMappingRepository
    private lateinit var bluetoothControllerManager: BluetoothControllerManager
    private lateinit var updateManager: UpdateManager

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        loadProfile()
    }

    private var profileState = mutableStateOf(UserProfile())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appRepository = AppRepository(this)
        userProfileRepository = UserProfileRepository(this)
        batteryRepository = BatteryRepository(this)
        favoritesRepository = FavoritesRepository(this)
        soundEffectManager = SoundEffectManager(this)
        gamepadMappingRepository = GamepadMappingRepository(this)
        bluetoothControllerManager = BluetoothControllerManager(this)
        updateManager = UpdateManager(this)

        checkAndRequestPermissions()

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var appsList by remember { mutableStateOf<List<AppModel>>(emptyList()) }
            var batteryLevel by remember { mutableIntStateOf(100) }
            var isLoading by remember { mutableStateOf(true) }

            val scope = rememberCoroutineScope()

            fun reloadApps() {
                scope.launch {
                    appsList = appRepository.getInstalledApps()
                }
            }

            LaunchedEffect(Unit) {
                scope.launch {
                    isLoading = true
                    appsList = appRepository.getInstalledApps()
                    loadProfile()
                    isLoading = false
                }

                scope.launch {
                    batteryRepository.getBatteryLevelFlow().collect { level ->
                        batteryLevel = level
                    }
                }
            }

            HorizonLauncherTheme(darkTheme = isDarkTheme) {
                HorizonHomeScreen(
                    appsList = appsList,
                    userProfile = profileState.value,
                    batteryLevel = batteryLevel,
                    isLoading = isLoading,
                    isDarkTheme = isDarkTheme,
                    soundManager = soundEffectManager,
                    favoritesRepo = favoritesRepository,
                    gamepadMappingRepo = gamepadMappingRepository,
                    bluetoothManager = bluetoothControllerManager,
                    updateManager = updateManager,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onToggleFavoriteApp = { app ->
                        val isFav = favoritesRepository.toggleFavorite(app.packageName)
                        val msg = if (isFav) "Fijado en favoritos ★" else "Desfijado de favoritos"
                        Toast.makeText(this@MainActivity, "${app.label}: $msg", Toast.LENGTH_SHORT).show()
                        reloadApps()
                    },
                    onLaunchApp = { app ->
                        if (app.launchIntent != null) {
                            try {
                                appRepository.recordAppLaunch(app.packageName)
                                reloadApps()
                                startActivity(app.launchIntent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "No se pudo iniciar ${app.label}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Aplicación sin actividad principal",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundEffectManager.release()
        bluetoothControllerManager.unregisterReceiver()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.GET_ACCOUNTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }

        // Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun loadProfile() {
        @Suppress("OPT_IN_USAGE")
        kotlinx.coroutines.GlobalScope.launch {
            val prof = userProfileRepository.getUserProfile()
            profileState.value = prof
        }
    }
}
