package com.quranmedia.player.presentation.screens.hadith.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranmedia.player.data.database.entity.HadithEntity
import com.quranmedia.player.presentation.screens.reader.components.scheherazadeFont
import com.quranmedia.player.presentation.theme.AppTheme

@Composable
fun HadithCard(
    hadith: HadithEntity,
    modifier: Modifier = Modifier,
    isArabic: Boolean = true,
    showArabic: Boolean = true,
    showEnglish: Boolean = true
) {
    // Don't merge descendants — let TalkBack focus on each section independently
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isArabic) {
            ArabicSection(hadith, showArabic, isArabic)
            DividerSection(showArabic, showEnglish)
            EnglishSection(hadith, showEnglish, isArabic)
        } else {
            EnglishSection(hadith, showEnglish, isArabic)
            DividerSection(showArabic, showEnglish)
            ArabicSection(hadith, showArabic, isArabic)
        }
    }
}

@Composable
private fun ArabicSection(hadith: HadithEntity, show: Boolean, isArabic: Boolean) {
    if (show) {
        Text(
            text = hadith.textArabic,
            fontFamily = scheherazadeFont,
            fontSize = 24.sp,
            lineHeight = 44.sp,
            color = AppTheme.colors.textPrimary,
            style = LocalTextStyle.current.copy(
                textDirection = TextDirection.Rtl
            ),
            modifier = Modifier.semantics {
                this.contentDescription = if (isArabic) "النص العربي: ${hadith.textArabic}" else "Arabic text: ${hadith.textArabic}"
            }
        )
    }
}

@Composable
private fun EnglishSection(hadith: HadithEntity, show: Boolean, isArabic: Boolean) {
    if (show) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {
                this.contentDescription = buildString {
                    if (isArabic) append("الترجمة الإنجليزية: ") else append("English: ")
                    if (hadith.narratorEnglish.isNotEmpty()) append("${hadith.narratorEnglish}. ")
                    append(hadith.textEnglish)
                }
            }
        ) {
            if (hadith.narratorEnglish.isNotEmpty()) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(hadith.narratorEnglish)
                        }
                    },
                    fontSize = 14.sp,
                    color = AppTheme.colors.textSecondary
                )
            }
            Text(
                text = hadith.textEnglish,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = AppTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun DividerSection(showArabic: Boolean, showEnglish: Boolean) {
    if (showArabic && showEnglish) {
        HorizontalDivider(
            color = AppTheme.colors.divider,
            thickness = 0.5.dp
        )
    }
}
