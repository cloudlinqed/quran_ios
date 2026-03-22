package com.quranmedia.player.presentation.screens.hadith.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranmedia.player.data.database.entity.HadithBookEntity
import com.quranmedia.player.domain.repository.HadithDownloadProgress
import com.quranmedia.player.domain.repository.HadithDownloadState
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme

// Book cover colors from Stitch Hadith design
private val BookBlue = Color(0xFF233B65)
private val BookBrown = Color(0xFF6D4321)
private val BookGreen = Color(0xFF265338)
private val BookTeal = Color(0xFF287A76)
private val BadgeBg = Color(0xFFE6CE93)
private val BrandGold = Color(0xFFCDAD70)

/**
 * Book accent color mapping — 4 rotating colors from Stitch design.
 */
fun getBookAccentColor(bookId: String): Color {
    val colors = listOf(BookBlue, BookBrown, BookGreen, BookTeal)
    return colors[bookId.hashCode().and(0x7FFFFFFF) % colors.size]
}

/**
 * Lighter accent color for use as text/icon tint on dark backgrounds.
 * In light mode returns the same dark accent; in dark mode returns a bright variant.
 */
@Composable
fun getBookTextAccentColor(bookId: String): Color {
    if (!isSystemInDarkTheme()) return getBookAccentColor(bookId)
    return when (bookId) {
        "bukhari" -> Color(0xFF81C784)            // light green
        "muslim" -> Color(0xFF64B5F6)             // light blue
        "nawawi40" -> Color(0xFFBCAAA4)           // light brown
        "riyad_assalihin" -> Color(0xFF9FA8DA)    // light indigo
        "tirmidhi" -> Color(0xFFF48FB1)           // light pink
        "abudawud" -> Color(0xFF80CBC4)           // light teal
        "nasai" -> Color(0xFFB39DDB)              // light purple
        "ibnmajah" -> Color(0xFFFFAB91)           // light orange
        "malik" -> Color(0xFFA5D6A7)              // light green
        "ahmed" -> Color(0xFF81D4FA)              // light blue
        "darimi" -> Color(0xFFBCAAA4)             // light brown
        "qudsi40" -> Color(0xFF80DEEA)            // light cyan
        "bulugh_almaram" -> Color(0xFFB0BEC5)     // light blue grey
        "aladab_almufrad" -> Color(0xFF9FA8DA)    // light indigo
        "mishkat_almasabih" -> Color(0xFFBCAAA4)  // light brown
        "shamail_muhammadiyah" -> Color(0xFFF48FB1) // light pink
        else -> Color(0xFFB0BEC5)                 // light blue grey
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCoverCard(
    book: HadithBookEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloadable: Boolean = false,
    downloadProgress: HadithDownloadProgress? = null,
    onDownloadClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val bookColor = getBookAccentColor(book.id)
    val isDownloading = downloadProgress?.bookId == book.id &&
        (downloadProgress.state == HadithDownloadState.DOWNLOADING || downloadProgress.state == HadithDownloadState.PARSING)

    val description = buildString {
        append(book.titleArabic)
        append(", ${book.titleEnglish}. ${book.authorArabic}. ")
        append("${book.hadithCount} hadiths")
        if (isDownloadable && !book.isDownloaded && !book.isBundled) {
            if (isDownloading) append(". Downloading ${((downloadProgress?.progress ?: 0f) * 100).toInt()}%")
            else append(". Tap to download")
        }
    }

    // Stitch design: solid color bg, 1.5dp padding, inner gold border, gold text
    Card(
        onClick = {
            if (!isDownloadable || book.isDownloaded || book.isBundled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            } else {
                onDownloadClick?.invoke()
            }
        },
        modifier = modifier
            .aspectRatio(0.75f)
            .semantics {
                this.contentDescription = description
                this.role = Role.Button
            },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = bookColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Outer padding (book edge)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
        ) {
            // Inner bordered area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, BrandGold, RoundedCornerShape(4.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Book title — gold, bold, centered
                    Text(
                        text = book.titleArabic,
                        color = BrandGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = scheherazadeFont,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.weight(1f))

                    // Author — gold bordered pill at bottom
                    if (isDownloadable && !book.isDownloaded && !book.isBundled) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { downloadProgress?.progress ?: 0f },
                                modifier = Modifier.size(20.dp),
                                color = BrandGold,
                                trackColor = BrandGold.copy(alpha = 0.3f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = "تحميل ${book.titleArabic}",
                                tint = BrandGold.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .border(0.5.dp, BrandGold, RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = book.authorArabic,
                                color = BrandGold,
                                fontSize = 7.sp,
                                fontFamily = scheherazadeFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))
                }

                // Hadith count badge — bottom-right corner
                if (!isDownloadable || book.isDownloaded || book.isBundled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(BadgeBg, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${book.hadithCount}",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
