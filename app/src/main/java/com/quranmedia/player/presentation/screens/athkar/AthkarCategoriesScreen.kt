package com.quranmedia.player.presentation.screens.athkar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.AthkarCategory
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.components.BottomNavBar
import com.quranmedia.player.presentation.components.DarkModeToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthkarCategoriesScreen(
    onNavigateBack: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    onCategoryClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToQibla: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    onNavigateToHadith: () -> Unit = {},
    onNavigateByRoute: (String) -> Unit = {},
    viewModel: AthkarCategoriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage

    // Always use RTL for Athkar since content is Arabic
    CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "الأذكار" else "Athkar",
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        DarkModeToggle(language = language, onToggle = { onToggleDarkMode() })
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppTheme.colors.topBarBackground,
                        titleContentColor = AppTheme.colors.goldAccent,
                        navigationIconContentColor = AppTheme.colors.goldAccent,
                        actionIconContentColor = AppTheme.colors.goldAccent
                    )
                )
            },
            bottomBar = {
                BottomNavBar(
                    currentRoute = "athkarCategories",
                    language = language,
                    onNavigate = { route -> onNavigateByRoute(route) }
                )
            },
            containerColor = AppTheme.colors.screenBackground
        ) { paddingValues ->
            if (categories.isEmpty()) {
                // Loading or empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.islamicGreen)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            language = language,
                            onClick = { onCategoryClick(category.id) }
                        )
                    }
                }
            }
        }
    }
}

// Athkar design colors from Stitch
private val AthkarGold = Color(0xFFD6C291)
private val AthkarDarkGreen = Color(0xFF1F3E33)
private val AthkarIconBgBlue = Color(0xFFC6DFD2)
private val AthkarIconBgYellow = Color(0xFFF1E3B8)

@Composable
private fun CategoryCard(
    category: AthkarCategory,
    language: AppLanguage,
    onClick: () -> Unit
) {
    // Alternate icon bg colors between blue and yellow
    val index = category.id.hashCode()
    val iconBg = if (index % 2 == 0) AthkarIconBgBlue else AthkarIconBgYellow

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.5.dp, AthkarGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon in colored rounded square
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCategory(category.iconName),
                    contentDescription = null,
                    tint = AthkarDarkGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category name
            Text(
                text = if (language == AppLanguage.ARABIC) category.nameArabic else category.nameEnglish,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AthkarDarkGreen,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        "WbSunny" -> Icons.Default.WbSunny
        "NightsStay" -> Icons.Default.NightsStay
        "Mosque" -> Icons.Default.AccountBalance
        "Bedtime" -> Icons.Default.Bedtime
        "Alarm" -> Icons.Default.Alarm
        "Home" -> Icons.Default.Home
        "ExitToApp" -> Icons.Default.ExitToApp
        "Restaurant" -> Icons.Default.Restaurant
        "Flight" -> Icons.Default.Flight
        "Shield" -> Icons.Default.Shield
        else -> Icons.Default.AutoAwesome
    }
}
