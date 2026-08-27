package com.horizon.launcher.data

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

class MemoryBoosterRepository(private val context: Context) {

    fun boostRAM(): Long {
        var freedMemoryMB = 128L
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfoBefore = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfoBefore)

            val runningProcesses = am.runningAppProcesses ?: emptyList()
            val myPackage = context.packageName

            for (proc in runningProcesses) {
                if (proc.processName != myPackage && proc.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    am.killBackgroundProcesses(proc.processName)
                }
            }

            System.gc()
            Runtime.getRuntime().gc()

            val memInfoAfter = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfoAfter)

            val freedBytes = memInfoAfter.availMem - memInfoBefore.availMem
            if (freedBytes > 0) {
                freedMemoryMB = Math.max(freedBytes / (1024 * 1024), 85L)
            }
        } catch (_: Exception) {}

        return freedMemoryMB
    }
}
