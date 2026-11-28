package com.solerunner26.zeusreader.ui.passages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme

/**
 * Floating selection toolbar (Capture.dc.html): Spark it · Glow · Lens · More (Define, Copy, Card).
 */
@Composable
fun SelectionFloatingToolbar(
    onHighlight: () -> Unit,
    onSavePassage: () -> Unit,
    onTranslate: () -> Unit,
    onDefine: () -> Unit,
    onCopy: () -> Unit,
    onCreateCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    var showMore by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .testTag("selection_floating_toolbar")
            .shadow(ZeusSizes.shadowSoft, ZeusShapes.pill)
            .clip(ZeusShapes.pill)
            .background(colors.ink)
            .padding(horizontal = ZeusSpacing.xs, vertical = ZeusSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
    ) {
        PrimaryButton(label = "Save", onClick = onSavePassage, icon = Icons.Rounded.AutoAwesome, leadingIcon = true, height = ZeusSpacing.touchMin, shine = false)
        ToolbarText("Highlight", onHighlight)
        ToolbarText("Translate", onTranslate)
        Box {
            CircleIconButton(Icons.Rounded.MoreHoriz, "More selection actions", { showMore = true }, tint = colors.canvas)
            DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }, containerColor = colors.card, shape = ZeusShapes.cardSmall) {
                DropdownMenuItem(leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.accentInk) }, text = { Text("Dictionary", color = colors.ink) }, onClick = { showMore = false; onDefine() })
                DropdownMenuItem(leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, tint = colors.accentInk) }, text = { Text("Copy", color = colors.ink) }, onClick = { showMore = false; onCopy() })
                DropdownMenuItem(leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, null, tint = colors.accentInk) }, text = { Text("Make a card", color = colors.ink) }, onClick = { showMore = false; onCreateCard() })
            }
        }
    }
}

@Composable
private fun ToolbarText(label: String, onClick: () -> Unit) {
    val colors = ZeusTheme.colors
    Text(
        text = label,
        style = ZeusTheme.type.label,
        color = colors.canvas,
        modifier = Modifier
            .clip(ZeusShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ZeusSpacing.md, vertical = ZeusSpacing.md)
    )
}


/**
 * Save-highlight sheet: quote preview, collection picker (+ new), note, save / save & card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavePassageBottomSheet(
    quotePreview: String,
    folders: List<PassageFolderEntity>,
    onCreateFolder: (String) -> Unit,
    onSave: (folderId: String?, note: String?, makeCard: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var showNewFolderInput by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    ZeusBottomSheet(
        onDismissRequest = onDismiss,
        title = "Save highlight",
        subtitle = "Keep this line in your highlights",
        modifier = Modifier.testTag("save_passage_sheet")
    ) {
        com.solerunner26.zeusreader.ui.components.ZeusCard(modifier = Modifier.fillMaxWidth(), selected = true, padding = androidx.compose.foundation.layout.PaddingValues(ZeusSpacing.none)) {
            Text(
                text = "“$quotePreview”",
                style = type.quote,
                color = colors.ink,
                maxLines = 5,
                modifier = Modifier.padding(ZeusSpacing.lg)
            )
        }
        Eyebrow(text = "Collection")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
            ZeusChip("Unsorted", selectedFolderId == null, { selectedFolderId = null })
            folders.forEach { folder ->
                ZeusChip(folder.name, selectedFolderId == folder.id, { selectedFolderId = folder.id }, icon = Icons.Rounded.AutoAwesome)
            }
            ZeusChip("New", showNewFolderInput, { showNewFolderInput = !showNewFolderInput }, icon = Icons.Rounded.Add)
        }
        if (showNewFolderInput) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                ZeusTextField(value = newFolderName, onValueChange = { newFolderName = it }, label = "Collection name", modifier = Modifier.weight(1f))
                SecondaryButton(
                    label = "Create",
                    enabled = newFolderName.isNotBlank(),
                    onClick = {
                        onCreateFolder(newFolderName.trim())
                        newFolderName = ""
                        showNewFolderInput = false
                    }
                )
            }
        }
        ZeusTextField(value = note, onValueChange = { note = it }, label = "Note (optional)", singleLine = false, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
            SecondaryButton(
                label = "Save",
                onClick = { onSave(selectedFolderId, note.takeIf { it.isNotBlank() }, false) },
                height = ZeusSpacing.primaryButtonHeight,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                label = "Save & card",
                icon = Icons.Rounded.AddPhotoAlternate,
                onClick = { onSave(selectedFolderId, note.takeIf { it.isNotBlank() }, true) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
