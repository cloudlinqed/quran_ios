package com.quranmedia.player.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.DarkModePreference
import com.quranmedia.player.data.repository.PrayerNotificationMode
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.data.repository.ReminderInterval
import com.quranmedia.player.data.source.FontDownloadProgress
import com.quranmedia.player.data.source.FontDownloadState
import com.quranmedia.player.domain.model.AsrJuristicMethod
import com.quranmedia.player.domain.model.Athan
import com.quranmedia.player.domain.model.CalculationMethod
import com.quranmedia.player.domain.model.PrayerType
import com.quranmedia.player.presentation.screens.prayertimes.AthanSettingsViewModel
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.theme.ReadingThemes
import com.quranmedia.player.presentation.util.Strings
import com.quranmedia.player.presentation.util.layoutDirection
import com.quranmedia.player.domain.model.AvailableTafseers
import com.quranmedia.player.domain.model.TafseerInfo
import com.quranmedia.player.domain.model.TafseerDownload
import com.quranmedia.player.domain.model.TafseerType

// creamPaper alias removed, use AppTheme.colors.screenBackground directly

private enum class SettingsTab {
    READING, PRAYER
}

internal enum class UnifiedColorPickerType {
    BACKGROUND, TEXT, HEADER
}

// Helper function to safely convert Long to Color
private fun Long.toColor(): Color {
    val argb = (this and 0xFFFFFFFF).toInt()
    return Color(argb)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedSettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    athanViewModel: AthanSettingsViewModel = hiltViewModel(),
    initialTab: String = "reading",
    onNavigateBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val athanUiState by athanViewModel.uiState.collectAsState()
    val language = settings.appLanguage
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(if (initialTab == "prayer") SettingsTab.PRAYER else SettingsTab.READING) }

    // Font download progress
    val v4Progress by settingsViewModel.v4DownloadProgress.collectAsState()

    var showIntervalDialog by remember { mutableStateOf(false) }
    var showQuietHoursDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf<UnifiedColorPickerType?>(null) }

    // Prayer settings state
    var selectedPrayerForAthan by remember { mutableStateOf<PrayerType?>(null) }
    var showGlobalAthanSelector by remember { mutableStateOf(false) }
    var showPerPrayerCustomization by remember { mutableStateOf(false) }
    var pendingModeChange by remember { mutableStateOf<Pair<PrayerType, PrayerNotificationMode>?>(null) }

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (selectedTab == SettingsTab.READING) {
                settingsViewModel.setReminderEnabled(true)
            } else {
                pendingModeChange?.let { (prayerType, mode) ->
                    athanViewModel.setPrayerMode(prayerType, mode)
                }
            }
        }
        pendingModeChange = null
    }

    // Helper function for prayer mode with permission check
    fun setPrayerModeWithPermissionCheck(prayerType: PrayerType, mode: PrayerNotificationMode) {
        if (mode == PrayerNotificationMode.SILENT) {
            athanViewModel.setPrayerMode(prayerType, mode)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingModeChange = Pair(prayerType, mode)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        athanViewModel.setPrayerMode(prayerType, mode)
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                // 3D polished header with wood theme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                            ambientColor = AppTheme.colors.darkGreen.copy(alpha = 0.4f),
                            spotColor = AppTheme.colors.darkGreen.copy(alpha = 0.4f)
                        )
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(AppTheme.colors.topBarBackground)
                        .statusBarsPadding()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Title row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AppTheme.colors.goldAccent
                                )
                            }
                            Text(
                                text = Strings.settings.get(language),
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.goldAccent
                            )
                            Spacer(modifier = Modifier.size(48.dp))
                        }

                        // Tab row
                        TabRow(
                            selectedTabIndex = selectedTab.ordinal,
                            containerColor = Color.Transparent,
                            contentColor = AppTheme.colors.goldAccent,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                    color = AppTheme.colors.goldAccent,
                                    height = 3.dp
                                )
                            },
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedTab == SettingsTab.READING,
                                onClick = { selectedTab = SettingsTab.READING },
                                text = {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) "القراءة" else "Reading",
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedTab == SettingsTab.READING) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                selectedContentColor = AppTheme.colors.goldAccent,
                                unselectedContentColor = AppTheme.colors.goldAccent.copy(alpha = 0.6f)
                            )
                            Tab(
                                selected = selectedTab == SettingsTab.PRAYER,
                                onClick = { selectedTab = SettingsTab.PRAYER },
                                text = {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) "الصلاة" else "Prayer",
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedTab == SettingsTab.PRAYER) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                selectedContentColor = AppTheme.colors.goldAccent,
                                unselectedContentColor = AppTheme.colors.goldAccent.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            },
            containerColor = AppTheme.colors.screenBackground
        ) { paddingValues ->
            when (selectedTab) {
                SettingsTab.READING -> {
                    val downloadedTafseers by settingsViewModel.downloadedTafseers.collectAsState()
                    val tafseerProgress by settingsViewModel.tafseerDownloadProgress.collectAsState()
                    val downloadingTafseerId by settingsViewModel.downloadingTafseerId.collectAsState()

                    ReadingSettingsContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        settings = settings,
                        language = language,
                        v4Progress = v4Progress,
                        viewModel = settingsViewModel,
                        context = context,
                        notificationPermissionLauncher = notificationPermissionLauncher,
                        showIntervalDialog = showIntervalDialog,
                        onShowIntervalDialog = { showIntervalDialog = it },
                        showQuietHoursDialog = showQuietHoursDialog,
                        onShowQuietHoursDialog = { showQuietHoursDialog = it },
                        showColorPicker = showColorPicker,
                        onShowColorPicker = { showColorPicker = it },
                        downloadedTafseers = downloadedTafseers,
                        tafseerProgress = tafseerProgress,
                        downloadingTafseerId = downloadingTafseerId,
                        onDownloadTafseer = { settingsViewModel.downloadTafseer(it) },
                        onDeleteTafseer = { settingsViewModel.deleteTafseer(it) }
                    )
                }
                SettingsTab.PRAYER -> {
                    PrayerSettingsContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        uiState = athanUiState,
                        language = language,
                        viewModel = athanViewModel,
                        showGlobalAthanSelector = showGlobalAthanSelector,
                        onShowGlobalAthanSelector = { showGlobalAthanSelector = it },
                        showPerPrayerCustomization = showPerPrayerCustomization,
                        onShowPerPrayerCustomization = { showPerPrayerCustomization = it },
                        selectedPrayerForAthan = selectedPrayerForAthan,
                        onSelectPrayerForAthan = { selectedPrayerForAthan = it },
                        setPrayerModeWithPermissionCheck = ::setPrayerModeWithPermissionCheck
                    )
                }
            }
        }
    }

    // Dialogs
    if (showIntervalDialog) {
        IntervalSelectionDialogWood(
            currentInterval = settings.readingReminderInterval,
            language = language,
            onDismiss = { showIntervalDialog = false },
            onIntervalSelected = { interval ->
                settingsViewModel.setReminderInterval(interval)
                showIntervalDialog = false
            }
        )
    }

    if (showQuietHoursDialog) {
        QuietHoursDialogWood(
            startHour = settings.quietHoursStart,
            endHour = settings.quietHoursEnd,
            language = language,
            onDismiss = { showQuietHoursDialog = false },
            onHoursSelected = { start, end ->
                settingsViewModel.setQuietHours(start, end)
                showQuietHoursDialog = false
            }
        )
    }

    // V4 Tajweed font download prompt — removed, Tajweed now uses built-in TajweedRuleEngine

    showColorPicker?.let { type ->
        val currentColor = when (type) {
            UnifiedColorPickerType.BACKGROUND -> settings.customBackgroundColor.toColor()
            UnifiedColorPickerType.TEXT -> settings.customTextColor.toColor()
            UnifiedColorPickerType.HEADER -> settings.customHeaderColor.toColor()
        }

        val title = when (type) {
            UnifiedColorPickerType.BACKGROUND -> if (language == AppLanguage.ARABIC) "لون الخلفية" else "Background Color"
            UnifiedColorPickerType.TEXT -> if (language == AppLanguage.ARABIC) "لون النص" else "Text Color"
            UnifiedColorPickerType.HEADER -> if (language == AppLanguage.ARABIC) "لون العنوان" else "Header Color"
        }

        ColorPickerDialogWood(
            title = title,
            currentColor = currentColor,
            language = language,
            onDismiss = { showColorPicker = null },
            onColorSelected = { color ->
                val colorValue = color.toArgb().toLong()
                when (type) {
                    UnifiedColorPickerType.BACKGROUND -> settingsViewModel.setCustomBackgroundColor(colorValue)
                    UnifiedColorPickerType.TEXT -> settingsViewModel.setCustomTextColor(colorValue)
                    UnifiedColorPickerType.HEADER -> settingsViewModel.setCustomHeaderColor(colorValue)
                }
            }
        )
    }

    // Global Athan selection dialog
    if (showGlobalAthanSelector) {
        CompactAthanSelectionDialogWood(
            prayerType = PrayerType.FAJR,
            availableAthans = athanUiState.availableAthans,
            downloadedAthanIds = athanUiState.downloadedAthans.map { it.id }.toSet(),
            currentAthanId = athanUiState.fajrAthanId,
            isLoading = athanUiState.isLoadingAthans,
            downloadingAthanId = athanUiState.downloadingAthanId,
            previewingAthanId = athanUiState.previewingAthanId,
            language = language,
            onDismiss = { showGlobalAthanSelector = false },
            onSelectAthan = { athanId ->
                athanViewModel.setGlobalAthan(athanId)
                showGlobalAthanSelector = false
            },
            onDownloadAthan = { athan -> athanViewModel.downloadAthan(athan) },
            onPreviewAthan = { athan -> athanViewModel.previewAthan(athan) },
            onStopPreview = { athanViewModel.stopPreview() }
        )
    }

    // Athan selection dialog (per-prayer)
    if (selectedPrayerForAthan != null) {
        CompactAthanSelectionDialogWood(
            prayerType = selectedPrayerForAthan!!,
            availableAthans = athanUiState.availableAthans,
            downloadedAthanIds = athanUiState.downloadedAthans.map { it.id }.toSet(),
            currentAthanId = athanUiState.getSelectedAthanId(selectedPrayerForAthan!!),
            isLoading = athanUiState.isLoadingAthans,
            downloadingAthanId = athanUiState.downloadingAthanId,
            previewingAthanId = athanUiState.previewingAthanId,
            language = language,
            onDismiss = { selectedPrayerForAthan = null },
            onSelectAthan = { athanId ->
                athanViewModel.selectAthanForPrayer(selectedPrayerForAthan!!, athanId)
                selectedPrayerForAthan = null
            },
            onDownloadAthan = { athan -> athanViewModel.downloadAthan(athan) },
            onPreviewAthan = { athan -> athanViewModel.previewAthan(athan) },
            onStopPreview = { athanViewModel.stopPreview() }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingSettingsContent(
    modifier: Modifier = Modifier,
    settings: com.quranmedia.player.data.repository.UserSettings,
    language: AppLanguage,
    v4Progress: FontDownloadProgress,
    viewModel: SettingsViewModel,
    context: android.content.Context,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    showIntervalDialog: Boolean,
    onShowIntervalDialog: (Boolean) -> Unit,
    showQuietHoursDialog: Boolean,
    onShowQuietHoursDialog: (Boolean) -> Unit,
    showColorPicker: UnifiedColorPickerType?,
    onShowColorPicker: (UnifiedColorPickerType?) -> Unit,
    downloadedTafseers: List<TafseerDownload>,
    tafseerProgress: Map<String, Float>,
    downloadingTafseerId: String?,
    onDownloadTafseer: (String) -> Unit,
    onDeleteTafseer: (String) -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reading Theme Section
        SettingsSectionWood(
            title = if (language == AppLanguage.ARABIC) "مظهر القراءة" else "Reading Theme",
            language = language
        ) {
            // Theme selection chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReadingTheme.entries.toList().forEach { theme: ReadingTheme ->
                    ThemeChipCompactWood(
                        theme = theme,
                        isSelected = settings.readingTheme == theme,
                        language = language,
                        onClick = { viewModel.setReadingTheme(theme) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom theme color pickers
            if (settings.readingTheme == ReadingTheme.CUSTOM) {
                CustomThemeColorPickersWood(
                    backgroundColor = settings.customBackgroundColor.toColor(),
                    textColor = settings.customTextColor.toColor(),
                    headerColor = settings.customHeaderColor.toColor(),
                    language = language,
                    onBackgroundColorClick = { onShowColorPicker(UnifiedColorPickerType.BACKGROUND) },
                    onTextColorClick = { onShowColorPicker(UnifiedColorPickerType.TEXT) },
                    onHeaderColorClick = { onShowColorPicker(UnifiedColorPickerType.HEADER) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Theme preview
            ThemePreviewCompactWood(
                theme = settings.readingTheme,
                customBackgroundColor = settings.customBackgroundColor.toColor(),
                customTextColor = settings.customTextColor.toColor(),
                customHeaderColor = settings.customHeaderColor.toColor(),
                language = language
            )
        }

        // Reading Reminder Section
        SettingsSectionWood(
            title = if (language == AppLanguage.ARABIC) "تذكير القراءة" else "Reading Reminder",
            language = language
        ) {
            SettingsSwitchItemWood(
                title = if (language == AppLanguage.ARABIC) "تفعيل التذكير" else "Enable Reminder",
                subtitle = if (language == AppLanguage.ARABIC)
                    "تذكيرك بمواصلة قراءة القرآن"
                else
                    "Remind you to continue reading Quran",
                icon = Icons.Default.Notifications,
                checked = settings.readingReminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@SettingsSwitchItemWood
                            }
                        }
                    }
                    viewModel.setReminderEnabled(enabled)
                },
                language = language
            )

            if (settings.readingReminderEnabled) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = AppTheme.colors.divider
                )

                SettingsClickableItemWood(
                    title = if (language == AppLanguage.ARABIC) "فترة التذكير" else "Reminder Interval",
                    subtitle = settings.readingReminderInterval.getLabel(language),
                    icon = Icons.Default.Schedule,
                    onClick = { onShowIntervalDialog(true) },
                    language = language
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = AppTheme.colors.divider
                )

                SettingsClickableItemWood(
                    title = if (language == AppLanguage.ARABIC) "ساعات الهدوء" else "Quiet Hours",
                    subtitle = if (language == AppLanguage.ARABIC)
                        "من ${formatHour(settings.quietHoursStart)} إلى ${formatHour(settings.quietHoursEnd)}"
                    else
                        "From ${formatHour(settings.quietHoursStart)} to ${formatHour(settings.quietHoursEnd)}",
                    icon = Icons.Default.Bedtime,
                    onClick = { onShowQuietHoursDialog(true) },
                    language = language
                )

                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "لن يتم إرسال تذكيرات خلال ساعات الهدوء"
                    else
                        "No reminders will be sent during quiet hours",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 12.sp,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 52.dp, top = 4.dp)
                )
            }
        }

        // Athkar Notifications Section
        SettingsSectionWood(
            title = if (language == AppLanguage.ARABIC) "إشعارات الأذكار" else "Athkar Reminders",
            language = language
        ) {
            SettingsSwitchItemWood(
                title = if (language == AppLanguage.ARABIC) "أذكار الصباح" else "Morning Athkar",
                subtitle = String.format("%02d:%02d", settings.morningAthkarNotificationHour, settings.morningAthkarNotificationMinute),
                icon = Icons.Default.WbSunny,
                checked = settings.morningAthkarNotificationEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@SettingsSwitchItemWood
                        }
                    }
                    viewModel.setMorningAthkarNotificationEnabled(enabled)
                },
                language = language
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = AppTheme.colors.divider
            )

            SettingsSwitchItemWood(
                title = if (language == AppLanguage.ARABIC) "أذكار المساء" else "Evening Athkar",
                subtitle = String.format("%02d:%02d", settings.eveningAthkarNotificationHour, settings.eveningAthkarNotificationMinute),
                icon = Icons.Default.NightsStay,
                checked = settings.eveningAthkarNotificationEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@SettingsSwitchItemWood
                        }
                    }
                    viewModel.setEveningAthkarNotificationEnabled(enabled)
                },
                language = language
            )
        }

        // Display Settings Section
        SettingsSectionWood(
            title = if (language == AppLanguage.ARABIC) "إعدادات العرض" else "Display Settings",
            language = language
        ) {
            // Dark Mode selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = AppTheme.colors.islamicGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "المظهر" else "Appearance",
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DarkModePreference.entries.forEach { pref ->
                            val isSelected = settings.darkModePreference == pref
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setDarkModePreference(pref) },
                                label = {
                                    Text(
                                        text = pref.getLabel(language),
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = AppTheme.colors.chipBackground,
                                    selectedContainerColor = AppTheme.colors.chipSelectedBackground,
                                    labelColor = AppTheme.colors.textPrimary,
                                    selectedLabelColor = AppTheme.colors.islamicGreen,
                                    selectedLeadingIconColor = AppTheme.colors.islamicGreen
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = AppTheme.colors.border,
                                    selectedBorderColor = AppTheme.colors.islamicGreen,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = AppTheme.colors.divider
            )

            // Mushaf Font selector
            Text(
                text = if (language == AppLanguage.ARABIC) "خط المصحف" else "Mushaf Font",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.colors.islamicGreen,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(true, false) // true = SVG, false = Plain Text
                options.forEach { isQCF ->
                    val isSelected = settings.useQCFFont == isQCF
                    val label = if (isQCF) {
                        if (language == AppLanguage.ARABIC) "خط المصحف" else "Mushaf Font"
                    } else {
                        if (language == AppLanguage.ARABIC) "نص عادي" else "Plain Text"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) AppTheme.colors.islamicGreen.copy(alpha = 0.15f)
                                else AppTheme.colors.chipBackground
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) AppTheme.colors.islamicGreen else AppTheme.colors.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setUseQCFFont(isQCF) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AppTheme.colors.islamicGreen else AppTheme.colors.textSecondary
                        )
                    }
                }
            }

            Text(
                text = if (language == AppLanguage.ARABIC)
                    "يتم التبديل تلقائيًا للنص العادي عند تفعيل TalkBack"
                else
                    "Automatically switches to Plain Text when TalkBack is enabled",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = AppTheme.colors.divider
            )

            // Tajweed info — V4 font download removed, Tajweed now uses built-in color engine
            Text(
                text = if (language == AppLanguage.ARABIC)
                    "اختر مظهر التجويد من مظهر القراءة لعرض ألوان التجويد"
                else
                    "Select Tajweed from Reading Theme to display Tajweed colors",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = AppTheme.colors.divider
            )

            SettingsSwitchItemWood(
                title = if (language == AppLanguage.ARABIC) "خط عريض" else "Bold Font",
                subtitle = if (language == AppLanguage.ARABIC)
                    "استخدام خط عريض للقرآن في وضع القراءة"
                else
                    "Use bold font for Quran text in reading mode",
                icon = Icons.Default.FormatBold,
                checked = settings.useBoldFont,
                onCheckedChange = { viewModel.setUseBoldFont(it) },
                language = language
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = AppTheme.colors.divider
            )

            SettingsSwitchItemWood(
                title = if (language == AppLanguage.ARABIC) "إبقاء الشاشة مضاءة" else "Keep Screen On",
                subtitle = if (language == AppLanguage.ARABIC)
                    "منع الشاشة من النوم أثناء القراءة والتلاوة"
                else
                    "Prevent screen from sleeping during reading and recitation",
                icon = Icons.Default.ScreenLockPortrait,
                checked = settings.keepScreenOn,
                onCheckedChange = { viewModel.setKeepScreenOn(it) },
                language = language
            )
        }

        // TODO: Recite Settings Section - Hidden until feature is ready
        /*
        SettingsSectionWood(
            title = if (language == AppLanguage.ARABIC) "إعدادات التسميع" else "Recite Settings",
            language = language
        ) {
            SettingsSwitchItemWood(
                title = if (language == AppLanguage.ARABIC) "التقييم الفوري" else "Real-Time Assessment",
                subtitle = if (language == AppLanguage.ARABIC)
                    "تقييم كل آية على حدة مع تنبيه اهتزازي عند الخطأ"
                else
                    "Assess each ayah separately with haptic feedback on mistakes",
                icon = Icons.Default.Speed,
                checked = settings.reciteRealTimeAssessment,
                onCheckedChange = { viewModel.setReciteRealTimeAssessment(it) },
                language = language
            )

            if (settings.reciteRealTimeAssessment) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = AppTheme.colors.divider
                )

                SettingsSwitchItemWood(
                    title = if (language == AppLanguage.ARABIC) "الاهتزاز عند الخطأ" else "Vibrate on Mistake",
                    subtitle = if (language == AppLanguage.ARABIC)
                        "اهتزاز ثلاثي عند اكتشاف خطأ في التلاوة"
                    else
                        "Triple vibration when a mistake is detected",
                    icon = Icons.Default.Vibration,
                    checked = settings.reciteHapticOnMistake,
                    onCheckedChange = { viewModel.setReciteHapticOnMistake(it) },
                    language = language
                )
            }

            Text(
                text = if (language == AppLanguage.ARABIC)
                    "الوضع العادي: تسجيل كامل ثم عرض النتائج\nالوضع الفوري: تقييم كل آية على حدة"
                else
                    "Normal mode: Full recording then show results\nReal-time mode: Assess each ayah separately",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 52.dp, top = 8.dp)
            )
        }
        */

        // Tafseer Downloads Section
        SettingsSectionWood(
            title = if (language == AppLanguage.ARABIC) "تحميل التفسير" else "Tafseer Downloads",
            language = language
        ) {
            TafseerDropdownSection(
                downloadedTafseers = downloadedTafseers,
                tafseerProgress = tafseerProgress,
                downloadingTafseerId = downloadingTafseerId,
                language = language,
                viewModel = viewModel,
                onDownloadTafseer = onDownloadTafseer,
                onDeleteTafseer = onDeleteTafseer
            )
        }

        // Language Section
        SettingsSectionWood(
            title = if (language == AppLanguage.ARABIC) "اللغة" else "Language",
            language = language
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.toList().forEach { lang ->
                    LanguageOptionWood(
                        language = lang,
                        isSelected = settings.appLanguage == lang,
                        onClick = { viewModel.setAppLanguage(lang) },
                        currentLanguage = language
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (language == AppLanguage.ARABIC)
                    "سيتم تطبيق اللغة فوراً على كامل التطبيق"
                else
                    "Language will be applied immediately to the entire app",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun PrayerSettingsContent(
    modifier: Modifier = Modifier,
    uiState: com.quranmedia.player.presentation.screens.prayertimes.AthanSettingsUiState,
    language: AppLanguage,
    viewModel: AthanSettingsViewModel,
    showGlobalAthanSelector: Boolean,
    onShowGlobalAthanSelector: (Boolean) -> Unit,
    showPerPrayerCustomization: Boolean,
    onShowPerPrayerCustomization: (Boolean) -> Unit,
    selectedPrayerForAthan: PrayerType?,
    onSelectPrayerForAthan: (PrayerType?) -> Unit,
    setPrayerModeWithPermissionCheck: (PrayerType, PrayerNotificationMode) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Battery Optimization - for reliable Athan
        item {
            val context = LocalContext.current
            val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }
            var isBatteryOptimizationDisabled by remember {
                mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
            }

            // Launcher to refresh state after returning from settings
            val batteryLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                isBatteryOptimizationDisabled = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }

            CompactCardWood {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isBatteryOptimizationDisabled) {
                            val intent = Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            batteryLauncher.launch(intent)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isBatteryOptimizationDisabled) AppTheme.colors.islamicGreen else Color(0xFFFF9800)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = AppTheme.colors.textOnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Text
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "البطارية" else "Battery",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            color = AppTheme.colors.darkGreen
                        )
                        Text(
                            text = if (isBatteryOptimizationDisabled) {
                                if (language == AppLanguage.ARABIC) "✓ الأذان سيعمل في الوقت المحدد" else "✓ Athan will play on time"
                            } else {
                                if (language == AppLanguage.ARABIC) "اضغط للسماح - لضمان عمل الأذان" else "Tap to allow - ensures Athan works"
                            },
                            fontSize = 12.sp,
                            color = if (isBatteryOptimizationDisabled) AppTheme.colors.islamicGreen else Color(0xFFFF9800),
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                        )
                    }

                    // Status indicator
                    if (isBatteryOptimizationDisabled) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppTheme.colors.islamicGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Calculation Method Selection
        item {
            var expanded by remember { mutableStateOf(false) }
            CompactCardWood {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "طريقة الحساب" else "Calculation Method",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.darkGreen,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppTheme.colors.chipBackground)
                                .clickable { expanded = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC)
                                    uiState.calculationMethod.nameArabic
                                else
                                    uiState.calculationMethod.nameEnglish,
                                fontSize = 14.sp,
                                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                color = AppTheme.colors.islamicGreen
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = AppTheme.colors.islamicGreen
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            CalculationMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (language == AppLanguage.ARABIC)
                                                method.nameArabic else method.nameEnglish,
                                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                                            fontWeight = if (method == uiState.calculationMethod)
                                                FontWeight.Bold else FontWeight.Normal,
                                            color = if (method == uiState.calculationMethod)
                                                AppTheme.colors.islamicGreen else AppTheme.colors.textPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.setCalculationMethod(method)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Asr Juristic Method
        item {
            CompactCardWood {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "حساب العصر" else "Asr Calculation",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.darkGreen,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsrJuristicMethod.entries.forEach { method ->
                            val isSelected = method == uiState.asrJuristicMethod
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAsrJuristicMethod(method) },
                                label = {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC)
                                            method.nameArabic else method.nameEnglish,
                                        fontSize = 13.sp,
                                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppTheme.colors.islamicGreen,
                                    selectedLabelColor = AppTheme.colors.textOnPrimary,
                                    containerColor = AppTheme.colors.chipBackground
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Hijri Date Adjustment
        item {
            CompactCardWood {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "تعديل التاريخ الهجري" else "Hijri Date Adjustment",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.darkGreen,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-2, -1, 0, 1, 2).forEach { adjustment ->
                            val isSelected = adjustment == uiState.hijriDateAdjustment
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setHijriDateAdjustment(adjustment) },
                                label = {
                                    Text(
                                        text = when {
                                            adjustment > 0 -> "+$adjustment"
                                            adjustment == 0 -> "0"
                                            else -> "$adjustment"
                                        },
                                        fontSize = 13.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppTheme.colors.islamicGreen,
                                    selectedLabelColor = AppTheme.colors.textOnPrimary,
                                    containerColor = AppTheme.colors.chipBackground
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (uiState.hijriDateAdjustment != 0) {
                        Text(
                            text = if (language == AppLanguage.ARABIC)
                                "تعديل ${if (uiState.hijriDateAdjustment > 0) "+" else ""}${uiState.hijriDateAdjustment} ${if (kotlin.math.abs(uiState.hijriDateAdjustment) == 1) "يوم" else "أيام"}"
                            else
                                "${if (uiState.hijriDateAdjustment > 0) "+" else ""}${uiState.hijriDateAdjustment} ${if (kotlin.math.abs(uiState.hijriDateAdjustment) == 1) "day" else "days"}",
                            fontSize = 11.sp,
                            color = AppTheme.colors.textSecondary,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Check if any prayer uses Athan mode
        val hasAnyAthanModeEnabled = uiState.fajrMode == PrayerNotificationMode.ATHAN ||
                uiState.dhuhrMode == PrayerNotificationMode.ATHAN ||
                uiState.asrMode == PrayerNotificationMode.ATHAN ||
                uiState.maghribMode == PrayerNotificationMode.ATHAN ||
                uiState.ishaMode == PrayerNotificationMode.ATHAN

        // Global Muezzin Selector
        if (hasAnyAthanModeEnabled) {
            item {
                CompactCardWood {
                    Column {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "المؤذن" else "Muezzin",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.colors.darkGreen,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 6.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppTheme.colors.chipBackground)
                                .clickable { onShowGlobalAthanSelector(true) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.getSelectedAthanName(PrayerType.FAJR) ?: (if (language == AppLanguage.ARABIC) "اختر المؤذن" else "Select Muezzin"),
                                fontSize = 13.sp,
                                color = if (uiState.getSelectedAthanName(PrayerType.FAJR) != null) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.isAthanDownloaded(PrayerType.FAJR)) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AppTheme.colors.islamicGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = AppTheme.colors.iconDefault,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onShowPerPrayerCustomization(!showPerPrayerCustomization) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "تخصيص لكل صلاة" else "Customize per prayer",
                                fontSize = 12.sp,
                                color = AppTheme.colors.islamicGreen,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                if (showPerPrayerCustomization) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = AppTheme.colors.islamicGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (showPerPrayerCustomization) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = AppTheme.colors.divider
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            listOf(
                                PrayerType.FAJR,
                                PrayerType.DHUHR,
                                PrayerType.ASR,
                                PrayerType.MAGHRIB,
                                PrayerType.ISHA
                            ).forEachIndexed { index, prayerType ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                PerPrayerAthanSelectorWood(
                                    prayerType = prayerType,
                                    selectedAthanName = uiState.getSelectedAthanName(prayerType),
                                    isAthanDownloaded = uiState.isAthanDownloaded(prayerType),
                                    language = language,
                                    onSelectAthan = { onSelectPrayerForAthan(prayerType) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // All prayers in one compact card
        item {
            CompactCardWood {
                Column {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "إعدادات كل صلاة" else "Prayer Notifications",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.darkGreen,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 6.dp)
                    )

                    listOf(
                        PrayerType.FAJR,
                        PrayerType.DHUHR,
                        PrayerType.ASR,
                        PrayerType.MAGHRIB,
                        PrayerType.ISHA
                    ).forEachIndexed { index, prayerType ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = AppTheme.colors.divider
                            )
                        }
                        CompactPrayerRowWood(
                            prayerType = prayerType,
                            mode = uiState.getPrayerMode(prayerType),
                            selectedAthanName = uiState.getSelectedAthanName(prayerType),
                            isAthanDownloaded = uiState.isAthanDownloaded(prayerType),
                            language = language,
                            onModeChange = { mode -> setPrayerModeWithPermissionCheck(prayerType, mode) },
                            onSelectAthan = { onSelectPrayerForAthan(prayerType) }
                        )
                    }
                }
            }
        }

        // Check if any prayer has notifications enabled
        val hasAnyNotificationEnabled = uiState.fajrMode != PrayerNotificationMode.SILENT ||
                uiState.dhuhrMode != PrayerNotificationMode.SILENT ||
                uiState.asrMode != PrayerNotificationMode.SILENT ||
                uiState.maghribMode != PrayerNotificationMode.SILENT ||
                uiState.ishaMode != PrayerNotificationMode.SILENT

        val hasAnyAthanEnabled = uiState.fajrMode == PrayerNotificationMode.ATHAN ||
                uiState.dhuhrMode == PrayerNotificationMode.ATHAN ||
                uiState.asrMode == PrayerNotificationMode.ATHAN ||
                uiState.maghribMode == PrayerNotificationMode.ATHAN ||
                uiState.ishaMode == PrayerNotificationMode.ATHAN

        val hasAnyAlertEnabled = uiState.fajrMode == PrayerNotificationMode.NOTIFICATION ||
                uiState.dhuhrMode == PrayerNotificationMode.NOTIFICATION ||
                uiState.asrMode == PrayerNotificationMode.NOTIFICATION ||
                uiState.maghribMode == PrayerNotificationMode.NOTIFICATION ||
                uiState.ishaMode == PrayerNotificationMode.NOTIFICATION

        // Notification timing
        if (hasAnyNotificationEnabled) {
            item {
                CompactCardWood {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "توقيت التنبيه" else "Notify Before",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.colors.darkGreen,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 5, 10, 15, 30).forEach { minutes ->
                                val isSelected = uiState.notifyMinutesBefore == minutes
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setNotifyMinutesBefore(minutes) },
                                    label = {
                                        Text(
                                            text = if (minutes == 0) {
                                                if (language == AppLanguage.ARABIC) "الآن" else "Now"
                                            } else "${minutes}m",
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppTheme.colors.islamicGreen,
                                        selectedLabelColor = AppTheme.colors.textOnPrimary,
                                        containerColor = AppTheme.colors.chipBackground
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Athan options
        if (hasAnyAthanEnabled) {
            item {
                CompactCardWood {
                    Column {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "خيارات الأذان" else "Athan Options",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.colors.darkGreen,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 4.dp)
                        )

                        CompactToggleRowWood(
                            title = if (language == AppLanguage.ARABIC) "حتى في الوضع الصامت" else "Even in Silent Mode",
                            checked = uiState.athanInSilentMode,
                            onCheckedChange = { viewModel.setAthanInSilentMode(it) },
                            language = language
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = AppTheme.colors.divider
                        )

                        CompactToggleRowWood(
                            title = if (language == AppLanguage.ARABIC) "الصوت الأعلى" else "Max Volume",
                            checked = uiState.athanMaxVolume,
                            onCheckedChange = { viewModel.setAthanMaxVolume(it) },
                            language = language
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = AppTheme.colors.divider
                        )

                        CompactToggleRowWood(
                            title = if (language == AppLanguage.ARABIC) "قلب للإيقاف" else "Flip to Silence",
                            checked = uiState.flipToSilence,
                            onCheckedChange = { viewModel.setFlipToSilence(it) },
                            language = language
                        )
                    }
                }
            }
        }

        // Notification options
        if (hasAnyAlertEnabled) {
            item {
                CompactCardWood {
                    Column {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "خيارات الإشعار" else "Notification Options",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.colors.darkGreen,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 4.dp)
                        )

                        CompactToggleRowWood(
                            title = if (language == AppLanguage.ARABIC) "الصوت" else "Sound",
                            checked = uiState.notificationSound,
                            onCheckedChange = { viewModel.setNotificationSound(it) },
                            language = language
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = AppTheme.colors.divider
                        )

                        CompactToggleRowWood(
                            title = if (language == AppLanguage.ARABIC) "الاهتزاز" else "Vibration",
                            checked = uiState.notificationVibrate,
                            onCheckedChange = { viewModel.setNotificationVibrate(it) },
                            language = language
                        )
                    }
                }
            }
        }

        // Downloaded athans
        if (hasAnyAthanEnabled && uiState.downloadedAthans.isNotEmpty()) {
            item {
                CompactCardWood {
                    Column {
                        Text(
                            text = if (language == AppLanguage.ARABIC) "الأذانات المحملة" else "Downloaded",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.colors.darkGreen,
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            modifier = Modifier.padding(12.dp, 10.dp, 12.dp, 4.dp)
                        )

                        uiState.downloadedAthans.forEachIndexed { index, athan ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = AppTheme.colors.divider
                                )
                            }
                            CompactDownloadedAthanRowWood(
                                athan = athan,
                                isPlaying = uiState.previewingAthanId == athan.id,
                                onPlay = { viewModel.previewAthan(athan) },
                                onStop = { viewModel.stopPreview() },
                                onDelete = { viewModel.deleteAthan(athan.id) }
                            )
                        }
                    }
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============= Wood-themed Component Composables =============

@Composable
private fun SettingsSectionWood(
    title: String,
    language: AppLanguage,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = AppTheme.colors.islamicGreen.copy(alpha = 0.2f),
                spotColor = AppTheme.colors.islamicGreen.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            AppTheme.colors.islamicGreen.copy(alpha = 0.05f),
                            AppTheme.colors.cardBackground
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.islamicGreen,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItemWood(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    language: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(2.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppTheme.colors.lightGreen.copy(alpha = 0.2f),
                            AppTheme.colors.islamicGreen.copy(alpha = 0.15f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppTheme.colors.islamicGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = AppTheme.colors.textSecondary,
                maxLines = 1
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.colors.textOnPrimary,
                checkedTrackColor = AppTheme.colors.islamicGreen,
                uncheckedThumbColor = AppTheme.colors.cardBackground,
                uncheckedTrackColor = AppTheme.colors.switchTrackOff
            )
        )
    }
}

@Composable
private fun SettingsClickableItemWood(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    language: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(2.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppTheme.colors.lightGreen.copy(alpha = 0.2f),
                            AppTheme.colors.islamicGreen.copy(alpha = 0.15f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppTheme.colors.islamicGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = AppTheme.colors.islamicGreen,
                maxLines = 1
            )
        }

        Icon(
            if (language == AppLanguage.ARABIC) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppTheme.colors.iconDefault,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ThemeChipCompactWood(
    theme: ReadingTheme,
    isSelected: Boolean,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val themeColors = ReadingThemes.getTheme(theme)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) themeColors.accent else AppTheme.colors.chipBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) themeColors.accent else AppTheme.colors.border
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Small swatch showing the actual theme background color
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) AppTheme.colors.textOnPrimary.copy(alpha = 0.3f) else themeColors.background)
                    .then(
                        if (!isSelected) Modifier.border(0.5.dp, AppTheme.colors.border, RoundedCornerShape(4.dp))
                        else Modifier
                    )
            )

            Text(
                text = theme.getLabel(language),
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AppTheme.colors.textOnPrimary else AppTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun CustomThemeColorPickersWood(
    backgroundColor: Color,
    textColor: Color,
    headerColor: Color,
    language: AppLanguage,
    onBackgroundColorClick: () -> Unit,
    onTextColorClick: () -> Unit,
    onHeaderColorClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (language == AppLanguage.ARABIC) "تخصيص الألوان" else "Customize Colors",
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colors.islamicGreen
        )

        ColorPickerRowWood(
            label = if (language == AppLanguage.ARABIC) "الخلفية" else "Background",
            color = backgroundColor,
            language = language,
            onClick = onBackgroundColorClick
        )

        ColorPickerRowWood(
            label = if (language == AppLanguage.ARABIC) "النص" else "Text",
            color = textColor,
            language = language,
            onClick = onTextColorClick
        )

        ColorPickerRowWood(
            label = if (language == AppLanguage.ARABIC) "العنوان" else "Header",
            color = headerColor,
            language = language,
            onClick = onHeaderColorClick
        )
    }
}

@Composable
private fun ColorPickerRowWood(
    label: String,
    color: Color,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            fontSize = 13.sp,
            color = AppTheme.colors.textPrimary
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, AppTheme.colors.border, CircleShape)
        )
    }
}

@Composable
private fun ThemePreviewCompactWood(
    theme: ReadingTheme,
    customBackgroundColor: Color = Color.White,
    customTextColor: Color = Color.Black,
    customHeaderColor: Color = Color.Unspecified,
    language: AppLanguage
) {
    val resolvedHeaderColor = if (customHeaderColor == Color.Unspecified) AppTheme.colors.islamicGreen else customHeaderColor
    val themeColors = if (theme == ReadingTheme.CUSTOM) {
        ReadingThemes.getTheme(
            theme,
            com.quranmedia.player.presentation.theme.CustomThemeColors(
                backgroundColor = customBackgroundColor,
                textColor = customTextColor,
                headerColor = resolvedHeaderColor
            )
        )
    } else {
        ReadingThemes.getTheme(theme)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontFamily = scheherazadeFont,
                        fontSize = 18.sp,
                        color = themeColors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColors.highlightBackground)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                                fontFamily = scheherazadeFont,
                                fontSize = 16.sp,
                                color = themeColors.highlight
                            )
                        }
                        Text(
                            text = " ﴿٢﴾",
                            fontFamily = scheherazadeFont,
                            fontSize = 12.sp,
                            color = themeColors.ayahMarker
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionWood(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    currentLanguage: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AppTheme.colors.islamicGreen.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(
                                AppTheme.colors.lightGreen.copy(alpha = 0.3f),
                                AppTheme.colors.islamicGreen.copy(alpha = 0.2f)
                            )
                        } else {
                            listOf(
                                AppTheme.colors.lightGreen.copy(alpha = 0.1f),
                                AppTheme.colors.islamicGreen.copy(alpha = 0.05f)
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = if (isSelected) AppTheme.colors.islamicGreen else AppTheme.colors.iconDefault,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (language) {
                    AppLanguage.ARABIC -> "العربية"
                    AppLanguage.ENGLISH -> "English"
                },
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AppTheme.colors.darkGreen else AppTheme.colors.textPrimary
            )
            Text(
                text = when (language) {
                    AppLanguage.ARABIC -> if (currentLanguage == AppLanguage.ARABIC) "نظام الكتابة من اليمين إلى اليسار" else "Right-to-left"
                    AppLanguage.ENGLISH -> if (currentLanguage == AppLanguage.ARABIC) "من اليسار إلى اليمين" else "Left-to-right"
                },
                fontFamily = if (currentLanguage == AppLanguage.ARABIC) scheherazadeFont else null,
                fontSize = 11.sp,
                color = AppTheme.colors.textSecondary
            )
        }

        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AppTheme.colors.islamicGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FontDownloadItemWood(
    title: String,
    subtitle: String,
    progress: FontDownloadProgress,
    language: AppLanguage,
    formatSize: (Long) -> String,
    downloadedSize: Long,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val isDownloaded = progress.state == FontDownloadState.DOWNLOADED
    val isDownloading = progress.state == FontDownloadState.DOWNLOADING
    val hasError = progress.state == FontDownloadState.ERROR

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDownloaded) AppTheme.colors.lightGreen.copy(alpha = 0.1f) else AppTheme.colors.chipBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = when {
                            isDownloaded -> if (language == AppLanguage.ARABIC)
                                "تم التحميل (${formatSize(downloadedSize)})"
                            else
                                "Downloaded (${formatSize(downloadedSize)})"
                            isDownloading -> if (language == AppLanguage.ARABIC)
                                "جاري التحميل... ${(progress.progress * 100).toInt()}%"
                            else
                                "Downloading... ${(progress.progress * 100).toInt()}%"
                            hasError -> progress.errorMessage ?: (if (language == AppLanguage.ARABIC) "خطأ" else "Error")
                            else -> subtitle
                        },
                        fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                        fontSize = 11.sp,
                        color = when {
                            isDownloaded -> AppTheme.colors.islamicGreen
                            hasError -> Color.Red
                            else -> AppTheme.colors.textSecondary
                        }
                    )
                }

                when {
                    isDownloading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppTheme.colors.islamicGreen,
                            strokeWidth = 2.dp
                        )
                    }
                    isDownloaded -> {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (language == AppLanguage.ARABIC) "حذف" else "Delete",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.islamicGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppTheme.colors.textOnPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) "تحميل" else "Download",
                                fontSize = 12.sp,
                                color = AppTheme.colors.textOnPrimary
                            )
                        }
                    }
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AppTheme.colors.islamicGreen,
                    trackColor = AppTheme.colors.divider
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TafseerDropdownSection(
    downloadedTafseers: List<TafseerDownload>,
    tafseerProgress: Map<String, Float>,
    downloadingTafseerId: String?,
    language: AppLanguage,
    viewModel: SettingsViewModel,
    onDownloadTafseer: (String) -> Unit,
    onDeleteTafseer: (String) -> Unit
) {
    val availableTafseers = AvailableTafseers.getSortedByLanguage(language.name.lowercase())
    val downloadedIds = downloadedTafseers.map { it.tafseerInfo.id }.toSet()
    var expanded by remember { mutableStateOf(false) }
    var selectedTafseer by remember { mutableStateOf(availableTafseers.firstOrNull()) }

    val isArabic = language == AppLanguage.ARABIC

    Text(
        text = if (isArabic) "حمّل التفسير للقراءة بدون اتصال" else "Download tafseer for offline reading",
        fontFamily = if (isArabic) scheherazadeFont else null,
        fontSize = 11.sp,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    // Dropdown + Download/Delete button row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedTafseer?.let {
                    if (it.language == "arabic") it.nameArabic ?: it.nameEnglish else it.nameEnglish
                } ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 13.sp,
                    fontFamily = if (selectedTafseer?.language == "arabic") scheherazadeFont else null
                ),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                leadingIcon = selectedTafseer?.let { tafseer ->
                    if (downloadedIds.contains(tafseer.id)) {
                        { Icon(Icons.Default.CheckCircle, null, tint = AppTheme.colors.islamicGreen, modifier = Modifier.size(18.dp)) }
                    } else null
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.islamicGreen,
                    unfocusedBorderColor = AppTheme.colors.border
                ),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Group: Word Meanings & Grammar (shaded)
                val wordAndGrammar = availableTafseers.filter {
                    it.type == TafseerType.WORD_MEANING || it.type == TafseerType.GRAMMAR
                }
                val tafseers = availableTafseers.filter { it.type == TafseerType.TAFSEER }

                if (tafseers.isNotEmpty()) {
                    tafseers.forEach { tafseer ->
                        val isDownloaded = downloadedIds.contains(tafseer.id)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Language badge
                                    Text(
                                        text = if (tafseer.language == "arabic") "ع" else "EN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.textOnPrimary,
                                        modifier = Modifier
                                            .background(
                                                color = if (tafseer.language == "arabic") AppTheme.colors.goldAccent else AppTheme.colors.islamicGreen,
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        text = if (tafseer.language == "arabic") tafseer.nameArabic ?: tafseer.nameEnglish else tafseer.nameEnglish,
                                        fontFamily = if (tafseer.language == "arabic") scheherazadeFont else null,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isDownloaded) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AppTheme.colors.islamicGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedTafseer = tafseer
                                expanded = false
                            }
                        )
                    }
                }

                // Divider before word meanings / grammar section
                if (wordAndGrammar.isNotEmpty() && tafseers.isNotEmpty()) {
                    HorizontalDivider(color = AppTheme.colors.divider)
                }

                // Word Meanings & Grammar items with shaded background
                wordAndGrammar.forEach { tafseer ->
                    val isDownloaded = downloadedIds.contains(tafseer.id)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Type badge
                                Text(
                                    text = if (tafseer.type == TafseerType.GRAMMAR) {
                                        if (isArabic) "نحو" else "Gram"
                                    } else {
                                        if (tafseer.language == "arabic") "ع" else "EN"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textOnPrimary,
                                    modifier = Modifier
                                        .background(
                                            color = if (tafseer.type == TafseerType.GRAMMAR) AppTheme.colors.textPrimary
                                            else if (tafseer.language == "arabic") AppTheme.colors.goldAccent else AppTheme.colors.islamicGreen,
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = if (tafseer.language == "arabic") tafseer.nameArabic ?: tafseer.nameEnglish else tafseer.nameEnglish,
                                    fontFamily = if (tafseer.language == "arabic") scheherazadeFont else null,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isDownloaded) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AppTheme.colors.islamicGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            selectedTafseer = tafseer
                            expanded = false
                        },
                        modifier = Modifier.background(AppTheme.colors.textSecondary.copy(alpha = 0.15f))
                    )
                }
            }
        }

        // Download or Delete button
        val selected = selectedTafseer
        if (selected != null) {
            val isSelectedDownloaded = downloadedIds.contains(selected.id)
            val isSelectedDownloading = downloadingTafseerId == selected.id
            val progress = tafseerProgress[selected.id]

            when {
                isSelectedDownloading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                        CircularProgressIndicator(
                            progress = { progress ?: 0f },
                            modifier = Modifier.size(32.dp),
                            color = AppTheme.colors.islamicGreen,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "${((progress ?: 0f) * 100).toInt()}%",
                            fontSize = 8.sp,
                            color = AppTheme.colors.islamicGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                isSelectedDownloaded -> {
                    IconButton(
                        onClick = { onDeleteTafseer(selected.id) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = if (isArabic) "حذف" else "Delete",
                            tint = Color.Red.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    IconButton(
                        onClick = { onDownloadTafseer(selected.id) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(AppTheme.colors.islamicGreen, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = if (isArabic) "تحميل" else "Download",
                            tint = AppTheme.colors.textOnPrimary
                        )
                    }
                }
            }
        }
    }

    // Download progress bar
    if (downloadingTafseerId != null && selectedTafseer?.id == downloadingTafseerId) {
        val progress = tafseerProgress[downloadingTafseerId]
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress ?: 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AppTheme.colors.islamicGreen,
            trackColor = AppTheme.colors.divider
        )
    }
}

@Composable
private fun CompactCardWood(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun CompactPrayerRowWood(
    prayerType: PrayerType,
    mode: PrayerNotificationMode,
    selectedAthanName: String?,
    isAthanDownloaded: Boolean,
    language: AppLanguage,
    onModeChange: (PrayerNotificationMode) -> Unit,
    onSelectAthan: () -> Unit
) {
    val prayerName = if (language == AppLanguage.ARABIC) prayerType.nameArabic else prayerType.nameEnglish

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = prayerName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.colors.darkGreen,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                modifier = Modifier.width(70.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PrayerNotificationMode.entries.forEach { notificationMode ->
                    val isSelected = mode == notificationMode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onModeChange(notificationMode) },
                        color = if (isSelected) AppTheme.colors.islamicGreen else AppTheme.colors.chipBackground,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = when (notificationMode) {
                                PrayerNotificationMode.ATHAN -> if (language == AppLanguage.ARABIC) "أذان" else "Athan"
                                PrayerNotificationMode.NOTIFICATION -> if (language == AppLanguage.ARABIC) "إشعار" else "Alert"
                                PrayerNotificationMode.SILENT -> if (language == AppLanguage.ARABIC) "صامت" else "Off"
                            },
                            fontSize = 10.sp,
                            color = if (isSelected) AppTheme.colors.textOnPrimary else AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (mode == PrayerNotificationMode.ATHAN) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppTheme.colors.chipBackground)
                    .clickable { onSelectAthan() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedAthanName ?: (if (language == AppLanguage.ARABIC) "اختر المؤذن" else "Select Muezzin"),
                    fontSize = 12.sp,
                    color = if (selectedAthanName != null) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAthanDownloaded) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppTheme.colors.islamicGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AppTheme.colors.iconDefault,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PerPrayerAthanSelectorWood(
    prayerType: PrayerType,
    selectedAthanName: String?,
    isAthanDownloaded: Boolean,
    language: AppLanguage,
    onSelectAthan: () -> Unit
) {
    val prayerName = if (language == AppLanguage.ARABIC) prayerType.nameArabic else prayerType.nameEnglish

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prayerName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colors.darkGreen,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
            modifier = Modifier.width(60.dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(AppTheme.colors.chipBackground)
                .clickable { onSelectAthan() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedAthanName ?: (if (language == AppLanguage.ARABIC) "اختر المؤذن" else "Select"),
                fontSize = 11.sp,
                color = if (selectedAthanName != null) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAthanDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AppTheme.colors.islamicGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppTheme.colors.iconDefault,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactToggleRowWood(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    language: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(24.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.colors.textOnPrimary,
                checkedTrackColor = AppTheme.colors.islamicGreen
            )
        )
    }
}

@Composable
private fun CompactDownloadedAthanRowWood(
    athan: Athan,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = athan.name,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row {
            IconButton(
                onClick = { if (isPlaying) onStop() else onPlay() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AppTheme.colors.islamicGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ============= Dialogs =============

@Composable
private fun IntervalSelectionDialogWood(
    currentInterval: ReminderInterval,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onIntervalSelected: (ReminderInterval) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.cardBackground,
        titleContentColor = AppTheme.colors.textPrimary,
        textContentColor = AppTheme.colors.textPrimary,
        title = {
            Text(
                text = if (language == AppLanguage.ARABIC) "فترة التذكير" else "Reminder Interval",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.islamicGreen
            )
        },
        text = {
            Column {
                ReminderInterval.entries.filter { it != ReminderInterval.OFF }.forEach { interval ->
                    val isSelected = interval == currentInterval
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AppTheme.colors.islamicGreen.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { onIntervalSelected(interval) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onIntervalSelected(interval) },
                            colors = RadioButtonDefaults.colors(selectedColor = AppTheme.colors.islamicGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = interval.getLabel(language),
                            fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AppTheme.colors.darkGreen else AppTheme.colors.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (language == AppLanguage.ARABIC) "إغلاق" else "Close",
                    color = AppTheme.colors.islamicGreen
                )
            }
        }
    )
}

@Composable
private fun QuietHoursDialogWood(
    startHour: Int,
    endHour: Int,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onHoursSelected: (Int, Int) -> Unit
) {
    var selectedStart by remember { mutableStateOf(startHour) }
    var selectedEnd by remember { mutableStateOf(endHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.cardBackground,
        titleContentColor = AppTheme.colors.textPrimary,
        textContentColor = AppTheme.colors.textPrimary,
        title = {
            Text(
                text = if (language == AppLanguage.ARABIC) "ساعات الهدوء" else "Quiet Hours",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.islamicGreen
            )
        },
        text = {
            Column {
                Text(
                    text = if (language == AppLanguage.ARABIC)
                        "لن يتم إرسال تذكيرات خلال هذه الفترة"
                    else
                        "No reminders will be sent during this period",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontSize = 14.sp,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = if (language == AppLanguage.ARABIC) "من:" else "From:",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.textPrimary
                )
                HourSelectorWood(
                    selectedHour = selectedStart,
                    onHourSelected = { selectedStart = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (language == AppLanguage.ARABIC) "إلى:" else "To:",
                    fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.textPrimary
                )
                HourSelectorWood(
                    selectedHour = selectedEnd,
                    onHourSelected = { selectedEnd = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onHoursSelected(selectedStart, selectedEnd) }) {
                Text(
                    if (language == AppLanguage.ARABIC) "حفظ" else "Save",
                    color = AppTheme.colors.islamicGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel",
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    )
}

@Composable
private fun HourSelectorWood(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit
) {
    val hourOptions = listOf(
        6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0, 1, 2, 3, 4, 5
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        hourOptions.forEach { hour ->
            val isSelected = hour == selectedHour
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onHourSelected(hour) },
                color = if (isSelected) AppTheme.colors.islamicGreen else AppTheme.colors.chipBackground
            ) {
                Text(
                    text = formatHour(hour),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AppTheme.colors.textOnPrimary else AppTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun ColorPickerDialogWood(
    title: String,
    currentColor: Color,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }

    val presetColors = listOf(
        Color.White, Color(0xFFFAF8F3), Color(0xFFF5F5DC), Color(0xFFE8D9C0),
        Color.Black, Color(0xFF333333), Color(0xFF666666), Color(0xFF999999),
        Color(0xFF8B5A2B), Color(0xFF5D3A1A), Color(0xFFA0784E), Color(0xFFD4A574),
        Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF4CAF50), Color(0xFF8BC34A),
        Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFF42A5F5), Color(0xFF64B5F6),
        Color(0xFFD32F2F), Color(0xFFC62828), Color(0xFFE57373), Color(0xFFFFCDD2)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.cardBackground,
        title = {
            Text(
                text = title,
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.islamicGreen
            )
        },
        text = {
            Column {
                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedColor)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color)
                                        .border(
                                            width = if (color == selectedColor) 3.dp else 1.dp,
                                            color = if (color == selectedColor) AppTheme.colors.islamicGreen else AppTheme.colors.border,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selectedColor); onDismiss() }) {
                Text(
                    if (language == AppLanguage.ARABIC) "اختيار" else "Select",
                    color = AppTheme.colors.islamicGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel",
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    )
}

@Composable
private fun CompactAthanSelectionDialogWood(
    prayerType: PrayerType,
    availableAthans: List<Athan>,
    downloadedAthanIds: Set<String>,
    currentAthanId: String?,
    isLoading: Boolean,
    downloadingAthanId: String?,
    previewingAthanId: String?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSelectAthan: (String) -> Unit,
    onDownloadAthan: (Athan) -> Unit,
    onPreviewAthan: (Athan) -> Unit,
    onStopPreview: () -> Unit
) {
    val prayerName = if (language == AppLanguage.ARABIC) prayerType.nameArabic else prayerType.nameEnglish

    AlertDialog(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        containerColor = AppTheme.colors.cardBackground,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = if (language == AppLanguage.ARABIC) "اختر أذان $prayerName" else "$prayerName Athan",
                fontFamily = if (language == AppLanguage.ARABIC) scheherazadeFont else null,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AppTheme.colors.darkGreen
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.islamicGreen, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableAthans) { athan ->
                        val isDownloaded = athan.id in downloadedAthanIds
                        val isSelected = athan.id == currentAthanId
                        val isDownloading = athan.id == downloadingAthanId
                        val isPreviewing = athan.id == previewingAthanId

                        CompactAthanListItemWood(
                            athan = athan,
                            isSelected = isSelected,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            isPreviewing = isPreviewing,
                            onSelect = {
                                if (isDownloaded) {
                                    onSelectAthan(athan.id)
                                } else {
                                    onDownloadAthan(athan)
                                }
                            },
                            onPreview = {
                                if (isPreviewing) onStopPreview() else onPreviewAthan(athan)
                            },
                            onDownload = { onDownloadAthan(athan) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onStopPreview()
                onDismiss()
            }) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "إغلاق" else "Close",
                    color = AppTheme.colors.islamicGreen,
                    fontSize = 13.sp
                )
            }
        }
    )
}

@Composable
private fun CompactAthanListItemWood(
    athan: Athan,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isPreviewing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = !isDownloading) { onSelect() },
        color = if (isSelected) AppTheme.colors.lightGreen.copy(alpha = 0.3f) else AppTheme.colors.chipBackground,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = athan.name,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) AppTheme.colors.darkGreen else AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = athan.location,
                    fontSize = 10.sp,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onPreview,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AppTheme.colors.islamicGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                when {
                    isDownloading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AppTheme.colors.islamicGreen,
                            strokeWidth = 2.dp
                        )
                    }
                    isDownloaded -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppTheme.colors.islamicGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    else -> {
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = AppTheme.colors.iconDefault,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = AppTheme.colors.darkGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return "$displayHour $amPm"
}
