package com.solerunner26.zeusreader.ui.reader

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.solerunner26.zeusreader.data.local.HighlightEntity
import com.solerunner26.zeusreader.reader.highlights.MarkdownExporter
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.SectionCard
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusDialog
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme

/** Glow: highlights & notes for this book, with Markdown export. */
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

    ZeusBottomSheet(onDismissRequest = onDismiss) {
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
        ZeusDialog(
            onDismissRequest = { showAddDialog = false },
            title = "Highlight a passage",
            confirmLabel = "Save",
            confirmEnabled = snippet.isNotBlank(),
            onConfirm = {
                onAddHighlight(snippet.trim(), note.trim())
                showAddDialog = false
            }
        ) {
            ZeusTextField(value = snippet, onValueChange = { snippet = it }, label = "Passage / excerpt", singleLine = false, minLines = 2, modifier = Modifier.fillMaxWidth())
            ZeusTextField(value = note, onValueChange = { note = it }, label = "Note (optional)", singleLine = false, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
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
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("highlights_sheet_root"),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        Text("Highlights & notes", style = type.headline, color = colors.ink)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
            PrimaryButton("Add highlight", onOpenAddDialog, icon = Icons.Rounded.Add, leadingIcon = true, height = ZeusSpacing.secondaryButtonHeight, shine = false, modifier = Modifier.weight(1f))
            SecondaryButton("Export .md", onExportMarkdown, icon = Icons.Rounded.IosShare, enabled = highlights.isNotEmpty(), modifier = Modifier.weight(1f))
        }
        if (highlights.isEmpty()) {
            Text("No highlights yet for $bookTitle.", style = type.body, color = colors.muted)
        } else {
            highlights.forEach { item ->
                SectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                            val loc = buildList {
                                if (item.chapterTitle.isNotBlank()) add(item.chapterTitle)
                                if (item.pageNumber != null) add("Page ${item.pageNumber}")
                            }.joinToString(" · ")
                            if (loc.isNotBlank()) Eyebrow(text = loc, accent = true)
                            Text(item.selectedText, style = type.quote, color = colors.ink)
                            if (item.note.isNotBlank()) {
                                Text("Note: ${item.note}", style = type.caption, color = colors.muted)
                            }
                        }
                        CircleIconButton(Icons.Rounded.DeleteOutline, "Delete highlight", { onDeleteHighlight(item.id) })
                    }
                }
            }
        }
    }
}
