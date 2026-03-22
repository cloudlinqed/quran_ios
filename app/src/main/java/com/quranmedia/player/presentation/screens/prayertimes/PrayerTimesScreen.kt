package com.quranmedia.player.presentation.screens.prayertimes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.domain.model.PrayerTimes
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.components.BottomNavBar
import com.quranmedia.player.presentation.components.DarkModeToggle
import com.quranmedia.player.domain.util.ArabicNumeralUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Ramadan accent colors (subtle hints) — hidden outside Ramadan
private val RamadanNight = Color(0xFF1A1A2E)
private val RamadanPurple = Color(0xFF2D2B55)
private val RamadanGold = Color(0xFFFFD700)

// Prayer-specific colors (dark mode uses AppTheme.colors for bg/text)
private val BrandGreen = Color(0xFF2B4234)      // next prayer card bg (dark green — works in both modes)
private val HighlightBgLight = Color(0xFFEFE6D5) // highlighted prayer row (light mode)
private val HighlightBgDark = Color(0xFF3A3020)   // highlighted prayer row (dark mode)
private val IconCircleBgLight = Color(0xFFF3ECE0)
private val IconCircleBgDark = Color(0xFF3A3530)

// Hijri months in order (for date adjustment calculations)
private val hijriMonthsEnglish = listOf(
    "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
    "Jumada al-Ula", "Jumada al-Thani", "Rajab", "Sha'ban",
    "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
)

private val hijriMonthsArabic = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
    "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان",
    "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
)

// Data class for adjusted Hijri date
private data class AdjustedHijriDate(
    val day: Int,
    val monthEnglish: String,
    val monthArabic: String,
    val year: Int
)

/**
 * Calculate adjusted Hijri date with proper month/year boundary handling.
 * Hijri months alternate between 29 and 30 days (roughly), but we use 30 as default.
 * Uses monthNumber (1-12) directly to avoid string matching issues with transliteration.
 */
private fun calculateAdjustedHijriDate(
    originalDay: Int,
    originalMonthNumber: Int,
    originalYear: Int,
    adjustment: Int
): AdjustedHijriDate {
    // Convert 1-based month number to 0-based index
    val currentMonthIndex = (originalMonthNumber - 1).coerceIn(0, 11)

    if (adjustment == 0) {
        return AdjustedHijriDate(
            originalDay,
            hijriMonthsEnglish[currentMonthIndex],
            hijriMonthsArabic[currentMonthIndex],
            originalYear
        )
    }

    var newDay = originalDay + adjustment
    var newMonthIndex = currentMonthIndex
    var newYear = originalYear

    // Handle day overflow (going forward past end of month)
    while (newDay > 30) {
        newDay -= 30
        newMonthIndex++
        if (newMonthIndex > 11) {
            newMonthIndex = 0
            newYear++
        }
    }

    // Handle day underflow (going backward past start of month)
    while (newDay < 1) {
        newMonthIndex--
        if (newMonthIndex < 0) {
            newMonthIndex = 11
            newYear--
        }
        newDay += 30  // Previous month's days (simplified to 30)
    }

    return AdjustedHijriDate(
        day = newDay,
        monthEnglish = hijriMonthsEnglish[newMonthIndex],
        monthArabic = hijriMonthsArabic[newMonthIndex],
        year = newYear
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onNavigateBack: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    onNavigateToAthanSettings: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToQibla: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    onNavigateToHadith: () -> Unit = {},
    onNavigateByRoute: (String) -> Unit = {},
    viewModel: PrayerTimesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.appLanguage

    var showLocationDialog by remember { mutableStateOf(false) }
    var cityInput by remember { mutableStateOf("") }

    // Snackbar for offline warnings
    val snackbarHostState = remember { SnackbarHostState() }

    // Show offline warning as snackbar
    LaunchedEffect(uiState.offlineWarning) {
        uiState.offlineWarning?.let { warning ->
            snackbarHostState.showSnackbar(
                message = warning,
                duration = SnackbarDuration.Short
            )
            viewModel.clearOfflineWarning()
        }
    }

    // Location permission launcher (approximate location is sufficient for prayer times)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "مواقيت الصلاة" else "Prayer Times",
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
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = AppTheme.colors.darkGreen,
                        contentColor = AppTheme.colors.textOnPrimary
                    )
                }
            },
            containerColor = AppTheme.colors.screenBackground,
            bottomBar = {
                BottomNavBar(
                    currentRoute = "prayerTimes",
                    language = language,
                    onNavigate = { route -> onNavigateByRoute(route) }
                )
            }
        ) { paddingValues ->
            val useIndoArabic = language == AppLanguage.ARABIC && settings.useIndoArabicNumerals
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Location card - compact
                LocationCard(
                    location = uiState.location,
                    hijriDate = uiState.prayerTimes?.hijriDate?.let { hijri ->
                        // Apply Hijri date adjustment with proper month/year handling
                        val adjusted = calculateAdjustedHijriDate(
                            originalDay = hijri.day,
                            originalMonthNumber = hijri.monthNumber,
                            originalYear = hijri.year,
                            adjustment = settings.hijriDateAdjustment
                        )
                        if (language == AppLanguage.ARABIC) {
                            val dayStr = ArabicNumeralUtils.formatNumber(adjusted.day, useIndoArabic)
                            val yearStr = ArabicNumeralUtils.formatNumber(adjusted.year, useIndoArabic)
                            "$dayStr ${adjusted.monthArabic} $yearStr"
                        } else {
                            "${adjusted.day} ${adjusted.monthEnglish} ${adjusted.year} AH"
                        }
                    },
                    language = language,
                    hasPermission = uiState.hasLocationPermission,
                    onDetectLocation = {
                        if (uiState.hasLocationPermission) {
                            viewModel.detectLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        }
                    },
                    onManualLocation = { showLocationDialog = true },
                    useIndoArabic = useIndoArabic
                )

                // Loading indicator
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppTheme.colors.islamicGreen)
                    }
                }

                // Error message
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }

                // Next prayer countdown
                uiState.prayerTimes?.let { prayerTimes ->
                    uiState.nextPrayer?.let { nextPrayer ->
                        NextPrayerCard(
                            nextPrayer = nextPrayer,
                            hoursRemaining = uiState.hoursRemaining,
                            minutesRemaining = uiState.minutesRemaining,
                            prayerTime = getPrayerTime(prayerTimes, nextPrayer),
                            language = language,
                            useIndoArabic = useIndoArabic
                        )
                    }

                    // Qibla Direction card — dark green bar
                    Surface(
                        onClick = { onNavigateToQibla() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = BrandGreen
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward, null,
                                tint = AppTheme.colors.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "اتجاه القبلة" else "Qibla Direction",
                                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.goldAccent
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Explore, null, tint = AppTheme.colors.goldAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // All prayer times - takes remaining space
                    PrayerTimesCard(
                        prayerTimes = prayerTimes,
                        nextPrayer = uiState.nextPrayer,
                        language = language,
                        useIndoArabic = useIndoArabic,
                        modifier = Modifier.weight(1f)
                    )
                }

                // No location message
                if (uiState.location == null && !uiState.isLoading && uiState.error == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.lightGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AppTheme.colors.islamicGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC)
                                    "يرجى تحديد موقعك للحصول على مواقيت الصلاة"
                                else
                                    "Please set your location to view prayer times",
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                textAlign = TextAlign.Center,
                                color = AppTheme.colors.islamicGreen
                            )
                        }
                    }
                }
            }
        }
    }

    // Location input dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.ARABIC) "أدخل المدينة" else "Enter City",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                )
            },
            text = {
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "اسم المدينة" else "City Name"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (cityInput.isNotBlank()) {
                            viewModel.setManualLocation(cityInput)
                            showLocationDialog = false
                            cityInput = ""
                        }
                    }
                ) {
                    Text(if (language == AppLanguage.ARABIC) "موافق" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text(if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel")
                }
            }
        )
    }

}

@Composable
private fun LocationCard(
    location: com.quranmedia.player.domain.model.UserLocation?,
    hijriDate: String?,
    language: AppLanguage,
    hasPermission: Boolean,
    onDetectLocation: () -> Unit,
    onManualLocation: () -> Unit,
    useIndoArabic: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.cardBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                hijriDate?.let { date ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = AppTheme.colors.goldAccent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = date,
                            fontFamily = scheherazadeFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = AppTheme.colors.goldAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = buildString {
                            append(location?.cityName ?: if (language == AppLanguage.ARABIC) "الموقع غير محدد" else "Location not set")
                            location?.countryName?.let { append(", $it") }
                        },
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 12.sp,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }
            IconButton(onClick = onDetectLocation, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MyLocation, "Detect", tint = AppTheme.colors.goldAccent, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onManualLocation, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, "Manual", tint = AppTheme.colors.goldAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun NextPrayerCard(
    nextPrayer: PrayerType,
    hoursRemaining: Int,
    minutesRemaining: Int,
    prayerTime: LocalTime,
    language: AppLanguage,
    useIndoArabic: Boolean = false
) {
    // Format time remaining based on language
    val timeRemaining = if (language == AppLanguage.ARABIC) {
        val hoursStr = ArabicNumeralUtils.formatNumber(hoursRemaining, useIndoArabic)
        val minutesStr = ArabicNumeralUtils.formatNumber(minutesRemaining, useIndoArabic)
        if (hoursRemaining > 0) {
            "$hoursStr س $minutesStr د"
        } else {
            "$minutesStr د"
        }
    } else {
        if (hoursRemaining > 0) {
            "${hoursRemaining}h ${minutesRemaining}m"
        } else {
            "${minutesRemaining}m"
        }
    }

    // Ramadan fasting labels — hidden outside Ramadan
    val isRamadanPrayer = false

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BrandGreen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Countdown box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF3A5344)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "متبقي" else "remaining",
                        fontSize = 10.sp,
                        color = AppTheme.colors.goldAccent.copy(alpha = 0.8f)
                    )
                    Text(
                        text = timeRemaining,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Prayer info + icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "الصلاة القادمة" else "Next Prayer",
                        fontSize = 10.sp,
                        color = AppTheme.colors.goldAccent
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) nextPrayer.nameArabic else nextPrayer.nameEnglish,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatPrayerTime(prayerTime, language, useIndoArabic),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AppTheme.colors.topBarBackground,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            getIconForPrayer(nextPrayer), null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimesCard(
    prayerTimes: PrayerTimes,
    nextPrayer: PrayerType?,
    language: AppLanguage,
    useIndoArabic: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.cardBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Section header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.surfaceVariant)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("✦", color = AppTheme.colors.goldAccent, fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC) "أوقات الصلاة" else "Prayer Times",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("✦", color = AppTheme.colors.goldAccent, fontSize = 10.sp)
                }
            }

            // Prayer times list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                PrayerTimeRow(PrayerType.FAJR, prayerTimes.fajr, nextPrayer == PrayerType.FAJR, language, useIndoArabic)
                PrayerTimeRow(PrayerType.SUNRISE, prayerTimes.sunrise, nextPrayer == PrayerType.SUNRISE, language, useIndoArabic)
                PrayerTimeRow(PrayerType.DHUHR, prayerTimes.dhuhr, nextPrayer == PrayerType.DHUHR, language, useIndoArabic)
                PrayerTimeRow(PrayerType.ASR, prayerTimes.asr, nextPrayer == PrayerType.ASR, language, useIndoArabic)
                PrayerTimeRow(PrayerType.MAGHRIB, prayerTimes.maghrib, nextPrayer == PrayerType.MAGHRIB, language, useIndoArabic)
                PrayerTimeRow(PrayerType.ISHA, prayerTimes.isha, nextPrayer == PrayerType.ISHA, language, useIndoArabic)
            }
        }
    }
}

@Composable
private fun PrayerTimeRow(
    prayerType: PrayerType,
    time: LocalTime,
    isNext: Boolean,
    language: AppLanguage,
    useIndoArabic: Boolean = false,
    isRamadanPrayer: Boolean = false
) {
    val isArabic = language == AppLanguage.ARABIC
    val rowBg = if (isNext) if (isSystemInDarkTheme()) HighlightBgDark else HighlightBgLight else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(if (isSystemInDarkTheme()) IconCircleBgDark else IconCircleBgLight, shape = RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getIconForPrayer(prayerType), null,
                    tint = if (prayerType == PrayerType.FAJR || prayerType == PrayerType.ISHA)
                        BrandGreen else AppTheme.colors.goldAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isArabic) prayerType.nameArabic else prayerType.nameEnglish,
                fontFamily = if (isArabic) scheherazadeFont else null,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isNext) AppTheme.colors.goldAccent.copy(alpha = 0.2f) else Color.Transparent
        ) {
            Text(
                text = formatPrayerTime(time, language, useIndoArabic),
                fontSize = 13.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

private fun formatPrayerTime(time: LocalTime, language: AppLanguage, useIndoArabic: Boolean = false): String {
    val timeStr = if (language == AppLanguage.ARABIC) {
        // Arabic format: use صباحاً (morning) and مساءً (evening) instead of AM/PM
        val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
        val minute = time.minute.toString().padStart(2, '0')
        val period = if (time.hour < 12) "صباحاً" else "مساءً"
        "$hour:$minute $period"
    } else {
        time.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }
    return if (useIndoArabic) ArabicNumeralUtils.toIndoArabic(timeStr) else timeStr
}

private fun getIconForPrayer(prayerType: PrayerType): ImageVector {
    return when (prayerType) {
        PrayerType.FAJR -> Icons.Default.NightsStay
        PrayerType.SUNRISE -> Icons.Default.WbSunny
        PrayerType.DHUHR -> Icons.Default.WbSunny
        PrayerType.ASR -> Icons.Default.WbTwilight
        PrayerType.MAGHRIB -> Icons.Default.WbTwilight
        PrayerType.ISHA -> Icons.Default.NightsStay
    }
}

private fun getPrayerTime(prayerTimes: PrayerTimes, prayerType: PrayerType): LocalTime {
    return when (prayerType) {
        PrayerType.FAJR -> prayerTimes.fajr
        PrayerType.SUNRISE -> prayerTimes.sunrise
        PrayerType.DHUHR -> prayerTimes.dhuhr
        PrayerType.ASR -> prayerTimes.asr
        PrayerType.MAGHRIB -> prayerTimes.maghrib
        PrayerType.ISHA -> prayerTimes.isha
    }
}
