package com.horizon.launcher.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import com.horizon.launcher.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class UserProfileRepository(private val context: Context) {

    suspend fun getUserProfile(): UserProfile = withContext(Dispatchers.IO) {
        var accountName = "Usuario Google"
        var accountEmail: String? = null
        var photoBitmap: Bitmap? = null

        // 1. Fetch Google Account via AccountManager
        try {
            val am = AccountManager.get(context)
            val accounts: Array<Account> = am.getAccountsByType("com.google")
            if (accounts.isNotEmpty()) {
                val primaryAccount = accounts[0]
                accountEmail = primaryAccount.name
                if (accountEmail.contains("@")) {
                    val rawName = accountEmail.substringBefore("@")
                    accountName = rawName.split(".", "_", "-")
                        .joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }
                } else {
                    accountName = primaryAccount.name
                }
            }
        } catch (_: Exception) {}

        // 2. Query ContactsContract Profile for Display Name & Photo
        try {
            val cr: ContentResolver = context.contentResolver
            val cursor = cr.query(
                ContactsContract.Profile.CONTENT_URI,
                arrayOf(
                    ContactsContract.Profile.DISPLAY_NAME,
                    ContactsContract.Profile.PHOTO_URI,
                    ContactsContract.Profile.PHOTO_THUMBNAIL_URI
                ),
                null, null, null
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)
                    val photoIdx = c.getColumnIndex(ContactsContract.Profile.PHOTO_URI)
                    val thumbIdx = c.getColumnIndex(ContactsContract.Profile.PHOTO_THUMBNAIL_URI)

                    if (nameIdx != -1) {
                        val nameStr = c.getString(nameIdx)
                        if (!nameStr.isNullOrBlank()) {
                            accountName = nameStr
                        }
                    }

                    val uriStr = if (photoIdx != -1) c.getString(photoIdx) else null
                    val thumbStr = if (thumbIdx != -1) c.getString(thumbIdx) else null
                    val finalUriStr = uriStr ?: thumbStr

                    if (!finalUriStr.isNullOrBlank()) {
                        photoBitmap = loadBitmapFromUri(context, Uri.parse(finalUriStr))
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Search Contacts by Google Email if profile photo was null
        if (photoBitmap == null && !accountEmail.isNullOrBlank()) {
            try {
                val cr = context.contentResolver
                val emailCursor = cr.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Email.PHOTO_URI),
                    "${ContactsContract.CommonDataKinds.Email.ADDRESS} = ?",
                    arrayOf(accountEmail),
                    null
                )
                emailCursor?.use { c ->
                    if (c.moveToFirst()) {
                        val photoIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.PHOTO_URI)
                        if (photoIdx != -1) {
                            val uriStr = c.getString(photoIdx)
                            if (!uriStr.isNullOrBlank()) {
                                photoBitmap = loadBitmapFromUri(context, Uri.parse(uriStr))
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

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
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
            null
        }
    }
}
