package com.horizon.launcher.data

import android.accounts.AccountManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import com.horizon.launcher.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserProfileRepository(private val context: Context) {

    suspend fun getUserProfile(): UserProfile = withContext(Dispatchers.IO) {
        var accountName = "Usuario Google"
        var accountEmail: String? = null
        var photoBitmap: Bitmap? = null

        // 1. Fetch Google Account from AccountManager
        try {
            val am = AccountManager.get(context)
            val googleAccounts = am.getAccountsByType("com.google")
            if (googleAccounts.isNotEmpty()) {
                accountEmail = googleAccounts[0].name
                accountName = if (accountEmail.contains("@")) {
                    val rawName = accountEmail.substringBefore("@")
                    rawName.replace(".", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                } else {
                    accountEmail
                }
            }
        } catch (_: Exception) {}

        // 2. Fetch Display Name & Photo from ContactsContract Profile
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Profile.CONTENT_URI,
                arrayOf(
                    ContactsContract.Profile.DISPLAY_NAME,
                    ContactsContract.Profile.PHOTO_URI,
                    ContactsContract.Profile.PHOTO_THUMBNAIL_URI
                ),
                null, null, null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIdx = it.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)
                    val photoUriIdx = it.getColumnIndex(ContactsContract.Profile.PHOTO_URI)
                    val thumbUriIdx = it.getColumnIndex(ContactsContract.Profile.PHOTO_THUMBNAIL_URI)

                    if (displayNameIdx != -1) {
                        val nameStr = it.getString(displayNameIdx)
                        if (!nameStr.isNullOrBlank()) {
                            accountName = nameStr
                        }
                    }

                    val uriStr = if (photoUriIdx != -1) it.getString(photoUriIdx) else null
                    val thumbStr = if (thumbUriIdx != -1) it.getString(thumbUriIdx) else null
                    val finalUriStr = uriStr ?: thumbStr

                    if (!finalUriStr.isNullOrBlank()) {
                        val imageUri = Uri.parse(finalUriStr)
                        photoBitmap = loadBitmapFromUri(context, imageUri)
                    }
                }
            }
        } catch (_: Exception) {}

        UserProfile(
            name = accountName,
            email = accountEmail,
            photoBitmap = photoBitmap
        )
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }
}
