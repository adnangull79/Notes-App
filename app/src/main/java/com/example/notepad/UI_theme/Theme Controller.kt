package com.example.notepad.UI_theme

import android.content.Context
import android.content.SharedPreferences

object ThemeController {
    private const val PREFS_NAME = "theme_prefs"
    private const val THEME_KEY = "is_dark_theme"

    private var prefs: SharedPreferences? = null
    var isDarkTheme = false
        private set

    var onThemeChanged: ((Boolean) -> Unit)? = null

    fun loadTheme(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme = prefs?.getBoolean(THEME_KEY, false) ?: false
    }

    fun toggleTheme(context: Context, isDark: Boolean) {
        // Ensure prefs is initialized
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        isDarkTheme = isDark
        prefs?.edit()?.putBoolean(THEME_KEY, isDark)?.apply()
        onThemeChanged?.invoke(isDark)
    }
}