package com.quranmedia.player.data.source

import com.quranmedia.player.domain.repository.HadithDownloadProgress
import com.quranmedia.player.domain.repository.HadithDownloadState
import com.quranmedia.player.domain.repository.HadithRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for downloading non-bundled hadith books.
 * Delegates to HadithRepository for actual download and parsing.
 */
@Singleton
class HadithDownloadManager @Inject constructor(
    private val hadithRepository: HadithRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloadProgress: StateFlow<HadithDownloadProgress> = hadithRepository.downloadProgress

    fun downloadBook(bookId: String) {
        scope.launch {
            try {
                hadithRepository.downloadBook(bookId)
            } catch (e: Exception) {
                Timber.e(e, "Download manager: failed to download book $bookId")
            }
        }
    }

    fun isDownloading(): Boolean {
        val state = downloadProgress.value.state
        return state == HadithDownloadState.DOWNLOADING || state == HadithDownloadState.PARSING
    }
}
