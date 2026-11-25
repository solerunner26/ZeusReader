package com.solerunner26.zeusreader.ui.passages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import com.solerunner26.zeusreader.ui.theme.LocalZeusPalette
import com.solerunner26.zeusreader.ui.theme.ZeusFonts

/**
 * Floating ink selection toolbar per Selection.dc.html:
 * Highlight · Save passage (hue) · Translate · More (Define, Copy, Create card).
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
    val palette = LocalZeusPalette.current
    var showMore by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.testTag("selection_floating_toolbar"),
        shape = MaterialTheme.shapes.extraLarge,
        color = palette.ink,
        contentColor = palette.surface,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = onHighlight, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Highlight", color = palette.surface)
            }
            Button(onClick = onSavePassage, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Save passage")
            }
            TextButton(onClick = onTranslate, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Translate", color = palette.surface)
            }
            TextButton(onClick = { showMore = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("More", color = palette.surface)
            }
            DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                DropdownMenuItem(text = { Text("Define") }, onClick = { showMore = false; onDefine() })
                DropdownMenuItem(text = { Text("Copy") }, onClick = { showMore = false; onCopy() })
                DropdownMenuItem(text = { Text("Create card") }, onClick = { showMore = false; onCreateCard() })
            }
        }
    }
}

/**
 * Bottom sheet for saving a selected passage to a folder with an optional note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePassageBottomSheet(
    quotePreview: String,
    folders: List<PassageFolderEntity>,
    onCreateFolder: (String) -> Unit,
    onSave: (folderId: String?, note: String?, makeCard: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var showNewFolderInput by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("save_passage_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Save passage", style = MaterialTheme.typography.titleLarge)

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "“$quotePreview”",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = ZeusFonts.Fraunces,
                        fontStyle = FontStyle.Italic
                    ),
                    modifier = Modifier.padding(14.dp),
                    maxLines = 4
                )
            }

            Text("Folder", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { selectedFolderId = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text("Unsorted") }
                )
                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(folder.name) }
                    )
                }
                FilterChip(
                    selected = showNewFolderInput,
                    onClick = { showNewFolderInput = !showNewFolderInput },
                    modifier = Modifier.heightIn(min = 48.dp),
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    label = { Text("New folder") }
                )
            }

            if (showNewFolderInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                onCreateFolder(newFolderName.trim())
                                newFolderName = ""
                                showNewFolderInput = false
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Create")
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onSave(selectedFolderId, note.takeIf { it.isNotBlank() }, false) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Save")
                }
                Button(
                    onClick = { onSave(selectedFolderId, note.takeIf { it.isNotBlank() }, true) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Save & make card")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
