package com.quranmedia.player.presentation.screens.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranmedia.player.data.database.entity.HadithEntity
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.domain.repository.HadithRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HadithSearchViewModel @Inject constructor(
    private val hadithRepository: HadithRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getCurrentSettings())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<HadithEntity>>(emptyList())
    val searchResults: StateFlow<List<HadithEntity>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var bookScope: String? = null
    private var searchJob: Job? = null

    fun setBookScope(bookId: String?) {
        bookScope = bookId
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _isSearching.value = true
            try {
                val results = if (bookScope != null) {
                    hadithRepository.searchHadithsInBook(newQuery, bookScope!!)
                } else {
                    hadithRepository.searchHadiths(newQuery)
                }
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}
