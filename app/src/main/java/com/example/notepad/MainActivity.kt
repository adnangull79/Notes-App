package com.example.notepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.notepad.UI_theme.NotesAppTheme
import com.example.notepad.UI_theme.ThemeController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.loadTheme(this)

        setContent {
            var isDarkTheme by remember { mutableStateOf(ThemeController.isDarkTheme) }

            ThemeController.onThemeChanged = {
                isDarkTheme = it
            }

            NotesAppTheme(darkTheme = isDarkTheme) {
                AppNavigation()  // Single entry point
            }
        }
    }
}