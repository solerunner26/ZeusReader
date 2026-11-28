package com.solerunner26.zeusreader.ui.passages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.ui.components.EmptyState
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.SectionCard
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusDialog
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassageDetailScreen(
    passageId: String,
    viewModel: PassagesViewModel,
    onBack: () -> Unit,
    onOpenInBook: (editionId: String, locatorJson: String) -> Unit,
    onCreateCard: (String) -> Unit,
    onExplainPassage: ((String) -> Unit)? = null,
    onSuggestTags: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val passage by viewModel.observePassage(passageId).collectAsStateWithLifecycle(initialValue = null)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type

    var editedText by remember(passage?.id) { mutableStateOf(passage?.text.orEmpty()) }
    var editedNote by remember(passage?.id) { mutableStateOf(passage?.note.orEmpty()) }
    var selectedFolderId by remember(passage?.id) { mutableStateOf(passage?.folderId) }
    var tagsInput by remember(passage?.id) { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var savedFlash by remember { mutableStateOf(false) }

    LaunchedEffect(passage?.id) {
        passage?.let { p ->
            editedText = p.text
            editedNote = p.note.orEmpty()
            selectedFolderId = p.folderId
            tagsInput = viewModel.getTags(p.id).joinToString(", ")
        }
    }

    val save: () -> Unit = {
        passage?.let { current ->
            val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            viewModel.updatePassage(current, editedText, editedNote, selectedFolderId, tags)
            savedFlash = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("passage_detail_root")
    ) {
        ZeusTopBar(
            title = "Highlight",
            subtitle = if (savedFlash) "Saved" else "Edit, file and share",
            onBack = onBack
        ) {
            if (passage != null) {
                CircleIconButton(Icons.Rounded.Check, "Save highlight", save, tint = colors.accentInk)
            }
        }
        val current = passage
        if (current == null) {
            EmptyState(
                title = "Highlight not found",
                message = "It may have been deleted. Go back to your highlights.",
                modifier = Modifier.padding(ZeusSpacing.gutter)
            )
            return@Column
        }
        val book = uiState.books.firstOrNull { it.editionId == current.editionId }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ZeusSpacing.gutter)
                .padding(bottom = rememberNavBarClearance())
                .widthIn(max = ZeusSizes.contentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)
        ) {
            SectionCard(selected = true) {
                Eyebrow(
                    text = listOfNotNull(book?.title, current.chapterTitle?.takeIf { it.isNotBlank() }, current.pageLabel).joinToString(" · ").ifBlank { "Manual highlight" },
                    accent = true,
                    maxLines = 2
                )
                Text("“${editedText.ifBlank { current.text }}”", style = type.quote, color = colors.ink)
                if (current.originalText.isNotBlank() && current.originalText != current.text) {
                    Eyebrow(text = "Original kept")
                    Text(current.originalText, style = type.caption, color = colors.muted, maxLines = 4)
                }
                val hasLocator = current.locatorJson.isNotBlank() && !current.editionId.isNullOrBlank()
                if (hasLocator) {
                    SecondaryButton(
                        label = "Jump to page",
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = { onOpenInBook(current.editionId!!, current.locatorJson) }
                    )
                } else {
                    Text("Added by hand — no book location.", style = type.caption, color = colors.muted)
                }
            }

            ZeusTextField(value = editedText, onValueChange = { editedText = it; savedFlash = false }, label = "Highlight text", singleLine = false, minLines = 3, modifier = Modifier.fillMaxWidth())
            ZeusTextField(value = editedNote, onValueChange = { editedNote = it; savedFlash = false }, label = "Note", singleLine = false, minLines = 2, modifier = Modifier.fillMaxWidth())

            SectionCard(title = "Collection") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                    ZeusChip("Unsorted", selectedFolderId == null, { selectedFolderId = null; savedFlash = false })
                    uiState.folders.forEach { folder ->
                        ZeusChip(folder.name, selectedFolderId == folder.id, { selectedFolderId = folder.id; savedFlash = false })
                    }
                }
            }

            ZeusTextField(value = tagsInput, onValueChange = { tagsInput = it; savedFlash = false }, label = "Tags (comma-separated)", leadingIcon = Icons.Rounded.Sell, modifier = Modifier.fillMaxWidth())

            if (onExplainPassage != null || onSuggestTags != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                    onExplainPassage?.let { explain -> SecondaryButton("Explain this", { explain(current.text) }, modifier = Modifier.weight(1f)) }
                    onSuggestTags?.let { suggest -> SecondaryButton("Suggest tags", { suggest(current.text) }, modifier = Modifier.weight(1f)) }
                }
            }

            PrimaryButton(label = "Save changes", icon = Icons.Rounded.Check, leadingIcon = true, onClick = save, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                SecondaryButton("Delete", { showDeleteConfirm = true }, icon = Icons.Rounded.DeleteOutline, danger = true, modifier = Modifier.weight(1f))
                SecondaryButton("Make a card", { onCreateCard(current.id) }, icon = Icons.Rounded.AddPhotoAlternate, modifier = Modifier.weight(1f))
            }
        }
    }

    if (showDeleteConfirm && passage != null) {
        ZeusDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Delete this highlight?",
            confirmLabel = "Delete",
            dangerConfirm = true,
            onConfirm = {
                passage?.let { viewModel.softDeletePassage(it.id) }
                showDeleteConfirm = false
                onBack()
            }
        ) {
            Text("You can undo right after from the Highlights list.", style = type.body, color = colors.muted)
        }
    }
}
