package com.example

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {

    private const val PREF_NAME = "pdf_theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"

    private val _darkModeFlow = MutableStateFlow(false)
    val darkModeFlow: StateFlow<Boolean> = _darkModeFlow.asStateFlow()

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val dark = prefs.getBoolean(KEY_DARK_MODE, false)
        if (_darkModeFlow.value != dark) {
            _darkModeFlow.value = dark
        }
        return dark
    }

    fun setDarkMode(context: Context, darkMode: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply()
        _darkModeFlow.value = darkMode
        applyTheme(darkMode)
    }

    fun applySavedTheme(context: Context) {
        val dark = isDarkMode(context)
        applyTheme(dark)
        if (context is Activity) {
            DynamicColors.applyIfAvailable(context)
        }
    }

    private fun applyTheme(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
