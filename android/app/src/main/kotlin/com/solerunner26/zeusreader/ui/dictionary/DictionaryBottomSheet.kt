package com.solerunner26.zeusreader.ui.dictionary

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solerunner26.zeusreader.dictionary.DictionaryLookupResult
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.reader.prefs.ReaderFontChoice
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryBottomSheet(
    initialWord: String,
    sentenceContext: String,
    lookupResult: DictionaryLookupResult?,
    sentenceTranslation: String?,
    selectedTargetLang: StudyTargetLanguage,
    onSelectTargetLang: (StudyTargetLanguage) -> Unit,
    onLookupWord: (String) -> Unit,
    onSaveToVocab: (DictionaryLookupResult, StudyTargetLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
                ttsReady = true
            }
        }
        instance
    }

    DisposableEffect(tts) {
        onDispose {
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        DictionarySheetContent(
            initialWord = initialWord,
            sentenceContext = sentenceContext,
            lookupResult = lookupResult,
            sentenceTranslation = sentenceTranslation,
            selectedTargetLang = selectedTargetLang,
            onSelectTargetLang = onSelectTargetLang,
            onLookupWord = onLookupWord,
            onSpeak = { text ->
                if (ttsReady && text.isNotBlank()) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dict_pronounce")
                }
            },
            onSaveToVocab = { result ->
                onSaveToVocab(result, selectedTargetLang)
            }
        )
    }
}

@Composable
internal fun DictionarySheetContent(
    initialWord: String,
    sentenceContext: String,
    lookupResult: DictionaryLookupResult?,
    sentenceTranslation: String?,
    selectedTargetLang: StudyTargetLanguage,
    onSelectTargetLang: (StudyTargetLanguage) -> Unit,
    onLookupWord: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onSaveToVocab: (DictionaryLookupResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var wordInput by remember(initialWord) { mutableStateOf(initialWord) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("dictionary_sheet_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = wordInput,
                onValueChange = { wordInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Word lookup") }
            )
            IconButton(
                onClick = { onLookupWord(wordInput) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Look up word")
            }
            IconButton(
                onClick = { onSpeak(lookupResult?.queryWord ?: wordInput) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = "Pronounce word")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyTargetLanguage.entries.forEach { lang ->
                FilterChip(
                    selected = selectedTargetLang == lang,
                    onClick = { onSelectTargetLang(lang) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text(lang.displayName) }
                )
            }
        }

        if (lookupResult == null) {
            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Looking up \"$wordInput\"…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lookupResult.matchedLemma.ifBlank { lookupResult.queryWord },
                    style = MaterialTheme.typography.headlineMedium
                )
                if (lookupResult.usedMlKitFallback) {
                    AssistChip(
                        onClick = {},
                        label = { Text("ML Kit fallback") }
                    )
                }
            }

            val glosses = if (selectedTargetLang == StudyTargetLanguage.HINDI) {
                lookupResult.hindiGlosses
            } else {
                lookupResult.gujaratiGlosses
            }
            val scriptFont = if (selectedTargetLang == StudyTargetLanguage.HINDI) {
                ReaderFontChoice.NOTO_DEVANAGARI.toComposeFontFamily()
            } else {
                ReaderFontChoice.NOTO_GUJARATI.toComposeFontFamily()
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${selectedTargetLang.displayName} Meaning",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (glosses.isEmpty()) {
                        Text(
                            text = "No offline meaning available. Download the ${selectedTargetLang.code} pack in Prepare for offline reading.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        glosses.forEach { g ->
                            val prefix = if (g.pos.isNotBlank()) "(${g.pos}) " else ""
                            Text(
                                text = "$prefix${g.gloss}",
                                fontFamily = scriptFont,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (g.transliteration.isNotBlank()) {
                                Text(
                                    text = g.transliteration,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (lookupResult.englishSenses.isNotEmpty()) {
                Text("English Senses (WordNet)", style = MaterialTheme.typography.titleMedium)
                lookupResult.englishSenses.forEachIndexed { idx, sense ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${idx + 1}. (${sense.pos}) ${sense.definition}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (sense.example.isNotBlank()) {
                            Text(
                                text = "\"${sense.example}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Word not found in bundled English WordNet; showing translation fallback if installed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sentenceContext.isNotBlank()) {
                HorizontalDivider()
                Text("Sentence Context", style = MaterialTheme.typography.titleMedium)
                Text(text = sentenceContext, style = MaterialTheme.typography.bodyMedium)
                if (!sentenceTranslation.isNullOrBlank()) {
                    Text(
                        text = sentenceTranslation,
                        fontFamily = scriptFont,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = { onSaveToVocab(lookupResult) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Save to Vocabulary")
            }

            Text(
                text = "${lookupResult.attribution} • ${lookupResult.buildDate}".trim(' ', '•'),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
