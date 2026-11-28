package com.solerunner26.zeusreader.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.data.importer.BookCoverAndMetadataExtractor
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.data.local.UnsupportedFileEntity
import com.solerunner26.zeusreader.data.local.WatchedFolderEntity
import com.solerunner26.zeusreader.ui.components.BookCover
import com.solerunner26.zeusreader.ui.components.EmptyState
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassPillButton
import com.solerunner26.zeusreader.ui.components.GradientHeading
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.PrimaryPillButton
import com.solerunner26.zeusreader.ui.components.StickerChip
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusChipRow
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.ZeusListRow
import com.solerunner26.zeusreader.ui.components.ZeusSnackbarHost
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.components.rememberDockClearance
import com.solerunner26.zeusreader.ui.home.knownProgress
import com.solerunner26.zeusreader.ui.home.progressLabel
import com.solerunner26.zeusreader.ui.theme.GlassSurface
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.rememberPrimaryGradient
import kotlin.math.absoluteValue

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (LibraryBookSummary) -> Unit,
    onOpenVocabulary: () -> Unit = {},
    onOpenPrepareOffline: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedRack by viewModel.selectedRackCategory.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val watchedFolders by viewModel.watchedFolders.collectAsStateWithLifecycle()
    val unsupportedFiles by viewModel.unsupportedFiles.collectAsStateWithLifecycle()

    LaunchedEffect((uiState as? LibraryUiState.Populated)?.books?.size) {
        val books = (uiState as? LibraryUiState.Populated)?.books.orEmpty()
        if (books.isNotEmpty()) {
            viewModel.refreshCoversAndMetadata(books)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var showWatchedFoldersSheet by remember { mutableStateOf(false) }
    var showUnsupportedSheet by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris, origin = "picker")
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.registerWatchedFolder(treeUri)
        }
    }

    LibraryScreenContent(
        uiState = uiState,
        selectedRackCategory = selectedRack,
        isImporting = isImporting,
        unsupportedCount = unsupportedFiles.size,
        snackbarHostState = snackbarHostState,
        onImportClick = {
            documentPickerLauncher.launch(
                arrayOf("*/*")
            )
        },
        onOpenWatchedFolders = { showWatchedFoldersSheet = true },
        onOpenUnsupportedFiles = { showUnsupportedSheet = true },
        onOpenVocabulary = onOpenVocabulary,
        onOpenPrepareOffline = onOpenPrepareOffline,
        onOpenSettings = onOpenSettings,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterSelected = viewModel::onFilterSelected,
        onRackSelected = viewModel::onRackCategorySelected,
        onOpenBook = onOpenBook,
        modifier = modifier
    )

    if (showWatchedFoldersSheet) {
        WatchedFoldersBottomSheet(
            folders = watchedFolders,
            onDismiss = { showWatchedFoldersSheet = false },
            onAddFolder = { folderPickerLauncher.launch(null) },
            onRescanAll = viewModel::rescanWatchedFolders,
            onRemoveFolder = viewModel::removeWatchedFolder
        )
    }

    if (showUnsupportedSheet) {
        UnsupportedFilesBottomSheet(
            files = unsupportedFiles,
            onClear = viewModel::clearUnsupportedFiles,
            onDismiss = { showUnsupportedSheet = false }
        )
    }
}

@Composable
internal fun LibraryScreenContent(
    uiState: LibraryUiState,
    selectedRackCategory: String = "All",
    isImporting: Boolean,
    unsupportedCount: Int = 0,
    snackbarHostState: SnackbarHostState,
    onImportClick: () -> Unit,
    onOpenWatchedFolders: () -> Unit,
    onOpenUnsupportedFiles: () -> Unit = {},
    onOpenVocabulary: () -> Unit = {},
    onOpenPrepareOffline: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (LibraryFormatFilter) -> Unit,
    onRackSelected: (String) -> Unit = {},
    onOpenBook: (LibraryBookSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val dockClearance = rememberDockClearance()
    val bookCount = (uiState as? LibraryUiState.Populated)?.books?.size ?: 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("library_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OlympusHeader(
                bookCount = bookCount,
                unsupportedCount = unsupportedCount,
                onImportClick = onImportClick,
                onOpenWatchedFolders = onOpenWatchedFolders,
                onOpenUnsupportedFiles = onOpenUnsupportedFiles,
                onOpenVocabulary = onOpenVocabulary,
                onOpenPrepareOffline = onOpenPrepareOffline,
                onOpenSettings = onOpenSettings
            )
            if (isImporting) {
                ZeusLinearProgress(modifier = Modifier.padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.xs))
            }
            when (uiState) {
                LibraryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ZeusLinearProgress(modifier = Modifier.padding(horizontal = ZeusSpacing.huge))
                }

                LibraryUiState.Empty -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ZeusSpacing.gutter)
                        .padding(bottom = dockClearance),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Rounded.AutoStories,
                        title = stringResource(R.string.library_empty_title),
                        message = stringResource(R.string.library_empty_subtitle),
                        actionLabel = stringResource(R.string.library_import_cta),
                        actionIcon = Icons.Rounded.FileOpen,
                        onAction = onImportClick,
                        secondaryLabel = stringResource(R.string.library_watched_folders),
                        onSecondary = onOpenWatchedFolders
                    )
                }

                is LibraryUiState.Populated -> PopulatedLibraryContent(
                    state = uiState,
                    selectedRackCategory = selectedRackCategory,
                    onSearchQueryChange = onSearchQueryChange,
                    onFilterSelected = onFilterSelected,
                    onRackSelected = onRackSelected,
                    onOpenBook = onOpenBook,
                    bottomPadding = dockClearance,
                    modifier = Modifier.fillMaxSize()
                )

                is LibraryUiState.Error -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ZeusSpacing.xxl),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.message, style = type.body, color = colors.errorGlow, textAlign = TextAlign.Center)
                }
            }
        }
        ZeusSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = ZeusSpacing.gutter)
                .padding(bottom = dockClearance)
        )
    }
}

@Composable
private fun OlympusHeader(
    bookCount: Int,
    unsupportedCount: Int,
    onImportClick: () -> Unit,
    onOpenWatchedFolders: () -> Unit,
    onOpenUnsupportedFiles: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenPrepareOffline: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, top = ZeusSpacing.md, bottom = ZeusSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            GradientHeading(text = "Olympus", modifier = Modifier.semantics { contentDescription = "Olympus library" })
            Text(
                text = if (bookCount == 1) "1 book on your mountain" else "$bookCount books on your mountain",
                style = type.caption,
                color = colors.textOnSkySecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (unsupportedCount > 0) {
            GlassIconButton(
                icon = Icons.Rounded.WarningAmber,
                contentDescription = "Unsupported files ($unsupportedCount)",
                onClick = onOpenUnsupportedFiles,
                tint = colors.errorGlow
            )
        }
        Box {
            GlassIconButton(icon = Icons.Rounded.MoreVert, contentDescription = "More library options", onClick = { menuOpen = true })
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = ZeusShapes.cardSmall,
                containerColor = colors.sheetBackground
            ) {
                OlympusMenuItem(Icons.Rounded.FolderCopy, stringResource(R.string.library_watched_folders)) { menuOpen = false; onOpenWatchedFolders() }
                OlympusMenuItem(Icons.Rounded.Translate, "Prepare for offline reading") { menuOpen = false; onOpenPrepareOffline() }
                OlympusMenuItem(Icons.Rounded.School, "Vocabulary & flashcards") { menuOpen = false; onOpenVocabulary() }
                OlympusMenuItem(Icons.Rounded.Settings, "Settings") { menuOpen = false; onOpenSettings() }
            }
        }
        // Import orb
        Box(
            modifier = Modifier
                .size(ZeusSpacing.giant)
                .shadow(ZeusSizes.shadowGlow, ZeusShapes.circle, ambientColor = ZeusTheme.sky.glow2, spotColor = ZeusTheme.sky.glow2)
                .clip(ZeusShapes.circle)
                .background(rememberPrimaryGradient())
                .clickable(role = Role.Button, onClick = onImportClick)
                .semantics { contentDescription = "Import files" },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.textOnAccent, modifier = Modifier.size(ZeusSizes.iconLg))
        }
    }
}

@Composable
private fun OlympusMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = ZeusTheme.colors
    DropdownMenuItem(
        leadingIcon = { Icon(icon, contentDescription = null, tint = colors.accentOnSky) },
        text = { Text(label, style = ZeusTheme.type.label, color = colors.textOnSky) },
        onClick = onClick,
        modifier = Modifier.heightIn(min = ZeusSpacing.touchMin)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PopulatedLibraryContent(
    state: LibraryUiState.Populated,
    selectedRackCategory: String = "All",
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (LibraryFormatFilter) -> Unit,
    onRackSelected: (String) -> Unit = {},
    onOpenBook: (LibraryBookSummary) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val showFlow = state.searchQuery.isBlank() && state.books.size >= 2
    val flowBooks = remember(state.books) { state.books.sortedByDescending { it.updatedAtEpochMs }.take(8) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = ZeusSizes.gridCoverMin),
        modifier = modifier,
        contentPadding = PaddingValues(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, top = ZeusSpacing.xs, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)
    ) {
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            ZeusTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = stringResource(R.string.library_search_hint),
                leadingIcon = Icons.Rounded.Search,
                modifier = Modifier.fillMaxWidth(),
                trailing = if (state.searchQuery.isNotEmpty()) {
                    {
                        GlassIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            onClick = { onSearchQueryChange("") },
                            size = ZeusSizes.avatar - ZeusSpacing.xs
                        )
                    }
                } else {
                    null
                }
            )
        }

        if (showFlow) {
            item(key = "flow", span = { GridItemSpan(maxLineSpan) }) {
                CoverFlow(books = flowBooks, onOpenBook = onOpenBook)
            }
        }

        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
            ) {
                ZeusChipRow(contentPadding = ZeusSpacing.none) {
                    BookCoverAndMetadataExtractor.RACK_CATEGORIES.forEach { category ->
                        ZeusChip(label = category, selected = selectedRackCategory == category, onClick = { onRackSelected(category) })
                    }
                }
                ZeusChipRow(contentPadding = ZeusSpacing.none) {
                    ZeusChip(stringResource(R.string.library_filter_all), state.selectedFilter == LibraryFormatFilter.ALL, { onFilterSelected(LibraryFormatFilter.ALL) })
                    ZeusChip(stringResource(R.string.library_filter_epub), state.selectedFilter == LibraryFormatFilter.EPUB, { onFilterSelected(LibraryFormatFilter.EPUB) })
                    ZeusChip(stringResource(R.string.library_filter_pdf), state.selectedFilter == LibraryFormatFilter.PDF, { onFilterSelected(LibraryFormatFilter.PDF) })
                    ZeusChip(stringResource(R.string.library_filter_images), state.selectedFilter == LibraryFormatFilter.IMAGE_BOOK, { onFilterSelected(LibraryFormatFilter.IMAGE_BOOK) })
                }
            }
        }

        if (state.books.isEmpty()) {
            item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "No matching books on this shelf.",
                    style = type.body,
                    color = colors.textOnSkySecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ZeusSpacing.huge)
                )
            }
        } else {
            items(state.books, key = { it.editionId }) { book ->
                LibraryBookCard(book = book, onClick = { onOpenBook(book) })
            }
        }
    }
}

/** Cover-flow: centre book large and upright, neighbours smaller, tilted and dimmed. */
@Composable
private fun CoverFlow(books: List<LibraryBookSummary>, onOpenBook: (LibraryBookSummary) -> Unit) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val pagerState = rememberPagerState(pageCount = { books.size })
    Column(verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = ZeusSizes.coverFlowSideWidth),
            pageSpacing = ZeusSpacing.sm,
            modifier = Modifier
                .fillMaxWidth()
                .height(ZeusSizes.coverFlowHeight)
        ) { page ->
            val book = books[page]
            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val abs = offset.absoluteValue.coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BookCover(
                    title = book.title,
                    authors = book.authors,
                    format = if (book.isImageBook) "IMG" else book.primaryFormat,
                    coverPath = book.coverPath,
                    shape = ZeusShapes.coverLarge,
                    modifier = Modifier
                        .height(ZeusSizes.coverHeroHeight)
                        .aspectRatio(2f / 3f)
                        .graphicsLayer {
                            val scale = 1f - 0.22f * abs
                            scaleX = scale
                            scaleY = scale
                            rotationY = offset.coerceIn(-1f, 1f) * 24f
                            alpha = 1f - 0.35f * abs
                            cameraDistance = 14f * density
                        }
                        .shadow(ZeusSizes.shadowCover, ZeusShapes.coverLarge)
                        .clickable(role = Role.Button) { onOpenBook(book) }
                )
            }
        }
        val current = books.getOrNull(pagerState.currentPage)
        if (current != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(current.title, style = type.title.copy(fontFamily = type.Unbounded), color = colors.textOnSky, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (current.authors.isNotBlank()) {
                        Text(current.authors, style = type.caption, color = colors.textOnSkySecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                StickerChip(text = current.progressLabel(), accentFill = true, rotation = 3f)
            }
            GlassPillButton(
                label = "Open ${current.title}",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = { onOpenBook(current) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LibraryBookCard(
    book: LibraryBookSummary,
    onClick: () -> Unit
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    GlassSurface(shape = ZeusShapes.cardSmall, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(ZeusSpacing.smPlus),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
        ) {
            BookCover(
                title = book.title,
                authors = book.authors,
                format = if (book.isImageBook) "IMG" else book.primaryFormat,
                coverPath = book.coverPath,
                compact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
            Text(book.title, style = type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.textOnSky, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.authors.isNotBlank()) {
                Text(book.authors, style = type.caption, color = colors.textOnSkySecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            book.knownProgress()?.let { ZeusLinearProgress(progress = it) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                val badge = if (book.isImageBook) "${book.imagePageCount.coerceAtLeast(1)}p IMG" else book.primaryFormat.ifBlank { "Book" }
                MetaLabel(text = badge, accent = true)
                Spacer(Modifier.weight(1f))
                MetaLabel(text = book.category.ifBlank { "General" }, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
private fun WatchedFoldersBottomSheet(
    folders: List<WatchedFolderEntity>,
    onDismiss: () -> Unit,
    onAddFolder: () -> Unit,
    onRescanAll: () -> Unit,
    onRemoveFolder: (String) -> Unit
) {
    ZeusBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.watched_folders_title),
        subtitle = "New books dropped into these folders are imported automatically."
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
            PrimaryPillButton(
                label = stringResource(R.string.watched_folders_add),
                icon = Icons.Rounded.Add,
                leadingIcon = true,
                onClick = onAddFolder,
                height = ZeusSpacing.secondaryButtonHeight,
                modifier = Modifier.weight(1f)
            )
            GlassPillButton(
                label = stringResource(R.string.watched_folders_rescan),
                icon = Icons.Rounded.Refresh,
                onClick = onRescanAll,
                enabled = folders.isNotEmpty(),
                modifier = Modifier.weight(1f)
            )
        }
        if (folders.isEmpty()) {
            Text(stringResource(R.string.watched_folders_empty), style = ZeusTheme.type.body, color = ZeusTheme.colors.textOnSkySecondary)
        } else {
            folders.forEach { folder ->
                ZeusListRow(
                    title = folder.displayName,
                    subtitle = if (folder.lastScannedEpochMs != null) "Synced" else "Pending first scan",
                    icon = Icons.Rounded.Folder
                ) {
                    GlassIconButton(icon = Icons.Rounded.DeleteOutline, contentDescription = "Remove watched folder", onClick = { onRemoveFolder(folder.id) })
                }
            }
        }
    }
}

@Composable
private fun UnsupportedFilesBottomSheet(
    files: List<UnsupportedFileEntity>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ZeusBottomSheet(
        onDismissRequest = onDismiss,
        title = "Unsupported files",
        headerAction = if (files.isNotEmpty()) {
            { GlassPillButton(label = "Clear", onClick = onClear) }
        } else {
            null
        }
    ) {
        if (files.isEmpty()) {
            Text("No rejected or unsupported files.", style = ZeusTheme.type.body, color = ZeusTheme.colors.textOnSkySecondary)
        } else {
            files.forEach { item ->
                ZeusListRow(title = item.displayName, subtitle = item.reason, icon = Icons.Rounded.WarningAmber)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EmptyLibraryPreview() {
    ZeusReaderTheme {
        LibraryScreenContent(
            uiState = LibraryUiState.Empty,
            isImporting = false,
            snackbarHostState = SnackbarHostState(),
            onImportClick = {},
            onOpenWatchedFolders = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onOpenBook = {}
        )
    }
}
