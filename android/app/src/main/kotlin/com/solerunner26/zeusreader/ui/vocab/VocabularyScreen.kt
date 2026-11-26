package com.solerunner26.zeusreader.ui.vocab

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.ml.OfflinePackStatus
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.vocab.ReviewGrade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareOfflineScreen(
    viewModel: VocabularyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val packStatus by viewModel.packStatus.collectAsStateWithLifecycle()
    val downloading by viewModel.downloadingLang.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Prepare for Offline Reading") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        PrepareOfflineContent(
            packStatus = packStatus,
            downloadingLang = downloading,
            statusMessage = message,
            onDownloadPack = viewModel::downloadPack,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
internal fun PrepareOfflineContent(
    packStatus: OfflinePackStatus,
    downloadingLang: StudyTargetLanguage?,
    statusMessage: String?,
    onDownloadPack: (StudyTargetLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(20.dp)
            .testTag("prepare_offline_root"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (packStatus.isPreparedPhone) {
                        "State: Prepared Phone (Full Offline En / Hi / Gu)"
                    } else {
                        "State: Fresh Install (English Ready; Missing ML Kit Packs)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bundled WordNet English dictionary and Wiktionary seed glosses work immediately in airplane mode. Download ML Kit packs below for full sentence & fallback translation offline.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        PackRow(
            title = "English WordNet + Bilingual Seed DB",
            ready = packStatus.englishDictionaryReady,
            isDownloading = false,
            onDownload = {}
        )
        PackRow(
            title = "ML Kit English -> Hindi (Devanagari)",
            ready = packStatus.hindiPackDownloaded,
            isDownloading = downloadingLang == StudyTargetLanguage.HINDI,
            onDownload = { onDownloadPack(StudyTargetLanguage.HINDI) }
        )
        PackRow(
            title = "ML Kit English -> Gujarati (ગુજરાતી)",
            ready = packStatus.gujaratiPackDownloaded,
            isDownloading = downloadingLang == StudyTargetLanguage.GUJARATI,
            onDownload = { onDownloadPack(StudyTargetLanguage.GUJARATI) }
        )

        if (!statusMessage.isNullOrBlank()) {
            Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PackRow(
    title: String,
    ready: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (ready) {
                    AssistChip(
                        onClick = {},
                        leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text("Ready") }
                    )
                } else {
                    Button(
                        onClick = onDownload,
                        enabled = !isDownloading,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Download")
                    }
                }
            }
            if (isDownloading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    viewModel: VocabularyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val words by viewModel.words.collectAsStateWithLifecycle()
    var flashcardMode by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                title = { Text("Vocabulary & Flashcards") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !flashcardMode,
                    onClick = { flashcardMode = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text("Saved Words (${words.size})") }
                )
                FilterChip(
                    selected = flashcardMode,
                    onClick = { flashcardMode = true },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text("Flashcard Review") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (words.isEmpty()) {
                Text(
                    text = "No saved vocabulary words yet. Look up words while reading to save them with context.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else if (flashcardMode) {
                val now = System.currentTimeMillis()
                val due = words.filter { it.nextReviewEpochMs <= now }.ifEmpty { words }
                val card = due.first()
                var revealed by remember(card.id) { mutableStateOf(false) }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = card.word, style = MaterialTheme.typography.headlineMedium)
                        if (card.sentenceContext.isNotBlank()) {
                            Text(text = "\"${card.sentenceContext}\"", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (!revealed) {
                            Button(
                                onClick = { revealed = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Reveal Meaning")
                            }
                        } else {
                            Text(text = card.translatedMeaning, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            Text(text = card.englishMeaning, style = MaterialTheme.typography.bodyLarge)
                            if (card.bookTitle.isNotBlank()) {
                                Text("From: ${card.bookTitle}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReviewGrade.entries.forEach { grade ->
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.reviewWord(card, grade)
                                            revealed = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Text(grade.label)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(words, key = { it.id }) { item ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${item.word} (${item.targetLang})", style = MaterialTheme.typography.titleMedium)
                                    Text(item.translatedMeaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                    if (item.englishMeaning.isNotBlank()) {
                                        Text(item.englishMeaning, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (item.bookTitle.isNotBlank() || item.sentenceContext.isNotBlank()) {
                                        Text(
                                            text = "${item.bookTitle}: ${item.sentenceContext}".trim(' ', ':'),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteWord(item.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete word")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
