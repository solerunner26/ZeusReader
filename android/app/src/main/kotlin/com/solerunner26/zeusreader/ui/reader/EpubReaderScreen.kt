package com.solerunner26.zeusreader.ui.reader

import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.epub.EpubSearchResultItem
import com.solerunner26.zeusreader.reader.epub.EpubTocItem
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassPillButton
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.ZeusListRow
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.dictionary.DictionaryBottomSheet
import com.solerunner26.zeusreader.ui.passages.SavePassageBottomSheet
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment

@Composable
fun EpubReaderScreen(
    editionId: String,
    title: String,
    localPath: String,
    activity: FragmentActivity,
    viewModel: EpubReaderViewModel,
    volumeTurnEvents: Flow<Int>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val translatedModeEnabled by viewModel.translatedModeEnabled.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val translatedDisplayMode by viewModel.translatedDisplayMode.collectAsStateWithLifecycle()
    val translationEnginePreference by viewModel.translationEnginePreference.collectAsStateWithLifecycle()
    val translatedPassage by viewModel.translatedPassage.collectAsStateWithLifecycle()
    val isTranslatingPassage by viewModel.isTranslatingPassage.collectAsStateWithLifecycle()
    val bookGlossary by viewModel.bookGlossary.collectAsStateWithLifecycle()
    val dictionaryResult by viewModel.dictionaryResult.collectAsStateWithLifecycle()
    val sentenceTranslation by viewModel.sentenceTranslation.collectAsStateWithLifecycle()
    val passageFolders by viewModel.passageFolders.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var navigatorRef by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showTypographySheet by remember { mutableStateOf(false) }
    var showHighlightsSheet by remember { mutableStateOf(false) }
    var showDictionarySheet by remember { mutableStateOf(false) }
    var showSavePassageSheet by remember { mutableStateOf(false) }
    var listenText by remember { mutableStateOf<String?>(null) }
    var selectionSnippet by remember { mutableStateOf("") }
    var zapWord by remember { mutableStateOf("") }
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }
    var focusRibbonEnabled by remember { mutableStateOf(false) }
    var ribbonBookmarked by remember { mutableStateOf(false) }

    LaunchedEffect(editionId, localPath) {
        viewModel.openBook(editionId = editionId, title = title, localPath = localPath)
    }
    LaunchedEffect(settings.rotationLocked) {
        activity.requestedOrientation = if (settings.rotationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    DisposableEffect(Unit) {
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    LaunchedEffect(navigatorRef) {
        val nav = navigatorRef ?: return@LaunchedEffect
        nav.currentLocator.collect { locator -> viewModel.onLocatorChanged(locator) }
    }
    // Apply Readium preferences & preserve exact passage anchor on font/theme changes
    LaunchedEffect(navigatorRef, settings) {
        val nav = navigatorRef ?: return@LaunchedEffect
        val anchor = nav.currentLocator.value
        nav.submitPreferences(viewModel.toReadiumPreferences(settings))
        nav.go(anchor, animated = false)
    }
    LaunchedEffect(navigatorRef, volumeTurnEvents) {
        volumeTurnEvents.collect { delta ->
            val nav = navigatorRef ?: return@collect
            if (delta > 0) nav.goForward(animated = true) else nav.goBackward(animated = true)
        }
    }

    /** Reads the current Readium selection (or empty). */
    suspend fun currentSelectionText(): String =
        runCatching { navigatorRef?.currentSelection()?.locator?.text?.highlight.orEmpty() }.getOrDefault("").trim()

    val ready = uiState as? EpubReaderUiState.Ready
    val tools = listOf(
        OrbTool("Lens", Icons.Rounded.Translate, enabled = ready != null, active = translatedModeEnabled) {
            viewModel.toggleTranslatedReadingMode(!translatedModeEnabled)
        },
        OrbTool("Zap a word", Icons.Rounded.Bolt, enabled = ready != null) {
            scope.launch {
                val sel = currentSelectionText()
                selectionSnippet = sel
                val words = sel.split(Regex("\\s+")).filter { it.isNotBlank() }
                zapWord = words.firstOrNull()?.trim { !it.isLetterOrDigit() && it != '-' }.orEmpty()
                if (zapWord.isNotBlank()) viewModel.lookupWord(zapWord, if (words.size > 1) sel else "")
                showDictionarySheet = true
            }
        },
        OrbTool("Vibe & type", Icons.Rounded.Tune) { showTypographySheet = true },
        OrbTool("Listen", Icons.AutoMirrored.Rounded.VolumeUp, enabled = ready != null) {
            scope.launch {
                listenText = currentSelectionText().ifBlank { viewModel.currentSectionPlainText() }
            }
        },
        OrbTool("Spark it", Icons.Rounded.AutoAwesome, enabled = ready != null) {
            scope.launch {
                selectionSnippet = currentSelectionText()
                showSavePassageSheet = true
            }
        },
        OrbTool("Glow", Icons.Rounded.BorderColor) {
            scope.launch {
                selectionSnippet = currentSelectionText()
                showHighlightsSheet = true
            }
        },
        OrbTool("Contents", Icons.AutoMirrored.Rounded.FormatListBulleted, enabled = ready != null) { showTocSheet = true }
    )
    val fly = ready?.tocItems?.takeIf { it.size > 1 }?.let { toc ->
        val currentIdx = toc.indexOfLast { it.title == ready.currentChapterTitle }.coerceAtLeast(0)
        FlyModel(
            count = toc.size,
            current = currentIdx,
            unitLabel = "chapters",
            labelFor = { i -> toc.getOrNull(i)?.title.orEmpty() },
            onFly = { i -> toc.getOrNull(i)?.let { navigatorRef?.go(it.link, animated = true) } }
        )
    }

    StormlightReaderFrame(
        title = title,
        meta = ready?.currentChapterTitle.orEmpty(),
        paperColor = settings.theme.backgroundColor,
        onBack = onBack,
        progressFraction = ready?.currentProgressionPercent?.let { it / 100f },
        progressLabel = ready?.let { "${it.currentProgressionPercent}% · drag the orb to fly" } ?: "Opening…",
        tools = tools,
        fly = fly,
        onPrevious = { navigatorRef?.goBackward(animated = true) },
        onNext = { navigatorRef?.goForward(animated = true) },
        modifier = modifier,
        actions = {
            GlassIconButton(Icons.Rounded.Search, "Search in book", { showSearchSheet = true }, enabled = ready?.capabilities?.search == true)
        }
    ) {
        when (val state = uiState) {
            EpubReaderUiState.Loading -> Box(Modifier.fillMaxSize().padding(ZeusSpacing.huge), contentAlignment = Alignment.Center) {
                ZeusLinearProgress()
            }

            is EpubReaderUiState.Error -> Box(Modifier.fillMaxSize().padding(ZeusSpacing.xxl), contentAlignment = Alignment.Center) {
                Text(state.message, style = ZeusTheme.type.body, color = settings.theme.textColor)
            }

            is EpubReaderUiState.Ready -> {
                val containerId = remember { View.generateViewId() }
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = ZeusSpacing.md),
                    factory = { ctx ->
                        FragmentContainerView(ctx).apply {
                            id = containerId
                            doOnAttach {
                                val fm = activity.supportFragmentManager
                                val existing = fm.findFragmentById(containerId) as? EpubNavigatorFragment
                                if (existing == null) {
                                    val factory = EpubNavigatorFactory(
                                        publication = state.publication,
                                        configuration = EpubNavigatorFactory.Configuration()
                                    )
                                    fm.fragmentFactory = factory.createFragmentFactory(
                                        initialLocator = state.initialLocator,
                                        initialPreferences = viewModel.toReadiumPreferences(settings)
                                    )
                                    val fragment = fm.fragmentFactory.instantiate(
                                        ctx.classLoader,
                                        EpubNavigatorFragment::class.java.name
                                    ) as EpubNavigatorFragment
                                    fm.beginTransaction()
                                        .replace(containerId, fragment)
                                        .commitAllowingStateLoss()
                                    navigatorRef = fragment
                                } else {
                                    navigatorRef = existing
                                }
                            }
                        }
                    }
                )

                if (settings.edgeBrightnessSwipe) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(ZeusSizes.brightnessZoneWidth)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    brightnessLevel = (brightnessLevel - dragAmount / 1200f).coerceIn(0.08f, 1f)
                                    val lp = activity.window.attributes
                                    lp.screenBrightness = brightnessLevel
                                    activity.window.attributes = lp
                                }
                            }
                    )
                }

                if (!translatedModeEnabled) {
                    FocusRibbonOverlay(
                        enabled = focusRibbonEnabled,
                        bandFraction = 0.45f,
                        surfaceBackground = settings.theme.backgroundColor
                    )
                    if (highlights.isNotEmpty()) {
                        MarginMarkBar(onClick = { showHighlightsSheet = true }, modifier = Modifier.align(Alignment.CenterStart))
                    }
                    ForeEdgeProgress(
                        progressFraction = state.currentProgressionPercent / 100f,
                        onSeekFraction = { fraction ->
                            val items = state.tocItems
                            if (items.isNotEmpty()) {
                                val idx = (fraction * (items.size - 1)).toInt().coerceIn(0, items.lastIndex)
                                navigatorRef?.go(items[idx].link, animated = true)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    RibbonBookmark(
                        isBookmarked = ribbonBookmarked,
                        chromeVisible = true,
                        onToggleBookmark = { ribbonBookmarked = !ribbonBookmarked },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = ZeusSpacing.huge)
                    )
                } else {
                    TranslatedReadingOverlay(
                        sectionLabel = state.currentChapterTitle.ifBlank { "Current chapter" },
                        passageResult = translatedPassage,
                        isTranslating = isTranslatingPassage,
                        ocrNotice = null,
                        targetLanguage = targetLanguage,
                        displayMode = translatedDisplayMode,
                        enginePreference = translationEnginePreference,
                        glossaryTerms = bookGlossary,
                        settings = settings,
                        onSelectTargetLanguage = viewModel::setTargetLanguage,
                        onSelectDisplayMode = viewModel::setTranslatedDisplayMode,
                        onSelectEnginePreference = viewModel::setTranslationEnginePreference,
                        onRetranslatePassage = viewModel::retranslateCurrentPassage,
                        onAddGlossaryTerm = viewModel::addGlossaryTerm,
                        onRemoveGlossaryTerm = viewModel::removeGlossaryTerm,
                        onExitTranslatedMode = { viewModel.toggleTranslatedReadingMode(false) }
                    )
                }
            }
        }
        WarmLightOverlay(settings.warmLightStrength)
    }

    if (showTocSheet && ready != null) {
        EpubTocBottomSheet(
            items = ready.tocItems,
            currentTitle = ready.currentChapterTitle,
            onDismiss = { showTocSheet = false },
            onSelect = { item ->
                navigatorRef?.go(item.link, animated = true)
                showTocSheet = false
            }
        )
    }
    if (showSearchSheet && ready != null) {
        EpubSearchBottomSheet(
            results = searchResults,
            isSearching = isSearching,
            onSearch = viewModel::search,
            onDismiss = { showSearchSheet = false },
            onSelect = { match ->
                navigatorRef?.go(match.locator, animated = true)
                showSearchSheet = false
            }
        )
    }
    if (showTypographySheet) {
        TypographyBottomSheet(
            settings = settings,
            isPdfMode = false,
            focusRibbon = focusRibbonEnabled,
            onToggleFocusRibbon = { focusRibbonEnabled = it },
            onSettingsChange = viewModel::updateSettings,
            onDismiss = { showTypographySheet = false }
        )
    }
    if (showHighlightsSheet) {
        HighlightsBottomSheet(
            bookTitle = title,
            highlights = highlights,
            defaultSnippet = selectionSnippet,
            onAddHighlight = viewModel::addHighlight,
            onDeleteHighlight = viewModel::deleteHighlight,
            onDismiss = { showHighlightsSheet = false }
        )
    }
    if (showDictionarySheet) {
        DictionaryBottomSheet(
            initialWord = zapWord,
            sentenceContext = selectionSnippet,
            lookupResult = dictionaryResult,
            sentenceTranslation = sentenceTranslation,
            selectedTargetLang = targetLanguage,
            onSelectTargetLang = { viewModel.setTargetLanguage(it); viewModel.translateSentence(selectionSnippet, it) },
            onLookupWord = { viewModel.lookupWord(it, selectionSnippet) },
            onSaveToVocab = { res, lang -> viewModel.saveLookupToVocabulary(res, lang, selectionSnippet) },
            onDismiss = { showDictionarySheet = false }
        )
    }
    listenText?.let { text ->
        ListenSheet(
            title = title,
            sectionLabel = ready?.currentChapterTitle?.ifBlank { "This chapter" } ?: "This chapter",
            text = text,
            onDismiss = { listenText = null }
        )
    }
    if (showSavePassageSheet) {
        val quote = selectionSnippet.ifBlank { ready?.currentChapterTitle.orEmpty() }
        SavePassageBottomSheet(
            quotePreview = quote.ifBlank { "Select text on the page first, then tap Spark it." },
            folders = passageFolders,
            onCreateFolder = viewModel::createPassageFolder,
            onSave = { folderId, note, _ ->
                if (quote.isNotBlank()) {
                    viewModel.savePassage(
                        text = quote,
                        originalText = quote,
                        language = if (translatedModeEnabled) targetLanguage.code else "en",
                        folderId = folderId,
                        note = note
                    )
                }
                showSavePassageSheet = false
            },
            onDismiss = { showSavePassageSheet = false }
        )
    }
}

@Composable
private fun EpubTocBottomSheet(
    items: List<EpubTocItem>,
    currentTitle: String,
    onDismiss: () -> Unit,
    onSelect: (EpubTocItem) -> Unit
) {
    ZeusBottomSheet(onDismissRequest = onDismiss, title = "Contents", subtitle = "${items.size} chapters") {
        if (items.isEmpty()) {
            Text("No table of contents in this EPUB.", style = ZeusTheme.type.body, color = ZeusTheme.colors.textOnSkySecondary)
        } else {
            items.forEachIndexed { index, item ->
                ZeusListRow(
                    title = item.title,
                    modifier = Modifier.padding(start = ZeusSpacing.md * item.depth.coerceAtMost(3)),
                    onClick = { onSelect(item) },
                    leading = { MetaLabel(text = "${index + 1}".padStart(2, '0'), accent = item.title == currentTitle) }
                )
            }
        }
    }
}

@Composable
private fun EpubSearchBottomSheet(
    results: List<EpubSearchResultItem>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (EpubSearchResultItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    ZeusBottomSheet(onDismissRequest = onDismiss, title = "Search in book") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
            ZeusTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search passage…",
                leadingIcon = Icons.Rounded.Search,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch(query) }),
                modifier = Modifier.weight(1f)
            )
            GlassPillButton(label = "Find", onClick = { onSearch(query) }, enabled = query.isNotBlank())
        }
        if (isSearching) ZeusLinearProgress()
        results.forEach { match ->
            ZeusListRow(title = match.chapterTitle, subtitle = match.snippet, onClick = { onSelect(match) }, modifier = Modifier.fillMaxWidth())
        }
    }
}
