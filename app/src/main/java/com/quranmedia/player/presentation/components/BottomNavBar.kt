package com.quranmedia.player.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.quranmedia.player.presentation.theme.AppTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont

// Design system colors — dark mode aware
private val NavGold = Color(0xFFD6C291)

data class BottomNavItem(
    val labelArabic: String,
    val labelEnglish: String,
    val icon: ImageVector,
    val route: String
)

val mainNavItems = listOf(
    BottomNavItem("الرئيسية", "Home", Icons.Default.Home, "home"),
    BottomNavItem("القرآن", "Quran", Icons.Default.MenuBook, "quranIndex"),
    BottomNavItem("الصلاة", "Prayer", Icons.Default.Schedule, "prayerTimes"),
    BottomNavItem("الأذكار", "Athkar", Icons.Default.StarOutline, "athkarCategories"),
    BottomNavItem("المزيد", "More", Icons.Default.MoreHoriz, "more")
)

/**
 * Shared bottom navigation bar for all screens except QuranReader, QuranIndex, and Reading.
 *
 * @param currentRoute The current screen route to highlight the active tab.
 * @param language The app language for label display.
 * @param onNavigate Callback with the route string for navigation.
 * @param onMoreClick Callback when "More" is tapped — shows overflow with remaining items.
 */
@Composable
fun BottomNavBar(
    currentRoute: String,
    language: AppLanguage,
    onNavigate: (String) -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isArabic = language == AppLanguage.ARABIC
    var showMoreMenu by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val navBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val navTabColor = if (isDark) AppTheme.colors.goldAccent else AppTheme.colors.topBarBackground
    // Dropdown bg handled globally by MaterialTheme.colorScheme.surfaceContainer

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = if (isArabic) "شريط التنقل" else "Navigation bar"
            },
        color = navBg,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter out the current screen's tab — dynamic bar
                val visibleItems = mainNavItems.filter { !isRouteActive(currentRoute, it.route) }

                visibleItems.forEach { item ->
                    val isActive = false // never active since we filtered it out
                    val isMore = item.route == "more"

                    val tabLabel = if (isArabic) item.labelArabic else item.labelEnglish

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .semantics {
                                this.contentDescription = tabLabel
                                this.role = Role.Tab
                            }
                            .clickable {
                                if (isMore) {
                                    showMoreMenu = true
                                } else {
                                    onNavigate(item.route)
                                }
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                item.icon, null,
                                tint = navTabColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (isArabic) item.labelArabic else item.labelEnglish,
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = navTabColor,
                                maxLines = 1
                            )
                        }

                        // More dropdown
                        if (isMore) {
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                // Dark mode: black bg from MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                MoreMenuItem(if (isArabic) "الأحاديث" else "Hadith", Icons.Default.HistoryEdu) {
                                    showMoreMenu = false; onNavigate("hadithLibrary")
                                }
                                MoreMenuItem(if (isArabic) "المتابعة اليومية" else "Daily Tracker", Icons.Default.CheckBox) {
                                    showMoreMenu = false; onNavigate("tracker")
                                }
                                MoreMenuItem(if (isArabic) "المفضلة" else "Bookmarks", Icons.Default.FavoriteBorder) {
                                    showMoreMenu = false; onNavigate("bookmarks")
                                }
                                MoreMenuItem(if (isArabic) "التنزيلات" else "Downloads", Icons.Default.Download) {
                                    showMoreMenu = false; onNavigate("downloads")
                                }
                                MoreMenuItem(if (isArabic) "اتجاه القبلة" else "Qibla", Icons.Default.Explore) {
                                    showMoreMenu = false; onNavigate("qibla")
                                }
                                MoreMenuItem(if (isArabic) "الإعدادات" else "Settings", Icons.Default.Settings) {
                                    showMoreMenu = false; onNavigate("settings")
                                }
                                MoreMenuItem(if (isArabic) "حول التطبيق" else "About", Icons.Default.Info) {
                                    showMoreMenu = false; onNavigate("about")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(title, fontSize = 14.sp) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) }
    )
}

private fun isRouteActive(currentRoute: String, tabRoute: String): Boolean {
    return when (tabRoute) {
        "home" -> currentRoute == "home"
        "quranIndex" -> currentRoute in listOf("quranIndex", "quranReader")
        "prayerTimes" -> currentRoute in listOf("prayerTimes", "athanSettings")
        "athkarCategories" -> currentRoute.startsWith("athkar")
        else -> false
    }
}
