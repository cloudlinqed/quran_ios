package com.quranmedia.player.presentation.screens.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.entity.HadithBookEntity
import com.quranmedia.player.data.repository.HadithRepositoryImpl
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.repository.HadithDownloadProgress
import com.quranmedia.player.domain.repository.HadithDownloadState
import com.quranmedia.player.domain.repository.HadithRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HadithLibraryViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getCurrentSettings())

    val books = hadithRepository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgress: StateFlow<HadithDownloadProgress> = hadithRepository.downloadProgress

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        viewModelScope.launch {
            hadithRepository.initializeBundledBooks()
            _isInitialized.value = true
        }
    }

    fun downloadBook(bookId: String) {
        viewModelScope.launch {
            hadithRepository.downloadBook(bookId)
        }
    }

    fun getMyLibraryBooks(allBooks: List<HadithBookEntity>): List<HadithBookEntity> {
        return allBooks.filter { it.isBundled || it.isDownloaded }
    }

    fun getDownloadableBooks(allBooks: List<HadithBookEntity>): List<HadithBookEntity> {
        return allBooks.filter { !it.isBundled && !it.isDownloaded }
    }
}
