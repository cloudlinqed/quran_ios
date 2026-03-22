package com.quranmedia.player.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.quranmedia.player.data.database.dao.HadithDao
import com.quranmedia.player.data.database.entity.HadithBookEntity
import com.quranmedia.player.data.database.entity.HadithChapterEntity
import com.quranmedia.player.data.database.entity.HadithEntity
import com.quranmedia.player.domain.repository.HadithDownloadProgress
import com.quranmedia.player.domain.repository.HadithDownloadState
import com.quranmedia.player.domain.repository.HadithRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HadithRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hadithDao: HadithDao,
    private val gson: Gson,
    private val okHttpClient: OkHttpClient
) : HadithRepository {

    companion object {
        private const val BATCH_SIZE = 500
        private const val BASE_DOWNLOAD_URL = "https://raw.githubusercontent.com/AhmedBaset/hadith-json/main/db/by_book"

        // Book registry: all 17 books with metadata
        val BOOK_REGISTRY = listOf(
            BookInfo("bukhari", "صحيح البخاري", "Sahih al-Bukhari", "الإمام البخاري", "Imam Bukhari", 7563, true, "the_9_books"),
            BookInfo("muslim", "صحيح مسلم", "Sahih Muslim", "الإمام مسلم", "Imam Muslim", 3033, false, "the_9_books"),
            BookInfo("tirmidhi", "جامع الترمذي", "Jami at-Tirmidhi", "الإمام الترمذي", "Imam Tirmidhi", 3956, false, "the_9_books"),
            BookInfo("abudawud", "سنن أبي داود", "Sunan Abi Dawud", "الإمام أبو داود", "Imam Abu Dawud", 5274, false, "the_9_books"),
            BookInfo("nasai", "سنن النسائي", "Sunan an-Nasai", "الإمام النسائي", "Imam an-Nasai", 5758, false, "the_9_books"),
            BookInfo("ibnmajah", "سنن ابن ماجه", "Sunan Ibn Majah", "الإمام ابن ماجه", "Imam Ibn Majah", 4341, false, "the_9_books"),
            BookInfo("malik", "موطأ مالك", "Muwatta Malik", "الإمام مالك", "Imam Malik", 1832, false, "the_9_books"),
            BookInfo("ahmed", "مسند أحمد", "Musnad Ahmad", "الإمام أحمد", "Imam Ahmad", 4305, false, "the_9_books"),
            BookInfo("darimi", "سنن الدارمي", "Sunan ad-Darimi", "الإمام الدارمي", "Imam ad-Darimi", 3367, false, "the_9_books"),
            BookInfo("nawawi40", "الأربعون النووية", "40 Hadith Nawawi", "الإمام النووي", "Imam Nawawi", 42, true, "forties"),
            BookInfo("qudsi40", "الأحاديث القدسية", "40 Hadith Qudsi", "مجموعة علماء", "Various Scholars", 40, false, "forties"),
            BookInfo("riyad_assalihin", "رياض الصالحين", "Riyad as-Salihin", "الإمام النووي", "Imam Nawawi", 1896, true, "other_books"),
            BookInfo("bulugh_almaram", "بلوغ المرام", "Bulugh al-Maram", "ابن حجر العسقلاني", "Ibn Hajar al-Asqalani", 1358, false, "other_books"),
            BookInfo("aladab_almufrad", "الأدب المفرد", "Al-Adab Al-Mufrad", "الإمام البخاري", "Imam Bukhari", 1322, false, "other_books"),
            BookInfo("mishkat_almasabih", "مشكاة المصابيح", "Mishkat al-Masabih", "الخطيب التبريزي", "al-Khatib at-Tabrizi", 6285, false, "other_books"),
            BookInfo("shamail_muhammadiyah", "الشمائل المحمدية", "Shamail Muhammadiyah", "الإمام الترمذي", "Imam Tirmidhi", 399, false, "other_books")
        )
    }

    data class BookInfo(
        val id: String,
        val titleArabic: String,
        val titleEnglish: String,
        val authorArabic: String,
        val authorEnglish: String,
        val hadithCount: Int,
        val isBundled: Boolean,
        val folder: String
    ) {
        fun toEntity(isDownloaded: Boolean = false) = HadithBookEntity(
            id = id,
            titleArabic = titleArabic,
            titleEnglish = titleEnglish,
            authorArabic = authorArabic,
            authorEnglish = authorEnglish,
            hadithCount = hadithCount,
            isBundled = isBundled,
            isDownloaded = isDownloaded
        )

        val downloadUrl: String
            get() = "$BASE_DOWNLOAD_URL/$folder/$id.json"
    }

    private val _downloadProgress = MutableStateFlow(HadithDownloadProgress())
    override val downloadProgress: StateFlow<HadithDownloadProgress> = _downloadProgress.asStateFlow()

    override fun getAllBooks(): Flow<List<HadithBookEntity>> = hadithDao.getAllBooks()
    override fun getAvailableBooks(): Flow<List<HadithBookEntity>> = hadithDao.getAvailableBooks()
    override suspend fun getBook(bookId: String): HadithBookEntity? = hadithDao.getBook(bookId)

    override fun getChapters(bookId: String): Flow<List<HadithChapterEntity>> = hadithDao.getChapters(bookId)
    override suspend fun getHadithCountForChapter(bookId: String, chapterId: Int): Int =
        hadithDao.getHadithCountForChapter(bookId, chapterId)

    override fun getHadithsByChapter(bookId: String, chapterId: Int): Flow<List<HadithEntity>> =
        hadithDao.getHadithsByChapter(bookId, chapterId)

    override fun getAllHadithsForBook(bookId: String): Flow<List<HadithEntity>> =
        hadithDao.getAllHadithsForBook(bookId)

    override suspend fun getHadith(bookId: String, hadithId: Int): HadithEntity? =
        hadithDao.getHadith(bookId, hadithId)

    override suspend fun searchHadiths(query: String): List<HadithEntity> {
        return try {
            hadithDao.searchHadiths(query)
        } catch (e: Exception) {
            Timber.e(e, "FTS search failed for query: $query")
            emptyList()
        }
    }

    override suspend fun searchHadithsInBook(query: String, bookId: String): List<HadithEntity> {
        return try {
            hadithDao.searchHadithsInBook(query, bookId)
        } catch (e: Exception) {
            Timber.e(e, "FTS search failed for query: $query in book: $bookId")
            emptyList()
        }
    }

    override suspend fun initializeBundledBooks() = withContext(Dispatchers.IO) {
        try {
            // Check if books are already registered
            if (hadithDao.getBookCount() > 0) return@withContext

            // Register all books in the catalog
            val bookEntities = BOOK_REGISTRY.map { it.toEntity() }
            hadithDao.insertBooks(bookEntities)

            // Load bundled books from assets
            val bundledBooks = BOOK_REGISTRY.filter { it.isBundled }
            for (bookInfo in bundledBooks) {
                loadBookFromAsset(bookInfo)
            }

            Timber.d("Initialized ${bundledBooks.size} bundled hadith books")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize bundled hadith books")
        }
    }

    override suspend fun downloadBook(bookId: String) = withContext(Dispatchers.IO) {
        val bookInfo = BOOK_REGISTRY.find { it.id == bookId } ?: return@withContext
        _downloadProgress.value = HadithDownloadProgress(
            state = HadithDownloadState.DOWNLOADING,
            progress = 0f,
            bookId = bookId
        )

        try {
            // Download JSON to temp file
            val hadithDir = File(context.filesDir, "hadith")
            hadithDir.mkdirs()
            val tempFile = File(hadithDir, "${bookId}_temp.json")
            val targetFile = File(hadithDir, "${bookId}.json")

            val request = Request.Builder().url(bookInfo.downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Download failed: ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty response")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes) * 0.7f
                        } else 0.5f
                        _downloadProgress.value = HadithDownloadProgress(
                            state = HadithDownloadState.DOWNLOADING,
                            progress = progress,
                            bookId = bookId
                        )
                    }
                }
            }

            tempFile.renameTo(targetFile)

            // Parse and insert into DB
            _downloadProgress.value = HadithDownloadProgress(
                state = HadithDownloadState.PARSING,
                progress = 0.75f,
                bookId = bookId
            )

            loadBookFromFile(bookInfo, targetFile)

            // Mark as downloaded
            hadithDao.updateBookDownloadStatus(bookId, true, System.currentTimeMillis())

            _downloadProgress.value = HadithDownloadProgress(
                state = HadithDownloadState.COMPLETED,
                progress = 1f,
                bookId = bookId
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to download book: $bookId")
            _downloadProgress.value = HadithDownloadProgress(
                state = HadithDownloadState.ERROR,
                progress = 0f,
                bookId = bookId,
                errorMessage = e.message
            )
        }
    }

    private suspend fun loadBookFromAsset(bookInfo: BookInfo) {
        try {
            val inputStream = context.assets.open("hadith/${bookInfo.id}.json")
            parseAndInsertBook(bookInfo, InputStreamReader(inputStream))

            // Mark bundled books as downloaded
            hadithDao.updateBookDownloadStatus(bookInfo.id, true, System.currentTimeMillis())
            Timber.d("Loaded bundled book: ${bookInfo.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bundled book: ${bookInfo.id}")
        }
    }

    private suspend fun loadBookFromFile(bookInfo: BookInfo, file: File) {
        val reader = InputStreamReader(file.inputStream())
        parseAndInsertBook(bookInfo, reader)
    }

    private suspend fun parseAndInsertBook(bookInfo: BookInfo, reader: InputStreamReader) {
        val jsonReader = JsonReader(reader)
        val chapters = mutableListOf<HadithChapterEntity>()
        val hadithBatch = mutableListOf<HadithEntity>()

        try {
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                when (jsonReader.nextName()) {
                    "chapters" -> {
                        jsonReader.beginArray()
                        while (jsonReader.hasNext()) {
                            jsonReader.beginObject()
                            var chapterId = 0
                            var arabic = ""
                            var english = ""
                            while (jsonReader.hasNext()) {
                                when (jsonReader.nextName()) {
                                    "id" -> chapterId = jsonReader.nextInt()
                                    "arabic" -> arabic = jsonReader.nextString()
                                    "english" -> english = jsonReader.nextString()
                                    else -> jsonReader.skipValue()
                                }
                            }
                            jsonReader.endObject()
                            chapters.add(
                                HadithChapterEntity(
                                    bookId = bookInfo.id,
                                    chapterId = chapterId,
                                    titleArabic = arabic,
                                    titleEnglish = english
                                )
                            )
                        }
                        jsonReader.endArray()
                    }
                    "hadiths" -> {
                        jsonReader.beginArray()
                        while (jsonReader.hasNext()) {
                            jsonReader.beginObject()
                            var hadithId = 0
                            var idInBook = 0
                            var chapterId = 0
                            var textArabic = ""
                            var narrator = ""
                            var textEnglish = ""
                            while (jsonReader.hasNext()) {
                                when (jsonReader.nextName()) {
                                    "id" -> hadithId = jsonReader.nextInt()
                                    "idInBook" -> idInBook = jsonReader.nextInt()
                                    "chapterId" -> chapterId = jsonReader.nextInt()
                                    "arabic" -> textArabic = jsonReader.nextString()
                                    "english" -> {
                                        jsonReader.beginObject()
                                        while (jsonReader.hasNext()) {
                                            when (jsonReader.nextName()) {
                                                "narrator" -> narrator = jsonReader.nextString()
                                                "text" -> textEnglish = jsonReader.nextString()
                                                else -> jsonReader.skipValue()
                                            }
                                        }
                                        jsonReader.endObject()
                                    }
                                    else -> jsonReader.skipValue()
                                }
                            }
                            jsonReader.endObject()

                            hadithBatch.add(
                                HadithEntity(
                                    bookId = bookInfo.id,
                                    hadithId = hadithId,
                                    idInBook = idInBook,
                                    chapterId = chapterId,
                                    textArabic = textArabic,
                                    narratorEnglish = narrator,
                                    textEnglish = textEnglish
                                )
                            )

                            // Batch insert every BATCH_SIZE hadiths
                            if (hadithBatch.size >= BATCH_SIZE) {
                                hadithDao.insertHadiths(hadithBatch.toList())
                                hadithBatch.clear()
                            }
                        }
                        jsonReader.endArray()
                    }
                    else -> jsonReader.skipValue()
                }
            }
            jsonReader.endObject()

            // Insert remaining hadiths
            if (hadithBatch.isNotEmpty()) {
                hadithDao.insertHadiths(hadithBatch)
            }

            // Insert chapters
            if (chapters.isNotEmpty()) {
                hadithDao.insertChapters(chapters)
            }
        } finally {
            jsonReader.close()
        }
    }
}
