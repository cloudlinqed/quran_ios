package com.quranmedia.player.presentation.screens.hadith

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.database.entity.HadithEntity
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.data.repository.HadithRepositoryImpl
import com.quranmedia.player.presentation.components.AccessibleIconButton
import com.quranmedia.player.presentation.components.DarkModeToggle
import com.quranmedia.player.presentation.screens.hadith.components.getBookTextAccentColor
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.util.layoutDirection
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithSearchScreen(
    bookId: String?,
    onNavigateBack: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    onNavigateToReader: (String, Int, Int) -> Unit,
    viewModel: HadithSearchViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val language = settings.appLanguage
    val isArabic = language == AppLanguage.ARABIC

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(bookId) {
        viewModel.setBookScope(bookId)
    }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = viewModel.query.collectAsState().value,
                            onValueChange = { viewModel.onQueryChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    text = if (isArabic) "بحث في الأحاديث..." else "Search hadiths...",
                                    fontFamily = if (isArabic) scheherazadeFont else null,
                                    color = AppTheme.colors.textOnHeader.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AppTheme.colors.textOnHeader,
                                unfocusedTextColor = AppTheme.colors.textOnHeader,
                                cursorColor = AppTheme.colors.textOnHeader,
                                focusedBorderColor = AppTheme.colors.textOnHeader.copy(alpha = 0.5f),
                                unfocusedBorderColor = AppTheme.colors.textOnHeader.copy(alpha = 0.3f)
                            ),
                            trailingIcon = {
                                if (viewModel.query.collectAsState().value.isNotEmpty()) {
                                    AccessibleIconButton(
                                        onClick = { viewModel.onQueryChanged("") },
                                        contentDescription = if (isArabic) "مسح" else "Clear"
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = null,
                                            tint = AppTheme.colors.textOnHeader
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppTheme.colors.topBarBackground,
                        titleContentColor = AppTheme.colors.goldAccent,
                        navigationIconContentColor = AppTheme.colors.goldAccent,
                        actionIconContentColor = AppTheme.colors.goldAccent
                    )
                )
            },
            containerColor = AppTheme.colors.screenBackground
        ) { padding ->
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.islamicGreen)
                }
            } else if (results.isEmpty() && viewModel.query.collectAsState().value.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "لا توجد نتائج" else "No results found",
                        fontFamily = if (isArabic) scheherazadeFont else null,
                        color = AppTheme.colors.textSecondary,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Group results by book
                val grouped = results.groupBy { it.bookId }
                val context = LocalContext.current

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Result count
                    item {
                        Text(
                            text = if (isArabic) "${results.size} نتيجة" else "${results.size} results",
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            fontSize = 12.sp,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    grouped.forEach { (resultBookId, hadithList) ->
                        val bookInfo = HadithRepositoryImpl.BOOK_REGISTRY.find { it.id == resultBookId }
                        val bookTitle = if (isArabic) bookInfo?.titleArabic ?: resultBookId else bookInfo?.titleEnglish ?: resultBookId

                        item {
                            Text(
                                text = bookTitle,
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isArabic) 16.sp else 14.sp,
                                color = getBookTextAccentColor(resultBookId),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(hadithList, key = { "${it.bookId}_${it.hadithId}" }) { hadith ->
                            SearchResultItem(
                                hadith = hadith,
                                isArabic = isArabic,
                                onClick = {
                                    onNavigateToReader(hadith.bookId, hadith.chapterId, 0)
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Hadith", hadith.textArabic))
                                    Toast.makeText(context, if (isArabic) "تم النسخ" else "Copied", Toast.LENGTH_SHORT).show()
                                },
                                onShare = {
                                    val shareText = buildString {
                                        append(hadith.textArabic)
                                        if (hadith.textEnglish.isNotEmpty()) {
                                            append("\n\n")
                                            if (hadith.narratorEnglish.isNotEmpty()) append("${hadith.narratorEnglish}\n")
                                            append(hadith.textEnglish)
                                        }
                                        append("\n\n— ${bookInfo?.titleEnglish ?: resultBookId}, Hadith ${hadith.idInBook}")
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    hadith: HadithEntity,
    isArabic: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    val resultDescription = if (isArabic) {
        "حديث رقم ${hadith.idInBook}. ${hadith.textArabic.take(80)}"
    } else {
        "Hadith ${hadith.idInBook}. ${hadith.textEnglish.take(80)}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .semantics { this.contentDescription = resultDescription }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Hadith number
            Text(
                text = if (isArabic) "حديث ${hadith.idInBook}" else "Hadith #${hadith.idInBook}",
                fontFamily = if (isArabic) scheherazadeFont else null,
                fontSize = 12.sp,
                color = AppTheme.colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Arabic preview
            Text(
                text = hadith.textArabic,
                fontFamily = scheherazadeFont,
                fontSize = 15.sp,
                color = AppTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // English preview
            if (hadith.textEnglish.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hadith.textEnglish,
                    fontSize = 13.sp,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Copy/Share actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { this.contentDescription = if (isArabic) "نسخ الحديث" else "Copy hadith" }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                        tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { this.contentDescription = if (isArabic) "مشاركة الحديث" else "Share hadith" }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null,
                        tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
