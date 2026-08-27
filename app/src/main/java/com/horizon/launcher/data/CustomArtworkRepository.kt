package com.horizon.launcher.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.InputStream

class CustomArtworkRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_custom_artwork", Context.MODE_PRIVATE)

    fun setCustomArtworkUri(packageName: String, uri: Uri) {
        prefs.edit().putString(packageName, uri.toString()).apply()
    }

    fun removeCustomArtwork(packageName: String) {
        prefs.edit().remove(packageName).apply()
    }

    fun getCustomArtworkBitmap(packageName: String): Bitmap? {
        val uriStr = prefs.getString(packageName, null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
            null
        }
    }
}
