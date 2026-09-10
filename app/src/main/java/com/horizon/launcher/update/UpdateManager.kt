package com.horizon.launcher.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val hasUpdate: Boolean
)

class UpdateManager(private val context: Context) {

    private val releasesApiUrl = "https://api.github.com/repos/claudi0-udev/horizon-launcher/releases/latest"

    fun getCurrentVersion(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()
            val url = URL(releasesApiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "HorizonLauncher-Android")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val tagName = json.optString("tag_name", "v1.0")
                val releaseNotes = json.optString("body", "Mejoras de rendimiento y correcciones de errores.")

                // Look for .apk asset
                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                // If no specific asset, fallback to release html url or tag url
                if (downloadUrl.isEmpty()) {
                    downloadUrl = json.optString("html_url", "https://github.com/claudi0-udev/horizon-launcher/releases")
                }

                val remoteVersionClean = tagName.trim().removePrefix("v")
                val currentVersionClean = currentVersion.trim().removePrefix("v")
                val hasUpdate = isNewerVersion(remoteVersionClean, currentVersionClean)

                Result.success(
                    UpdateInfo(
                        currentVersion = currentVersion,
                        latestVersion = tagName,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl,
                        hasUpdate = hasUpdate
                    )
                )
            } else {
                Result.failure(Exception("Error al consultar GitHub: Código ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndInstallApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            // Handle HTTP redirects (GitHub Releases redirect to AWS S3)
            var redirectedConn: HttpURLConnection = connection
            var status = redirectedConn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                val newUrl = redirectedConn.getHeaderField("Location")
                redirectedConn = URL(newUrl).openConnection() as HttpURLConnection
                redirectedConn.connectTimeout = 15000
                redirectedConn.readTimeout = 30000
            }

            val totalBytes = redirectedConn.contentLength.toLong()
            val downloadDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(downloadDir, "horizon-update.apk")

            redirectedConn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalRead.toFloat() / totalBytes.toFloat())
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                installApk(apkFile)
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File) {
        try {
            // Check unknown sources permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        return try {
            val remoteParts = remote.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val maxLen = maxOf(remoteParts.size, currentParts.size)

            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            false
        } catch (_: Exception) {
            remote != current
        }
    }
}
