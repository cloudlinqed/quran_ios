package com.quranmedia.player.data.database.entity

import androidx.room.Entity

@Entity(
    tableName = "hadith_chapters",
    primaryKeys = ["bookId", "chapterId"]
)
data class HadithChapterEntity(
    val bookId: String,
    val chapterId: Int,
    val titleArabic: String,
    val titleEnglish: String
)
