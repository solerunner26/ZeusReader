package com.solerunner26.zeusreader.ui.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.local.ReadingProgressEntity
import com.solerunner26.zeusreader.reader.core.ReaderCapabilities
import com.solerunner26.zeusreader.reader.core.UnifiedLocator
import com.solerunner26.zeusreader.reader.epub.EpubSearchResultItem
import com.solerunner26.zeusreader.reader.epub.EpubTocItem
import com.solerunner26.zeusreader.reader.epub.ReadiumEpubSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import javax.inject.Inject

sealed interface EpubReaderUiState {
    data object Loading : EpubReaderUiState
    data class Ready(
        val editionId: String,
        val title: String,
        val publication: Publication,
        val initialLocator: Locator?,
        val tocItems: List<EpubTocItem>,
        val capabilities: ReaderCapabilities,
        val currentProgressionPercent: Int
    ) : EpubReaderUiState

    data class Error(val message: String) : EpubReaderUiState
}

@HiltViewModel
class EpubReaderViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private var session: ReadiumEpubSession? = null
    private var currentEditionId: String = ""

    private val _uiState = MutableStateFlow<EpubReaderUiState>(EpubReaderUiState.Loading)
    val uiState: StateFlow<EpubReaderUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<EpubSearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<EpubSearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun openBook(editionId: String, title: String, localPath: String) {
        if (currentEditionId == editionId && session?.publication != null) return
        currentEditionId = editionId
        session?.close()
        val newSession = ReadiumEpubSession(context)
        session = newSession

        viewModelScope.launch {
            _uiState.value = EpubReaderUiState.Loading
            val openResult = newSession.open(localPath)
            openResult.fold(
                onSuccess = { pub ->
                    val saved = libraryRepository.getReadingProgress(editionId)
                    val unified = UnifiedLocator.decode(saved?.locatorJson) as? UnifiedLocator.EpubLocator
                    val restoredLocator = unified?.readiumLocatorJson?.let { raw ->
                        runCatching { Locator.fromJSON(JSONObject(raw)) }.getOrNull()
                    }
                    val pct = ((restoredLocator?.locations?.totalProgression ?: 0.0) * 100.0).toInt()
                    _uiState.value = EpubReaderUiState.Ready(
                        editionId = editionId,
                        title = title,
                        publication = pub,
                        initialLocator = restoredLocator,
                        tocItems = newSession.flattenedToc(),
                        capabilities = newSession.capabilities,
                        currentProgressionPercent = pct
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = EpubReaderUiState.Error(
                        throwable.message ?: "Unable to open EPUB"
                    )
                }
            )
        }
    }

    fun onLocatorChanged(locator: Locator) {
        val editionId = currentEditionId
        if (editionId.isBlank()) return
        val totalProg = locator.locations.totalProgression
        val unified = UnifiedLocator.EpubLocator(
            readiumLocatorJson = locator.toJSON().toString(),
            href = locator.href.toString(),
            title = locator.title,
            progression = locator.locations.progression ?: 0.0,
            totalProgression = totalProg
        )
        val current = _uiState.value as? EpubReaderUiState.Ready
        if (current != null) {
            _uiState.value = current.copy(
                currentProgressionPercent = ((totalProg ?: 0.0) * 100.0).toInt()
            )
        }
        viewModelScope.launch {
            libraryRepository.saveReadingProgress(
                ReadingProgressEntity(
                    editionId = editionId,
                    pageIndex = ((totalProg ?: 0.0) * 1000.0).toInt(),
                    locatorJson = UnifiedLocator.encode(unified),
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
                _searchResults.value = active.search(query)
            } finally {
                _isSearching.value = false
            }
        }
    }

    override fun onCleared() {
        session?.close()
        session = null
        super.onCleared()
    }
}
