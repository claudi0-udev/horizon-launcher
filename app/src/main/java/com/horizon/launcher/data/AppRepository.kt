package com.horizon.launcher.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import com.horizon.launcher.model.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class AppRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_launch_stats", Context.MODE_PRIVATE)
    private val favoritesRepo = FavoritesRepository(context)
    private val artworkRepo = CustomArtworkRepository(context)

    fun recordAppLaunch(packageName: String) {
        val currentCount = prefs.getInt(packageName, 0)
        prefs.edit().putInt(packageName, currentCount + 1).apply()
    }

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

        val usageMap = mutableMapOf<String, Long>()
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager != null) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -14)
                val stats: List<UsageStats>? = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    calendar.timeInMillis,
                    System.currentTimeMillis()
                )
                stats?.forEach { stat ->
                    val existingTime = usageMap[stat.packageName] ?: 0L
                    usageMap[stat.packageName] = Math.max(existingTime, stat.totalTimeInForeground)
                }
            }
        } catch (_: Exception) {}

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

            val isFav = favoritesRepo.isFavorite(packageName)
            val customBmp = artworkRepo.getCustomArtworkBitmap(packageName)

            appList.add(
                AppModel(
                    packageName = packageName,
                    label = label,
                    icon = icon,
                    isGame = isGame,
                    isFavorite = isFav,
                    customBitmap = customBmp,
                    launchIntent = launchIntent
                )
            )
        }

        // Pinned Favorites ALWAYS appear first, then sorted by Most Used score
        appList.sortWith(Comparator { app1, app2 ->
            if (app1.isFavorite != app2.isFavorite) {
                if (app1.isFavorite) -1 else 1
            } else {
                val localClicks1 = prefs.getInt(app1.packageName, 0)
                val localClicks2 = prefs.getInt(app2.packageName, 0)

                val timeUsed1 = usageMap[app1.packageName] ?: 0L
                val timeUsed2 = usageMap[app2.packageName] ?: 0L

                val score1 = localClicks1 * 600000L + timeUsed1
                val score2 = localClicks2 * 600000L + timeUsed2

                if (score1 != score2) {
                    score2.compareTo(score1)
                } else {
                    app1.label.lowercase().compareTo(app2.label.lowercase())
                }
            }
        })

        appList
    }
}
