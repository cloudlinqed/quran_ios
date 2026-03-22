package com.quranmedia.player.presentation.screens.hadith

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.entity.HadithBookEntity
import com.quranmedia.player.data.database.entity.HadithChapterEntity
import com.quranmedia.player.data.database.entity.HadithEntity
import com.quranmedia.player.data.repository.SettingsRepository
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
class HadithReaderViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val bookId: String = savedStateHandle.get<String>("bookId") ?: ""
    val chapterId: Int = savedStateHandle.get<Int>("chapterId") ?: 1
    val initialHadithIndex: Int = savedStateHandle.get<Int>("hadithIndex") ?: 0

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getCurrentSettings())

    private val _book = MutableStateFlow<HadithBookEntity?>(null)
    val book: StateFlow<HadithBookEntity?> = _book.asStateFlow()

    private val _chapter = MutableStateFlow<HadithChapterEntity?>(null)
    val chapter: StateFlow<HadithChapterEntity?> = _chapter.asStateFlow()

    private val _hadiths = MutableStateFlow<List<HadithEntity>>(emptyList())
    val hadiths: StateFlow<List<HadithEntity>> = _hadiths.asStateFlow()

    // All chapters for in-reader chapter navigation
    private val _allChapters = MutableStateFlow<List<HadithChapterEntity>>(emptyList())
    val allChapters: StateFlow<List<HadithChapterEntity>> = _allChapters.asStateFlow()

    private var _currentChapterId = MutableStateFlow(chapterId)
    val currentChapterId: StateFlow<Int> = _currentChapterId.asStateFlow()

    init {
        viewModelScope.launch {
            _book.value = hadithRepository.getBook(bookId)
        }
        viewModelScope.launch {
            hadithRepository.getChapters(bookId).collect { chapters ->
                _allChapters.value = chapters
                _chapter.value = chapters.find { it.chapterId == _currentChapterId.value }
            }
        }
        loadChapterHadiths(chapterId)
    }

    private fun loadChapterHadiths(chapId: Int) {
        viewModelScope.launch {
            hadithRepository.getHadithsByChapter(bookId, chapId).collect { hadithList ->
                _hadiths.value = hadithList
            }
        }
    }

    fun navigateToChapter(chapId: Int) {
        _currentChapterId.value = chapId
        _chapter.value = _allChapters.value.find { it.chapterId == chapId }
        _hadiths.value = emptyList()
        loadChapterHadiths(chapId)
    }

    fun hasNextChapter(): Boolean {
        val chapters = _allChapters.value
        val currentIdx = chapters.indexOfFirst { it.chapterId == _currentChapterId.value }
        return currentIdx >= 0 && currentIdx < chapters.lastIndex
    }

    fun hasPreviousChapter(): Boolean {
        val chapters = _allChapters.value
        val currentIdx = chapters.indexOfFirst { it.chapterId == _currentChapterId.value }
        return currentIdx > 0
    }

    fun goToNextChapter(): Int? {
        val chapters = _allChapters.value
        val currentIdx = chapters.indexOfFirst { it.chapterId == _currentChapterId.value }
        if (currentIdx >= 0 && currentIdx < chapters.lastIndex) {
            val nextChapId = chapters[currentIdx + 1].chapterId
            navigateToChapter(nextChapId)
            return nextChapId
        }
        return null
    }

    fun goToPreviousChapter(): Int? {
        val chapters = _allChapters.value
        val currentIdx = chapters.indexOfFirst { it.chapterId == _currentChapterId.value }
        if (currentIdx > 0) {
            val prevChapId = chapters[currentIdx - 1].chapterId
            navigateToChapter(prevChapId)
            return prevChapId
        }
        return null
    }

    fun getShareText(hadith: HadithEntity): String {
        val book = _book.value
        return buildString {
            append(hadith.textArabic)
            append("\n\n")
            if (hadith.narratorEnglish.isNotEmpty()) {
                append(hadith.narratorEnglish)
                append("\n")
            }
            append(hadith.textEnglish)
            append("\n\n")
            if (book != null) {
                append("— ${book.titleEnglish}, Hadith ${hadith.idInBook}")
            }
        }
    }
}
