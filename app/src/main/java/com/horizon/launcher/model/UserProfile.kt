package com.horizon.launcher.model

import android.graphics.Bitmap

data class UserProfile(
    val name: String = "Usuario",
    val email: String? = null,
    val photoBitmap: Bitmap? = null
)
