package com.quranmedia.player.presentation.screens.hadith

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranmedia.player.data.repository.AppLanguage
import com.quranmedia.player.presentation.components.AccessibleIconButton
import com.quranmedia.player.presentation.components.DarkModeToggle
import com.quranmedia.player.presentation.screens.hadith.components.HadithCard
import com.quranmedia.player.presentation.screens.hadith.components.getBookAccentColor
import com.quranmedia.player.presentation.screens.hadith.components.getBookTextAccentColor
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme
import com.quranmedia.player.presentation.util.layoutDirection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HadithReaderScreen(
    onNavigateBack: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    viewModel: HadithReaderViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val book by viewModel.book.collectAsState()
    val chapter by viewModel.chapter.collectAsState()
    val hadiths by viewModel.hadiths.collectAsState()
    val language = settings.appLanguage
    val isArabic = language == AppLanguage.ARABIC
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accentColor = getBookAccentColor(viewModel.bookId)
    val textAccentColor = getBookTextAccentColor(viewModel.bookId)

    var showGoToDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = viewModel.initialHadithIndex,
        pageCount = { hadiths.size }
    )

    // Reset pager to first hadith when chapter changes
    val currentChapterId by viewModel.currentChapterId.collectAsState()
    LaunchedEffect(currentChapterId) {
        if (pagerState.currentPage != 0 && hadiths.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    // Announce page changes for TalkBack
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    LaunchedEffect(pagerState.currentPage, hadiths.size) {
        if (hadiths.isEmpty() || accessibilityManager?.isEnabled != true) return@LaunchedEffect
        val hadith = hadiths.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        val announcement = if (isArabic) {
            "حديث ${pagerState.currentPage + 1} من ${hadiths.size}، رقم ${hadith.idInBook}"
        } else {
            "Hadith ${pagerState.currentPage + 1} of ${hadiths.size}, number ${hadith.idInBook}"
        }
        val event = android.view.accessibility.AccessibilityEvent.obtain(
            android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
        )
        event.text.add(announcement)
        accessibilityManager.sendAccessibilityEvent(event)
    }

    CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isArabic) (book?.titleArabic ?: "") else (book?.titleEnglish ?: ""),
                                fontFamily = if (isArabic) scheherazadeFont else null,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isArabic) 18.sp else 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hadiths.isNotEmpty()) {
                                Text(
                                    text = "${pagerState.currentPage + 1}/${hadiths.size}",
                                    fontSize = 12.sp,
                                    color = AppTheme.colors.textOnHeader.copy(alpha = 0.7f)
                                )
                            }
                        }
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
                        if (hadiths.isNotEmpty()) {
                            // Copy button
                            AccessibleIconButton(
                                onClick = {
                                    val hadith = hadiths[pagerState.currentPage]
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Hadith", viewModel.getShareText(hadith)))
                                    Toast.makeText(context, if (isArabic) "تم النسخ" else "Copied", Toast.LENGTH_SHORT).show()
                                },
                                contentDescription = if (isArabic) "نسخ" else "Copy"
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            }
                            // Share button
                            AccessibleIconButton(
                                onClick = {
                                    val hadith = hadiths[pagerState.currentPage]
                                    val shareText = viewModel.getShareText(hadith)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                },
                                contentDescription = if (isArabic) "مشاركة" else "Share"
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
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
                val allChapters by viewModel.allChapters.collectAsState()

                if (hadiths.isNotEmpty()) {
                    Surface(
                        color = AppTheme.colors.cardBackground,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            // Hadith navigation row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AccessibleIconButton(
                                    onClick = {
                                        if (pagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        }
                                    },
                                    contentDescription = if (isArabic) "الحديث السابق" else "Previous hadith",
                                    enabled = pagerState.currentPage > 0
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.NavigateBefore,
                                        contentDescription = null,
                                        tint = if (pagerState.currentPage > 0) textAccentColor else AppTheme.colors.textSecondary
                                    )
                                }

                                TextButton(
                                    onClick = { showGoToDialog = true },
                                    modifier = Modifier.semantics {
                                        this.contentDescription = if (isArabic)
                                            "حديث ${pagerState.currentPage + 1} من ${hadiths.size}، اضغط للانتقال"
                                        else
                                            "Hadith ${pagerState.currentPage + 1} of ${hadiths.size}, tap to jump"
                                    }
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / ${hadiths.size}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = AppTheme.colors.textPrimary
                                    )
                                }

                                AccessibleIconButton(
                                    onClick = {
                                        if (pagerState.currentPage < hadiths.size - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        }
                                    },
                                    contentDescription = if (isArabic) "الحديث التالي" else "Next hadith",
                                    enabled = pagerState.currentPage < hadiths.size - 1
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.NavigateNext,
                                        contentDescription = null,
                                        tint = if (pagerState.currentPage < hadiths.size - 1) textAccentColor else AppTheme.colors.textSecondary
                                    )
                                }
                            }

                            // Chapter navigation row (prev chapter / current chapter / next chapter)
                            if (allChapters.size > 1) {
                                HorizontalDivider(color = AppTheme.colors.divider)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.goToPreviousChapter() },
                                        enabled = viewModel.hasPreviousChapter(),
                                        modifier = Modifier.semantics {
                                            this.contentDescription = if (isArabic) "الباب السابق" else "Previous chapter"
                                        }
                                    ) {
                                        Text(
                                            text = if (isArabic) "الباب السابق" else "Prev Chapter",
                                            fontSize = 11.sp,
                                            color = if (viewModel.hasPreviousChapter()) textAccentColor else AppTheme.colors.textSecondary
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.goToNextChapter() },
                                        enabled = viewModel.hasNextChapter(),
                                        modifier = Modifier.semantics {
                                            this.contentDescription = if (isArabic) "الباب التالي" else "Next chapter"
                                        }
                                    ) {
                                        Text(
                                            text = if (isArabic) "الباب التالي" else "Next Chapter",
                                            fontSize = 11.sp,
                                            color = if (viewModel.hasNextChapter()) textAccentColor else AppTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = AppTheme.colors.screenBackground
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Chapter name bar
                chapter?.let { ch ->
                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                this.contentDescription = if (isArabic)
                                    "الباب: ${ch.titleArabic}"
                                else
                                    "Chapter: ${ch.titleEnglish}"
                            }
                    ) {
                        Text(
                            text = if (isArabic) ch.titleArabic else ch.titleEnglish,
                            fontFamily = if (isArabic) scheherazadeFont else null,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isArabic) 16.sp else 14.sp,
                            color = textAccentColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // Hadith pager
                if (hadiths.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppTheme.colors.islamicGreen)
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val hadith = hadiths[page]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Hadith number indicator
                            Surface(
                                color = AppTheme.colors.cardBackground,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .semantics {
                                        this.contentDescription = if (isArabic)
                                            "حديث رقم ${hadith.idInBook}، ${pagerState.currentPage + 1} من ${hadiths.size}"
                                        else
                                            "Hadith number ${hadith.idInBook}, ${pagerState.currentPage + 1} of ${hadiths.size}"
                                        heading()
                                    }
                            ) {
                                Text(
                                    text = if (isArabic) "حديث رقم ${hadith.idInBook}" else "Hadith #${hadith.idInBook}",
                                    fontFamily = if (isArabic) scheherazadeFont else null,
                                    fontSize = 13.sp,
                                    color = AppTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            HadithCard(hadith = hadith, isArabic = isArabic)

                            // Per-hadith action row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Copy Arabic only
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Hadith", hadith.textArabic))
                                        Toast.makeText(context, if (isArabic) "تم نسخ النص العربي" else "Arabic copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.semantics {
                                        this.contentDescription = if (isArabic) "نسخ النص العربي" else "Copy Arabic text"
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "نسخ العربي" else "Copy Arabic",
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                // Share full
                                TextButton(
                                    onClick = {
                                        val shareText = viewModel.getShareText(hadith)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    modifier = Modifier.semantics {
                                        this.contentDescription = if (isArabic) "مشاركة الحديث" else "Share hadith"
                                    }
                                ) {
                                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "مشاركة" else "Share",
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Go to hadith number dialog
    if (showGoToDialog && hadiths.isNotEmpty()) {
        var inputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGoToDialog = false },
            title = {
                Text(
                    text = if (isArabic) "انتقل إلى حديث" else "Go to Hadith",
                    fontFamily = if (isArabic) scheherazadeFont else null
                )
            },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.filter { c -> c.isDigit() } },
                    label = {
                        Text(
                            text = if (isArabic) "رقم الحديث (1-${hadiths.size})" else "Hadith # (1-${hadiths.size})",
                            fontFamily = if (isArabic) scheherazadeFont else null
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val num = inputText.toIntOrNull()
                    if (num != null && num in 1..hadiths.size) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(num - 1)
                        }
                    }
                    showGoToDialog = false
                }) {
                    Text(if (isArabic) "انتقال" else "Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
