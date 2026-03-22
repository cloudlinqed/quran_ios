package com.quranmedia.player.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.quranmedia.player.data.repository.AppLanguage

private val ToggleGold = Color(0xFFCDAD70)

/**
 * Dark/Light mode toggle icon for screen headers.
 * Shows sun icon in dark mode, moon icon in light mode.
 */
@Composable
fun DarkModeToggle(
    language: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isArabic = language == AppLanguage.ARABIC

    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(36.dp)
            .semantics {
                this.contentDescription = if (isArabic)
                    if (isDark) "تفعيل الوضع الفاتح" else "تفعيل الوضع الداكن"
                else
                    if (isDark) "Switch to light mode" else "Switch to dark mode"
            }
    ) {
        Icon(
            if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = null,
            tint = ToggleGold,
            modifier = Modifier.size(20.dp)
        )
    }
}
