package com.quranmedia.player.presentation.screens.hadith

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.components.AccessibleIconButton
import com.quranmedia.player.presentation.components.BottomNavBar
import com.quranmedia.player.presentation.components.DarkModeToggle
import com.quranmedia.player.presentation.screens.hadith.components.getBookAccentColor
import com.quranmedia.player.presentation.screens.hadith.components.getBookTextAccentColor
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.util.layoutDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithBookScreen(
    onNavigateBack: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    onNavigateToReader: (String, Int, Int) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateByRoute: (String) -> Unit = {},
    viewModel: HadithBookViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val book by viewModel.book.collectAsState()
    val chaptersWithCounts by viewModel.chaptersWithCounts.collectAsState()
    val language = settings.appLanguage
    val isArabic = language == AppLanguage.ARABIC
    val bookId = viewModel.bookId
    val accentColor = getBookAccentColor(bookId)
    val textAccentColor = getBookTextAccentColor(bookId)

    var expandedChapterId by remember { mutableStateOf<Int?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isArabic) (book?.titleArabic ?: "") else (book?.titleEnglish ?: ""),
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isArabic) 20.sp else 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        AccessibleIconButton(
                            onClick = onNavigateBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        DarkModeToggle(language = language, onToggle = { onToggleDarkMode() })
                        AccessibleIconButton(
                            onClick = { onNavigateToSearch(bookId) },
                            contentDescription = if (isArabic) "بحث في الكتاب" else "Search in Book"
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
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
                    currentRoute = "hadithBook",
                    language = language,
                    onNavigate = onNavigateByRoute
                )
            },
            containerColor = AppTheme.colors.screenBackground
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Subtitle bar
                item {
                    book?.let { b ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) b.authorArabic else b.authorEnglish,
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontSize = 14.sp,
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = "  •  ",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isArabic) "${b.hadithCount} حديث" else "${b.hadithCount} hadiths",
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontSize = 14.sp,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                        HorizontalDivider(color = AppTheme.colors.divider)
                    }
                }

                items(chaptersWithCounts, key = { it.chapter.chapterId }) { chapterWithCount ->
                    val chapter = chapterWithCount.chapter
                    val count = chapterWithCount.hadithCount
                    val isExpanded = expandedChapterId == chapter.chapterId

                    val expandState = if (isExpanded) {
                        if (isArabic) "مفتوح" else "Expanded"
                    } else {
                        if (isArabic) "مغلق" else "Collapsed"
                    }

                    val chapterDescription = if (isArabic) {
                        "باب ${chapter.titleArabic}, $count أحاديث"
                    } else {
                        "Chapter ${chapter.chapterId}: ${chapter.titleEnglish}, $count hadiths"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .animateContentSize()
                            .semantics {
                                this.contentDescription = chapterDescription
                                this.stateDescription = expandState
                                this.role = Role.Button
                            }
                            .clickable {
                                expandedChapterId = if (isExpanded) null else chapter.chapterId
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chapter number circle
                                Surface(
                                    shape = CircleShape,
                                    color = accentColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${chapter.chapterId}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textAccentColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Chapter title
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) chapter.titleArabic else chapter.titleEnglish,
                                        fontFamily = if (isArabic) scheherazadeFont else null,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = if (isArabic) 16.sp else 14.sp,
                                        color = AppTheme.colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!isArabic && chapter.titleArabic.isNotEmpty()) {
                                        Text(
                                            text = chapter.titleArabic,
                                            fontFamily = scheherazadeFont,
                                            fontSize = 13.sp,
                                            color = AppTheme.colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Hadith count
                                Text(
                                    text = "($count)",
                                    fontSize = 13.sp,
                                    color = AppTheme.colors.textSecondary
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                // Expand icon
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) {
                                        if (isArabic) "طي" else "Collapse"
                                    } else {
                                        if (isArabic) "توسيع" else "Expand"
                                    },
                                    tint = AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Expanded: show read + search actions
                            if (isExpanded) {
                                HorizontalDivider(
                                    color = AppTheme.colors.divider,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(
                                        onClick = {
                                            onNavigateToReader(bookId, chapter.chapterId, 0)
                                        }
                                    ) {
                                        Text(
                                            text = if (isArabic) "قراءة الأحاديث" else "Read Hadiths",
                                            fontFamily = if (isArabic) scheherazadeFont else null,
                                            color = textAccentColor,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                    TextButton(
                                        onClick = { onNavigateToSearch(bookId) }
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = textAccentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isArabic) "بحث" else "Search",
                                            fontFamily = if (isArabic) scheherazadeFont else null,
                                            color = textAccentColor,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
