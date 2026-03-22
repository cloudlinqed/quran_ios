package com.quranmedia.player.presentation.screens.hadith

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.components.AccessibleIconButton
import com.quranmedia.player.presentation.components.BottomNavBar
import com.quranmedia.player.presentation.components.DarkModeToggle
// CommonOverflowMenu removed — overflow moved to bottom nav
import com.quranmedia.player.presentation.screens.hadith.components.BookCoverCard
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.util.layoutDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithLibraryScreen(
    onNavigateBack: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    onNavigateToBook: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToReading: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToQibla: () -> Unit = {},
    onNavigateToAthkar: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToImsakiya: () -> Unit = {},
    onNavigateByRoute: (String) -> Unit = {},
    viewModel: HadithLibraryViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val books by viewModel.books.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()
    val language = settings.appLanguage
    val isArabic = language == AppLanguage.ARABIC

    val myLibrary = viewModel.getMyLibraryBooks(books)
    val downloadable = viewModel.getDownloadableBooks(books)

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isArabic) "المكتبة الحديثية" else "Hadith Library",
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isArabic) 20.sp else 18.sp
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
                            onClick = { onNavigateToSearch(null) },
                            contentDescription = if (isArabic) "بحث في الأحاديث" else "Search Hadiths"
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
                    currentRoute = "hadithLibrary",
                    language = language,
                    onNavigate = onNavigateByRoute
                )
            },
            containerColor = AppTheme.colors.screenBackground
        ) { padding ->
            if (!isInitialized) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .semantics {
                            this.contentDescription = if (isArabic) "جاري تحميل المكتبة" else "Loading library"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.islamicGreen)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // My Library header
                    if (myLibrary.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = if (isArabic) "مكتبتي" else "My Library",
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isArabic) 20.sp else 16.sp,
                                color = AppTheme.colors.goldAccent,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .semantics { heading() }
                            )
                        }

                        items(myLibrary, key = { it.id }) { book ->
                            BookCoverCard(
                                book = book,
                                onClick = { onNavigateToBook(book.id) }
                            )
                        }
                    }

                    // Available for Download header
                    if (downloadable.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = if (isArabic) "متاح للتحميل" else "Available for Download",
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isArabic) 20.sp else 16.sp,
                                color = AppTheme.colors.goldAccent,
                                modifier = Modifier
                                    .padding(top = 16.dp, bottom = 8.dp)
                                    .semantics { heading() }
                            )
                        }

                        items(downloadable, key = { it.id }) { book ->
                            BookCoverCard(
                                book = book,
                                onClick = { onNavigateToBook(book.id) },
                                isDownloadable = true,
                                downloadProgress = downloadProgress,
                                onDownloadClick = { viewModel.downloadBook(book.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
