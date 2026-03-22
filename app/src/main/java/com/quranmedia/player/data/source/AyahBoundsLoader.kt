package com.quranmedia.player.data.source

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class AyahLineBoundsRecord(
    val pageNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val lineNumber: Int,
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
)

object AyahBoundsLoader {
    private const val ASSET_FILE_NAME = "ayah_bounds.db"
    private const val DATABASE_FILE_NAME = "ayah_bounds.db"

    private val pageCache = mutableMapOf<Int, List<AyahLineBoundsRecord>>()
    private val cacheMutex = Mutex()
    private val copyMutex = Mutex()

    suspend fun loadPageBounds(context: Context, pageNumber: Int): List<AyahLineBoundsRecord> {
        cacheMutex.withLock {
            pageCache[pageNumber]?.let { return it }
        }

        return withContext(Dispatchers.IO) {
            try {
                val databaseFile = ensureDatabaseFile(context.applicationContext)
                val pageBounds = queryPageBounds(databaseFile, pageNumber)

                cacheMutex.withLock {
                    pageCache[pageNumber] = pageBounds
                }

                pageBounds
            } catch (e: Exception) {
                Timber.e(e, "Failed to load ayah bounds for page $pageNumber")
                emptyList()
            }
        }
    }

    private suspend fun ensureDatabaseFile(context: Context): File {
        val databaseFile = File(context.cacheDir, DATABASE_FILE_NAME)
        if (databaseFile.exists() && databaseFile.length() > 0L) {
            return databaseFile
        }

        copyMutex.withLock {
            if (databaseFile.exists() && databaseFile.length() > 0L) {
                return databaseFile
            }

            context.assets.open(ASSET_FILE_NAME).use { input ->
                databaseFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return databaseFile
    }

    private fun queryPageBounds(databaseFile: File, pageNumber: Int): List<AyahLineBoundsRecord> {
        val bounds = mutableListOf<AyahLineBoundsRecord>()
        val database = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        database.use { db ->
            db.query(
                "ayah_bounds",
                arrayOf(
                    "page_number",
                    "sura_number",
                    "ayah_number",
                    "line_number",
                    "min_x",
                    "max_x",
                    "min_y",
                    "max_y"
                ),
                "page_number = ?",
                arrayOf(pageNumber.toString()),
                null,
                null,
                "line_number ASC, min_x ASC"
            ).use { cursor ->
                val pageNumberIndex = cursor.getColumnIndexOrThrow("page_number")
                val surahNumberIndex = cursor.getColumnIndexOrThrow("sura_number")
                val ayahNumberIndex = cursor.getColumnIndexOrThrow("ayah_number")
                val lineNumberIndex = cursor.getColumnIndexOrThrow("line_number")
                val minXIndex = cursor.getColumnIndexOrThrow("min_x")
                val maxXIndex = cursor.getColumnIndexOrThrow("max_x")
                val minYIndex = cursor.getColumnIndexOrThrow("min_y")
                val maxYIndex = cursor.getColumnIndexOrThrow("max_y")

                while (cursor.moveToNext()) {
                    bounds += AyahLineBoundsRecord(
                        pageNumber = cursor.getInt(pageNumberIndex),
                        surahNumber = cursor.getInt(surahNumberIndex),
                        ayahNumber = cursor.getInt(ayahNumberIndex),
                        lineNumber = cursor.getInt(lineNumberIndex),
                        minX = cursor.getInt(minXIndex),
                        maxX = cursor.getInt(maxXIndex),
                        minY = cursor.getInt(minYIndex),
                        maxY = cursor.getInt(maxYIndex)
                    )
                }
            }
        }

        return bounds
    }
}
