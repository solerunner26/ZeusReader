package com.solerunner26.zeusreader.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.importer.BookCoverAndMetadataExtractor
import com.solerunner26.zeusreader.data.importer.ImportBatchReport
import com.solerunner26.zeusreader.data.importer.LibraryImporter
import com.solerunner26.zeusreader.data.importer.WatchedFolderManager
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.data.local.LibraryDao
import com.solerunner26.zeusreader.data.local.UnsupportedFileEntity
import com.solerunner26.zeusreader.data.local.WatchedFolderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFormatFilter {
    ALL,
    EPUB,
    PDF,
    IMAGE_BOOK
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val libraryImporter: LibraryImporter,
    private val watchedFolderManager: WatchedFolderManager,
    private val coverExtractor: BookCoverAndMetadataExtractor? = null,
    private val libraryDao: LibraryDao? = null
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val formatFilter = MutableStateFlow(LibraryFormatFilter.ALL)
    private val _selectedRackCategory = MutableStateFlow("All")
    val selectedRackCategory: StateFlow<String> = _selectedRackCategory.asStateFlow()
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    val watchedFolders: StateFlow<List<WatchedFolderEntity>> =
        libraryRepository.observeWatchedFolders()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val unsupportedFiles: StateFlow<List<UnsupportedFileEntity>> =
        libraryRepository.observeUnsupportedFiles()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val booksFlow = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            libraryRepository.observeLibrarySummaries()
        } else {
            libraryRepository.searchLibrarySummaries(query)
        }
    }

    val uiState: StateFlow<LibraryUiState> =
        combine(booksFlow, searchQuery, formatFilter, _selectedRackCategory) { books, query, filter, rack ->
            val byFormat = when (filter) {
                LibraryFormatFilter.ALL -> books
                LibraryFormatFilter.EPUB -> books.filter { it.primaryFormat.equals("EPUB", ignoreCase = true) }
                LibraryFormatFilter.PDF -> books.filter { it.primaryFormat.equals("PDF", ignoreCase = true) }
                LibraryFormatFilter.IMAGE_BOOK -> books.filter { it.isImageBook }
            }
            val filtered = if (rack == "All") {
                byFormat
            } else {
                byFormat.filter { it.category.equals(rack, ignoreCase = true) }
            }
            if (books.isEmpty() && query.isBlank()) {
                LibraryUiState.Empty
            } else {
                LibraryUiState.Populated(
                    books = filtered,
                    searchQuery = query,
                    selectedFilter = filter
                )
            }
        }
            .catch { throwable ->
                emit(LibraryUiState.Error(throwable.message ?: "Failed to load library"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LibraryUiState.Loading
            )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onFilterSelected(filter: LibraryFormatFilter) {
        formatFilter.value = filter
    }

    fun onRackCategorySelected(category: String) {
        _selectedRackCategory.value = category
    }

    fun refreshCoversAndMetadata(books: List<LibraryBookSummary>) {
        val extractor = coverExtractor ?: return
        val dao = libraryDao ?: return
        viewModelScope.launch {
            books.filter { it.coverPath.isNullOrBlank() || it.authors.isBlank() || it.category == "General" }
                .forEach { book ->
                    val meta = extractor.extractAndEnrich(
                        editionId = book.editionId,
                        title = book.title,
                        existingAuthors = book.authors,
                        format = book.primaryFormat,
                        localPath = book.primaryLocalPath,
                        allowNetworkLookup = true
                    )
                    if (meta.coverPath != null && meta.coverPath != book.coverPath) {
                        dao.updateEditionCover(book.editionId, meta.coverPath)
                    }
                    dao.updateWorkMetadata(book.workId, meta.authors, meta.category)
                }
        }
    }

    fun importUris(uris: List<Uri>, origin: String = "picker") {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val report = libraryImporter.importUris(uris, origin = origin)
                emitImportSummary(report)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun registerWatchedFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                watchedFolderManager.registerFolder(treeUri)
                val report = watchedFolderManager.scanAllFolders()
                emitImportSummary(report, prefix = "Watched folder synced: ")
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun rescanWatchedFolders() {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val report = watchedFolderManager.scanAllFolders()
                emitImportSummary(report, prefix = "Folder scan complete: ")
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun removeWatchedFolder(folderId: String) {
        viewModelScope.launch {
            watchedFolderManager.removeFolder(folderId)
            _messages.tryEmit("Watched folder removed.")
        }
    }

    fun clearUnsupportedFiles() {
        viewModelScope.launch {
            libraryRepository.clearUnsupportedFiles()
        }
    }

    private fun emitImportSummary(report: ImportBatchReport, prefix: String = "") {
        val parts = buildList {
            if (report.importedBooks.isNotEmpty()) {
                add("${report.importedBooks.size} added")
            }
            if (report.duplicates.isNotEmpty()) {
                add("${report.duplicates.size} duplicate skipped")
            }
            if (report.rejected.isNotEmpty()) {
                val firstReason = report.rejected.first().reason
                add("${report.rejected.size} rejected ($firstReason)")
            }
        }
        val msg = if (parts.isEmpty()) {
            "${prefix}No new supported files found."
        } else {
            "$prefix${parts.joinToString(" • ")}"
        }
        _messages.tryEmit(msg)
    }
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Populated(
        val books: List<LibraryBookSummary>,
        val searchQuery: String = "",
        val selectedFilter: LibraryFormatFilter = LibraryFormatFilter.ALL
    ) : LibraryUiState

    data class Error(val message: String) : LibraryUiState
}
