package com.example

import android.content.Context
import android.content.SharedPreferences

class FavoritesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isFavorite(file: PdfFile?): Boolean {
        if (file == null) return false
        val key = getPdfKey(file)
        val favs = prefs.getStringSet(KEY_FAVORITES, null)
        return favs != null && favs.contains(key)
    }

    fun setFavorite(file: PdfFile?, favorite: Boolean) {
        if (file == null) return
        val key = getPdfKey(file)
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        val updated = current.toMutableSet()
        if (favorite) {
            updated.add(key)
        } else {
            updated.remove(key)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, updated).apply()
    }

    fun toggleFavorite(file: PdfFile?): Boolean {
        val newState = !isFavorite(file)
        setFavorite(file, newState)
        return newState
    }

    fun getAllFavoriteKeys(): Set<String> {
        return (prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()).toHashSet()
    }

    fun restoreFavoriteKeys(keys: Set<String>?) {
        if (keys == null) return
        prefs.edit().putStringSet(KEY_FAVORITES, keys.toHashSet()).apply()
    }

    companion object {
        private const val PREF_NAME = "pdf_favorites_prefs"
        private const val KEY_FAVORITES = "favorite_pdf_identifiers"

        fun getPdfKey(file: PdfFile?): String {
            if (file == null) return ""
            if (file.path.trim().isNotEmpty()) return file.path.trim()
            if (file.uri != null) return file.uri.toString()
            return file.name.ifEmpty { file.id.toString() }
        }
    }
}
