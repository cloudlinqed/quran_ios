package com.quranmedia.player.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.components.BottomNavBar
import com.quranmedia.player.presentation.components.DarkModeToggle
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.domain.util.ArabicNumeralUtils

// ── Design System Colors ──
private val PanelGreenStart = Color(0xFF234C3E)
private val PanelGreenEnd = Color(0xFF143228)

// Card-specific pastel colors (light mode only — dark mode uses cardBackground)
private val CardGreenLight = Color(0xFFE6F2E6)
private val CardYellowLight = Color(0xFFFDF6E3)
private val CardBlueLight = Color(0xFFEAF4FF)
private val CardLightLight = Color(0xFFF9F7EF)

// Dark mode card variants
private val CardGreenDark = Color(0xFF1E2E1F)
private val CardYellowDark = Color(0xFF2E2A1E)
private val CardBlueDark = Color(0xFF1E2430)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenNew(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToReciters: () -> Unit = {},
    onNavigateToSurahs: () -> Unit = {},
    onNavigateToPlayer: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToQuranReader: (Int?) -> Unit = { _ -> },
    onNavigateToQuranIndex: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToRecite: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    onNavigateToHadith: () -> Unit = {},
    onNavigateByRoute: (String) -> Unit = {}
) {
    val lastPlaybackInfo by viewModel.lastPlaybackInfo.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val reciters by viewModel.reciters.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val language = settings.appLanguage
    val isArabic = language == AppLanguage.ARABIC
    val useIndoArabic = isArabic && settings.useIndoArabicNumerals
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshLastPlaybackInfo()
        viewModel.refreshSelectedReciter()
    }

    val navigateToReaderSmart: () -> Unit = {
        coroutineScope.launch {
            val currentPlaybackPage = viewModel.getCurrentPlaybackPage()
            onNavigateToQuranReader(currentPlaybackPage)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            containerColor = AppTheme.colors.screenBackground,
            bottomBar = {
                BottomNavBar(
                    currentRoute = "home",
                    language = language,
                    onNavigate = { route -> onNavigateByRoute(route) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ── Header: Dark mode toggle + App name + Language toggle ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // App name — centered
                    Text(
                        text = if (isArabic) "الْفُرْقَان" else "AlFurqan",
                        fontFamily = if (isArabic) scheherazadeFont else null,
                        fontSize = if (isArabic) 28.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.goldAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Controls overlaid — dark mode on start, language on end
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dark mode toggle — start side
                        DarkModeToggle(
                            language = language,
                            onToggle = {
                                val current = settings.darkModePreference
                                val next = when (current) {
                                    com.quranmedia.player.data.repository.DarkModePreference.OFF ->
                                        com.quranmedia.player.data.repository.DarkModePreference.ON
                                    com.quranmedia.player.data.repository.DarkModePreference.ON ->
                                        com.quranmedia.player.data.repository.DarkModePreference.OFF
                                    else -> com.quranmedia.player.data.repository.DarkModePreference.ON
                                }
                                viewModel.setDarkMode(next)
                            }
                        )

                        Spacer(Modifier.weight(1f))

                        // Language pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AppTheme.colors.surfaceVariant,
                            modifier = Modifier.clickable {
                                viewModel.setLanguage(
                                    if (isArabic) AppLanguage.ENGLISH else AppLanguage.ARABIC
                                )
                            }
                        ) {
                            Text(
                                text = if (isArabic) "AR / EN" else "EN / AR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // ── Main content — compact, no scrolling ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Glass Media Panel ("تلاوة") ──
                    GlassMediaPanel(
                        isPlaying = playbackState.isPlaying,
                        currentSurahNumber = playbackState.currentSurah ?: lastPlaybackInfo?.surah?.number,
                        currentAyah = playbackState.currentAyah,
                        totalAyahs = playbackState.totalAyahs,
                        selectedReciter = selectedReciter ?: lastPlaybackInfo?.reciter,
                        reciters = reciters,
                        surahs = surahs,
                        language = language,
                        onReciterSelected = { viewModel.selectReciter(it) },
                        onSurahSelected = { viewModel.selectSurah(it) },
                        playbackSpeed = playbackState.playbackSpeed,
                        ayahRepeatCount = settings.ayahRepeatCount,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onPreviousAyah = { viewModel.previousAyah() },
                        onNextAyah = { viewModel.nextAyah() },
                        onSpeedClick = { viewModel.cyclePlaybackSpeed() },
                        onRepeatClick = { viewModel.cycleAyahRepeatCount() },
                        useIndoArabic = useIndoArabic
                    )

                    // ── Row 1: 3 primary feature cards (tall, square) ──
                    // Order: Quran first (rightmost in RTL Arabic), Prayer middle, Hadith last
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryCard(
                            title = if (isArabic) "القرآن الكريم" else "Quran",
                            icon = Icons.Default.MenuBook,
                            cardColor = if (isSystemInDarkTheme()) CardGreenDark else CardGreenLight,
                            iconColor = AppTheme.colors.islamicGreen,
                            language = language,
                            onClick = onNavigateToQuranIndex,
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryCard(
                            title = if (isArabic) "مواقيت الصلاة" else "Prayer Times",
                            icon = Icons.Default.Schedule,
                            cardColor = if (isSystemInDarkTheme()) CardYellowDark else CardYellowLight,
                            iconColor = AppTheme.colors.islamicGreen,
                            language = language,
                            onClick = onNavigateToPrayerTimes,
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryCard(
                            title = if (isArabic) "الأحاديث" else "Hadith",
                            icon = Icons.Default.HistoryEdu,
                            cardColor = if (isSystemInDarkTheme()) CardBlueDark else CardBlueLight,
                            iconColor = if (isSystemInDarkTheme()) Color(0xFF7EAAD4) else Color(0xFF4A729A),
                            language = language,
                            onClick = onNavigateToHadith,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ── Row 2: Tracker + Athkar ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryCard(
                            title = if (isArabic) "المتابعة اليومية" else "Daily Tracker",
                            icon = Icons.Default.CheckBox,
                            iconOnEnd = true,
                            language = language,
                            onClick = onNavigateToTracker,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryCard(
                            title = if (isArabic) "الأذكار" else "Athkar",
                            icon = Icons.Default.StarOutline,
                            iconOnEnd = true,
                            language = language,
                            onClick = onNavigateToAthkar,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ── Row 3: Downloads + Bookmarks ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryCard(
                            title = if (isArabic) "التنزيلات" else "Downloads",
                            icon = Icons.Default.Download,
                            iconOnEnd = true,
                            language = language,
                            onClick = onNavigateToDownloads,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryCard(
                            title = if (isArabic) "المفضلة" else "Bookmarks",
                            icon = Icons.Default.FavoriteBorder,
                            iconOnEnd = true,
                            language = language,
                            onClick = onNavigateToBookmarks,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        color = AppTheme.colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Glass Media Panel — dark green frosted card
// ═══════════════════════════════════════════════════════
@Composable
private fun GlassMediaPanel(
    isPlaying: Boolean,
    currentSurahNumber: Int?,
    currentAyah: Int?,
    totalAyahs: Int?,
    selectedReciter: Reciter?,
    reciters: List<Reciter>,
    surahs: List<Surah>,
    language: AppLanguage,
    onReciterSelected: (Reciter) -> Unit,
    onSurahSelected: (Surah) -> Unit,
    playbackSpeed: Float = 1.0f,
    ayahRepeatCount: Int = 1,
    onPlayPauseClick: () -> Unit,
    onPreviousAyah: () -> Unit,
    onNextAyah: () -> Unit,
    onSpeedClick: () -> Unit = {},
    onRepeatClick: () -> Unit = {},
    useIndoArabic: Boolean = false
) {
    val isArabic = language == AppLanguage.ARABIC
    var showReciterMenu by remember { mutableStateOf(false) }
    var showSurahMenu by remember { mutableStateOf(false) }

    val reciterDisplayName = selectedReciter?.let {
        if (isArabic) it.nameArabic?.takeIf { n -> n.isNotBlank() && !n.all { c -> c == '.' || c == ' ' } } ?: it.name
        else it.name
    } ?: if (isArabic) "قارئ" else "Reciter"

    val currentSurahObj = surahs.find { it.number == currentSurahNumber }
    val surahDisplayName = currentSurahObj?.let {
        if (isArabic) it.nameArabic else it.nameEnglish
    } ?: if (isArabic) "سورة" else "Surah"

    val sortedReciters = remember(reciters, language) {
        reciters.sortedBy { if (isArabic) it.nameArabic ?: it.name else it.name }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(PanelGreenStart.copy(alpha = 0.95f), PanelGreenEnd.copy(alpha = 0.95f))
                    )
                )
                .padding(20.dp)
        ) {
            // "تلاوة" heading
            Text(
                text = if (isArabic) "تلاوة" else "Recitation",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = if (isArabic) scheherazadeFont else null,
                color = Color(0xFFE8F0E8),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.33f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppTheme.colors.goldAccent)
                )
            }

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("12:45", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                Text("04:20 /", fontSize = 10.sp, color = AppTheme.colors.goldAccent)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Surah + Reciter selectors (white pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Surah pill
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { showSurahMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isArabic) "السورة" else "Surah", fontSize = 9.sp, color = Color.Gray)
                                Text(
                                    surahDisplayName,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = if (isArabic) scheherazadeFont else null,
                                    color = Color(0xFF1B1C18),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    DropdownMenu(expanded = showSurahMenu, onDismissRequest = { showSurahMenu = false }, modifier = Modifier.heightIn(max = 350.dp)) {
                        surahs.forEach { surah ->
                            val sel = surah.number == currentSurahNumber
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isArabic) "${ArabicNumeralUtils.formatNumber(surah.number, useIndoArabic)}. ${surah.nameArabic}"
                                        else "${surah.number}. ${surah.nameEnglish}",
                                        fontFamily = if (isArabic) scheherazadeFont else null,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sel) PanelGreenStart else Color.Unspecified
                                    )
                                },
                                onClick = { onSurahSelected(surah); showSurahMenu = false }
                            )
                        }
                    }
                }

                // Reciter pill
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { showReciterMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isArabic) "القارئ" else "Reciter", fontSize = 9.sp, color = Color.Gray)
                                Text(
                                    reciterDisplayName,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1C18),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    DropdownMenu(expanded = showReciterMenu, onDismissRequest = { showReciterMenu = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                        sortedReciters.forEach { reciter ->
                            val sel = reciter.id == selectedReciter?.id
                            val name = if (isArabic) reciter.nameArabic?.takeIf { it.isNotBlank() } ?: reciter.name else reciter.name
                            DropdownMenuItem(
                                text = { Text(name, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) PanelGreenStart else Color.Unspecified) },
                                onClick = { onReciterSelected(reciter); showReciterMenu = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback controls
            val isRtl = isArabic
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ayah repeat count
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (ayahRepeatCount > 1) AppTheme.colors.goldAccent.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { onRepeatClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${ayahRepeatCount}×",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.goldAccent
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    // Left arrow (points outward ← away from play)
                    // In LTR: this is "previous". In RTL (Arabic): "next" (forward = left)
                    IconButton(onClick = if (isRtl) onNextAyah else onPreviousAyah, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipPrevious, null, tint = AppTheme.colors.goldAccent, modifier = Modifier.size(22.dp))
                    }

                    Spacer(Modifier.width(8.dp))

                    // Play button — white circle
                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White,
                            contentColor = PanelGreenStart
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Right arrow (points outward → away from play)
                    // In LTR: this is "next". In RTL (Arabic): "previous" (backward = right)
                    IconButton(onClick = if (isRtl) onPreviousAyah else onNextAyah, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipNext, null, tint = AppTheme.colors.goldAccent, modifier = Modifier.size(22.dp))
                    }

                    Spacer(Modifier.width(16.dp))

                    // Playback speed
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (playbackSpeed != 1.0f) AppTheme.colors.goldAccent.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { onSpeedClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.goldAccent
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Primary Card — tall, square, icon top-center, text bottom
// ═══════════════════════════════════════════════════════
@Composable
private fun PrimaryCard(
    title: String,
    icon: ImageVector,
    cardColor: Color,
    iconColor: Color,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = language == AppLanguage.ARABIC

    Surface(
        onClick = onClick,
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(18.dp),
        color = cardColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontFamily = if (isArabic) scheherazadeFont else null,
                fontSize = if (isArabic) 13.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 16.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// Secondary Card — shorter, horizontal, icon on one side
// ═══════════════════════════════════════════════════════
@Composable
private fun SecondaryCard(
    title: String,
    icon: ImageVector,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconOnEnd: Boolean = true
) {
    val isArabic = language == AppLanguage.ARABIC

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.cardBackground
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!iconOnEnd) {
                Icon(icon, null, tint = AppTheme.colors.islamicGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = title,
                fontFamily = if (isArabic) scheherazadeFont else null,
                fontSize = if (isArabic) 13.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (iconOnEnd) Modifier.weight(1f) else Modifier
            )

            if (iconOnEnd) {
                Icon(icon, null, tint = AppTheme.colors.islamicGreen, modifier = Modifier.size(18.dp))
            }
        }
    }
}
