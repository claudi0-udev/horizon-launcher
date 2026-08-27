package com.horizon.launcher

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.horizon.launcher.data.AppRepository
import com.horizon.launcher.model.AppModel
import com.horizon.launcher.ui.HorizonHomeScreen
import com.horizon.launcher.ui.theme.HorizonLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appRepository = AppRepository(this)

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var appsList by remember { mutableStateOf<List<AppModel>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }

            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                scope.launch {
                    isLoading = true
                    appsList = appRepository.getInstalledApps()
                    isLoading = false
                }
            }

            HorizonLauncherTheme(darkTheme = isDarkTheme) {
                HorizonHomeScreen(
                    appsList = appsList,
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
}
