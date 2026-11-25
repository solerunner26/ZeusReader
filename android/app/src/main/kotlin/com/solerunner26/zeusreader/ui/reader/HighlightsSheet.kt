package com.solerunner26.zeusreader.ui.reader

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solerunner26.zeusreader.data.local.HighlightEntity
import com.solerunner26.zeusreader.reader.highlights.MarkdownExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsBottomSheet(
    bookTitle: String,
    highlights: List<HighlightEntity>,
    defaultSnippet: String,
    onAddHighlight: (selectedText: String, note: String) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        HighlightsSheetContent(
            bookTitle = bookTitle,
            highlights = highlights,
            onOpenAddDialog = { showAddDialog = true },
            onDeleteHighlight = onDeleteHighlight,
            onExportMarkdown = {
                val md = MarkdownExporter.exportHighlights(bookTitle, highlights = highlights)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/markdown"
                    putExtra(Intent.EXTRA_SUBJECT, "$bookTitle — Highlights")
                    putExtra(Intent.EXTRA_TEXT, md)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export highlights"))
            }
        )
    }

    if (showAddDialog) {
        var snippet by remember { mutableStateOf(defaultSnippet) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Save Highlight & Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = snippet,
                        onValueChange = { snippet = it },
                        label = { Text("Passage / excerpt") },
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (snippet.isNotBlank()) {
                            onAddHighlight(snippet.trim(), note.trim())
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
internal fun HighlightsSheetContent(
    bookTitle: String,
    highlights: List<HighlightEntity>,
    onOpenAddDialog: () -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onExportMarkdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("highlights_sheet_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Highlights & Notes", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenAddDialog,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Add highlight")
            }
            OutlinedButton(
                onClick = onExportMarkdown,
                enabled = highlights.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Rounded.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Export .md")
            }
        }

        if (highlights.isEmpty()) {
            Text(
                text = "No highlights yet for $bookTitle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(highlights, key = { it.id }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val loc = buildList {
                                    if (item.chapterTitle.isNotBlank()) add(item.chapterTitle)
                                    if (item.pageNumber != null) add("Page ${item.pageNumber}")
                                }.joinToString(" • ")
                                if (loc.isNotBlank()) {
                                    Text(
                                        text = loc,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = item.selectedText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (item.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Note: ${item.note}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteHighlight(item.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete highlight"
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
