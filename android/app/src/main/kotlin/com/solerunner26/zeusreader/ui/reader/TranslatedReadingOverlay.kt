package com.solerunner26.zeusreader.ui.reader

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solerunner26.zeusreader.data.local.BookGlossaryEntity
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.reader.prefs.ReaderFontChoice
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import com.solerunner26.zeusreader.translation.TranslatedDisplayMode
import com.solerunner26.zeusreader.translation.TranslatedPassageResult
import com.solerunner26.zeusreader.translation.TranslationEnginePreference
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TranslatedReadingOverlay(
    sectionLabel: String,
    passageResult: TranslatedPassageResult?,
    isTranslating: Boolean,
    ocrNotice: String?,
    targetLanguage: StudyTargetLanguage,
    displayMode: TranslatedDisplayMode,
    enginePreference: TranslationEnginePreference,
    glossaryTerms: List<BookGlossaryEntity>,
    settings: ReaderSettings,
    onSelectTargetLanguage: (StudyTargetLanguage) -> Unit,
    onSelectDisplayMode: (TranslatedDisplayMode) -> Unit,
    onSelectEnginePreference: (TranslationEnginePreference) -> Unit,
    onRetranslatePassage: () -> Unit,
    onAddGlossaryTerm: (String) -> Unit,
    onRemoveGlossaryTerm: (String) -> Unit,
    onExitTranslatedMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showGlossarySheet by remember { mutableStateOf(false) }
    val revealedEnglishIndices = remember(passageResult?.passageHash) { mutableStateMapOf<Int, Boolean>() }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
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

    val indicFont = remember(targetLanguage) {
        when (targetLanguage) {
            StudyTargetLanguage.HINDI -> ReaderFontChoice.NOTO_DEVANAGARI.toComposeFontFamily()
            StudyTargetLanguage.GUJARATI -> ReaderFontChoice.NOTO_GUJARATI.toComposeFontFamily()
        }
    }
    val baseSp = (16f * (settings.fontSizePercent / 100f)).sp
    val windowWidthPx = LocalWindowInfo.current.containerSize.width
    val isWideWindow = windowWidthPx >= 1500

    Surface(
        modifier = modifier.fillMaxSize(),
        color = settings.theme.backgroundColor,
        contentColor = settings.theme.textColor
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = settings.theme.surfaceColor,
                contentColor = settings.theme.textColor,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Translated Reading • $sectionLabel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            val engineSummary = buildString {
                                append(passageResult?.engineIdVersion ?: enginePreference.engineIdVersion)
                                if (passageResult?.fromCache == true) append(" • cached")
                                append(" • glossary rev ${passageResult?.glossaryRevision ?: 0}")
                            }
                            Text(
                                text = engineSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onRetranslatePassage,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("retranslate_passage_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Retranslate this passage")
                        }
                        IconButton(
                            onClick = { showGlossarySheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_glossary_button")
                        ) {
                            Icon(Icons.Rounded.Book, contentDescription = "Don't translate glossary")
                        }
                        IconButton(
                            onClick = onExitTranslatedMode,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("exit_translated_mode_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Return to original view")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StudyTargetLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = targetLanguage == lang,
                                onClick = { onSelectTargetLanguage(lang) },
                                label = { Text(lang.displayName) }
                            )
                        }
                        TranslatedDisplayMode.entries.forEach { mode ->
                            FilterChip(
                                selected = displayMode == mode,
                                onClick = { onSelectDisplayMode(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                        TranslationEnginePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = enginePreference == pref,
                                onClick = { onSelectEnginePreference(pref) },
                                label = { Text(pref.label) }
                            )
                        }
                    }
                }
            }

            if (isTranslating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!ocrNotice.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ocrNotice,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            val pairs = passageResult?.pairs.orEmpty()
            if (pairs.isEmpty() && !isTranslating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No translatable prose found in this section. Diagrams and formulas stay untouched in original view.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = settings.horizontalMarginDp.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pairs, key = { "${passageResult?.passageHash}_${it.sentenceIndex}" }) { pair ->
                        val revealOriginal = revealedEnglishIndices[pair.sentenceIndex] == true
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = displayMode == TranslatedDisplayMode.TRANSLATION_ONLY) {
                                    revealedEnglishIndices[pair.sentenceIndex] = !revealOriginal
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (displayMode == TranslatedDisplayMode.SIDE_BY_SIDE && isWideWindow) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = pair.sourceEnglish,
                                            fontFamily = settings.fontChoice.toComposeFontFamily(),
                                            fontSize = baseSp,
                                            lineHeight = baseSp * settings.lineHeightMultiplier,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = pair.translatedText,
                                            fontFamily = indicFont,
                                            fontSize = baseSp,
                                            lineHeight = baseSp * settings.lineHeightMultiplier,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    val showEnglish = displayMode != TranslatedDisplayMode.TRANSLATION_ONLY || revealOriginal
                                    if (showEnglish) {
                                        Text(
                                            text = pair.sourceEnglish,
                                            fontFamily = settings.fontChoice.toComposeFontFamily(),
                                            fontSize = baseSp * 0.92f,
                                            lineHeight = baseSp * settings.lineHeightMultiplier,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!pair.keptUntranslated) {
                                        Text(
                                            text = pair.translatedText,
                                            fontFamily = indicFont,
                                            fontSize = baseSp,
                                            lineHeight = baseSp * settings.lineHeightMultiplier
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (pair.keptUntranslated) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("Kept in English") }
                                            )
                                        }
                                        if (displayMode == TranslatedDisplayMode.TRANSLATION_ONLY && !revealOriginal) {
                                            Text(
                                                text = "Tap to reveal English",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    if (!pair.keptUntranslated) {
                                        IconButton(
                                            onClick = {
                                                if (ttsReady) {
                                                    tts.language = Locale.forLanguageTag(targetLanguage.code)
                                                    tts.speak(
                                                        pair.translatedText,
                                                        TextToSpeech.QUEUE_FLUSH,
                                                        null,
                                                        "tr_${pair.sentenceIndex}"
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Listen to translation")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showGlossarySheet) {
        var newTerm by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showGlossarySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Per-Book \"Don't Translate\" Glossary", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Protected terms stay in English across future passages. Adding or removing a term automatically invalidates affected cached translations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTerm,
                        onValueChange = { newTerm = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Add term (e.g., Euler, xylem)…") }
                    )
                    Button(
                        onClick = {
                            if (newTerm.isNotBlank()) {
                                onAddGlossaryTerm(newTerm)
                                newTerm = ""
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
                HorizontalDivider()
                if (glossaryTerms.isEmpty()) {
                    Text(
                        text = "No protected glossary terms added for this book yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(glossaryTerms, key = { it.id }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.term, style = MaterialTheme.typography.bodyLarge)
                                IconButton(
                                    onClick = { onRemoveGlossaryTerm(entry.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Remove glossary term")
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { showGlossarySheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Done")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
