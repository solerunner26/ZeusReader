package com.solerunner26.zeusreader.ui.passages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.PassageRepository
import com.solerunner26.zeusreader.data.PassageWithTags
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.data.local.PassageEntity
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PassageSortMode(val label: String) {
    RECENT("Recent"),
    BY_BOOK("By book"),
    SHARED("Shared")
}

data class PassagesUiState(
    val passages: List<PassageWithTags> = emptyList(),
    val folders: List<PassageFolderEntity> = emptyList(),
    val books: List<LibraryBookSummary> = emptyList(),
    val searchQuery: String = "",
    val selectedFolderId: String? = null, // null = All
    val sortMode: PassageSortMode = PassageSortMode.RECENT,
    val selectedIds: Set<String> = emptySet()
)

@HiltViewModel
class PassagesViewModel @Inject constructor(
    private val passageRepository: PassageRepository,
    libraryRepository: LibraryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFolderId = MutableStateFlow<String?>(null)
    private val sortMode = MutableStateFlow(PassageSortMode.RECENT)
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val ftsResults = MutableStateFlow<List<PassageEntity>?>(null)

    private val _lastDeletedId = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val lastDeletedId = _lastDeletedId.asSharedFlow()

    val uiState: StateFlow<PassagesUiState> = combine(
        passageRepository.activePassagesWithTags,
        passageRepository.activeFolders,
        libraryRepository.observeLibrarySummaries(),
        searchQuery,
        selectedFolderId,
        sortMode,
        selectedIds,
        ftsResults
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allPassages = values[0] as List<PassageWithTags>
        @Suppress("UNCHECKED_CAST")
        val folders = values[1] as List<PassageFolderEntity>
        @Suppress("UNCHECKED_CAST")
        val books = values[2] as List<LibraryBookSummary>
        val query = values[3] as String
        val folderFilter = values[4] as String?
        val sort = values[5] as PassageSortMode
        @Suppress("UNCHECKED_CAST")
        val selected = values[6] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val fts = values[7] as List<PassageEntity>?

        val base = if (query.isNotBlank() && fts != null) {
            val hitIds = fts.map { it.id }.toSet()
            allPassages.filter { it.passage.id in hitIds }
        } else {
            allPassages
        }

        val folderFiltered = when (folderFilter) {
            null -> base
            UNSORTED_FOLDER_KEY -> base.filter { it.passage.folderId == null }
            else -> base.filter { it.passage.folderId == folderFilter }
        }

        val sorted = when (sort) {
            PassageSortMode.RECENT -> folderFiltered.sortedByDescending { it.passage.updatedAt }
            PassageSortMode.BY_BOOK -> folderFiltered.sortedBy { it.passage.bookId }
            PassageSortMode.SHARED -> folderFiltered.sortedByDescending { it.passage.shareCount }
        }

        PassagesUiState(
            passages = sorted,
            folders = folders,
            books = books,
            searchQuery = query,
            selectedFolderId = folderFilter,
            sortMode = sort,
            selectedIds = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PassagesUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            ftsResults.value = null
        } else {
            viewModelScope.launch {
                ftsResults.value = passageRepository.searchPassages(query)
            }
        }
    }

    fun selectFolder(folderId: String?) {
        selectedFolderId.value = folderId
    }

    fun setSortMode(mode: PassageSortMode) {
        sortMode.value = mode
    }

    fun toggleSelection(passageId: String) {
        val current = selectedIds.value
        selectedIds.value = if (passageId in current) current - passageId else current + passageId
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            passageRepository.createFolder(name)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            passageRepository.deleteFolderMovePassagesToUnsorted(folderId)
            if (selectedFolderId.value == folderId) {
                selectedFolderId.value = null
            }
        }
    }

    fun saveManualPassage(
        book: LibraryBookSummary,
        text: String,
        pageLabel: String?,
        folderId: String?,
        note: String?
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            passageRepository.savePassage(
                bookId = book.workId,
                editionId = book.editionId,
                folderId = folderId,
                text = text,
                originalText = text,
                locatorJson = "",
                pageLabel = pageLabel?.takeIf { it.isNotBlank() },
                chapterTitle = book.title,
                language = "en",
                note = note
            )
        }
    }

    fun softDeletePassage(passageId: String) {
        viewModelScope.launch {
            passageRepository.softDeletePassage(passageId)
            _lastDeletedId.emit(passageId)
        }
    }

    fun restorePassage(passageId: String) {
        viewModelScope.launch {
            passageRepository.restorePassage(passageId)
        }
    }

    fun deleteSelected() {
        val ids = selectedIds.value.toList()
        selectedIds.value = emptySet()
        viewModelScope.launch {
            ids.forEach { passageRepository.softDeletePassage(it) }
        }
    }

    fun moveSelectedToFolder(folderId: String?) {
        val ids = selectedIds.value
        val passages = uiState.value.passages.filter { it.passage.id in ids }
        selectedIds.value = emptySet()
        viewModelScope.launch {
            passages.forEach { item ->
                passageRepository.updatePassage(
                    passage = item.passage,
                    newText = item.passage.text,
                    newNote = item.passage.note,
                    newFolderId = folderId,
                    newTags = item.tags
                )
            }
        }
    }

    fun observePassage(passageId: String) = passageRepository.observePassage(passageId)

    suspend fun getTags(passageId: String) = passageRepository.getTags(passageId)

    fun updatePassage(
        passage: PassageEntity,
        newText: String,
        newNote: String?,
        newFolderId: String?,
        newTags: List<String>
    ) {
        viewModelScope.launch {
            passageRepository.updatePassage(passage, newText, newNote, newFolderId, newTags)
        }
    }

    fun incrementShareCount(passageId: String) {
        viewModelScope.launch {
            passageRepository.incrementShareCount(passageId)
        }
    }

    companion object {
        const val UNSORTED_FOLDER_KEY = "__UNSORTED__"
    }
}
