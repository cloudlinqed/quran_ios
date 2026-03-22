package com.quranmedia.player.domain.repository

import com.quranmedia.player.data.database.entity.HadithBookEntity
import com.quranmedia.player.data.database.entity.HadithChapterEntity
import com.quranmedia.player.data.database.entity.HadithEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class HadithDownloadProgress(
    val state: HadithDownloadState = HadithDownloadState.IDLE,
    val progress: Float = 0f,
    val bookId: String? = null,
    val errorMessage: String? = null
)

enum class HadithDownloadState {
    IDLE, DOWNLOADING, PARSING, COMPLETED, ERROR
}

interface HadithRepository {
    fun getAllBooks(): Flow<List<HadithBookEntity>>
    fun getAvailableBooks(): Flow<List<HadithBookEntity>>
    suspend fun getBook(bookId: String): HadithBookEntity?

    fun getChapters(bookId: String): Flow<List<HadithChapterEntity>>
    suspend fun getHadithCountForChapter(bookId: String, chapterId: Int): Int

    fun getHadithsByChapter(bookId: String, chapterId: Int): Flow<List<HadithEntity>>
    fun getAllHadithsForBook(bookId: String): Flow<List<HadithEntity>>
    suspend fun getHadith(bookId: String, hadithId: Int): HadithEntity?

    suspend fun searchHadiths(query: String): List<HadithEntity>
    suspend fun searchHadithsInBook(query: String, bookId: String): List<HadithEntity>

    suspend fun initializeBundledBooks()
    suspend fun downloadBook(bookId: String)

    val downloadProgress: StateFlow<HadithDownloadProgress>
}
