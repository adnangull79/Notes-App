package com.example.notepad.UI_theme

import androidx.compose.ui.graphics.Color

// =====================================================
// CORE APP TOKENS (kept close to your existing choices)
// =====================================================

// Light
val AppBackgroundLight = Color(0xFFF6F7F8)
val TopAppBarLight     = Color(0x00000000) // transparent
val BottomBarLight     = Color(0xFFFFFFFF)
val CardBackgroundLight= Color(0xFFFFFFFF)

// Dark
val AppBackgroundDark  = Color(0xFF1C1C1C)
val TopAppBarDark      = Color(0x00000000) // transparent
val BottomBarDark      = Color(0xFF000000)
val CardBackgroundDark = Color(0xFF000000)

// Primary / Secondary
val PrimaryBlue        = Color(0xFF2196F3)
val PrimaryBlueDark    = Color(0xFF1976D2)
val PrimaryBlueLight   = Color(0xFF64B5F6)

val SecondaryBlue      = Color(0xFF03A9F4)
val SecondaryBlueDark  = Color(0xFF0288D1)
val SecondaryBlueLight = Color(0xFF4FC3F7)

// Text
val TextPrimaryLight   = Color(0xFF000000)
val TextSecondaryLight = Color(0xFF757575)
val TextTertiaryLight  = Color(0xFFBDBDBD)

val TextPrimaryDark    = Color(0xFFFFFFFF)
val TextSecondaryDark  = Color(0xFFB0B0B0)
val TextTertiaryDark   = Color(0xFF757575)

// Status / Accents
val ErrorRed           = Color(0xFFD32F2F)
val ErrorRedDark       = Color(0xFFC62828)
val SuccessGreen       = Color(0xFF4CAF50)
val SuccessGreenDark   = Color(0xFF388E3C)
val WarningOrange      = Color(0xFFFF9800)
val WarningOrangeDark  = Color(0xFFF57C00)
val InfoBlue           = Color(0xFF2196F3)
val InfoBlueDark       = Color(0xFF1976D2)

val AccentPurple       = Color(0xFF9C27B0)
val AccentPurpleDark   = Color(0xFF7B1FA2)
val AccentGreen        = Color(0xFF4CAF50)
val AccentGreenDark    = Color(0xFF388E3C)
val AccentOrange       = Color(0xFFFF9800)
val AccentOrangeDark   = Color(0xFFF57C00)

// Border & divider
val BorderLight        = Color(0xFFE0E0E0)
val BorderDark         = Color(0xFF3C3C3C)
val DividerLight       = Color(0xFFE0E0E0)
val DividerDark        = Color(0xFF3C3C3C)

// Icon
val IconPrimaryLight   = Color(0xFF000000)
val IconSecondaryLight = Color(0xFF757575)
val IconOnPrimaryLight = Color(0xFFFFFFFF)

val IconPrimaryDark    = Color(0xFFFFFFFF)
val IconSecondaryDark  = Color(0xFFB0B0B0)
val IconOnPrimaryDark  = Color(0xFFFFFFFF)

// FAB
val FabBackgroundLight = PrimaryBlue
val FabBackgroundDark  = PrimaryBlueLight
val FabContentLight    = Color(0xFFFFFFFF)
val FabContentDark     = Color(0xFF000000)

// Nav bar
val NavBarSelectedLight   = PrimaryBlue
val NavBarUnselectedLight = Color(0xFF757575)
val NavBarSelectedDark    = PrimaryBlueLight
val NavBarUnselectedDark  = Color(0xFF757575)

// Search highlight
val SearchHighlightLight  = PrimaryBlue.copy(alpha = 0.30f)
val SearchHighlightDark   = PrimaryBlueLight.copy(alpha = 0.40f)

// Overlays (we're not using ripple by default in clickable)
val OverlayLight          = Color(0x80000000)
val OverlayDark           = Color(0xB3000000)
val RippleLight           = Color(0x1F000000)
val RippleDark            = Color(0x3FFFFFFF)

// =====================================================
// CATEGORY PALETTES (light & dark tuned for contrast)
// Built-in: Work, Personal, Ideas, Checklist
// Extra user-pickable: Mint, Sky, Sun, Coral, Lavender, Sage, Peach, Slate
// Each has: container (bg), onContainer (text), icon (accent)
// =====================================================

enum class CategoryKey {
    WORK, PERSONAL, IDEAS, CHECKLIST,
    MINT, SKY, SUN, CORAL, LAVENDER, SAGE, PEACH, SLATE
}

data class CategoryPalette(
    val container: Color,
    val onContainer: Color,
    val icon: Color
)

// ---------- LIGHT mode palettes (VERY LIGHT pastel containers + saturated icons) ----------
object CategoryLight {
    val WORK       = CategoryPalette(
        container   = Color(0xFFE3F2FD),  // very light blue (Material Blue 50)
        onContainer = Color(0xFF0D47A1),
        icon        = Color(0xFF1976D2)   // blue 700
    )
    val PERSONAL   = CategoryPalette(
        container   = Color(0xFFFCE4EC),  // very light pink (Material Pink 50)
        onContainer = Color(0xFF880E4F),
        icon        = Color(0xFFE91E63)   // pink 500
    )
    val IDEAS      = CategoryPalette(
        container   = Color(0xFFE8F5E9),  // very light green (Material Green 50)
        onContainer = Color(0xFF1B5E20),
        icon        = Color(0xFF4CAF50)   // green 500
    )
    val CHECKLIST  = CategoryPalette(
        container   = Color(0xFFFFF8E1),  // very light amber (Material Amber 50)
        onContainer = Color(0xFFFF6F00),
        icon        = Color(0xFFFFA726)   // amber 400
    )

    // Extras (user selectable) - all very light pastels
    val MINT       = CategoryPalette(Color(0xFFE0F2F1), Color(0xFF004D40), Color(0xFF26A69A))
    val SKY        = CategoryPalette(Color(0xFFE1F5FE), Color(0xFF01579B), Color(0xFF29B6F6))
    val SUN        = CategoryPalette(Color(0xFFFFF9C4), Color(0xFFF57F17), Color(0xFFFFEB3B))
    val CORAL      = CategoryPalette(Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFEF5350))
    val LAVENDER   = CategoryPalette(Color(0xFFF3E5F5), Color(0xFF4A148C), Color(0xFFAB47BC))
    val SAGE       = CategoryPalette(Color(0xFFF1F8E9), Color(0xFF33691E), Color(0xFF9CCC65))
    val PEACH      = CategoryPalette(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFFF9800))
    val SLATE      = CategoryPalette(Color(0xFFECEFF1), Color(0xFF263238), Color(0xFF607D8B))
}

// ---------- DARK mode palettes (low-chroma containers + bright icons) ----------
object CategoryDark {
    val WORK       = CategoryPalette(
        container   = Color(0xFF0B1B34),  // deep navy
        onContainer = Color(0xFFD6E4FF),
        icon        = Color(0xFF93C5FD)   // sky 300
    )
    val PERSONAL   = CategoryPalette(
        container   = Color(0xFF2A0F18),  // deep rose brown
        onContainer = Color(0xFFFFD5DD),
        icon        = Color(0xFFFB7185)   // rose 400
    )
    val IDEAS      = CategoryPalette(
        container   = Color(0xFF0F1F16),  // deep green/teal
        onContainer = Color(0xFFD1FAE5),
        icon        = Color(0xFF34D399)   // emerald 400
    )
    val CHECKLIST  = CategoryPalette(
        container   = Color(0xFF241A05),  // deep amber/brown
        onContainer = Color(0xFFFFE6B3),
        icon        = Color(0xFFFBBF24)   // amber 400
    )

    // Extras (user selectable)
    val MINT       = CategoryPalette(Color(0xFF0F1D17), Color(0xFFCCF3DE), Color(0xFF86EFAC))
    val SKY        = CategoryPalette(Color(0xFF0B1729), Color(0xFFCCE0FF), Color(0xFF60A5FA))
    val SUN        = CategoryPalette(Color(0xFF1F1704), Color(0xFFFFE8A3), Color(0xFFF59E0B))
    val CORAL      = CategoryPalette(Color(0xFF2A0D0B), Color(0xFFFFD1CC), Color(0xFFFCA5A5))
    val LAVENDER   = CategoryPalette(Color(0xFF1A1026), Color(0xFFE9D5FF), Color(0xFFA78BFA))
    val SAGE       = CategoryPalette(Color(0xFF121A14), Color(0xFFCFEAD9), Color(0xFFA7F3D0))
    val PEACH      = CategoryPalette(Color(0xFF2A140E), Color(0xFFFFD8C7), Color(0xFFFCA567))
    val SLATE      = CategoryPalette(Color(0xFF12161C), Color(0xFFE5E7EB), Color(0xFF93A3B8))
}

// Public map lookups by key
object CategoryTokens {
    val Light: Map<CategoryKey, CategoryPalette> = mapOf(
        CategoryKey.WORK to CategoryLight.WORK,
        CategoryKey.PERSONAL to CategoryLight.PERSONAL,
        CategoryKey.IDEAS to CategoryLight.IDEAS,
        CategoryKey.CHECKLIST to CategoryLight.CHECKLIST,
        CategoryKey.MINT to CategoryLight.MINT,
        CategoryKey.SKY to CategoryLight.SKY,
        CategoryKey.SUN to CategoryLight.SUN,
        CategoryKey.CORAL to CategoryLight.CORAL,
        CategoryKey.LAVENDER to CategoryLight.LAVENDER,
        CategoryKey.SAGE to CategoryLight.SAGE,
        CategoryKey.PEACH to CategoryLight.PEACH,
        CategoryKey.SLATE to CategoryLight.SLATE,
    )

    val Dark: Map<CategoryKey, CategoryPalette> = mapOf(
        CategoryKey.WORK to CategoryDark.WORK,
        CategoryKey.PERSONAL to CategoryDark.PERSONAL,
        CategoryKey.IDEAS to CategoryDark.IDEAS,
        CategoryKey.CHECKLIST to CategoryDark.CHECKLIST,
        CategoryKey.MINT to CategoryDark.MINT,
        CategoryKey.SKY to CategoryDark.SKY,
        CategoryKey.SUN to CategoryDark.SUN,
        CategoryKey.CORAL to CategoryDark.CORAL,
        CategoryKey.LAVENDER to CategoryDark.LAVENDER,
        CategoryKey.SAGE to CategoryDark.SAGE,
        CategoryKey.PEACH to CategoryDark.PEACH,
        CategoryKey.SLATE to CategoryDark.SLATE,
    )
}

// Optional: quick defaults for legacy code (if you referenced CategoryWork, etc.)
val CategoryWork     = PrimaryBlue
val CategoryPersonal = AccentGreen
val CategoryIdeas    = AccentPurple
val CategoryDefault  = TextSecondaryLight