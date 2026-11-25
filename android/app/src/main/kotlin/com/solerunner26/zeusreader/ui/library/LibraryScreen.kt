package com.solerunner26.zeusreader.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.data.local.UnsupportedFileEntity
import com.solerunner26.zeusreader.data.local.WatchedFolderEntity
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (LibraryBookSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val watchedFolders by viewModel.watchedFolders.collectAsStateWithLifecycle()
    val unsupportedFiles by viewModel.unsupportedFiles.collectAsStateWithLifecycle()
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
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterSelected = viewModel::onFilterSelected,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScreenContent(
    uiState: LibraryUiState,
    isImporting: Boolean,
    unsupportedCount: Int = 0,
    snackbarHostState: SnackbarHostState,
    onImportClick: () -> Unit,
    onOpenWatchedFolders: () -> Unit,
    onOpenUnsupportedFiles: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (LibraryFormatFilter) -> Unit,
    onOpenBook: (LibraryBookSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen_root"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                actions = {
                    if (unsupportedCount > 0) {
                        IconButton(
                            onClick = onOpenUnsupportedFiles,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Unsupported files ($unsupportedCount)",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenWatchedFolders,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderCopy,
                            contentDescription = stringResource(R.string.library_watched_folders)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (uiState is LibraryUiState.Populated) {
                FloatingActionButton(
                    onClick = onImportClick,
                    modifier = Modifier.heightIn(min = 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = stringResource(R.string.library_import_cta)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (uiState) {
                LibraryUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                LibraryUiState.Empty -> {
                    EmptyLibraryState(
                        onImportClick = onImportClick,
                        onOpenWatchedFolders = onOpenWatchedFolders,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is LibraryUiState.Populated -> {
                    PopulatedLibraryContent(
                        state = uiState,
                        onSearchQueryChange = onSearchQueryChange,
                        onFilterSelected = onFilterSelected,
                        onOpenBook = onOpenBook,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is LibraryUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopulatedLibraryContent(
    state: LibraryUiState.Populated,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (LibraryFormatFilter) -> Unit,
    onOpenBook: (LibraryBookSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.library_search_hint)) },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedFilter == LibraryFormatFilter.ALL,
                onClick = { onFilterSelected(LibraryFormatFilter.ALL) },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(stringResource(R.string.library_filter_all)) }
            )
            FilterChip(
                selected = state.selectedFilter == LibraryFormatFilter.EPUB,
                onClick = { onFilterSelected(LibraryFormatFilter.EPUB) },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(stringResource(R.string.library_filter_epub)) }
            )
            FilterChip(
                selected = state.selectedFilter == LibraryFormatFilter.PDF,
                onClick = { onFilterSelected(LibraryFormatFilter.PDF) },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(stringResource(R.string.library_filter_pdf)) }
            )
            FilterChip(
                selected = state.selectedFilter == LibraryFormatFilter.IMAGE_BOOK,
                onClick = { onFilterSelected(LibraryFormatFilter.IMAGE_BOOK) },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(stringResource(R.string.library_filter_images)) }
            )
        }

        if (state.books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching books in your library.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.books, key = { it.editionId }) { book ->
                    LibraryBookCard(book = book, onClick = { onOpenBook(book) })
                }
            }
        }
    }
}

@Composable
private fun LibraryBookCard(
    book: LibraryBookSummary,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (book.isImageBook) {
                    Icons.Rounded.CollectionsBookmark
                } else {
                    Icons.Rounded.PictureAsPdf
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.authors.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.authors,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = onClick,
                        label = {
                            Text(
                                if (book.isImageBook) {
                                    "${book.imagePageCount.coerceAtLeast(1)} pages • Images"
                                } else {
                                    book.primaryFormat.ifBlank { "PDF" }
                                }
                            )
                        }
                    )
                    Text(
                        text = formatFileSize(book.totalByteSize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    onImportClick: () -> Unit,
    onOpenWatchedFolders: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoStories,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.library_empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.library_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                AssistChip(
                    onClick = {},
                    modifier = Modifier.heightIn(min = 48.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.OfflinePin,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.library_offline_ready_badge)) }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onImportClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.library_import_cta))
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenWatchedFolders,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.library_watched_folders))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchedFoldersBottomSheet(
    folders: List<WatchedFolderEntity>,
    onDismiss: () -> Unit,
    onAddFolder: () -> Unit,
    onRescanAll: () -> Unit,
    onRemoveFolder: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.watched_folders_title),
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddFolder,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.watched_folders_add))
                }
                OutlinedButton(
                    onClick = onRescanAll,
                    enabled = folders.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.watched_folders_rescan))
                }
            }

            if (folders.isEmpty()) {
                Text(
                    text = stringResource(R.string.watched_folders_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                folders.forEach { folder ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (folder.lastScannedEpochMs != null) {
                                        "Synced"
                                    } else {
                                        "Pending first scan"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onRemoveFolder(folder.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove watched folder"
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnsupportedFilesBottomSheet(
    files: List<UnsupportedFileEntity>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Unsupported Files", style = MaterialTheme.typography.titleLarge)
                if (files.isNotEmpty()) {
                    OutlinedButton(onClick = onClear, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Clear")
                    }
                }
            }
            if (files.isEmpty()) {
                Text(
                    text = "No rejected or unsupported files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.id }) { item ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${kb.toInt().coerceAtLeast(1)} KB"
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
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
