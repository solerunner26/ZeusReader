package com.solerunner26.zeusreader.ui.reader

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.DocumentScanner
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.prefs.PdfFitMode
import com.solerunner26.zeusreader.reader.prefs.ReaderThemeMode
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassPillButton
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.ZeusListRow
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.dictionary.DictionaryBottomSheet
import com.solerunner26.zeusreader.ui.passages.SavePassageBottomSheet
import com.solerunner26.zeusreader.ui.theme.ZeusPalette
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.ZeusType
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfReaderScreen(
    editionId: String,
    title: String,
    localPath: String,
    viewModel: PdfReaderViewModel,
    volumeTurnEvents: Flow<Int>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val searchHits by viewModel.searchHits.collectAsStateWithLifecycle()
    val searchQuads by viewModel.searchQuads.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val dictionaryResult by viewModel.dictionaryResult.collectAsStateWithLifecycle()
    val sentenceTranslation by viewModel.sentenceTranslation.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val translatedModeEnabled by viewModel.translatedModeEnabled.collectAsStateWithLifecycle()
    val translatedDisplayMode by viewModel.translatedDisplayMode.collectAsStateWithLifecycle()
    val translationEnginePreference by viewModel.translationEnginePreference.collectAsStateWithLifecycle()
    val translatedPassage by viewModel.translatedPassage.collectAsStateWithLifecycle()
    val isTranslatingPassage by viewModel.isTranslatingPassage.collectAsStateWithLifecycle()
    val ocrNotice by viewModel.ocrNotice.collectAsStateWithLifecycle()
    val bookGlossary by viewModel.bookGlossary.collectAsStateWithLifecycle()
    val passageFolders by viewModel.passageFolders.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    var showTocSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showTypographySheet by remember { mutableStateOf(false) }
    var showHighlightsSheet by remember { mutableStateOf(false) }
    var showDictionarySheet by remember { mutableStateOf(false) }
    var showSavePassageSheet by remember { mutableStateOf(false) }
    var listenText by remember { mutableStateOf<String?>(null) }
    var focusRibbonEnabled by remember { mutableStateOf(false) }
    var ribbonBookmarked by remember { mutableStateOf(false) }
    var activeSearchQuery by remember { mutableStateOf("") }
    var textSelectionMode by remember { mutableStateOf(false) }
    var selectedTextBuffer by remember { mutableStateOf("") }
    var zapWord by remember { mutableStateOf("") }
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }

    val widthPx = LocalWindowInfo.current.containerSize.width.coerceAtLeast(1080)

    LaunchedEffect(editionId, localPath) {
        viewModel.openDocument(editionId = editionId, title = title, localPath = localPath, viewportWidthPx = widthPx)
    }
    LaunchedEffect(settings.pdfAutoCropMargins, settings.pdfReflowMode) {
        viewModel.reloadCurrentPageForSettings()
    }
    LaunchedEffect(settings.rotationLocked) {
        activity?.requestedOrientation = if (settings.rotationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    DisposableEffect(Unit) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    LaunchedEffect(volumeTurnEvents) {
        volumeTurnEvents.collect { delta -> if (delta > 0) viewModel.nextPage() else viewModel.previousPage() }
    }

    val nightInvertFilter = remember(settings.theme) {
        if (settings.theme == ReaderThemeMode.NIGHT || settings.theme == ReaderThemeMode.PURE_BLACK_OLED) {
            // Soft luminance-preserving night inversion for readable text & gentler diagrams
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.88f, 0f, 0f, 0f, 232f,
                        0f, -0.88f, 0f, 0f, 230f,
                        0f, 0f, -0.88f, 0f, 224f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else {
            null
        }
    }

    val ready = uiState as? PdfReaderUiState.Ready
    val pageText = ready?.reflowedText.orEmpty()

    val tools = listOf(
        OrbTool("Lens", Icons.Rounded.Translate, enabled = ready != null, active = translatedModeEnabled) {
            viewModel.toggleTranslatedReadingMode(!translatedModeEnabled)
        },
        OrbTool("Zap a word", Icons.Rounded.Bolt, enabled = ready != null) {
            // Without a chosen word, switch to tappable text so the reader can pick one.
            if (zapWord.isBlank()) textSelectionMode = true
            if (zapWord.isNotBlank()) viewModel.lookupWord(zapWord, selectedTextBuffer)
            showDictionarySheet = true
        },
        OrbTool("Vibe & type", Icons.Rounded.Tune) { showTypographySheet = true },
        OrbTool("Listen", Icons.AutoMirrored.Rounded.VolumeUp, enabled = ready != null) {
            listenText = selectedTextBuffer.ifBlank { pageText }
        },
        OrbTool("Spark it", Icons.Rounded.AutoAwesome, enabled = ready != null) {
            if (selectedTextBuffer.isBlank()) textSelectionMode = true
            showSavePassageSheet = true
        },
        OrbTool("Glow", Icons.Rounded.BorderColor) { showHighlightsSheet = true },
        OrbTool(if (textSelectionMode) "Page view" else "Text & OCR", Icons.Rounded.DocumentScanner, enabled = ready != null, active = textSelectionMode) {
            textSelectionMode = !textSelectionMode
        },
        OrbTool("Outline", Icons.AutoMirrored.Rounded.FormatListBulleted, enabled = ready != null) { showTocSheet = true }
    )
    val fly = ready?.takeIf { it.pageCount > 1 }?.let { r ->
        FlyModel(
            count = r.pageCount,
            current = r.currentPage,
            unitLabel = "pages",
            labelFor = { i -> r.outline.lastOrNull { it.pageIndex <= i }?.title ?: "Page ${i + 1}" },
            onFly = { i -> viewModel.goToPage(i) }
        )
    }

    StormlightReaderFrame(
        title = title,
        meta = ready?.let { r -> r.outline.lastOrNull { it.pageIndex <= r.currentPage }?.title ?: "Page ${r.currentPage + 1}" }.orEmpty(),
        paperColor = settings.theme.backgroundColor,
        onBack = onBack,
        progressFraction = ready?.let { if (it.pageCount > 1) it.currentPage.toFloat() / (it.pageCount - 1) else 1f },
        progressLabel = ready?.let { "p.${it.currentPage + 1} / ${it.pageCount}" } ?: "Opening…",
        tools = tools,
        fly = fly,
        onPrevious = viewModel::previousPage,
        onNext = viewModel::nextPage,
        canGoPrevious = (ready?.currentPage ?: 0) > 0,
        canGoNext = ready != null && ready.currentPage + 1 < ready.pageCount,
        modifier = modifier,
        actions = {
            GlassIconButton(Icons.Rounded.Search, "Search PDF", { showSearchSheet = true }, enabled = ready != null)
        }
    ) {
        val ink = settings.theme.textColor
        when (val state = uiState) {
            PdfReaderUiState.Loading -> Box(Modifier.fillMaxSize().padding(ZeusSpacing.huge), contentAlignment = Alignment.Center) { ZeusLinearProgress() }

            is PdfReaderUiState.Error -> Box(Modifier.fillMaxSize().padding(ZeusSpacing.xxl), contentAlignment = Alignment.Center) {
                Text(state.message, style = ZeusTheme.type.body, color = ink)
            }

            is PdfReaderUiState.Ready -> {
                if (translatedModeEnabled) {
                    TranslatedReadingOverlay(
                        sectionLabel = "Page ${state.currentPage + 1} / ${state.pageCount}",
                        passageResult = translatedPassage,
                        isTranslating = isTranslatingPassage,
                        ocrNotice = ocrNotice,
                        targetLanguage = targetLanguage,
                        displayMode = translatedDisplayMode,
                        enginePreference = translationEnginePreference,
                        glossaryTerms = bookGlossary,
                        settings = settings,
                        onSelectTargetLanguage = { viewModel.setTargetLanguage(it) },
                        onSelectDisplayMode = viewModel::setTranslatedDisplayMode,
                        onSelectEnginePreference = viewModel::setTranslationEnginePreference,
                        onRetranslatePassage = viewModel::retranslateCurrentPassage,
                        onAddGlossaryTerm = viewModel::addGlossaryTerm,
                        onRemoveGlossaryTerm = viewModel::removeGlossaryTerm,
                        onExitTranslatedMode = { viewModel.toggleTranslatedReadingMode(false) }
                    )
                } else if (textSelectionMode || (settings.pdfReflowMode && state.reflowedText != null)) {
                    val baseSp = ZeusType.readingSize(settings.fontSizePercent)
                    val text = state.reflowedText?.takeIf { it.isNotBlank() }
                        ?: "No selectable text on this page yet. Try OCR or another page."
                    val sentences = remember(text) {
                        text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
                    }
                    val hl = settings.theme.textColor
                    val annotated = highlightSearchMatches(
                        text = text,
                        query = activeSearchQuery,
                        highlightBg = ZeusPalette.searchHit.copy(alpha = 0.55f),
                        highlightFg = hl
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = ZeusSpacing.xl, vertical = ZeusSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
                    ) {
                        if (activeSearchQuery.isNotBlank()) {
                            PaperPill("Highlighting “$activeSearchQuery” · tap to clear", true, ink, settings.theme.backgroundColor, { activeSearchQuery = "" })
                        }
                        if (!textSelectionMode) {
                            SelectionContainer {
                                Text(
                                    text = annotated,
                                    fontFamily = settings.fontChoice.toComposeFontFamily(),
                                    fontSize = baseSp,
                                    lineHeight = baseSp * settings.lineHeightMultiplier,
                                    color = ink
                                )
                            }
                        } else {
                            Text("Tap any word to Zap it, or Spark / Glow a sentence.", style = ZeusTheme.type.metaSmall, color = ink.copy(alpha = 0.7f))
                            sentences.take(60).forEach { sentence ->
                                val words = remember(sentence) { sentence.split(Regex("\\s+")).filter { it.isNotBlank() } }
                                val picked = selectedTextBuffer == sentence
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ZeusShapes.tile)
                                        .background(if (picked) ZeusTheme.colors.neonA.copy(alpha = 0.14f) else Color.Transparent)
                                        .padding(ZeusSpacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
                                ) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs)) {
                                        words.forEach { rawWord ->
                                            val cleanWord = rawWord.trim { !it.isLetterOrDigit() && it != '-' }
                                            Text(
                                                text = rawWord,
                                                fontFamily = settings.fontChoice.toComposeFontFamily(),
                                                fontSize = baseSp,
                                                color = ink,
                                                modifier = Modifier
                                                    .clip(ZeusShapes.tiny)
                                                    .clickable(enabled = cleanWord.isNotBlank(), role = Role.Button) {
                                                        selectedTextBuffer = sentence
                                                        zapWord = cleanWord
                                                        viewModel.lookupWord(cleanWord, sentence)
                                                        showDictionarySheet = true
                                                    }
                                                    .padding(horizontal = ZeusSpacing.xxs)
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                                        PaperPill("Spark it", false, ink, settings.theme.backgroundColor, {
                                            selectedTextBuffer = sentence
                                            showSavePassageSheet = true
                                        })
                                        PaperPill("Glow", false, ink, settings.theme.backgroundColor, {
                                            selectedTextBuffer = sentence
                                            viewModel.addHighlight(sentence, "")
                                        })
                                        PaperPill("Select", picked, ink, settings.theme.backgroundColor, {
                                            selectedTextBuffer = if (picked) "" else sentence
                                        })
                                    }
                                }
                            }
                        }
                    }
                } else {
                    var scale by remember(state.currentPage, settings.pdfRetainZoomAcrossPages) { mutableFloatStateOf(state.initialScale) }
                    var offsetX by remember(state.currentPage, settings.pdfRetainZoomAcrossPages) { mutableFloatStateOf(state.initialScrollX) }
                    var offsetY by remember(state.currentPage, settings.pdfRetainZoomAcrossPages) { mutableFloatStateOf(state.initialScrollY) }
                    var swipeDeltaX by remember(state.currentPage) { mutableFloatStateOf(0f) }
                    LaunchedEffect(state.currentPage, activeSearchQuery) { viewModel.refreshSearchQuadsForPage(state.currentPage) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(state.currentPage, scale) {
                                if (scale <= 1.05f) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { swipeDeltaX = 0f },
                                        onHorizontalDrag = { _, dragAmount -> swipeDeltaX += dragAmount },
                                        onDragEnd = {
                                            // Book convention: swipe right-to-left = next page, left-to-right = previous.
                                            if (swipeDeltaX < -80f) viewModel.nextPage() else if (swipeDeltaX > 80f) viewModel.previousPage()
                                            swipeDeltaX = 0f
                                        }
                                    )
                                }
                            }
                            .pointerInput(state.currentPage) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                    scale = nextScale
                                    if (nextScale > 1f) {
                                        val maxX = size.width * (nextScale - 1f) / 2f
                                        val maxY = size.height * (nextScale - 1f) / 2f
                                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                    viewModel.updateViewport(state.currentPage, scale, offsetX, offsetY)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        state.bitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF page ${state.currentPage + 1}",
                                contentScale = if (settings.pdfFitMode == PdfFitMode.FIT_WIDTH) ContentScale.FillWidth else ContentScale.Fit,
                                colorFilter = nightInvertFilter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                            )
                            if (searchQuads.isNotEmpty()) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                                ) {
                                    val bmpAspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat()
                                    val boxAspect = size.width / size.height.coerceAtLeast(1f)
                                    val drawW: Float
                                    val drawH: Float
                                    if (settings.pdfFitMode == PdfFitMode.FIT_WIDTH || bmpAspect > boxAspect) {
                                        drawW = size.width
                                        drawH = size.width / bmpAspect
                                    } else {
                                        drawH = size.height
                                        drawW = size.height * bmpAspect
                                    }
                                    val left = (size.width - drawW) / 2f
                                    val top = (size.height - drawH) / 2f
                                    searchQuads.forEach { q ->
                                        drawRect(
                                            color = ZeusPalette.searchHit.copy(alpha = 0.45f),
                                            topLeft = Offset(left + q.left * drawW, top + q.top * drawH),
                                            size = Size(((q.right - q.left) * drawW).coerceAtLeast(6f), ((q.bottom - q.top) * drawH).coerceAtLeast(6f))
                                        )
                                    }
                                }
                            }
                        }
                        if (state.isRenderingPage) {
                            ZeusLinearProgress(modifier = Modifier.align(Alignment.TopCenter).padding(ZeusSpacing.lg))
                        }
                        if (scale <= 1.05f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(ZeusSizes.edgeZoneWidth)
                                    .fillMaxHeight()
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = viewModel::previousPage)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(ZeusSizes.edgeZoneWidth)
                                    .fillMaxHeight()
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = viewModel::nextPage)
                            )
                        }
                    }
                }

                if (settings.edgeBrightnessSwipe && activity != null && !textSelectionMode && !translatedModeEnabled) {
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
                    FocusRibbonOverlay(enabled = focusRibbonEnabled, bandFraction = 0.45f, surfaceBackground = settings.theme.backgroundColor)
                    if (highlights.isNotEmpty()) {
                        MarginMarkBar(onClick = { showHighlightsSheet = true }, modifier = Modifier.align(Alignment.CenterStart))
                    }
                    if (!textSelectionMode) {
                        ForeEdgeProgress(
                            progressFraction = if (state.pageCount > 1) state.currentPage.toFloat() / (state.pageCount - 1).toFloat() else 0f,
                            onSeekFraction = { fraction ->
                                viewModel.goToPage((fraction * (state.pageCount - 1)).toInt().coerceIn(0, (state.pageCount - 1).coerceAtLeast(0)))
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                    RibbonBookmark(
                        isBookmarked = ribbonBookmarked,
                        chromeVisible = true,
                        onToggleBookmark = { ribbonBookmarked = !ribbonBookmarked },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = ZeusSpacing.huge)
                    )
                }
            }
        }
        WarmLightOverlay(settings.warmLightStrength)
    }

    if (showTocSheet && ready != null) {
        ZeusBottomSheet(onDismissRequest = { showTocSheet = false }, title = "Outline", subtitle = "${ready.pageCount} pages") {
            if (ready.outline.isEmpty()) {
                Text("No embedded outline in this PDF — drag the orb to fly through pages.", style = ZeusTheme.type.body, color = ZeusTheme.colors.textOnSkySecondary)
            } else {
                ready.outline.forEach { entry ->
                    ZeusListRow(
                        title = entry.title,
                        modifier = Modifier.padding(start = ZeusSpacing.md * entry.depth.coerceAtMost(3)),
                        onClick = {
                            viewModel.goToPage(entry.pageIndex)
                            showTocSheet = false
                        }
                    ) {
                        MetaLabel(text = "p.${entry.pageIndex + 1}", accent = entry.pageIndex <= ready.currentPage)
                    }
                }
            }
        }
    }

    if (showSearchSheet && ready != null) {
        var query by remember { mutableStateOf(activeSearchQuery) }
        ZeusBottomSheet(onDismissRequest = { showSearchSheet = false }, title = "Search PDF") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                ZeusTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search text…",
                    leadingIcon = Icons.Rounded.Search,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        activeSearchQuery = query.trim()
                        viewModel.search(query)
                    }),
                    modifier = Modifier.weight(1f)
                )
                GlassPillButton(label = "Find", enabled = query.isNotBlank(), onClick = {
                    activeSearchQuery = query.trim()
                    viewModel.search(query)
                })
            }
            if (isSearching) ZeusLinearProgress()
            searchHits.forEach { hit ->
                ZeusListRow(
                    title = "Page ${hit.pageIndex + 1}",
                    onClick = {
                        activeSearchQuery = query.trim()
                        viewModel.goToPage(hit.pageIndex)
                        showSearchSheet = false
                    }
                ) {
                    MetaLabel(text = "${hit.matchCount} match${if (hit.matchCount == 1) "" else "es"}", accent = true)
                }
            }
        }
    }

    if (showTypographySheet) {
        TypographyBottomSheet(
            settings = settings,
            isPdfMode = true,
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
            defaultSnippet = selectedTextBuffer.ifBlank { pageText.take(160) },
            onAddHighlight = viewModel::addHighlight,
            onDeleteHighlight = viewModel::deleteHighlight,
            onDismiss = { showHighlightsSheet = false }
        )
    }

    if (showDictionarySheet) {
        val ctxSentence = selectedTextBuffer
        DictionaryBottomSheet(
            initialWord = zapWord,
            sentenceContext = ctxSentence,
            lookupResult = dictionaryResult,
            sentenceTranslation = sentenceTranslation,
            selectedTargetLang = targetLanguage,
            onSelectTargetLang = { viewModel.setTargetLanguage(it, ctxSentence) },
            onLookupWord = {
                zapWord = it
                viewModel.lookupWord(it, ctxSentence)
            },
            onSaveToVocab = { res, lang -> viewModel.saveLookupToVocabulary(res, lang, ctxSentence) },
            onDismiss = { showDictionarySheet = false }
        )
    }

    listenText?.let { text ->
        ListenSheet(
            title = title,
            sectionLabel = ready?.let { "Page ${it.currentPage + 1}" } ?: "",
            text = text,
            onDismiss = { listenText = null }
        )
    }

    if (showSavePassageSheet) {
        val quote = selectedTextBuffer
        SavePassageBottomSheet(
            quotePreview = quote.ifBlank { "Pick a sentence on the page (Text & OCR view), then Spark it." },
            folders = passageFolders,
            onCreateFolder = viewModel::createPassageFolder,
            onSave = { folderId, note, _ ->
                if (quote.isNotBlank()) {
                    viewModel.savePassage(text = quote, originalText = quote, language = "en", folderId = folderId, note = note)
                }
                selectedTextBuffer = ""
                showSavePassageSheet = false
            },
            onDismiss = { showSavePassageSheet = false }
        )
    }
}

private fun highlightSearchMatches(
    text: String,
    query: String,
    highlightBg: Color,
    highlightFg: Color
): AnnotatedString {
    val q = query.trim()
    if (q.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        var startIndex = 0
        while (startIndex < text.length) {
            val found = text.indexOf(q, startIndex = startIndex, ignoreCase = true)
            if (found < 0) break
            addStyle(SpanStyle(background = highlightBg, color = highlightFg), start = found, end = found + q.length)
            startIndex = found + q.length
        }
    }
}
