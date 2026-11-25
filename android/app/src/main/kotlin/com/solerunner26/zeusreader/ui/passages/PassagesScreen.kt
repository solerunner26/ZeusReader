package com.solerunner26.zeusreader.ui.passages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.data.PassageWithTags
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.theme.ZeusFonts

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PassagesScreen(
    viewModel: PassagesViewModel,
    onOpenPassage: (String) -> Unit,
    onCreateCard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFabMenu by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewPassageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.lastDeletedId.collect { deletedId ->
            val result = snackbarHostState.showSnackbar(
                message = "Passage deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restorePassage(deletedId)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .testTag("passages_screen_root"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${uiState.selectedIds.size} selected") },
                    actions = {
                        TextButton(onClick = viewModel::deleteSelected, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Delete")
                        }
                        TextButton(onClick = viewModel::clearSelection, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Cancel")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            } else {
                TopAppBar(
                    title = { Text("Passages (${uiState.passages.size})") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "New passage or folder")
                }
                DropdownMenu(expanded = showFabMenu, onDismissRequest = { showFabMenu = false }) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.FormatQuote, contentDescription = null) },
                        text = { Text("New passage") },
                        onClick = { showFabMenu = false; showNewPassageDialog = true }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
                        text = { Text("New folder") },
                        onClick = { showFabMenu = false; showNewFolderDialog = true }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search passages, notes, tags") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFolderId == null,
                    onClick = { viewModel.selectFolder(null) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.selectedFolderId == PassagesViewModel.UNSORTED_FOLDER_KEY,
                    onClick = { viewModel.selectFolder(PassagesViewModel.UNSORTED_FOLDER_KEY) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text("Unsorted") }
                )
                uiState.folders.forEach { folder ->
                    FilterChip(
                        selected = uiState.selectedFolderId == folder.id,
                        onClick = { viewModel.selectFolder(folder.id) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(folder.name) }
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PassageSortMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = uiState.sortMode == mode,
                        onClick = { viewModel.setSortMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(idx, PassageSortMode.entries.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(mode.label) }
                    )
                }
            }

            if (uiState.passages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No passages saved yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.passages, key = { it.passage.id }) { item ->
                        val folderName = uiState.folders.firstOrNull { it.id == item.passage.folderId }?.name ?: "Unsorted"
                        val isSelected = item.passage.id in uiState.selectedIds
                        PassageCard(
                            item = item,
                            folderName = folderName,
                            isSelected = isSelected,
                            onClick = {
                                if (uiState.selectedIds.isNotEmpty()) {
                                    viewModel.toggleSelection(item.passage.id)
                                } else {
                                    onOpenPassage(item.passage.id)
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(item.passage.id) },
                            onShareCard = { onCreateCard(item.passage.id) },
                            onDelete = { viewModel.softDeletePassage(item.passage.id) }
                        )
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createFolder(name.trim())
                            showNewFolderDialog = false
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNewPassageDialog) {
        ManualPassageDialog(
            books = uiState.books,
            onDismiss = { showNewPassageDialog = false },
            onSave = { book, text, page, note ->
                viewModel.saveManualPassage(
                    book = book,
                    text = text,
                    pageLabel = page,
                    folderId = uiState.selectedFolderId?.takeIf { it != PassagesViewModel.UNSORTED_FOLDER_KEY },
                    note = note
                )
                showNewPassageDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PassageCard(
    item: PassageWithTags,
    folderName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShareCard: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "“${item.passage.text}”",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ZeusFonts.Fraunces,
                    fontStyle = FontStyle.Italic
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    item.passage.chapterTitle?.takeIf { it.isNotBlank() },
                    item.passage.pageLabel?.takeIf { it.isNotBlank() }
                ).joinToString(" · ").ifBlank { "Manual passage" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = onClick,
                    label = { Text(if (isSelected) "Selected · $folderName" else folderName) }
                )
                Row {
                    IconButton(onClick = onShareCard, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share as card")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete passage")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualPassageDialog(
    books: List<LibraryBookSummary>,
    onDismiss: () -> Unit,
    onSave: (LibraryBookSummary, String, String?, String?) -> Unit
) {
    var selectedBook by remember(books) { mutableStateOf(books.firstOrNull()) }
    var text by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New passage") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (books.isEmpty()) {
                    Text("Import a book to your library first.")
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        books.take(6).forEach { book ->
                            FilterChip(
                                selected = selectedBook?.editionId == book.editionId,
                                onClick = { selectedBook = book },
                                label = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Passage text") },
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = page,
                        onValueChange = { page = it },
                        label = { Text("Page (optional)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedBook != null && text.isNotBlank(),
                onClick = { selectedBook?.let { onSave(it, text, page, note) } },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}
