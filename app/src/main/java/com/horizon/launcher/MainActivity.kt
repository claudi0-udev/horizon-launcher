package com.horizon.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.horizon.launcher.data.AppRepository
import com.horizon.launcher.data.BatteryRepository
import com.horizon.launcher.data.UserProfileRepository
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.model.UserProfile
import com.horizon.launcher.ui.HorizonHomeScreen
import com.horizon.launcher.ui.theme.HorizonLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var batteryRepository: BatteryRepository

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Reload user profile once permissions are granted or denied
        loadProfile()
    }

    private var profileState = mutableStateOf(UserProfile())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appRepository = AppRepository(this)
        userProfileRepository = UserProfileRepository(this)
        batteryRepository = BatteryRepository(this)

        checkAndRequestPermissions()

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var appsList by remember { mutableStateOf<List<AppModel>>(emptyList()) }
            var batteryLevel by remember { mutableIntStateOf(100) }
            var isLoading by remember { mutableStateOf(true) }

            val scope = rememberCoroutineScope()

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
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onLaunchApp = { app ->
                        if (app.launchIntent != null) {
                            try {
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

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.GET_ACCOUNTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun loadProfile() {
        kotlinx.coroutines.GlobalScope.launch {
            val prof = userProfileRepository.getUserProfile()
            profileState.value = prof
        }
    }
}
