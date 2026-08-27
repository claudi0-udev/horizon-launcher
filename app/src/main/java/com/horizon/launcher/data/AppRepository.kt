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

    private val knownEmulatorPackages = setOf(
        "org.ppsspp.ppsspp", "org.ppsspp.ppssppgold",
        "org.retroarch", "org.retroarch.aarch64", "org.retroarch.ra32",
        "xyz.aethersx2.android", "xyz.nethersx2.android",
        "org.dolphinemu.dolphinemu",
        "org.citra.citra_emu", "org.citra.citra_canary", "org.lemonade.lemonade_emu",
        "org.yuzu.yuzu_emu", "org.suyu.suyu_emu", "org.sudachi.sudachi_emu", "org.uzuy.uzuy_emu",
        "com.github.stenzek.duckstation",
        "org.vita3k.emulator",
        "emu.skyline", "emu.ryujinx",
        "com.dsemu.drastic", "org.melonds.melonds",
        "com.ex.SNES9xPlus", "com.ex.MD", "com.ex.GBC", "com.ex.GBA", "com.ex.NEO",
        "com.fastem.gba", "com.fastem.gbc",
        "org.mupen64plusae.v3.fsi",
        "org.scummvm.scummvm",
        "io.retronet.redream",
        "com.winlator", "com.mobox", "com.horizon.emu",
        "com.epsxe.ePSXe", "com.damonplay.damonps2.pro.ppsspp"
    )

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

            val isEmu = knownEmulatorPackages.contains(packageName) ||
                    label.contains("emulator", ignoreCase = true) ||
                    label.contains("emulador", ignoreCase = true) ||
                    label.contains("retroarch", ignoreCase = true) ||
                    label.contains("ppsspp", ignoreCase = true) ||
                    label.contains("dolphin", ignoreCase = true) ||
                    label.contains("citra", ignoreCase = true) ||
                    label.contains("yuzu", ignoreCase = true) ||
                    label.contains("aethersx2", ignoreCase = true)

            val isFav = favoritesRepo.isFavorite(packageName)
            val customBmp = artworkRepo.getCustomArtworkBitmap(packageName)

            appList.add(
                AppModel(
                    packageName = packageName,
                    label = label,
                    icon = icon,
                    isGame = isGame || isEmu,
                    isEmulator = isEmu,
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
