package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.HadithBookEntity
import com.quranmedia.player.data.database.entity.HadithChapterEntity
import com.quranmedia.player.data.database.entity.HadithEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HadithDao {

    // Books
    @Query("SELECT * FROM hadith_books ORDER BY isBundled DESC, titleArabic")
    fun getAllBooks(): Flow<List<HadithBookEntity>>

    @Query("SELECT * FROM hadith_books WHERE id = :bookId")
    suspend fun getBook(bookId: String): HadithBookEntity?

    @Query("SELECT * FROM hadith_books WHERE isDownloaded = 1 OR isBundled = 1")
    fun getAvailableBooks(): Flow<List<HadithBookEntity>>

    @Query("SELECT COUNT(*) FROM hadith_books")
    suspend fun getBookCount(): Int

    // Chapters
    @Query("SELECT * FROM hadith_chapters WHERE bookId = :bookId ORDER BY chapterId")
    fun getChapters(bookId: String): Flow<List<HadithChapterEntity>>

    @Query("SELECT COUNT(*) FROM hadiths WHERE bookId = :bookId AND chapterId = :chapterId")
    suspend fun getHadithCountForChapter(bookId: String, chapterId: Int): Int

    // Hadiths
    @Query("SELECT * FROM hadiths WHERE bookId = :bookId AND chapterId = :chapterId ORDER BY idInBook")
    fun getHadithsByChapter(bookId: String, chapterId: Int): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE bookId = :bookId ORDER BY hadithId")
    fun getAllHadithsForBook(bookId: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE bookId = :bookId AND hadithId = :hadithId")
    suspend fun getHadith(bookId: String, hadithId: Int): HadithEntity?

    // Search
    @Query("SELECT * FROM hadiths WHERE textArabic LIKE '%' || :query || '%' OR textEnglish LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchHadiths(query: String): List<HadithEntity>

    @Query("SELECT * FROM hadiths WHERE bookId = :bookId AND (textArabic LIKE '%' || :query || '%' OR textEnglish LIKE '%' || :query || '%') LIMIT 100")
    suspend fun searchHadithsInBook(query: String, bookId: String): List<HadithEntity>

    // Inserts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<HadithBookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<HadithChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    // Cleanup for re-download
    @Query("DELETE FROM hadiths WHERE bookId = :bookId")
    suspend fun deleteHadithsForBook(bookId: String)

    @Query("DELETE FROM hadith_chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: String)

    @Query("UPDATE hadith_books SET isDownloaded = :downloaded, downloadedAt = :timestamp WHERE id = :bookId")
    suspend fun updateBookDownloadStatus(bookId: String, downloaded: Boolean, timestamp: Long?)
}
