package com.quranmedia.player.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    // Brand greens
    val islamicGreen: Color,
    val darkGreen: Color,
    val lightGreen: Color,
    val goldAccent: Color,

    // Surfaces & backgrounds
    val screenBackground: Color,
    val cardBackground: Color,
    val surfaceVariant: Color,       // slightly raised surface (search bars, chips, input fields)
    val surfaceOverlay: Color,       // modal/dialog scrim overlay

    // Text
    val textPrimary: Color,          // main body text
    val textSecondary: Color,        // subtitles, hints, captions
    val textOnPrimary: Color,        // text on green/accent buttons
    val textOnHeader: Color,         // text/icons sitting on header gradients

    // Borders & dividers
    val divider: Color,
    val border: Color,               // card borders, outlines

    // Interactive
    val iconDefault: Color,          // default icon color
    val switchTrackOff: Color,       // switch unchecked track
    val chipBackground: Color,       // unselected chip/tag
    val chipSelectedBackground: Color,

    // Header / TopAppBar
    val topBarBackground: Color,     // flat TopAppBar container color
    val headerGradientStart: Color,
    val headerGradientMid: Color,
    val headerGradientEnd: Color,

    // Semantic accent colors
    val teal: Color,
    val purple: Color,
    val orange: Color,
    val blue: Color,
    val grey: Color,
)

// ── Light palette ──────────────────────────────────────────────
val LightAppColors = AppColors(
    islamicGreen = Color(0xFF2E7D32),
    darkGreen = Color(0xFF1B5E20),
    lightGreen = Color(0xFF4CAF50),
    goldAccent = Color(0xFFCDAD70),  // new design brand gold

    screenBackground = Color(0xFFF9F6EE),   // new design warm cream
    cardBackground = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5F0E8),      // warm off-white
    surfaceOverlay = Color(0x66000000),       // 40 % black

    textPrimary = Color(0xFF3E2723),         // coffee brown
    textSecondary = Color(0xFFA1887F),       // soft wood
    textOnPrimary = Color.White,
    textOnHeader = Color(0xFFCDAD70),  // gold on dark header

    divider = Color(0xFFA1887F).copy(alpha = 0.3f),
    border = Color(0xFFD7CCC8),              // warm grey border

    iconDefault = Color(0xFF757575),
    switchTrackOff = Color(0xFFBDBDBD).copy(alpha = 0.4f),
    chipBackground = Color(0xFFF5F5F5),
    chipSelectedBackground = Color(0xFF2E7D32).copy(alpha = 0.15f),

    topBarBackground = Color(0xFF275239),  // new design brand green
    headerGradientStart = Color(0xFF1B5E20),
    headerGradientMid = Color(0xFF2E7D32),
    headerGradientEnd = Color(0xFF4CAF50),

    teal = Color(0xFF00897B),
    purple = Color(0xFF7B1FA2),
    orange = Color(0xFFE65100),
    blue = Color(0xFF1565C0),
    grey = Color(0xFF757575),
)

// ── Dark palette ───────────────────────────────────────────────
val DarkAppColors = AppColors(
    islamicGreen = Color(0xFF6BAF6F),        // readable green for text & accents
    darkGreen = Color(0xFF8CB88E),           // readable sage for body text
    lightGreen = Color(0xFF8FD094),          // bright sage for highlights
    goldAccent = Color(0xFFCDAD70),          // same gold works in both modes

    screenBackground = Color(0xFF1A1A1A),    // warm dark grey (not pure black)
    cardBackground = Color(0xFF2A2A2A),      // elevated card surface
    surfaceVariant = Color(0xFF333333),       // search bars, inputs
    surfaceOverlay = Color(0x99000000),       // 60 % black

    textPrimary = Color(0xFFDDD8D3),         // warm off-white (not blinding)
    textSecondary = Color(0xFF9E9590),        // warm muted grey
    textOnPrimary = Color(0xFFE8E4DF),       // light text on accent buttons
    textOnHeader = Color(0xFFCDAD70),         // gold on dark header (matches new design)

    divider = Color(0xFF3D3D3D),
    border = Color(0xFF444444),

    iconDefault = Color(0xFF9E9590),
    switchTrackOff = Color(0xFF555555),
    chipBackground = Color(0xFF333333),
    chipSelectedBackground = Color(0xFF6BAF6F).copy(alpha = 0.25f),

    topBarBackground = Color(0xFF1E2E1F),  // darker green for dark mode header
    headerGradientStart = Color(0xFF1E2E1F), // dark olive-grey
    headerGradientMid = Color(0xFF2C452E),   // mid olive
    headerGradientEnd = Color(0xFF3A6B3E),   // olive green

    teal = Color(0xFF5BA8A0),           // muted teal
    purple = Color(0xFFB39DDB),          // soft lavender
    orange = Color(0xFFE0A155),          // warm amber
    blue = Color(0xFF7EAAD4),            // muted sky
    grey = Color(0xFFBDBDBD),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}
