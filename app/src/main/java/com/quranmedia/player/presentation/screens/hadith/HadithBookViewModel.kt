package com.quranmedia.player.presentation.screens.hadith

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.entity.HadithBookEntity
import com.quranmedia.player.data.database.entity.HadithChapterEntity
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

data class ChapterWithCount(
    val chapter: HadithChapterEntity,
    val hadithCount: Int
)

@HiltViewModel
class HadithBookViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val bookId: String = savedStateHandle.get<String>("bookId") ?: ""

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getCurrentSettings())

    private val _book = MutableStateFlow<HadithBookEntity?>(null)
    val book: StateFlow<HadithBookEntity?> = _book.asStateFlow()

    private val _chaptersWithCounts = MutableStateFlow<List<ChapterWithCount>>(emptyList())
    val chaptersWithCounts: StateFlow<List<ChapterWithCount>> = _chaptersWithCounts.asStateFlow()

    val chapters = hadithRepository.getChapters(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _book.value = hadithRepository.getBook(bookId)
        }
        viewModelScope.launch {
            hadithRepository.getChapters(bookId).collect { chapterList ->
                val withCounts = chapterList.map { chapter ->
                    ChapterWithCount(
                        chapter = chapter,
                        hadithCount = hadithRepository.getHadithCountForChapter(bookId, chapter.chapterId)
                    )
                }
                _chaptersWithCounts.value = withCounts
            }
        }
    }
}
