package com.quranmedia.player.presentation.screens.surahs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.domain.model.RevelationType
import com.quranmedia.player.domain.model.Surah
import com.quranmedia.player.presentation.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahsScreenNew(
    viewModel: SurahsViewModel = hiltViewModel(),
    onSurahClick: (reciterId: String, surah: Surah) -> Unit,
    onBack: () -> Unit
) {
    val surahs by viewModel.surahs.collectAsState()
    val reciters by viewModel.reciters.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    var reciterDropdownExpanded by remember { mutableStateOf(false) }

    // Auto-select first reciter if none selected
    LaunchedEffect(reciters, selectedReciter) {
        if (selectedReciter == null && reciters.isNotEmpty()) {
            viewModel.selectReciter(reciters[0])
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "السور",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Surahs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = AppTheme.colors.textOnHeader
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.topBarBackground,
                    titleContentColor = AppTheme.colors.textOnHeader,
                    navigationIconContentColor = AppTheme.colors.textOnHeader
                )
            )
        },
        containerColor = AppTheme.colors.screenBackground
    ) { paddingValues ->
        if (surahs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.islamicGreen)
                    Text(
                        text = "Loading surahs...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Reciter Selector Dropdown
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.islamicGreen.copy(alpha = 0.1f)
                    )
                ) {
                    ExposedDropdownMenuBox(
                        expanded = reciterDropdownExpanded,
                        onExpandedChange = { reciterDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedReciter?.name ?: "Select Reciter",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Column {
                                    Text("القارئ", fontSize = 12.sp, color = AppTheme.colors.islamicGreen)
                                    Text("Reciter", fontSize = 10.sp, color = AppTheme.colors.darkGreen)
                                }
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Reciter",
                                    tint = AppTheme.colors.islamicGreen
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colors.islamicGreen,
                                unfocusedBorderColor = AppTheme.colors.islamicGreen.copy(alpha = 0.5f),
                                focusedLabelColor = AppTheme.colors.islamicGreen,
                                unfocusedLabelColor = AppTheme.colors.darkGreen,
                                focusedTextColor = AppTheme.colors.darkGreen,
                                unfocusedTextColor = AppTheme.colors.textPrimary,
                                cursorColor = AppTheme.colors.islamicGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .padding(16.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = reciterDropdownExpanded,
                            onDismissRequest = { reciterDropdownExpanded = false },
                            modifier = Modifier.background(AppTheme.colors.cardBackground)
                        ) {
                            reciters.forEach { reciter ->
                                val isSelected = reciter.id == selectedReciter?.id
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = reciter.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 15.sp,
                                                color = if (isSelected) AppTheme.colors.textOnPrimary else AppTheme.colors.darkGreen
                                            )
                                            reciter.nameArabic?.let { arabicName ->
                                                Text(
                                                    text = arabicName,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) AppTheme.colors.lightGreen.copy(alpha = 0.9f) else AppTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectReciter(reciter)
                                        reciterDropdownExpanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = AppTheme.colors.darkGreen,
                                        leadingIconColor = AppTheme.colors.darkGreen,
                                        disabledTextColor = AppTheme.colors.textSecondary
                                    ),
                                    modifier = Modifier.background(
                                        if (isSelected) AppTheme.colors.islamicGreen else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }

                // Header card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.lightGreen.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = AppTheme.colors.islamicGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "114 Surahs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.darkGreen
                            )
                            Text(
                                text = "Select a surah to begin listening",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Surahs list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(surahs) { surah ->
                        SurahItemNew(
                            surah = surah,
                            onClick = {
                                // Only allow clicking if a reciter is selected
                                selectedReciter?.let { reciter ->
                                    onSurahClick(reciter.id, surah)
                                }
                            },
                            isEnabled = selectedReciter != null
                        )
                    }
                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SurahItemNew(
    surah: Surah,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) AppTheme.colors.cardBackground else AppTheme.colors.cardBackground.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah number circle - smaller
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (surah.number == 1) AppTheme.colors.goldAccent.copy(alpha = 0.2f)
                        else AppTheme.colors.lightGreen.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (surah.number == 1) Color(0xFFF57C00) else AppTheme.colors.islamicGreen
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Surah info - compact
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = "${when (surah.revelationType) {
                        RevelationType.MECCAN -> "Meccan"
                        RevelationType.MEDINAN -> "Medinan"
                    }} • ${surah.ayahCount} ayahs",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = AppTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Arabic name - smaller
            Text(
                text = surah.nameArabic,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.islamicGreen
            )
        }
    }
}
