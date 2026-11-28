package com.solerunner26.zeusreader.ui.passages

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileMove
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.data.PassageWithTags
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import com.solerunner26.zeusreader.ui.components.EmptyState
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassPillButton
import com.solerunner26.zeusreader.ui.components.GradientHeading
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.StickerChip
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusChipRow
import com.solerunner26.zeusreader.ui.components.ZeusDialog
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusSnackbarHost
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.components.rememberDockClearance
import com.solerunner26.zeusreader.ui.theme.GlassSurface
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.rememberPrimaryGradient
import com.solerunner26.zeusreader.ui.theme.rememberStormlightPhase
import kotlin.math.sin

@Composable
fun PassagesScreen(
    viewModel: PassagesViewModel,
    onOpenPassage: (String) -> Unit,
    onCreateCard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.lastDeletedId.collect { deletedId ->
            val result = snackbarHostState.showSnackbar(
                message = "Spark faded away",
                actionLabel = "Bring it back",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restorePassage(deletedId)
            }
        }
    }

    PassagesContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
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
            viewModel.saveManualPassage(
                book = book,
                text = text,
                pageLabel = page,
                folderId = uiState.selectedFolderId?.takeIf { it != PassagesViewModel.UNSORTED_FOLDER_KEY },
                note = note
            )
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
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    var showSearch by remember { mutableStateOf(uiState.searchQuery.isNotBlank()) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewPassageDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    val dockClearance = rememberDockClearance()
    val selecting = uiState.selectedIds.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("passages_screen_root")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = dockClearance),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            item(key = "header") {
                if (selecting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
                    ) {
                        GlassIconButton(Icons.Rounded.Close, "Cancel selection", onClearSelection)
                        Text("${uiState.selectedIds.size} selected", style = type.headline, color = colors.textOnSky, modifier = Modifier.weight(1f), maxLines = 1)
                        GlassIconButton(Icons.Rounded.DriveFileMove, "Move to constellation", { showMoveDialog = true })
                        GlassIconButton(Icons.Rounded.DeleteOutline, "Delete selected", onDeleteSelected, tint = colors.errorGlow)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, top = ZeusSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
                    ) {
                        GradientHeading(text = "Sparks", modifier = Modifier.weight(1f, fill = false))
                        StickerChip(text = "${uiState.passages.size} sparks", accentFill = true, rotation = 4f)
                        Box(modifier = Modifier.weight(1f))
                        GlassIconButton(Icons.Rounded.Search, "Search sparks", { showSearch = !showSearch }, highlighted = showSearch)
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(ZeusSpacing.touchMin)
                                    .clip(ZeusShapes.circle)
                                    .background(rememberPrimaryGradient())
                                    .combinedClickable(onClick = { showAddMenu = true })
                                    .semantics { contentDescription = "New spark or constellation" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.textOnAccent)
                            }
                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false },
                                shape = ZeusShapes.cardSmall,
                                containerColor = colors.sheetBackground
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Rounded.FormatQuote, null, tint = colors.accentOnSky) },
                                    text = { Text("New spark", style = type.label, color = colors.textOnSky) },
                                    onClick = { showAddMenu = false; showNewPassageDialog = true }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null, tint = colors.accentOnSky) },
                                    text = { Text("New constellation", style = type.label, color = colors.textOnSky) },
                                    onClick = { showAddMenu = false; showNewFolderDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            if (showSearch && !selecting) {
                item(key = "search") {
                    ZeusTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = "Search sparks, notes, tags",
                        leadingIcon = Icons.Rounded.Search,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ZeusSpacing.gutter)
                    )
                }
            }

            item(key = "starmap") {
                StarMap(
                    folders = uiState.folders,
                    passages = uiState.passages,
                    selectedFolderId = uiState.selectedFolderId,
                    onSelectFolder = onSelectFolder,
                    modifier = Modifier.padding(horizontal = ZeusSpacing.gutter)
                )
            }

            item(key = "folders") {
                ZeusChipRow {
                    ZeusChip("All", uiState.selectedFolderId == null, { onSelectFolder(null) })
                    ZeusChip("Unsorted", uiState.selectedFolderId == PassagesViewModel.UNSORTED_FOLDER_KEY, { onSelectFolder(PassagesViewModel.UNSORTED_FOLDER_KEY) })
                    uiState.folders.forEach { folder ->
                        ZeusChip(folder.name, uiState.selectedFolderId == folder.id, { onSelectFolder(folder.id) }, icon = Icons.Rounded.AutoAwesome)
                    }
                }
            }

            val selectedFolder = uiState.folders.firstOrNull { it.id == uiState.selectedFolderId }
            if (selectedFolder != null) {
                item(key = "folder_actions") {
                    Row(modifier = Modifier.padding(horizontal = ZeusSpacing.gutter)) {
                        GlassPillButton(
                            label = "Dissolve “${selectedFolder.name}”",
                            icon = Icons.Rounded.DeleteOutline,
                            danger = true,
                            onClick = { onDeleteFolder(selectedFolder.id) }
                        )
                    }
                }
            }

            item(key = "sort") {
                ZeusSegmented(
                    options = PassageSortMode.entries.map { it.label },
                    selectedIndex = PassageSortMode.entries.indexOf(uiState.sortMode),
                    onSelect = { onSortMode(PassageSortMode.entries[it]) },
                    modifier = Modifier.padding(horizontal = ZeusSpacing.gutter)
                )
            }

            if (uiState.passages.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "No sparks yet",
                        message = "Select a line while reading and tap Spark it — it becomes a star here.",
                        actionLabel = "Add a spark",
                        onAction = { showNewPassageDialog = true },
                        modifier = Modifier.padding(horizontal = ZeusSpacing.gutter)
                    )
                }
            } else {
                items(uiState.passages, key = { it.passage.id }) { item ->
                    val folderName = uiState.folders.firstOrNull { it.id == item.passage.folderId }?.name ?: "Unsorted"
                    val bookTitle = uiState.books.firstOrNull { it.editionId == item.passage.editionId }?.title
                    SparkCard(
                        item = item,
                        folderName = folderName,
                        bookTitle = bookTitle,
                        isSelected = item.passage.id in uiState.selectedIds,
                        onClick = {
                            if (selecting) onToggleSelection(item.passage.id) else onOpenPassage(item.passage.id)
                        },
                        onLongClick = { onToggleSelection(item.passage.id) },
                        onShareCard = { onCreateCard(item.passage.id) },
                        onDelete = { onSoftDelete(item.passage.id) },
                        modifier = Modifier.padding(horizontal = ZeusSpacing.gutter)
                    )
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

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        ZeusDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = "New constellation",
            confirmLabel = "Create",
            confirmEnabled = name.isNotBlank(),
            onConfirm = {
                onCreateFolder(name.trim())
                showNewFolderDialog = false
            }
        ) {
            ZeusTextField(value = name, onValueChange = { name = it }, label = "Constellation name", modifier = Modifier.fillMaxWidth())
        }
    }

    if (showMoveDialog) {
        ZeusDialog(
            onDismissRequest = { showMoveDialog = false },
            title = "Move to constellation",
            confirmLabel = "Done",
            onConfirm = { showMoveDialog = false },
            dismissLabel = null
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                ZeusChip("Unsorted", false, { onMoveSelected(null); showMoveDialog = false })
                uiState.folders.forEach { folder ->
                    ZeusChip(folder.name, false, { onMoveSelected(folder.id); showMoveDialog = false })
                }
            }
        }
    }

    if (showNewPassageDialog) {
        ManualPassageDialog(
            books = uiState.books,
            onDismiss = { showNewPassageDialog = false },
            onSave = { book, text, page, note ->
                onSaveManual(book, text, page, note)
                showNewPassageDialog = false
            }
        )
    }
}

/** Animated star map: each constellation (folder) is a glowing chip linked by faint lines. */
@Composable
private fun StarMap(
    folders: List<PassageFolderEntity>,
    passages: List<PassageWithTags>,
    selectedFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    val sky = ZeusTheme.sky
    val phase = rememberStormlightPhase(durationMillis = 7_000)
    val counts = remember(passages) { passages.groupingBy { it.passage.folderId }.eachCount() }
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = ZeusShapes.card, neon = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ZeusSizes.starMapHeight)
        ) {
            if (!ZeusTheme.paper.isEink) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val p = phase.value
                    val pts = listOf(0.08f to 0.30f, 0.22f to 0.62f, 0.40f to 0.22f, 0.58f to 0.70f, 0.74f to 0.28f, 0.90f to 0.58f)
                    pts.zipWithNext().forEachIndexed { i, (a, b) ->
                        drawLine(
                            color = (if (i % 2 == 0) sky.glow1 else sky.glow2).copy(alpha = 0.45f),
                            start = Offset(size.width * a.first, size.height * a.second),
                            end = Offset(size.width * b.first, size.height * b.second),
                            strokeWidth = density
                        )
                    }
                    pts.forEachIndexed { i, (x, y) ->
                        val tw = 0.5f + 0.5f * ((sin(p * 6.283f + i) + 1f) / 2f)
                        drawCircle(
                            color = (if (i % 2 == 0) sky.glow1 else sky.accent).copy(alpha = tw),
                            radius = (if (i % 3 == 0) 4f else 2.5f) * density,
                            center = Offset(size.width * x, size.height * y)
                        )
                    }
                    for (k in 0 until 18) {
                        val fx = ((k * 37) % 100) / 100f
                        val fy = ((k * 53 + 11) % 100) / 100f
                        drawCircle(Color.White.copy(alpha = 0.25f + 0.2f * ((k + (p * 10).toInt()) % 3) / 2f), radius = density, center = Offset(size.width * fx, size.height * fy))
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ZeusSpacing.lg),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                MetaLabel(text = "Your star map", accent = true)
                if (folders.isEmpty()) {
                    Text(
                        "Group sparks into constellations — tap + to start one.",
                        style = ZeusTheme.type.caption,
                        color = colors.textOnSkySecondary
                    )
                } else {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm),
                        maxLines = 2
                    ) {
                        folders.take(6).forEachIndexed { i, folder ->
                            StickerChip(
                                text = "${folder.name} · ${counts[folder.id] ?: 0}",
                                rotation = if (i % 2 == 0) -3f else 3f,
                                accentFill = folder.id == selectedFolderId,
                                onClick = { onSelectFolder(if (selectedFolderId == folder.id) null else folder.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SparkCard(
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
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = ZeusShapes.cardCompact, neon = isSelected, neonAnimated = false) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = ZeusSpacing.lg, top = ZeusSpacing.mdPlus, bottom = ZeusSpacing.mdPlus, end = ZeusSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                Text(
                    text = "“${item.passage.text}”",
                    style = type.quote,
                    color = colors.textOnSky,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)) {
                    Box(
                        modifier = Modifier
                            .size(ZeusSizes.starBright)
                            .clip(ZeusShapes.circle)
                            .background(colors.neonA)
                    )
                    val source = listOfNotNull(
                        bookTitle,
                        item.passage.chapterTitle?.takeIf { it.isNotBlank() && it != bookTitle },
                        item.passage.pageLabel?.takeIf { it.isNotBlank() }
                    ).joinToString(" · ").ifBlank { "Manual spark" }
                    Text(
                        text = if (isSelected) "Selected · $folderName" else "$source · $folderName",
                        style = type.caption,
                        color = colors.textOnSkySecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column {
                GlassIconButton(Icons.Rounded.AddPhotoAlternate, "Make a spark card", onShareCard, tint = colors.accentOnSky)
                GlassIconButton(Icons.Rounded.DeleteOutline, "Delete spark", onDelete, tint = colors.textOnSkySecondary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

    ZeusDialog(
        onDismissRequest = onDismiss,
        title = "New spark",
        confirmLabel = "Save",
        confirmEnabled = selectedBook != null && text.isNotBlank(),
        onConfirm = { selectedBook?.let { onSave(it, text, page, note) } }
    ) {
        if (books.isEmpty()) {
            Text("Import a book into Olympus first.", style = ZeusTheme.type.body, color = ZeusTheme.colors.textOnSkySecondary)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs), maxLines = 3) {
                books.take(6).forEach { book ->
                    ZeusChip(book.title, selectedBook?.editionId == book.editionId, { selectedBook = book })
                }
            }
            ZeusTextField(value = text, onValueChange = { text = it }, label = "Passage text", singleLine = false, minLines = 3, modifier = Modifier.fillMaxWidth())
            ZeusTextField(value = page, onValueChange = { page = it }, label = "Page (optional)", modifier = Modifier.fillMaxWidth())
            ZeusTextField(value = note, onValueChange = { note = it }, label = "Note (optional)", modifier = Modifier.fillMaxWidth())
        }
    }
}
