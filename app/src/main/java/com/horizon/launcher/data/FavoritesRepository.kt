package com.horizon.launcher.data

import android.content.Context
import android.content.SharedPreferences

class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_favorites", Context.MODE_PRIVATE)

    fun isFavorite(packageName: String): Boolean {
        return prefs.getBoolean(packageName, false)
    }

    fun toggleFavorite(packageName: String): Boolean {
        val current = isFavorite(packageName)
        val newState = !current
        prefs.edit().putBoolean(packageName, newState).apply()
        return newState
    }

    fun getFavoritePackages(): Set<String> {
        return prefs.all.filterValues { it == true }.keys
    }
}
