package com.example.notepad.UI_theme


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val THEME_PREF_NAME = "theme_preferences"
val Context.dataStore by preferencesDataStore(name = THEME_PREF_NAME)

object ThemeDataStore {
    private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

    suspend fun saveTheme(context: Context, isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_THEME_KEY] = isDark
        }
    }

    fun getTheme(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[DARK_THEME_KEY] ?: false
        }
    }
}
