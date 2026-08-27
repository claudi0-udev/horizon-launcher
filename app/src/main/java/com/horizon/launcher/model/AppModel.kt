package com.horizon.launcher.model

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class AppModel(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isGame: Boolean = false,
    val isFavorite: Boolean = false,
    val customBitmap: Bitmap? = null,
    val launchIntent: Intent? = null
)
