package com.solerunner26.zeusreader.ui.reader

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.reader.prefs.PdfFitMode
import com.solerunner26.zeusreader.reader.prefs.ReaderThemeMode
import com.solerunner26.zeusreader.ui.dictionary.DictionaryBottomSheet
import com.solerunner26.zeusreader.ui.passages.SavePassageBottomSheet
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
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
    val activity = LocalActivity.current

    var showTocSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showTypographySheet by remember { mutableStateOf(false) }
    var showHighlightsSheet by remember { mutableStateOf(false) }
    var showDictionarySheet by remember { mutableStateOf(false) }
    var focusRibbonEnabled by remember { mutableStateOf(false) }
    var ribbonBookmarked by remember { mutableStateOf(false) }
    var showSavePassageSheet by remember { mutableStateOf(false) }
    var activeSearchQuery by remember { mutableStateOf("") }
    var textSelectionMode by remember { mutableStateOf(false) }
    var selectedTextBuffer by remember { mutableStateOf("") }
    val passageFolders by viewModel.passageFolders.collectAsStateWithLifecycle()
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }

    val widthPx = LocalWindowInfo.current.containerSize.width.coerceAtLeast(1080)

    LaunchedEffect(editionId, localPath) {
        viewModel.openDocument(
            editionId = editionId,
            title = title,
            localPath = localPath,
            viewportWidthPx = widthPx
        )
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
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(volumeTurnEvents) {
        volumeTurnEvents.collect { delta ->
            if (delta > 0) viewModel.nextPage() else viewModel.previousPage()
        }
    }

    val nightInvertFilter = remember(settings.theme) {
        if (settings.theme == ReaderThemeMode.NIGHT || settings.theme == ReaderThemeMode.PURE_BLACK_OLED) {
            // Soft luminance-preserving night inversion matrix for readable text & gentler diagrams
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = settings.theme.backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = settings.theme.surfaceColor,
                    titleContentColor = settings.theme.textColor,
                    navigationIconContentColor = settings.theme.textColor,
                    actionIconContentColor = settings.theme.textColor
                ),
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back)
                        )
                    }
                },
                actions = {
                    val ready = uiState as? PdfReaderUiState.Ready
                    IconButton(
                        onClick = { showSearchSheet = true },
                        enabled = ready != null,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search PDF")
                    }
                    IconButton(
                        onClick = { showTypographySheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Appearance & PDF layout")
                    }
                }
            )
        },
        bottomBar = {
            val ready = uiState as? PdfReaderUiState.Ready
            if (ready != null) {
                BottomAppBar(
                    containerColor = settings.theme.surfaceColor,
                    contentColor = settings.theme.textColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = viewModel::previousPage,
                            enabled = ready.currentPage > 0,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.reader_prev_page)
                            )
                        }
                        Text(
                            text = "${ready.currentPage + 1}/${ready.pageCount}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(onClick = { showTocSheet = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, contentDescription = "Table of contents")
                        }
                        IconButton(onClick = { showHighlightsSheet = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Highlights & notes")
                        }
                        IconButton(
                            onClick = { viewModel.toggleTranslatedReadingMode(!translatedModeEnabled) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.AutoStories, contentDescription = "Translated reading mode")
                        }
                        IconButton(
                            onClick = {
                                viewModel.lookupWord("capillary", ready.reflowedText?.take(220).orEmpty())
                                showDictionarySheet = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Translate, contentDescription = "Dictionary & translate")
                        }
                        FilterChip(
                            selected = textSelectionMode,
                            onClick = { textSelectionMode = !textSelectionMode },
                            label = { Text(if (textSelectionMode) "PDF" else "OCR") },
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                        IconButton(
                            onClick = viewModel::nextPage,
                            enabled = ready.currentPage + 1 < ready.pageCount,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.reader_next_page)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(settings.theme.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                PdfReaderUiState.Loading -> CircularProgressIndicator()

                is PdfReaderUiState.Error -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
                        val baseSp = (16f * settings.fontSizePercent / 100f).sp
                        val pageText = state.reflowedText?.takeIf { it.isNotBlank() }
                            ?: "No selectable text extracted yet. Tap OCR or switch page."
                        val sentences = remember(pageText) {
                            pageText.split(Regex("(?<=[.!?])\\s+"))
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                        }
                        val annotated = highlightSearchMatches(
                            text = pageText,
                            query = activeSearchQuery,
                            highlightBg = MaterialTheme.colorScheme.primaryContainer,
                            highlightFg = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = settings.horizontalMarginDp.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (activeSearchQuery.isNotBlank()) {
                                AssistChip(
                                    onClick = { activeSearchQuery = "" },
                                    label = { Text("Highlight: \"$activeSearchQuery\" • Tap to clear") }
                                )
                            }
                            SelectionContainer {
                                Text(
                                    text = annotated,
                                    fontFamily = settings.fontChoice.toComposeFontFamily(),
                                    fontSize = baseSp,
                                    lineHeight = baseSp * settings.lineHeightMultiplier,
                                    color = settings.theme.textColor
                                )
                            }
                            if (textSelectionMode && sentences.isNotEmpty()) {
                                Text(
                                    text = "Tap a sentence below to Highlight, Save Passage, or Translate:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                sentences.take(25).forEach { sentence ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedTextBuffer = sentence
                                                showSavePassageSheet = true
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(sentence, style = MaterialTheme.typography.bodyMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                AssistChip(
                                                    onClick = {
                                                        selectedTextBuffer = sentence
                                                        showSavePassageSheet = true
                                                    },
                                                    label = { Text("Save passage") }
                                                )
                                                AssistChip(
                                                    onClick = {
                                                        viewModel.addHighlight(sentence, "")
                                                    },
                                                    label = { Text("Highlight") }
                                                )
                                                AssistChip(
                                                    onClick = {
                                                        val firstWord = sentence.split(Regex("\\s+")).firstOrNull().orEmpty()
                                                        selectedTextBuffer = sentence
                                                        viewModel.lookupWord(firstWord, sentence)
                                                        showDictionarySheet = true
                                                    },
                                                    label = { Text("Define") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        var scale by remember(state.currentPage, settings.pdfRetainZoomAcrossPages) {
                            mutableFloatStateOf(state.initialScale)
                        }
                        var offsetX by remember(state.currentPage, settings.pdfRetainZoomAcrossPages) {
                            mutableFloatStateOf(state.initialScrollX)
                        }
                        var offsetY by remember(state.currentPage, settings.pdfRetainZoomAcrossPages) {
                            mutableFloatStateOf(state.initialScrollY)
                        }

                        var swipeDeltaX by remember(state.currentPage) { mutableFloatStateOf(0f) }
                        LaunchedEffect(state.currentPage, activeSearchQuery) {
                            viewModel.refreshSearchQuadsForPage(state.currentPage)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(state.currentPage, scale) {
                                    if (scale <= 1.05f) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { swipeDeltaX = 0f },
                                            onHorizontalDrag = { _, dragAmount -> swipeDeltaX += dragAmount },
                                            onDragEnd = {
                                                // Left-to-right swipe moves forward; right-to-left moves backward
                                                if (swipeDeltaX > 80f) {
                                                    viewModel.nextPage()
                                                } else if (swipeDeltaX < -80f) {
                                                    viewModel.previousPage()
                                                }
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
                                            offsetX += pan.x
                                            offsetY += pan.y
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
                                    contentScale = if (settings.pdfFitMode == PdfFitMode.FIT_WIDTH) {
                                        ContentScale.FillWidth
                                    } else {
                                        ContentScale.Fit
                                    },
                                    colorFilter = nightInvertFilter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                )
                                if (searchQuads.isNotEmpty()) {
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offsetX,
                                                translationY = offsetY
                                            )
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
                                        val offsetLeft = (size.width - drawW) / 2f
                                        val offsetTop = (size.height - drawH) / 2f
                                        searchQuads.forEach { q ->
                                            drawRect(
                                                color = Color(0xFFFFD54F).copy(alpha = 0.45f),
                                                topLeft = androidx.compose.ui.geometry.Offset(
                                                    x = offsetLeft + q.left * drawW,
                                                    y = offsetTop + q.top * drawH
                                                ),
                                                size = androidx.compose.ui.geometry.Size(
                                                    width = ((q.right - q.left) * drawW).coerceAtLeast(6f),
                                                    height = ((q.bottom - q.top) * drawH).coerceAtLeast(6f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (state.isRenderingPage) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                            if (scale <= 1.05f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(64.dp)
                                        .fillMaxHeight()
                                        .heightIn(min = 48.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = viewModel::previousPage
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(64.dp)
                                        .fillMaxHeight()
                                        .heightIn(min = 48.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = viewModel::nextPage
                                        )
                                )
                            }
                        }
                    }

                    if (settings.edgeBrightnessSwipe && activity != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(28.dp)
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

                    FocusRibbonOverlay(
                        enabled = focusRibbonEnabled,
                        bandFraction = 0.45f,
                        surfaceBackground = settings.theme.backgroundColor
                    )
                    if (highlights.isNotEmpty()) {
                        MarginMarkBar(
                            onClick = { showHighlightsSheet = true },
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                    ForeEdgeProgress(
                        progressFraction = if (state.pageCount > 1) {
                            state.currentPage.toFloat() / (state.pageCount - 1).toFloat()
                        } else {
                            0f
                        },
                        onSeekFraction = { fraction ->
                            val targetPage = (fraction * (state.pageCount - 1)).toInt()
                                .coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
                            viewModel.goToPage(targetPage)
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    RibbonBookmark(
                        isBookmarked = ribbonBookmarked,
                        chromeVisible = true,
                        onToggleBookmark = { ribbonBookmarked = !ribbonBookmarked },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 16.dp)
                    )
                }
            }

            if (settings.warmLightStrength > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFA726).copy(alpha = settings.warmLightStrength))
                )
            }
        }
    }

    val readyState = uiState as? PdfReaderUiState.Ready
    if (showTocSheet && readyState != null) {
        ModalBottomSheet(onDismissRequest = { showTocSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Outline", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                if (readyState.outline.isEmpty()) {
                    Text(
                        "No embedded outline in this PDF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(readyState.outline) { entry ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = (entry.depth * 12).dp)
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        viewModel.goToPage(entry.pageIndex)
                                        showTocSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.title, modifier = Modifier.weight(1f))
                                    Text("p.${entry.pageIndex + 1}")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSearchSheet && readyState != null) {
        var query by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showSearchSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Search PDF", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search text…") }
                    )
                    Button(
                        onClick = {
                            activeSearchQuery = query.trim()
                            viewModel.search(query)
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Find")
                    }
                }
                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchHits) { hit ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    activeSearchQuery = query.trim()
                                    viewModel.goToPage(hit.pageIndex)
                                    showSearchSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Page ${hit.pageIndex + 1}")
                                Text("${hit.matchCount} match(es)")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
        val snippet = selectedTextBuffer.ifBlank { readyState?.reflowedText?.take(160).orEmpty() }
        HighlightsBottomSheet(
            bookTitle = title,
            highlights = highlights,
            defaultSnippet = snippet,
            onAddHighlight = viewModel::addHighlight,
            onDeleteHighlight = viewModel::deleteHighlight,
            onDismiss = { showHighlightsSheet = false }
        )
    }

    if (showDictionarySheet) {
        val ctxSentence = selectedTextBuffer.ifBlank { readyState?.reflowedText?.take(220).orEmpty() }
        DictionaryBottomSheet(
            initialWord = dictionaryResult?.queryWord ?: "capillary",
            sentenceContext = ctxSentence,
            lookupResult = dictionaryResult,
            sentenceTranslation = sentenceTranslation,
            selectedTargetLang = targetLanguage,
            onSelectTargetLang = { viewModel.setTargetLanguage(it, ctxSentence) },
            onLookupWord = { viewModel.lookupWord(it, ctxSentence) },
            onSaveToVocab = { res, lang ->
                viewModel.saveLookupToVocabulary(res, lang, ctxSentence)
            },
            onDismiss = { showDictionarySheet = false }
        )
    }

    if (showSavePassageSheet) {
        val quote = selectedTextBuffer.ifBlank { readyState?.reflowedText?.take(220).orEmpty() }
        SavePassageBottomSheet(
            quotePreview = quote,
            folders = passageFolders,
            onCreateFolder = viewModel::createPassageFolder,
            onSave = { folderId, note, _ ->
                viewModel.savePassage(
                    text = quote,
                    originalText = quote,
                    language = "en",
                    folderId = folderId,
                    note = note
                )
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
): androidx.compose.ui.text.AnnotatedString {
    val q = query.trim()
    if (q.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        var startIndex = 0
        while (startIndex < text.length) {
            val found = text.indexOf(q, startIndex = startIndex, ignoreCase = true)
            if (found < 0) break
            addStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    background = highlightBg,
                    color = highlightFg
                ),
                start = found,
                end = found + q.length
            )
            startIndex = found + q.length
        }
    }
}
