package com.solerunner26.zeusreader.ui.passages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.data.PassageWithTags
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.components.BoltMascot
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.EmptyState
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.GradientCard
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.ZeusCard
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusChipRow
import com.solerunner26.zeusreader.ui.components.ZeusDialog
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusSnackbarHost
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.components.staggerIn
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme

@Composable
fun PassagesScreen(
    viewModel: PassagesViewModel,
    onOpenPassage: (String) -> Unit,
    onCreateCard: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.lastDeletedId.collect { deletedId ->
            val result = snackbarHostState.showSnackbar("Highlight deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.restorePassage(deletedId)
        }
    }
    PassagesContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSelectFolder = viewModel::selectFolder,
        onSortMode = viewModel::setSortMode,
        onToggleSelection = viewModel::toggleSelection,
        onClearSelection = viewModel::clearSelection,
        onDeleteSelected = viewModel::deleteSelected,
        onMoveSelected = viewModel::moveSelectedToFolder,
        onCreateFolder = viewModel::createFolder,
        onDeleteFolder = viewModel::deleteFolder,
        onSoftDelete = viewModel::softDeletePassage,
        onSaveManual = { book, text, page, note ->
            viewModel.saveManualPassage(book = book, text = text, pageLabel = page, folderId = uiState.selectedFolderId?.takeIf { it != PassagesViewModel.UNSORTED_FOLDER_KEY }, note = note)
        },
        onOpenPassage = onOpenPassage,
        onCreateCard = onCreateCard,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PassagesContent(
    uiState: PassagesUiState,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onSortMode: (PassageSortMode) -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelected: (String?) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onSoftDelete: (String) -> Unit,
    onSaveManual: (LibraryBookSummary, String, String?, String?) -> Unit,
    onOpenPassage: (String) -> Unit,
    onCreateCard: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    var showSearch by remember { mutableStateOf(uiState.searchQuery.isNotBlank()) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewPassageDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    val selecting = uiState.selectedIds.isNotEmpty()
    val bottom = rememberNavBarClearance()

    Box(modifier.fillMaxSize().statusBarsPadding().testTag("passages_screen_root")) {
        Column(Modifier.fillMaxSize()) {
            if (selecting) {
                ZeusTopBar("${uiState.selectedIds.size} selected") {
                    CircleIconButton(Icons.AutoMirrored.Rounded.DriveFileMove, "Move to collection", { showMoveDialog = true })
                    CircleIconButton(Icons.Rounded.DeleteOutline, "Delete selected", onDeleteSelected, tint = c.rose)
                    CircleIconButton(Icons.Rounded.Close, "Cancel selection", onClearSelection)
                }
            } else {
                ZeusTopBar("Highlights", subtitle = "${uiState.passages.size} saved passages", onBack = onBack) {
                    CircleIconButton(Icons.Rounded.Search, "Search highlights", { showSearch = !showSearch })
                    Box {
                        CircleIconButton(Icons.Rounded.Add, "New highlight or collection", { showAddMenu = true }, filled = true)
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }, shape = ZeusShapes.card, containerColor = c.card) {
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Rounded.FormatQuote, null, tint = c.accentInk) }, text = { Text("New highlight", style = type.label, color = c.ink) }, onClick = { showAddMenu = false; showNewPassageDialog = true })
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null, tint = c.accentInk) }, text = { Text("New collection", style = type.label, color = c.ink) }, onClick = { showAddMenu = false; showNewFolderDialog = true })
                        }
                    }
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottom),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
            ) {
                if (showSearch && !selecting) {
                    item(key = "search") {
                        ZeusTextField(uiState.searchQuery, onSearchQueryChange, placeholder = "Search highlights, notes, tags", leadingIcon = Icons.Rounded.Search, modifier = Modifier.fillMaxWidth().padding(horizontal = ZeusSpacing.gutter))
                    }
                }
                item(key = "hero") {
                    GradientCard(Modifier.fillMaxWidth().padding(horizontal = ZeusSpacing.gutter), padding = PaddingValues(ZeusSpacing.none)) {
                        Box(Modifier.fillMaxWidth().heightIn(min = ZeusSizes.mascotMedium + ZeusSpacing.sm)) {
                            BoltMascot(ZeusSizes.mascotMedium, Modifier.align(Alignment.BottomEnd).offset(x = ZeusSpacing.xs, y = ZeusSpacing.sm))
                            Column(Modifier.fillMaxWidth(0.64f).padding(ZeusSpacing.gutter), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                                Eyebrow("Your best lines", color = c.onAccent)
                                Text("${uiState.passages.size} highlights in ${uiState.folders.size} collections", style = type.headline, color = c.onAccent)
                                Text("Long-press to select. Turn any line into a share card.", style = type.caption, color = c.onAccent.copy(alpha = 0.88f))
                            }
                        }
                    }
                }
                item(key = "folders") {
                    ZeusChipRow {
                        ZeusChip("All", uiState.selectedFolderId == null, { onSelectFolder(null) })
                        ZeusChip("Unsorted", uiState.selectedFolderId == PassagesViewModel.UNSORTED_FOLDER_KEY, { onSelectFolder(PassagesViewModel.UNSORTED_FOLDER_KEY) })
                        uiState.folders.forEach { f -> ZeusChip(f.name, uiState.selectedFolderId == f.id, { onSelectFolder(f.id) }, icon = Icons.Rounded.AutoAwesome) }
                    }
                }
                uiState.folders.firstOrNull { it.id == uiState.selectedFolderId }?.let { folder ->
                    item(key = "dissolve") {
                        Row(Modifier.padding(horizontal = ZeusSpacing.gutter)) {
                            SecondaryButton("Delete collection “${folder.name}”", { onDeleteFolder(folder.id) }, icon = Icons.Rounded.DeleteOutline, danger = true)
                        }
                    }
                }
                item(key = "sort") {
                    ZeusSegmented(PassageSortMode.entries.map { it.label }, PassageSortMode.entries.indexOf(uiState.sortMode), { onSortMode(PassageSortMode.entries[it]) }, Modifier.padding(horizontal = ZeusSpacing.gutter))
                }
                if (uiState.passages.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            title = "No highlights yet",
                            message = "In the reader, pick a line and tap Save — it lands here, ready to share as a card.",
                            actionLabel = "Add a highlight",
                            onAction = { showNewPassageDialog = true },
                            modifier = Modifier.padding(horizontal = ZeusSpacing.gutter)
                        )
                    }
                } else {
                    itemsIndexed(uiState.passages, key = { _, it -> it.passage.id }) { i, item ->
                        val folderName = uiState.folders.firstOrNull { it.id == item.passage.folderId }?.name ?: "Unsorted"
                        val bookTitle = uiState.books.firstOrNull { it.editionId == item.passage.editionId }?.title
                        HighlightCard(
                            item = item,
                            folderName = folderName,
                            bookTitle = bookTitle,
                            isSelected = item.passage.id in uiState.selectedIds,
                            onClick = { if (selecting) onToggleSelection(item.passage.id) else onOpenPassage(item.passage.id) },
                            onLongClick = { onToggleSelection(item.passage.id) },
                            onShareCard = { onCreateCard(item.passage.id) },
                            onDelete = { onSoftDelete(item.passage.id) },
                            modifier = Modifier.padding(horizontal = ZeusSpacing.gutter).staggerIn(i)
                        )
                    }
                }
            }
        }
        ZeusSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(ZeusSpacing.gutter).padding(bottom = bottom))
    }

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        ZeusDialog(onDismissRequest = { showNewFolderDialog = false }, title = "New collection", confirmLabel = "Create", confirmEnabled = name.isNotBlank(), onConfirm = { onCreateFolder(name.trim()); showNewFolderDialog = false }) {
            ZeusTextField(name, { name = it }, label = "Collection name", modifier = Modifier.fillMaxWidth())
        }
    }
    if (showMoveDialog) {
        ZeusDialog(onDismissRequest = { showMoveDialog = false }, title = "Move to collection", confirmLabel = "Done", onConfirm = { showMoveDialog = false }, dismissLabel = null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                ZeusChip("Unsorted", false, { onMoveSelected(null); showMoveDialog = false })
                uiState.folders.forEach { f -> ZeusChip(f.name, false, { onMoveSelected(f.id); showMoveDialog = false }) }
            }
        }
    }
    if (showNewPassageDialog) {
        ManualPassageDialog(uiState.books, { showNewPassageDialog = false }) { b, t, p, n -> onSaveManual(b, t, p, n); showNewPassageDialog = false }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HighlightCard(
    item: PassageWithTags,
    folderName: String,
    bookTitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShareCard: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    ZeusCard(modifier.fillMaxWidth(), selected = isSelected, padding = PaddingValues(ZeusSpacing.none)) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = ZeusSpacing.cardPadding, top = ZeusSpacing.mdPlus, bottom = ZeusSpacing.mdPlus, end = ZeusSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                Text("“${item.passage.text}”", style = type.quote, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)) {
                    Box(Modifier.size(ZeusSpacing.sm).clip(ZeusShapes.circle).background(c.accent))
                    val src = listOfNotNull(bookTitle, item.passage.chapterTitle?.takeIf { it.isNotBlank() && it != bookTitle }, item.passage.pageLabel?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { "Manual highlight" }
                    Text(if (isSelected) "Selected · $folderName" else "$src · $folderName", style = type.caption, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                CircleIconButton(Icons.Rounded.AddPhotoAlternate, "Make a share card", onShareCard, tint = c.accentInk)
                CircleIconButton(Icons.Rounded.DeleteOutline, "Delete highlight", onDelete, tint = c.muted)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualPassageDialog(books: List<LibraryBookSummary>, onDismiss: () -> Unit, onSave: (LibraryBookSummary, String, String?, String?) -> Unit) {
    var selectedBook by remember(books) { mutableStateOf(books.firstOrNull()) }
    var text by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    ZeusDialog(onDismissRequest = onDismiss, title = "New highlight", confirmLabel = "Save", confirmEnabled = selectedBook != null && text.isNotBlank(), onConfirm = { selectedBook?.let { onSave(it, text, page, note) } }) {
        if (books.isEmpty()) {
            Text("Import a book into your library first.", style = ZeusTheme.type.body, color = ZeusTheme.colors.muted)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs), maxLines = 3) {
                books.take(6).forEach { b -> ZeusChip(b.title, selectedBook?.editionId == b.editionId, { selectedBook = b }, outline = selectedBook?.editionId != b.editionId) }
            }
            ZeusTextField(text, { text = it }, label = "Passage text", singleLine = false, minLines = 3, modifier = Modifier.fillMaxWidth())
            ZeusTextField(page, { page = it }, label = "Page (optional)", modifier = Modifier.fillMaxWidth())
            ZeusTextField(note, { note = it }, label = "Note (optional)", modifier = Modifier.fillMaxWidth())
        }
    }
}
