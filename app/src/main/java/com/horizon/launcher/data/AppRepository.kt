package com.horizon.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import com.horizon.launcher.model.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val appList = mutableListOf<AppModel>()
        val myPackageName = context.packageName

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            if (packageName == myPackageName) continue

            val label = info.loadLabel(pm).toString()
            val icon = info.loadIcon(pm)
            val launchIntent = pm.getLaunchIntentForPackage(packageName)

            val appInfo = info.activityInfo.applicationInfo
            val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo.category == ApplicationInfo.CATEGORY_GAME
            } else {
                @Suppress("DEPRECATION")
                (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            }

            appList.add(
                AppModel(
                    packageName = packageName,
                    label = label,
                    icon = icon,
                    isGame = isGame,
                    launchIntent = launchIntent
                )
            )
        }

        appList.sortBy { it.label.lowercase() }
        appList
    }
}
