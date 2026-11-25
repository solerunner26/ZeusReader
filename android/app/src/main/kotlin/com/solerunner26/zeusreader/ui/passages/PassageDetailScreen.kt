package com.solerunner26.zeusreader.ui.passages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
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

    var editedText by remember(passage?.id) { mutableStateOf(passage?.text.orEmpty()) }
    var editedNote by remember(passage?.id) { mutableStateOf(passage?.note.orEmpty()) }
    var selectedFolderId by remember(passage?.id) { mutableStateOf(passage?.folderId) }
    var tagsInput by remember(passage?.id) { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(passage?.id) {
        passage?.let { p ->
            editedText = p.text
            editedNote = p.note.orEmpty()
            selectedFolderId = p.folderId
            tagsInput = viewModel.getTags(p.id).joinToString(", ")
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .testTag("passage_detail_root"),
        topBar = {
            TopAppBar(
                title = { Text("Passage") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    passage?.let { current ->
                        TextButton(
                            onClick = {
                                val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                viewModel.updatePassage(current, editedText, editedNote, selectedFolderId, tags)
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        val current = passage
        if (current == null) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Passage not found.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                label = { Text("Passage text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = current.chapterTitle ?: "Library book",
                        style = MaterialTheme.typography.titleMedium
                    )
                    current.pageLabel?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium)
                    }
                    val hasLocator = current.locatorJson.isNotBlank() && !current.editionId.isNullOrBlank()
                    Button(
                        enabled = hasLocator,
                        onClick = {
                            if (hasLocator) {
                                onOpenInBook(current.editionId!!, current.locatorJson)
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Open")
                    }
                    if (!hasLocator) {
                        Text(
                            text = "Manual passage has no book location.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = editedNote,
                onValueChange = { editedNote = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Text("Folder", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { selectedFolderId = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text("Unsorted") }
                )
                uiState.folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(folder.name) }
                    )
                }
            }

            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Tags (comma-separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (onExplainPassage != null || onSuggestTags != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    onExplainPassage?.let { explain ->
                        OutlinedButton(
                            onClick = { explain(current.text) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text("Explain this passage")
                        }
                    }
                    onSuggestTags?.let { suggest ->
                        OutlinedButton(
                            onClick = { suggest(current.text) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text("Suggest tags")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("Delete")
                }
                Button(
                    onClick = { onCreateCard(current.id) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("Create card")
                }
            }
        }
    }

    if (showDeleteConfirm && passage != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete passage?") },
            text = { Text("You can undo right after deleting from the Passages list.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.softDeletePassage(passage!!.id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Cancel")
                }
            }
        )
    }
}
