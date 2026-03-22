package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hadith_books")
data class HadithBookEntity(
    @PrimaryKey val id: String,
    val titleArabic: String,
    val titleEnglish: String,
    val authorArabic: String,
    val authorEnglish: String,
    val hadithCount: Int,
    val isBundled: Boolean,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null
)
