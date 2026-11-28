package com.solerunner26.zeusreader.ui.dictionary

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.solerunner26.zeusreader.dictionary.BilingualGloss
import com.solerunner26.zeusreader.dictionary.DictionaryLookupResult
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassSection
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.PrimaryPillButton
import com.solerunner26.zeusreader.ui.components.StickerChip
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.ZeusType
import java.util.Locale

/** Zap card (DESIGN.md §2): word lookup (meaning + हिन्दी + ગુજરાતી + sound) / sentence translation. */
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

    ZeusBottomSheet(onDismissRequest = onDismiss) {
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
            onSaveToVocab = { result -> onSaveToVocab(result, selectedTargetLang) }
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
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    var wordInput by remember(initialWord) { mutableStateOf(initialWord) }
    var mode by remember { mutableIntStateOf(0) }
    var saved by remember(lookupResult?.queryWord) { mutableStateOf(false) }
    // Only trust a result that matches what the user asked for (avoids showing a stale word).
    val result = lookupResult?.takeIf {
        wordInput.isBlank() || it.queryWord.equals(wordInput.trim(), ignoreCase = true) ||
            it.matchedLemma.equals(wordInput.trim(), ignoreCase = true)
    } ?: lookupResult?.takeIf { wordInput == initialWord }

    LaunchedEffect(initialWord) {
        if (initialWord.isNotBlank() && lookupResult?.queryWord?.equals(initialWord, true) != true) onLookupWord(initialWord)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dictionary_sheet_root"),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
            StickerChip(text = "Zap", icon = Icons.Rounded.Bolt, accentFill = true, rotation = -3f)
            ZeusSegmented(
                options = listOf("Word", "Whole sentence"),
                selectedIndex = mode,
                onSelect = { mode = it },
                modifier = Modifier.weight(1f)
            )
        }

        if (mode == 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                ZeusTextField(
                    value = wordInput,
                    onValueChange = { wordInput = it },
                    placeholder = "Type or tap a word",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (wordInput.isNotBlank()) onLookupWord(wordInput.trim()) }),
                    modifier = Modifier.weight(1f)
                )
                GlassIconButton(Icons.Rounded.Search, "Look up word", { if (wordInput.isNotBlank()) onLookupWord(wordInput.trim()) })
                GlassIconButton(Icons.AutoMirrored.Rounded.VolumeUp, "Pronounce word", { onSpeak(result?.queryWord ?: wordInput) })
            }

            when {
                wordInput.isBlank() -> Text(
                    "Select a word on the page, or type one above, to zap its meaning in English, हिन्दी and ગુજરાતી.",
                    style = type.body,
                    color = colors.textOnSkySecondary
                )
                result == null -> {
                    ZeusLinearProgress()
                    Text("Zapping “$wordInput”…", style = type.caption, color = colors.textOnSkySecondary)
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                        Text(result.matchedLemma.ifBlank { result.queryWord }, style = type.screenTitle, color = colors.textOnSky, modifier = Modifier.weight(1f), maxLines = 2)
                        if (result.usedMlKitFallback) MetaLabel(text = "ML Kit", accent = true)
                    }
                    result.englishSenses.firstOrNull()?.let { first ->
                        Text("(${first.pos}) ${first.definition}", style = type.body, color = colors.textOnSky)
                    }
                    GlossBlock("हिन्दी", result.hindiGlosses, StudyTargetLanguage.HINDI, selectedTargetLang == StudyTargetLanguage.HINDI, onSelectTargetLang)
                    GlossBlock("ગુજરાતી", result.gujaratiGlosses, StudyTargetLanguage.GUJARATI, selectedTargetLang == StudyTargetLanguage.GUJARATI, onSelectTargetLang)
                    if (result.englishSenses.size > 1) {
                        GlassSection(title = "More senses (WordNet)") {
                            result.englishSenses.drop(1).take(4).forEachIndexed { idx, sense ->
                                Text("${idx + 2}. (${sense.pos}) ${sense.definition}", style = type.caption, color = colors.textOnSky)
                                if (sense.example.isNotBlank()) Text("“${sense.example}”", style = type.quoteSmall, color = colors.textOnSkySecondary)
                            }
                        }
                    } else if (result.englishSenses.isEmpty()) {
                        Text("Not in the bundled English WordNet — showing translation fallback if installed.", style = type.caption, color = colors.textOnSkySecondary)
                    }
                    PrimaryPillButton(
                        label = if (saved) "Saved to Words" else "Save to Words",
                        icon = Icons.Rounded.BookmarkAdd,
                        leadingIcon = true,
                        enabled = !saved,
                        onClick = {
                            onSaveToVocab(result)
                            saved = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    MetaLabel(text = "${result.attribution} · ${result.buildDate}".trim(' ', '·'), maxLines = 2)
                }
            }
        } else {
            ZeusSegmented(
                options = StudyTargetLanguage.entries.map { if (it == StudyTargetLanguage.HINDI) "हिन्दी" else "ગુજરાતી" },
                selectedIndex = StudyTargetLanguage.entries.indexOf(selectedTargetLang),
                onSelect = { onSelectTargetLang(StudyTargetLanguage.entries[it]) }
            )
            if (sentenceContext.isBlank()) {
                Text("Select a sentence on the page to translate it.", style = type.body, color = colors.textOnSkySecondary)
            } else {
                GlassSection(title = "Original") {
                    Text(sentenceContext, style = type.quote, color = colors.textOnSky)
                }
                GlassSection(title = selectedTargetLang.displayName, neon = true) {
                    if (sentenceTranslation.isNullOrBlank()) {
                        ZeusLinearProgress()
                    } else {
                        Text(sentenceTranslation, style = ZeusType.readingFor(selectedTargetLang.code, type.body.copy(fontSize = type.title.fontSize)), color = colors.textOnSky)
                    }
                }
                GlassIconButton(Icons.AutoMirrored.Rounded.VolumeUp, "Read the sentence aloud", { onSpeak(sentenceContext) })
            }
        }
    }
}

@Composable
private fun GlossBlock(
    label: String,
    glosses: List<BilingualGloss>,
    lang: StudyTargetLanguage,
    selected: Boolean,
    onSelect: (StudyTargetLanguage) -> Unit
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    GlassSection(neon = selected) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetaLabel(text = label, accent = true, modifier = Modifier.weight(1f))
            if (!selected) {
                Text(
                    "Save in $label",
                    style = type.labelSmall,
                    color = colors.textOnSkySecondary,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) { onSelect(lang) }
                )
            } else {
                MetaLabel(text = "Saving in this language")
            }
        }
        if (glosses.isEmpty()) {
            Text("No offline meaning yet — download the ${lang.code} pack in Offline packs.", style = type.caption, color = colors.textOnSkySecondary)
        } else {
            glosses.forEach { g ->
                val prefix = if (g.pos.isNotBlank()) "(${g.pos}) " else ""
                Text("$prefix${g.gloss}", style = ZeusType.readingFor(lang.code, type.title), color = colors.textOnSky)
                if (g.transliteration.isNotBlank()) {
                    Text(g.transliteration, style = type.caption, color = colors.textOnSkySecondary)
                }
            }
        }
    }
}

