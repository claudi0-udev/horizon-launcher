package com.horizon.launcher.model

import android.content.Intent
import android.graphics.drawable.Drawable

data class AppModel(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isGame: Boolean = false,
    val launchIntent: Intent? = null
)
