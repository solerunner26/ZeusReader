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
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassPillButton
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.PrimaryPillButton
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.theme.GlassSurface
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
            .background(colors.chipSelectedBg)
            .padding(horizontal = ZeusSpacing.xs, vertical = ZeusSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
    ) {
        PrimaryPillButton(label = "Spark it", onClick = onSavePassage, icon = Icons.Rounded.AutoAwesome, leadingIcon = true, height = ZeusSpacing.touchMin, shimmer = false)
        ToolbarText("Glow", onHighlight)
        ToolbarText("Lens", onTranslate)
        Box {
            GlassIconButton(Icons.Rounded.MoreHoriz, "More selection actions", { showMore = true }, tint = colors.chipSelectedFg)
            DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }, containerColor = colors.sheetBackground, shape = ZeusShapes.cardSmall) {
                DropdownMenuItem(leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.accentOnSky) }, text = { Text("Zap (define)", color = colors.textOnSky) }, onClick = { showMore = false; onDefine() })
                DropdownMenuItem(leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, tint = colors.accentOnSky) }, text = { Text("Copy", color = colors.textOnSky) }, onClick = { showMore = false; onCopy() })
                DropdownMenuItem(leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, null, tint = colors.accentOnSky) }, text = { Text("Make a card", color = colors.textOnSky) }, onClick = { showMore = false; onCreateCard() })
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
        color = colors.chipSelectedFg,
        modifier = Modifier
            .clip(ZeusShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ZeusSpacing.md, vertical = ZeusSpacing.md)
    )
}


/**
 * "Spark it" capture sheet: quote preview, constellation picker (+ new), note, save / save & card.
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
        title = "Spark it",
        subtitle = "Save this line to your star map",
        modifier = Modifier.testTag("save_passage_sheet")
    ) {
        GlassSurface(modifier = Modifier.fillMaxWidth(), shape = ZeusShapes.cardCompact, neon = true) {
            Text(
                text = "“$quotePreview”",
                style = type.quote,
                color = colors.textOnSky,
                maxLines = 5,
                modifier = Modifier.padding(ZeusSpacing.lg)
            )
        }
        MetaLabel(text = "Constellation", accent = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
            ZeusChip("Unsorted", selectedFolderId == null, { selectedFolderId = null })
            folders.forEach { folder ->
                ZeusChip(folder.name, selectedFolderId == folder.id, { selectedFolderId = folder.id }, icon = Icons.Rounded.AutoAwesome)
            }
            ZeusChip("New", showNewFolderInput, { showNewFolderInput = !showNewFolderInput }, icon = Icons.Rounded.Add)
        }
        if (showNewFolderInput) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                ZeusTextField(value = newFolderName, onValueChange = { newFolderName = it }, label = "Constellation name", modifier = Modifier.weight(1f))
                GlassPillButton(
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
            GlassPillButton(
                label = "Save",
                onClick = { onSave(selectedFolderId, note.takeIf { it.isNotBlank() }, false) },
                height = ZeusSpacing.primaryButtonHeight,
                modifier = Modifier.weight(1f)
            )
            PrimaryPillButton(
                label = "Save & card",
                icon = Icons.Rounded.AddPhotoAlternate,
                onClick = { onSave(selectedFolderId, note.takeIf { it.isNotBlank() }, true) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
