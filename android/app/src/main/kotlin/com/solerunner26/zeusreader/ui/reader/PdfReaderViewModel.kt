package com.solerunner26.zeusreader.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.local.ReadingProgressEntity
import com.solerunner26.zeusreader.reader.pdf.MuPdfDocumentSession
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

    private val _uiState = MutableStateFlow<PdfReaderUiState>(PdfReaderUiState.Loading)
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

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
                    val saved = libraryRepository.getReadingProgress(editionId)
                    val initialPage = (saved?.pageIndex ?: 0).coerceIn(0, count - 1)
                    loadPage(initialPage)
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
        loadPage(clamped)
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

    private fun loadPage(pageIndex: Int) {
        val activeSession = session ?: return
        val previousBitmap = (_uiState.value as? PdfReaderUiState.Ready)?.bitmap
        _uiState.value = PdfReaderUiState.Ready(
            editionId = currentEditionId,
            title = currentTitle,
            currentPage = pageIndex,
            pageCount = totalPages,
            bitmap = previousBitmap,
            isRenderingPage = true
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
                        isRenderingPage = false
                    )
                    libraryRepository.saveReadingProgress(
                        ReadingProgressEntity(
                            editionId = currentEditionId,
                            pageIndex = pageIndex,
                            updatedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                },
                onFailure = { err ->
                    _uiState.value = PdfReaderUiState.Ready(
                        editionId = currentEditionId,
                        title = currentTitle,
                        currentPage = pageIndex,
                        pageCount = totalPages,
                        bitmap = previousBitmap,
                        isRenderingPage = false,
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
