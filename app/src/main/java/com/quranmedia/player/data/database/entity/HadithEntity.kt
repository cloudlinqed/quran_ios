package com.quranmedia.player.data.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "hadiths",
    primaryKeys = ["bookId", "hadithId"],
    indices = [Index("bookId", "chapterId")]
)
data class HadithEntity(
    val bookId: String,
    val hadithId: Int,
    val idInBook: Int,
    val chapterId: Int,
    val textArabic: String,
    val narratorEnglish: String,
    val textEnglish: String
)
