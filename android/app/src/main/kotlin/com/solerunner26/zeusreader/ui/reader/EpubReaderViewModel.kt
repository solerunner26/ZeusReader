package com.solerunner26.zeusreader.ui.reader

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.PassageRepository
import com.solerunner26.zeusreader.data.local.BookGlossaryEntity
import com.solerunner26.zeusreader.data.local.HighlightEntity
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import com.solerunner26.zeusreader.data.local.ReadingProgressEntity
import com.solerunner26.zeusreader.ui.theme.ActiveCoverHueHolder
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ml.TranslationService
import com.solerunner26.zeusreader.dictionary.DictionaryLookupResult
import com.solerunner26.zeusreader.dictionary.OfflineDictionaryRepository
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.reader.core.ReaderCapabilities
import com.solerunner26.zeusreader.reader.core.UnifiedLocator
import com.solerunner26.zeusreader.reader.epub.EpubSearchResultItem
import com.solerunner26.zeusreader.reader.epub.EpubTocItem
import com.solerunner26.zeusreader.reader.epub.ReadiumEpubSession
import com.solerunner26.zeusreader.reader.prefs.ReaderPreferencesRepository
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import com.solerunner26.zeusreader.reader.prefs.ReaderThemeMode
import com.solerunner26.zeusreader.translation.TranslatedDisplayMode
import com.solerunner26.zeusreader.translation.TranslatedPassageResult
import com.solerunner26.zeusreader.translation.TranslatedReadingEngine
import com.solerunner26.zeusreader.translation.TranslationEnginePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.FontFamily as ReadiumFontFamily
import org.readium.r2.navigator.preferences.Theme as ReadiumTheme
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.util.UUID
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
        val currentProgressionPercent: Int,
        val currentChapterTitle: String
    ) : EpubReaderUiState

    data class Error(val message: String) : EpubReaderUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EpubReaderViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val preferencesRepository: ReaderPreferencesRepository,
    private val translatedReadingEngine: TranslatedReadingEngine,
    private val passageRepository: PassageRepository,
    private val activeCoverHueHolder: ActiveCoverHueHolder,
    private val dictionaryRepository: OfflineDictionaryRepository,
    private val translationService: TranslationService
) : ViewModel() {

    private var currentBookTitle: String = ""

    private val _dictionaryResult = MutableStateFlow<DictionaryLookupResult?>(null)
    val dictionaryResult: StateFlow<DictionaryLookupResult?> = _dictionaryResult.asStateFlow()

    private val _sentenceTranslation = MutableStateFlow<String?>(null)
    val sentenceTranslation: StateFlow<String?> = _sentenceTranslation.asStateFlow()

    /** Zap: offline word lookup (+ optional sentence translation) for EPUB selections. */
    fun lookupWord(word: String, sentenceContext: String = "") {
        _dictionaryResult.value = null
        viewModelScope.launch {
            _dictionaryResult.value = dictionaryRepository.lookup(word)
            if (sentenceContext.isNotBlank()) {
                _sentenceTranslation.value = translationService
                    .translateFromEnglish(sentenceContext, _targetLanguage.value)
                    .getOrNull()
            }
        }
    }

    fun translateSentence(sentence: String, lang: StudyTargetLanguage) {
        _targetLanguage.value = lang
        if (sentence.isBlank()) return
        _sentenceTranslation.value = null
        viewModelScope.launch {
            _sentenceTranslation.value = translationService.translateFromEnglish(sentence, lang).getOrNull()
        }
    }

    fun saveLookupToVocabulary(result: DictionaryLookupResult, lang: StudyTargetLanguage, sentenceContext: String) {
        val now = System.currentTimeMillis()
        val gloss = if (lang == StudyTargetLanguage.HINDI) {
            result.hindiGlosses.joinToString("; ") { it.gloss }
        } else {
            result.gujaratiGlosses.joinToString("; ") { it.gloss }
        }
        viewModelScope.launch {
            libraryRepository.saveVocabularyWord(
                VocabWordEntity(
                    id = UUID.randomUUID().toString(),
                    word = result.queryWord,
                    lemma = result.matchedLemma,
                    targetLang = lang.code,
                    englishMeaning = result.englishSenses.firstOrNull()?.definition.orEmpty(),
                    translatedMeaning = gloss,
                    bookTitle = currentBookTitle,
                    editionId = editionIdFlow.value,
                    sentenceContext = sentenceContext,
                    nextReviewEpochMs = now,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
        }
    }

    /** Listen: plain text of the section currently on screen. */
    suspend fun currentSectionPlainText(): String =
        runCatching { session?.extractSectionPlainText(latestLocator?.href?.toString()).orEmpty() }.getOrDefault("")

    private var session: ReadiumEpubSession? = null
    private val editionIdFlow = MutableStateFlow<String?>(null)
    private var fileVersionSha256: String = ""
    private var latestLocator: Locator? = null

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

    private val _uiState = MutableStateFlow<EpubReaderUiState>(EpubReaderUiState.Loading)
    val uiState: StateFlow<EpubReaderUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<EpubSearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<EpubSearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

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

    private val _targetLanguage = MutableStateFlow(StudyTargetLanguage.HINDI)
    val targetLanguage: StateFlow<StudyTargetLanguage> = _targetLanguage.asStateFlow()

    private val _translatedDisplayMode = MutableStateFlow(TranslatedDisplayMode.INTERLINEAR)
    val translatedDisplayMode: StateFlow<TranslatedDisplayMode> = _translatedDisplayMode.asStateFlow()

    private val _translationEnginePreference = MutableStateFlow(TranslationEnginePreference.ON_DEVICE_MLKIT)
    val translationEnginePreference: StateFlow<TranslationEnginePreference> = _translationEnginePreference.asStateFlow()

    private val _translatedPassage = MutableStateFlow<TranslatedPassageResult?>(null)
    val translatedPassage: StateFlow<TranslatedPassageResult?> = _translatedPassage.asStateFlow()

    private val _isTranslatingPassage = MutableStateFlow(false)
    val isTranslatingPassage: StateFlow<Boolean> = _isTranslatingPassage.asStateFlow()

    fun openBook(editionId: String, title: String, localPath: String) {
        if (editionIdFlow.value == editionId && session?.publication != null) return
        editionIdFlow.value = editionId
        currentBookTitle = title
        session?.close()
        val newSession = ReadiumEpubSession(context)
        session = newSession

        viewModelScope.launch {
            _uiState.value = EpubReaderUiState.Loading
            fileVersionSha256 = libraryRepository.getPrimaryFileSha256(editionId).orEmpty().ifBlank { editionId }
            val openResult = newSession.open(localPath)
            openResult.fold(
                onSuccess = { pub ->
                    val saved = libraryRepository.getReadingProgress(editionId)
                    val unified = UnifiedLocator.decode(saved?.locatorJson) as? UnifiedLocator.EpubLocator
                    val restoredLocator = unified?.readiumLocatorJson?.let { raw ->
                        runCatching { Locator.fromJSON(JSONObject(raw)) }.getOrNull()
                    }
                    latestLocator = restoredLocator
                    val pct = ((restoredLocator?.locations?.totalProgression ?: 0.0) * 100.0).toInt()
                    _uiState.value = EpubReaderUiState.Ready(
                        editionId = editionId,
                        title = title,
                        publication = pub,
                        initialLocator = restoredLocator,
                        tocItems = newSession.flattenedToc(),
                        capabilities = newSession.capabilities,
                        currentProgressionPercent = pct,
                        currentChapterTitle = restoredLocator?.title.orEmpty()
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

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch {
            preferencesRepository.update(transform)
        }
    }

    fun toReadiumPreferences(settings: ReaderSettings): EpubPreferences {
        val readiumTheme = when (settings.theme) {
            ReaderThemeMode.DAY, ReaderThemeMode.E_INK -> ReadiumTheme.LIGHT
            ReaderThemeMode.SEPIA -> ReadiumTheme.SEPIA
            ReaderThemeMode.NIGHT, ReaderThemeMode.PURE_BLACK_OLED -> ReadiumTheme.DARK
        }
        return EpubPreferences(
            backgroundColor = ReadiumColor(settings.theme.backgroundColor.toArgb()),
            textColor = ReadiumColor(settings.theme.textColor.toArgb()),
            fontFamily = ReadiumFontFamily(settings.fontChoice.cssName),
            fontSize = settings.fontSizePercent / 100.0,
            lineHeight = settings.lineHeightMultiplier.toDouble(),
            pageMargins = settings.horizontalMarginDp / 20.0,
            publisherStyles = false,
            theme = readiumTheme
        )
    }

    fun onLocatorChanged(locator: Locator) {
        val editionId = editionIdFlow.value ?: return
        latestLocator = locator
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
                currentProgressionPercent = ((totalProg ?: 0.0) * 100.0).toInt(),
                currentChapterTitle = locator.title.orEmpty()
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
        if (_translatedModeEnabled.value) {
            translateCurrentChapter(forceRetranslate = false)
        }
    }

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
        val loc = latestLocator
        val ready = _uiState.value as? EpubReaderUiState.Ready
        viewModelScope.launch {
            passageRepository.savePassage(
                bookId = editionId,
                editionId = editionId,
                folderId = folderId,
                text = text,
                originalText = originalText,
                locatorJson = loc?.toJSON()?.toString().orEmpty(),
                pageLabel = loc?.locations?.progression?.let { "${(it * 100).toInt()}%" },
                chapterTitle = loc?.title ?: ready?.title,
                language = language,
                note = note
            )
        }
    }

    fun addHighlight(selectedText: String, note: String) {
        val editionId = editionIdFlow.value ?: return
        val now = System.currentTimeMillis()
        val loc = latestLocator
        viewModelScope.launch {
            libraryRepository.saveHighlight(
                HighlightEntity(
                    id = UUID.randomUUID().toString(),
                    editionId = editionId,
                    locatorJson = loc?.toJSON()?.toString().orEmpty(),
                    selectedText = selectedText,
                    note = note,
                    chapterTitle = loc?.title.orEmpty(),
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
                _searchResults.value = active.search(query)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun toggleTranslatedReadingMode(enabled: Boolean) {
        _translatedModeEnabled.value = enabled
        if (enabled) {
            translateCurrentChapter(forceRetranslate = false)
        }
    }

    fun setTargetLanguage(lang: StudyTargetLanguage) {
        _targetLanguage.value = lang
        if (_translatedModeEnabled.value) {
            translateCurrentChapter(forceRetranslate = false)
        }
    }

    fun setTranslatedDisplayMode(mode: TranslatedDisplayMode) {
        _translatedDisplayMode.value = mode
    }

    fun setTranslationEnginePreference(pref: TranslationEnginePreference) {
        _translationEnginePreference.value = pref
        if (_translatedModeEnabled.value) {
            translateCurrentChapter(forceRetranslate = false)
        }
    }

    fun retranslateCurrentPassage() {
        translateCurrentChapter(forceRetranslate = true)
    }

    fun addGlossaryTerm(term: String) {
        val editionId = editionIdFlow.value ?: return
        viewModelScope.launch {
            libraryRepository.addGlossaryTerm(editionId, term)
            if (_translatedModeEnabled.value) {
                translateCurrentChapter(forceRetranslate = false)
            }
        }
    }

    fun removeGlossaryTerm(termId: String) {
        val editionId = editionIdFlow.value ?: return
        viewModelScope.launch {
            libraryRepository.deleteGlossaryTerm(editionId, termId)
            if (_translatedModeEnabled.value) {
                translateCurrentChapter(forceRetranslate = false)
            }
        }
    }

    private fun translateCurrentChapter(forceRetranslate: Boolean) {
        val active = session ?: return
        val editionId = editionIdFlow.value ?: return
        val href = latestLocator?.href?.toString()
        val sectionId = href ?: "epub_section_0"
        viewModelScope.launch {
            _isTranslatingPassage.value = true
            try {
                val sectionText = active.extractSectionPlainText(href)
                if (sectionText.isBlank()) {
                    _translatedPassage.value = null
                    return@launch
                }
                val result = translatedReadingEngine.translatePassage(
                    editionId = editionId,
                    fileVersionSha256 = fileVersionSha256.ifBlank { editionId },
                    chapterOrSectionId = sectionId,
                    passageText = sectionText,
                    targetLang = _targetLanguage.value,
                    enginePreference = _translationEnginePreference.value,
                    forceRetranslate = forceRetranslate
                )
                _translatedPassage.value = result

                val upcoming = active.extractUpcomingSectionPlainTexts(href, count = 3)
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

    override fun onCleared() {
        session?.close()
        session = null
        super.onCleared()
    }
}
