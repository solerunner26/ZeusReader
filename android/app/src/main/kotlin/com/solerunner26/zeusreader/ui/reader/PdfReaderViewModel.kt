package com.solerunner26.zeusreader.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.PassageRepository
import com.solerunner26.zeusreader.data.local.BookGlossaryEntity
import com.solerunner26.zeusreader.data.local.HighlightEntity
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import com.solerunner26.zeusreader.data.local.ReadingProgressEntity
import com.solerunner26.zeusreader.ui.theme.ActiveCoverHueHolder
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.dictionary.DictionaryLookupResult
import com.solerunner26.zeusreader.dictionary.OfflineDictionaryRepository
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ml.TranslationService
import com.solerunner26.zeusreader.reader.core.UnifiedLocator
import com.solerunner26.zeusreader.reader.pdf.MuPdfDocumentSession
import com.solerunner26.zeusreader.reader.pdf.PdfOutlineEntry
import com.solerunner26.zeusreader.reader.pdf.PdfSearchHit
import com.solerunner26.zeusreader.reader.prefs.ReaderPreferencesRepository
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import com.solerunner26.zeusreader.translation.SentenceSegmenter
import com.solerunner26.zeusreader.translation.TranslatedDisplayMode
import com.solerunner26.zeusreader.translation.TranslatedPassageResult
import com.solerunner26.zeusreader.translation.TranslatedReadingEngine
import com.solerunner26.zeusreader.translation.TranslationEnginePreference
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
import java.util.UUID
import javax.inject.Inject

sealed interface PdfReaderUiState {
    data object Loading : PdfReaderUiState
    data class Ready(
        val editionId: String,
        val title: String,
        val currentPage: Int,
        val pageCount: Int,
        val bitmap: Bitmap?,
        val reflowedText: String?,
        val isRenderingPage: Boolean,
        val initialScale: Float = 1f,
        val initialScrollX: Float = 0f,
        val initialScrollY: Float = 0f,
        val outline: List<PdfOutlineEntry> = emptyList(),
        val pageError: String? = null
    ) : PdfReaderUiState

    data class Error(val message: String) : PdfReaderUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val preferencesRepository: ReaderPreferencesRepository,
    private val dictionaryRepository: OfflineDictionaryRepository,
    private val translationService: TranslationService,
    private val translatedReadingEngine: TranslatedReadingEngine,
    private val passageRepository: PassageRepository,
    private val activeCoverHueHolder: ActiveCoverHueHolder
) : ViewModel() {

    private var session: MuPdfDocumentSession? = null
    private val editionIdFlow = MutableStateFlow<String?>(null)
    private var fileVersionSha256: String = ""
    private var currentTitle: String = ""
    private var totalPages: Int = 0
    private var targetRenderWidthPx: Int = 1080
    private var outlineEntries: List<PdfOutlineEntry> = emptyList()
    private var lastScale: Float = 1f
    private var lastScrollX: Float = 0f
    private var lastScrollY: Float = 0f

    val settings: StateFlow<ReaderSettings> = preferencesRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReaderSettings()
        )

    val highlights: StateFlow<List<HighlightEntity>> = editionIdFlow
        .filterNotNull()
        .flatMapLatest { libraryRepository.observeHighlights(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val passageFolders: StateFlow<List<PassageFolderEntity>> = passageRepository.activeFolders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun createPassageFolder(name: String) {
        viewModelScope.launch {
            passageRepository.createFolder(name)
        }
    }

    fun savePassage(
        text: String,
        originalText: String,
        language: String,
        folderId: String?,
        note: String?
    ) {
        val editionId = editionIdFlow.value ?: return
        val ready = _uiState.value as? PdfReaderUiState.Ready
        val pageNum = (ready?.currentPage ?: 0) + 1
        viewModelScope.launch {
            passageRepository.savePassage(
                bookId = editionId,
                editionId = editionId,
                folderId = folderId,
                text = text,
                originalText = originalText,
                locatorJson = """{"page":${pageNum - 1}}""",
                pageLabel = "p. $pageNum",
                chapterTitle = currentTitle.ifBlank { null },
                language = language,
                note = note
            )
        }
    }

    private val _uiState = MutableStateFlow<PdfReaderUiState>(PdfReaderUiState.Loading)
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    private val _searchHits = MutableStateFlow<List<PdfSearchHit>>(emptyList())
    val searchHits: StateFlow<List<PdfSearchHit>> = _searchHits.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _dictionaryResult = MutableStateFlow<DictionaryLookupResult?>(null)
    val dictionaryResult: StateFlow<DictionaryLookupResult?> = _dictionaryResult.asStateFlow()

    private val _sentenceTranslation = MutableStateFlow<String?>(null)
    val sentenceTranslation: StateFlow<String?> = _sentenceTranslation.asStateFlow()

    private val _targetLanguage = MutableStateFlow(StudyTargetLanguage.HINDI)
    val targetLanguage: StateFlow<StudyTargetLanguage> = _targetLanguage.asStateFlow()

    val bookGlossary: StateFlow<List<BookGlossaryEntity>> = editionIdFlow
        .filterNotNull()
        .flatMapLatest { libraryRepository.observeBookGlossary(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _translatedModeEnabled = MutableStateFlow(false)
    val translatedModeEnabled: StateFlow<Boolean> = _translatedModeEnabled.asStateFlow()

    private val _translatedDisplayMode = MutableStateFlow(TranslatedDisplayMode.INTERLINEAR)
    val translatedDisplayMode: StateFlow<TranslatedDisplayMode> = _translatedDisplayMode.asStateFlow()

    private val _translationEnginePreference = MutableStateFlow(TranslationEnginePreference.ON_DEVICE_MLKIT)
    val translationEnginePreference: StateFlow<TranslationEnginePreference> = _translationEnginePreference.asStateFlow()

    private val _translatedPassage = MutableStateFlow<TranslatedPassageResult?>(null)
    val translatedPassage: StateFlow<TranslatedPassageResult?> = _translatedPassage.asStateFlow()

    private val _isTranslatingPassage = MutableStateFlow(false)
    val isTranslatingPassage: StateFlow<Boolean> = _isTranslatingPassage.asStateFlow()

    private val _ocrNotice = MutableStateFlow<String?>(null)
    val ocrNotice: StateFlow<String?> = _ocrNotice.asStateFlow()

    fun setTargetLanguage(lang: StudyTargetLanguage, sentenceContext: String = "") {
        _targetLanguage.value = lang
        if (sentenceContext.isNotBlank()) {
            viewModelScope.launch {
                _sentenceTranslation.value = translationService.translateFromEnglish(sentenceContext, lang).getOrNull()
            }
        }
        if (_translatedModeEnabled.value) {
            translateCurrentPdfPage(forceRetranslate = false)
        }
    }

    fun toggleTranslatedReadingMode(enabled: Boolean) {
        _translatedModeEnabled.value = enabled
        if (enabled) {
            translateCurrentPdfPage(forceRetranslate = false)
        }
    }

    fun setTranslatedDisplayMode(mode: TranslatedDisplayMode) {
        _translatedDisplayMode.value = mode
    }

    fun setTranslationEnginePreference(pref: TranslationEnginePreference) {
        _translationEnginePreference.value = pref
        if (_translatedModeEnabled.value) {
            translateCurrentPdfPage(forceRetranslate = false)
        }
    }

    fun retranslateCurrentPassage() {
        translateCurrentPdfPage(forceRetranslate = true)
    }

    fun addGlossaryTerm(term: String) {
        val editionId = editionIdFlow.value ?: return
        viewModelScope.launch {
            libraryRepository.addGlossaryTerm(editionId, term)
            if (_translatedModeEnabled.value) {
                translateCurrentPdfPage(forceRetranslate = false)
            }
        }
    }

    fun removeGlossaryTerm(termId: String) {
        val editionId = editionIdFlow.value ?: return
        viewModelScope.launch {
            libraryRepository.deleteGlossaryTerm(editionId, termId)
            if (_translatedModeEnabled.value) {
                translateCurrentPdfPage(forceRetranslate = false)
            }
        }
    }

    fun lookupWord(word: String, sentenceContext: String = "") {
        viewModelScope.launch {
            _dictionaryResult.value = dictionaryRepository.lookup(word)
            if (sentenceContext.isNotBlank()) {
                _sentenceTranslation.value = translationService
                    .translateFromEnglish(sentenceContext, _targetLanguage.value)
                    .getOrNull()
            }
        }
    }

    fun saveLookupToVocabulary(result: DictionaryLookupResult, lang: StudyTargetLanguage, sentenceContext: String) {
        val now = System.currentTimeMillis()
        val editionId = editionIdFlow.value
        val gloss = if (lang == StudyTargetLanguage.HINDI) {
            result.hindiGlosses.joinToString("; ") { it.gloss }
        } else {
            result.gujaratiGlosses.joinToString("; ") { it.gloss }
        }
        val enMeaning = result.englishSenses.firstOrNull()?.definition.orEmpty()
        viewModelScope.launch {
            libraryRepository.saveVocabularyWord(
                VocabWordEntity(
                    id = UUID.randomUUID().toString(),
                    word = result.queryWord,
                    lemma = result.matchedLemma,
                    targetLang = lang.code,
                    englishMeaning = enMeaning,
                    translatedMeaning = gloss,
                    bookTitle = currentTitle,
                    editionId = editionId,
                    sentenceContext = sentenceContext,
                    nextReviewEpochMs = now,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
        }
    }

    fun openDocument(
        editionId: String,
        title: String,
        localPath: String,
        viewportWidthPx: Int = 1080
    ) {
        if (editionIdFlow.value == editionId && session != null) return
        editionIdFlow.value = editionId
        currentTitle = title
        targetRenderWidthPx = viewportWidthPx.coerceAtLeast(720)

        session?.close()
        val newSession = MuPdfDocumentSession(localPath)
        session = newSession

        viewModelScope.launch {
            _uiState.value = PdfReaderUiState.Loading
            fileVersionSha256 = libraryRepository.getPrimaryFileSha256(editionId).orEmpty().ifBlank { editionId }
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
                    lastScale = pdfLoc?.viewportScale ?: saved?.viewportScale ?: 1f
                    lastScrollX = pdfLoc?.scrollX ?: saved?.scrollX ?: 0f
                    lastScrollY = pdfLoc?.scrollY ?: saved?.scrollY ?: 0f
                    loadPage(initialPage, lastScale, lastScrollX, lastScrollY)
                },
                onFailure = { throwable ->
                    _uiState.value = PdfReaderUiState.Error(
                        throwable.message ?: "Unable to open PDF document."
                    )
                }
            )
        }
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch {
            val prevCrop = settings.value.pdfAutoCropMargins
            val prevReflow = settings.value.pdfReflowMode
            preferencesRepository.update(transform)
            val ready = _uiState.value as? PdfReaderUiState.Ready ?: return@launch
            val updated = settings.value
            if (prevCrop != updated.pdfAutoCropMargins || prevReflow != updated.pdfReflowMode) {
                loadPage(ready.currentPage, lastScale, lastScrollX, lastScrollY)
            }
        }
    }

    fun reloadCurrentPageForSettings() {
        val ready = _uiState.value as? PdfReaderUiState.Ready ?: return
        loadPage(ready.currentPage, lastScale, lastScrollX, lastScrollY)
    }

    fun goToPage(pageIndex: Int) {
        if (totalPages <= 0) return
        val clamped = pageIndex.coerceIn(0, totalPages - 1)
        val retainZoom = settings.value.pdfRetainZoomAcrossPages
        if (!retainZoom) {
            lastScale = 1f
            lastScrollX = 0f
            lastScrollY = 0f
        }
        loadPage(clamped, lastScale, lastScrollX, lastScrollY)
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
        lastScale = scale
        lastScrollX = scrollX
        lastScrollY = scrollY
        val editionId = editionIdFlow.value ?: return
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

    fun addHighlight(selectedText: String, note: String) {
        val editionId = editionIdFlow.value ?: return
        val currentPage = (_uiState.value as? PdfReaderUiState.Ready)?.currentPage ?: 0
        val now = System.currentTimeMillis()
        val loc = UnifiedLocator.PdfLocator(currentPage, lastScale, lastScrollX, lastScrollY)
        viewModelScope.launch {
            libraryRepository.saveHighlight(
                HighlightEntity(
                    id = UUID.randomUUID().toString(),
                    editionId = editionId,
                    locatorJson = UnifiedLocator.encode(loc),
                    selectedText = selectedText,
                    note = note,
                    pageNumber = currentPage + 1,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
        }
    }

    fun deleteHighlight(highlightId: String) {
        viewModelScope.launch {
            libraryRepository.deleteHighlight(highlightId)
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
        val editionId = editionIdFlow.value ?: return
        val currentSettings = settings.value
        val previous = _uiState.value as? PdfReaderUiState.Ready
        _uiState.value = PdfReaderUiState.Ready(
            editionId = editionId,
            title = currentTitle,
            currentPage = pageIndex,
            pageCount = totalPages,
            bitmap = previous?.bitmap,
            reflowedText = previous?.reflowedText,
            isRenderingPage = true,
            initialScale = scale,
            initialScrollX = scrollX,
            initialScrollY = scrollY,
            outline = outlineEntries
        )

        viewModelScope.launch {
            if (currentSettings.pdfReflowMode) {
                val textResult = activeSession.extractReflowedPageText(pageIndex)
                textResult.fold(
                    onSuccess = { extracted ->
                        _uiState.value = PdfReaderUiState.Ready(
                            editionId = editionId,
                            title = currentTitle,
                            currentPage = pageIndex,
                            pageCount = totalPages,
                            bitmap = previous?.bitmap,
                            reflowedText = extracted,
                            isRenderingPage = false,
                            initialScale = scale,
                            initialScrollX = scrollX,
                            initialScrollY = scrollY,
                            outline = outlineEntries
                        )
                        updateViewport(pageIndex, scale, scrollX, scrollY)
                        if (_translatedModeEnabled.value) {
                            translateCurrentPdfPage(forceRetranslate = false)
                        }
                    },
                    onFailure = { err ->
                        _uiState.value = PdfReaderUiState.Ready(
                            editionId = editionId,
                            title = currentTitle,
                            currentPage = pageIndex,
                            pageCount = totalPages,
                            bitmap = previous?.bitmap,
                            reflowedText = null,
                            isRenderingPage = false,
                            initialScale = scale,
                            initialScrollX = scrollX,
                            initialScrollY = scrollY,
                            outline = outlineEntries,
                            pageError = err.message ?: "Failed to extract reflow text"
                        )
                    }
                )
            } else {
                val renderResult = activeSession.renderPage(
                    pageIndex = pageIndex,
                    targetWidthPx = targetRenderWidthPx,
                    autoCropMargins = currentSettings.pdfAutoCropMargins
                )
                renderResult.fold(
                    onSuccess = { bitmap ->
                        _uiState.value = PdfReaderUiState.Ready(
                            editionId = editionId,
                            title = currentTitle,
                            currentPage = pageIndex,
                            pageCount = totalPages,
                            bitmap = bitmap,
                            reflowedText = null,
                            isRenderingPage = false,
                            initialScale = scale,
                            initialScrollX = scrollX,
                            initialScrollY = scrollY,
                            outline = outlineEntries
                        )
                        updateViewport(pageIndex, scale, scrollX, scrollY)
                        if (_translatedModeEnabled.value) {
                            translateCurrentPdfPage(forceRetranslate = false)
                        }
                    },
                    onFailure = { err ->
                        _uiState.value = PdfReaderUiState.Ready(
                            editionId = editionId,
                            title = currentTitle,
                            currentPage = pageIndex,
                            pageCount = totalPages,
                            bitmap = previous?.bitmap,
                            reflowedText = null,
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
    }

    private fun translateCurrentPdfPage(forceRetranslate: Boolean) {
        val activeSession = session ?: return
        val editionId = editionIdFlow.value ?: return
        val pageIndex = (_uiState.value as? PdfReaderUiState.Ready)?.currentPage ?: 0
        viewModelScope.launch {
            _isTranslatingPassage.value = true
            _ocrNotice.value = null
            try {
                var pageText = extractCleanPdfPageText(activeSession, pageIndex)
                if (pageText.isBlank()) {
                    val bmp = activeSession.renderPage(pageIndex, targetWidthPx = 1280, autoCropMargins = false).getOrNull()
                    if (bmp != null) {
                        val ocrRaw = translationService.recognizeLatinText(bmp).getOrNull().orEmpty()
                        pageText = SentenceSegmenter.stripHeadersAndFooters(ocrRaw)
                        if (pageText.isNotBlank()) {
                            _ocrNotice.value = "Scanned page detected: extracted text via offline ML Kit OCR."
                        }
                    }
                }
                if (pageText.isBlank()) {
                    _translatedPassage.value = null
                    return@launch
                }
                val result = translatedReadingEngine.translatePassage(
                    editionId = editionId,
                    fileVersionSha256 = fileVersionSha256.ifBlank { editionId },
                    chapterOrSectionId = "pdf_page_$pageIndex",
                    passageText = pageText,
                    targetLang = _targetLanguage.value,
                    enginePreference = _translationEnginePreference.value,
                    forceRetranslate = forceRetranslate
                )
                _translatedPassage.value = result

                // Pre-translate next 2-3 pages asynchronously
                val upcoming = (1..3).mapNotNull { offset ->
                    val nextPage = pageIndex + offset
                    if (nextPage < totalPages) {
                        val text = extractCleanPdfPageText(activeSession, nextPage)
                        if (text.isNotBlank()) "pdf_page_$nextPage" to text else null
                    } else null
                }
                if (upcoming.isNotEmpty()) {
                    translatedReadingEngine.pretranslateUpcomingPages(
                        scope = viewModelScope,
                        editionId = editionId,
                        fileVersionSha256 = fileVersionSha256.ifBlank { editionId },
                        upcomingSections = upcoming,
                        targetLang = _targetLanguage.value,
                        enginePreference = _translationEnginePreference.value
                    )
                }
            } finally {
                _isTranslatingPassage.value = false
            }
        }
    }

    private suspend fun extractCleanPdfPageText(activeSession: MuPdfDocumentSession, pageIndex: Int): String {
        val raw = activeSession.extractReflowedPageText(pageIndex).getOrNull().orEmpty()
        if (raw.isBlank() || raw.startsWith("No selectable text detected")) {
            return ""
        }
        return SentenceSegmenter.stripHeadersAndFooters(raw)
    }

    override fun onCleared() {
        session?.close()
        session = null
        super.onCleared()
    }
}
