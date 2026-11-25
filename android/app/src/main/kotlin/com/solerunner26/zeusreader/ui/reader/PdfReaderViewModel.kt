package com.solerunner26.zeusreader.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.local.ReadingProgressEntity
import com.solerunner26.zeusreader.reader.core.UnifiedLocator
import com.solerunner26.zeusreader.reader.pdf.MuPdfDocumentSession
import com.solerunner26.zeusreader.reader.pdf.PdfOutlineEntry
import com.solerunner26.zeusreader.reader.pdf.PdfSearchHit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PdfReaderUiState {
    data object Loading : PdfReaderUiState
    data class Ready(
        val editionId: String,
        val title: String,
        val currentPage: Int,
        val pageCount: Int,
        val bitmap: Bitmap?,
        val isRenderingPage: Boolean,
        val initialScale: Float = 1f,
        val initialScrollX: Float = 0f,
        val initialScrollY: Float = 0f,
        val outline: List<PdfOutlineEntry> = emptyList(),
        val pageError: String? = null
    ) : PdfReaderUiState

    data class Error(val message: String) : PdfReaderUiState
}

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private var session: MuPdfDocumentSession? = null
    private var currentEditionId: String = ""
    private var currentTitle: String = ""
    private var totalPages: Int = 0
    private var targetRenderWidthPx: Int = 1080
    private var outlineEntries: List<PdfOutlineEntry> = emptyList()

    private val _uiState = MutableStateFlow<PdfReaderUiState>(PdfReaderUiState.Loading)
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    private val _searchHits = MutableStateFlow<List<PdfSearchHit>>(emptyList())
    val searchHits: StateFlow<List<PdfSearchHit>> = _searchHits.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun openDocument(
        editionId: String,
        title: String,
        localPath: String,
        viewportWidthPx: Int = 1080
    ) {
        if (currentEditionId == editionId && session != null) return
        currentEditionId = editionId
        currentTitle = title
        targetRenderWidthPx = viewportWidthPx.coerceAtLeast(720)

        session?.close()
        val newSession = MuPdfDocumentSession(localPath)
        session = newSession

        viewModelScope.launch {
            _uiState.value = PdfReaderUiState.Loading
            val openResult = newSession.open()
            openResult.fold(
                onSuccess = { count ->
                    totalPages = count
                    if (count <= 0) {
                        _uiState.value = PdfReaderUiState.Error("Document contains no pages.")
                        return@fold
                    }
                    outlineEntries = newSession.loadOutline()
                    val saved = libraryRepository.getReadingProgress(editionId)
                    val pdfLoc = UnifiedLocator.decode(saved?.locatorJson) as? UnifiedLocator.PdfLocator
                    val initialPage = (pdfLoc?.pageIndex ?: saved?.pageIndex ?: 0).coerceIn(0, count - 1)
                    loadPage(
                        pageIndex = initialPage,
                        scale = pdfLoc?.viewportScale ?: saved?.viewportScale ?: 1f,
                        scrollX = pdfLoc?.scrollX ?: saved?.scrollX ?: 0f,
                        scrollY = pdfLoc?.scrollY ?: saved?.scrollY ?: 0f
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = PdfReaderUiState.Error(
                        throwable.message ?: "Unable to open PDF document."
                    )
                }
            )
        }
    }

    fun goToPage(pageIndex: Int) {
        if (totalPages <= 0) return
        val clamped = pageIndex.coerceIn(0, totalPages - 1)
        loadPage(clamped, scale = 1f, scrollX = 0f, scrollY = 0f)
    }

    fun nextPage() {
        val state = _uiState.value as? PdfReaderUiState.Ready ?: return
        if (state.currentPage + 1 < state.pageCount) {
            goToPage(state.currentPage + 1)
        }
    }

    fun previousPage() {
        val state = _uiState.value as? PdfReaderUiState.Ready ?: return
        if (state.currentPage > 0) {
            goToPage(state.currentPage - 1)
        }
    }

    fun updateViewport(pageIndex: Int, scale: Float, scrollX: Float, scrollY: Float) {
        val editionId = currentEditionId
        if (editionId.isBlank()) return
        val locator = UnifiedLocator.PdfLocator(
            pageIndex = pageIndex,
            viewportScale = scale,
            scrollX = scrollX,
            scrollY = scrollY
        )
        viewModelScope.launch {
            libraryRepository.saveReadingProgress(
                ReadingProgressEntity(
                    editionId = editionId,
                    pageIndex = pageIndex,
                    viewportScale = scale,
                    scrollX = scrollX,
                    scrollY = scrollY,
                    locatorJson = UnifiedLocator.encode(locator),
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun search(query: String) {
        val active = session ?: return
        viewModelScope.launch {
            _isSearching.value = true
            try {
                _searchHits.value = active.searchText(query)
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun loadPage(pageIndex: Int, scale: Float, scrollX: Float, scrollY: Float) {
        val activeSession = session ?: return
        val previousBitmap = (_uiState.value as? PdfReaderUiState.Ready)?.bitmap
        _uiState.value = PdfReaderUiState.Ready(
            editionId = currentEditionId,
            title = currentTitle,
            currentPage = pageIndex,
            pageCount = totalPages,
            bitmap = previousBitmap,
            isRenderingPage = true,
            initialScale = scale,
            initialScrollX = scrollX,
            initialScrollY = scrollY,
            outline = outlineEntries
        )

        viewModelScope.launch {
            val renderResult = activeSession.renderPage(pageIndex, targetRenderWidthPx)
            renderResult.fold(
                onSuccess = { bitmap ->
                    _uiState.value = PdfReaderUiState.Ready(
                        editionId = currentEditionId,
                        title = currentTitle,
                        currentPage = pageIndex,
                        pageCount = totalPages,
                        bitmap = bitmap,
                        isRenderingPage = false,
                        initialScale = scale,
                        initialScrollX = scrollX,
                        initialScrollY = scrollY,
                        outline = outlineEntries
                    )
                    updateViewport(pageIndex, scale, scrollX, scrollY)
                },
                onFailure = { err ->
                    _uiState.value = PdfReaderUiState.Ready(
                        editionId = currentEditionId,
                        title = currentTitle,
                        currentPage = pageIndex,
                        pageCount = totalPages,
                        bitmap = previousBitmap,
                        isRenderingPage = false,
                        initialScale = scale,
                        initialScrollX = scrollX,
                        initialScrollY = scrollY,
                        outline = outlineEntries,
                        pageError = err.message ?: "Failed to render page"
                    )
                }
            )
        }
    }

    override fun onCleared() {
        session?.close()
        session = null
        super.onCleared()
    }
}
