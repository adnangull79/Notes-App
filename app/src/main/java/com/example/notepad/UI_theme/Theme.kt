package com.example.notepad.UI_theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================
// MATERIAL 3 COLOR SCHEMES
// ============================================

private val LightColorScheme = lightColorScheme(
    primary              = PrimaryBlue,
    onPrimary            = Color.White,
    primaryContainer     = PrimaryBlueLight,
    onPrimaryContainer   = TextPrimaryLight,

    secondary            = SecondaryBlue,
    onSecondary          = Color.White,
    secondaryContainer   = SecondaryBlueLight,
    onSecondaryContainer = TextPrimaryLight,

    tertiary             = AccentPurple,
    onTertiary           = Color.White,
    tertiaryContainer    = AccentPurple.copy(alpha = 0.15f),
    onTertiaryContainer  = TextPrimaryLight,

    error                = ErrorRed,
    onError              = Color.White,

    background           = AppBackgroundLight,
    onBackground         = TextPrimaryLight,

    surface              = BottomBarLight,
    onSurface            = TextPrimaryLight,
    surfaceVariant       = CardBackgroundLight,
    onSurfaceVariant     = TextSecondaryLight,

    outline              = BorderLight,
    surfaceTint          = PrimaryBlue
)

private val DarkColorScheme = darkColorScheme(
    primary              = PrimaryBlueLight,
    onPrimary            = TextPrimaryDark,
    primaryContainer     = PrimaryBlueDark,
    onPrimaryContainer   = TextPrimaryDark,

    secondary            = SecondaryBlueLight,
    onSecondary          = TextPrimaryDark,
    secondaryContainer   = SecondaryBlueDark,
    onSecondaryContainer = TextPrimaryDark,

    tertiary             = AccentPurpleDark,
    onTertiary           = TextPrimaryDark,
    tertiaryContainer    = AccentPurpleDark,
    onTertiaryContainer  = TextPrimaryDark,

    error                = ErrorRedDark,
    onError              = Color.White,

    background           = AppBackgroundDark,
    onBackground         = TextPrimaryDark,

    surface              = BottomBarDark,
    onSurface            = TextPrimaryDark,
    surfaceVariant       = CardBackgroundDark,
    onSurfaceVariant     = TextSecondaryDark,

    outline              = BorderDark,
    surfaceTint          = PrimaryBlueLight
)

// ============================================
// THEME ENTRY - Uses ThemeController
// ============================================
@Composable
fun NotesAppTheme(
    darkTheme: Boolean = ThemeController.isDarkTheme,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = (if (darkTheme) AppBackgroundDark else AppBackgroundLight).toArgb()
            window.navigationBarColor = (if (darkTheme) BottomBarDark else BottomBarLight).toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ============================================
// CATEGORY COLOR HELPERS - Uses ThemeController
// ============================================

@Composable
fun categoryContainerColor(key: CategoryKey): Color {
    val darkTheme = ThemeController.isDarkTheme
    val palette = if (darkTheme) CategoryTokens.Dark[key] else CategoryTokens.Light[key]
    return palette?.container ?: (if (darkTheme) CardBackgroundDark else CardBackgroundLight)
}

@Composable
fun categoryOnContainerColor(key: CategoryKey): Color {
    val darkTheme = ThemeController.isDarkTheme
    val palette = if (darkTheme) CategoryTokens.Dark[key] else CategoryTokens.Light[key]
    return palette?.onContainer ?: (if (darkTheme) TextPrimaryDark else TextPrimaryLight)
}

@Composable
fun categoryIconColor(key: CategoryKey): Color {
    val darkTheme = ThemeController.isDarkTheme
    val palette = if (darkTheme) CategoryTokens.Dark[key] else CategoryTokens.Light[key]
    return palette?.icon ?: (if (darkTheme) IconPrimaryDark else IconPrimaryLight)
}

// Optional helper: derive a CategoryKey from a folder name
fun categoryKeyFromName(name: String?): CategoryKey {
    val n = (name ?: "").lowercase()
    return when {
        listOf("work", "meeting", "docs", "note", "project").any { n.contains(it) } -> CategoryKey.WORK
        listOf("personal", "life", "home", "private", "diary", "anniversary", "love").any { n.contains(it) } -> CategoryKey.PERSONAL
        listOf("idea", "ideas", "brainstorm", "inspiration").any { n.contains(it) } -> CategoryKey.IDEAS
        listOf("check", "todo", "task", "list", "checklist", "grocery", "shopping").any { n.contains(it) } -> CategoryKey.CHECKLIST
        else -> CategoryKey.SLATE
    }
}