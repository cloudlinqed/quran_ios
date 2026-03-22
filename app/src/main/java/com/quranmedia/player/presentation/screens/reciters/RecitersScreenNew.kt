package com.quranmedia.player.presentation.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.domain.model.Reciter
import com.quranmedia.player.presentation.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitersScreenNew(
    viewModel: RecitersViewModel = hiltViewModel(),
    onReciterClick: (Reciter) -> Unit,
    onBack: () -> Unit
) {
    val reciters by viewModel.reciters.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "القراء",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reciters",
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
        if (reciters.isEmpty()) {
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
                        text = "Loading reciters...",
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
                // Header card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = AppTheme.colors.islamicGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${reciters.size} Reciters Available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.darkGreen
                            )
                            Text(
                                text = "Select a reciter to browse surahs",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Reciters list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reciters) { reciter ->
                        ReciterItemNew(
                            reciter = reciter,
                            onClick = { onReciterClick(reciter) }
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
fun ReciterItemNew(
    reciter: Reciter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.lightGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = AppTheme.colors.islamicGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Reciter info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reciter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary
                )
                reciter.nameArabic?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 18.sp,
                        color = AppTheme.colors.islamicGreen,
                        lineHeight = 28.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reciter.style ?: "Murattal",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = AppTheme.colors.textSecondary
                )
            }

            // Arrow indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppTheme.colors.iconDefault,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
