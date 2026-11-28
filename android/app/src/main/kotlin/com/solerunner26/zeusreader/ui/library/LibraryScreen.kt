package com.solerunner26.zeusreader.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.data.importer.BookCoverAndMetadataExtractor
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.data.local.UnsupportedFileEntity
import com.solerunner26.zeusreader.data.local.WatchedFolderEntity
import com.solerunner26.zeusreader.ui.components.BookCover
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.EmptyState
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.MenuRow
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ProgressBar
import com.solerunner26.zeusreader.ui.components.ScreenTitle
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.TextAction
import com.solerunner26.zeusreader.ui.components.Tone
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusCard
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusChipRow
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.ZeusSnackbarHost
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.components.rememberNavClearance
import com.solerunner26.zeusreader.ui.components.staggerIn
import com.solerunner26.zeusreader.ui.home.knownProgress
import com.solerunner26.zeusreader.ui.home.progressLabel
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme

/** UI-side reading-progress filter applied on top of the ViewModel's format filter. */
enum class ProgressFilter(val label: String) { ALL("All"), READING("Reading"), NEW("Not started"), }

/** UI-side sort order. */
enum class LibrarySort(val label: String) { RECENT("Recent"), TITLE("Title"), AUTHOR("Author") }

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (LibraryBookSummary) -> Unit,
    onOpenVocabulary: () -> Unit = {},
    onOpenPrepareOffline: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenOnline: () -> Unit = {},
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
        onOpenOnline = onOpenOnline,
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

@OptIn(ExperimentalLayoutApi::class)
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
    onOpenOnline: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (LibraryFormatFilter) -> Unit,
    onRackSelected: (String) -> Unit = {},
    onOpenBook: (LibraryBookSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    val navClearance = rememberNavClearance()
    var progressFilter by rememberSaveable { mutableStateOf(ProgressFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.RECENT) }
    var showFilters by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val populated = uiState as? LibraryUiState.Populated
    val total = populated?.books?.size ?: 0
    val inProgress = populated?.books?.count { (it.lastPageIndex ?: 0) > 0 } ?: 0

    Box(modifier.fillMaxSize().statusBarsPadding().testTag("library_screen_root")) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, top = ZeusSpacing.md, bottom = ZeusSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
            ) {
                ScreenTitle("Library", subtitle = "$total books · $inProgress in progress", modifier = Modifier.weight(1f))
                if (unsupportedCount > 0) CircleIconButton(Icons.Rounded.WarningAmber, "Unsupported files ($unsupportedCount)", onOpenUnsupportedFiles, tint = c.rose)
                CircleIconButton(Icons.Rounded.Tune, "Filter books", { showFilters = true }, enabled = populated != null)
                Box {
                    CircleIconButton(Icons.Rounded.MoreVert, "More library options", { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = ZeusShapes.card, containerColor = c.card) {
                        LibraryMenuItem(Icons.Rounded.FolderCopy, stringResource(R.string.library_watched_folders)) { menuOpen = false; onOpenWatchedFolders() }
                        LibraryMenuItem(Icons.Rounded.Public, "Find books online") { menuOpen = false; onOpenOnline() }
                        LibraryMenuItem(Icons.Rounded.Translate, "Offline language packs") { menuOpen = false; onOpenPrepareOffline() }
                        LibraryMenuItem(Icons.Rounded.Settings, "Settings") { menuOpen = false; onOpenSettings() }
                    }
                }
                CircleIconButton(Icons.Rounded.Add, stringResource(R.string.library_import_cta), onImportClick, filled = true)
            }
            if (isImporting) ZeusLinearProgress(modifier = Modifier.padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.xs))

            when (uiState) {
                LibraryUiState.Loading -> Box(Modifier.fillMaxSize().padding(ZeusSpacing.huge), contentAlignment = Alignment.Center) { ZeusLinearProgress() }
                LibraryUiState.Empty -> Box(Modifier.fillMaxSize().padding(horizontal = ZeusSpacing.gutter).padding(bottom = navClearance), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = stringResource(R.string.library_empty_title),
                        message = stringResource(R.string.library_empty_subtitle),
                        actionLabel = stringResource(R.string.library_import_cta),
                        actionIcon = Icons.Rounded.FileOpen,
                        onAction = onImportClick,
                        secondaryLabel = stringResource(R.string.library_watched_folders),
                        onSecondary = onOpenWatchedFolders
                    )
                }
                is LibraryUiState.Populated -> {
                    val books = remember(uiState.books, progressFilter, sort) {
                        uiState.books
                            .filter {
                                when (progressFilter) {
                                    ProgressFilter.ALL -> true
                                    ProgressFilter.READING -> (it.lastPageIndex ?: 0) > 0
                                    ProgressFilter.NEW -> (it.lastPageIndex ?: 0) <= 0
                                }
                            }
                            .let { list ->
                                when (sort) {
                                    LibrarySort.RECENT -> list.sortedByDescending { it.updatedAtEpochMs }
                                    LibrarySort.TITLE -> list.sortedBy { it.title.lowercase() }
                                    LibrarySort.AUTHOR -> list.sortedBy { it.authors.lowercase() }
                                }
                            }
                    }
                    LibraryGrid(
                        state = uiState,
                        books = books,
                        progressFilter = progressFilter,
                        onProgressFilter = { progressFilter = it },
                        onSearchQueryChange = onSearchQueryChange,
                        onFilterSelected = onFilterSelected,
                        onOpenBook = onOpenBook,
                        bottomPadding = navClearance
                    )
                }
                is LibraryUiState.Error -> Box(Modifier.fillMaxSize().padding(ZeusSpacing.xxl), contentAlignment = Alignment.Center) {
                    Text(uiState.message, style = type.body, color = c.rose, textAlign = TextAlign.Center)
                }
            }
        }
        ZeusSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(horizontal = ZeusSpacing.gutter).padding(bottom = navClearance))
    }

    if (showFilters && populated != null) {
        ZeusBottomSheet(
            onDismissRequest = { showFilters = false },
            title = "Filter books",
            headerAction = {
                TextAction("Reset", {
                    progressFilter = ProgressFilter.ALL
                    sort = LibrarySort.RECENT
                    onFilterSelected(LibraryFormatFilter.ALL)
                    onRackSelected("All")
                })
            }
        ) {
            Eyebrow("Format", accent = false)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                FormatChips(populated.selectedFilter, onFilterSelected)
            }
            Eyebrow("Progress", accent = false)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                ProgressFilter.entries.forEach { ZeusChip(it.label, progressFilter == it, { progressFilter = it }, outline = progressFilter != it) }
            }
            Eyebrow("Shelf", accent = false)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                BookCoverAndMetadataExtractor.RACK_CATEGORIES.forEach { cat ->
                    ZeusChip(cat, selectedRackCategory == cat, { onRackSelected(cat) }, outline = selectedRackCategory != cat)
                }
            }
            Eyebrow("Sort by", accent = false)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                LibrarySort.entries.forEach { ZeusChip(it.label, sort == it, { sort = it }, outline = sort != it) }
            }
            PrimaryButton("Show books", { showFilters = false }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FormatChips(selected: LibraryFormatFilter, onFilterSelected: (LibraryFormatFilter) -> Unit) {
    ZeusChip(stringResource(R.string.library_filter_all), selected == LibraryFormatFilter.ALL, { onFilterSelected(LibraryFormatFilter.ALL) }, outline = selected != LibraryFormatFilter.ALL)
    ZeusChip(stringResource(R.string.library_filter_epub), selected == LibraryFormatFilter.EPUB, { onFilterSelected(LibraryFormatFilter.EPUB) }, outline = selected != LibraryFormatFilter.EPUB)
    ZeusChip(stringResource(R.string.library_filter_pdf), selected == LibraryFormatFilter.PDF, { onFilterSelected(LibraryFormatFilter.PDF) }, outline = selected != LibraryFormatFilter.PDF)
    ZeusChip(stringResource(R.string.library_filter_images), selected == LibraryFormatFilter.IMAGE_BOOK, { onFilterSelected(LibraryFormatFilter.IMAGE_BOOK) }, outline = selected != LibraryFormatFilter.IMAGE_BOOK)
}

@Composable
private fun LibraryMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val c = ZeusTheme.colors
    DropdownMenuItem(
        leadingIcon = { Icon(icon, null, tint = c.accentInk) },
        text = { Text(label, style = ZeusTheme.type.label, color = c.ink) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = ZeusSpacing.xs)
    )
}

@Composable
private fun LibraryGrid(
    state: LibraryUiState.Populated,
    books: List<LibraryBookSummary>,
    progressFilter: ProgressFilter,
    onProgressFilter: (ProgressFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (LibraryFormatFilter) -> Unit,
    onOpenBook: (LibraryBookSummary) -> Unit,
    bottomPadding: Dp
) {
    val c = ZeusTheme.colors
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = ZeusSizes.gridCoverMin),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, top = ZeusSpacing.xs, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            ZeusTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = stringResource(R.string.library_search_hint),
                leadingIcon = Icons.Rounded.Search,
                trailing = if (state.searchQuery.isNotEmpty()) {
                    { CircleIconButton(Icons.Rounded.Close, "Clear search", { onSearchQueryChange("") }, size = ZeusSizes.avatar - ZeusSpacing.xs) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
            ZeusChipRow(contentPadding = ZeusSpacing.none) {
                ProgressFilter.entries.forEach { ZeusChip(it.label, progressFilter == it, { onProgressFilter(it) }) }
                FormatChips(state.selectedFilter, onFilterSelected)
            }
        }
        if (books.isEmpty()) {
            item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                Text("No books match these filters.", style = ZeusTheme.type.body, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = ZeusSpacing.huge))
            }
        } else {
            itemsIndexed(books, key = { _, b -> b.editionId }) { i, book ->
                LibraryBookCard(book = book, onClick = { onOpenBook(book) }, modifier = Modifier.staggerIn(i))
            }
        }
    }
}

@Composable
private fun LibraryBookCard(book: LibraryBookSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    ZeusCard(modifier = modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(ZeusSpacing.smPlus)) {
        BookCover(
            title = book.title,
            authors = book.authors,
            format = if (book.isImageBook) "IMG" else book.primaryFormat,
            coverPath = book.coverPath,
            compact = true,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
        )
        Text(book.title, style = type.label, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (book.authors.isNotBlank()) Text(book.authors, style = type.caption, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val badge = if (book.isImageBook) "${book.imagePageCount.coerceAtLeast(1)}p IMG" else book.primaryFormat.ifBlank { "Book" }
            Eyebrow(badge, modifier = Modifier.weight(1f))
            Text(book.progressLabel(), style = type.tiny, color = c.muted, maxLines = 1)
        }
        book.knownProgress()?.let { ProgressBar(it, height = ZeusSizes.progressThin) }
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
    ZeusBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.watched_folders_title), subtitle = "New books in these folders are imported automatically.") {
        Row(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
            PrimaryButton(stringResource(R.string.watched_folders_add), onAddFolder, icon = Icons.Rounded.Add, leadingIcon = true, height = ZeusSpacing.secondaryButtonHeight, modifier = Modifier.weight(1f))
            SecondaryButton(stringResource(R.string.watched_folders_rescan), onRescanAll, icon = Icons.Rounded.Refresh, enabled = folders.isNotEmpty(), modifier = Modifier.weight(1f))
        }
        if (folders.isEmpty()) {
            Text(stringResource(R.string.watched_folders_empty), style = ZeusTheme.type.body, color = ZeusTheme.colors.muted)
        } else {
            folders.forEach { folder ->
                MenuRow(folder.displayName, subtitle = if (folder.lastScannedEpochMs != null) "Synced" else "Pending first scan", icon = Icons.Rounded.Folder) {
                    CircleIconButton(Icons.Rounded.DeleteOutline, "Remove watched folder", { onRemoveFolder(folder.id) })
                }
            }
        }
    }
}

@Composable
private fun UnsupportedFilesBottomSheet(files: List<UnsupportedFileEntity>, onClear: () -> Unit, onDismiss: () -> Unit) {
    ZeusBottomSheet(
        onDismissRequest = onDismiss,
        title = "Unsupported files",
        headerAction = if (files.isNotEmpty()) { { TextAction("Clear", onClear) } } else null
    ) {
        if (files.isEmpty()) {
            Text("No rejected or unsupported files.", style = ZeusTheme.type.body, color = ZeusTheme.colors.muted)
        } else {
            files.forEach { MenuRow(it.displayName, subtitle = it.reason, icon = Icons.Rounded.WarningAmber, tone = Tone.Rose) }
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
