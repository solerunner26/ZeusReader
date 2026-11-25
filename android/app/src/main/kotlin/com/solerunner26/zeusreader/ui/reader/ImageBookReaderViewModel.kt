package com.solerunner26.zeusreader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.local.ImageBookPageEntity
import com.solerunner26.zeusreader.data.local.ReadingProgressEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ImageBookReaderViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val editionIdFlow = MutableStateFlow<String?>(null)
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    val pages: StateFlow<List<ImageBookPageEntity>> = editionIdFlow
        .filterNotNull()
        .flatMapLatest { id -> libraryRepository.observeImageBookPages(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun loadEdition(editionId: String) {
        if (editionIdFlow.value == editionId) return
        editionIdFlow.value = editionId
        viewModelScope.launch {
            val saved = libraryRepository.getReadingProgress(editionId)
            _currentPageIndex.value = saved?.pageIndex ?: 0
        }
    }

    fun selectPage(index: Int) {
        val count = pages.value.size
        if (count == 0) return
        val clamped = index.coerceIn(0, count - 1)
        _currentPageIndex.value = clamped
        val editionId = editionIdFlow.value ?: return
        viewModelScope.launch {
            libraryRepository.saveReadingProgress(
                ReadingProgressEntity(
                    editionId = editionId,
                    pageIndex = clamped,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun nextPage() = selectPage(_currentPageIndex.value + 1)

    fun previousPage() = selectPage(_currentPageIndex.value - 1)

    fun saveReorderedPages(orderedPageIds: List<String>) {
        val editionId = editionIdFlow.value ?: return
        viewModelScope.launch {
            libraryRepository.reorderImageBookPages(editionId, orderedPageIds)
            _currentPageIndex.value = 0
        }
    }
}
